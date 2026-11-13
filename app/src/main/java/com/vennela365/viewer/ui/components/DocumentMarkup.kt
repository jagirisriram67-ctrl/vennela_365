package com.vennela365.viewer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.VennelaFonts

/** One search hit, addressed in the *source* text that was passed to [DocumentMarkup]. */
data class DocumentHighlight(val start: Int, val end: Int, val primary: Boolean)

/**
 * Turns a document's extracted plain text into a styled, Google-Docs-like page.
 *
 * Office and ebook extractors hand back text with only newlines to preserve
 * structure. Rendering that as one flat block is what makes a viewer feel like a
 * log file rather than a document. This builder recovers the structure that is
 * genuinely present in the text — markdown headings, ALL-CAPS headings, bulleted
 * and numbered lists, block quotes, pipe tables, slide markers and inline
 * emphasis — and maps every run back to its source offsets so search highlights
 * still land on the right characters after markers are stripped.
 *
 * It never invents content and never changes the text: only presentation.
 */
object DocumentMarkup {

    /** Paper-aware typography for the generated page. */
    data class Typography(
        val bodySize: TextUnit,
        val ink: Color,
        val muted: Color,
        val accent: Color,
        val tableTint: Color,
        val isDarkPaper: Boolean = false,
        val headingFont: FontFamily = VennelaFonts.Serif,
        val bodyFont: FontFamily = VennelaFonts.Serif,
        val monoFont: FontFamily = VennelaFonts.Mono
    )

    private const val BOLD_WEIGHT = 700

    private val hashHeading = Regex("^(#{1,6})\\s+(.*)$")
    private val slideMarker = Regex("^[─—\\-\\s]*\\[?\\s*Slide\\s+(\\d+)\\s*\\]?[─—\\-\\s]*:?$", RegexOption.IGNORE_CASE)
    private val rule = Regex("^(?:-{3,}|\\*{3,}|_{3,}|={3,})$")
    private val bullet = Regex("^([•▪◦‣·\\u2022\\u25AA\\u25E6\\u2043]|[-*+])\\s+(.*)$")
    private val numbered = Regex("^(\\d{1,3}[.)])\\s+(.*)$")
    private val quote = Regex("^>\\s?(.*)$")
    private val pageMarker = Regex("^[─—\\-\\s]*\\[?\\s*(?:Page|Slide)\\s+(\\d+)\\s*(?:of\\s+(\\d+))?\\s*\\]?[─—\\-\\s]*:?$", RegexOption.IGNORE_CASE)

