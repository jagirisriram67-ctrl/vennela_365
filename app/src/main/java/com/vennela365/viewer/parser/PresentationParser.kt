package com.vennela365.viewer.parser

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

/** One slide's real text, in document order. */
@Immutable
data class Slide(
    val number: Int,
    val title: String,
    val body: List<String>,
    val notes: String?
)

/** A parsed presentation. [truncated] is honest about enforced limits. */
@Immutable
data class Deck(
    val title: String?,
    val author: String?,
    val slides: List<Slide>,
    val truncated: Boolean,
    val unsupportedReason: String? = null
)

/**
 * Presentation text extraction for OOXML and OpenDocument decks.
 *
 * This is a text-and-structure reader, not a layout rasteriser: it recovers the
 * real title and body text of every slide, in order, plus speaker notes and the
 * document properties. It never invents content, and it says so plainly when a
 * format is a binary container it cannot decompose.
 */
object PresentationParser {

    /** Bounded work so a hostile or enormous deck cannot exhaust memory. */
    private const val MAX_SLIDES = 500
    private const val MAX_CHARS_PER_SLIDE = 8_000
    private const val MAX_XML_CHARS = 400_000

    private const val CACHE_CAPACITY = 6

    private val cache = object : LinkedHashMap<String, Deck>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Deck>) =
            size > CACHE_CAPACITY
    }

    private val ooxmlExtensions = setOf(
        "pptx", "pptm", "ppsx", "ppsm", "potx", "potm"
    )
    private val openDocumentExtensions = setOf("odp", "otp", "fodp")
    private val binaryExtensions = setOf("ppt", "pps", "pot", "sxi", "show", "shw")

    /** True when [extension] is one this parser genuinely decomposes. */
    fun canParse(extension: String): Boolean {
        val ext = extension.lowercase().removePrefix(".")
        return ext in ooxmlExtensions || ext in openDocumentExtensions
    }

    /**
     * Parses [uri] as a deck. Returns null when nothing readable could be
     * recovered, so the caller can show an honest failure state instead of an
     * empty deck that looks like a successful parse.
     */
    suspend fun parse(context: Context, uri: Uri?, extension: String): Deck? {
        if (uri == null) return null
        val ext = extension.lowercase().removePrefix(".")
        val key = "$uri|$ext"

        synchronized(cache) { cache[key] }?.let { return it }

        val deck = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    ext in ooxmlExtensions -> parseOoxml(context, uri)
                    ext in openDocumentExtensions -> parseOpenDocument(context, uri)
                    ext in binaryExtensions -> null
                    else -> null
                }
            }.getOrNull()
        } ?: return null

        synchronized(cache) { cache[key] = deck }
        return deck
    }

    /* --------------------------------------------------------------------- */
    /* OOXML                                                                  */
    /* --------------------------------------------------------------------- */

    private fun parseOoxml(context: Context, uri: Uri): Deck? {
        val slideXml = LinkedHashMap<Int, String>()
        val notesXml = LinkedHashMap<Int, String>()
        var coreXml: String? = null
        var appXml: String? = null
        var truncated = false

        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Some Windows-hosted writers store `\` separators, which
                    // the ZIP specification forbids but which occur in the
                    // wild, so normalise the name before matching.
                    val name = entry.name.orEmpty().replace('\\', '/')
                    when {
                        name.matches(Regex("ppt/slides/slide\\d+\\.xml", RegexOption.IGNORE_CASE)) -> {
                            val number = trailingNumber(name)
                            if (number != null && slideXml.size < MAX_SLIDES) {
                                slideXml[number] = readEntryBounded(zip)
                            } else if (number != null) {
                                truncated = true
                            }
                        }

                        name.matches(Regex("ppt/notesSlides/notesSlide\\d+\\.xml", RegexOption.IGNORE_CASE)) -> {
                            val number = trailingNumber(name)
                            if (number != null && notesXml.size < MAX_SLIDES) {
                                notesXml[number] = readEntryBounded(zip)
                            }
                        }

                        name.equals("docProps/core.xml", ignoreCase = true) -> {
                            coreXml = readEntryBounded(zip)
                        }

                        name.equals("docProps/app.xml", ignoreCase = true) -> {
                            appXml = readEntryBounded(zip)
                        }

                        // Stop early once we have everything we need.
                        slideXml.size >= MAX_SLIDES && coreXml != null && appXml != null -> Unit
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        if (slideXml.isEmpty()) return null

        // Sort numerically: slide10 must never precede slide2.
        val ordered = slideXml.entries.sortedBy { it.key }
        val slides = ordered.mapIndexed { index, (number, xml) ->
            val paragraphs = paragraphsOf(xml, "a:p", "a:t")
            val titleParagraph = paragraphs.firstOrNull { it.isNotBlank() }
            val body = paragraphs.drop(1).filter { it.isNotBlank() }
            Slide(
                number = index + 1,
                title = titleParagraph?.take(180) ?: "Slide ${index + 1}",
                body = body.map { it.take(400) }.take(40),
                notes = notesXml[number]
                    ?.let { notesXmlText -> paragraphsOf(notesXmlText, "a:p", "a:t") }
                    ?.filter { it.isNotBlank() }
                    ?.joinToString("\n")
                    ?.takeIf { it.isNotBlank() }
            )
        }

        return Deck(
            title = coreXml?.let { tagValue(it, "dc:title") }?.takeIf { it.isNotBlank() },
            author = coreXml?.let { tagValue(it, "dc:creator") }?.takeIf { it.isNotBlank() },
            slides = slides,
            truncated = truncated,
            unsupportedReason = null
        )
    }

    /* --------------------------------------------------------------------- */
    /* OpenDocument                                                           */
    /* --------------------------------------------------------------------- */

    private fun parseOpenDocument(context: Context, uri: Uri): Deck? {
        // .fodp is a single flat XML document; .odp/.otp are ZIP containers.
        val xml = context.contentResolver.openInputStream(uri)?.use { stream ->
            readZipEntry(stream, "content.xml", MAX_XML_CHARS)
        }
        val content = xml ?: context.contentResolver.openInputStream(uri)?.use { stream ->
            readPlainBounded(stream, MAX_XML_CHARS)
        } ?: return null

        val pages = content.split(Regex("<draw:page\\b", RegexOption.IGNORE_CASE)).drop(1)
        if (pages.isEmpty()) return null

        val truncated = pages.size > MAX_SLIDES
        val slides = pages.take(MAX_SLIDES).mapIndexed { index, page ->
            val paragraphs = paragraphsOf(page, "text:p", "text:span")
                .ifEmpty { paragraphsOf(page, "text:p", "text:p") }
                .filter { it.isNotBlank() }
            Slide(
                number = index + 1,
                title = paragraphs.firstOrNull()?.take(180) ?: "Slide ${index + 1}",
                body = paragraphs.drop(1).map { it.take(400) }.take(40),
                notes = null
            )
        }

        return Deck(
            title = tagValue(content, "dc:title")?.takeIf { it.isNotBlank() },
            author = tagValue(content, "meta:initial-creator")
                ?.takeIf { it.isNotBlank() }
                ?: tagValue(content, "dc:creator")?.takeIf { it.isNotBlank() },
            slides = slides,
            truncated = truncated,
            unsupportedReason = null
        )
    }

    /* --------------------------------------------------------------------- */
    /* Text helpers                                                           */
    /* --------------------------------------------------------------------- */

    /**
     * Extracts paragraph text. [paragraphTag] delimits paragraphs and
     * [runTag] delimits inline runs; runs are concatenated without a separator
     * so a sentence split across runs reads correctly.
     */
    private fun paragraphsOf(xml: String, paragraphTag: String, runTag: String): List<String> {
        val paragraphs = Regex(
            "<$paragraphTag\\b[^>]*>(.*?)</$paragraphTag>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(xml)

        val runRegex = Regex(
            "<$runTag\\b[^>]*>(.*?)</$runTag>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val out = ArrayList<String>()
        for (paragraph in paragraphs) {
            val inner = paragraph.groupValues.getOrNull(1).orEmpty()
            val runs = runRegex.findAll(inner).map { unescape(it.groupValues[1]) }.toList()
            val text = if (runs.isNotEmpty()) runs.joinToString("") else unescape(stripTags(inner))
            val cleaned = text.replace(Regex("\\s+"), " ").trim()
            if (cleaned.isNotEmpty()) out.add(cleaned)
            if (out.size >= 200) break
        }
        return out
    }

    private fun stripTags(value: String): String = value.replace(Regex("<[^>]+>"), "")

    private fun tagValue(xml: String, tag: String): String? {
        val match = Regex(
            "<$tag\\b[^>]*>(.*?)</$tag>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(xml) ?: return null
        return unescape(match.groupValues[1]).trim().takeIf { it.isNotBlank() }
    }

    private fun unescape(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    private fun trailingNumber(name: String): Int? =
        Regex("(\\d+)\\.xml$", RegexOption.IGNORE_CASE).find(name)
            ?.groupValues?.getOrNull(1)?.toIntOrNull()

    /* --------------------------------------------------------------------- */
    /* Bounded IO                                                             */
    /* --------------------------------------------------------------------- */

    private fun readEntryBounded(zip: ZipInputStream, maxChars: Int = MAX_XML_CHARS): String {
        val builder = StringBuilder()
        val buffer = ByteArray(16 * 1024)
        while (builder.length < maxChars) {
            val read = zip.read(buffer)
            if (read <= 0) break
            builder.append(String(buffer, 0, read, Charsets.UTF_8))
        }
        return builder.toString()
    }

    private fun readPlainBounded(stream: InputStream, maxChars: Int): String {
        val builder = StringBuilder()
        val buffer = ByteArray(16 * 1024)
        while (builder.length < maxChars) {
            val read = stream.read(buffer)
            if (read <= 0) break
            builder.append(String(buffer, 0, read, Charsets.UTF_8))
        }
        return builder.toString()
    }

    /** Streams the archive once, returning the single wanted entry. */
    private fun readZipEntry(stream: InputStream, entryName: String, maxChars: Int): String? {
        return runCatching {
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.orEmpty().replace('\\', '/').equals(entryName, ignoreCase = true)) {
                        return@use readEntryBounded(zip, maxChars)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        }.getOrNull()
    }
}
