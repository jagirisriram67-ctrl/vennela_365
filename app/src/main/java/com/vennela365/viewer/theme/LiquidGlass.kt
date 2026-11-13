package com.vennela365.viewer.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple iOS 26 / visionOS Liquid Glass specification.
 *
 * Models optical refraction, dual-stop translucent lens fill, diagonal
 * specular rim lighting, and inner top/bottom caustic reflections.
 */
@Immutable
data class LiquidGlassSpec(
    val tint: Color,
    val tintEnd: Color = tint,
    val frost: Dp = VennelaTokens.frostRegular,
    val backdropAlpha: Float = 0.60f,
    val rim: Color = Color.White.copy(alpha = 0.24f),
    val rimWidth: Dp = 1.dp,
    val sheen: Float = 0.20f,
    val elevation: Dp = VennelaTokens.elevationCard,
    val shadowColor: Color = Color(0x55000000),
    val highlight: Float = 0.16f
)

/**
 * Builds a [LiquidGlassSpec] tuned to the active [VennelaPalette] and
 * [AccentOption] with authentic Apple Liquid Glass optical defaults.
 */
@Composable
fun liquidGlassSpec(
    alpha: Float = 0.56f,
    frost: Dp = VennelaTokens.frostRegular,
    elevation: Dp = VennelaTokens.elevationCard,
    sheen: Float = 0.20f,
    highlight: Float = 0.16f,
    strong: Boolean = false,
    accentRim: Boolean = false
): LiquidGlassSpec {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val baseAlpha = if (strong) (alpha + 0.14f) else alpha
    val resolvedAlpha = if (palette.isDark) {
        baseAlpha.coerceIn(0.18f, 0.90f)
    } else {
        (baseAlpha + 0.42f).coerceIn(0.82f, 0.97f)
    }
    val startSurface = if (palette.isDark) {
        Color(
            red = (palette.cardSurfaceStrong.red * 0.88f + 0.12f).coerceIn(0f, 1f),
            green = (palette.cardSurfaceStrong.green * 0.88f + 0.12f).coerceIn(0f, 1f),
            blue = (palette.cardSurfaceStrong.blue * 0.88f + 0.12f).coerceIn(0f, 1f),
            alpha = resolvedAlpha
        )
    } else {
        Color.White.copy(alpha = resolvedAlpha)
    }
    val endSurface = if (palette.isDark) {
        palette.cardSurface.copy(
            alpha = (resolvedAlpha * 0.82f).coerceIn(0.14f, 0.90f)
        )
    } else {
        palette.cardSurfaceStrong.copy(
            alpha = (resolvedAlpha * 0.95f).coerceIn(0.80f, 0.96f)
        )
    }
    val rimColor = when {
        accentRim -> accent.secondary.copy(alpha = if (palette.isDark) 0.45f else 0.38f)
        palette.isDark -> Color.White.copy(alpha = 0.22f)
        else -> accent.primary.copy(alpha = 0.24f)
    }
    val shadow = if (palette.isDark) {
        Color.Black.copy(alpha = 0.46f)
    } else {
        accent.primary.copy(alpha = 0.10f)
    }
    val resolvedElevation = if (palette.isDark) {
        elevation
    } else {
        (elevation * 0.35f).coerceAtMost(4.dp)
    }
    return LiquidGlassSpec(
        tint = startSurface,
        tintEnd = endSurface,
        frost = frost,
        backdropAlpha = resolvedAlpha,
        rim = rimColor,
        rimWidth = 1.dp,
        sheen = sheen,
        elevation = resolvedElevation,
        shadowColor = shadow,
        highlight = highlight
    )
}

/**
 * Applies the Apple iOS 26 Liquid Glass finish to any composable:
 *
 * 1. Soft optical drop shadow for floating depth.
 * 2. Translucent multi-stop refractive lens fill (`tint` -> `tintEnd`).
 * 3. Top inner optical lens curvature highlight + subtle bottom bounce reflection.
 * 4. Diagonal specular rim border (bright top-left white highlight transitioning
 *    through `spec.rim` to a soft bottom-right edge reflection).
 */
