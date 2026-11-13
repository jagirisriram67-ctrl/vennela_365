package com.vennela365.viewer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vennela365.viewer.media.MediaEngine
import com.vennela365.viewer.media.MediaInfo
import com.vennela365.viewer.model.*
import com.vennela365.viewer.parser.ArchiveParser
import com.vennela365.viewer.parser.CodeSyntaxHighlighter
import com.vennela365.viewer.parser.DocumentTextExtractor
import com.vennela365.viewer.parser.FileParserManager
import com.vennela365.viewer.parser.SpreadsheetParser
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.registry.FormatInfo
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.AllFilesAccess
import com.vennela365.viewer.storage.DirectFileBrowser
import com.vennela365.viewer.storage.FileNameResolver
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.storage.StorageShortcut
import com.vennela365.viewer.storage.StorageVolumeInfo
import com.vennela365.viewer.storage.StorageVolumes
import com.vennela365.viewer.theme.AccentOption
import com.vennela365.viewer.theme.AppTheme
import com.vennela365.viewer.theme.ThemeMode
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.FidelityGovernor
import com.vennela365.viewer.theme.FidelityPreference
import com.vennela365.viewer.theme.FidelityTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Context.vennelaDataStore: DataStore<Preferences> by preferencesDataStore(name = "vennela365")
private val KEY_THEME = stringPreferencesKey("theme_id")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_ACCENT = stringPreferencesKey("accent_id")
private val KEY_FAVS = stringPreferencesKey("favorites_csv")
private val KEY_OFFLINE = booleanPreferencesKey("offline_mode")
private val KEY_BROWSE_ROOT = stringPreferencesKey("browse_root_uri")

enum class SortMode { NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_LARGEST, SIZE_SMALLEST, TYPE }

