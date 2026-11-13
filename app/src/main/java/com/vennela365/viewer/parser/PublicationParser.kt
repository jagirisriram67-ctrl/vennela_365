package com.vennela365.viewer.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.vennela365.viewer.security.SafeLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

data class PublicationChapter(
    val index: Int,
    val title: String,
    val body: String,
    val wordCount: Int,
    val entryPath: String = ""
)

data class CbzComicPage(
    val index: Int,
    val entryPath: String,
    val fileName: String
)

data class PublicationDocument(
    val formatLabel: String,
    val title: String,
    val author: String = "",
    val language: String = "",
    val publisher: String = "",
    val description: String = "",
    val coverBytes: ByteArray? = null,
    val chapters: List<PublicationChapter> = emptyList(),
    val comicPages: List<CbzComicPage> = emptyList(),
    val isDrmOrUnsupportedBinary: Boolean = false,
    val notice: String? = null
) {
    val totalWords: Int get() = chapters.sumOf { it.wordCount }
}

/**
 * Safe, offline-only, zero-dependency parser for EPUB, FB2, and CBZ publications.
 * Respects [SafeLimits] traversal checks and memory bounds.
 */
object PublicationParser {

    private const val MAX_TEXT_ENTRY_BYTES = 2 * 1024 * 1024
    private const val MAX_IMAGE_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_TOTAL_TEXT_BYTES = 12 * 1024 * 1024

    suspend fun parsePublication(
        context: Context,
        uri: Uri?,
        extension: String,
        fallbackTitle: String
    ): PublicationDocument = withContext(Dispatchers.IO) {
        val ext = extension.lowercase().removePrefix(".")
        if (ext in setOf("mobi", "azw", "azw3", "cbr")) {
            val reason = when (ext) {
                "cbr" -> "CBR uses RAR compression which requires an external unrar codec. Rename uncompressed or convert to CBZ (ZIP) to read natively inside Vennela 365."
                else -> "Kindle binary ($ext.uppercase()) may use proprietary compression or DRM. Unprotected EPUB, FB2, and CBZ open directly with full chapter reflow."
            }
            return@withContext PublicationDocument(
                formatLabel = ext.uppercase(),
                title = fallbackTitle,
                isDrmOrUnsupportedBinary = true,
                notice = reason
            )
        }
        if (uri == null) {
            return@withContext PublicationDocument(
                formatLabel = ext.uppercase(),
                title = fallbackTitle,
                notice = "No file stream was provided."
            )
        }
        runCatching {
            when (ext) {
                "cbz" -> parseCbz(context, uri, fallbackTitle)
                "fb2" -> parseFb2(context, uri, fallbackTitle)
                else -> parseEpub(context, uri, fallbackTitle)
            }
        }.getOrElse { err ->
            PublicationDocument(
                formatLabel = ext.uppercase(),
                title = fallbackTitle,
                notice = err.message ?: "Could not decode publication."
            )
        }
    }

