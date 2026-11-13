package com.vennela365.viewer

import android.net.Uri
import android.os.Build
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.storage.AllFilesAccess
import com.vennela365.viewer.storage.DirectFileBrowser
import com.vennela365.viewer.storage.FileNameResolver
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.storage.StorageVolumes
import com.vennela365.viewer.storage.VolumeKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guards the restriction-less storage layer.
 *
 * These are the invariants that decide whether the file manager can honestly
 * claim to browse every folder: real capacity numbers never go negative, a
 * directory listing never silently swallows a permission failure, `file://`
 * URIs are never handed to another app, and the deep search terminates.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageLayerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val context get() = RuntimeEnvironment.getApplication() as android.content.Context

    /* ---- Volume enumeration --------------------------------------------- */

    @Test
    fun `volume enumeration returns at least the primary volume`() {
        val volumes = StorageVolumes.enumerate(context)
        assertTrue("expected at least one volume", volumes.isNotEmpty())
    }

    @Test
    fun `volume capacity never reports negative usage`() {
        StorageVolumes.enumerate(context).forEach { volume ->
            assertTrue(
                "usedBytes must not be negative for ${volume.label}",
                volume.usedBytes >= 0L
            )
            assertTrue(
                "usedFraction must stay within 0..1 for ${volume.label}",
                volume.usedFraction in 0f..1f
            )
        }
    }

    @Test
    fun `root paths classify into the right volume kinds`() {
        assertEquals(VolumeKind.INTERNAL, StorageVolumes.guessKind("/storage/emulated/0", removable = false))
        assertEquals(VolumeKind.INTERNAL, StorageVolumes.guessKind("/sdcard", removable = true))
        assertEquals(VolumeKind.USB_OTG, StorageVolumes.guessKind("/storage/1A2B-3C4D-usb", removable = true))
        assertEquals(VolumeKind.USB_OTG, StorageVolumes.guessKind("/mnt/otg-drive", removable = true))
        assertEquals(VolumeKind.SD_CARD, StorageVolumes.guessKind("/storage/1A2B-3C4D", removable = true))
        assertEquals(VolumeKind.UNKNOWN, StorageVolumes.guessKind("/storage/1A2B-3C4D", removable = false))
    }

    @Test
    fun `root paths derive the expected labels`() {
        assertEquals("Internal storage", StorageVolumes.guessLabel("/storage/emulated/0"))
        assertEquals("USB storage", StorageVolumes.guessLabel("/mnt/usb-otg"))
        assertEquals("SD card", StorageVolumes.guessLabel("/storage/1A2B-3C4D"))
    }

    @Test
    fun `every primary volume is classified as internal storage`() {
        // A headless Robolectric host may expose no platform volumes at all, so
        // assert the classification invariant over whatever was reported
        // rather than requiring the host to have real storage attached.
        StorageVolumes.enumerate(context)
            .filter { it.isPrimary }
            .forEach { volume ->
                assertEquals(
                    "the primary volume must be internal: ${volume.label}",
                    VolumeKind.INTERNAL,
                    volume.kind
                )
            }
    }

    @Test
    fun `browsable volumes always carry a root path`() {
        StorageVolumes.browsable(context).forEach { volume ->
            assertFalse(
                "a browsable volume must expose a path: ${volume.label}",
                volume.rootPath.isNullOrBlank()
            )
        }
    }

    /* ---- Access gating --------------------------------------------------- */

    @Test
    fun `runtime permissions are never empty`() {
        assertTrue(AllFilesAccess.runtimePermissions().isNotEmpty())
    }

    @Test
    fun `runtime permissions match the running sdk`() {
        val permissions = AllFilesAccess.runtimePermissions().toList()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                assertTrue(permissions.any { it.contains("READ_MEDIA") })
            else ->
                assertTrue(permissions.any { it.contains("READ_EXTERNAL_STORAGE") })
        }
    }

    @Test
    fun `grant status label is never blank`() {
        assertTrue(AllFilesAccess.statusLabel(context).isNotBlank())
    }

    @Test
    fun `settings intent always resolves to something`() {
        assertNotNull(AllFilesAccess.settingsIntent(context))
    }

    /* ---- Direct listing -------------------------------------------------- */

    @Test
    fun `listing a missing folder fails with a real message`() {
        val result = DirectFileBrowser.list(File(temp.root, "does-not-exist"))
        assertTrue("expected a failure", result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    @Test
    fun `listing a file rather than a folder fails`() {
        val file = temp.newFile("plain.txt").apply { writeText("hello") }
        val result = DirectFileBrowser.list(file)
        assertTrue(result.isFailure)
    }

    @Test
    fun `listing sorts folders before files`() {
        temp.newFolder("zeta")
        temp.newFile("alpha.txt").writeText("a")
        temp.newFolder("beta")

        val items = DirectFileBrowser.list(temp.root).getOrThrow()
        val firstFileIndex = items.indexOfFirst { !it.isDirectory }
        val lastFolderIndex = items.indexOfLast { it.isDirectory }
        assertTrue("no files found", firstFileIndex >= 0)
        assertTrue("folders must precede files", lastFolderIndex < firstFileIndex)
    }

    @Test
    fun `hidden entries are excluded unless requested`() {
        temp.newFile(".secret").writeText("s")
        temp.newFile("visible.txt").writeText("v")

        val hidden = DirectFileBrowser.list(temp.root, showHidden = false).getOrThrow()
        assertFalse(hidden.any { it.name == ".secret" })

        val shown = DirectFileBrowser.list(temp.root, showHidden = true).getOrThrow()
        assertTrue(shown.any { it.name == ".secret" })
    }

    @Test
    fun `a listed file carries its real size and a file uri`() {
        val payload = "0123456789"
        temp.newFile("sized.txt").writeText(payload)

        val item = DirectFileBrowser.list(temp.root).getOrThrow().first { it.name == "sized.txt" }
        assertEquals(payload.length.toLong(), item.sizeBytes)
        assertEquals("file", item.uri?.scheme)
        assertEquals(item.path, item.id)
    }

    @Test
    fun `format category is resolved from the extension`() {
        temp.newFile("sheet.csv").writeText("a,b\n1,2\n")
        val item = DirectFileBrowser.list(temp.root).getOrThrow().first { it.name == "sheet.csv" }
        assertEquals("csv", item.extension)
        assertEquals(FormatCategory.SPREADSHEETS, item.formatInfo.category)
    }

    @Test
    fun `a volume root is recognised as a root`() {
        val volumes = StorageVolumes.enumerate(context)
        val root = volumes.firstOrNull { it.isBrowsable }?.rootPath
        if (root != null) {
            assertTrue(DirectFileBrowser.isVolumeRoot(context, root))
        }
        assertTrue(DirectFileBrowser.isVolumeRoot(context, ""))
    }

    /* ---- Deep search ----------------------------------------------------- */

    @Test
    fun `deep search finds nested matches and ignores missing roots`() {
        val nested = File(temp.newFolder("one"), "two")
        nested.mkdirs()
        File(nested, "quarterly-report.pdf").writeText("x")
        File(nested, "notes.txt").writeText("y")

        val found = runBlocking {
            DirectFileBrowser.search(
                roots = listOf(temp.root.absolutePath, "/definitely/not/here"),
                query = "quarterly"
            )
        }
        assertEquals(1, found.size)
        assertEquals("quarterly-report.pdf", found.first().name)
    }

    @Test
    fun `deep search is case insensitive and matches partial names`() {
        temp.newFile("MixedCaseTarget.md").writeText("x")
        val found = runBlocking {
            DirectFileBrowser.search(listOf(temp.root.absolutePath), "mixedcase")
        }
        assertEquals(1, found.size)
    }

    @Test
    fun `deep search returns nothing for a blank query`() {
        temp.newFile("anything.txt").writeText("x")
        val found = runBlocking {
            DirectFileBrowser.search(listOf(temp.root.absolutePath), "   ")
        }
        assertTrue(found.isEmpty())
    }

    @Test
    fun `deep search respects its result limit`() {
        repeat(25) { index -> temp.newFile("match-$index.txt").writeText("x") }
        val found = runBlocking {
            DirectFileBrowser.search(listOf(temp.root.absolutePath), "match", limit = 10)
        }
        assertEquals(10, found.size)
    }

    @Test
    fun `deep search can surface hidden files when asked`() {
        temp.newFile(".hidden-match").writeText("x")
        val without = runBlocking {
            DirectFileBrowser.search(listOf(temp.root.absolutePath), "hidden-match")
        }
        assertTrue(without.isEmpty())
        val with = runBlocking {
            DirectFileBrowser.search(
                listOf(temp.root.absolutePath),
                "hidden-match",
                includeHidden = true
            )
        }
        assertEquals(1, with.size)
    }

    /* ---- Share safety ---------------------------------------------------- */

    @Test
    fun `a content uri passes through unchanged`() {
        val uri = Uri.parse("content://media/external/images/1")
        assertEquals(uri, ShareUri.toShareable(context, uri))
    }

    @Test
    fun `a file uri is never handed to another app unchanged`() {
        // The safety contract, asserted on every host: a `file://` URI must
        // either become a `content://` URI served by our own FileProvider, or
        // degrade to null. Returning the `file://` URI would throw
        // FileUriExposedException inside the receiving app, and throwing here
        // would take down the share sheet. Root matching itself is exercised
        // on-device, where the provider's canonical paths are real.
        val file = File(context.cacheDir, "share-me.txt")
        file.parentFile?.mkdirs()
        file.writeText("hello")

        val shareable = ShareUri.toShareable(context, Uri.fromFile(file))
        if (shareable != null) {
            assertEquals("content", shareable.scheme)
            assertTrue(
                "authority must be the app's own FileProvider",
                shareable.authority.orEmpty().endsWith(".fileprovider")
            )
            assertFalse("a file uri must never be exported as-is", shareable.scheme == "file")
        }
    }

    @Test
    fun `a path outside every provider root never throws`() {
        // Sharing must degrade, not crash: a null result lets the caller show
        // an error, while an exception would take the share sheet down.
        val outside = temp.newFile("outside-root.txt").apply { writeText("x") }
        ShareUri.toShareable(context, Uri.fromFile(outside))
    }

    @Test
    fun `a missing file uri is not shareable`() {
        val missing = File(temp.root, "gone.txt")
        assertTrue(ShareUri.toShareable(context, Uri.fromFile(missing)) == null)
    }

    @Test
    fun `a null uri is not shareable`() {
        assertTrue(ShareUri.toShareable(context, null) == null)
    }

    /* ---- Filename resolution -------------------------------------------- */

    @Test
    fun `compound suffixes beat their final extension`() {
        assertEquals("tar.gz", FileNameResolver.resolveKey("backup.tar.gz"))
        assertEquals("tar.bz2", FileNameResolver.resolveKey("archive.tar.bz2"))
        assertEquals("blade.php", FileNameResolver.resolveKey("welcome.blade.php"))
    }

    @Test
    fun `extensionless well-known names resolve by name`() {
        assertEquals("dockerfile", FileNameResolver.resolveKey("Dockerfile"))
        assertEquals("makefile", FileNameResolver.resolveKey("Makefile"))
    }

    @Test
    fun `paths are stripped before resolution`() {
        assertEquals("png", FileNameResolver.resolveKey("/storage/emulated/0/Pictures/a.png"))
        assertEquals("json", FileNameResolver.resolveKey("C:\\Users\\me\\data.json"))
    }
}
