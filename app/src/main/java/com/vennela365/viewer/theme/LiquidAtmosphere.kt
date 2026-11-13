package com.vennela365.viewer.theme

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * LiquidAtmosphere: OriginOS 7 Spatial Liquid & Light Caustic Engine.
 *
 * Replaces crude landscape drawings with an ultra-premium, pure optical spatial
 * environment. Generates real-time chromatic caustics, Snell-Descartes light
 * refraction paths, and deep obsidian/graphite spatial gradients that let
 * frosted glass surfaces refract true dynamic depth.
 */
object LiquidAtmosphere {

    fun render(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: VennelaPalette,
        accent: VennelaAccent,
        quality: Int
    ) {
        if (width <= 1f || height <= 1f) return

        drawSpatialFoundation(canvas, width, height, palette, accent)
        drawChromaticCaustics(canvas, width, height, palette, accent)
        drawQuantumPhotonField(canvas, width, height, palette, accent, quality)
        drawSpecularPrismEnergy(canvas, width, height, palette, accent)
    }

    /* ---- 1. Spatial Foundation ---------------------------------------- */

    private fun drawSpatialFoundation(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: VennelaPalette,
        accent: VennelaAccent
    ) {
        val paint = Paint(Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        
        // Deep obsidian-to-space-graphite vertical dispersion
        paint.shader = LinearGradient(
            0f, 0f, 0f, height,
            intArrayOf(
                palette.backgroundTop.toArgb(),
                palette.backgroundMid.toArgb(),
                palette.backgroundBottom.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, paint)

        // Top-center luminous aperture (ambient overhead light source)
        paint.shader = RadialGradient(
            width * 0.5f, 0f,
            width * 0.95f,
            intArrayOf(
                accent.secondary.copy(alpha = if (palette.isDark) 0.28f else 0.18f).toArgb(),
                accent.primary.copy(alpha = if (palette.isDark) 0.14f else 0.08f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height * 0.65f, paint)

        // Bottom-end counter-glow (OriginOS fluid grounding)
        paint.shader = RadialGradient(
            width * 0.85f, height,
            width * 0.80f,
            intArrayOf(
                accent.primary.copy(alpha = if (palette.isDark) 0.22f else 0.14f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.40f, width, height, paint)
    }

    /* ---- 2. Chromatic Light Caustics --------------------------------- */

    private fun drawChromaticCaustics(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: VennelaPalette,
        accent: VennelaAccent
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val intensity = if (palette.isDark) 1.25f else 0.85f

        // Caustic Stream 1: Diagonal fluid photon ribbon from top-left
        val path1 = Path().apply {
            moveTo(-width * 0.2f, height * 0.12f)
            cubicTo(
                width * 0.35f, height * 0.05f,
                width * 0.45f, height * 0.42f,
                width * 1.2f, height * 0.32f
            )
            lineTo(width * 1.2f, height * 0.48f)
            cubicTo(
                width * 0.55f, height * 0.52f,
                width * 0.25f, height * 0.22f,
                -width * 0.2f, height * 0.24f
            )
            close()
        }

        paint.shader = LinearGradient(
            0f, height * 0.1f, width, height * 0.4f,
            intArrayOf(
                accent.secondary.copy(alpha = 0.18f * intensity).toArgb(),
                accent.primary.copy(alpha = 0.12f * intensity).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path1, paint)

        // Caustic Stream 2: S-curve refractive fluid wave in lower-mid tier
        val path2 = Path().apply {
            moveTo(-width * 0.1f, height * 0.72f)
            cubicTo(
                width * 0.30f, height * 0.58f,
                width * 0.65f, height * 0.82f,
                width * 1.15f, height * 0.66f
            )
            lineTo(width * 1.15f, height * 0.78f)
            cubicTo(
                width * 0.70f, height * 0.90f,
                width * 0.25f, height * 0.70f,
                -width * 0.1f, height * 0.84f
            )
            close()
        }

        paint.shader = LinearGradient(
            0f, height * 0.65f, width, height * 0.85f,
            intArrayOf(
                Color.Transparent.toArgb(),
                blend(accent.primary, accent.secondary, 0.5f).copy(alpha = 0.15f * intensity).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path2, paint)
    }

    /* ---- 3. Quantum Photon Field ------------------------------------- */

    private fun drawQuantumPhotonField(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: VennelaPalette,
        accent: VennelaAccent,
        quality: Int
    ) {
        if (!palette.isDark || quality <= 0) return
        
        val random = Random(0x4F524947) // Origin seed
        val count = (28 * quality).coerceAtMost(80)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        repeat(count) {
            val px = random.nextFloat() * width
            val py = random.nextFloat() * height
            val radius = (0.6f + random.nextFloat() * 1.4f) * (width / 400f)
            
            // Photons closer to accent pools take on faint chromatic luminescence
            val distToCenter = Math.hypot((px - width * 0.5).toDouble(), (py - height * 0.3).toDouble()).toFloat()
            val centerProximity = (1f - distToCenter / (width * 0.85f)).coerceIn(0f, 1f)
            
            val baseColor = if (random.nextBoolean()) accent.secondary else Color.White
            val alpha = (0.08f + centerProximity * 0.35f + random.nextFloat() * 0.20f).coerceIn(0.04f, 0.65f)
            
            paint.color = baseColor.copy(alpha = alpha).toArgb()
            canvas.drawCircle(px, py, radius, paint)
        }
    }

    /* ---- 4. Specular Prism Energy Lines ------------------------------ */

    private fun drawSpecularPrismEnergy(
        canvas: Canvas,
        width: Float,
        height: Float,
        palette: VennelaPalette,
        accent: VennelaAccent
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Subtle angled prism refraction lines running diagonally (OriginOS Light & Shadow aesthetic)
        val prismAngle = -22f
        val save = canvas.save()
        canvas.rotate(prismAngle, width * 0.5f, height * 0.5f)

        paint.shader = LinearGradient(
            width * 0.2f, height * 0.3f, width * 0.8f, height * 0.3f,
            intArrayOf(
                Color.Transparent.toArgb(),
                accent.secondary.copy(alpha = if (palette.isDark) 0.06f else 0.04f).toArgb(),
                Color.White.copy(alpha = if (palette.isDark) 0.08f else 0.05f).toArgb(),
                accent.primary.copy(alpha = if (palette.isDark) 0.06f else 0.04f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(-width * 0.5f, height * 0.25f, width * 1.5f, height * 0.35f, paint)

        canvas.restoreToCount(save)
    }

    private fun blend(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
}