    fun build(
        source: String,
        typography: Typography,
        highlights: List<DocumentHighlight> = emptyList()
    ): AnnotatedString {
        if (source.isEmpty()) return AnnotatedString("")

        val base = typography.bodySize.value
        val builder = RichBuilder()
        val ink = SpanStyle(color = typography.ink, fontFamily = typography.bodyFont)

        var cursor = 0
        while (cursor <= source.length) {
            val newline = source.indexOf('\n', cursor)
            val end = if (newline < 0) source.length else newline
            val line = source.substring(cursor, end)
            val trimmed = line.trim()
            val offsetInLine = if (trimmed.isEmpty()) 0 else line.indexOf(trimmed)
            val textStart = cursor + offsetInLine

            if (trimmed.isNotEmpty()) {
                when {
                    hashHeading.matches(trimmed) -> {
                        val match = hashHeading.find(trimmed)!!
                        val level = match.groupValues[1].length
                        val text = match.groupValues[2].trim()
                        val factor = when (level) {
                            1 -> 1.75f
                            2 -> 1.42f
                            3 -> 1.20f
                            else -> 1.08f
                        }
                        builder.blankLineIfNeeded()
                        builder.appendLine(
                            srcStart = textStart + trimmed.indexOf(text).coerceAtLeast(0),
                            text = text,
                            span = SpanStyle(
                                color = if (level <= 2) typography.ink else typography.ink.copy(alpha = 0.94f),
                                fontFamily = typography.headingFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = (base * factor).sp,
                                letterSpacing = (-0.25).sp
                            ),
                            paragraph = ParagraphStyle(lineHeight = (base * factor * 1.28f).sp)
                        )
                        builder.append(cursor + line.length, "\n\n", null)
                        cursor = end + 1
                        continue
                    }

                    pageMarker.matches(trimmed) -> {
                        builder.blankLineIfNeeded()
                        builder.appendLine(
                            srcStart = textStart,
                            text = trimmed.uppercase(),
                            span = SpanStyle(
                                color = typography.accent,
                                fontFamily = VennelaFonts.Sans,
                                fontWeight = FontWeight.Bold,
                                fontSize = (base * 0.78f).sp,
                                letterSpacing = 1.2.sp
                            ),
                            paragraph = ParagraphStyle(lineHeight = (base * 1.6f).sp)
                        )
                        builder.append(cursor + line.length, "\n\n", null)
                        cursor = end + 1
                        continue
                    }

                    slideMarker.matches(trimmed) -> {
                        builder.blankLineIfNeeded()
                        builder.appendLine(
                            srcStart = textStart,
                            text = trimmed.replace("─", "").trim(),
                            span = SpanStyle(
                                color = typography.accent,
                                fontFamily = VennelaFonts.Sans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (base * 0.82f).sp,
                                letterSpacing = 0.8.sp
                            )
                        )
                        builder.append(cursor + line.length, "\n\n", null)
                        cursor = end + 1
                        continue
                    }

                    rule.matches(trimmed) -> {
                        builder.append(cursor + line.length, "\n", null)
                        cursor = end + 1
                        continue
                    }

                    bullet.matches(trimmed) -> {
                        val match = bullet.find(trimmed)!!
                        val glyph = match.groupValues[1]
                        val body = match.groupValues[2]
                        val dot = if (glyph == "-" || glyph == "*" || glyph == "+") "•" else glyph
                        builder.append(textStart, "$dot  ", SpanStyle(color = typography.accent.copy(alpha = 0.88f), fontWeight = FontWeight.Bold))
                        builder.appendInline(
                            line = body,
                            lineStart = textStart + trimmed.indexOf(body).coerceAtLeast(0),
                            base = ink,
                            typography = typography,
                            paragraph = ParagraphStyle(
                                textIndent = TextIndent(restLine = (base * 0.85f).sp),
                                lineHeight = (base * 1.52f).sp
                            )
                        )
                    }

                    numbered.matches(trimmed) -> {
                        val match = numbered.find(trimmed)!!
                        val marker = match.groupValues[1]
                        val body = match.groupValues[2]
                        val bodyStart = textStart + trimmed.indexOf(body).coerceAtLeast(0)
                        builder.append(textStart, "$marker  ", SpanStyle(color = typography.accent.copy(alpha = 0.92f), fontFamily = typography.monoFont, fontWeight = FontWeight.SemiBold))
                        builder.appendInline(
                            line = body,
                            lineStart = bodyStart,
                            base = ink,
                            typography = typography,
                            paragraph = ParagraphStyle(
                                textIndent = TextIndent(restLine = (base * 1.2f).sp),
                                lineHeight = (base * 1.52f).sp
                            )
                        )
                    }

                    quote.matches(trimmed) -> {
                        val body = quote.find(trimmed)!!.groupValues[1]
                        val bodyStart = cursor + line.indexOf(body).coerceAtLeast(0)
                        builder.append(textStart, "\u201C", SpanStyle(color = typography.accent, fontFamily = typography.headingFont, fontWeight = FontWeight.Bold))
                        builder.appendInline(
                            line = body,
                            lineStart = bodyStart,
                            base = SpanStyle(color = typography.muted, fontFamily = typography.bodyFont, fontStyle = FontStyle.Italic),
                            typography = typography,
                            paragraph = ParagraphStyle(lineHeight = (base * 1.52f).sp)
                        )
                        builder.append(textStart, "\u201D", SpanStyle(color = typography.accent, fontFamily = typography.headingFont, fontWeight = FontWeight.Bold))
                    }

                    trimmed.count { it == '|' } >= 2 -> {
                        builder.appendLine(
                            srcStart = textStart,
                            text = trimmed,
                            span = SpanStyle(
                                color = typography.ink,
                                fontFamily = typography.monoFont,
                                fontSize = (base * 0.84f).sp,
                                background = typography.tableTint
                            ),
                            paragraph = ParagraphStyle(lineHeight = (base * 1.38f).sp)
                        )
                    }

                    isAllCapsHeading(trimmed) -> {
                        builder.blankLineIfNeeded()
                        builder.appendLine(
                            srcStart = textStart,
                            text = trimmed,
                            span = SpanStyle(
                                color = typography.ink,
                                fontFamily = typography.headingFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = (base * 1.15f).sp,
                                letterSpacing = 0.3.sp
                            ),
                            paragraph = ParagraphStyle(lineHeight = (base * 1.45f).sp)
                        )
                        builder.append(cursor + line.length, "\n", null)
                        cursor = end + 1
                        continue
                    }

                    trimmed.endsWith(":") && trimmed.length in 4..72 -> {
                        builder.appendLine(
                            srcStart = textStart,
                            text = trimmed,
                            span = SpanStyle(
                                color = typography.ink,
                                fontFamily = typography.headingFont,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    else -> builder.appendInline(
                        line = trimmed,
                        lineStart = textStart,
                        base = ink,
                        typography = typography,
                        paragraph = ParagraphStyle(lineHeight = (base * 1.52f).sp)
                    )
                }
            }

            builder.append(cursor + line.length, "\n", null)
            if (newline < 0) break
            cursor = end + 1
        }

        builder.applyHighlights(highlights, typography)
        return builder.finish()
    }

    private fun isAllCapsHeading(text: String): Boolean {
        val letters = text.count { it.isLetter() }
        if (letters !in 3..64 || text.length > 72) return false
        if (text.endsWith(".") || text.endsWith(",")) return false
        return text.all { !it.isLetter() || it.isUpperCase() }
    }

    /* --------------------------------------------------------------------- */
    /* Builder                                                               */
    /* --------------------------------------------------------------------- */

    private data class Segment(val srcStart: Int, val srcEnd: Int, val dstStart: Int, val dstEnd: Int)

    private class RichBuilder {
        private val out = AnnotatedString.Builder()
        private val segments = ArrayList<Segment>(256)
        private var trailingBlank = false

        fun append(srcStart: Int, text: String, span: SpanStyle?) {
            if (text.isEmpty()) return
            val start = out.length
            out.append(text)
            val end = out.length
            if (span != null) out.addStyle(span, start, end)
            segments.add(Segment(srcStart, srcStart + text.length, start, end))
            trailingBlank = text.endsWith("\n\n")
        }

        fun blankLineIfNeeded() {
            if (out.length > 0 && !trailingBlank) {
                out.append("\n")
                trailingBlank = true
            }
        }

        fun appendLine(
            srcStart: Int,
            text: String,
            span: SpanStyle,
            paragraph: ParagraphStyle? = null
        ) {
            val start = out.length
            out.append(text)
            val end = out.length
            out.addStyle(span, start, end)
            if (paragraph != null) out.addStyle(paragraph, start, end)
            segments.add(Segment(srcStart, srcStart + text.length, start, end))
            trailingBlank = false
        }

        /** Appends a line with inline emphasis (`**bold**`, `*italic*`, `` `code` ``). */
        fun appendInline(
            line: String,
            lineStart: Int,
            base: SpanStyle,
            typography: Typography,
            paragraph: ParagraphStyle? = null
        ) {
            var cursor = 0
            var i = 0
            var pendingParagraph = paragraph

            fun flushPlain(from: Int, to: Int) {
                if (to <= from) return
                val srcStart = lineStart + from
                val start = out.length
                out.append(line.substring(from, to))
                val end = out.length
                out.addStyle(base, start, end)
                val paragraphStyle = pendingParagraph
                if (paragraphStyle != null) {
                    out.addStyle(paragraphStyle, start, end)
                    pendingParagraph = null
                }
                segments.add(Segment(srcStart, srcStart + (to - from), start, end))
            }

            while (i < line.length) {
                val ch = line[i]
                if (ch == '\\' && i + 1 < line.length) {
                    i += 2
                    continue
                }
                val pair = when {
                    line.startsWith("**", i) -> "**"
                    line.startsWith("__", i) -> "__"
                    ch == '`' -> "`"
                    ch == '*' -> "*"
                    ch == '_' -> "_"
                    else -> null
                }
                if (pair == null) {
                    i++
                    continue
                }
                val close = line.indexOf(pair, i + pair.length)
                if (close <= i + pair.length - 1) {
                    i++
                    continue
                }
                val inner = line.substring(i + pair.length, close)
                if (inner.isBlank()) {
                    i = close + pair.length
                    continue
                }
                val extra = when (pair) {
                    "**", "__" -> SpanStyle(fontWeight = FontWeight(BOLD_WEIGHT), color = typography.ink)
                    "`" -> SpanStyle(
                        fontFamily = typography.monoFont,
                        fontSize = (typography.bodySize.value * 0.90f).sp,
                        background = typography.tableTint
                    )
                    else -> SpanStyle(fontStyle = FontStyle.Italic)
                }
                flushPlain(cursor, i)
                val srcStart = lineStart + i + pair.length
                val start = out.length
                out.append(inner)
                val end = out.length
                out.addStyle(base.merge(extra), start, end)
                val paragraphStyle = pendingParagraph
                if (paragraphStyle != null) {
                    out.addStyle(paragraphStyle, start, end)
                    pendingParagraph = null
                }
                segments.add(Segment(srcStart, srcStart + inner.length, start, end))
                cursor = close + pair.length
                i = cursor
            }
            flushPlain(cursor, line.length)
        }

        fun applyHighlights(highlights: List<DocumentHighlight>, typography: Typography) {
            if (highlights.isEmpty()) return
            for (highlight in highlights) {
                if (highlight.end <= highlight.start) continue
                for (segment in segments) {
                    val from = maxOf(highlight.start, segment.srcStart)
                    val to = minOf(highlight.end, segment.srcEnd)
                    if (from >= to) continue
                    val dstFrom = segment.dstStart + (from - segment.srcStart)
                    val dstTo = segment.dstStart + (to - segment.srcStart)
                    if (dstFrom >= dstTo) continue
                    out.addStyle(
                        SpanStyle(
                            background = if (highlight.primary) {
                                typography.accent.copy(alpha = 0.85f)
                            } else {
                                typography.accent.copy(alpha = 0.26f)
                            },
                            color = if (highlight.primary) typography.ink else Color.Unspecified
                        ),
                        dstFrom,
                        dstTo
                    )
                }
            }
        }

        fun finish(): AnnotatedString = out.toAnnotatedString()
    }
}
