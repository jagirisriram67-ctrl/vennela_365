package com.vennela365.viewer.theme

import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Every dial that shapes a liquid-glass surface.
 *
 * Origin-OS style glass is not one effect: it is a frosted backdrop, a tinted
 * body, a bright specular rim, an interior sheen and a soft drop shadow stacked
 * in that order. This spec keeps all five tunable per surface.
 */
data class LiquidGlassSpec(
    /** Tint washed over the frosted backdrop. */
    val tint: Color,
    /** Secondary tint for the vertical body gradient. */
    val tintEnd: Color = tint,
    /** Frost strength in dp. 0 disables blur entirely. */
    val frost: Dp = 18.dp,
    /** Opacity of the frosted backdrop showing through the tint. */
    val backdropAlpha: Float = 0.58f,
    /** Rim colour, brightest at the top edge. */
    val rim: Color,
    /** Rim thickness. */
    val rimWidth: Dp = 1.dp,
    /** Interior sheen strength, 0..1. */
    val sheen: Float = 0.18f,
    /** Drop shadow elevation. */
    val elevation: Dp = 10.dp,
    /** Shadow colour; the accent glow reads best at low alpha. */
    val shadowColor: Color = Color(0x66000000),
    /** Liquid highlight blob strength, 0 disables it. */
    val highlight: Float = 0.14f
)

/**
 * Applies a real liquid-glass surface.
 *
 * The backdrop directly behind this element is sampled, down-scaled and blurred
 * so the result reads as frosted glass rather than a flat translucent
 * rectangle. When the backdrop is unavailable — or the device is in the
 * low-power fidelity tier — the same call degrades to a tinted, rimmed surface
 * at near-zero cost, so callers never need to branch.
 */
@Composable
fun Modifier.liquidGlass(
    spec: LiquidGlassSpec,
    shape: Shape = RoundedCornerShape(VennelaTokens.radiusCard)
): Modifier {
    val backdrop = LocalBackdropImage.current
    val density = LocalDensity.current
    val tier by FidelityGovernor.activeTier.collectAsState()
    val textures = tier != FidelityTier.TIER_3_PERFORMANCE

    var target by remember { mutableStateOf(BackdropTarget()) }

    val base = this
        .shadow(
            elevation = spec.elevation,
            shape = shape,
            clip = false,
            ambientColor = spec.shadowColor,
            spotColor = spec.shadowColor
        )
        .clip(shape)
        .onGloballyPositioned { coords ->
            val pos = coords.positionInRoot()
            val next = BackdropTarget(
                position = IntOffset(pos.x.roundToInt(), pos.y.roundToInt()),
                size = IntSize(coords.size.width, coords.size.height)
            )
            if (next != target) target = next
        }

    if (!textures || spec.frost <= 0.dp) {
        return base.drawWithCache {
            val specular = brushSpecular(size, spec.rim, spec.highlight)
            onDrawBehind {
                drawRect(Brush.verticalGradient(listOf(spec.tint, spec.tintEnd)))
                drawLiquidGlassFinish(spec, specular, size)
            }
        }
    }

    return base.drawWithCache {
        val densityValue = density.density
        val radius = spec.frost.value
        val shader = frostBackdrop(backdrop, target, radius, densityValue)
        val specular = brushSpecular(size, spec.rim, spec.highlight)
        onDrawBehind {
            if (shader != null) {
                drawFrostedBackdrop(shader, spec.backdropAlpha)
            } else {
                drawRect(spec.tint.copy(alpha = 0.72f))
            }
            // Tinted body on top of the frost.
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        spec.tint.copy(alpha = spec.tint.alpha),
                        spec.tintEnd.copy(alpha = spec.tintEnd.alpha * 0.82f)
                    )
                )
            )
            drawLiquidGlassFinish(spec, specular, size)
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Drawing primitives.                                                        */
/* ------------------------------------------------------------------------- */

private fun DrawScope.drawFrostedBackdrop(shader: android.graphics.Shader, alpha: Float) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.shader = shader
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        native.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

/** Interior sheen + bright top rim + hairline edge, the glass signature. */
private fun DrawScope.drawLiquidGlassFinish(
    spec: LiquidGlassSpec,
    specular: Brush,
    size: Size
) {
    // Interior sheen falling away from the top edge.
    if (spec.sheen > 0f) {
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = spec.sheen),
                0.42f to Color.Transparent,
                1f to Color.Transparent
            )
        )
    }

    // Liquid highlight blob in the upper-left, like light through a droplet.
    if (spec.highlight > 0f) {
        drawRect(brush = specular)
    }

    // Rim: bright at the top, almost invisible at the bottom.
    val rimPx = spec.rimWidth.toPx()
    if (rimPx > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    spec.rim.copy(alpha = (spec.rim.alpha * 1.5f).coerceAtMost(1f)),
                    spec.rim.copy(alpha = spec.rim.alpha * 0.45f),
                    spec.rim.copy(alpha = spec.rim.alpha * 0.12f)
                )
            ),
            style = Stroke(width = rimPx)
        )
    }
}

private fun brushSpecular(size: Size, rim: Color, strength: Float): Brush {
    if (strength <= 0f || size.width <= 0f) return Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    val radius = size.maxDimension * 0.85f
    return Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = strength),
            Color.White.copy(alpha = strength * 0.35f),
            Color.Transparent
        ),
        center = Offset(size.width * 0.24f, size.height * 0.10f),
        radius = radius
    )
}

/* ------------------------------------------------------------------------- */
/* Palette-aware spec factory.                                                */
/* ------------------------------------------------------------------------- */

/**
 * Builds a glass spec from the active palette so every surface automatically
 * follows the current theme, accent and light/dark mode.
 */
@Composable
fun liquidGlassSpec(
    alpha: Float = 0.42f,
    frost: Dp = 18.dp,
    elevation: Dp = 10.dp,
    sheen: Float = 0.22f,
    highlight: Float = 0.16f,
    strong: Boolean = false
): LiquidGlassSpec {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val bodyAlpha = if (strong) alpha * 1.35f else alpha
    return LiquidGlassSpec(
        tint = palette.cardSurface.copy(alpha = bodyAlpha.coerceIn(0.05f, 0.92f)),
        tintEnd = palette.cardSurfaceStrong.copy(alpha = (bodyAlpha * 0.78f).coerceIn(0.04f, 0.92f)),
        frost = frost,
        backdropAlpha = if (palette.isDark) 0.62f else 0.72f,
        rim = accent.secondary.copy(alpha = if (palette.isDark) 0.46f else 0.48f),
        sheen = sheen,
        elevation = elevation,
        shadowColor = if (palette.isDark) Color(0x99000000) else Color(0x330B142B),
        highlight = highlight
    )
}


