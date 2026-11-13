package com.vennela365.viewer

import com.vennela365.viewer.theme.FidelityGovernor
import com.vennela365.viewer.theme.FidelityPreference
import com.vennela365.viewer.theme.FidelityTier
import com.vennela365.viewer.theme.UniversalColorEngine
import com.vennela365.viewer.theme.ViewerColorMode
import org.junit.Assert.*
import org.junit.Test

class FidelityAndThemeEngineTest {

    @Test
    fun computePhi_atOptimalConditions_returnsNearOne() {
        // 8.33ms target (120Hz), 8.33ms actual, 20MB used out of 512MB max, no thermal
        val phi = FidelityGovernor.computePhi(
            targetFrameTimeMs = 8.33f,
            emaFrameTimeMs = 8.33f,
            usedMemoryBytes = 20L * 1024 * 1024,
            limitMemoryBytes = 512L * 1024 * 1024,
            thermalThrottling = 0.0f
        )
        assertTrue("Expected phi >= 0.90 but got $phi", phi >= 0.90f)
        assertEquals(FidelityTier.TIER_1_ULTRA, FidelityTier.fromPhi(phi))
    }

    @Test
    fun computePhi_underHeavyMemoryPressure_dropsToPerformanceTier() {
        // 90% memory used
        val phi = FidelityGovernor.computePhi(
            targetFrameTimeMs = 16.66f,
            emaFrameTimeMs = 28.0f,
            usedMemoryBytes = 460L * 1024 * 1024,
            limitMemoryBytes = 512L * 1024 * 1024,
            thermalThrottling = 0.3f
        )
        assertTrue("Expected phi < 0.45 under memory + frame pressure, got $phi", phi < 0.45f)
        assertEquals(FidelityTier.TIER_3_PERFORMANCE, FidelityTier.fromPhi(phi))
    }

    @Test
    fun computePhi_underSevereThermalThrottling_dropsPhi() {
        val phi = FidelityGovernor.computePhi(
            targetFrameTimeMs = 16.66f,
            emaFrameTimeMs = 16.66f,
            usedMemoryBytes = 50L * 1024 * 1024,
            limitMemoryBytes = 512L * 1024 * 1024,
            thermalThrottling = 0.7f
        )
        assertTrue("Thermal throttling should reduce phi below 0.35, got $phi", phi <= 0.35f)
        assertEquals(FidelityTier.TIER_3_PERFORMANCE, FidelityTier.fromPhi(phi))
    }

    @Test
    fun fidelityGovernor_forcePreferences_honored() {
        FidelityGovernor.setPreference(FidelityPreference.FORCE_ULTRA)
        assertEquals(FidelityTier.TIER_1_ULTRA, FidelityGovernor.activeTier.value)
        assertEquals(1.0f, FidelityGovernor.fidelityIndex.value, 0.001f)

        FidelityGovernor.setPreference(FidelityPreference.FORCE_PERFORMANCE)
        assertEquals(FidelityTier.TIER_3_PERFORMANCE, FidelityGovernor.activeTier.value)
        assertTrue(FidelityGovernor.fidelityIndex.value < 0.45f)

        // Reset to auto
        FidelityGovernor.setPreference(FidelityPreference.AUTO)
    }

    @Test
    fun universalColorEngine_allModesReturnValidMatrices() {
        for (mode in ViewerColorMode.entries) {
            val matrix = UniversalColorEngine.getColorMatrixForMode(mode)
            if (mode == ViewerColorMode.ORIGINAL) {
                assertNull(matrix)
            } else {
                assertNotNull("Mode $mode should have non-null ColorMatrix", matrix)
                assertEquals(20, matrix!!.values.size)
            }
        }
    }

    @Test
    fun universalColorEngine_oklabMatrix_preservesChrominance_notMonochrome() {
        val matrix = UniversalColorEngine.OklabColorMatrix
        val values = matrix.values

        // Row 1 (R) must NOT be identical to Row 2 (G) or Row 3 (B)
        assertNotEquals("Row 1 R and Row 2 G must differ to preserve color", values[0], values[5], 0.01f)
        assertNotEquals("Row 1 R and Row 3 B must differ to preserve color", values[0], values[10], 0.01f)

        // Check white inversion: (255, 255, 255) -> (0, 0, 0)
        val rOutWhite = values[0] * 255 + values[1] * 255 + values[2] * 255 + values[4]
        val gOutWhite = values[5] * 255 + values[6] * 255 + values[7] * 255 + values[9]
        val bOutWhite = values[10] * 255 + values[11] * 255 + values[12] * 255 + values[14]

        assertEquals(0.0f, rOutWhite, 1.0f)
        assertEquals(0.0f, gOutWhite, 1.0f)
        assertEquals(0.0f, bOutWhite, 1.0f)
    }

    @Test
    fun universalColorEngine_sepiaMatrix_preservesLuminanceOrder() {
        val matrix = UniversalColorEngine.SepiaColorMatrix
        val values = matrix.values
        assertEquals(0.393f, values[0], 0.001f)
        assertEquals(0.769f, values[1], 0.001f)
        assertEquals(0.189f, values[2], 0.001f)
    }

    @Test
    fun universalColorEngine_fromId_resolvesProperly() {
        assertEquals(ViewerColorMode.OKLAB_DARK, ViewerColorMode.fromId("oklab_dark"))
        assertEquals(ViewerColorMode.OLED_PITCH, ViewerColorMode.fromId("oled_pitch"))
        assertEquals(ViewerColorMode.SAUVOLA_BW, ViewerColorMode.fromId("sauvola_bw"))
        assertEquals(ViewerColorMode.ORIGINAL, ViewerColorMode.fromId("invalid_id"))
    }
}
