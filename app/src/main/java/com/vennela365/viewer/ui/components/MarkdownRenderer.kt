package com.vennela365.viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.MarkdownParser
import com.vennela365.viewer.parser.MdBlock
import com.vennela365.viewer.parser.MdSpan
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts

/**
 * Tables compose as a single unit, so an enormous one would block a frame.
 * These caps keep scrolling smooth; the source view still shows every row.
 */
private const val MaxTableRows = 120
private const val MaxTableColumns = 8

/**
 * Renders real Markdown blocks. Colours default to the active theme so the same
 * renderer works on the dark glass surface and on the paper preview card.
 */
@Composable
fun MarkdownBody(
    blocks: List<MdBlock>,
    modifier: Modifier = Modifier,
    textColor: Color = LocalVennelaPalette.current.textPrimary,
    mutedColor: Color = LocalVennelaPalette.current.textSecondary,
    codeBackground: Color = Color(0x14FFFFFF),
    headingColor: Color = textColor
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            MarkdownBlockView(
                block = block,
                textColor = textColor,
                mutedColor = mutedColor,
                codeBackground = codeBackground,
                headingColor = headingColor
            )
        }
    }
}

/**
 * Renders one block.
 *
 * Readers that virtualise their page call this per item instead of handing
 * over a list, so no list is allocated on every recomposition while scrolling.
 */
@Composable
fun MarkdownBlockView(
    block: MdBlock,
    modifier: Modifier = Modifier,
    textColor: Color = LocalVennelaPalette.current.textPrimary,
    mutedColor: Color = LocalVennelaPalette.current.textSecondary,
    codeBackground: Color = Color(0x14FFFFFF),
    headingColor: Color = textColor
) {
    val accent = LocalVennelaAccent.current
    val linkColor = accent.secondary

    when (block) {
        is MdBlock.Heading -> Text(
            text = inline(block.text, textColor, linkColor),
            color = headingColor,
            fontFamily = if (block.level <= 2) VennelaFonts.Serif else VennelaFonts.Sans,
            fontSize = when (block.level) {
                1 -> 28.sp
                2 -> 23.sp
                3 -> 19.sp
                4 -> 17.sp
                else -> 16.sp
            },
            fontWeight = if (block.level <= 2) FontWeight.Bold else FontWeight.SemiBold,
            lineHeight = when (block.level) {
                1 -> 34.sp
                2 -> 29.sp
                else -> 24.sp
            },
            modifier = modifier.padding(top = if (block.level <= 2) 10.dp else 4.dp)
        )

        is MdBlock.Paragraph -> Text(
            text = inline(block.spans, textColor, linkColor),
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            modifier = modifier
        )

        is MdBlock.Bullet -> Row(
            modifier = modifier.padding(start = (block.level * 18).dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("•", color = accent.secondary, fontSize = 15.sp, modifier = Modifier.padding(end = 9.dp))
            Text(
                text = inline(block.spans, textColor, linkColor),
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )
        }

        is MdBlock.Ordered -> Row(
            modifier = modifier.padding(start = (block.level * 18).dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "${block.number}.",
                color = accent.secondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 9.dp)
            )
            Text(
                text = inline(block.spans, textColor, linkColor),
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )
        }

        is MdBlock.Code -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(codeBackground)
                .border(1.dp, mutedColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            block.language?.let {
                Text(
                    text = it.uppercase(),
                    color = accent.secondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = block.code,
                color = textColor,
                fontFamily = VennelaFonts.Mono,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }

        is MdBlock.Quote -> Row(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 20.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = inline(block.spans, textColor, linkColor),
                color = mutedColor,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                fontStyle = FontStyle.Italic
            )
        }

        is MdBlock.Table -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, mutedColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.primary.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                block.header.forEach { cell ->
                    Text(
                        text = cell,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            block.rows.take(MaxTableRows).forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = mutedColor.copy(alpha = 0.18f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    row.take(MaxTableColumns).forEach { cell ->
                        Text(
                            text = cell,
                            color = textColor,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            // A table is composed as a single unit, so an enormous one would
            // freeze the frame. Say so plainly instead of silently cutting.
            val hidden = block.rows.size - MaxTableRows
            if (hidden > 0) {
                Text(
                    text = "+$hidden more rows — open the source view to read them all",
                    color = mutedColor,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        MdBlock.Rule -> HorizontalDivider(color = mutedColor.copy(alpha = 0.35f))
    }
}

@Composable
private fun inline(spans: List<MdSpan>, textColor: Color, linkColor: Color): AnnotatedString {
    val palette = LocalVennelaPalette.current
    // Rebuilding the annotated string on every recomposition meant the reader
    // re-tokenised each visible block during every scroll frame, which is what
    // made long documents feel stuck. The spans are immutable, so cache them.
    return remember(spans, textColor, linkColor, palette.textPrimary) {
        buildAnnotatedString {
            spans.forEach { span ->
                pushStyle(
                    SpanStyle(
                        color = when {
                            span.link != null -> linkColor
                            span.code -> palette.textPrimary
                            else -> textColor
                        },
                        fontWeight = if (span.bold) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = when {
                            span.link != null -> TextDecoration.Underline
                            span.strike -> TextDecoration.LineThrough
                            else -> null
                        },
                        fontFamily = if (span.code) VennelaFonts.Mono else null,
                        background = if (span.code) textColor.copy(alpha = 0.10f) else Color.Unspecified,
                        fontSize = if (span.code) 13.sp else TextUnit.Unspecified
                    )
                )
                append(span.text)
                pop()
            }
        }
    }
}

@Composable
private fun inline(text: String, textColor: Color, linkColor: Color): AnnotatedString {
    val spans = remember(text) { MarkdownParser.parseInline(text) }
    return inline(spans, textColor, linkColor)
}
