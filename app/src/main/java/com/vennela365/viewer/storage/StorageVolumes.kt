package com.vennela365.viewer.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.annotation.RequiresApi
import java.io.File
import java.util.Locale

/** What kind of physical or emulated volume a root belongs to. */
enum class VolumeKind { INTERNAL, SD_CARD, USB_OTG, UNKNOWN }

/**
 * A browsable storage root.
 *
 * [rootPath] is the absolute filesystem path when the volume can be reached
 * directly. It is `null` for volumes that exist but expose no path to this app.
 */
@androidx.compose.runtime.Immutable
data class StorageVolumeInfo(
    val id: String,
    val label: String,
    val kind: VolumeKind,
    val rootPath: String?,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
    val isMounted: Boolean,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toDouble() / totalBytes.toDouble()).toFloat()
    val isBrowsable: Boolean get() = isMounted && !rootPath.isNullOrBlank()
}

/**
 * Enumerates every storage volume the device exposes — built-in storage,
 * physical SD cards, and USB/OTG drives — and resolves a real browsable path
 * for each one.
 *
 * Resolution strategy, in order:
 *  1. API 30+: [StorageVolume.getDirectory] is authoritative.
 *  2. Any API: derive the volume root from `getExternalFilesDirs`, which the
 *     platform hands back per volume and which needs no permission at all.
 *  3. Fall back to well-known `/storage/<uuid>` and `/mnt/media_rw/<uuid>` paths.
 */
object StorageVolumes {

    private const val APP_DATA_MARKER = "/Android/data/"
    private const val APP_OBB_MARKER = "/Android/obb/"

    /** Every volume, primary first, then removable, each with capacity data. */
    fun enumerate(context: Context): List<StorageVolumeInfo> {
        val manager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val volumes = runCatching { manager?.storageVolumes.orEmpty() }.getOrDefault(emptyList())
        val derived = deriveRoots(context)

        val result = volumes.mapIndexedNotNull { index, volume ->
            runCatching { describe(context, volume, index, derived) }.getOrNull()
        }

        // Volumes the platform did not report but that we can still prove exist.
        val known = result.mapNotNull { it.rootPath }.toSet()
        val extras = derived.values
            .filter { it.isNotBlank() && it !in known }
            .map { path ->
                StorageVolumeInfo(
                    id = path,
                    label = guessLabel(path),
                    kind = guessKind(path, removable = true),
                    rootPath = path,
                    isRemovable = true,
                    isPrimary = false,
                    isMounted = runCatching { File(path).canRead() }.getOrDefault(false),
                    totalBytes = capacityTotal(context, File(path)),
                    freeBytes = capacityFree(context, File(path))
                )
            }

        return (result + extras)
            .distinctBy { it.rootPath ?: it.id }
            .sortedWith(compareByDescending<StorageVolumeInfo> { it.isPrimary }
                .thenByDescending { it.isMounted }
                .thenBy { it.label.lowercase(Locale.ROOT) })
    }

    /** Volumes that are mounted and directly browsable. */
    fun browsable(context: Context): List<StorageVolumeInfo> =
        enumerate(context).filter { it.isBrowsable }

