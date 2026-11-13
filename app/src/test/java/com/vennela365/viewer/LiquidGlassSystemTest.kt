package com.vennela365.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.vennela365.viewer.theme.AccentOption
import com.vennela365.viewer.theme.BackdropBlur
import com.vennela365.viewer.theme.DarkVennelaPalette
import com.vennela365.viewer.theme.LightVennelaPalette
import com.vennela365.viewer.theme.LiquidAtmosphere
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.toAccent
import com.vennela365.viewer.theme.tinted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the liquid-glass design system, which every screen now depends on.
 *
 * These are the invariants that silently break the visual language if a future
 * edit gets them wrong: the backdrop blur never becomes a no-op, the atmosphere
 * survives any palette/accent/quality combination including degenerate sizes,
 * and the motion curves stay ordered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidGlassSystemTest {

    /* ---- Backdrop blur -------------------------------------------------- */

    @Test
    fun `blur radius stays within a visible and cheap band`() {
        // A phone backdrop is ~180px wide for a ~1080px screen: scale ≈ 0.1667.
        val phoneScale = 180f / 1080f
        val radius = BackdropBlur.blurRadiusPx(
            requestedDp = VennelaTokens.frostRegular.value,
            backdropScale = phoneScale,
            density = 2.75f
        )
        assertTrue("blur must actually blur, got $radius", radius >= 0.6f)
        assertTrue("blur must stay affordable, got $radius", radius <= 22f)
    }

    @Test
    fun `cropScaleBlur produces the requested dimensions`() {
        val source = Bitmap.createBitmap(180, 400, Bitmap.Config.ARGB_8888)
        Canvas(source).drawColor(android.graphics.Color.BLUE)

        val out = BackdropBlur.cropScaleBlur(
            src = source,
            srcRect = Rect(20, 30, 140, 230),
            outWidth = 60,
            outHeight = 100,
            radiusPx = 4f
        )

        assertEquals(60, out.width)
        assertEquals(100, out.height)
        assertFalse(out.isRecycled)
        // Every pixel must be opaque: a transparent frost would look like a hole.
        assertEquals(android.graphics.Color.BLUE, out.getPixel(30, 50))
    }

    @Test
    fun `cropScaleBlur tolerates a degenerate source rect`() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val out = BackdropBlur.cropScaleBlur(
            src = source,
            srcRect = Rect(0, 0, 0, 0),
            outWidth = 0,
            outHeight = 0,
            radiusPx = 0f
        )
        // Must clamp to at least 1x1 rather than throwing on Bitmap.createBitmap.
        assertTrue(out.width >= 1)
        assertTrue(out.height >= 1)
    }

    /* ---- Atmosphere ----------------------------------------------------- */

    @Test
    fun `atmosphere renders in every palette, accent and quality tier`() {
        val bitmaps = listOf(DarkVennelaPalette, LightVennelaPalette)
        val accents = AccentOption.entries.map { it.toAccent() }
        val qualities = listOf(0, 2, 4)

        bitmaps.forEach { palette ->
            accents.forEach { accent ->
                qualities.forEach { quality ->
                    val target = Bitmap.createBitmap(180, 400, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(target)
                    LiquidAtmosphere.render(
                        canvas = canvas,
                        width = 180f,
                        height = 400f,
                        palette = palette.tinted(accent),
                        accent = accent,
                        quality = quality
                    )
                    // The sky gradient must have painted something everywhere.
                    assertEquals(
                        "unpainted atmosphere for $palette/$accent/q$quality",
                        255,
                        android.graphics.Color.alpha(target.getPixel(90, 200))
                    )
                }
            }
        }
    }

    @Test
    fun `atmosphere ignores degenerate canvas sizes instead of crashing`() {
        val target = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        LiquidAtmosphere.render(
            canvas = Canvas(target),
            width = 0f,
            height = 0f,
            palette = DarkVennelaPalette,
            accent = AccentOption.ICE_BLUE.toAccent(),
            quality = 4
        )
    }

    /* ---- Motion --------------------------------------------------------- */

    @Test
    fun `motion durations are ordered`() {
        assertTrue(LiquidMotion.durationInstant < LiquidMotion.durationFast)
        assertTrue(LiquidMotion.durationFast < LiquidMotion.durationBase)
        assertTrue(LiquidMotion.durationBase < LiquidMotion.durationSlow)
        assertTrue(LiquidMotion.durationSlow < LiquidMotion.durationHero)
    }

    @Test
    fun `spring specs and tweens are constructible for both float and offset`() {
        assertNotNull(LiquidMotion.press<Float>())
        assertNotNull(LiquidMotion.liquid<Float>())
        assertNotNull(LiquidMotion.playful<Float>())
        assertNotNull(LiquidMotion.shared<Float>())
        assertNotNull(LiquidMotion.glide<Float>())
        assertNotNull(LiquidMotion.exit<Float>())
        assertNotNull(LiquidMotion.emphasis<Float>())
        assertNotNull(LiquidMotion.glide<androidx.compose.ui.unit.IntOffset>())
    }

    /* ---- Palette tinting ------------------------------------------------ */

    @Test
    fun `accent tinting never produces a fully transparent background`() {
        val accents = AccentOption.entries.map { it.toAccent() } +
            com.vennela365.viewer.theme.AppTheme.entries.map { it.toAccent() }

        listOf(DarkVennelaPalette, LightVennelaPalette).forEach { base ->
            accents.forEach { accent ->
                val tinted = base.tinted(accent)
                assertTrue(
                    "backgroundTop vanished for ${accent.label}",
                    tinted.backgroundTop.alpha > 0.9f
                )
                assertTrue(
                    "cardSurface vanished for ${accent.label}",
                    tinted.cardSurface.alpha > 0.5f
                )
                assertTrue(
                    "cardBorder vanished for ${accent.label}",
                    tinted.cardBorder.alpha > 0.15f
                )
            }
        }
    }

    /* ---- Tokens --------------------------------------------------------- */

    @Test
    fun `token scales are monotonic`() {
        assertTrue(VennelaTokens.radiusChip < VennelaTokens.radiusCard)
        assertTrue(VennelaTokens.radiusCard < VennelaTokens.radiusCardLarge)
        assertTrue(VennelaTokens.radiusCardLarge < VennelaTokens.radiusSheet)
        assertTrue(VennelaTokens.radiusSheet < VennelaTokens.radiusDock)
        assertTrue(VennelaTokens.radiusDock < VennelaTokens.radiusPill)

        assertTrue(VennelaTokens.spaceXs < VennelaTokens.spaceSm)
        assertTrue(VennelaTokens.spaceSm < VennelaTokens.spaceMd)
        assertTrue(VennelaTokens.spaceMd < VennelaTokens.spaceLg)
        assertTrue(VennelaTokens.spaceLg < VennelaTokens.spaceXl)
        assertTrue(VennelaTokens.spaceXl < VennelaTokens.spaceXxl)
        assertTrue(VennelaTokens.spaceXxl < VennelaTokens.spaceHuge)

        assertTrue(VennelaTokens.frostLight < VennelaTokens.frostRegular)
        assertTrue(VennelaTokens.frostRegular < VennelaTokens.frostHeavy)
    }

    @Test
    fun `touch targets meet the accessibility minimum`() {
        assertTrue(VennelaTokens.touchTargetMin.value >= 48f)
    }

    @Test
    fun `switching between dark and light palette produces distinct atmosphere luminance`() {
        val accent = AccentOption.ICE_BLUE.toAccent()
        val darkBitmap = Bitmap.createBitmap(180, 400, Bitmap.Config.ARGB_8888)
        val lightBitmap = Bitmap.createBitmap(180, 400, Bitmap.Config.ARGB_8888)

        LiquidAtmosphere.render(
            canvas = Canvas(darkBitmap),
            width = 180f,
            height = 400f,
            palette = DarkVennelaPalette.tinted(accent),
            accent = accent,
            quality = 2
        )
        LiquidAtmosphere.render(
            canvas = Canvas(lightBitmap),
            width = 180f,
            height = 400f,
            palette = LightVennelaPalette.tinted(accent),
            accent = accent,
            quality = 2
        )

        val darkPixel = darkBitmap.getPixel(90, 200)
        val lightPixel = lightBitmap.getPixel(90, 200)
        val darkLum = android.graphics.Color.red(darkPixel) +
            android.graphics.Color.green(darkPixel) +
            android.graphics.Color.blue(darkPixel)
        val lightLum = android.graphics.Color.red(lightPixel) +
            android.graphics.Color.green(lightPixel) +
            android.graphics.Color.blue(lightPixel)

        assertTrue(
            "Light mode atmosphere luminance ($lightLum) must be significantly brighter than Dark mode ($darkLum)",
            lightLum > darkLum + 300
        )
    }
}