/** One breadcrumb level inside a SAF-granted folder tree. */
data class BrowseCrumb(val name: String, val uri: Uri)

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object FileExplorer : Screen("file_explorer")

    /** Restriction-less device file manager: internal + SD + USB/OTG. */
    object AllFiles : Screen("all_files")
    object UniversalSearch : Screen("universal_search")
    object RecentFiles : Screen("recent_files")
    object Favorites : Screen("favorites")
    object Categories : Screen("categories")
    object DocumentViewer : Screen("document_viewer")
    object PdfViewer : Screen("pdf_viewer")
    object ImageViewer : Screen("image_viewer")
    object CodeViewer : Screen("code_viewer")
    object MarkdownViewer : Screen("markdown_viewer")
    object MermaidViewer : Screen("mermaid_viewer")
    object AudioPlayer : Screen("audio_player")
    object VideoPlayer : Screen("video_player")
    object ArchiveBrowser : Screen("archive_browser")
    object FileMetadata : Screen("file_metadata")
    object UnknownFile : Screen("unknown_file")
    object UnsupportedFile : Screen("unsupported_file")
    object Settings : Screen("settings")
    object Permissions : Screen("permissions")
    object ErrorScreen : Screen("error_screen")
    object ThemeSelection : Screen("theme_selection")
    object ShareScreen : Screen("share_screen")
    object SpreadsheetViewer : Screen("spreadsheet_viewer")
    object UniversalLensViewer : Screen("universal_lens_viewer")
    // ---- Extended inventory (spec ?3, Groups A?I) ----
    object Onboarding : Screen("onboarding")
    object StorageIntro : Screen("storage_intro")
    object PermissionStatus : Screen("permission_status")
    object WelcomeHome : Screen("welcome_home")
    object ExplorerGrid : Screen("explorer_grid")
    object FolderContents : Screen("folder_contents")
    object AdvancedSearch : Screen("advanced_search")
    object SortFilterSheet : Screen("sort_filter_sheet")
    object FilePropertiesSheet : Screen("file_properties_sheet")
    object OpeningProgress : Screen("opening_progress")
    object PasswordProtected : Screen("password_protected")
    object LargeFileWarning : Screen("large_file_warning")
    object EncodingSelection : Screen("encoding_selection")
    object ImageGallery : Screen("image_gallery")
    object ImageExif : Screen("image_exif")
    object AnimatedImage : Screen("animated_image")
    object SvgVector : Screen("svg_vector")
    object RawFallback : Screen("raw_fallback")
    object PdfToc : Screen("pdf_toc")
    object PdfSearch : Screen("pdf_search")
    object LegacyWord : Screen("legacy_word")
    object CsvTable : Screen("csv_table")
    object ChartPreview : Screen("chart_preview")
    object PresentationOutline : Screen("presentation_outline")

    /** Real slide reader for pptx / pptm / odp / key and friends. */
    object PresentationViewer : Screen("presentation_viewer")
    object OpenDocument : Screen("open_document")
    object StructuredData : Screen("structured_data")
    object MarkdownSource : Screen("markdown_source")
    object DiagramFallback : Screen("diagram_fallback")
    object NotebookViewer : Screen("notebook_viewer")
    object AudioMetadata : Screen("audio_metadata")
    object TrackInfo : Screen("track_info")
    object NestedArchive : Screen("nested_archive")
    object HexViewer : Screen("hex_viewer")
    object DatabaseViewer : Screen("database_viewer")
    object ScientificViewer : Screen("scientific_viewer")
    object ModelInspector : Screen("model_inspector")
    object CadViewer : Screen("cad_viewer")
    object GisViewer : Screen("gis_viewer")
    object FontViewer : Screen("font_viewer")
    object EmailViewer : Screen("email_viewer")
    object ExecutableInspector : Screen("executable_inspector")
    object OptionalModules : Screen("optional_modules")
    object PrivacyPermissions : Screen("privacy_permissions")
    object CacheStorage : Screen("cache_storage")
    object AboutLicenses : Screen("about_licenses")
    object HelpFormats : Screen("help_formats")
    object OfflineState : Screen("offline_state")
    object DiagnosticsReport : Screen("diagnostics_report")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen = _currentScreen.asStateFlow()

    private val screenHistory = mutableListOf<Screen>(Screen.Home)

    private val _activeTheme = MutableStateFlow(AppTheme.LUNAR_NIGHT)
    val activeTheme = _activeTheme.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode = _themeMode.asStateFlow()

    private val _accent = MutableStateFlow<AccentOption?>(null)
    val accent = _accent.asStateFlow()

    private val _offlineMode = MutableStateFlow(true)
    val offlineMode = _offlineMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FormatCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedFile = MutableStateFlow<FileItem?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles = _recentFiles.asStateFlow()

    private val _favoriteFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val favoriteFiles = _favoriteFiles.asStateFlow()

    private val _archiveEntries = MutableStateFlow<List<ArchiveEntry>>(emptyList())
    val archiveEntries = _archiveEntries.asStateFlow()

    private val _spreadsheetSheets = MutableStateFlow<List<SpreadsheetSheet>>(emptyList())
    val spreadsheetSheets = _spreadsheetSheets.asStateFlow()

    private val _hexRows = MutableStateFlow<List<HexRow>>(emptyList())
    val hexRows = _hexRows.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(true)
    val isPlayingAudio = _isPlayingAudio.asStateFlow()

    private val _isPlayingVideo = MutableStateFlow(true)
    val isPlayingVideo = _isPlayingVideo.asStateFlow()

    // ---- Real Media3 playback (audio + video viewers) ----
    private val mediaEngine = MediaEngine(
        getApplication<Application>().applicationContext,
        viewModelScope
    )
    val mediaState = mediaEngine.state

    private val _mediaInfo = MutableStateFlow(MediaInfo())
    val mediaInfo = _mediaInfo.asStateFlow()

    // ---- Browser state: sort / encoding / viewer lifecycle (spec ?3C) ----
    private val _sortMode = MutableStateFlow(SortMode.NAME_ASC)
    val sortMode = _sortMode.asStateFlow()

    private val _textEncoding = MutableStateFlow("UTF-8")
    val textEncoding = _textEncoding.asStateFlow()

    private val _viewerState = MutableStateFlow<ViewerState>(ViewerState.Idle)
    val viewerState = _viewerState.asStateFlow()

    private val _registryQuery = MutableStateFlow("")
    val registryQuery = _registryQuery.asStateFlow()

    // ---- Real SAF folder browsing (no mock listings) ----
    private val _browseRootUri = MutableStateFlow<Uri?>(null)
    val browseRootUri = _browseRootUri.asStateFlow()

    private val _browseCrumbs = MutableStateFlow<List<BrowseCrumb>>(emptyList())
    val browseCrumbs = _browseCrumbs.asStateFlow()

    private val _browseItems = MutableStateFlow<List<FileItem>>(emptyList())
    val browseItems = _browseItems.asStateFlow()

    private val _browseLoading = MutableStateFlow(false)
    val browseLoading = _browseLoading.asStateFlow()

    private val _browseError = MutableStateFlow<String?>(null)
    val browseError = _browseError.asStateFlow()

    // ---- Restriction-less device browsing (internal + SD + USB/OTG) -------

    private val _volumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val volumes = _volumes.asStateFlow()

    private val _storageShortcuts = MutableStateFlow<List<StorageShortcut>>(emptyList())
    val storageShortcuts = _storageShortcuts.asStateFlow()

    private val _allFilesGranted = MutableStateFlow(false)
    val allFilesGranted = _allFilesGranted.asStateFlow()

    /** Absolute path currently open in the device browser; null shows the hub. */
    private val _devicePath = MutableStateFlow<String?>(null)
    val devicePath = _devicePath.asStateFlow()

    private val _deviceTrail = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val deviceTrail = _deviceTrail.asStateFlow()

    private val _deviceItems = MutableStateFlow<List<FileItem>>(emptyList())
    val deviceItems = _deviceItems.asStateFlow()

    private val _deviceLoading = MutableStateFlow(false)
    val deviceLoading = _deviceLoading.asStateFlow()

    private val _deviceError = MutableStateFlow<String?>(null)
    val deviceError = _deviceError.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles = _showHiddenFiles.asStateFlow()

    /** Multi-select set used by the file manager for bulk actions. */
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths = _selectedPaths.asStateFlow()

    // ---- Device-wide deep search -------------------------------------------

    private val _deepQuery = MutableStateFlow("")
    val deepQuery = _deepQuery.asStateFlow()

    private val _deepResults = MutableStateFlow<List<FileItem>>(emptyList())
    val deepResults = _deepResults.asStateFlow()

    private val _deepSearching = MutableStateFlow(false)
    val deepSearching = _deepSearching.asStateFlow()

    private val _deepScanned = MutableStateFlow(0)
    val deepScanned = _deepScanned.asStateFlow()

    private var deepSearchJob: Job? = null

    /** Extracted text for Word / OpenDocument / RTF files (read-only, bounded). */
    private val _documentText = MutableStateFlow("")
    val documentText = _documentText.asStateFlow()

    private val _documentTruncated = MutableStateFlow(false)
    val documentTruncated = _documentTruncated.asStateFlow()

    // ---- Unbeatable Architecture: Display Engine & Category-Killer State ----
    private val _viewerColorMode = MutableStateFlow(ViewerColorMode.ORIGINAL)
    val viewerColorMode = _viewerColorMode.asStateFlow()

    private val _isLassoActive = MutableStateFlow(false)
    val isLassoActive = _isLassoActive.asStateFlow()

    private val _isSplitHorizonActive = MutableStateFlow(false)
    val isSplitHorizonActive = _isSplitHorizonActive.asStateFlow()

    private val _isPrivacyActive = MutableStateFlow(false)
    val isPrivacyActive = _isPrivacyActive.asStateFlow()

    private val _enableMicroLouvre = MutableStateFlow(false)
    val enableMicroLouvre = _enableMicroLouvre.asStateFlow()

    private val _isStudySheetActive = MutableStateFlow(false)
    val isStudySheetActive = _isStudySheetActive.asStateFlow()

    private val _isLightboxActive = MutableStateFlow(false)
    val isLightboxActive = _isLightboxActive.asStateFlow()

    val fidelityGovernorTier = FidelityGovernor.activeTier
    val fidelityPreference = FidelityGovernor.preference

    init {
        restorePreferences()
        refreshStorage()
    }

    private fun loadMockInitialData() {
        val proposalPdf = FileItem(
            id = "1",
            name = "Project_Proposal.pdf",
            extension = "pdf",
            sizeBytes = (2.4 * 1024 * 1024).toLong(),
            isFavorite = true,
            formatInfo = FormatRegistry.lookup("pdf")
        )
        val natureJpg = FileItem(
            id = "2",
            name = "Nature.jpg",
            extension = "jpg",
            sizeBytes = (4.2 * 1024 * 1024).toLong(),
            isFavorite = true,
            formatInfo = FormatRegistry.lookup("jpg")
        )
        val readmeMd = FileItem(
            id = "3",
            name = "README.md",
            extension = "md",
            sizeBytes = 6 * 1024,
            isFavorite = true,
            formatInfo = FormatRegistry.lookup("md")
        )
        val docxItem = FileItem(
            id = "4",
            name = "project_proposal.docx",
            extension = "docx",
            sizeBytes = (2.4 * 1024 * 1024).toLong(),
            formatInfo = FormatRegistry.lookup("docx")
        )
        val xlsxItem = FileItem(
            id = "5",
            name = "project_data.xlsx",
            extension = "xlsx",
            sizeBytes = (1.2 * 1024 * 1024).toLong(),
            isFavorite = true,
            formatInfo = FormatRegistry.lookup("xlsx")
        )
        val pythonCode = FileItem(
            id = "6",
            name = "main.py",
            extension = "py",
            sizeBytes = (2.8 * 1024).toLong(),
            sampleContent = CodeSyntaxHighlighter.defaultSamplePython,
            formatInfo = FormatRegistry.lookup("py")
        )
        val diagramMmd = FileItem(
            id = "7",
            name = "diagram.mmd",
            extension = "mmd",
            sizeBytes = 4 * 1024,
            isFavorite = true,
            formatInfo = FormatRegistry.lookup("mmd")
        )
        val songMp3 = FileItem(
            id = "8",
            name = "Peaceful Night.mp3",
            extension = "mp3",
            sizeBytes = (5.6 * 1024 * 1024).toLong(),
            isFavorite = true,
            formatInfo = FormatRegistry.lookup("mp3")
        )
        val videoMp4 = FileItem(
            id = "9",
            name = "Sample_Video.mp4",
            extension = "mp4",
            sizeBytes = (48 * 1024 * 1024).toLong(),
            formatInfo = FormatRegistry.lookup("mp4")
        )
        val projectZip = FileItem(
            id = "10",
            name = "project.zip",
            extension = "zip",
            sizeBytes = (12.4 * 1024 * 1024).toLong(),
            formatInfo = FormatRegistry.lookup("zip")
        )
        val unknownFile = FileItem(
            id = "11",
            name = "data.unknown",
            extension = "unknown",
            sizeBytes = 12980321L,
            formatInfo = FormatRegistry.lookup("unknown")
        )
        val unsupportedFile = FileItem(
            id = "12",
            name = "model.xyz",
            path = "/Internal Storage/Download/model.xyz",
            extension = "xyz",
            sizeBytes = (12.4 * 1024 * 1024).toLong(),
            formatInfo = FormatRegistry.lookup("xyz")
        )
        val corruptedPdf = FileItem(
            id = "13",
            name = "corrupted_file.pdf",
            extension = "pdf",
            sizeBytes = (3.8 * 1024 * 1024).toLong(),
            formatInfo = FormatRegistry.lookup("pdf")
        )

        _recentFiles.value = listOf(
            proposalPdf, natureJpg, readmeMd, docxItem, xlsxItem, pythonCode,
            diagramMmd, songMp3, videoMp4, projectZip, unknownFile, unsupportedFile
        )

        _favoriteFiles.value = listOf(
            proposalPdf, natureJpg, readmeMd, xlsxItem, diagramMmd, songMp3,
            FileItem("14", "code.java", extension = "java", sizeBytes = 8192, isFavorite = true, formatInfo = FormatRegistry.lookup("java"))
        )

        _archiveEntries.value = ArchiveParser.getSampleArchiveEntries()
        _spreadsheetSheets.value = SpreadsheetParser.getSampleSpreadsheets()
        _hexRows.value = FileParserManager.generateSampleHexDump()
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenHistory.isNotEmpty()) {
            val prev = screenHistory.removeAt(screenHistory.lastIndex)
            _currentScreen.value = prev
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: FormatCategory?) {
        _selectedCategory.value = category
    }

    fun setTheme(theme: AppTheme) {
        _activeTheme.value = theme
        viewModelScope.launch {
            try {
                getApplication<Application>().applicationContext.vennelaDataStore.edit { prefs ->
                    prefs[KEY_THEME] = theme.name
                }
            } catch (_: Exception) { }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            try {
                getApplication<Application>().applicationContext.vennelaDataStore.edit { prefs ->
                    prefs[KEY_THEME_MODE] = mode.id
                }
            } catch (_: Exception) { }
        }
    }

    fun setAccent(accent: AccentOption) {
        _accent.value = accent
        viewModelScope.launch {
            try {
                getApplication<Application>().applicationContext.vennelaDataStore.edit { prefs ->
                    prefs[KEY_ACCENT] = accent.id
                }
            } catch (_: Exception) { }
        }
    }

    /** Drops the swatch override so the Theme Gallery's own colours win. */
    fun clearAccent() {
        _accent.value = null
        viewModelScope.launch {
            try {
                getApplication<Application>().applicationContext.vennelaDataStore.edit { prefs ->
                    prefs[KEY_ACCENT] = "default"
                }
            } catch (_: Exception) { }
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        viewModelScope.launch {
            try {
                getApplication<Application>().applicationContext.vennelaDataStore.edit { prefs ->
                    prefs[KEY_OFFLINE] = enabled
                }
            } catch (_: Exception) { }
        }
    }

    private fun restorePreferences() {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>().applicationContext
                val prefs = ctx.vennelaDataStore.data.first()
                prefs[KEY_THEME]?.let { saved ->
                    runCatching { AppTheme.valueOf(saved) }.getOrNull()?.let { _activeTheme.value = it }
                }
                _themeMode.value = ThemeMode.fromId(prefs[KEY_THEME_MODE])
                _accent.value = AccentOption.entries.firstOrNull { it.id == prefs[KEY_ACCENT] }
                _offlineMode.value = prefs[KEY_OFFLINE] ?: true
                val favCsv = prefs[KEY_FAVS].orEmpty()
                if (favCsv.isNotBlank()) {
                    val names = favCsv.split('|').toSet()
                    _favoriteFiles.value = _favoriteFiles.value.map { it.copy(isFavorite = it.name in names) }
                        .filter { it.isFavorite }
                        .ifEmpty { _favoriteFiles.value }
                }
                val savedRoot = prefs[KEY_BROWSE_ROOT]?.let { runCatching { Uri.parse(it) }.getOrNull() }
                if (savedRoot != null) {
                    val granted = runCatching {
                        ctx.contentResolver.persistedUriPermissions.any {
                            it.uri == savedRoot && it.isReadPermission
                        }
                    }.getOrDefault(false)
                    if (granted) {
                        _browseRootUri.value = savedRoot
                        val rootName = runCatching {
                            DocumentFile.fromTreeUri(ctx, savedRoot)?.name
                        }.getOrNull().orEmpty().ifBlank { "Selected folder" }
                        _browseCrumbs.value = listOf(BrowseCrumb(rootName, savedRoot))
                        loadBrowseFolder(savedRoot)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun persistFavorites() {
        viewModelScope.launch {
            try {
                val csv = _favoriteFiles.value.joinToString("|") { it.name }
                getApplication<Application>().applicationContext.vennelaDataStore.edit { prefs ->
                    prefs[KEY_FAVS] = csv
                }
            } catch (_: Exception) { }
        }
    }

    // ---- Browser helpers ----

    fun setSortMode(mode: SortMode) { _sortMode.value = mode }

    fun setEncoding(enc: String) { _textEncoding.value = enc }

    fun setRegistryQuery(q: String) { _registryQuery.value = q }

    fun setViewerState(state: ViewerState) { _viewerState.value = state }

    // ---- Unbeatable Architecture Action Handlers ----
    fun setViewerColorMode(mode: ViewerColorMode) { _viewerColorMode.value = mode }
    fun cycleViewerColorMode() {
        val entries = ViewerColorMode.entries
        val nextIndex = (entries.indexOf(_viewerColorMode.value) + 1) % entries.size
        _viewerColorMode.value = entries[nextIndex]
    }
    fun toggleLasso() { _isLassoActive.value = !_isLassoActive.value }
    fun toggleSplitHorizon() { _isSplitHorizonActive.value = !_isSplitHorizonActive.value }
    fun togglePrivacy() { _isPrivacyActive.value = !_isPrivacyActive.value }
    fun setMicroLouvre(enable: Boolean) { _enableMicroLouvre.value = enable }
    fun showStudySheet() { _isStudySheetActive.value = true }
    fun dismissStudySheet() { _isStudySheetActive.value = false }
    fun showLightbox() { _isLightboxActive.value = true }
    fun dismissLightbox() { _isLightboxActive.value = false }
    fun setFidelityPreference(pref: FidelityPreference) { FidelityGovernor.setPreference(pref) }

    fun searchFormats(query: String): List<FormatInfo> = FormatRegistry.search(query)

    fun compatibilityDashboard() = FormatRegistry.dashboard()

    fun sortedFiles(files: List<FileItem>): List<FileItem> = when (_sortMode.value) {
        SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
        SortMode.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
        SortMode.DATE_NEWEST -> files.sortedByDescending { it.lastModified }
        SortMode.DATE_OLDEST -> files.sortedBy { it.lastModified }
        SortMode.SIZE_LARGEST -> files.sortedByDescending { it.sizeBytes }
        SortMode.SIZE_SMALLEST -> files.sortedBy { it.sizeBytes }
        SortMode.TYPE -> files.sortedBy { it.extension }
    }

    // ---- Real SAF folder browsing -------------------------------------------

    /** Grants a persisted folder tree and lists it for real. */
    fun setBrowseRoot(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            _browseRootUri.value = uri
            val rootName = withContext(Dispatchers.IO) {
                runCatching { DocumentFile.fromTreeUri(ctx, uri)?.name }.getOrNull()
            }.orEmpty().ifBlank { "Selected folder" }
            _browseCrumbs.value = listOf(BrowseCrumb(rootName, uri))
            try {
                ctx.vennelaDataStore.edit { prefs -> prefs[KEY_BROWSE_ROOT] = uri.toString() }
            } catch (_: Exception) { }
            loadBrowseFolder(uri)
        }
    }

    fun browseToCrumb(index: Int) {
        val crumbs = _browseCrumbs.value
        if (index !in crumbs.indices || index == crumbs.lastIndex) return
        val trimmed = crumbs.take(index + 1)
        _browseCrumbs.value = trimmed
        loadBrowseFolder(trimmed.last().uri)
    }

    fun browseInto(folder: FileItem) {
        val uri = folder.uri ?: return
        _browseCrumbs.value = _browseCrumbs.value + BrowseCrumb(folder.name, uri)
        loadBrowseFolder(uri)
    }

    /** Returns false when already at the granted root (caller can exit the screen). */
    fun browseBack(): Boolean {
        val crumbs = _browseCrumbs.value
        if (crumbs.size <= 1) return false
        val trimmed = crumbs.dropLast(1)
        _browseCrumbs.value = trimmed
        loadBrowseFolder(trimmed.last().uri)
        return true
    }

    fun refreshBrowse() {
        _browseCrumbs.value.lastOrNull()?.let { loadBrowseFolder(it.uri) }
    }

    fun clearBrowseRoot() {
        val ctx = getApplication<Application>().applicationContext
        _browseRootUri.value?.let { uri ->
            runCatching { ctx.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        _browseRootUri.value = null
        _browseCrumbs.value = emptyList()
        _browseItems.value = emptyList()
        _browseError.value = null
        viewModelScope.launch {
            try {
                ctx.vennelaDataStore.edit { prefs -> prefs.remove(KEY_BROWSE_ROOT) }
            } catch (_: Exception) { }
        }
    }

    private fun loadBrowseFolder(uri: Uri) {
        viewModelScope.launch {
            _browseLoading.value = true
            _browseError.value = null
            val ctx = getApplication<Application>().applicationContext
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = DocumentFile.fromSingleUri(ctx, uri)
                        ?: DocumentFile.fromTreeUri(ctx, uri)
                        ?: error("Folder access expired ? grant it again")
                    if (!dir.isDirectory) error("This item is not a folder")
                    dir.listFiles().mapNotNull { child ->
                        runCatching {
                            val name = child.name ?: return@runCatching null
                            val isDir = child.isDirectory
                            val key = if (isDir) "" else FileNameResolver.resolveKey(name)
                            FileItem(
                                id = child.uri.toString(),
                                name = name,
                                uri = child.uri,
                                extension = key,
                                sizeBytes = if (isDir) 0L else child.length(),
                                lastModified = child.lastModified().takeIf { it > 0 }
                                    ?: System.currentTimeMillis(),
                                isDirectory = isDir,
                                formatInfo = FormatRegistry.lookup(key)
                            )
                        }.getOrNull()
                    }.sortedWith(
                        compareByDescending<FileItem> { it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
                }
            }
            result.onSuccess { _browseItems.value = it }
                .onFailure {
                    _browseItems.value = emptyList()
                    _browseError.value = it.message ?: "Could not read this folder"
                }
            _browseLoading.value = false
        }
    }

    // ---- Restriction-less device browser -----------------------------------

    /** Re-reads volumes, shortcuts and the all-files grant. Cheap; UI-safe. */
    fun refreshStorage() {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val grant = AllFilesAccess.canBrowseDirectly(ctx)
            val volumes = withContext(Dispatchers.IO) { StorageVolumes.enumerate(ctx) }
            val shortcuts = withContext(Dispatchers.IO) { DirectFileBrowser.shortcuts(ctx) }
            _allFilesGranted.value = grant
            _volumes.value = volumes
            _storageShortcuts.value = shortcuts
        }
    }

    /** Re-checks the grant only, for onResume after the settings round trip. */
    fun refreshAllFilesGrant() {
        val ctx = getApplication<Application>().applicationContext
        val granted = AllFilesAccess.canBrowseDirectly(ctx)
        if (granted != _allFilesGranted.value) {
            _allFilesGranted.value = granted
            if (granted) refreshStorage()
        }
    }

    /**
     * Opens an absolute path in the device browser. Passing null returns to the
     * storage hub. Reading happens off the main thread and every failure is
     * surfaced as a real message rather than an empty list.
     */
    fun openDevicePath(path: String?) {
        if (path == null) {
            _devicePath.value = null
            _deviceTrail.value = emptyList()
            _deviceItems.value = emptyList()
            _deviceError.value = null
            _selectedPaths.value = emptySet()
            return
        }
        viewModelScope.launch {
            _deviceLoading.value = true
            _deviceError.value = null
            val ctx = getApplication<Application>().applicationContext
            val showHidden = _showHiddenFiles.value
            val listing = withContext(Dispatchers.IO) {
                DirectFileBrowser.list(java.io.File(path), showHidden)
            }
            val trail = withContext(Dispatchers.IO) { DirectFileBrowser.trail(ctx, path) }
            listing.onSuccess {
                _devicePath.value = path
                _deviceItems.value = it
                _deviceTrail.value = trail
                _selectedPaths.value = emptySet()
            }.onFailure {
                _deviceItems.value = emptyList()
                _deviceError.value = it.message ?: "This folder could not be opened"
            }
            _deviceLoading.value = false
        }
    }

    /** Steps up one level. Returns false at a volume root. */
    fun deviceUp(): Boolean {
        val current = _devicePath.value ?: return false
        val ctx = getApplication<Application>().applicationContext
        if (DirectFileBrowser.isVolumeRoot(ctx, current)) return false
        val parent = DirectFileBrowser.parentOf(current) ?: return false
        openDevicePath(parent.absolutePath)
        return true
    }

    fun toggleShowHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        _devicePath.value?.let { openDevicePath(it) }
    }

    fun setHiddenFilesVisible(visible: Boolean) {
        if (_showHiddenFiles.value == visible) return
        _showHiddenFiles.value = visible
        _devicePath.value?.let { openDevicePath(it) }
    }

    /** Opens a file from the device browser through the normal viewer pipeline. */
    fun openDeviceFile(file: FileItem) {
        if (file.isDirectory) {
            openDevicePath(file.path)
            return
        }
        rememberRecent(file)
        openFile(file)
    }

    // ---- Multi-select ------------------------------------------------------

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value
        _selectedPaths.value = if (path in current) current - path else current + path
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun selectedItems(): List<FileItem> {
        val chosen = _selectedPaths.value
        if (chosen.isEmpty()) return emptyList()
        return _deviceItems.value.filter { it.path in chosen || it.id in chosen }
    }

    /** Real delete over direct paths; returns the number removed. */
    fun deleteSelected(): Int {
        val items = selectedItems()
        if (items.isEmpty()) return 0
        var removed = 0
        items.forEach { item ->
            val ok = runCatching {
                val f = java.io.File(item.path)
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }.getOrDefault(false)
            if (ok) removed++
        }
        _selectedPaths.value = emptySet()
        _devicePath.value?.let { openDevicePath(it) } ?: refreshStorage()
        return removed
    }

    /** Every file under the current device folder, for "play all" style actions. */
    fun deviceFiles(): List<FileItem> = _deviceItems.value.filterNot { it.isDirectory }

    private fun rememberRecent(file: FileItem) {
        val without = _recentFiles.value.filterNot { it.id == file.id }
        _recentFiles.value = (listOf(file) + without).take(60)
    }

    // ---- Device-wide deep search -------------------------------------------

    /**
     * Searches every mounted volume from its root, streaming matches as they
     * are found so results appear immediately instead of after the whole walk.
     * A new call cancels the previous one.
     */
    fun startDeepSearch(query: String) {
        _deepQuery.value = query
        deepSearchJob?.cancel()
        if (query.isBlank()) {
            _deepResults.value = emptyList()
            _deepSearching.value = false
            _deepScanned.value = 0
            return
        }
        deepSearchJob = viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            _deepSearching.value = true
            _deepScanned.value = 0
            _deepResults.value = emptyList()
            val roots = withContext(Dispatchers.IO) {
                StorageVolumes.enumerate(ctx)
                    .filter { it.isBrowsable }
                    .mapNotNull { it.rootPath }
            }
            val found = runCatching {
                DirectFileBrowser.search(
                    roots = roots,
                    query = query,
                    includeHidden = _showHiddenFiles.value,
                    onBatch = { batch ->
                        _deepResults.value = (_deepResults.value + batch)
                            .sortedByDescending { it.lastModified }
                            .take(400)
                        _deepScanned.value += batch.size
                    }
                )
            }.getOrElse { emptyList() }
            if (_deepQuery.value == query) {
                _deepResults.value = found.take(400)
                _deepSearching.value = false
            }
        }
    }

    fun clearDeepSearch() {
        deepSearchJob?.cancel()
        deepSearchJob = null
        _deepQuery.value = ""
        _deepResults.value = emptyList()
        _deepSearching.value = false
        _deepScanned.value = 0
    }

    fun toggleAudioPlayback() {
        _isPlayingAudio.value = !_isPlayingAudio.value
    }

    fun toggleVideoPlayback() {
        _isPlayingVideo.value = !_isPlayingVideo.value
    }

    // ---- Real media playback (Media3) ---------------------------------------

    /**
     * Loads the selected file into the shared ExoPlayer and reads its real
     * container metadata. Nothing is downloaded or written.
     */
    fun prepareMedia(file: FileItem, autoPlay: Boolean = false) {
        val uri = file.uri ?: run {
            _mediaInfo.value = MediaInfo()
            return
        }
        val ctx = getApplication<Application>().applicationContext
        val isAudioHint = file.formatInfo.category == FormatCategory.AUDIO
        // Create/attach the player immediately so video surfaces can bind it.
        mediaEngine.open(uri, file.name, autoPlay = autoPlay)
        viewModelScope.launch {
            val info = MediaEngine.readInfo(ctx, uri, isAudioHint)
            _mediaInfo.value = info
            mediaEngine.setHasVideo(info.hasVideo)
        }
    }

    /** Shared ExoPlayer instance for the video surface. */
    fun mediaPlayer() = mediaEngine.obtainPlayer()

    fun toggleMediaPlayback() = mediaEngine.togglePlayPause()

    fun seekMedia(fraction: Float) = mediaEngine.seekToFraction(fraction)

    fun skipMedia(deltaMs: Long) = mediaEngine.skipBy(deltaMs)

    fun setMediaSpeed(speed: Float) = mediaEngine.setSpeed(speed)

    fun setMediaRepeat(enabled: Boolean) = mediaEngine.setRepeatMode(enabled)

    fun stopMedia() = mediaEngine.stop()

    /** Real sibling media files in the current folder, for the playlist panel. */
    fun siblingMediaFiles(): List<FileItem> {
        val audio = setOf(
            "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "wma",
            "amr", "aiff", "aif", "alac", "ape", "mka", "mid", "midi"
        )
        val video = setOf(
            "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "flv", "wmv",
            "mpeg", "mpg", "ts", "mts", "m2ts", "ogv", "vob"
        )
        val wanted = audio + video
        val pool = if (_browseItems.value.isNotEmpty()) _browseItems.value else _recentFiles.value
        return pool.filter { !it.isDirectory && it.uri != null && it.extension.lowercase() in wanted }
    }

    /** Real sibling images in the current folder, for the viewer filmstrip. */
    fun siblingImageFiles(): List<FileItem> {
        val wanted = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif",
            "avif", "jfif", "tif", "tiff", "ico", "dng"
        )
        val pool = if (_browseItems.value.isNotEmpty()) _browseItems.value else _recentFiles.value
        return pool.filter { !it.isDirectory && it.uri != null && it.extension.lowercase() in wanted }
    }

    override fun onCleared() {
        mediaEngine.release()
        super.onCleared()
    }

    fun toggleFavorite(file: FileItem) {
        val currentFavs = _favoriteFiles.value.toMutableList()
        val index = currentFavs.indexOfFirst { it.name == file.name }
        if (index >= 0) {
            currentFavs.removeAt(index)
        } else {
            currentFavs.add(file.copy(isFavorite = true))
        }
        _favoriteFiles.value = currentFavs
        persistFavorites()
    }

    private var activeParseJob: Job? = null
    private var activeHexJob: Job? = null

    fun openFile(file: FileItem) {
        // Cancel active background jobs to prevent race conditions during rapid file switching
        activeHexJob?.cancel()
        activeParseJob?.cancel()

        // 1. Safety gates evaluated BEFORE initiating heavy I/O
        if (file.name.contains("corrupted", ignoreCase = true)) {
            _selectedFile.value = file
            _viewerState.value = ViewerState.Failed("File appears damaged; original left untouched.")
            navigateTo(Screen.ErrorScreen)
            return
        }
        if (file.name.contains("locked", ignoreCase = true) || file.name.contains("encrypted", ignoreCase = true)) {
            _selectedFile.value = file
            _viewerState.value = ViewerState.PasswordRequired
            navigateTo(Screen.PasswordProtected)
            return
        }
        if (file.sizeBytes > 50L * 1024 * 1024) {
            _selectedFile.value = file
            _viewerState.value = ViewerState.Loading(null)
            navigateTo(Screen.LargeFileWarning)
            return
        }

        // 2. Set active file state
        _selectedFile.value = file
        _viewerState.value = ViewerState.Detecting
        _documentText.value = ""
        _documentTruncated.value = false

        // 3. Launch managed hex dump job
        activeHexJob = viewModelScope.launch {
            val rows = FileParserManager.readHexDump(
                getApplication<Application>().applicationContext,
                file.uri
            )
            if (_selectedFile.value?.id == file.id) {
                _hexRows.value = rows
            }
        }

        // 4. Launch managed content parser job (supports content URIs and in-archive extracted entries)
        activeParseJob = viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext

            // Handle entries opened from inside an archive
            if (file.archiveParentUri != null && file.archiveEntryPath != null && file.sampleContent.isBlank()) {
                val bytes = ArchiveParser.extractEntry(ctx, file.archiveParentUri, file.archiveEntryPath)
                if (bytes != null && _selectedFile.value?.id == file.id) {
                    val text = String(bytes, Charsets.UTF_8)
                    _selectedFile.value = _selectedFile.value?.copy(sampleContent = text)
                    if (file.formatInfo.category in setOf(FormatCategory.DOCUMENTS, FormatCategory.CODE, FormatCategory.MARKDOWN, FormatCategory.CONFIG, FormatCategory.LOGS)) {
                        _documentText.value = text
                    }
                }
            } else if (file.uri != null) {
                when (file.formatInfo.category) {
                    FormatCategory.CODE, FormatCategory.CONFIG,
                    FormatCategory.MARKDOWN, FormatCategory.LOGS -> {
                        if (file.sampleContent.isBlank()) {
                            val text = DocumentTextExtractor.readPlainText(ctx, file.uri)
                            if (text.isNotBlank() && _selectedFile.value?.id == file.id) {
                                _selectedFile.value = _selectedFile.value?.copy(sampleContent = text)
                            }
                        }
                    }
                    FormatCategory.SPREADSHEETS -> {
                        val sheets = SpreadsheetParser.parseSpreadsheet(ctx, file.uri, file.extension)
                        if (_selectedFile.value?.id == file.id) _spreadsheetSheets.value = sheets
                    }
                    FormatCategory.ARCHIVES -> {
                        val entries = ArchiveParser.parseArchiveEntries(ctx, file.uri, file.extension)
                        if (_selectedFile.value?.id == file.id) _archiveEntries.value = entries
                    }
                    FormatCategory.DOCUMENTS, FormatCategory.EMAIL_CONTACTS, FormatCategory.EBOOKS -> {
                        val extracted = DocumentTextExtractor.extract(ctx, file.uri, file.extension)
                        if (_selectedFile.value?.id == file.id) {
                            _documentText.value = extracted.text
                            _documentTruncated.value = extracted.truncated
                        }
                    }
                    else -> Unit
                }
            }
        }

        _viewerState.value = ViewerState.Ready
        when (file.extension.lowercase()) {
            "pdf" -> navigateTo(Screen.PdfViewer)
            "jpg", "jpeg", "jpe", "jfif", "png", "bmp", "ico", "cur", "tif", "tiff", "qoi" -> navigateTo(Screen.ImageViewer)
            "gif", "apng", "webp", "avif" -> navigateTo(Screen.AnimatedImage)
            "svg", "svgz" -> navigateTo(Screen.SvgVector)
            "dng", "cr2", "cr3", "nef", "arw", "raf", "orf", "rw2", "raw" -> navigateTo(Screen.RawFallback)
            "docx", "doc", "odt", "ott", "fodt", "rtf", "abw" -> navigateTo(Screen.DocumentViewer)
            "wps", "wpt", "hwp", "pages", "wpd", "lwp", "sxw", "sdw", "xps", "oxps", "chm", "wri" -> navigateTo(Screen.LegacyWord)
            "ppt", "pptx", "pptm", "ppsm", "ppsx", "pps", "potx", "potm", "pot",
            "odp", "otp", "fodp", "key", "keynote", "show", "shw", "sxi" ->
                navigateTo(Screen.PresentationViewer)
            "xlsx", "xls", "xlsm", "xlsb", "xlt", "xltx", "ods", "ots", "dbf", "numbers", "gnumeric" -> navigateTo(Screen.SpreadsheetViewer)
            "csv", "tsv", "dsv", "tab" -> navigateTo(Screen.CsvTable)
            "py", "java", "kt", "kts", "js", "mjs", "cjs", "jsx", "ts", "tsx", "cpp", "c", "h", "hpp", "cs", "html", "css", "scss", "sh", "bash", "zsh", "ps1", "sql", "go", "rs", "swift", "dart", "rb", "php", "lua", "r", "jl", "vue", "svelte", "astro" -> navigateTo(Screen.CodeViewer)
            "json", "jsonc", "json5", "ndjson", "jsonl", "xml", "yaml", "yml", "toml", "ini", "proto", "graphql" -> navigateTo(Screen.StructuredData)
            "md", "markdown", "mdown", "mkd", "rst", "adoc", "asciidoc", "org", "tex", "bib" -> navigateTo(Screen.MarkdownViewer)
            "mmd", "mermaid", "puml", "plantuml", "dot", "gv" -> navigateTo(Screen.MermaidViewer)
            "drawio", "bpmn", "opml" -> navigateTo(Screen.DiagramFallback)
            "ipynb", "rmd", "qmd" -> navigateTo(Screen.NotebookViewer)
            "mp3", "m4a", "m4b", "aac", "wav", "wave", "flac", "ogg", "oga", "opus", "aiff", "amr", "mid", "midi", "wma", "alac" -> navigateTo(Screen.AudioPlayer)
            "mp4", "m4v", "mov", "qt", "mkv", "webm", "avi", "wmv", "mpg", "mpeg", "3gp", "ogv", "ts", "m2ts" -> navigateTo(Screen.VideoPlayer)
            "m3u", "m3u8", "pls", "srt", "vtt", "ass", "lrc" -> navigateTo(Screen.TrackInfo)
            "zip", "zipx", "jar", "apk", "aab", "tar", "tgz", "gz", "7z", "rar", "xz", "zst", "bz2", "cab" -> navigateTo(Screen.ArchiveBrowser)
            "tar.gz", "tar.xz", "tar.zst" -> navigateTo(Screen.NestedArchive)
            "ttf", "otf", "ttc", "woff", "woff2", "eot" -> navigateTo(Screen.FontViewer)
            "eml", "msg", "mbox", "pst", "ics", "vcf" -> navigateTo(Screen.EmailViewer)
            "db", "sqlite", "sqlite3", "mdb", "accdb", "realm", "parquet" -> navigateTo(Screen.DatabaseViewer)
            "h5", "hdf5", "nc", "npy", "npz", "fits", "mat", "cif", "mol", "xyz" -> navigateTo(Screen.ScientificViewer)
            "pt", "pth", "onnx", "tflite", "pkl", "safetensors", "ckpt" -> navigateTo(Screen.ModelInspector)
            "stl", "obj", "fbx", "gltf", "glb", "step", "stp", "iges", "dwg", "dxf", "blend", "usd", "usdz" -> navigateTo(Screen.CadViewer)
            "shp", "gpkg", "gpx", "kml", "kmz", "geojson", "tif" -> navigateTo(Screen.GisViewer)
            "exe", "dll", "so", "dylib", "elf", "bin", "dex", "msi", "class", "pyc" -> navigateTo(Screen.ExecutableInspector)
            "log", "out", "err", "trace", "pcap", "har", "hprof", "evtx" -> navigateTo(Screen.CodeViewer)
            "epub", "mobi", "azw", "azw3", "fb2", "cbz", "cbr" -> navigateTo(Screen.OpenDocument)
            "unknown" -> navigateTo(Screen.UnknownFile)
            "xyz" -> navigateTo(Screen.UnsupportedFile)
            else -> navigateTo(Screen.UnsupportedFile)
        }
        _viewerState.value = ViewerState.Ready
    }

    fun openUri(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val item = withContext(Dispatchers.IO) {
                val metadata = runCatching {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            Pair(
                                if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                            )
                        } else null
                    }
                }.getOrNull()
                val name = metadata?.first ?: displayName ?: uri.lastPathSegment ?: "opened_file"
                val key = FileNameResolver.resolveKey(name)
                val info = FormatRegistry.lookup(key)
                val preview = if (info.category in setOf(FormatCategory.CODE, FormatCategory.CONFIG, FormatCategory.MARKDOWN, FormatCategory.LOGS)) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                            reader.readText().take(512 * 1024)
                        }.orEmpty()
                    }.getOrDefault("")
                } else ""
                FileItem(
                    id = uri.toString(),
                    name = name,
                    uri = uri,
                    extension = key,
                    sizeBytes = metadata?.second ?: 0L,
                    formatInfo = info,
                    sampleContent = preview
                )
            }
            _recentFiles.value = (listOf(item) + _recentFiles.value.filterNot { it.id == item.id }).take(100)
            openFile(item)
        }
    }

    /**
     * Adds extra files from a multi-file share/open to the recents shelf without
     * stealing the viewer away from the file that was opened first.
     */
    fun openUris(uris: List<Pair<Uri, String?>>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val items = withContext(Dispatchers.IO) {
                uris.mapNotNull { (uri, displayName) ->
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    runCatching {
                        val metadata = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                                Pair(
                                    if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                                )
                            } else null
                        }
                        val name = metadata?.first ?: displayName ?: uri.lastPathSegment ?: return@runCatching null
                        val key = FileNameResolver.resolveKey(name)
                        FileItem(
                            id = uri.toString(),
                            name = name,
                            uri = uri,
                            extension = key,
                            sizeBytes = metadata?.second ?: 0L,
                            formatInfo = FormatRegistry.lookup(key)
                        )
                    }.getOrNull()
                }
            }
            if (items.isNotEmpty()) {
                val incoming = items.map { it.id }.toSet()
                _recentFiles.value = (items + _recentFiles.value.filterNot { it.id in incoming }).take(100)
            }
        }
    }
}
