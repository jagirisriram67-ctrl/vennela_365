package com.vennela365.viewer.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

/**
 * Converts any item URI into something safe to hand to another app.
 *
 * Direct-path browsing yields `file://` URIs, and passing one of those to
 * another package throws [android.os.FileUriExposedException]. Everything
 * leaving the app therefore goes through the FileProvider, while
 * `content://` URIs from the Storage Access Framework pass through unchanged.
 */
object ShareUri {

    /** Maps [uri] to a shareable URI, or null when it cannot be exported. */
    fun toShareable(context: Context, uri: Uri?): Uri? {
        if (uri == null) return null
        if (uri.scheme == "content" || uri.scheme == "android.resource") return uri
        val path = uri.path ?: return null
        return runCatching {
            val file = java.io.File(path)
            if (!file.exists()) return null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    /**
     * Attaches [uri] as the shared stream plus the read grant. Returns false
     * when nothing shareable could be produced, so callers can show a real
     * error instead of launching an intent that will crash the receiver.
     */
    fun attachStream(context: Context, intent: Intent, uri: Uri?): Boolean {
        val shareable = toShareable(context, uri) ?: return false
        intent.putExtra(Intent.EXTRA_STREAM, shareable)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return true
    }

    /** Same as [attachStream] for a multi-item share. */
    fun attachStreams(context: Context, intent: Intent, uris: List<Uri>): Boolean {
        val shareable = uris.mapNotNull { toShareable(context, it) }
        if (shareable.isEmpty()) return false
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareable))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return true
    }

    /** A `VIEW` intent for the same content, used by "open with another app". */
    fun viewIntent(context: Context, uri: Uri?, mime: String): Intent? {
        val shareable = toShareable(context, uri) ?: return null
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(shareable, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
