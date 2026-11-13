package com.vennela365.viewer.registry

import androidx.compose.runtime.Immutable

enum class SupportLevel(val label: String, val description: String, val level: Int) {
    DETECTED("Detected", "Identified format with safe hex dump and external launcher", 0),
    METADATA_READABLE("Metadata Readable", "Header, EXIF, tags, and structure inspection", 1),
    TEXT_EXTRACTABLE("Text Extractable", "Syntax highlighted text and line viewer", 2),
    PREVIEWABLE("Previewable", "Structured visual or layout preview", 3),
    FULLY_RENDERED("Fully Rendered", "Native high-fidelity visual rendering", 4);

    companion object {
        fun fromLevel(level: Int): SupportLevel = entries.firstOrNull { it.level == level } ?: DETECTED
    }
}

enum class FormatCategory(val displayName: String, val iconName: String) {
    DOCUMENTS("Documents", "description"),
    SPREADSHEETS("Spreadsheets", "table_chart"),
    PRESENTATIONS("Presentations", "slideshow"),
    EBOOKS("Ebooks", "menu_book"),
    IMAGES("Images", "image"),
    AUDIO("Audio", "music_note"),
    VIDEO("Video", "play_circle"),
    ARCHIVES("Archives", "folder_zip"),
    CODE("Code", "code"),
    CONFIG("Configuration", "settings_applications"),
    MARKDOWN("Markdown & Diagrams", "account_tree"),
    DATABASES("Databases & Science", "storage"),
    CAD_3D("CAD, 3D & GIS", "view_in_ar"),
    EMAIL_CONTACTS("Email & Contacts", "contact_mail"),
    FONTS("Fonts", "font_download"),
    LOGS("Logs & Diagnostics", "receipt_long"),
    BINARIES("Binaries & Executables", "memory"),
    OTHERS("Others & Legacy", "category")
}

@Immutable
data class FormatInfo(
    val extension: String,
    val name: String,
    val category: FormatCategory,
    val mimeType: String,
    val supportLevel: SupportLevel,
    val defaultRenderer: String,
    val fallbackHandler: String = "Hex & Metadata Inspector / External App",
    val description: String = "",
    val sampleFileName: String = "",
    // --- Extended registry record — spec §2.2 (all defaulted for back-compat) ---
    val canonicalId: String = extension,
    val aliases: List<String> = emptyList(),
    val isCompoundSuffix: Boolean = extension.contains('.'),
    val mimeTypes: List<String> = listOf(mimeType),
    val signatures: List<String> = emptyList(),
    val priority: Int = 0,
    val passwordRequired: Boolean = false,
    val maxSafeBytes: Long? = null,
    val limitations: String = "",
    val testFixture: String = sampleFileName,
    val securityNote: String = ""
) {
    /** Spec §2.1 numeric level 0..4. */
    val levelNumber: Int get() = supportLevel.level
}
