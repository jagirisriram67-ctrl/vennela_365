package com.vennela365.viewer

import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.security.SafeLimits
import com.vennela365.viewer.storage.FileNameResolver
import org.junit.Assert.*
import org.junit.Test

class FormatRegistryTest {

    @Test
    fun registry_meetsCoverageTarget() {
        assertTrue(
            "Registry holds ${FormatRegistry.totalCount}, expected 1000+",
            FormatRegistry.totalCount >= 1000
        )
    }

    @Test
    fun registry_hasNoMalformedRecords() {
        assertTrue(FormatRegistry.validate().isEmpty())
    }

    @Test
    fun registry_rejectsPlaceholderSuffixes() {
        assertEquals("Detected", FormatRegistry.lookup("xlsx?").supportLevel.label.let {
            // placeholder must NOT resolve to the real xlsx record
            if (FormatRegistry.lookup("xlsx?").extension == "xlsx") "WRONG" else "Detected"
        })
    }

    @Test
    fun compoundSuffix_longestMatchWins() {
        assertEquals("tar.gz", FileNameResolver.resolveKey("backup.tar.gz"))
        assertEquals("tar.xz", FileNameResolver.resolveKey("data.tar.xz"))
        assertEquals("blade.php", FileNameResolver.resolveKey("view.blade.php"))
        assertEquals("pdf", FileNameResolver.resolveKey("Report.PDF"))
    }

    @Test
    fun extensionlessNames_resolve() {
        assertEquals("dockerfile", FileNameResolver.resolveKey("Dockerfile"))
        assertEquals("makefile", FileNameResolver.resolveKey("Makefile"))
        assertEquals(".gitignore", FileNameResolver.resolveKey(".gitignore"))
    }

    @Test
    fun lookupForFile_prefersCompoundOverSimpleExt() {
        assertEquals("tar.gz", FormatRegistry.lookupForFile("backup.tar.gz").extension)
        assertEquals("dockerfile", FormatRegistry.lookupForFile("Dockerfile").extension)
    }

    @Test
    fun supportLevels_areHonestZeroToFour() {
        val dash = FormatRegistry.dashboard()
        val total = dash.byLevel.values.sum()
        assertEquals(FormatRegistry.totalCount, total)
        // Binaries must stay at L0 (never claim rendering)
        assertEquals(0, FormatRegistry.lookup("exe").levelNumber)
        // Common images genuinely render at L4
        assertEquals(4, FormatRegistry.lookup("png").levelNumber)
        // Proprietary CAD stays metadata-only
        assertEquals(1, FormatRegistry.lookup("dwg").levelNumber)
    }

    @Test
    fun signatureDetect_identifiesContainers() {
        assertEquals("PDF Document", FormatRegistry.signatureDetect(byteArrayOf(0x25, 0x50, 0x44, 0x46)))
        assertEquals("PNG Image", FormatRegistry.signatureDetect(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)))
        assertEquals("GZIP Stream", FormatRegistry.signatureDetect(byteArrayOf(0x1F, 0x8B.toByte(), 0x08, 0x00)))
    }

    @Test
    fun safeLimits_blockTraversal() {
        assertFalse(SafeLimits.isSafeEntryPath("../../etc/passwd"))
        assertFalse(SafeLimits.isSafeEntryPath("/abs/path"))
        assertTrue(SafeLimits.isSafeEntryPath("docs/report.pdf"))
    }
}
