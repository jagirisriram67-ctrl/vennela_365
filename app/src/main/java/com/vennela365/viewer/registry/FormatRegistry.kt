package com.vennela365.viewer.registry

import com.vennela365.viewer.storage.FileNameResolver

// Short aliases for compact seed table (top-level, since typealias inside object is not supported)
private typealias C = FormatCategory
private typealias L = SupportLevel

/**
 * Vennela 365 format registry — spec §2.
 *
 * - Target: 1,000+ distinct recognized extension/compound-suffix patterns.
 * - Honest levels L0..L4 (never claim unverified rendering).
 * - Compound suffixes (tar.gz…), extensionless names (Dockerfile…),
 *   magic-signature hints, dedupe + validation, live dashboard counts.
 */
object FormatRegistry {

    private val registry = LinkedHashMap<String, FormatInfo>()

    private data class S(
        val ext: String,
        val name: String,
        val cat: FormatCategory,
        val mime: String,
        val lvl: SupportLevel,
        val renderer: String,
        val desc: String = "",
        val sig: String = ""
    )

    // ---- Renderers (honest, size-budget aware) ----
    private const val R_PDF = "Native PdfRenderer"
    private const val R_TEXT = "Native Text Reader"
    private const val R_MD = "Native Markdown Rich Renderer"
    private const val R_CSV = "Native Interactive Grid"
    private const val R_IMG = "Native Image Canvas with Pinch-Zoom"
    private const val R_MEDIA = "Native Media3 Player"
    private const val R_ARC = "Safe Archive Tree Browser"
    private const val R_OFFICE = "Structured Office Parser"
    private const val R_CODE = "Syntax Highlighting Engine"
    private const val R_CFG = "Formatted Tree & Syntax Engine"
    private const val R_HEX = "Safe Hex & Header Inspector"
    private const val R_META = "Header & Metadata Inspector"
    private const val R_MAIL = "RFC822 Message Viewer"
    private const val R_FONT = "Typeface Specimen Preview"
    private const val R_DIAGRAM = "Vector Diagram Engine"

    init {
        for (s in catalogue()) addSeed(s)
    }

    private fun addSeed(s: S) {
        val key = s.ext.lowercase().trim().removePrefix(".")
        if (key.isBlank() || key.contains('?') || key.contains(' ') || key.contains('*')) return
        if (registry.containsKey(key)) return // first curated record wins
        registry[key] = FormatInfo(
            extension = key,
            name = s.name,
            category = s.cat,
            mimeType = s.mime,
            supportLevel = s.lvl,
            defaultRenderer = s.renderer,
            fallbackHandler = "Hex & Metadata / Compatible App",
            description = s.desc,
            sampleFileName = "sample.$key",
            canonicalId = key,
            isCompoundSuffix = key.contains('.') || FileNameResolver.compoundSuffixes.contains(key),
            mimeTypes = listOf(s.mime),
            signatures = if (s.sig.isNotBlank()) listOf(s.sig) else emptyList()
        )
    }

    // ============ LOOKUP API (back-compat + extended) ============

    fun lookup(extension: String): FormatInfo {
        val clean = extension.lowercase().trim().removePrefix(".")
        if (clean.isBlank()) return unknown("")
        return registry[clean] ?: unknown(clean)
    }

    /** Filename-aware resolution: compound suffix longest-match + extensionless names. */
    fun lookupForFile(fileName: String): FormatInfo {
        val key = FileNameResolver.resolveKey(fileName)
        registry[key]?.let { return it }
        return lookup(key.substringAfterLast('.', key))
    }

    private fun unknown(clean: String) = FormatInfo(
        extension = clean,
        name = if (clean.isNotBlank()) "${clean.uppercase()} File" else "Unknown File",
        category = C.OTHERS,
        mimeType = "application/octet-stream",
        supportLevel = L.DETECTED,
        defaultRenderer = R_HEX,
        fallbackHandler = "External App",
        description = "Unregistered or unknown binary format",
        sampleFileName = "file.$clean"
    )

    fun getAllFormats(): List<FormatInfo> = registry.values.toList()

    fun getByCategory(category: FormatCategory): List<FormatInfo> =
        registry.values.filter { it.category == category }

