package com.vennela365.viewer

import com.vennela365.viewer.ui.components.DiffLineType
import com.vennela365.viewer.ui.components.LassoAnalyticsEngine
import com.vennela365.viewer.ui.components.LightboxDiffEngine
import com.vennela365.viewer.ui.components.PiiCategory
import com.vennela365.viewer.ui.components.PrivacyEngine
import com.vennela365.viewer.ui.components.StudySheetCompilerEngine
import org.junit.Assert.*
import org.junit.Test

class CategoryKillerToolsTest {

    @Test
    fun lassoAnalyticsEngine_extractsNumbers_filtersOutIsoDates() {
        val sample = "Report date: 2026-11-01. Revenue was $1,250.50, units sold 2,400, net loss -150.25."
        val numbers = LassoAnalyticsEngine.extractNumbers(sample)
        assertEquals(3, numbers.size)
        assertEquals(1250.50, numbers[0], 0.001)
        assertEquals(2400.0, numbers[1], 0.001)
        assertEquals(-150.25, numbers[2], 0.001)
    }

    @Test
    fun lassoAnalyticsEngine_extractNumbersInRegion_respectsVerticalBounds() {
        val lines = listOf(
            "Line 1: 100",
            "Line 2: 200",
            "Line 3: 300",
            "Line 4: 400",
            "Line 5: 500"
        )
        // Select lines 2 to 3 (ratio 0.25 to 0.55)
        val selected = LassoAnalyticsEngine.extractNumbersInRegion(lines, 0.25f, 0.55f)
        assertTrue(selected.contains(200.0))
        assertTrue(selected.contains(300.0))
        assertFalse(selected.contains(500.0))
    }

    @Test
    fun lassoAnalyticsEngine_computesStatsAccurately() {
        val numbers = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        val stats = LassoAnalyticsEngine.computeStats(numbers)
        assertNotNull(stats)
        assertEquals(5, stats!!.count)
        assertEquals(150.0, stats.sum, 0.001)
        assertEquals(30.0, stats.mean, 0.001)
        assertEquals(10.0, stats.min, 0.001)
        assertEquals(50.0, stats.max, 0.001)
        assertEquals(30.0, stats.median, 0.001)
    }

    @Test
    fun lassoAnalyticsEngine_evenCountMedian() {
        val numbers = listOf(10.0, 20.0, 30.0, 40.0)
        val stats = LassoAnalyticsEngine.computeStats(numbers)
        assertNotNull(stats)
        assertEquals(25.0, stats!!.median, 0.001)
    }

    @Test
    fun lightboxDiffEngine_lcsDiff_doesNotCascadeOnShift() {
        val original = "Line A\nLine B\nLine C\nLine D\nLine E"
        val modified = "Line Inserted\nLine A\nLine B\nLine C\nLine D\nLine E"

        val diff = LightboxDiffEngine.computeDiff(original, modified)

        // Only ONE added line, remaining 5 lines must be UNCHANGED
        val added = diff.filter { it.type == DiffLineType.ADDED }
        val unchanged = diff.filter { it.type == DiffLineType.UNCHANGED }

        assertEquals(1, added.size)
        assertTrue(added[0].text.contains("Line Inserted"))
        assertEquals(5, unchanged.size)
    }

    @Test
    fun privacyEngine_detectsAllPiiCategories_mergesOverlaps() {
        val text = "Contact john.doe@company.com or call +1-555-234-5678. Salary is $125,000. Card: 4111 2222 3333 4444. SSN: 123-45-6789."
        val matches = PrivacyEngine.scanPii(text)

        assertTrue(matches.any { it.category == PiiCategory.EMAIL })
        assertTrue(matches.any { it.category == PiiCategory.PHONE })
        assertTrue(matches.any { it.category == PiiCategory.FINANCIAL })
        assertTrue(matches.any { it.category == PiiCategory.CREDIT_CARD })
        assertTrue(matches.any { it.category == PiiCategory.IDENTIFIER })

        // Verify non-overlapping invariant
        for (i in 0 until matches.size - 1) {
            assertTrue(
                "Matches must not overlap",
                matches[i].range.last < matches[i + 1].range.first
            )
        }
    }

    @Test
    fun privacyEngine_masksAndReveals() {
        val text = "Send details to support@vennela365.com now."
        val masked = PrivacyEngine.maskPii(text)
        assertFalse(masked.contains("support@vennela365.com"))
        assertTrue(masked.contains("[•••• Email ••••]"))

        // Tap-to-peek reveal match index 0
        val revealed = PrivacyEngine.maskPii(text, revealedMatches = setOf(0))
        assertTrue(revealed.contains("support@vennela365.com"))
    }

    @Test
    fun studySheetCompiler_extractsSectionsAndInsights_fromGeneralDocs() {
        val doc = """
            CLINICAL TRIAL SUMMARY: PHOSPHODIESTERASE INHIBITION
            Section 1. Primary Objectives
            - Evaluated 450 patients over 12 weeks.
            - Total response rate increased by 38.5% compared to placebo.
            
            Section 2. Key Terminology
            - Bioavailability means the fraction of an administered dose reaching systemic circulation.
            - Maximum serum concentration was 14.2 mg/L within 2.5 hours.
        """.trimIndent()

        val sheet = StudySheetCompilerEngine.compile(doc, "Clinical Trial Summary")
        assertEquals("Clinical Trial Summary", sheet.docTitle)
        assertTrue("Expected at least 2 cards", sheet.cards.size >= 2)
        assertTrue("Expected at least 3 insights", sheet.totalTakeaways >= 3)
    }
}
