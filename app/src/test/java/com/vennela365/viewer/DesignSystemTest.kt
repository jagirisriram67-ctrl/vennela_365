package com.vennela365.viewer

import androidx.compose.ui.unit.dp
import com.vennela365.viewer.theme.AppFontStyle
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.vennelaTypographyFor
import com.vennela365.viewer.ui.components.OkenScanFilter
import com.vennela365.viewer.ui.components.ViewerKind
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.WpsPaperTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the shape of the design system.
 *
 * The app's chrome is deliberately compact: the user asked for components that
 * stop stealing the screen from the content. These tests pin the invariants
 * that keep that true, so a future tweak cannot quietly re-inflate the app.
 */
class DesignSystemTest {

    @Test
    fun `radii increase monotonically from chip to dock`() {
        val ordered = listOf(
            VennelaTokens.radiusChip,
            VennelaTokens.radiusBadge,
            VennelaTokens.radiusControl,
            VennelaTokens.radiusTile,
            VennelaTokens.radiusCard,
            VennelaTokens.radiusCardLarge,
            VennelaTokens.radiusSheet,
            VennelaTokens.radiusDock
        )
        for (i in 0 until ordered.size - 1) {
            assertTrue(
                "radius scale must not shrink: ${ordered[i]} then ${ordered[i + 1]}",
                ordered[i] <= ordered[i + 1]
            )
        }
    }

    @Test
    fun `compact icon control fits inside the viewer bar`() {
        assertTrue(
            "controlSm (${VennelaTokens.controlSm}) must fit inside viewerBar (${VennelaTokens.viewerBar})",
            VennelaTokens.controlSm <= VennelaTokens.viewerBar
        )
    }

    @Test
    fun `icon scale increases with the badge scale`() {
        assertTrue(VennelaTokens.iconSm <= VennelaTokens.iconMd)
        assertTrue(VennelaTokens.iconMd <= VennelaTokens.iconLg)
        assertTrue(VennelaTokens.iconLg <= VennelaTokens.iconXl)
        assertTrue(VennelaTokens.badgeXs <= VennelaTokens.badgeSm)
        assertTrue(VennelaTokens.badgeSm <= VennelaTokens.badgeMd)
        assertTrue(VennelaTokens.badgeMd <= VennelaTokens.badgeLg)
    }

    @Test
    fun `compact badges never grow into the old oversized scale`() {
        // 26..44dp is the badge range. Anything larger starts competing with
        // the content it is meant to label.
        assertTrue(VennelaTokens.badgeLg <= 48.dp)
        assertTrue(VennelaTokens.badgeMd <= 40.dp)
        assertTrue(VennelaTokens.badgeSm <= 34.dp)
    }

    @Test
    fun `spacing scale increases monotonically`() {
        val ordered = listOf(
            VennelaTokens.spaceXxs,
            VennelaTokens.spaceXs,
            VennelaTokens.spaceSm,
            VennelaTokens.spaceMd,
            VennelaTokens.spaceLg,
            VennelaTokens.spaceXl,
            VennelaTokens.spaceXxl,
            VennelaTokens.spaceHuge
        )
        for (i in 0 until ordered.size - 1) {
            assertTrue(
                "spacing scale must not shrink: ${ordered[i]} then ${ordered[i + 1]}",
                ordered[i] <= ordered[i + 1]
            )
        }
    }

    @Test
    fun `type scale descends from display to nano`() {
        val ordered = listOf(
            VennelaTokens.titleHero,
            VennelaTokens.titleDisplay,
            VennelaTokens.titleLarge,
            VennelaTokens.titleMedium,
            VennelaTokens.titleSmall,
            VennelaTokens.body,
            VennelaTokens.bodySmall,
            VennelaTokens.caption,
            VennelaTokens.micro,
            VennelaTokens.nano
        )
        for (i in 0 until ordered.size - 1) {
            assertTrue(
                "type scale must not grow downwards: ${ordered[i]} then ${ordered[i + 1]}",
                ordered[i].value >= ordered[i + 1].value
            )
        }
    }

    @Test
    fun `touch targets keep an accessible floor`() {
        // The painted control may be compact, but the accessible floor stays.
        assertTrue(VennelaTokens.touchTargetMin >= 44.dp)
        assertTrue(VennelaTokens.touchTargetCompact >= 36.dp)
    }

    @Test
    fun `all 5 flagship viewing suites round-trip by id and provide distinct brand names`() {
        assertEquals(5, ViewerSuiteStyle.entries.size)
        ViewerSuiteStyle.entries.forEach { suite ->
            assertEquals(suite, ViewerSuiteStyle.fromId(suite.id))
            ViewerKind.entries.forEach { kind ->
                assertTrue(kind.appNameFor(suite).isNotBlank())
                assertTrue(kind.accentFor(suite).alpha > 0.9f)
            }
        }
        assertEquals("Google Docs", ViewerKind.DOCS.appNameFor(ViewerSuiteStyle.GOOGLE_WORKSPACE))
        assertEquals("Microsoft Word", ViewerKind.DOCS.appNameFor(ViewerSuiteStyle.MICROSOFT_365))
        assertEquals("WPS Writer", ViewerKind.DOCS.appNameFor(ViewerSuiteStyle.WPS_SUPER))
        assertEquals("LibreOffice Writer", ViewerKind.DOCS.appNameFor(ViewerSuiteStyle.LIBRE_OFFICE))
        assertEquals("Oken HD Doc Scan", ViewerKind.DOCS.appNameFor(ViewerSuiteStyle.OKEN_SCANNER))
    }

    @Test
    fun `oneplus sans is the default typography style and all font presets build valid typography`() {
        assertEquals(AppFontStyle.ONEPLUS_SANS, AppFontStyle.fromId(null))
        assertEquals(AppFontStyle.ONEPLUS_SANS, AppFontStyle.fromId("oneplus_sans"))
        AppFontStyle.entries.forEach { style ->
            val typo = vennelaTypographyFor(style)
            assertNotNull(typo.bodyLarge.fontFamily)
            assertNotNull(typo.headlineLarge.fontFamily)
        }
    }

    @Test
    fun `oken scanner filters and wps eye care paper tones provide valid color matrices`() {
        assertEquals(5, OkenScanFilter.entries.size)
        assertNotNull(OkenScanFilter.MAGIC_COLOR.toColorFilter())
        assertNotNull(OkenScanFilter.BW_CRISP.toColorFilter())
        assertEquals(0xFFC7EDCC.toInt(), WpsPaperTone.EYE_CARE_GREEN.paperColor.value.shr(32).toInt())
    }
}