    fun search(query: String): List<FormatInfo> {
        val q = query.lowercase().trim()
        if (q.isBlank()) return registry.values.toList()
        return registry.values.filter {
            it.extension.contains(q) || it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) || it.category.displayName.lowercase().contains(q)
        }
    }

    val totalCount: Int get() = registry.size

    // ============ VALIDATION + DASHBOARD (spec §11) ============

    /** Registry self-check: malformed keys must never ship. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        for ((k, v) in registry) {
            if (k != k.lowercase()) errors += "not-normalized: $k"
            if (k.isBlank()) errors += "blank-key"
            if (v.extension != k) errors += "key-mismatch: $k"
        }
        return errors
    }

    data class Dashboard(
        val totalPatterns: Int,
        val uniqueSuffixes: Int,
        val compoundSuffixes: Int,
        val extensionlessNames: Int,
        val signatureRules: Int,
        val rendererTypes: Int,
        val byLevel: Map<Int, Int>,
        val byCategory: Map<String, Int>
    )

    fun dashboard(): Dashboard {
        val vals = registry.values.toList()
        return Dashboard(
            totalPatterns = vals.size,
            uniqueSuffixes = vals.map { it.extension }.toSet().size,
            compoundSuffixes = vals.count { it.isCompoundSuffix },
            extensionlessNames = vals.count { !it.extension.contains('.') && !it.extension.matches(Regex("[a-z0-9]{1,8}")) },
            signatureRules = vals.sumOf { it.signatures.size },
            rendererTypes = vals.map { it.defaultRenderer }.toSet().size,
            byLevel = (0..4).associateWith { lv -> vals.count { it.levelNumber == lv } },
            byCategory = vals.groupBy { it.category.displayName }.mapValues { it.value.size }
        )
    }

    /** Bounded magic-byte sniff → human-readable container guess. */
    fun signatureDetect(bytes: ByteArray): String {
        if (bytes.size < 4) return "Unknown"
        val b = IntArray(4) { i -> if (i < bytes.size) bytes[i].toInt() and 0xFF else -1 }
        return when {
            b[0] == 0x50 && b[1] == 0x4B && b[2] == 0x03 && b[3] == 0x04 -> "ZIP / Office Open XML / APK"
            b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46 -> "PDF Document"
            b[0] == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47 -> "PNG Image"
            b[0] == 0xFF && b[1] == 0xD8 && b[2] == 0xFF -> "JPEG Image"
            b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 -> "GIF Image"
            b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46 -> "RIFF (WAV/AVI/WEBP)"
            b[0] == 0x7F && b[1] == 0x45 && b[2] == 0x4C && b[3] == 0x46 -> "ELF Binary"
            b[0] == 0x4D && b[1] == 0x5A -> "Windows PE Executable"
            b[0] == 0xCA && b[1] == 0xFE && b[2] == 0xBA && b[3] == 0xBE -> "Java Class"
            b[0] == 0x1F && b[1] == 0x8B -> "GZIP Stream"
            b[0] == 0x37 && b[1] == 0x7A && b[2] == 0xBC && b[3] == 0xAF -> "7-Zip Archive"
            b[0] == 0x52 && b[1] == 0x61 && b[2] == 0x72 && b[3] == 0x21 -> "RAR Archive"
            else -> "Binary Stream"
        }
    }

    // ============ CATALOGUE (prompt §2.3, cleaned + deduped) ============

    private fun catalogue(): List<S> = buildList {
        // ---- A. PDF / word-processing / office ----
        add(S("pdf", "PDF Document", C.DOCUMENTS, "application/pdf", L.FULLY_RENDERED, R_PDF, "Adobe Portable Document Format", "25 50 44 46"))
        add(S("doc", "Word 97-2003 Document", C.DOCUMENTS, "application/msword", L.PREVIEWABLE, R_OFFICE, sig = "D0 CF 11 E0"))
        add(S("docx", "Word Document", C.DOCUMENTS, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("docm", "Word Macro Document", C.DOCUMENTS, "application/vnd.ms-word.document.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("dot", "Word Legacy Template", C.DOCUMENTS, "application/msword", L.PREVIEWABLE, R_OFFICE))
        add(S("dotx", "Word XML Template", C.DOCUMENTS, "application/vnd.openxmlformats-officedocument.wordprocessingml.template", L.PREVIEWABLE, R_OFFICE))
        add(S("dotm", "Word Macro Template", C.DOCUMENTS, "application/vnd.ms-word.template.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("rtf", "Rich Text Format", C.DOCUMENTS, "application/rtf", L.PREVIEWABLE, R_OFFICE, sig = "7B 5C 72 74 66"))
        add(S("odt", "OpenDocument Text", C.DOCUMENTS, "application/vnd.oasis.opendocument.text", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("ott", "OpenDocument Text Template", C.DOCUMENTS, "application/vnd.oasis.opendocument.text-template", L.PREVIEWABLE, R_OFFICE))
        add(S("fodt", "Flat OpenDocument Text", C.DOCUMENTS, "application/vnd.oasis.opendocument.text", L.PREVIEWABLE, R_OFFICE))
        add(S("sxw", "StarOffice Writer", C.DOCUMENTS, "application/vnd.sun.xml.writer", L.PREVIEWABLE, R_OFFICE))
        add(S("stw", "StarOffice Writer Template", C.DOCUMENTS, "application/vnd.sun.xml.writer.template", L.METADATA_READABLE, R_META))
        add(S("wps", "WPS / Works Document", C.DOCUMENTS, "application/vnd.ms-works", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wpt", "WPS Template", C.DOCUMENTS, "application/kswps", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("abw", "AbiWord Document", C.DOCUMENTS, "application/x-abiword", L.PREVIEWABLE, R_OFFICE))
        add(S("zabw", "Compressed AbiWord", C.DOCUMENTS, "application/x-abiword", L.PREVIEWABLE, R_OFFICE, sig = "1F 8B"))
        add(S("hwp", "Hangul Word Processor", C.DOCUMENTS, "application/x-hwp", L.TEXT_EXTRACTABLE, R_TEXT, sig = "D0 CF 11 E0"))
        add(S("hwpx", "Hangul Word XML", C.DOCUMENTS, "application/haansofthwpx", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("pages", "Apple Pages", C.DOCUMENTS, "application/x-iwork-pages-sffpages", L.PREVIEWABLE, R_OFFICE, "Package preview only"))
        add(S("cwk", "ClarisWorks Document", C.DOCUMENTS, "application/clarisworks", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wpd", "WordPerfect Document", C.DOCUMENTS, "application/vnd.wordperfect", L.TEXT_EXTRACTABLE, R_TEXT, sig = "FF 57 50 43"))
        add(S("lwp", "Lotus Word Pro", C.DOCUMENTS, "application/vnd.lotus-wordpro", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("sam", "Ami Pro Document", C.DOCUMENTS, "application/x-sam", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("sdw", "StarOffice Writer 5", C.DOCUMENTS, "application/x-sdw", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("xps", "XML Paper Specification", C.DOCUMENTS, "application/vnd.ms-xpsdocument", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("oxps", "Open XPS", C.DOCUMENTS, "application/oxps", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("djvu", "DjVu Document", C.DOCUMENTS, "image/vnd.djvu", L.METADATA_READABLE, R_META, sig = "41 54 26 54"))
        add(S("djv", "DjVu Alias", C.DOCUMENTS, "image/vnd.djvu", L.METADATA_READABLE, R_META))
        add(S("chm", "Compiled HTML Help", C.DOCUMENTS, "application/vnd.ms-htmlhelp", L.PREVIEWABLE, R_ARC, sig = "49 54 53 46"))
        add(S("wri", "Windows Write", C.DOCUMENTS, "application/x-mswrite", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("psw", "Pocket Word", C.DOCUMENTS, "application/x-psw", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("kdoc", "KWord Document", C.DOCUMENTS, "application/x-kword", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("mw", "MacWrite", C.DOCUMENTS, "application/x-macwrite", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("tex", "LaTeX Source", C.MARKDOWN, "application/x-tex", L.FULLY_RENDERED, R_CODE, "Source view; no compilation"))
        add(S("latex", "LaTeX Document", C.MARKDOWN, "application/x-latex", L.FULLY_RENDERED, R_CODE))
        add(S("ltx", "LaTeX Include", C.MARKDOWN, "application/x-tex", L.FULLY_RENDERED, R_CODE))
        add(S("dvi", "Device Independent File", C.DOCUMENTS, "application/x-dvi", L.METADATA_READABLE, R_META, sig = "F7 02"))
        add(S("ps", "PostScript", C.DOCUMENTS, "application/postscript", L.PREVIEWABLE, R_META, sig = "25 21 50 53"))
        add(S("eps", "Encapsulated PostScript", C.IMAGES, "application/postscript", L.PREVIEWABLE, R_META, sig = "25 21 50 53"))

        // ---- A2. Google Workspace shortcut pointers ----
        // These are tiny JSON pointers stored in Drive, never the document
        // itself. They are read as text/metadata and clearly labelled as cloud
        // shortcuts instead of pretending to be an offline document.
        add(S("gdoc", "Google Docs Shortcut", C.DOCUMENTS, "application/vnd.google-apps.document", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut: points at a Google Docs file"))
        add(S("gsheet", "Google Sheets Shortcut", C.SPREADSHEETS, "application/vnd.google-apps.spreadsheet", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut: points at a Google Sheet"))
        add(S("gslides", "Google Slides Shortcut", C.PRESENTATIONS, "application/vnd.google-apps.presentation", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut: points at a Google Slides deck"))
        add(S("gdraw", "Google Drawing Shortcut", C.IMAGES, "application/vnd.google-apps.drawing", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut: points at a Google Drawing"))
        add(S("gform", "Google Forms Shortcut", C.DOCUMENTS, "application/vnd.google-apps.form", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut: points at a Google Form"))
        add(S("gsite", "Google Sites Shortcut", C.DOCUMENTS, "application/vnd.google-apps.site", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut: points at a Google Site"))
        add(S("gshortcut", "Google Drive Shortcut", C.DOCUMENTS, "application/vnd.google-apps.shortcut", L.TEXT_EXTRACTABLE, R_META, "Cloud shortcut to another Drive item"))

        // ---- B. Spreadsheets ----
        add(S("xls", "Excel 97-2003 Workbook", C.SPREADSHEETS, "application/vnd.ms-excel", L.PREVIEWABLE, R_OFFICE, sig = "D0 CF 11 E0"))
        add(S("xlsx", "Excel Workbook", C.SPREADSHEETS, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("xlsm", "Excel Macro Workbook", C.SPREADSHEETS, "application/vnd.ms-excel.sheet.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("xlsb", "Excel Binary Workbook", C.SPREADSHEETS, "application/vnd.ms-excel.sheet.binary.macroEnabled.12", L.METADATA_READABLE, R_META))
        add(S("xlt", "Excel Legacy Template", C.SPREADSHEETS, "application/vnd.ms-excel", L.PREVIEWABLE, R_OFFICE))
        add(S("xltx", "Excel Template", C.SPREADSHEETS, "application/vnd.openxmlformats-officedocument.spreadsheetml.template", L.PREVIEWABLE, R_OFFICE))
        add(S("xltm", "Excel Macro Template", C.SPREADSHEETS, "application/vnd.ms-excel.template.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("xlam", "Excel Add-in", C.SPREADSHEETS, "application/vnd.ms-excel.addin.macroEnabled.12", L.METADATA_READABLE, R_META, "Never executed"))
        add(S("xla", "Excel Legacy Add-in", C.SPREADSHEETS, "application/vnd.ms-excel", L.METADATA_READABLE, R_META, "Never executed"))
        add(S("xll", "Excel DLL Add-in", C.SPREADSHEETS, "application/x-excel", L.DETECTED, R_HEX, "Never executed"))
        add(S("xlc", "Excel Legacy Chart", C.SPREADSHEETS, "application/vnd.ms-excel", L.METADATA_READABLE, R_META))
        add(S("xlw", "Excel Workspace", C.SPREADSHEETS, "application/vnd.ms-excel", L.METADATA_READABLE, R_META))
        add(S("numbers", "Apple Numbers", C.SPREADSHEETS, "application/x-iwork-numbers-sffnumbers", L.PREVIEWABLE, R_OFFICE, "Package preview only"))
        add(S("ods", "OpenDocument Spreadsheet", C.SPREADSHEETS, "application/vnd.oasis.opendocument.spreadsheet", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("ots", "OpenDocument Sheet Template", C.SPREADSHEETS, "application/vnd.oasis.opendocument.spreadsheet-template", L.PREVIEWABLE, R_OFFICE))
        add(S("fods", "Flat OpenDocument Sheet", C.SPREADSHEETS, "application/vnd.oasis.opendocument.spreadsheet", L.PREVIEWABLE, R_OFFICE))
        add(S("sxc", "StarOffice Calc", C.SPREADSHEETS, "application/vnd.sun.xml.calc", L.PREVIEWABLE, R_OFFICE))
        add(S("csv", "Comma-Separated Values", C.SPREADSHEETS, "text/csv", L.FULLY_RENDERED, R_CSV))
        add(S("tsv", "Tab-Separated Values", C.SPREADSHEETS, "text/tab-separated-values", L.FULLY_RENDERED, R_CSV))
        add(S("dsv", "Delimiter-Separated Values", C.SPREADSHEETS, "text/plain", L.FULLY_RENDERED, R_CSV))
        add(S("tab", "Tab Data File", C.SPREADSHEETS, "text/plain", L.FULLY_RENDERED, R_CSV))
        add(S("dif", "Data Interchange Format", C.SPREADSHEETS, "application/x-dif", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("slk", "SYLK Spreadsheet", C.SPREADSHEETS, "application/x-sylk", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("sylk", "SYLK Alias", C.SPREADSHEETS, "application/x-sylk", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("prn", "Lotus Print File", C.SPREADSHEETS, "application/x-prn", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("dbf", "dBASE Table", C.SPREADSHEETS, "application/x-dbf", L.PREVIEWABLE, R_OFFICE, sig = "03"))
        add(S("wks", "Lotus 1-2-3 Sheet", C.SPREADSHEETS, "application/vnd.lotus-1-2-3", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wk1", "Lotus Worksheet v2", C.SPREADSHEETS, "application/x-123", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wk3", "Lotus Worksheet v4", C.SPREADSHEETS, "application/x-123", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wk4", "Lotus Worksheet v5", C.SPREADSHEETS, "application/x-123", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wq1", "Quattro Pro Sheet", C.SPREADSHEETS, "application/x-quattropro", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("wq2", "Quattro Pro v5", C.SPREADSHEETS, "application/x-quattropro", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("qpw", "Quattro Pro 9+", C.SPREADSHEETS, "application/x-quattropro", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("et", "WPS Spreadsheets", C.SPREADSHEETS, "application/kset", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("ett", "WPS Sheet Template", C.SPREADSHEETS, "application/kset", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("123", "Lotus 1-2-3 97", C.SPREADSHEETS, "application/vnd.lotus-1-2-3", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("gnumeric", "Gnumeric Sheet", C.SPREADSHEETS, "application/x-gnumeric", L.PREVIEWABLE, R_OFFICE, sig = "1F 8B"))
        add(S("pxl", "Pocket Excel", C.SPREADSHEETS, "application/x-pxl", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("uos", "Uniform Office Sheet", C.SPREADSHEETS, "application/x-uos", L.TEXT_EXTRACTABLE, R_TEXT))

        // ---- C. Presentations ----
        add(S("ppt", "PowerPoint 97-2003", C.PRESENTATIONS, "application/vnd.ms-powerpoint", L.PREVIEWABLE, R_OFFICE, sig = "D0 CF 11 E0"))
        add(S("pptx", "PowerPoint Presentation", C.PRESENTATIONS, "application/vnd.openxmlformats-officedocument.presentationml.presentation", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("pptm", "PowerPoint Macro Deck", C.PRESENTATIONS, "application/vnd.ms-powerpoint.presentation.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("pps", "PowerPoint Show", C.PRESENTATIONS, "application/vnd.ms-powerpoint", L.PREVIEWABLE, R_OFFICE))
        add(S("ppsx", "PowerPoint XML Show", C.PRESENTATIONS, "application/vnd.openxmlformats-officedocument.presentationml.slideshow", L.PREVIEWABLE, R_OFFICE))
        add(S("ppsm", "PowerPoint Macro Show", C.PRESENTATIONS, "application/vnd.ms-powerpoint.slideshow.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("pot", "PowerPoint Legacy Template", C.PRESENTATIONS, "application/vnd.ms-powerpoint", L.PREVIEWABLE, R_OFFICE))
        add(S("potx", "PowerPoint Template", C.PRESENTATIONS, "application/vnd.openxmlformats-officedocument.presentationml.template", L.PREVIEWABLE, R_OFFICE))
        add(S("potm", "PowerPoint Macro Template", C.PRESENTATIONS, "application/vnd.ms-powerpoint.template.macroEnabled.12", L.PREVIEWABLE, R_OFFICE, "Macros never executed"))
        add(S("odp", "OpenDocument Presentation", C.PRESENTATIONS, "application/vnd.oasis.opendocument.presentation", L.PREVIEWABLE, R_OFFICE, sig = "PK"))
        add(S("otp", "OpenDocument Pres Template", C.PRESENTATIONS, "application/vnd.oasis.opendocument.presentation-template", L.PREVIEWABLE, R_OFFICE))
        add(S("fodp", "Flat OpenDocument Pres", C.PRESENTATIONS, "application/vnd.oasis.opendocument.presentation", L.PREVIEWABLE, R_OFFICE))
        add(S("sxi", "StarOffice Impress", C.PRESENTATIONS, "application/vnd.sun.xml.impress", L.PREVIEWABLE, R_OFFICE))
        add(S("sti", "StarOffice Impress Template", C.PRESENTATIONS, "application/vnd.sun.xml.impress.template", L.METADATA_READABLE, R_META))
        add(S("key", "Apple Keynote", C.PRESENTATIONS, "application/x-iwork-keynote-sffkey", L.PREVIEWABLE, R_OFFICE, "Package preview only"))
        add(S("keynote", "Keynote Alias", C.PRESENTATIONS, "application/x-iwork-keynote-sffkey", L.PREVIEWABLE, R_OFFICE))
        add(S("show", "Haansoft Show", C.PRESENTATIONS, "application/x-show", L.METADATA_READABLE, R_META))
        add(S("shw", "Corel Show", C.PRESENTATIONS, "application/x-shw", L.METADATA_READABLE, R_META))

        // ---- D. Ebooks / comics ----
        add(S("epub", "EPUB Ebook", C.EBOOKS, "application/epub+zip", L.PREVIEWABLE, "EPUB Chapter Flow Reader", sig = "PK"))
        add(S("mobi", "Mobipocket Ebook", C.EBOOKS, "application/x-mobipocket-ebook", L.PREVIEWABLE, R_OFFICE))
        add(S("azw", "Kindle Ebook", C.EBOOKS, "application/vnd.amazon.ebook", L.PREVIEWABLE, R_OFFICE, "DRM never circumvented"))
        add(S("azw3", "Kindle Format 8", C.EBOOKS, "application/vnd.amazon.ebook", L.PREVIEWABLE, R_OFFICE, "DRM never circumvented"))
        add(S("azw4", "Kindle Print Replica", C.EBOOKS, "application/vnd.amazon.ebook", L.METADATA_READABLE, R_META, "DRM never circumvented"))
        add(S("kfx", "Kindle KFX", C.EBOOKS, "application/vnd.amazon.mobi8-ebook", L.METADATA_READABLE, R_META, "DRM never circumvented"))
        add(S("prc", "Palm Reader Ebook", C.EBOOKS, "application/x-mobipocket-ebook", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("fb2", "FictionBook 2", C.EBOOKS, "application/x-fictionbook+xml", L.PREVIEWABLE, R_MD, sig = "3C 3F 78 6D 6C"))
        add(S("fb3", "FictionBook 3", C.EBOOKS, "application/x-fictionbook", L.PREVIEWABLE, R_MD, sig = "PK"))
        add(S("lit", "MS Reader Ebook", C.EBOOKS, "application/x-ms-reader", L.METADATA_READABLE, R_META, sig = "49 54 4F 4C"))
        add(S("lrf", "Sony BBeB Ebook", C.EBOOKS, "application/x-sony-bbeb", L.METADATA_READABLE, R_META))
        add(S("rb", "Rocket Ebook", C.EBOOKS, "application/x-rocket", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("cbz", "Comic ZIP", C.EBOOKS, "application/vnd.comicbook+zip", L.FULLY_RENDERED, R_ARC, sig = "PK"))
        add(S("cbr", "Comic RAR", C.EBOOKS, "application/vnd.comicbook-rar", L.PREVIEWABLE, R_ARC, sig = "52 61 72 21"))
        add(S("cb7", "Comic 7Z", C.EBOOKS, "application/x-cb7", L.PREVIEWABLE, R_ARC, sig = "37 7A BC AF"))
        add(S("cbt", "Comic TAR", C.EBOOKS, "application/x-cbt", L.FULLY_RENDERED, R_ARC))
        add(S("cba", "Comic ACE", C.EBOOKS, "application/x-cba", L.METADATA_READABLE, R_META))
        add(S("acsm", "Adobe Fulfillment Token", C.EBOOKS, "application/vnd.adobe.adept+xml", L.TEXT_EXTRACTABLE, R_TEXT, "DRM fulfillment only"))
        add(S("kepub", "Kobo EPUB", C.EBOOKS, "application/epub+zip", L.PREVIEWABLE, "EPUB Chapter Flow Reader", sig = "PK"))
        add(S("kepub.epub", "Kobo EPUB Container", C.EBOOKS, "application/epub+zip", L.PREVIEWABLE, "EPUB Chapter Flow Reader", sig = "PK"))
        add(S("opf", "EPUB Package File", C.EBOOKS, "application/oebps-package+xml", L.TEXT_EXTRACTABLE, R_CFG))
        add(S("ncx", "EPUB Navigation File", C.EBOOKS, "application/x-dtbncx+xml", L.TEXT_EXTRACTABLE, R_CFG))

        // ---- E. Plain text / markup ----
        add(S("txt", "Plain Text", C.DOCUMENTS, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("text", "Plain Text Document", C.DOCUMENTS, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("log", "Log File", C.LOGS, "text/plain", L.FULLY_RENDERED, "Log Stream Viewer"))
        add(S("out", "Process Output", C.LOGS, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("err", "Error Output", C.LOGS, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("md", "Markdown", C.MARKDOWN, "text/markdown", L.FULLY_RENDERED, R_MD))
        add(S("markdown", "Markdown Document", C.MARKDOWN, "text/markdown", L.FULLY_RENDERED, R_MD))
        add(S("mdown", "Markdown Variant", C.MARKDOWN, "text/markdown", L.FULLY_RENDERED, R_MD))
        add(S("mkd", "Markdown Short", C.MARKDOWN, "text/markdown", L.FULLY_RENDERED, R_MD))
        add(S("mkdn", "Markdown Alt", C.MARKDOWN, "text/markdown", L.FULLY_RENDERED, R_MD))
        add(S("mdx", "MDX Document", C.MARKDOWN, "text/markdown", L.FULLY_RENDERED, R_MD))
        add(S("rst", "reStructuredText", C.MARKDOWN, "text/x-rst", L.FULLY_RENDERED, R_MD))
        add(S("rest", "reST Alias", C.MARKDOWN, "text/x-rst", L.FULLY_RENDERED, R_MD))
        add(S("adoc", "AsciiDoc", C.MARKDOWN, "text/asciidoc", L.FULLY_RENDERED, R_MD))
        add(S("asciidoc", "AsciiDoc Full", C.MARKDOWN, "text/asciidoc", L.FULLY_RENDERED, R_MD))
        add(S("asc", "PGP Text / AsciiDoc", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("org", "Org Mode", C.MARKDOWN, "text/org", L.FULLY_RENDERED, R_TEXT))
        add(S("wiki", "Wiki Markup", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("textile", "Textile Markup", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("creole", "Creole Wiki", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_TEXT))
        add(S("pod", "Perl POD Doc", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_CODE))
        add(S("man", "Man Page Source", C.MARKDOWN, "text/troff", L.FULLY_RENDERED, R_TEXT))
        add(S("roff", "Roff Source", C.MARKDOWN, "text/troff", L.FULLY_RENDERED, R_TEXT))
        add(S("nroff", "Nroff Source", C.MARKDOWN, "text/troff", L.FULLY_RENDERED, R_TEXT))
        add(S("troff", "Troff Source", C.MARKDOWN, "text/troff", L.FULLY_RENDERED, R_TEXT))
        add(S("info", "Texinfo Source", C.MARKDOWN, "text/info", L.FULLY_RENDERED, R_TEXT))
        add(S("bib", "BibTeX Bibliography", C.MARKDOWN, "text/x-bibtex", L.FULLY_RENDERED, R_CODE))
        add(S("bibtex", "BibTeX Alias", C.MARKDOWN, "text/x-bibtex", L.FULLY_RENDERED, R_CODE))
        add(S("rst.txt", "reST Plain Bundle", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_TEXT))

        // ---- F. Web / markup sources ----
        val web = listOf(
            "html" to "text/html", "htm" to "text/html", "xhtml" to "application/xhtml+xml",
            "xht" to "application/xhtml+xml", "shtml" to "text/html", "dhtml" to "text/html",
            "css" to "text/css", "scss" to "text/x-scss", "sass" to "text/x-sass",
            "less" to "text/x-less", "styl" to "text/x-stylus", "js" to "text/javascript",
            "mjs" to "text/javascript", "cjs" to "text/javascript", "jsx" to "text/jsx",
            "ts" to "text/typescript", "tsx" to "text/tsx", "vue" to "text/x-vue",
            "svelte" to "text/x-svelte", "astro" to "text/x-astro", "php" to "application/x-php",
            "phtml" to "application/x-php", "htmx" to "text/html", "hbs" to "text/x-handlebars",
            "handlebars" to "text/x-handlebars", "mustache" to "text/x-mustache",
            "ejs" to "text/x-ejs", "njk" to "text/x-nunjucks", "liquid" to "text/x-liquid",
            "erb" to "text/x-erb", "twig" to "text/x-twig", "wasm" to "application/wasm",
            "wat" to "text/wast"
        )
        for ((e, m) in web) {
            val lvl = if (e == "wasm") L.METADATA_READABLE else L.TEXT_EXTRACTABLE
            val r = if (e == "wasm") R_HEX else R_CODE
            add(S(e, "${e.uppercase()} Web Source", C.CODE, m, lvl, r, "Source view only; scripts never executed"))
        }
        add(S("blade.php", "Laravel Blade Template", C.CODE, "text/x-blade", L.TEXT_EXTRACTABLE, R_CODE, "Source view only"))
        add(S("d.ts", "TypeScript Declaration", C.CODE, "text/typescript", L.TEXT_EXTRACTABLE, R_CODE))

        // ---- G. C/C++ / systems ----
        for (e in listOf("c", "h", "i", "ii", "cc", "cp", "cxx", "cpp", "hh", "hpp", "hxx", "h++", "c++", "inl", "ipp", "tpp", "cu", "cuh", "cl", "metal", "s", "asm", "inc", "def", "rc", "idl", "midl")) {
            add(S(e, "${e.uppercase()} Systems Source", C.CODE, "text/x-csrc", L.TEXT_EXTRACTABLE, R_CODE))
        }
        add(S("m", "Objective-C / MATLAB Source", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE, "Ambiguous; inspected as text"))
        add(S("mm", "Objective-C++ Source", C.CODE, "text/x-c++src", L.TEXT_EXTRACTABLE, R_CODE))
        add(S("bat", "Batch Script", C.CODE, "application/x-bat", L.TEXT_EXTRACTABLE, R_CODE, "View only; never executed"))
        add(S("cmd", "Command Script", C.CODE, "application/x-bat", L.TEXT_EXTRACTABLE, R_CODE, "View only; never executed"))

        // ---- H. JVM / managed ----
        for (e in listOf("java", "jav", "jsp", "jspx", "kt", "kts", "scala", "sc", "groovy", "gvy", "gy", "gsh", "clj", "cljs", "cljc", "edn", "properties", "mf", "manifest")) {
            add(S(e, "${e.uppercase()} JVM Source", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))
        }
        add(S("class", "Java Bytecode", C.BINARIES, "application/java-vm", L.METADATA_READABLE, R_HEX, "Never executed", "CA FE BA BE"))
        add(S("jar", "Java Archive", C.ARCHIVES, "application/java-archive", L.FULLY_RENDERED, R_ARC, "Contents browsed; never executed", "PK"))
        add(S("gradle", "Gradle Build Script", C.CODE, "text/x-gradle", L.TEXT_EXTRACTABLE, R_CODE))
        add(S("gradle.kts", "Gradle Kotlin DSL", C.CODE, "text/x-kotlin", L.TEXT_EXTRACTABLE, R_CODE))
        add(S("pom", "Maven Stub", C.CODE, "text/xml", L.TEXT_EXTRACTABLE, R_CFG))
        add(S("pom.xml", "Maven Project File", C.CODE, "application/xml", L.TEXT_EXTRACTABLE, R_CFG))
        add(S("mvn", "Maven Wrapper Ref", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_TEXT))

        // ---- I. Python / data science ----
        for (e in listOf("py", "pyw", "pyi", "pyx", "pxd", "pxi", "r", "rmd", "jl", "m", "do", "ado", "qmd", "rproj", "rprofile")) {
            if (e == "m") continue // already registered (ambiguous)
            add(S(e, "${e.uppercase()} Data-Science Source", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE, "Cells viewed; never executed"))
        }
        add(S("ipynb", "Jupyter Notebook", C.CODE, "application/x-ipynb+json", L.PREVIEWABLE, R_OFFICE, "Cells + outputs shown; never executed"))
        add(S("pyo", "Python Optimized Bytecode", C.BINARIES, "application/x-python", L.METADATA_READABLE, R_HEX, "Never executed"))
        add(S("pyc", "Python Bytecode", C.BINARIES, "application/x-python", L.METADATA_READABLE, R_HEX, "Never executed"))
        add(S("pyd", "Python Native Module", C.BINARIES, "application/x-python", L.DETECTED, R_HEX, "Never executed"))
        add(S("whl", "Python Wheel", C.ARCHIVES, "application/zip", L.PREVIEWABLE, R_ARC, "Never installed; browsed only", "PK"))
        add(S("egg", "Python Egg", C.ARCHIVES, "application/zip", L.PREVIEWABLE, R_ARC, "Never installed", "PK"))
        add(S("rds", "R Data Snapshot", C.DATABASES, "application/x-r-data", L.METADATA_READABLE, R_META, "Never deserialized unsafely"))
        add(S("rda", "R Data Archive", C.DATABASES, "application/x-r-data", L.METADATA_READABLE, R_META))
        add(S("rdata", "R Workspace Image", C.DATABASES, "application/x-r-data", L.METADATA_READABLE, R_META))
        add(S("mlx", "MATLAB Live Script", C.CODE, "text/xml", L.TEXT_EXTRACTABLE, R_CODE))
        add(S("mat", "MATLAB Data File", C.DATABASES, "application/x-matlab-data", L.METADATA_READABLE, R_META, "Never deserialized unsafely"))
        add(S("nb", "Notebook / Mathematica", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE, "Never executed"))
        add(S("wolfram", "Wolfram Script", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE, "Never executed"))
        add(S("wls", "Wolfram Language Script", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE, "Never executed"))
        add(S("sas", "SAS Program", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))
        add(S("sas7bdat", "SAS Dataset", C.DATABASES, "application/x-sas", L.METADATA_READABLE, R_META))
        add(S("sav", "SPSS Data File", C.DATABASES, "application/x-spss", L.METADATA_READABLE, R_META))
        add(S("por", "SPSS Portable File", C.DATABASES, "application/x-spss", L.METADATA_READABLE, R_META))
        add(S("dta", "Stata Dataset", C.DATABASES, "application/x-stata", L.METADATA_READABLE, R_META))
        add(S("sps", "SPSS Syntax", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))
        add(S("spv", "SPSS Output", C.DATABASES, "application/x-spss", L.METADATA_READABLE, R_META))

        // ---- J. Modern app languages ----
        for (e in listOf("go", "mod", "sum", "rs", "rlib", "swift", "swiftdoc", "swiftsourceinfo", "dart", "elm", "ex", "exs", "erl", "hrl", "hs", "lhs", "cabal", "ml", "mli", "re", "rei", "res", "resi", "fs", "fsi", "fsx", "vb", "bas", "cls", "frm", "frx", "vbs", "vba", "cs", "csx", "fsproj", "csproj", "sln", "vbproj", "stack")) {
            add(S(e, "${e.uppercase()} Application Source", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))
        }

        // ---- K. Other language families ----
        for (e in listOf("rb", "rake", "gemspec", "g", "pl", "pm", "t", "podspec", "lua", "luac", "hack", "hh", "hhi", "d", "di", "nim", "nims", "zig", "v", "vala", "vapi", "f", "for", "f77", "f90", "f95", "f03", "f08", "cob", "cbl", "cpy", "p", "pas", "pp", "lpr", "dpr", "adb", "ads", "ada", "pro", "prolog", "lisp", "scm", "ss", "rkt", "el", "factor", "forth", "4th", "rexx", "rex", "awk", "sed", "sol", "vy", "sv", "vhd", "vhdl", "purescript")) {
            val lvl = if (e == "luac") L.METADATA_READABLE else L.TEXT_EXTRACTABLE
            add(S(e, "${e.uppercase()} Language Source", C.CODE, "text/plain", lvl, R_CODE, "View only; never executed"))
        }

        // ---- L. Shell / automation (view only) ----
        for (e in listOf("sh", "bash", "zsh", "fish", "ksh", "csh", "tcsh", "ps1", "psm1", "psd1", "pssc", "applescript", "scpt", "expect")) {
            add(S(e, "${e.uppercase()} Shell Script", C.CODE, "text/x-shellscript", L.TEXT_EXTRACTABLE, R_CODE, "View only; never executed"))
        }

        // ---- M. Queries / schemas / APIs ----
        for (e in listOf("sql", "ddl", "dml", "plsql", "pks", "pkb", "psql", "mysql", "pgsql", "graphql", "gql", "graphqls", "prisma", "cypher", "cql", "cassandra", "sparql", "rq", "proto", "protobuf", "thrift", "avsc", "avdl", "xsd", "wsdl", "openapi", "swagger")) {
            add(S(e, "${e.uppercase()} Query / Schema", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))
        }
        add(S("prisma.schema", "Prisma Schema File", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))

        // ---- N. JSON / YAML / XML / config ----
        for (e in listOf("json", "jsonc", "json5", "geojson", "topojson", "ndjson", "jsonl", "xml", "xsd", "xsl", "xslt", "xslfo", "yaml", "yml", "toml", "ini", "cfg", "conf", "config", "env", "dotenv", "plist", "reg", "desktop", "editorconfig", "gitignore", "gitattributes", "gitmodules", "gitconfig", "hcl", "tf", "tfvars", "nomad", "cue", "dhall", "ncl")) {
            add(S(e, "${e.uppercase()} Structured Config", C.CONFIG, "application/json", L.TEXT_EXTRACTABLE, R_CFG))
        }
        add(S("bson", "BSON Document", C.DATABASES, "application/bson", L.METADATA_READABLE, R_META))
        add(S("mime", "MIME Type File", C.CONFIG, "text/plain", L.TEXT_EXTRACTABLE, R_TEXT))
        add(S("mimes", "MIME Database", C.CONFIG, "text/plain", L.TEXT_EXTRACTABLE, R_TEXT))
        // Extensionless well-known names
        for (e in listOf("dockerfile", "containerfile", "makefile", "jenkinsfile", "brewfile", "gemfile", "procfile", "vagrantfile", "justfile", ".env", ".gitignore", ".editorconfig", ".gitattributes", ".gitmodules")) {
            add(S(e, "Well-known $e", C.CONFIG, "text/plain", L.TEXT_EXTRACTABLE, R_CODE, "Extensionless project file"))
        }

        // ---- O. Build / infra ----
        for (e in listOf("build.xml", "ant", "ivy", "sbt", "project", "cmake", "mk", "make", "mak", "ninja", "meson", "bazel", "bzl", "bzlmod", "build", "workspace", "workspace.bazel", "dockerignore", "ansible", "j2", "helm", "chart", "lock", "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "poetry.lock", "pipfile", "pipfile.lock", "requirements.txt", "cargo.lock", "cargo.toml", "go.mod", "go.sum", "gemfile.lock")) {
            add(S(e, "Build File $e", C.CODE, "text/plain", L.TEXT_EXTRACTABLE, R_CODE))
        }

        // ---- P. Raster / image ----
        val imgL4 = setOf("jpg", "jpeg", "jpe", "jfif", "png", "gif", "webp", "bmp", "heic", "heif", "ico", "svg")
        for (e in listOf("jpg", "jpeg", "jpe", "jfif", "png", "apng", "gif", "webp", "avif", "heic", "heif", "bmp", "dib", "tif", "tiff", "ico", "cur", "jxl", "jp2", "j2k", "jpf", "jpx", "jpm", "mj2", "tga", "icb", "vda", "vst", "pcx", "ppm", "pgm", "pbm", "pnm", "pam", "pfm", "hdr", "exr", "dds", "ktx", "ktx2", "astc", "basis", "bpg", "flif", "ras", "iff", "ilbm", "lbm", "wmf", "emf", "cgm", "fits", "fit", "fts", "pic", "pict", "pct", "qoi", "svg", "svgz")) {
            val lvl = if (imgL4.contains(e)) L.FULLY_RENDERED else L.PREVIEWABLE
            val sig = when (e) {
                "png" -> "89 50 4E 47"; "jpg", "jpeg", "jpe", "jfif" -> "FF D8 FF"
                "gif" -> "47 49 46 38"; "webp" -> "RIFF WEBP"; "bmp", "dib" -> "42 4D"
                "tif", "tiff" -> "49 49 2A 00"; "ico" -> "00 00 01 00"; else -> ""
            }
            add(S(e, "${e.uppercase()} Image", C.IMAGES, "image/$e", lvl, R_IMG, sig = sig))
        }
        add(S("mac", "MacPaint Image", C.IMAGES, "image/x-macpaint", L.METADATA_READABLE, R_META))

        // ---- Q. Camera RAW ----
        for (e in listOf("raw", "dng", "cr2", "cr3", "crw", "nef", "nrw", "arw", "srf", "sr2", "raf", "orf", "rw2", "rwl", "pef", "ptx", "x3f", "3fr", "fff", "mef", "mos", "mrw", "cap", "iiq", "eip", "kdc", "dcr", "erf", "bay", "r3d", "braw")) {
            add(S(e, "Camera RAW ${e.uppercase()}", C.IMAGES, "image/x-raw", L.METADATA_READABLE, R_META, "EXIF + embedded preview where decodable"))
        }

        // ---- R. Vector / design ----
        for (e in listOf("ai", "cdr", "cmx", "sk", "sk1", "afdesign", "afphoto", "afpub", "sketch", "fig", "xd", "indd", "idml", "qxd", "qxp", "pub", "vsd", "vsdx", "vsdm", "vssx", "vstx", "drawio", "dio", "excalidraw", "omnigraffle", "graffle", "psd", "psb", "xcf", "kra", "ora", "ase", "aseprite")) {
            add(S(e, "${e.uppercase()} Design File", C.CAD_3D, "application/octet-stream", L.METADATA_READABLE, R_META, "Layers/preview only when safely extractable"))
        }

        // ---- S. Animation / 3D assets ----
        add(S("aep", "After Effects Project", C.CAD_3D, "application/x-aep", L.METADATA_READABLE, R_META))
        add(S("aepx", "After Effects XML Project", C.CAD_3D, "text/xml", L.TEXT_EXTRACTABLE, R_CFG))
        add(S("prproj", "Premiere Project", C.CAD_3D, "application/octet-stream", L.METADATA_READABLE, R_META))
        add(S("drp", "DaVinci Project", C.CAD_3D, "application/octet-stream", L.METADATA_READABLE, R_META))
        add(S("blend", "Blender Scene", C.CAD_3D, "application/x-blend", L.METADATA_READABLE, R_META, sig = "42 4C 45 4E 44 45 52"))
        add(S("gltf", "glTF Scene", C.CAD_3D, "model/gltf+json", L.PREVIEWABLE, R_META))
        add(S("glb", "glTF Binary", C.CAD_3D, "model/gltf-binary", L.PREVIEWABLE, R_META, sig = "67 6C 54 46"))

        // ---- T. Audio ----
        val audL4 = setOf("mp3", "m4a", "aac", "wav", "wave", "flac", "ogg", "oga", "opus")
        for (e in listOf("mp3", "m4a", "m4b", "m4p", "aac", "wav", "wave", "flac", "ogg", "oga", "opus", "aif", "aiff", "aifc", "caf", "au", "snd", "amr", "awb", "mid", "midi", "kar", "ape", "wma", "alac", "mp2", "mp1", "ac3", "eac3", "dts", "mka", "tta", "tak", "wv", "dsf", "dff", "mpc", "spx", "ra", "ram", "voc", "mod", "xm", "it", "s3m")) {
            val lvl = if (audL4.contains(e)) L.FULLY_RENDERED else L.PREVIEWABLE
            add(S(e, "${e.uppercase()} Audio", C.AUDIO, "audio/$e", lvl, R_MEDIA, "Codec support verified at playback; never inferred from extension alone"))
        }

        // ---- U. Video ----
        val vidL4 = setOf("mp4", "m4v", "mov", "qt", "mkv", "mk3d", "webm", "3gp", "ts")
        for (e in listOf("mp4", "m4v", "mov", "qt", "mkv", "mk3d", "mka", "webm", "avi", "wmv", "asf", "flv", "f4v", "f4p", "mpg", "mpeg", "m1v", "m2v", "m2ts", "mts", "ts", "vob", "3gp", "3g2", "ogv", "ogx", "rm", "rmvb", "divx", "dv", "yuv", "mxf", "mpv", "nut", "viv", "amv", "bink", "bik", "smk")) {
            val lvl = if (vidL4.contains(e)) L.FULLY_RENDERED else L.PREVIEWABLE
            add(S(e, "${e.uppercase()} Video", C.VIDEO, "video/$e", lvl, R_MEDIA, "Seek/tracks/subtitles where Media3 exposes them"))
        }

        // ---- V. Playlists / captions ----
        for (e in listOf("m3u", "m3u8", "pls", "xspf", "wpl", "asx", "cue", "srt", "vtt", "webvtt", "ass", "ssa", "sub", "idx", "sbv", "lrc", "smi", "sami", "ttml", "dfxp", "stl")) {
            val lvl = if (e in setOf("srt", "vtt", "webvtt", "ass", "ssa", "lrc", "m3u", "m3u8", "pls")) L.FULLY_RENDERED else L.TEXT_EXTRACTABLE
            add(S(e, "${e.uppercase()} Playlist / Subtitle", C.AUDIO, "text/plain", lvl, "Timed Subtitle / Playlist Engine"))
        }

        // ---- W. Archives ----
        for (e in listOf("zip", "zipx", "7z", "rar", "tar", "tgz", "tar.gz", "gz", "gzip", "bz2", "bzip2", "xz", "txz", "tar.xz", "zst", "zstd", "tzst", "tar.zst", "lz", "lzma", "lzo", "lz4", "br", "cab", "arj", "lzh", "lha", "ace", "alz", "arc", "zoo", "pak", "sit", "sitx", "sea", "shar", "cpio", "wim", "swm", "esd", "xar", "dmg", "iso", "img", "tar.bz2", "tar.lz4")) {
            val lvl = if (e in setOf("zip", "tar", "gz", "tgz", "tar.gz")) L.FULLY_RENDERED else L.PREVIEWABLE
            val sig = when (e) {
                "zip", "zipx" -> "PK"; "7z" -> "37 7A BC AF"; "rar" -> "52 61 72 21"
                "gz", "gzip", "tgz", "tar.gz" -> "1F 8B"; "bz2", "bzip2" -> "42 5A 68"
                "xz", "txz", "tar.xz" -> "FD 37 7A 58 5A 00"; "zst", "zstd" -> "28 B5 2F FD"
                else -> ""
            }
            add(S(e, "${e.uppercase()} Archive", C.ARCHIVES, "application/x-archive", lvl, R_ARC, "Read-only browse; bomb + traversal guarded", sig))
        }

        // ---- X. Installers / packages / executables ----
        for (e in listOf("apk", "aab", "apks", "xapk", "apkx", "ipa", "war", "ear", "deb", "rpm", "appimage", "snap", "flatpak", "appx", "msix", "pkg", "vhd", "vhdx", "vmdk", "qcow", "qcow2", "ova", "ovf")) {
            add(S(e, "${e.uppercase()} Package", C.ARCHIVES, "application/octet-stream", L.PREVIEWABLE, R_ARC, "Manifest/metadata browsed; never installed", "PK"))
        }
        for (e in listOf("msi", "msp", "exe", "dll", "so", "dylib", "elf", "bin", "dat", "dex", "msix")) {
            if (e == "msix") continue // already package-registered
            add(S(e, "${e.uppercase()} Binary", C.BINARIES, "application/octet-stream", L.DETECTED, R_HEX, "Inspection only; never executed"))
        }

        // ---- Y. Fonts ----
        for (e in listOf("ttf", "otf", "ttc", "otc", "woff", "woff2", "eot", "pfb", "pfa", "afm", "pfm", "fon", "fnt", "bdf", "sfd", "ufo", "designspace", "glyphs", "fea")) {
            add(S(e, "${e.uppercase()} Typeface", C.FONTS, "font/$e", L.PREVIEWABLE, R_FONT, "Specimen only; never installed"))
        }

        // ---- Z. Email / contacts / calendar ----
        for (e in listOf("eml", "emlx", "msg", "mbox", "mbx", "pst", "ost", "tnef", "winmail.dat")) {
            add(S(e, "${e.uppercase()} Mail Store", C.EMAIL_CONTACTS, "message/rfc822", L.PREVIEWABLE, R_MAIL, "Headers + body preview; attachments listed, never auto-opened"))
        }
        for (e in listOf("ics", "ical", "ifb", "vcf", "vcard", "contact", "abbu")) {
            add(S(e, "${e.uppercase()} Calendar / Contact", C.EMAIL_CONTACTS, "text/calendar", L.PREVIEWABLE, "VCalendar / VCard Card View"))
        }

        // ---- AA. Scientific / ML data ----
        for (e in listOf("h5", "hdf5", "hdf", "nc", "nc4", "netcdf", "cdf", "grib", "grb", "grb2", "mtx", "mtx.gz", "npy", "npz", "parquet", "avro", "orc", "arrow", "feather", "zarr", "mhd", "mha", "nii", "nii.gz", "nrrd", "vtu", "vtk", "vtp", "xdmf", "xmf", "cif", "mmcif", "sdf", "mol", "mol2", "xyz", "cube", "gro", "pdbqt", "data", "csv", "tsv")) {
            if (e == "csv" || e == "tsv") continue // already sheets-registered
            add(S(e, "${e.uppercase()} Scientific Dataset", C.DATABASES, "application/octet-stream", L.METADATA_READABLE, R_META, "Structural metadata; tensors summarized, never executed"))
        }
        for (e in listOf("pkl", "pickle", "joblib", "safetensors", "pt", "pth", "ckpt", "checkpoint", "onnx", "pb", "tflite", "mlmodel", "mlpackage", "model", "weights", "params", "pdb")) {
            if (e == "pdb") continue // ebook-registered Palm DB keeps its record
            add(S(e, "${e.uppercase()} Model Checkpoint", C.DATABASES, "application/octet-stream", L.METADATA_READABLE, R_META, "Metadata only; graphs/objects never deserialized unsafely"))
        }

        // ---- AB. Databases ----
        for (e in listOf("db", "sqlite", "sqlite3", "db3", "s3db", "sl3", "sqlite-wal", "sqlite-shm", "mdb", "accdb", "odb", "fdb", "gdb", "ib", "ibd", "frm", "myd", "myi", "bsonl", "rdb", "aof", "dump", "backup", "bak", "sqlitedb", "realm", "leveldb", "lmdb")) {
            val sig = if (e in setOf("sqlite", "sqlite3", "db", "db3", "sqlitedb")) "53 51 4C 69 74 65" else ""
            add(S(e, "${e.uppercase()} Database", C.DATABASES, "application/x-sqlite3", L.METADATA_READABLE, R_META, "Read-only schema inspection; no write statements", sig))
        }

        // ---- AC. CAD / 3D ----
        for (e in listOf("stl", "obj", "mtl", "fbx", "dae", "3ds", "ply", "off", "wrl", "vrml", "x3d", "3mf", "amf", "step", "stp", "iges", "igs", "dwg", "dxf", "dgn", "skp", "3dm", "sldprt", "sldasm", "slddrw", "ipt", "iam", "idw", "prt", "asm", "sat", "sab", "jt", "usd", "usda", "usdc", "usdz", "abc", "ma", "mb", "max", "c4d", "lwo", "lws", "ifc")) {
            val lvl = if (e in setOf("obj", "stl")) L.PREVIEWABLE else L.METADATA_READABLE
            val renderer = if (e in setOf("obj", "stl")) "3D Wireframe Canvas Engine" else R_META
            add(S(e, "${e.uppercase()} CAD / 3D Model", C.CAD_3D, "model/$e", lvl, renderer, "Interactive 3D wireframe for OBJ/ASCII STL; metadata for proprietary CAD"))
        }

        // ---- AD. GIS / maps ----
        for (e in listOf("shp", "shx", "prj", "cpg", "gpkg", "gpx", "kml", "kmz", "geotiff", "dem", "vrt", "mbtiles", "pbf", "osm", "laz", "las", "e57", "3dtiles")) {
            val lvl = if (e in setOf("gpx", "kml", "geojson")) L.FULLY_RENDERED else L.METADATA_READABLE
            val renderer = if (e in setOf("gpx", "kml", "geojson")) "Vector Coordinate Plotter" else R_META
            add(S(e, "${e.uppercase()} Geospatial Data", C.CAD_3D, "application/octet-stream", lvl, renderer, "Offline route & waypoint plotter; no location permission requested"))
        }

        // ---- AE. Logs / diagnostics ----
        for (e in listOf("logs", "trace", "traces", "stdout", "stderr", "dump", "dmp", "core", "stack", "stacktrace", "crash", "crashlog", "journal", "syslog", "pcap", "pcapng", "har", "netlog", "perf", "profile", "prof", "heap", "hprof", "gc", "eventlog", "evtx")) {
            add(S(e, "${e.uppercase()} Diagnostic Log", C.LOGS, "text/plain", L.TEXT_EXTRACTABLE, "Log Stream Viewer", "Streamed + searchable; large files paged"))
        }
        add(S("cap", "Capture File", C.LOGS, "application/vnd.tcpdump", L.METADATA_READABLE, R_META, sig = "D4 C3 B2 A1"))

        // ---- AF. Network / security ----
        for (e in listOf("http", "der", "pem", "crt", "cer", "csr", "p7b", "p7c", "p12", "pfx", "jks", "keystore", "truststore", "asc", "gpg", "pgp")) {
            val lvl = if (e in setOf("pem", "crt", "cer", "csr", "asc", "http")) L.TEXT_EXTRACTABLE else L.METADATA_READABLE
            add(S(e, "${e.uppercase()} Security Artifact", C.CONFIG, "application/x-pem-file", lvl, R_META, "Metadata/text only; never imported or installed"))
        }

        // ---- AG. Localization / misc ----
        for (e in listOf("po", "pot", "mo", "resx", "resw", "strings", "stringsdict", "xcstrings", "lproj", "theme", "skin", "qss", "qrc", "ui", "xib", "storyboard", "nib", "meta", "index", "idx", "cache", "temp", "tmp", "old", "orig", "patch", "diff", "map", "sourcemap", "source-map", "one", "onetoc2", "mpp", "xmind", "webarchive", "vcs")) {
            val lvl = if (e in setOf("po", "pot", "strings", "xcstrings", "patch", "diff", "map")) L.TEXT_EXTRACTABLE else L.METADATA_READABLE
            add(S(e, "${e.uppercase()} Resource File", C.OTHERS, "text/plain", lvl, R_TEXT))
        }

        // ---- Diagram sources (Mermaid/PlantUML/Graphviz…) ----
        for (e in listOf("mmd", "mermaid", "puml", "plantuml", "dot", "gv", "ditaa", "bpmn", "opml", "mm")) {
            add(S(e, "${e.uppercase()} Diagram Source", C.MARKDOWN, "text/plain", L.FULLY_RENDERED, R_DIAGRAM, "Supported types render; parse errors show locations"))
        }
    }
}
