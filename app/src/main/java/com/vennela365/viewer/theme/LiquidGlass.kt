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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Google Android 16 Material 3 Expressive + Frosted Glassmorphism specification.
 *
 * Combines M3 tonal surface elevation (`surfaceContainerHigh` / `primaryContainer` harmonics)
 * with translucent frosted glass fills, diagonal specular rim lighting, and top-edge illumination.
 */
@Immutable
data class LiquidGlassSpec(
    val tint: Color,
    val tintEnd: Color = tint,
    val frost: Dp = VennelaTokens.frostRegular,
    val backdropAlpha: Float = 0.64f,
    val rim: Color = Color.White.copy(alpha = 0.24f),
    val rimWidth: Dp = 1.dp,
    val sheen: Float = 0.20f,
    val elevation: Dp = VennelaTokens.elevationCard,
    val shadowColor: Color = Color(0x55000000),
    val highlight: Float = 0.16f
)

/**
 * Builds a [LiquidGlassSpec] tuned to the active [VennelaPalette] and
 * [AccentOption] using Google Android 16 Material 3 Expressive tonal glass defaults.
 */
@Composable
fun liquidGlassSpec(
    alpha: Float = 0.60f,
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
        baseAlpha.coerceIn(0.24f, 0.92f)
    } else {
        (baseAlpha + 0.40f).coerceIn(0.84f, 0.98f)
    }
    val tonalBase = lerp(
        palette.cardSurfaceStrong,
        accent.primary,
        if (palette.isDark) 0.10f else 0.05f
    )
    val startSurface = if (palette.isDark) {
        tonalBase.copy(alpha = resolvedAlpha)
    } else {
        Color.White.copy(alpha = resolvedAlpha)
    }
    val endSurface = if (palette.isDark) {
        palette.cardSurface.copy(
            alpha = (resolvedAlpha * 0.86f).coerceIn(0.18f, 0.90f)
        )
    } else {
        tonalBase.copy(
            alpha = (resolvedAlpha * 0.95f).coerceIn(0.82f, 0.97f)
        )
    }
    val rimColor = when {
        accentRim -> accent.secondary.copy(alpha = if (palette.isDark) 0.50f else 0.40f)
        palette.isDark -> lerp(Color.White, accent.primary, 0.28f).copy(alpha = 0.26f)
        else -> accent.primary.copy(alpha = 0.22f)
    }
    val shadow = if (palette.isDark) {
        Color.Black.copy(alpha = 0.48f)
    } else {
        accent.primary.copy(alpha = 0.12f)
    }
    val resolvedElevation = if (palette.isDark) {
        elevation
    } else {
        (elevation * 0.40f).coerceAtMost(5.dp)
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
 * Applies the Android 16 Material 3 Expressive Frosted Glassmorphism surface to any composable:
 *
 * 1. Tonal Material 3 elevation shadow.
 * 2. Translucent multi-stop M3 tonal glass container fill (`tint` -> `tintEnd`).
 * 3. Top frosted glass specular highlight + subtle bottom rim reflection.
 * 4. Diagonal glassmorphic border with accent-harmonized rim.
 */
fun Modifier.liquidGlass(
    spec: LiquidGlassSpec,
    shape: Shape = RoundedCornerShape(VennelaTokens.radiusCard)
): Modifier {
    val fillBrush = Brush.verticalGradient(
        0.0f to spec.tint,
        0.58f to spec.tintEnd,
        1.0f to spec.tintEnd.copy(alpha = (spec.tintEnd.alpha * 0.95f).coerceIn(0f, 1f))
    )
    val topRightCornerHighlight = Color.White.copy(
        alpha = (spec.sheen * 1.25f).coerceIn(0.16f, 0.62f)
    )
    val bottomEdgeRim = spec.rim.copy(
        alpha = (spec.rim.alpha * 0.45f).coerceIn(0.08f, 0.36f)
    )
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            topRightCornerHighlight,
            spec.rim,
            bottomEdgeRim,
            Color.White.copy(alpha = (spec.sheen * 0.50f).coerceIn(0.08f, 0.26f))
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
            drawFrostedGlassFinish(spec)
        }
        .border(width = spec.rimWidth, brush = rimBrush, shape = shape)
}

private fun ContentDrawScope.drawFrostedGlassFinish(spec: LiquidGlassSpec) {
    if (spec.sheen > 0f && size.height > 0f && size.width > 0f) {
        val sheenHeight = (size.height * 0.38f).coerceAtLeast(1f)
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = (spec.sheen * 0.78f).coerceIn(0f, 0.34f)),
                0.45f to Color.White.copy(alpha = (spec.sheen * 0.22f).coerceIn(0f, 0.12f)),
                1.0f to Color.Transparent,
                startY = 0f,
                endY = sheenHeight
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, sheenHeight)
        )
    }

    if (spec.highlight > 0f && size.width > 0f) {
        val highlightHeight = 1.25.dp.toPx().coerceAtMost(size.height)
        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to Color.White.copy(alpha = (spec.highlight * 0.32f).coerceIn(0f, 0.22f)),
                0.30f to Color.White.copy(alpha = (spec.highlight * 1.25f).coerceIn(0f, 0.48f)),
                0.70f to Color.White.copy(alpha = (spec.highlight * 1.05f).coerceIn(0f, 0.40f)),
                1.0f to Color.White.copy(alpha = (spec.highlight * 0.28f).coerceIn(0f, 0.18f))
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, highlightHeight)
        )
    }

    drawContent()
}

fun Modifier.frostedSelfBlur(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.dp) {
        this.blur(radius = radius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        this
    }

/**
 * Draws the Android 16 Material 3 Expressive ambient glassmorphic atmosphere behind a page.
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
                            accent.primary.copy(alpha = if (palette.isDark) 0.18f else 0.11f),
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
                            accent.secondary.copy(alpha = if (palette.isDark) 0.15f else 0.09f),
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
