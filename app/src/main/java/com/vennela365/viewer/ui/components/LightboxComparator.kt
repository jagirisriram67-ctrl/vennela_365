package com.vennela365.viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.CatRose
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.aeroGlass
import kotlin.math.roundToInt

enum class DiffLineType { UNCHANGED, ADDED, REMOVED }

@Immutable
data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val lineNumber: Int
)

enum class LightboxMode(val label: String, val icon: ImageVector) {
    CURTAIN_WIPER("Curtain Wiper", Icons.Default.ViewColumn),
    ONION_SKIN("Onion Skin", Icons.Default.Layers),
    DELTA_DIFF("Delta Diff", Icons.Default.Difference)
}

object LightboxDiffEngine {

    /**
     * Computes line-by-line diff using Longest Common Subsequence (LCS)
     * so that line insertions/deletions do not cascade-shift all subsequent lines.
     */
    fun computeDiff(original: String, modified: String): List<DiffLine> {
        val a = original.lines()
        val b = modified.lines()

        val n = a.size
        val m = b.size

        // DP table for LCS
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (a[i] == b[j]) {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        // Backtrack to build diff
        val result = mutableListOf<DiffLine>()
        var i = n
        var j = m

        val reversedDiff = mutableListOf<DiffLine>()
        var lineNum = 1

        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && a[i - 1] == b[j - 1] -> {
                    reversedDiff.add(DiffLine(DiffLineType.UNCHANGED, "  ${a[i - 1]}", 0))
                    i--
                    j--
                }
                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                    reversedDiff.add(DiffLine(DiffLineType.ADDED, "+ ${b[j - 1]}", 0))
                    j--
                }
                i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j]) -> {
                    reversedDiff.add(DiffLine(DiffLineType.REMOVED, "- ${a[i - 1]}", 0))
                    i--
                }
            }
        }

        // Restore original order and assign sequential line numbers
        reversedDiff.reversed().forEachIndexed { index, line ->
            result.add(line.copy(lineNumber = index + 1))
        }

        return result
    }
}

/**
 * Lightbox Ghost-Overlay Diffing Component.
 *
 * Provides:
 * 1. Curtain Wiper split slider with zero text reflow distortion (both layers full width)
 * 2. Onion Skinning with dynamic opacity
 * 3. LCS-based Neon Delta Diffing
 */
@Composable
fun LightboxComparator(
    originalText: String,
    modifiedText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var activeMode by remember { mutableStateOf(LightboxMode.DELTA_DIFF) }
    var wiperRatio by remember { mutableFloatStateOf(0.5f) } // 0.0 to 1.0
    var onionSkinAlpha by remember { mutableFloatStateOf(0.5f) }

    val diffLines = remember(originalText, modifiedText) {
        LightboxDiffEngine.computeDiff(originalText, modifiedText)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (activeMode) {
            LightboxMode.DELTA_DIFF -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 70.dp)
                ) {
                    items(diffLines) { line ->
                        val (textColor, lineBg) = when (line.type) {
                            DiffLineType.ADDED -> accent.secondary to accent.primary.copy(alpha = 0.18f)
                            DiffLineType.REMOVED -> CatRose to CatRose.copy(alpha = 0.16f)
                            DiffLineType.UNCHANGED -> palette.textSecondary to Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(lineBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${line.lineNumber}".padStart(4, ' '),
                                color = palette.textTertiary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = line.text,
                                color = textColor,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (line.type == DiffLineType.UNCHANGED) FontWeight.Normal else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            LightboxMode.ONION_SKIN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 70.dp)
                ) {
                    // Layer A (Original)
                    Text(
                        text = originalText,
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(1f - onionSkinAlpha)
                    )

                    // Layer B (Modified)
                    Text(
                        text = modifiedText,
                        color = accent.secondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(onionSkinAlpha)
                    )
                }
            }

            LightboxMode.CURTAIN_WIPER -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 70.dp, bottom = 40.dp)
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val wiperX = (wiperRatio * widthPx).coerceIn(0f, widthPx)

                    // Left View (Original): Clipped to [0 .. wiperX] but rendered at full width
                    val leftClipShape = remember(wiperX) {
                        GenericShape { size, _ ->
                            addRect(Rect(0f, 0f, wiperX, size.height))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(leftClipShape)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = originalText,
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Right View (Modified): Clipped to [wiperX .. width] rendered at full width
                    val rightClipShape = remember(wiperX, widthPx) {
                        GenericShape { size, _ ->
                            addRect(Rect(wiperX, 0f, size.width, size.height))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(rightClipShape)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = modifiedText,
                            color = accent.secondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Draggable Wiper Divider Line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .offset { IntOffset(wiperX.roundToInt() - 15, 0) }
                            .width(30.dp)
                            .pointerInput(widthPx) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (widthPx > 0f) {
                                        wiperRatio = (wiperRatio + dragAmount / widthPx).coerceIn(0.05f, 0.95f)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.5.dp)
                                .background(accent.secondary)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurfaceStrong)
                                .border(1.5.dp, accent.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewColumn,
                                contentDescription = null,
                                tint = accent.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Glass Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .aeroGlass(shape = RoundedCornerShape(24.dp), elevation = 12.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LightboxMode.entries.forEach { mode ->
                    val isSelected = activeMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent.primary.copy(alpha = 0.35f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.label,
                                tint = if (isSelected) accent.secondary else palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.label,
                                color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Lightbox",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Controls for Onion Skin
        if (activeMode == LightboxMode.ONION_SKIN) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 32.dp, end = 32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .aeroGlass(shape = RoundedCornerShape(20.dp), elevation = 10.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Original", color = palette.textSecondary, fontSize = 11.sp)
                    Slider(
                        value = onionSkinAlpha,
                        onValueChange = { onionSkinAlpha = it },
                        modifier = Modifier.width(180.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = accent.secondary,
                            activeTrackColor = accent.primary
                        )
                    )
                    Text("Modified", color = accent.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
