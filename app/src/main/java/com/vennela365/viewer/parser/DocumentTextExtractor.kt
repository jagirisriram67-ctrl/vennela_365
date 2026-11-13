package com.vennela365.viewer.parser

import android.content.Context
import android.net.Uri
import com.vennela365.viewer.storage.VennelaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

data class ExtractedText(
    val text: String,
    val truncated: Boolean = false
)

/**
 * Read-only, bounded text extraction for office documents, ebooks, contacts, and calendar files.
 *
 * - Never writes to the source file.
 * - Never executes macros, scripts, or embedded objects.
 * - Caps extracted character count (400,000 chars) for zero-latency, low-memory performance on low-end devices.
 */
object DocumentTextExtractor {

    private const val MAX_CHARS = 400_000
    private const val MAX_ENTRY_BYTES = 8 * 1024 * 1024

    suspend fun readPlainText(context: Context, uri: Uri?): String = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext ""
        val cacheKey = "plain_${uri}"
        VennelaCacheManager.getCachedText(cacheKey)?.let { return@withContext it }

        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(MAX_CHARS)
                var total = 0
                while (total < buffer.size) {
                    val read = reader.read(buffer, total, buffer.size - total)
                    if (read <= 0) break
                    total += read
                }
                if (total <= 0) "" else String(buffer, 0, total)
            }.orEmpty()
        }.getOrDefault("")

        VennelaCacheManager.putCachedText(cacheKey, text)
        text
    }

    suspend fun extract(
        context: Context,
        uri: Uri?,
        extension: String
    ): ExtractedText = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext ExtractedText("")

        val cacheKey = "extract_${uri}_${extension}"
        VennelaCacheManager.getCachedText(cacheKey)?.let { return@withContext ExtractedText(it) }

        val ext = extension.lowercase().removePrefix(".")
        val result = runCatching {
            when (ext) {
                "docx", "docm", "dotx", "dotm", "hwpx", "xps", "oxps" ->
                    fromZip(context, uri, listOf("word/document.xml"), listOf("</w:p>", "<w:br/>"))

                "odt", "ott", "fodt", "abw", "sxw" ->
                    fromZip(context, uri, listOf("content.xml"), listOf("</text:p>", "</text:h>"))

                "pptx", "pptm", "ppsx", "ppsm", "potx", "potm" ->
                    fromPptx(context, uri)

                "epub" ->
                    fromEpub(context, uri)

                "rtf" ->
                    ExtractedText(rtfToText(readPlainText(context, uri)))

                "vcf" ->
                    ExtractedText(parseVCard(readPlainText(context, uri)))

                "ics" ->
                    ExtractedText(parseICalendar(readPlainText(context, uri)))

                "html", "htm", "xhtml" ->
                    ExtractedText(htmlToText(readPlainText(context, uri)))

                "pages", "key", "numbers" -> ExtractedText(
                    "Apple iWork Document ($ext.uppercase())\n\n" +
                    "Vennela 365 opens this package in read-only mode.\n" +
                    "Exact high-fidelity page rendering requires Apple Keynote/Pages/Numbers, " +
                    "but you can inspect all bundled media, XML structures, and embedded preview images safely in Archive Browser."
                )

                else -> ExtractedText(readPlainText(context, uri))
            }
        }.getOrDefault(ExtractedText(""))

        VennelaCacheManager.putCachedText(cacheKey, result.text)
        result
    }

    private fun fromPptx(context: Context, uri: Uri): ExtractedText {
        val builder = StringBuilder()
        var truncated = false
        var slideIndex = 1

        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && builder.length < MAX_CHARS) {
                    val name = entry.name.orEmpty()
                    if (name.startsWith("ppt/slides/slide", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) {
                        val xml = readEntryBounded(zip)
                        val slideText = xmlToText(xml, listOf("</a:p>", "</p:txBody>"))
                        if (slideText.isNotBlank()) {
                            builder.append("─── [Slide ").append(slideIndex++).append("] ───\n")
                            builder.append(slideText).append("\n\n")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        if (builder.length > MAX_CHARS) {
            truncated = true
            builder.setLength(MAX_CHARS)
        }
        return ExtractedText(cleanWhitespace(builder.toString()), truncated)
    }

    private fun fromEpub(context: Context, uri: Uri): ExtractedText {
        val builder = StringBuilder()
        var truncated = false

        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && builder.length < MAX_CHARS) {
                    val name = entry.name.orEmpty()
                    if ((name.endsWith(".xhtml", ignoreCase = true) || name.endsWith(".html", ignoreCase = true)) &&
                        !name.contains("nav", ignoreCase = true) && !name.contains("toc", ignoreCase = true)) {
                        val html = readEntryBounded(zip)
                        val text = htmlToText(html)
                        if (text.isNotBlank()) {
                            builder.append(text).append("\n\n")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        if (builder.length > MAX_CHARS) {
            truncated = true
            builder.setLength(MAX_CHARS)
        }
        return ExtractedText(cleanWhitespace(builder.toString()), truncated)
    }

    private fun fromZip(
        context: Context,
        uri: Uri,
        entryNames: List<String> = emptyList(),
        paragraphTags: List<String>
    ): ExtractedText {
        val builder = StringBuilder()
        var truncated = false
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                var visited = 0
                while (entry != null && visited < 64 && builder.length < MAX_CHARS) {
                    val name = entry.name.orEmpty()
                    val wanted = entryNames.any { it.equals(name, ignoreCase = true) }
                    if (wanted) {
                        val xml = readEntryBounded(zip)
                        builder.append(xmlToText(xml, paragraphTags))
                        builder.append('\n')
                        visited++
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        if (builder.length > MAX_CHARS) {
            truncated = true
            builder.setLength(MAX_CHARS)
        }
        return ExtractedText(cleanWhitespace(builder.toString()), truncated)
    }

    private fun readEntryBounded(zip: ZipInputStream): String {
        val out = StringBuilder()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (total < MAX_ENTRY_BYTES) {
            val read = zip.read(buffer)
            if (read <= 0) break
            total += read
            out.append(String(buffer, 0, read, Charsets.UTF_8))
        }
        return out.toString()
    }

    private fun xmlToText(xml: String, paragraphTags: List<String>): String {
        var text = xml

        // Word records outline levels in paragraph styles. Turning those into
        // markdown hashes lets the reader show real headings instead of a wall
        // of body text, without needing a layout engine.
        text = Regex(
            """<w:pStyle\s+w:val="(?:Heading|heading)\s*([1-6])"\s*/>""",
            RegexOption.IGNORE_CASE
        ).replace(text) { match ->
            "\n" + "#".repeat(match.groupValues[1].toIntOrNull() ?: 1) + " "
        }
        text = Regex(
            """<w:pStyle\s+w:val="Title"\s*/>""",
            RegexOption.IGNORE_CASE
        ).replace(text, "\n# ")

        for (tag in paragraphTags) text = text.replace(tag, "\n", ignoreCase = true)
        text = text
            .replace(Regex("<w:tab[^>]*/>", RegexOption.IGNORE_CASE), "\t")
            // Table cells become pipe-separated rows so the reader can lay
            // them out as a table rather than one run-on paragraph.
            .replace(Regex("</w:tc>", RegexOption.IGNORE_CASE), " | ")
            .replace(Regex("</w:tr>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
        return decodeEntities(text)
    }

    private fun htmlToText(html: String): String {
        var text = html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<h[1-6][^>]*>(.*?)</h[1-6]>", RegexOption.IGNORE_CASE), "\n### $1\n")
            .replace(Regex("<[^>]+>"), "")
        return decodeEntities(cleanWhitespace(text))
    }

    private fun decodeEntities(text: String): String {
        var out = text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
        out = Regex("&#x([0-9a-fA-F]{1,6});").replace(out) { m ->
            val code = m.groupValues[1].toIntOrNull(16)
            if (code != null && code in 0..0x10FFFF) String(Character.toChars(code)) else ""
        }
        out = Regex("&#([0-9]{1,7});").replace(out) { m ->
            val code = m.groupValues[1].toIntOrNull()
            if (code != null && code in 0..0x10FFFF) String(Character.toChars(code)) else ""
        }
        return out
    }

    private fun rtfToText(rtf: String): String {
        var text = rtf
            .replace(Regex("\\\\'[0-9a-fA-F]{2}"), " ")
            .replace("\\par", "\n")
            .replace("\\line", "\n")
            .replace("\\tab", "\t")
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), " ")
            .replace(Regex("[{}]"), "")
        text = decodeEntities(text)
        return cleanWhitespace(text)
    }

    private fun parseVCard(vcf: String): String {
        val lines = vcf.lines()
        val sb = StringBuilder("📇 Contact Card\n\n")
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("FN:", ignoreCase = true) -> sb.append("Name: ").append(trimmed.substring(3)).append('\n')
                trimmed.startsWith("TEL", ignoreCase = true) -> sb.append("Phone: ").append(trimmed.substringAfter(':')).append('\n')
                trimmed.startsWith("EMAIL", ignoreCase = true) -> sb.append("Email: ").append(trimmed.substringAfter(':')).append('\n')
                trimmed.startsWith("ORG:", ignoreCase = true) -> sb.append("Organization: ").append(trimmed.substring(4)).append('\n')
                trimmed.startsWith("TITLE:", ignoreCase = true) -> sb.append("Title: ").append(trimmed.substring(6)).append('\n')
                trimmed.startsWith("ADR", ignoreCase = true) -> sb.append("Address: ").append(trimmed.substringAfter(':').replace(';', ' ')).append('\n')
                trimmed.startsWith("NOTE:", ignoreCase = true) -> sb.append("Note: ").append(trimmed.substring(5)).append('\n')
            }
        }
        return sb.toString()
    }

    private fun parseICalendar(ics: String): String {
        val lines = ics.lines()
        val sb = StringBuilder("📅 Calendar Event\n\n")
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("SUMMARY:", ignoreCase = true) -> sb.append("Event: ").append(trimmed.substring(8)).append('\n')
                trimmed.startsWith("DTSTART", ignoreCase = true) -> sb.append("Starts: ").append(trimmed.substringAfter(':')).append('\n')
                trimmed.startsWith("DTEND", ignoreCase = true) -> sb.append("Ends: ").append(trimmed.substringAfter(':')).append('\n')
                trimmed.startsWith("LOCATION:", ignoreCase = true) -> sb.append("Location: ").append(trimmed.substring(9)).append('\n')
                trimmed.startsWith("DESCRIPTION:", ignoreCase = true) -> sb.append("Description: ").append(trimmed.substring(12)).append('\n')
                trimmed.startsWith("ORGANIZER", ignoreCase = true) -> sb.append("Organizer: ").append(trimmed.substringAfter(':')).append('\n')
            }
        }
        return sb.toString()
    }

    private fun cleanWhitespace(text: String): String = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(Regex("[ \\t]+\\n"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