    suspend fun loadCbzPageBitmap(
        context: Context,
        uri: Uri,
        entryPath: String,
        maxDimension: Int = 1800
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!SafeLimits.isSafeEntryPath(entryPath)) return@withContext null
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == entryPath) {
                            return@use readBoundedBytes(zip, MAX_IMAGE_ENTRY_BYTES)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    null
                }
            }
        }.getOrNull() ?: return@withContext null
        decodeSampledBitmap(bytes, maxDimension)
    }

    private fun parseCbz(context: Context, uri: Uri, fallbackTitle: String): PublicationDocument {
        val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
        val pages = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                var count = 0
                while (entry != null && count < SafeLimits.MAX_ARCHIVE_ENTRIES) {
                    val name = entry.name.orEmpty()
                    if (!entry.isDirectory && SafeLimits.isSafeEntryPath(name)) {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in imageExts && !name.substringAfterLast('/').startsWith(".")) {
                            pages += name
                        }
                    }
                    count++
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val sorted = pages.sortedWith(naturalPathComparator)
        val comicPages = sorted.mapIndexed { idx, path ->
            CbzComicPage(
                index = idx,
                entryPath = path,
                fileName = path.substringAfterLast('/')
            )
        }
        return PublicationDocument(
            formatLabel = "CBZ COMIC",
            title = fallbackTitle.substringBeforeLast('.'),
            comicPages = comicPages,
            notice = if (comicPages.isEmpty()) "No supported image pages found in this CBZ archive." else null
        )
    }

    private fun parseFb2(context: Context, uri: Uri, fallbackTitle: String): PublicationDocument {
        val xml = context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = readBoundedBytes(stream, MAX_TEXT_ENTRY_BYTES * 2)
            String(bytes, Charsets.UTF_8)
        }.orEmpty()
        return parseFb2Xml(xml, fallbackTitle)
    }

    internal fun parseFb2Xml(xml: String, fallbackTitle: String): PublicationDocument {
        if (xml.isBlank()) {
            return PublicationDocument("FB2", fallbackTitle, notice = "Empty FB2 document.")
        }
        var title = fallbackTitle.substringBeforeLast('.')
        val authorParts = mutableListOf<String>()
        var lang = ""
        val chapters = mutableListOf<PublicationChapter>()

        runCatching {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var event = parser.eventType
            var inTitleInfo = false
            var inBody = false
            var sectionDepth = 0
            var inSectionTitle = false
            var currentTag = ""
            val sectionTitleBuf = StringBuilder()
            val sectionBodyBuf = StringBuilder()

            fun flushSection() {
                val bodyText = sectionBodyBuf.toString().trim()
                if (bodyText.isNotBlank()) {
                    val chTitle = sectionTitleBuf.toString().trim()
                        .ifBlank { "Chapter ${chapters.size + 1}" }
                    chapters += PublicationChapter(
                        index = chapters.size,
                        title = chTitle,
                        body = bodyText,
                        wordCount = countWords(bodyText)
                    )
                }
                sectionTitleBuf.setLength(0)
                sectionBodyBuf.setLength(0)
            }

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()
                        when (currentTag) {
                            "title-info" -> inTitleInfo = true
                            "body" -> inBody = true
                            "section" -> if (inBody) {
                                if (sectionDepth == 0) {
                                    sectionTitleBuf.setLength(0)
                                    sectionBodyBuf.setLength(0)
                                }
                                sectionDepth++
                            }
                            "title" -> if (inBody && sectionDepth > 0) inSectionTitle = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val t = parser.text.orEmpty()
                        if (inTitleInfo) {
                            when (currentTag) {
                                "book-title" -> if (t.isNotBlank()) title = t.trim()
                                "first-name", "last-name" -> if (t.isNotBlank()) authorParts += t.trim()
                                "lang" -> if (t.isNotBlank()) lang = t.trim().uppercase()
                            }
                        } else if (inBody && sectionDepth > 0) {
                            if (inSectionTitle) {
                                sectionTitleBuf.append(t)
                            } else if (currentTag in setOf("p", "v", "subtitle", "emphasis", "strong", "a", "cite")) {
                                sectionBodyBuf.append(t)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name.lowercase()
                        when (endTag) {
                            "title-info" -> inTitleInfo = false
                            "body" -> {
                                flushSection()
                                inBody = false
                            }
                            "title" -> inSectionTitle = false
                            "p", "v", "empty-line", "subtitle" -> {
                                if (inBody && sectionDepth > 0 && !inSectionTitle) {
                                    sectionBodyBuf.append("\n\n")
                                } else if (inSectionTitle) {
                                    sectionTitleBuf.append(" ")
                                }
                            }
                            "section" -> if (inBody) {
                                sectionDepth = (sectionDepth - 1).coerceAtLeast(0)
                                if (sectionDepth == 0) flushSection()
                            }
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
            flushSection()
        }

        return PublicationDocument(
            formatLabel = "FB2 EBOOK",
            title = title,
            author = authorParts.joinToString(" "),
            language = lang,
            chapters = chapters,
            notice = if (chapters.isEmpty()) "No readable chapters extracted from this FB2 file." else null
        )
    }

    private fun parseEpub(context: Context, uri: Uri, fallbackTitle: String): PublicationDocument {
        val textEntries = LinkedHashMap<String, String>()
        var containerXml: String? = null
        var opfPathHint: String? = null
        var totalTextRead = 0
        val potentialCovers = mutableMapOf<String, ByteArray>()

        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                var count = 0
                while (entry != null && count < SafeLimits.MAX_ARCHIVE_ENTRIES) {
                    val name = entry.name.orEmpty()
                    if (!entry.isDirectory && SafeLimits.isSafeEntryPath(name)) {
                        val lower = name.lowercase()
                        when {
                            lower == "meta-inf/container.xml" -> {
                                containerXml = String(readBoundedBytes(zip, 64 * 1024), Charsets.UTF_8)
                            }
                            lower.endsWith(".opf") -> {
                                opfPathHint = name
                                val content = String(readBoundedBytes(zip, MAX_TEXT_ENTRY_BYTES), Charsets.UTF_8)
                                textEntries[name] = content
                            }
                            (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xml")) &&
                                totalTextRead < MAX_TOTAL_TEXT_BYTES -> {
                                val bytes = readBoundedBytes(zip, MAX_TEXT_ENTRY_BYTES)
                                totalTextRead += bytes.size
                                textEntries[name] = String(bytes, Charsets.UTF_8)
                            }
                            (lower.contains("cover") && (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"))) &&
                                potentialCovers.size < 3 -> {
                                potentialCovers[name] = readBoundedBytes(zip, 2 * 1024 * 1024)
                            }
                        }
                    }
                    count++
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val resolvedOpfPath = containerXml?.let { extractOpfPathFromContainer(it) } ?: opfPathHint
        val opfXml = resolvedOpfPath?.let { path ->
            textEntries[path] ?: textEntries.entries.firstOrNull { it.key.equals(path, ignoreCase = true) }?.value
        }

        return parseEpubContents(
            opfPath = resolvedOpfPath,
            opfXml = opfXml,
            textEntries = textEntries,
            coverBytes = potentialCovers.values.firstOrNull(),
            fallbackTitle = fallbackTitle
        )
    }

    internal fun extractOpfPathFromContainer(containerXml: String): String? {
        val m = Regex("""full-path\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(containerXml)
        return m?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    internal fun parseEpubContents(
        opfPath: String?,
        opfXml: String?,
        textEntries: Map<String, String>,
        coverBytes: ByteArray? = null,
        fallbackTitle: String
    ): PublicationDocument {
        var title = fallbackTitle.substringBeforeLast('.')
        var author = ""
        var language = ""
        var publisher = ""
        val spineFiles = mutableListOf<String>()

        if (!opfXml.isNullOrBlank()) {
            val baseDir = opfPath?.substringBeforeLast('/', "")?.let { if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it }.orEmpty()
            val manifest = LinkedHashMap<String, String>()
            val spineIds = mutableListOf<String>()

            Regex("""<(?:[a-zA-Z0-9]+:)?title[^>]*>([^<]+)</""", RegexOption.IGNORE_CASE).find(opfXml)?.let {
                title = decodeXmlEntities(it.groupValues[1].trim())
            }
            Regex("""<(?:[a-zA-Z0-9]+:)?creator[^>]*>([^<]+)</""", RegexOption.IGNORE_CASE).find(opfXml)?.let {
                author = decodeXmlEntities(it.groupValues[1].trim())
            }
            Regex("""<(?:[a-zA-Z0-9]+:)?language[^>]*>([^<]+)</""", RegexOption.IGNORE_CASE).find(opfXml)?.let {
                language = decodeXmlEntities(it.groupValues[1].trim()).uppercase()
            }
            Regex("""<(?:[a-zA-Z0-9]+:)?publisher[^>]*>([^<]+)</""", RegexOption.IGNORE_CASE).find(opfXml)?.let {
                publisher = decodeXmlEntities(it.groupValues[1].trim())
            }

            // Parse manifest items: <item id="..." href="..." />
            val itemRegex = Regex("""<item\b([^>]+)/?>""", RegexOption.IGNORE_CASE)
            itemRegex.findAll(opfXml).forEach { match ->
                val attrs = match.groupValues[1]
                val id = extractAttr(attrs, "id")
                val href = extractAttr(attrs, "href")
                if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                    val decodedHref = runCatching { java.net.URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
                    manifest[id] = baseDir + decodedHref.removePrefix("./")
                }
            }

            // Parse spine order: <itemref idref="..." />
            val itemRefRegex = Regex("""<itemref\b([^>]+)/?>""", RegexOption.IGNORE_CASE)
            itemRefRegex.findAll(opfXml).forEach { match ->
                val attrs = match.groupValues[1]
                val idref = extractAttr(attrs, "idref")
                if (!idref.isNullOrBlank()) spineIds += idref
            }

            spineIds.forEach { id ->
                manifest[id]?.let { path -> spineFiles += path }
            }
        }

        val orderedHtmlEntries = if (spineFiles.isNotEmpty()) {
            spineFiles.mapNotNull { target ->
                val raw = textEntries[target]
                    ?: textEntries.entries.firstOrNull { it.key.equals(target, ignoreCase = true) }?.value
                if (raw != null) target to raw else null
            }
        } else {
            textEntries.entries
                .filter { (k, _) ->
                    val l = k.lowercase()
                    (l.endsWith(".xhtml") || l.endsWith(".html") || l.endsWith(".htm")) &&
                        !l.endsWith("nav.xhtml")
                }
                .sortedBy { it.key }
                .map { it.key to it.value }
        }

        val chapters = mutableListOf<PublicationChapter>()
        orderedHtmlEntries.forEach { (path, html) ->
            val cleanText = htmlChapterToText(html)
            if (cleanText.length >= 20 || chapters.isEmpty()) {
                if (cleanText.isNotBlank()) {
                    val heading = extractHtmlHeading(html) ?: path.substringAfterLast('/').substringBeforeLast('.')
                        .replace('_', ' ').replace('-', ' ').replaceFirstChar { it.uppercase() }
                    chapters += PublicationChapter(
                        index = chapters.size,
                        title = heading,
                        body = cleanText,
                        wordCount = countWords(cleanText),
                        entryPath = path
                    )
                }
            }
        }

        return PublicationDocument(
            formatLabel = "EPUB EBOOK",
            title = title,
            author = author,
            language = language,
            publisher = publisher,
            coverBytes = coverBytes,
            chapters = chapters,
            notice = if (chapters.isEmpty()) "No readable XHTML chapters were found inside this EPUB." else null
        )
    }

    private fun extractAttr(attrs: String, name: String): String? {
        val m = Regex("""\b$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(attrs)
        return m?.groupValues?.getOrNull(1)
    }

    private fun extractHtmlHeading(html: String): String? {
        val heading = Regex("""<(h[1-3])\b[^>]*>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(2)
            ?.let { stripTags(it).trim() }
            ?.takeIf { it.isNotBlank() && it.length <= 120 }
        if (heading != null) return heading
        val title = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)
            ?.let { stripTags(it).trim() }
            ?.takeIf { it.isNotBlank() && it.length <= 100 && !it.equals("unknown", ignoreCase = true) }
        return title
    }

    private fun htmlChapterToText(html: String): String {
        var text = html
        text = text.replace(Regex("""<head\b[^>]*>.*?</head>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        text = text.replace(Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        text = text.replace(Regex("""<style\b[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        text = text.replace(Regex("""<(h[1-6])\b[^>]*>""", RegexOption.IGNORE_CASE), "\n\n## ")
        text = text.replace(Regex("""</(p|div|li|blockquote|h[1-6]|section|article|tr)>""", RegexOption.IGNORE_CASE), "\n\n")
        text = text.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        text = stripTags(text)
        text = decodeXmlEntities(text)
        return text.lines()
            .map { it.trim() }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun stripTags(input: String): String =
        input.replace(Regex("""<[^>]+>"""), "")

    private fun decodeXmlEntities(input: String): String = input
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .replace("&rsquo;", "’")
        .replace("&lsquo;", "‘")
        .replace("&rdquo;", "”")
        .replace("&ldquo;", "“")

    private fun countWords(text: String): Int =
        text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }

    private fun readBoundedBytes(stream: java.io.InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (total < limit) {
            val toRead = minOf(buf.size, limit - total)
            val r = stream.read(buf, 0, toRead)
            if (r <= 0) break
            out.write(buf, 0, r)
            total += r
        }
        return out.toByteArray()
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private val naturalPathComparator = Comparator<String> { a, b ->
        val chunkRegex = Regex("""(\d+|\D+)""")
        val chunksA = chunkRegex.findAll(a.lowercase()).map { it.value }.toList()
        val chunksB = chunkRegex.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(chunksA.size, chunksB.size)) {
            val ca = chunksA[i]
            val cb = chunksB[i]
            val na = ca.toLongOrNull()
            val nb = cb.toLongOrNull()
            val cmp = if (na != null && nb != null) na.compareTo(nb) else ca.compareTo(cb)
            if (cmp != 0) return@Comparator cmp
        }
        chunksA.size.compareTo(chunksB.size)
    }
}
