package com.vennela365.viewer.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** A named entry point shown in the storage hub. */
@androidx.compose.runtime.Immutable
data class StorageShortcut(
    val id: String,
    val label: String,
    val path: String,
    val volumeId: String?,
    val isRemovable: Boolean
)

/**
 * Direct filesystem browsing over `java.io.File`.
 *
 * This is the fast path: real paths, real `lastModified`, real sizes, no
 * document-provider round trips. Items carry a `file://` URI so every existing
 * reader (hex, text, archive, media) works through the same content resolver it
 * already uses.
 */
object DirectFileBrowser {

    /** Byte ceiling for counting children so a huge folder cannot stall the UI. */
    private const val COUNT_LIMIT = 5000

    /**
     * Lists a directory. Unreadable children and permission failures are
     * reported through [Result] rather than thrown, so the UI can show a real
     * message instead of an empty list.
     */
    fun list(dir: File, showHidden: Boolean = false): Result<List<FileItem>> = runCatching {
        if (!dir.exists()) error("This folder no longer exists")
        if (!dir.isDirectory) error("This item is not a folder")
        if (!dir.canRead()) error("Access denied — grant all files access to read ${dir.name}")
        val children = dir.listFiles() ?: error("This folder could not be read")
        children.asSequence()
            .filter { showHidden || !it.name.startsWith('.') }
            .mapNotNull { child -> runCatching { fileItem(child) }.getOrNull() }
            .sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            .toList()
    }

    /** Builds a [FileItem] for one path without listing anything. */
    fun fileItem(file: File, countChildren: Boolean = false): FileItem {
        val isDir = file.isDirectory
        val name = file.name.ifBlank { file.absolutePath }
        val key = if (isDir) "" else FileNameResolver.resolveKey(name)
        return FileItem(
            id = file.absolutePath,
            name = name,
            path = file.absolutePath,
            uri = Uri.fromFile(file),
            extension = key,
            sizeBytes = if (isDir) 0L else file.length(),
            lastModified = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
            isDirectory = isDir,
            itemCount = if (isDir && countChildren) countChildren(file) else 0,
            formatInfo = FormatRegistry.lookup(key)
        )
    }

    /** Bounded child count so large directories stay responsive. */
    fun countChildren(dir: File): Int = runCatching {
        val names = dir.list() ?: return 0
        names.size.coerceAtMost(COUNT_LIMIT)
    }.getOrDefault(0)

    /** The parent of a path, or null at a volume root. */
    fun parentOf(path: String): File? = runCatching {
        File(path).parentFile?.takeIf { it.canRead() }
    }.getOrNull()

    /**
     * True when [path] is already a storage root, so the browser knows to stop
     * offering "up" and show the volume list instead.
     */
    fun isVolumeRoot(context: Context, path: String): Boolean {
        val normalized = path.trimEnd('/')
        if (normalized.isEmpty()) return true
        return StorageVolumes.enumerate(context).any { it.rootPath?.trimEnd('/') == normalized }
    }

    /**
     * Convenience entry points: every mounted volume plus the standard user
     * folders that actually exist on this device.
     */
    fun shortcuts(context: Context): List<StorageShortcut> {
        val out = mutableListOf<StorageShortcut>()
        val volumes = StorageVolumes.enumerate(context)
        volumes.filter { it.isBrowsable }.forEach { volume ->
            out += StorageShortcut(
                id = "vol:${volume.id}",
                label = volume.label,
                path = volume.rootPath.orEmpty(),
                volumeId = volume.id,
                isRemovable = volume.isRemovable
            )
        }
        val primary = volumes.firstOrNull { it.isPrimary && it.isBrowsable }?.rootPath
        if (primary != null) {
            wellKnownFolders().forEach { (label, relative) ->
                val dir = File(primary, relative)
                if (runCatching { dir.isDirectory && dir.canRead() }.getOrDefault(false)) {
                    out += StorageShortcut(
                        id = "dir:$relative",
                        label = label,
                        path = dir.absolutePath,
                        volumeId = volumes.firstOrNull { it.isPrimary }?.id,
                        isRemovable = false
                    )
                }
            }
            // Shared media written by other apps lives outside the scoped roots.
            val shared = File(primary, "Android/media")
            if (runCatching { shared.isDirectory && shared.canRead() }.getOrDefault(false)) {
                out += StorageShortcut(
                    id = "dir:Android/media",
                    label = "App media",
                    path = shared.absolutePath,
                    volumeId = volumes.firstOrNull { it.isPrimary }?.id,
                    isRemovable = false
                )
            }
        }
        return out
    }

