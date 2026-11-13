package com.vennela365.viewer

import com.vennela365.viewer.model.ArchiveEntry
import com.vennela365.viewer.parser.ArchiveParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the archive tree the browser relies on.
 *
 * Many archive writers emit only file entries, so the folder a file lives in
 * never appears in the listing and the file becomes unreachable. These tests
 * pin the synthesis pass that repairs that, and the immediate-child rule the
 * browser uses to pick one level at a time.
 */
class ArchiveTreeTest {

    private fun file(path: String) = ArchiveEntry(
        name = path.substringAfterLast('/'),
        path = path,
        sizeBytes = 10L,
        isDirectory = false,
        dateStr = "Modified"
    )

    private fun dir(path: String) = ArchiveEntry(
        name = path.trimEnd('/').substringAfterLast('/'),
        path = if (path.endsWith("/")) path else "$path/",
        sizeBytes = 0L,
        isDirectory = true,
        dateStr = ""
    )

    /** Mirrors the browser's level selection so the two cannot drift apart. */
    private fun immediateChildren(all: List<ArchiveEntry>, prefix: String): List<ArchiveEntry> =
        if (prefix.isBlank()) {
            all.filter { it.path.trimEnd('/').count { c -> c == '/' } == 0 }
        } else {
            all.filter { entry ->
                entry.path.startsWith(prefix) &&
                    entry.path != prefix &&
                    !entry.path.removePrefix(prefix).trimEnd('/').contains('/')
            }
        }

    @Test
    fun `files without directory entries still get their folders`() {
        val raw = listOf(file("docs/readme.md"), file("docs/notes.txt"), file("data.csv"))
        val tree = ArchiveParser.withImpliedDirectories(raw)

        assertTrue(
            "the implied 'docs' folder must be synthesised",
            tree.any { it.isDirectory && it.path == "docs/" }
        )
        val root = immediateChildren(tree, "")
        assertEquals(
            "root must show the folder and the root file",
            listOf("data.csv", "docs"),
            root.map { it.name }.sorted()
        )
    }

    @Test
    fun `the nested file is reachable one level down`() {
        val tree = ArchiveParser.withImpliedDirectories(
            listOf(file("docs/readme.md"), file("data.csv"))
        )
        val insideDocs = immediateChildren(tree, "docs/")
        assertEquals(listOf("readme.md"), insideDocs.map { it.name })
    }

    @Test
    fun `explicit directory entries are never duplicated`() {
        val raw = listOf(dir("docs"), file("docs/readme.md"))
        val tree = ArchiveParser.withImpliedDirectories(raw)
        assertEquals(
            "exactly one 'docs/' entry",
            1,
            tree.count { it.isDirectory && it.path == "docs/" }
        )
    }

    @Test
    fun `deep nesting synthesises every ancestor`() {
        val tree = ArchiveParser.withImpliedDirectories(listOf(file("a/b/c/note.txt")))
        val dirs = tree.filter { it.isDirectory }.map { it.path }.sorted()
        assertEquals(listOf("a/", "a/b/", "a/b/c/"), dirs)
    }

    @Test
    fun `grandchildren do not leak into a parent level`() {
        val tree = ArchiveParser.withImpliedDirectories(
            listOf(file("docs/sub/note.txt"), file("docs/readme.md"))
        )
        val insideDocs = immediateChildren(tree, "docs/")
        assertEquals(
            "only the sub folder and readme belong at this level",
            listOf("readme.md", "sub"),
            insideDocs.map { it.name }.sorted()
        )
        assertFalse(
            "a grandchild must not appear while browsing its parent",
            insideDocs.any { it.path == "docs/sub/note.txt" }
        )
    }

    @Test
    fun `root level files are untouched`() {
        val raw = listOf(file("a.txt"), file("b.txt"))
        val tree = ArchiveParser.withImpliedDirectories(raw)
        assertEquals(raw.size, tree.size)
        assertEquals(raw, tree)
    }

    @Test
    fun `an empty archive stays empty`() {
        assertEquals(emptyList<ArchiveEntry>(), ArchiveParser.withImpliedDirectories(emptyList()))
    }

    @Test
    fun `an archive that already declares its folders is returned unchanged`() {
        val raw = listOf(dir("docs"), dir("img"), file("docs/readme.md"), file("img/a.png"))
        val tree = ArchiveParser.withImpliedDirectories(raw)
        assertEquals(
            "no extra entries should be added",
            raw.size,
            tree.size
        )
    }

    @Test
    fun `synthesised folders carry no size and are marked as directories`() {
        val tree = ArchiveParser.withImpliedDirectories(listOf(file("build/out/app.apk")))
        val build = tree.first { it.path == "build/" }
        assertTrue(build.isDirectory)
        assertEquals(0L, build.sizeBytes)
        assertEquals("build", build.name)
    }
}
