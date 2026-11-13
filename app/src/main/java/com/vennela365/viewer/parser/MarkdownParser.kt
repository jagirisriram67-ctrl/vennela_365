package com.vennela365.viewer.parser

/** A run of inline Markdown with the emphasis that applies to it. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strike: Boolean = false,
    val link: String? = null
)

/** Block-level Markdown. Deliberately small: headings, prose, lists, code, quotes, tables. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Bullet(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Ordered(val number: String, val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Code(val language: String?, val code: String) : MdBlock
    data class Quote(val spans: List<MdSpan>) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

/**
 * A pragmatic CommonMark subset renderer. It never throws: anything it cannot
 * interpret is emitted as plain prose, so a malformed file still reads.
 */
object MarkdownParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val orderedRegex = Regex("^(\\s*)(\\d{1,9})[.)]\\s+(.*)$")
    private val fenceRegex = Regex("^\\s*(```+|~~~+)\\s*([A-Za-z0-9_+-]*)\\s*$")
    private val tableDividerRegex = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

    fun parse(source: String): List<MdBlock> {
        if (source.isBlank()) return emptyList()
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = mutableListOf<MdBlock>()
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) {
                blocks += MdBlock.Paragraph(parseInline(paragraph.toString().trim()))
                paragraph.setLength(0)
            }
        }

        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trimEnd()

            // Fenced code
            val fence = fenceRegex.find(line)
            if (fence != null) {
                flushParagraph()
                val marker = fence.groupValues[1].take(3)
                val language = fence.groupValues[2].ifBlank { null }
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(marker)) {
                    code.appendLine(lines[i])
                    i++
                }
                blocks += MdBlock.Code(language, code.toString().trimEnd('\n'))
                i++
                continue
            }

            // Blank line closes a paragraph
            if (line.isBlank()) {
                flushParagraph()
                i++
                continue
            }

            // Horizontal rule (before bullet so "---" isn't a list item)
            val trimmed = line.trim()
            if (trimmed.length >= 3 && trimmed.all { it == '-' } ||
                trimmed.length >= 3 && trimmed.all { it == '*' } ||
                trimmed.length >= 3 && trimmed.all { it == '_' }
            ) {
                flushParagraph()
                blocks += MdBlock.Rule
                i++
                continue
            }

            // ATX heading
            val heading = headingRegex.find(trimmed)
            if (heading != null) {
                flushParagraph()
                val level = heading.groupValues[1].length
                blocks += MdBlock.Heading(level, heading.groupValues[2].trim().trimEnd('#'))
                i++
                continue
            }

            // Table: header row + divider row
            if (line.contains('|') && i + 1 < lines.size && tableDividerRegex.matches(lines[i + 1])) {
                flushParagraph()
                val header = splitTableRow(line)
                val rows = mutableListOf<List<String>>()
                i += 2
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows += splitTableRow(lines[i])
                    i++
                }
                blocks += MdBlock.Table(header, rows)
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                flushParagraph()
                val quote = StringBuilder()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quote.append(lines[i].trim().removePrefix(">").trim()).append(' ')
                    i++
                }
                blocks += MdBlock.Quote(parseInline(quote.toString().trim()))
                continue
            }

            // Bullet list
            val bullet = bulletRegex.find(raw)
            if (bullet != null) {
                flushParagraph()
                val indent = bullet.groupValues[1].length / 2
                blocks += MdBlock.Bullet(indent, parseInline(bullet.groupValues[2].trim()))
                i++
                continue
            }

            // Ordered list
            val ordered = orderedRegex.find(raw)
            if (ordered != null) {
                flushParagraph()
                val indent = ordered.groupValues[1].length / 2
                blocks += MdBlock.Ordered(
                    ordered.groupValues[2],
                    indent,
                    parseInline(ordered.groupValues[3].trim())
                )
                i++
                continue
            }

            // Setext heading (text followed by === or ---)
            if (i + 1 < lines.size) {
                val underline = lines[i + 1].trim()
                if (underline.length >= 2 && underline.all { it == '=' }) {
                    flushParagraph()
                    blocks += MdBlock.Heading(1, trimmed)
                    i += 2
                    continue
                }
                if (underline.length >= 2 && underline.all { it == '-' } && trimmed.isNotEmpty()) {
                    flushParagraph()
                    blocks += MdBlock.Heading(2, trimmed)
                    i += 2
                    continue
                }
            }

            // Prose
            if (paragraph.isNotEmpty()) paragraph.append(' ')
            paragraph.append(trimmed)
            i++
        }
        flushParagraph()
        return blocks
    }

    private fun splitTableRow(line: String): List<String> =
        line.trim().trim('|').split('|').map { it.trim() }

    fun parseInline(text: String): List<MdSpan> {
        if (text.isEmpty()) return emptyList()
        val spans = mutableListOf<MdSpan>()
        val buffer = StringBuilder()

        fun flush() {
            if (buffer.isNotEmpty()) {
                spans += MdSpan(buffer.toString())
                buffer.setLength(0)
            }
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        flush()
                        spans += MdSpan(text.substring(i + 1, end), code = true)
                        i = end + 1
                    } else {
                        buffer.append(c); i++
                    }
                }

                text.startsWith("**", i) || text.startsWith("__", i) -> {
                    val marker = text.substring(i, i + 2)
                    val end = text.indexOf(marker, i + 2)
                    if (end > i + 2) {
                        flush()
                        spans += MdSpan(text.substring(i + 2, end), bold = true)
                        i = end + 2
                    } else {
                        buffer.append(c); i++
                    }
                }

                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i + 2) {
                        flush()
                        spans += MdSpan(text.substring(i + 2, end), strike = true)
                        i = end + 2
                    } else {
                        buffer.append(c); i++
                    }
                }

                (c == '*' || c == '_') -> {
                    val previous = if (i == 0) ' ' else text[i - 1]
                    val next = if (i + 1 < text.length) text[i + 1] else ' '
                    val canOpen = (previous.isWhitespace() || previous in "([") && !next.isWhitespace()
                    val end = if (canOpen) text.indexOf(c, i + 1) else -1
                    if (canOpen && end > i + 1) {
                        flush()
                        spans += MdSpan(text.substring(i + 1, end), italic = true)
                        i = end + 1
                    } else {
                        buffer.append(c); i++
                    }
                }

                c == '[' -> {
                    val close = text.indexOf(']', i + 1)
                    val openParen = if (close > 0) text.indexOf('(', close) else -1
                    val closeParen = if (openParen > 0) text.indexOf(')', openParen) else -1
                    if (close > 0 && openParen == close + 1 && closeParen > openParen) {
                        flush()
                        spans += MdSpan(
                            text = text.substring(i + 1, close),
                            link = text.substring(openParen + 1, closeParen)
                        )
                        i = closeParen + 1
                    } else {
                        buffer.append(c); i++
                    }
                }

                else -> {
                    buffer.append(c); i++
                }
            }
        }
        flush()
        return spans
    }
}
