package com.vennela365.viewer.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.vennela365.viewer.registry.FormatInfo
import com.vennela365.viewer.registry.FormatRegistry

@Immutable
data class FileItem(
    val id: String,
    val name: String,
    val path: String = "",
    val uri: Uri? = null,
    val extension: String,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val isDirectory: Boolean = false,
    val isFavorite: Boolean = false,
    val itemCount: Int = 0,
    val formatInfo: FormatInfo = FormatRegistry.lookup(extension),
    val sampleContent: String = "",
    val imageResId: Int? = null,
    val subtitleTag: String = "",
    val archiveParentUri: Uri? = null,
    val archiveEntryPath: String? = null
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "$itemCount items"
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format("%.2f GB", gb)
        }

    val formattedDate: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - lastModified
            val hours = diff / (1000 * 60 * 60)
            return when {
                hours < 24 -> "Today " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(lastModified))
                hours < 48 -> "Yesterday"
                else -> DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(lastModified))
            }
        }
}

@Immutable
data class HexRow(
    val offset: String,
    val hexBytes: String,
    val ascii: String
)

@Immutable
data class ArchiveEntry(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val dateStr: String,
    val extension: String = if (!isDirectory) name.substringAfterLast('.', "") else ""
) {
    val formattedSize: String
        get() {
            if (isDirectory) return ""
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.1f MB", mb)
        }
}

@Immutable
data class SpreadsheetCell(
    val value: String,
    val isHeader: Boolean = false,
    val tagColor: String? = null // e.g. "green", "amber", "red"
)

@Immutable
data class SpreadsheetSheet(
    val name: String,
    val headers: List<String>,
    val rows: List<List<SpreadsheetCell>>
)
