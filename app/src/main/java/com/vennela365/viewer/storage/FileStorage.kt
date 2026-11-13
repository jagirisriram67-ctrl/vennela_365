package com.vennela365.viewer.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.vennela365.viewer.security.SafeLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Storage abstraction — spec §5.3. Renderer code never assumes a filesystem
 * path; everything flows through content URIs / bounded reads.
 */
interface FileStorage {
    suspend fun displayName(uri: Uri): String?
    suspend fun sizeBytes(uri: Uri): Long?
    suspend fun sniffBytes(uri: Uri, maxBytes: Int = SafeLimits.SNIFF_BYTES): ByteArray
}

object SafFileStorage : FileStorage {
    override suspend fun displayName(uri: Uri): String? = null // context-bound below

    override suspend fun sizeBytes(uri: Uri): Long? = null

    override suspend fun sniffBytes(uri: Uri, maxBytes: Int): ByteArray = ByteArray(0)

    suspend fun queryDisplayName(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1 && c.moveToFirst()) return@withContext c.getString(idx)
                    }
                }
            } catch (_: Exception) { }
            uri.lastPathSegment
        }

    suspend fun querySize(context: Context, uri: Uri): Long? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx != -1 && c.moveToFirst()) return@withContext c.getLong(idx)
                }
            }
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        } catch (_: Exception) { null }
    }

    suspend fun readBounded(context: Context, uri: Uri, maxBytes: Int): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val buf = ByteArray(maxBytes.coerceAtMost(64 * 1024))
                    val read = stream.read(buf)
                    if (read <= 0) ByteArray(0) else buf.copyOf(read)
                } ?: ByteArray(0)
            } catch (_: Exception) { ByteArray(0) }
        }
}

/**
 * Filename → extension resolution with compound-suffix longest-match
 * (tar.gz, tar.xz, blade.php…) + extensionless well-known names
 * (Dockerfile, Makefile, .gitignore…). Spec §2.4.
 */
object FileNameResolver {
    val compoundSuffixes = listOf(
        "tar.gz", "tgz", "tar.xz", "txz", "tar.zst", "tzst", "tar.bz2", "tar.lz4",
        "blade.php", "d.ts", "gradle.kts", "pom.xml", "package-lock.json",
        "kepub.epub", "sqlite-wal", "sqlite-shm", "winmail.dat", "nii.gz", "mtx.gz"
    ).sortedByDescending { it.length }

    val extensionlessNames = setOf(
        "dockerfile", "containerfile", "makefile", "jenkinsfile", "brewfile",
        "gemfile", "procfile", "vagrantfile", "justfile", "cmakelists.txt",
        ".env", ".gitignore", ".gitattributes", ".gitmodules", ".editorconfig",
        "build", "workspace", "cargo.toml", "cargo.lock", "go.mod", "go.sum",
        "requirements.txt", "pipfile", "cmakefile"
    )

    /** Returns normalized lookup key: compound suffix, extension, or filename. */
    fun resolveKey(fileName: String): String {
        val base = fileName.substringAfterLast('/').substringAfterLast('\\')
        val lower = base.lowercase()
        for (compound in compoundSuffixes) {
            if (lower == compound || lower.endsWith(".$compound")) return compound
        }
        if (!base.contains('.') || extensionlessNames.contains(lower)) {
            if (extensionlessNames.contains(lower)) return lower
            // Dockerfile-style: strip nothing, match by full lowercase name
            if (!base.contains('.')) return lower
        }
        // Hidden dotfiles like .env handled above; otherwise take substring after last dot
        val ext = lower.substringAfterLast('.', "")
        return ext
    }
}