fun Modifier.liquidGlass(
    spec: LiquidGlassSpec,
    shape: Shape = RoundedCornerShape(VennelaTokens.radiusCard)
): Modifier {
    val fillBrush = Brush.verticalGradient(
        0.0f to spec.tint,
        0.55f to spec.tintEnd,
        1.0f to spec.tintEnd.copy(alpha = (spec.tintEnd.alpha * 0.94f).coerceIn(0f, 1f))
    )
    val topRightCornerHighlight = Color.White.copy(
        alpha = (spec.sheen * 1.35f).coerceIn(0.18f, 0.68f)
    )
    val bottomEdgeRim = spec.rim.copy(
        alpha = (spec.rim.alpha * 0.42f).coerceIn(0.06f, 0.36f)
    )
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            topRightCornerHighlight,
            spec.rim,
            bottomEdgeRim,
            Color.White.copy(alpha = (spec.sheen * 0.55f).coerceIn(0.08f, 0.28f))
        ),
        start = Offset.Zero,
        end = Offset(900f, 900f)
    )
    val shadowModifier = if (spec.elevation > 0.dp) {
        Modifier.shadow(
            elevation = spec.elevation,
            shape = shape,
            clip = false,
            ambientColor = spec.shadowColor,
            spotColor = spec.shadowColor
        )
    } else {
        Modifier
    }
    return this
        .then(shadowModifier)
        .clip(shape)
        .background(fillBrush, shape)
        .drawWithContent {
            drawLiquidGlassFinish(spec)
        }
        .border(width = spec.rimWidth, brush = rimBrush, shape = shape)
}

private fun ContentDrawScope.drawLiquidGlassFinish(spec: LiquidGlassSpec) {
    // 1. Top refractive lens sheen across the upper 38% of the glass surface.
    if (spec.sheen > 0f && size.height > 0f && size.width > 0f) {
        val sheenHeight = (size.height * 0.40f).coerceAtLeast(1f)
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = (spec.sheen * 0.85f).coerceIn(0f, 0.38f)),
                0.45f to Color.White.copy(alpha = (spec.sheen * 0.25f).coerceIn(0f, 0.14f)),
                1.0f to Color.Transparent,
                startY = 0f,
                endY = sheenHeight
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, sheenHeight)
        )
    }

    // 2. Crisp top-edge specular caustic line (simulates light hitting curved glass).
    if (spec.highlight > 0f && size.width > 0f) {
        val highlightHeight = 1.5.dp.toPx().coerceAtmost(size.height)
        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to Color.White.copy(alpha = (spec.highlight * 0.35f).coerceIn(0f, 0.25f)),
                0.25f to Color.White.copy(alpha = (spec.highlight * 1.35f).coerceIn(0f, 0.52f)),
                0.75f to Color.White.copy(alpha = (spec.highlight * 1.10f).coerceIn(0f, 0.44f)),
                1.0f to Color.White.copy(alpha = (spec.highlight * 0.30f).coerceIn(0f, 0.20f))
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, highlightHeight)
        )

        // 3. Subtle bottom inner bounce light for authentic 3D glass thickness.
        val bounceHeight = (size.height * 0.18f).coerceAtLeast(1f)
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color.Transparent,
                1.0f to Color.White.copy(alpha = (spec.highlight * 0.22f).coerceIn(0f, 0.10f)),
                startY = size.height - bounceHeight,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - bounceHeight),
            size = Size(size.width, bounceHeight)
        )
    }

    drawContent()
}

private fun Float.coerceAtmost(maximumValue: Float): Float =
    if (this > maximumValue) maximumValue else this

/**
 * Applies a hardware RenderEffect blur to the composable itself on Android 12+
 * (API 31+), degrading to a no-op on older API levels where the translucent
 * gradient + specular rim in [liquidGlass] carries the frosted appearance.
 */
fun Modifier.frostedSelfBlur(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.dp) {
        this.blur(radius = radius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        this
    }

/**
 * Draws the ambient liquid-glass atmosphere behind a full-screen page: the
 * vertical sky gradient plus two soft radial colour blooms driven by the
 * active [AccentOption] and [VennelaPalette].
 */
@Composable
fun Modifier.liquidAtmosphere(): Modifier {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    return this
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    palette.backgroundTop,
                    palette.backgroundMid,
                    palette.backgroundBottom
                )
            )
        )
        .drawWithContent {
            val w = size.width
            val h = size.height
            if (w > 0f && h > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.primary.copy(alpha = if (palette.isDark) 0.16f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.10f),
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = Offset(w * 0.85f, h * 0.10f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.secondary.copy(alpha = if (palette.isDark) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.15f, h * 0.88f),
                        radius = w * 0.78f
                    ),
                    radius = w * 0.78f,
                    center = Offset(w * 0.15f, h * 0.88f)
                )
            }
            drawContent()
        }
}
