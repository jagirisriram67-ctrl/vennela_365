package com.vennela365.viewer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.CatEmerald
import com.vennela365.viewer.theme.CatViolet
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.aeroGlass

@Immutable
data class StudyCard(
    val sectionTitle: String,
    val keyPoints: List<String>,
    val badgeLabel: String? = null
)

@Immutable
data class StudySheet(
    val docTitle: String,
    val estimatedReadMinutes: Int,
    val cards: List<StudyCard>,
    val totalTakeaways: Int
)

object StudySheetCompilerEngine {

    private val HEADING_REGEX = Regex("^(?:#{1,4}\\s+|Slide\\s+\\d+:?\\s*|Chapter\\s+\\d+:?\\s*|Section\\s+\\d+(?:\\.\\d+)*:?\\s*|\\d+[.)]\\s+)(.+)$", RegexOption.MULTILINE)
    private val BULLET_REGEX = Regex("^(?:[-*•–—]|\\d+[.)]|[a-zA-Z][.)])\\s+(.+)$")
    private val DEFINITION_PATTERNS = listOf("is defined as", "refers to", "means", "consists of", "known as", "responsible for")
    private val METRIC_REGEX = Regex("[$€£₹%]|\\b\\d+(?:\\.\\d+)?\\s*(?:MB|GB|ms|kg|km|percent|years|days|users|bytes)\\b")

    /**
     * Compiles raw document text into an executive study cram sheet.
     * Works on any document (academic, financial, legal, technical, medical).
     */
    fun compile(rawText: String, title: String): StudySheet {
        val lines = rawText.lines()
        val cards = mutableListOf<StudyCard>()

        var currentSection = "Executive Summary"
        var currentPoints = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // 1. Heading detection (Markdown, Numbered, Title Case, or Suffix Colon)
            val headingMatch = HEADING_REGEX.find(trimmed)
            val isUpperCaseHeader = trimmed.length in 4..45 && trimmed.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() }
            val isColonHeader = trimmed.endsWith(":") && trimmed.length in 4..50 && !trimmed.contains(".")

            if (headingMatch != null || isUpperCaseHeader || isColonHeader) {
                if (currentPoints.isNotEmpty()) {
                    cards.add(
                        StudyCard(
                            sectionTitle = currentSection,
                            keyPoints = currentPoints.take(6),
                            badgeLabel = "${currentPoints.size} insights"
                        )
                    )
                    currentPoints = mutableListOf()
                }
                currentSection = headingMatch?.groupValues?.get(1)?.trim() ?: trimmed.removeSuffix(":")
                continue
            }

            // 2. Point extraction (Bullets, Metrics, Definitions, Key Statements)
            val bulletMatch = BULLET_REGEX.find(trimmed)
            val hasMetric = METRIC_REGEX.containsMatchIn(trimmed)
            val hasDefinition = DEFINITION_PATTERNS.any { trimmed.contains(it, ignoreCase = true) }
            val isSignificantSentence = trimmed.length in 15..240 && (hasMetric || hasDefinition || bulletMatch != null)

            if (isSignificantSentence) {
                val cleanPoint = bulletMatch?.groupValues?.get(1)?.trim() ?: trimmed
                if (cleanPoint !in currentPoints) {
                    currentPoints.add(cleanPoint)
                }
            }
        }

        if (currentPoints.isNotEmpty()) {
            cards.add(
                StudyCard(
                    sectionTitle = currentSection,
                    keyPoints = currentPoints.take(6),
                    badgeLabel = "${currentPoints.size} insights"
                )
            )
        }

        // Fallback for compact or unstructured text
        if (cards.isEmpty()) {
            val meaningfulLines = lines.filter { it.trim().length in 12..200 }.take(5)
            cards.add(
                StudyCard(
                    sectionTitle = "Key Highlights",
                    keyPoints = if (meaningfulLines.isNotEmpty()) meaningfulLines else listOf("Document loaded cleanly with zero latency."),
                    badgeLabel = "Summary"
                )
            )
        }

        val totalPoints = cards.sumOf { it.keyPoints.size }
        val readTime = maxOf(1, (totalPoints + 3) / 4)

        return StudySheet(
            docTitle = title,
            estimatedReadMinutes = readTime,
            cards = cards,
            totalTakeaways = totalPoints
        )
    }
}

/**
 * 2-Column Luxury Apple-Inspired Study Sheet Dialog/Screen.
 */
@Composable
fun StudySheetCompilerView(
    rawText: String,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val sheet = remember(rawText, title) {
        StudySheetCompilerEngine.compile(rawText, title)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.backgroundBottom.copy(alpha = 0.95f))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Span 2
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .aeroGlass(shape = RoundedCornerShape(24.dp), elevation = 12.dp)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CatViolet.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CatViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Executive Cram Sheet",
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${sheet.docTitle} • ${sheet.totalTakeaways} takeaways • ~${sheet.estimatedReadMinutes} min read",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Cards Grid
            items(sheet.cards) { card ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .aeroGlass(shape = RoundedCornerShape(20.dp), elevation = 8.dp)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.sectionTitle,
                                color = accent.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (card.badgeLabel != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accent.primary.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = card.badgeLabel,
                                        color = accent.secondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        card.keyPoints.forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CatEmerald,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = point,
                                    color = palette.textPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Copy cram sheet button: Span 2
            item(span = { GridItemSpan(2) }) {
                ElevatedButton(
                    onClick = {
                        val fullSummary = buildString {
                            appendLine("=== STUDY CRAM SHEET: ${sheet.docTitle} ===")
                            sheet.cards.forEach { c ->
                                appendLine("\n[ ${c.sectionTitle} ]")
                                c.keyPoints.forEach { p -> appendLine("• $p") }
                            }
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Study Sheet", fullSummary))
                        Toast.makeText(context, "Copied Study Sheet to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = accent.primary.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Entire Study Sheet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
