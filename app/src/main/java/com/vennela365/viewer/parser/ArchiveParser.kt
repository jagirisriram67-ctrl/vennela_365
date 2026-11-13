package com.vennela365.viewer.parser

import android.content.Context
import android.net.Uri
import com.vennela365.viewer.model.ArchiveEntry
import com.vennela365.viewer.security.SafeLimits
import com.vennela365.viewer.storage.VennelaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

object ArchiveParser {

    suspend fun parseArchiveEntries(
        context: Context,
        uri: Uri?,
        extension: String = "zip"
    ): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext emptyList()

        val cacheKey = "archive_${uri}_${extension}"
        VennelaCacheManager.getCachedArchive(cacheKey)?.let { return@withContext it }

        val ext = extension.lowercase().removePrefix(".")
        val list = try {
            when {
                ext in setOf("tar.gz", "tgz", "tar.zst") -> parseTarGz(context, uri)
                ext == "tar" -> parseTar(context, uri)
                ext == "gz" -> parseSingleGz(context, uri)
                else -> parseZip(context, uri)
            }
        } catch (_: Exception) {
            parseZip(context, uri)
        }

        // Never spoof a real archive with fake entries on failure
        val complete = withImpliedDirectories(list)
        VennelaCacheManager.putCachedArchive(cacheKey, complete)
        complete
    }

    /**
     * Adds a directory entry for every folder that the archive's file paths
     * imply but the archiver never wrote explicitly.
     *
     * Many zip writers — including Java's own `ZipArchive`, `git archive` and
     * several build tools — emit only file entries, so `docs/readme.md` exists
     * with no `docs/` record. The browser builds its tree purely from entries,
     * so without this pass those files are silently unreachable. Returns a new
     * list; an archive that already declares its folders is returned unchanged
     * in content (order preserved, only missing parents appended).
     */
    internal fun withImpliedDirectories(entries: List<ArchiveEntry>): List<ArchiveEntry> {
        if (entries.isEmpty()) return entries

        val declared = entries.filter { it.isDirectory }
            .mapTo(HashSet()) { it.path.trimEnd('/') }
        val known = entries.mapTo(HashSet()) { it.path.trimEnd('/') }
        val implied = LinkedHashMap<String, ArchiveEntry>()

        entries.forEach { entry ->
            val normalized = entry.path.trimEnd('/')
            val lastSlash = normalized.lastIndexOf('/')
            if (lastSlash <= 0) return@forEach
            // Walk each ancestor prefix, e.g. a/b/c.txt -> a, a/b
            for (index in normalized.indices) {
                if (normalized[index] != '/') continue
                val parent = normalized.substring(0, index)
                if (parent.isBlank()) continue
                if (parent in known || parent in declared || parent in implied) continue
                implied[parent] = ArchiveEntry(
                    name = parent.substringAfterLast('/'),
                    path = "$parent/",
                    sizeBytes = 0L,
                    isDirectory = true,
                    dateStr = ""
                )
            }
        }

        if (implied.isEmpty()) return entries
        return entries + implied.values
    }

    /**
     * Extracts a single file entry into memory (up to safe max bounds)
     * so child files inside an archive can be viewed in code/image/markdown screens.
     */
    suspend fun extractEntry(
        context: Context,
        archiveUri: Uri,
        entryPath: String,
        maxBytes: Int = 10 * 1024 * 1024
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(archiveUri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == entryPath && !entry.isDirectory) {
                            val baos = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var totalRead = 0
                            var bytesRead = zis.read(buffer)
                            while (bytesRead != -1 && totalRead < maxBytes) {
                                baos.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                bytesRead = zis.read(buffer)
                            }
                            return@withContext baos.toByteArray()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    // ================= ZIP / APK / JAR / CBZ PARSER =================
    private fun parseZip(context: Context, uri: Uri): List<ArchiveEntry> {
        val list = ArrayList<ArchiveEntry>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                var count = 0
                while (entry != null && count < SafeLimits.MAX_ARCHIVE_ENTRIES) {
                    val rawPath = entry.name
                    if (SafeLimits.isSafeEntryPath(rawPath)) {
                        val name = rawPath.trimEnd('/').substringAfterLast('/')
                        val displayName = name.ifBlank { rawPath }
                        list.add(
                            ArchiveEntry(
                                name = displayName,
                                path = rawPath,
                                sizeBytes = if (entry.size >= 0) entry.size else 0L,
                                isDirectory = entry.isDirectory,
                                dateStr = "Modified"
                            )
                        )
                        count++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return list
    }

    // ================= POSIX USTAR TAR PARSER =================
    private fun parseTar(context: Context, uri: Uri): List<ArchiveEntry> {
        val list = ArrayList<ArchiveEntry>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            parseTarStream(stream, list)
        }
        return list
    }

    // ================= TAR.GZ / TGZ PARSER =================
    private fun parseTarGz(context: Context, uri: Uri): List<ArchiveEntry> {
        val list = ArrayList<ArchiveEntry>()
        context.contentResolver.openInputStream(uri)?.use { rawStream ->
            GZIPInputStream(rawStream).use { gzStream ->
                parseTarStream(gzStream, list)
            }
        }
        return list
    }

    private fun parseTarStream(stream: InputStream, list: ArrayList<ArchiveEntry>) {
        val header = ByteArray(512)
        var count = 0

        while (count < SafeLimits.MAX_ARCHIVE_ENTRIES) {
            var bytesRead = 0
            while (bytesRead < 512) {
                val r = stream.read(header, bytesRead, 512 - bytesRead)
                if (r < 0) return
                bytesRead += r
            }

            if (header.all { it.toInt() == 0 }) break

            val nameBytes = header.copyOfRange(0, 100)
            val rawPath = String(nameBytes).trimEnd { it == '\u0000' || it.isWhitespace() }
            if (rawPath.isBlank() || !SafeLimits.isSafeEntryPath(rawPath)) continue

            val sizeStr = String(header.copyOfRange(124, 136)).trim { it == '\u0000' || it.isWhitespace() }
            val size = sizeStr.toLongOrNull(8) ?: 0L

            val typeFlag = header[156].toInt().toChar()
            val isDir = typeFlag == '5' || rawPath.endsWith('/')

            val name = rawPath.trimEnd('/').substringAfterLast('/')
            list.add(
                ArchiveEntry(
                    name = name.ifBlank { rawPath },
                    path = rawPath,
                    sizeBytes = size,
                    isDirectory = isDir,
                    dateStr = "TAR Entry"
                )
            )
            count++

            if (size > 0 && !isDir) {
                val remainder = size % 512
                val padding = if (remainder != 0L) 512 - remainder else 0L
                val totalToSkip = size + padding
                var skippedTotal = 0L
                while (skippedTotal < totalToSkip) {
                    val s = stream.skip(totalToSkip - skippedTotal)
                    if (s <= 0) break
                    skippedTotal += s
                }
            }
        }
    }

    // ================= SINGLE GZ PARSER =================
    private fun parseSingleGz(context: Context, uri: Uri): List<ArchiveEntry> {
        val name = uri.lastPathSegment?.removeSuffix(".gz") ?: "decompressed_data"
        return listOf(
            ArchiveEntry(
                name = name,
                path = name,
                sizeBytes = 0L,
                isDirectory = false,
                dateStr = "GZ Stream"
            )
        )
    }

    fun getSampleArchiveEntries(): List<ArchiveEntry> = listOf(
        ArchiveEntry("src", "src/", 0, true, "10 Oct 2026"),
        ArchiveEntry("assets", "assets/", 0, true, "08 Oct 2026"),
        ArchiveEntry("gradle", "gradle/", 0, true, "05 Oct 2026"),
        ArchiveEntry("build.gradle.kts", "build.gradle.kts", 4520, false, "10 Oct 2026"),
        ArchiveEntry("settings.gradle.kts", "settings.gradle.kts", 820, false, "10 Oct 2026"),
        ArchiveEntry("AndroidManifest.xml", "src/main/AndroidManifest.xml", 2150, false, "09 Oct 2026"),
        ArchiveEntry("MainActivity.kt", "src/main/java/MainActivity.kt", 6840, false, "10 Oct 2026"),
        ArchiveEntry("README.md", "README.md", 3200, false, "06 Oct 2026"),
        ArchiveEntry("banner.webp", "assets/banner.webp", 145000, false, "07 Oct 2026")
    )
}
