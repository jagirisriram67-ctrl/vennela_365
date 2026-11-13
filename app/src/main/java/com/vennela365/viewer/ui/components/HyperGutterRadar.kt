package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.CatRose
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.MoonSky
import com.vennela365.viewer.theme.VennelaAccent
import com.vennela365.viewer.theme.aeroGlass
import kotlin.math.roundToInt

/**
 * Radar tick categories. Colours are resolved at render time so the SEARCH_MATCH
 * ribbon follows the active accent instead of a frozen palette constant.
 */
enum class RadarTickType {
    HEADING,
    SEARCH_MATCH,
    ERROR_ANOMALY,
    BOOKMARK;

    fun resolveColor(accent: VennelaAccent): Color = when (this) {
        HEADING -> MoonSky
        SEARCH_MATCH -> accent.secondary
        ERROR_ANOMALY -> CatRose
        BOOKMARK -> accent.primary
    }
}

@Immutable
data class RadarTick(
    val normalizedPosition: Float, // 0.0 to 1.0
    val type: RadarTickType,
    val label: String? = null
)

/**
 * 6-Pixel Hyper-Gutter Radar Ribbon.
 *
 * Implements high-speed document spatial navigation:
 * - Ultra-thin 6dp visual ribbon with system gesture exclusion to prevent OS back navigation conflicts.
 * - Interactive dragging gesture with density-independent floating magnifier badge.
 */
@Composable
fun BoxScope.HyperGutterRadar(
    ticks: List<RadarTick>,
    currentScrollProgress: Float, // 0.0 to 1.0
    onScrub: (Float) -> Unit,
    formatPageOrLineLabel: (Float) -> String = { "${(it * 100).roundToInt()}%" },
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val density = LocalDensity.current

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(currentScrollProgress) }
    var gutterHeightPx by remember { mutableFloatStateOf(1f) }

    val activeProgress = if (isScrubbing) scrubProgress else currentScrollProgress
    val hudOffsetX = with(density) { (-120).dp.roundToPx() }

    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(32.dp)
            .systemGestureExclusion() // Prevents Android OS back gesture collision
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (gutterHeightPx > 0f) {
                            isScrubbing = true
                            val newProgress = (offset.y / gutterHeightPx).coerceIn(0f, 1f)
                            scrubProgress = newProgress
                            onScrub(newProgress)
                        }
                    },
                    onDragEnd = { isScrubbing = false },
                    onDragCancel = { isScrubbing = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        if (gutterHeightPx > 0f) {
                            val newProgress = (change.position.y / gutterHeightPx).coerceIn(0f, 1f)
                            scrubProgress = newProgress
                            onScrub(newProgress)
                        }
                    }
                )
            }
            .onSizeChanged { size ->
                if (size.height > 0) {
                    gutterHeightPx = size.height.toFloat()
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        // 6-Pixel Visual Radar Ribbon
        Canvas(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .padding(vertical = 48.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.cardSurfaceStrong.copy(alpha = 0.5f))
        ) {
            val totalHeight = size.height

            // Render structural markers / ticks
            for (tick in ticks) {
                val y = tick.normalizedPosition.coerceIn(0f, 1f) * totalHeight
                drawRect(
                    color = tick.type.resolveColor(accent),
                    topLeft = Offset(0f, y - 2f),
                    size = Size(size.width, 3.5f)
                )
            }

            // Render current scroll indicator pill
            val thumbY = activeProgress.coerceIn(0f, 1f) * totalHeight
            drawRoundRect(
                color = accent.secondary,
                topLeft = Offset(-1f, thumbY - 8f),
                size = Size(size.width + 2f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }

        // Floating Magnifier HUD Pill when scrubbing (density-scaled)
        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val safeY = ((activeProgress * gutterHeightPx) - 30)
                        .roundToInt()
                        .coerceIn(40, (gutterHeightPx - 100).roundToInt().coerceAtLeast(40))
                    IntOffset(x = hudOffsetX, y = safeY)
                }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .aeroGlass(shape = RoundedCornerShape(14.dp), elevation = 10.dp)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatPageOrLineLabel(activeProgress),
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