    /**
     * Volume roots keyed by volume UUID, derived from the per-volume app
     * directories. Works on every supported API level without permission.
     */
    private fun deriveRoots(context: Context): Map<String, String> {
        val out = linkedMapOf<String, String>()
        val dirs = runCatching { context.getExternalFilesDirs(null) }.getOrDefault(emptyArray())
        dirs.filterNotNull().forEach { dir ->
            val path = dir.absolutePath
            val marker = when {
                path.contains(APP_DATA_MARKER) -> APP_DATA_MARKER
                path.contains(APP_OBB_MARKER) -> APP_OBB_MARKER
                else -> null
            } ?: return@forEach
            val root = path.substringBefore(marker).takeIf { it.isNotBlank() } ?: return@forEach
            val uuid = StorageVolumeUuid.fromRoot(root)
            out[uuid] = root
        }
        // The emulated primary root is always present in practice.
        runCatching { Environment.getExternalStorageDirectory()?.absolutePath }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { out.putIfAbsent("primary", it) }
        return out
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun describe(
        context: Context,
        volume: StorageVolume,
        index: Int,
        derived: Map<String, String>
    ): StorageVolumeInfo {
        val uuid = runCatching { volume.uuid }.getOrNull()
        val label = runCatching { volume.getDescription(context) }.getOrNull()
            ?: volumeLabelFallback(volume, index)
        val primary = runCatching { volume.isPrimary }.getOrDefault(false)
        val removable = runCatching { volume.isRemovable }.getOrDefault(false)
        val state = runCatching { volume.state }.getOrNull()
        val mounted = state == Environment.MEDIA_MOUNTED || state == null

        val direct = rootFromDirectory(volume)
        val byUuid = uuid?.let { derived[it] }
        val byPrimary = if (primary) derived["primary"] else null
        val root = direct
            ?: byUuid
            ?: byPrimary
            ?: guessedRoot(uuid, primary)
        val resolved = root?.takeIf { path -> runCatching { File(path).exists() }.getOrDefault(false) || direct != null }

        return StorageVolumeInfo(
            id = uuid ?: if (primary) "primary" else root ?: "volume-$index",
            label = label,
            kind = if (primary) VolumeKind.INTERNAL else if (removable) guessKind(root.orEmpty(), true) else VolumeKind.UNKNOWN,
            rootPath = resolved,
            isRemovable = removable,
            isPrimary = primary,
            isMounted = mounted,
            totalBytes = resolved?.let { capacityTotal(context, File(it)) } ?: 0L,
            freeBytes = resolved?.let { capacityFree(context, File(it)) } ?: 0L
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun rootFromDirectory(volume: StorageVolume): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching { volume.directory?.absolutePath }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun volumeLabelFallback(volume: StorageVolume, index: Int): String {
        val removable = runCatching { volume.isRemovable }.getOrDefault(false)
        val primary = runCatching { volume.isPrimary }.getOrDefault(false)
        return when {
            primary -> "Internal storage"
            removable -> "Removable storage"
            else -> "Storage ${index + 1}"
        }
    }

    private fun guessedRoot(uuid: String?, primary: Boolean): String? {
        if (primary) {
            return runCatching { Environment.getExternalStorageDirectory()?.absolutePath }.getOrNull()
        }
        val id = uuid ?: return null
        return listOf("/storage/$id", "/mnt/media_rw/$id").firstOrNull { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
    }

    /** Classifies a root path. Internal because the tests assert this mapping. */
    internal fun guessKind(path: String, removable: Boolean): VolumeKind {
        val lower = path.lowercase(Locale.ROOT)
        return when {
            lower.contains("/storage/emulated") || lower.contains("/sdcard") -> VolumeKind.INTERNAL
            lower.contains("usb") || lower.contains("otg") -> VolumeKind.USB_OTG
            removable -> VolumeKind.SD_CARD
            else -> VolumeKind.UNKNOWN
        }
    }

    /** Human label derived from a root path. Internal for the same reason. */
    internal fun guessLabel(path: String): String {
        val lower = path.lowercase(Locale.ROOT)
        return when {
            lower.contains("/storage/emulated") || lower.contains("/sdcard") -> "Internal storage"
            lower.contains("usb") || lower.contains("otg") -> "USB storage"
            else -> "SD card"
        }
    }

    /**
     * Total capacity. [android.app.usage.StorageStatsManager] is preferred
     * because it stays correct under scoped storage; raw [File] totals are the
     * fallback when the volume is not stat-able.
     */
    private fun capacityTotal(context: Context, root: File): Long {
        val manager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager ?: return 0L
        val stats = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? android.app.usage.StorageStatsManager
        val uuid = runCatching { manager.getUuidForPath(root) }.getOrNull()
        val reported = uuid?.let { runCatching { stats?.getTotalBytes(it) }.getOrNull() } ?: 0L
        if (reported > 0L) return reported
        return runCatching { root.totalSpace }.getOrDefault(0L)
    }

    private fun capacityFree(context: Context, root: File): Long {
        val manager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager ?: return 0L
        val stats = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? android.app.usage.StorageStatsManager
        val uuid = runCatching { manager.getUuidForPath(root) }.getOrNull()
        val reported = uuid?.let { runCatching { stats?.getFreeBytes(it) }.getOrNull() } ?: 0L
        if (reported > 0L) return reported
        return runCatching { root.freeSpace }.getOrDefault(0L)
    }
}

/** Parses the volume UUID out of a `/storage/<uuid>` style root path. */
internal object StorageVolumeUuid {
    fun fromRoot(root: String): String {
        val normalized = root.trimEnd('/')
        val tail = normalized.substringAfterLast('/')
        return when {
            normalized.startsWith("/storage/emulated") -> "primary"
            tail.isBlank() -> "primary"
            else -> tail
        }
    }
}