    /** Standard user folders in display order, skipping ones that do not exist. */
    private fun wellKnownFolders(): List<Pair<String, String>> = listOf(
        "Downloads" to Environment.DIRECTORY_DOWNLOADS,
        "Documents" to Environment.DIRECTORY_DOCUMENTS,
        "Pictures" to Environment.DIRECTORY_PICTURES,
        "DCIM" to Environment.DIRECTORY_DCIM,
        "Movies" to Environment.DIRECTORY_MOVIES,
        "Music" to Environment.DIRECTORY_MUSIC,
        "Podcasts" to Environment.DIRECTORY_PODCASTS,
        "Ringtones" to Environment.DIRECTORY_RINGTONES,
        "Alarms" to Environment.DIRECTORY_ALARMS,
        "Notifications" to Environment.DIRECTORY_NOTIFICATIONS,
        "Audiobooks" to Environment.DIRECTORY_AUDIOBOOKS
    )

    /**
     * Breadcrumb trail from the volume root down to [path] so the browser can
     * offer real, tappable hops instead of a single opaque label.
     */
    fun trail(context: Context, path: String): List<Pair<String, String>> {
        val target = runCatching { File(path).canonicalFile }.getOrNull() ?: return emptyList()
        // Match the whole volume, not just its path, so the outermost crumb can
        // carry the volume's real name ("Internal shared storage") instead of
        // the last path segment, which for primary storage is just "0".
        val volume = StorageVolumes.enumerate(context)
            .filter { !it.rootPath.isNullOrBlank() }
            .filter { target.absolutePath.startsWith(it.rootPath!!.trimEnd('/')) }
            .maxByOrNull { it.rootPath!!.length }
        val rootPath = volume?.rootPath

        val crumbs = mutableListOf<Pair<String, String>>()
        var cursor: File? = target
        while (cursor != null) {
            val isRoot = rootPath != null && cursor.absolutePath == rootPath
            val label = if (isRoot) {
                volume?.label?.takeIf { it.isNotBlank() }
                    ?: StorageVolumes.guessLabel(cursor.absolutePath)
            } else {
                cursor.name.ifBlank { cursor.absolutePath }
            }
            crumbs += label to cursor.absolutePath
            if (isRoot) break
            val parent = cursor.parentFile
            cursor = if (parent != null && parent.absolutePath.length >= (rootPath?.length ?: 1)) parent else null
        }
        return crumbs.reversed()
    }

    /**
     * Directories that are never worth walking: they are enormous, mostly
     * unreadable, or hold another app's private cache.
     */
    private val SKIPPED_DIRS = setOf(
        "Android", "obb", ".thumbnails", ".cache", "cache", "lost+found",
        ".Trash", "LOST.DIR", ".nomedia"
    )

    /**
     * Device-wide search from [roots], breadth-first so shallow and therefore
     * more relevant matches arrive first.
     *
     * The walk honours [limit], reports progress through [onBatch] so the UI can
     * stream results, and cooperatively cancels when the calling coroutine is
     * cancelled. Unreadable directories are skipped rather than aborting.
     */
    suspend fun search(
        roots: List<String>,
        query: String,
        limit: Int = 400,
        includeHidden: Boolean = false,
        maxVisited: Int = 60_000,
        onBatch: (List<FileItem>) -> Unit = {}
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return@withContext emptyList()

        val results = ArrayList<FileItem>(minOf(limit, 256))
        val queue = ArrayDeque<File>()
        roots.filter { it.isNotBlank() }.forEach { root ->
            val dir = File(root)
            if (runCatching { dir.isDirectory }.getOrDefault(false)) queue.addLast(dir)
        }

        var visited = 0
        var pending = ArrayList<FileItem>(32)

        while (queue.isNotEmpty() && results.size < limit && visited < maxVisited) {
            ensureActive()
            val dir = queue.removeFirst()
            visited++
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                val name = child.name
                val hidden = name.startsWith('.')
                if (hidden && !includeHidden) continue
                if (child.isDirectory) {
                    if (name in SKIPPED_DIRS) continue
                    queue.addLast(child)
                } else if (name.lowercase(Locale.ROOT).contains(needle)) {
                    runCatching { fileItem(child) }.getOrNull()?.let { item ->
                        results += item
                        pending += item
                    }
                    if (results.size >= limit) break
                }
            }
            if (pending.size >= 16) {
                onBatch(pending)
                pending = ArrayList(32)
            }
        }
        if (pending.isNotEmpty()) onBatch(pending)
        results.sortedByDescending { it.lastModified }
    }
}
