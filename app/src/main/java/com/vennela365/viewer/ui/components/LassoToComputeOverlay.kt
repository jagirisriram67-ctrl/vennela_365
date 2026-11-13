package com.vennela365.viewer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.CatEmerald
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.aeroGlass
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Immutable
data class LassoStats(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val min: Double,
    val max: Double,
    val median: Double,
    val numbers: List<Double>
)

object LassoAnalyticsEngine {

    private val ISO_DATE_REGEX = Regex("\\b\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b")
    private val NUMBER_REGEX = Regex("[-+]?\\b\\d+(?:,\\d{3})*(?:\\.\\d+)?\\b")

    /**
     * Extracts numerical values from raw text lines, filtering out dates.
     */
    fun extractNumbers(text: String): List<Double> {
        // Strip ISO dates first to avoid splitting dates into negative numbers
        val dateScrubbed = ISO_DATE_REGEX.replace(text, " ")

        return NUMBER_REGEX.findAll(dateScrubbed).mapNotNull { match ->
            val cleanStr = match.value.replace(",", "")
            cleanStr.toDoubleOrNull()
        }.toList()
    }

    /**
     * Extracts numbers strictly within the lines spanned by the normalized vertical bounding box.
     */
    fun extractNumbersInRegion(
        allLines: List<String>,
        topRatio: Float,
        bottomRatio: Float
    ): List<Double> {
        if (allLines.isEmpty()) return emptyList()

        val startLine = (topRatio * allLines.size).toInt().coerceIn(0, allLines.size - 1)
        val endLine = (bottomRatio * allLines.size).toInt().coerceIn(startLine, allLines.size - 1)

        val selectedText = allLines.subList(startLine, endLine + 1).joinToString("\n")
        return extractNumbers(selectedText)
    }

    /**
     * Computes statistical aggregates for extracted numbers.
     */
    fun computeStats(numbers: List<Double>): LassoStats? {
        if (numbers.isEmpty()) return null
        val count = numbers.size
        val sum = numbers.sum()
        val mean = sum / count
        val sorted = numbers.sorted()
        val min = sorted.first()
        val max = sorted.last()
        val median = if (count % 2 == 1) {
            sorted[count / 2]
        } else {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        }
        return LassoStats(
            count = count,
            sum = sum,
            mean = mean,
            min = min,
            max = max,
            median = median,
            numbers = numbers
        )
    }
}

/**
 * Lasso-to-Compute Overlay.
 *
 * User drags over any document or data area, extracting numbers within the drawn
 * bounding box and showing Sum, Mean, Min, Max, Median, and an inline sparkline.
 */
@Composable
fun BoxScope.LassoToComputeOverlay(
    isActive: Boolean,
    onClose: () -> Unit,
    sourceTextProvider: () -> String,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var canvasHeightPx by remember { mutableFloatStateOf(1f) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var computedStats by remember { mutableStateOf<LassoStats?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size.height > 0) canvasHeightPx = size.height.toFloat()
            }
            .pointerInput(canvasHeightPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragEnd = offset
                        computedStats = null
                    },
                    onDragEnd = {
                        val s = dragStart
                        val e = dragEnd
                        if (s != null && e != null && canvasHeightPx > 0f) {
                            val topY = min(s.y, e.y)
                            val bottomY = max(s.y, e.y)
                            val topRatio = (topY / canvasHeightPx).coerceIn(0f, 1f)
                            val bottomRatio = (bottomY / canvasHeightPx).coerceIn(0f, 1f)

                            val rawText = sourceTextProvider()
                            val lines = rawText.lines()

                            val extracted = if (abs(bottomY - topY) > 30f) {
                                // Real spatial selection within drawn bounding box
                                LassoAnalyticsEngine.extractNumbersInRegion(lines, topRatio, bottomRatio)
                            } else {
                                // Tap or small drag defaults to full active content
                                LassoAnalyticsEngine.extractNumbers(rawText)
                            }
                            computedStats = LassoAnalyticsEngine.computeStats(extracted)
                        }
                    },
                    onDragCancel = {
                        dragStart = null
                        dragEnd = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragEnd = change.position
                    }
                )
            }
    ) {
        // Draw active lasso rectangle with glowing neon border
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = dragStart
            val end = dragEnd
            if (start != null && end != null) {
                val left = min(start.x, end.x)
                val top = min(start.y, end.y)
                val width = max(abs(end.x - start.x), 2f)
                val height = max(abs(end.y - start.y), 2f)

                // Translucent fill
                drawRect(
                    color = accent.primary.copy(alpha = 0.15f),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )

                // Glowing border
                drawRect(
                    color = accent.secondary,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // Floating Stats HUD Card
        val stats = computedStats
        if (stats != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .aeroGlass(shape = RoundedCornerShape(22.dp), elevation = 16.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CatEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lasso Analytics (${stats.count} values)",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { computedStats = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sparkline mini chart
                    if (stats.numbers.size >= 2) {
                        SparklinePreview(
                            numbers = stats.numbers,
                            lineColor = accent.secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Stat Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetric(label = "Sum (Σ)", value = formatNumber(stats.sum))
                        StatMetric(label = "Mean (μ)", value = formatNumber(stats.mean))
                        StatMetric(label = "Median", value = formatNumber(stats.median))
                        StatMetric(label = "Range", value = "${formatNumber(stats.min)} .. ${formatNumber(stats.max)}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Copy summary button
                    ElevatedButton(
                        onClick = {
                            val summary = "Lasso Stats:\nCount: ${stats.count}\nSum: ${stats.sum}\nMean: ${stats.mean}\nMedian: ${stats.median}\nMin: ${stats.min}\nMax: ${stats.max}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Lasso Stats", summary))
                            Toast.makeText(context, "Copied statistics to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = accent.primary.copy(alpha = 0.4f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Stats Summary", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SparklinePreview(
    numbers: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val minVal = numbers.minOrNull() ?: 0.0
        val maxVal = numbers.maxOrNull() ?: 1.0
        val range = max(maxVal - minVal, 0.0001)

        val path = Path()
        val stepX = size.width / (numbers.size - 1)

        numbers.forEachIndexed { i, value ->
            val normY = 1.0 - ((value - minVal) / range)
            val x = i * stepX
            val y = (normY * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StatMetric(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Column {
        Text(text = label, color = palette.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Normal)
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatNumber(num: Double): String {
    return if (num % 1.0 == 0.0) {
        String.format(Locale.US, "%,d", num.toLong())
    } else {
        String.format(Locale.US, "%,.2f", num)
    }
}

private fun abs(a: Float): Float = kotlin.math.abs(a)
