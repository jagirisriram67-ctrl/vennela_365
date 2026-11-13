package com.vennela365.viewer.rendering

import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatInfo

/**
 * Stable renderer contract — spec §5.2.
 * Every viewer (PDF, image, code, archive…) implements this so the
 * registry can route files without UI code knowing parser details.
 */
interface FileRenderer {
    val rendererId: String
    fun canHandle(file: FileItem, info: FormatInfo): Boolean
    fun getSupportLevel(file: FileItem, info: FormatInfo): Int // 0..4
    fun getCapabilities(): Set<RendererCapability>
    suspend fun getMetadata(file: FileItem): Map<String, String>
    fun close()
}

enum class RendererCapability {
    PAGE_NAVIGATION, ZOOM, TEXT_SEARCH, SHEET_NAVIGATION,
    PLAYBACK, SEEK, ARCHIVE_BROWSING, SYNTAX_HIGHLIGHT,
    HEX_INSPECTION, METADATA_ONLY
}

/** Immutable renderer states — spec §5.2. Renderer must never fake success. */
sealed interface ViewerState {
    data object Idle : ViewerState
    data object Detecting : ViewerState
    data class Loading(val progress: Float? = null) : ViewerState
    data object Ready : ViewerState
    data class PartiallySupported(val reason: String) : ViewerState
    data object Unsupported : ViewerState
    data object PasswordRequired : ViewerState
    data class Failed(val reason: String, val canRetry: Boolean = true) : ViewerState
    data object Cancelled : ViewerState
}
