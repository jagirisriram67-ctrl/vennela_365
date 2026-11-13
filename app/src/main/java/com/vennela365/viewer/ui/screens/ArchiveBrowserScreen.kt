package com.vennela365.viewer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.model.ArchiveEntry
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.parser.ArchiveParser
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.ReaderBottomPill
import com.vennela365.viewer.ui.components.ReaderChromeVisibility
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderProgressRule
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.VennelaBackground
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/* ------------------------------------------------------------------------- */
/* Model                                                                      */
/* ------------------------------------------------------------------------- */

private enum class ArchiveSortMode(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    TYPE("Type")
}

/** Real, reversible filter over the entries the archive actually contains. */
private enum class ArchiveTypeFilter(val label: String) {
    ALL("All items"),
    FOLDERS("Folders"),
    FILES("Files")
}

/** One archive opened from inside another, with the entries it really listed. */
private data class NestedArchiveLevel(
    val name: String,
    val uri: Uri,
    val path: String,
    val entries: List<ArchiveEntry>
)

/** One stop on the breadcrumb trail, carrying the folder it opens. */
private data class ArchiveCrumb(val label: String, val prefix: String)

/**
 * The archive this browser is showing.
 *
 * Opening an entry hands that entry to the ViewModel as the selected file, so
 * returning here would otherwise forget which container we were browsing and
 * the title, share and open-with actions would target the entry instead. This
 * keeps the last container so those actions keep working across a preview.
 */
private object ArchiveSession {
    private var cachedId: String? = null
    private var cached: FileItem? = null
    private var cachedPrefix: String = ""

    fun resolve(selected: FileItem?): FileItem? {
        if (selected == null) return cached
        val isArchiveEntry = !selected.archiveEntryPath.isNullOrBlank()
        if (!isArchiveEntry && selected.uri != null) {
            if (selected.id != cachedId) {
                cachedId = selected.id
                cached = selected
                cachedPrefix = ""
            }
            return cached
        }
        return cached ?: selected
    }

    /** Remembers the open folder so a preview round trip lands back in it. */
    fun storePrefix(archiveId: String?, prefix: String) {
        if (archiveId != null && archiveId == cachedId) cachedPrefix = prefix
    }

    fun prefixFor(archiveId: String?): String =
        if (archiveId != null && archiveId == cachedId) cachedPrefix else ""
}

/* ------------------------------------------------------------------------- */
/* Tunables                                                                   */
/* ------------------------------------------------------------------------- */

/** Entries materialised per level. The listing itself stays fully browsable. */
private const val MaxRenderedEntries = 400

/** How deep a nested container may be drilled before we stop. */
private const val MaxNestedDepth = 3

/** Crumb chips painted at once; the rest collapse into the leading ellipsis. */
private const val MaxBreadcrumbSegments = 6

/** Extracted copies kept in the cache so share / open-with stays bounded. */
private const val StagedFileBudget = 10

/** Ceiling for one staged entry, matching the parser's extraction window. */
private const val MaxStagedEntryBytes = 10 * 1024 * 1024

/**
 * Containers this viewer can unpack in place: the ZIP family plus plain TAR.
 * Everything else is still listed, and its entries keep the existing
 * "open through the ViewModel" route instead of a dead-end drill-in.
 */
private val NestedBrowsableExtensions = setOf("zip", "zipx", "jar", "apk", "aab", "tar")

/** Every container extension, used only to decide whether to show a chevron. */
private val ArchiveContainerExtensions = NestedBrowsableExtensions + setOf(
    "tgz", "gz", "7z", "rar", "xz", "zst", "bz2", "cab"
)

private val StagedArchiveDir = "archive_stage"

/* ------------------------------------------------------------------------- */
/* Small helpers                                                              */
/* ------------------------------------------------------------------------- */

private fun archiveMimeType(extension: String): String {
    val mime = FormatRegistry.lookup(extension.lowercase(Locale.US)).mimeType
    return mime.ifBlank { "application/octet-stream" }
}

private fun archiveStagedName(name: String): String {
    val cleaned = name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
    return cleaned.ifBlank { "extracted_entry" }.take(80)
}

private fun pruneStagedArchiveFiles(dir: File) {
    runCatching {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(StagedFileBudget - 1).forEach { it.delete() }
    }
}

/**
 * Streams one entry out of the archive into the app's own cache and returns a
 * FileProvider URI, so nothing ever hands a raw `file://` path to another app.
 * Runs entirely off the main thread.
 */
private suspend fun stageArchiveEntry(
    context: Context,
    archiveUri: Uri,
    entry: ArchiveEntry,
    maxBytes: Int = MaxStagedEntryBytes
): Uri? = withContext(Dispatchers.IO) {
    val bytes = ArchiveParser.extractEntry(context, archiveUri, entry.path, maxBytes)
        ?: return@withContext null
    if (bytes.isEmpty()) return@withContext null
    // The extractor stops at the ceiling without flagging a cut, so a full
    // window is treated as "too large" rather than handed out corrupt.
    if (bytes.size >= maxBytes) return@withContext null
    val dir = File(context.cacheDir, StagedArchiveDir)
    if (!dir.exists() && !dir.mkdirs()) return@withContext null
    pruneStagedArchiveFiles(dir)
    val target = File(dir, archiveStagedName(entry.name))
    val written = runCatching {
        FileOutputStream(target).use { it.write(bytes) }
    }.isSuccess
    if (!written) return@withContext null
    val shareable = ShareUri.toShareable(context, Uri.fromFile(target)) ?: return@withContext null
    shareable
}

private fun launchArchiveChooser(
    context: Context,
    target: Intent,
    title: String,
    failure: String
) {
    runCatching {
        context.startActivity(
            Intent.createChooser(target, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { Toast.makeText(context, failure, Toast.LENGTH_SHORT).show() }
}

private fun archiveEntrySubtitle(entry: ArchiveEntry, showPath: Boolean): String {
    if (entry.isDirectory) {
        val parts = mutableListOf("Folder")
        if (entry.dateStr.isNotBlank()) parts += entry.dateStr
        if (showPath && entry.path.isNotBlank()) parts += entry.path
        return parts.joinToString(" • ")
    }
    val parts = mutableListOf<String>()
    if (entry.extension.isNotBlank()) parts += entry.extension.uppercase(Locale.US)
    val size = entry.formattedSize
    if (size.isNotBlank()) parts += size
    if (entry.dateStr.isNotBlank()) parts += entry.dateStr
    if (showPath && entry.path.isNotBlank()) parts += entry.path
    return parts.joinToString(" • ")
}

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * Archive browser on the content-first reader frame.
 *
 * The page owns the whole viewport: the toolbar and the action pill float over
 * the listing and slide away while the user reads. Every listing, folder jump,
 * search, sort, extraction and share below is the real implementation.
 */
@Composable
fun ArchiveBrowserScreen(viewModel: MainViewModel) {
    val archiveEntries by viewModel.archiveEntries.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val archiveFile = ArchiveSession.resolve(selectedFile)
    val archiveId = archiveFile?.id

    var currentPathPrefix by remember(archiveId) {
        mutableStateOf(ArchiveSession.prefixFor(archiveId))
    }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(ArchiveSortMode.NAME) }
    var typeFilter by remember { mutableStateOf(ArchiveTypeFilter.ALL) }
    var stagingPath by remember { mutableStateOf<String?>(null) }

    var nestedLevels by remember(archiveId) { mutableStateOf<List<NestedArchiveLevel>>(emptyList()) }
    var nestedLoading by remember { mutableStateOf(false) }
    var nestedError by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val crumbScroll = rememberScrollState()
    var chromeVisible by remember { mutableStateOf(true) }

    val levelEntries = nestedLevels.lastOrNull()?.entries ?: archiveEntries
    val levelUri = nestedLevels.lastOrNull()?.uri ?: archiveFile?.uri
    val levelExtension = nestedLevels.lastOrNull()
        ?.name?.substringAfterLast('.', "")
        ?: archiveFile?.extension.orEmpty()
    val levelName = nestedLevels.lastOrNull()?.name ?: archiveFile?.name ?: "Archive"

    val failure = viewerState as? ViewerState.Failed
    val isLoading = viewerState is ViewerState.Loading || viewerState is ViewerState.Detecting

    /* ---------------------------- filtering ---------------------------- */

    val visibleEntries = remember(levelEntries, currentPathPrefix, searchQuery, sortMode, typeFilter) {
        val base = when {
            searchQuery.isNotBlank() -> levelEntries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.path.contains(searchQuery, ignoreCase = true)
            }

            currentPathPrefix.isBlank() -> levelEntries.filter { entry ->
                entry.path.trimEnd('/').count { it == '/' } == 0
            }

            else -> levelEntries.filter { entry ->
                // Immediate children only. A plain prefix test would also pull
                // in grandchildren such as "docs/sub/note.txt" while browsing
                // "docs/", which put deep files at the wrong level.
                entry.path.startsWith(currentPathPrefix) &&
                    entry.path != currentPathPrefix &&
                    !entry.path.removePrefix(currentPathPrefix).trimEnd('/').contains('/')
            }
        }

        val typed = when (typeFilter) {
            ArchiveTypeFilter.ALL -> base
            ArchiveTypeFilter.FOLDERS -> base.filter { it.isDirectory }
            ArchiveTypeFilter.FILES -> base.filter { !it.isDirectory }
        }

        when (sortMode) {
            ArchiveSortMode.NAME -> typed.sortedWith(
                compareByDescending<ArchiveEntry> { it.isDirectory }.thenBy { it.name.lowercase(Locale.US) }
            )

            ArchiveSortMode.SIZE -> typed.sortedWith(
                compareByDescending<ArchiveEntry> { it.isDirectory }.thenByDescending { it.sizeBytes }
            )

            ArchiveSortMode.TYPE -> typed.sortedWith(
                compareByDescending<ArchiveEntry> { it.isDirectory }.thenBy { it.extension }
            )
        }
    }

    val renderedEntries = remember(visibleEntries) { visibleEntries.take(MaxRenderedEntries) }
    val folderCount = visibleEntries.count { it.isDirectory }
    val fileCount = visibleEntries.size - folderCount
    val hasParent = currentPathPrefix.isNotBlank()
    val isSearching = searchQuery.isNotBlank()

    val crumbs = remember(levelName, currentPathPrefix) {
        val folders = currentPathPrefix.split('/').filter { it.isNotBlank() }
        val all = ArrayList<ArchiveCrumb>(folders.size + 1)
        all += ArchiveCrumb(levelName, "")
        folders.forEachIndexed { index, folder ->
            all += ArchiveCrumb(folder, folders.take(index + 1).joinToString("/") + "/")
        }
        if (all.size > MaxBreadcrumbSegments) {
            buildList {
                add(all.first())
                add(ArchiveCrumb("…", ""))
                addAll(all.takeLast(MaxBreadcrumbSegments - 2))
            }
        } else {
            all
        }
    }

    /* ------------------------- progress + chrome ----------------------- */

    val progress = if (visibleEntries.isEmpty()) {
        0f
    } else {
        ((listState.firstVisibleItemIndex + 1f) / (visibleEntries.size + 2).toFloat())
            .coerceIn(0f, 1f)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.lastScrolledBackward,
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            )
        }.collect { (scrolling, backward, atTop) ->
            if (atTop) chromeVisible = true
            else if (scrolling) chromeVisible = backward
        }
    }

    LaunchedEffect(archiveId, currentPathPrefix) {
        ArchiveSession.storePrefix(archiveId, currentPathPrefix)
    }

    /* ------------------------------ actions ---------------------------- */

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun resetToLevelTop() {
        currentPathPrefix = ""
        searchQuery = ""
        isSearchVisible = false
        scope.launch { runCatching { listState.scrollToItem(0) } }
    }

    fun openNested(entry: ArchiveEntry, parentUri: Uri) {
        if (nestedLevels.size >= MaxNestedDepth) {
            toast("Nested archives open $MaxNestedDepth levels deep")
            return
        }
        nestedLoading = true
        nestedError = null
        scope.launch {
            val stagedUri = stageArchiveEntry(context, parentUri, entry)
            if (stagedUri == null) {
                nestedLoading = false
                nestedError = "\"${entry.name}\" could not be unpacked for browsing. Only ZIP-style " +
                    "nested containers can be read in place — the original archive is untouched."
                return@launch
            }
            val children = runCatching {
                withContext(Dispatchers.IO) {
                    ArchiveParser.parseArchiveEntries(context, stagedUri, entry.extension)
                }
            }.getOrDefault(emptyList())
            nestedLoading = false
            if (children.isEmpty()) {
                nestedError = "\"${entry.name}\" opened but listed no readable entries. It may be empty, " +
                    "encrypted, or use a compression method this viewer does not support."
            } else {
                nestedLevels = nestedLevels + NestedArchiveLevel(
                    name = entry.name,
                    uri = stagedUri,
                    path = entry.path,
                    entries = children
                )
                resetToLevelTop()
            }
        }
    }

    fun navigateUp() {
        when {
            hasParent -> {
                val parent = currentPathPrefix.trimEnd('/').substringBeforeLast('/', "")
                currentPathPrefix = if (parent.isBlank()) "" else "$parent/"
                scope.launch { runCatching { listState.scrollToItem(0) } }
            }

            nestedLevels.isNotEmpty() -> {
                nestedLevels = nestedLevels.dropLast(1)
                nestedError = null
                nestedLoading = false
                resetToLevelTop()
            }

            else -> viewModel.navigateBack()
        }
    }

    fun openEntry(entry: ArchiveEntry) {
        if (entry.isDirectory) {
            currentPathPrefix = entry.path.trimEnd('/') + "/"
            searchQuery = ""
            isSearchVisible = false
            scope.launch { runCatching { listState.scrollToItem(0) } }
            return
        }
        val parentUri = levelUri
        if (parentUri == null) {
            toast("This entry cannot be opened without its carrier archive")
            return
        }
        if (entry.extension.lowercase(Locale.US) in NestedBrowsableExtensions) {
            openNested(entry, parentUri)
            return
        }
        // Remember the containing folder so returning from the preview lands
        // back where the entry came from.
        val container = entry.path.substringBeforeLast('/', "")
        val containerPrefix = if (container.isBlank()) "" else "$container/"
        currentPathPrefix = containerPrefix
        searchQuery = ""
        isSearchVisible = false
        ArchiveSession.storePrefix(archiveId, containerPrefix)
        viewModel.openFile(
            FileItem(
                id = entry.path,
                name = entry.name,
                extension = entry.extension,
                sizeBytes = entry.sizeBytes,
                formatInfo = FormatRegistry.lookup(entry.extension),
                archiveParentUri = parentUri,
                archiveEntryPath = entry.path
            )
        )
    }

    fun stageAndDeliver(entry: ArchiveEntry, openWith: Boolean) {
        val parentUri = levelUri
        if (parentUri == null) {
            toast("This entry cannot be extracted without its carrier archive")
            return
        }
        if (stagingPath != null) return
        stagingPath = entry.path
        scope.launch {
            val stagedUri = stageArchiveEntry(context, parentUri, entry)
            stagingPath = null
            if (stagedUri == null) {
                toast(
                    "Could not extract \"${entry.name}\". It may be larger than the 10 MB " +
                        "extraction window or use a compression method this viewer does not support."
                )
                return@launch
            }
            val mime = archiveMimeType(entry.extension)
            if (openWith) {
                val intent = ShareUri.viewIntent(context, stagedUri, mime)
                if (intent == null) {
                    toast("Nothing on this device can open \"${entry.name}\"")
                    return@launch
                }
                launchArchiveChooser(
                    context = context,
                    target = intent,
                    title = "Open \"${entry.name}\" with",
                    failure = "No app accepted \"${entry.name}\""
                )
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_SUBJECT, entry.name)
                }
                if (!ShareUri.attachStream(context, intent, stagedUri)) {
                    toast("This entry cannot be shared from its location")
                    return@launch
                }
                launchArchiveChooser(
                    context = context,
                    target = intent,
                    title = "Share \"${entry.name}\"",
                    failure = "No app accepted \"${entry.name}\""
                )
            }
        }
    }

    fun shareArchive() {
        val uri = levelUri
        if (uri == null) {
            toast("There is no archive to share")
            return
        }
        val mime = archiveMimeType(levelExtension)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_SUBJECT, levelName)
        }
        if (!ShareUri.attachStream(context, intent, uri)) {
            toast("This archive's location cannot be handed to another app")
            return
        }
        launchArchiveChooser(
            context = context,
            target = intent,
            title = "Share $levelName",
            failure = "No app accepted this archive"
        )
    }

    fun openArchiveWith() {
        val uri = levelUri
        if (uri == null) {
            toast("There is no archive to open")
            return
        }
        val intent = ShareUri.viewIntent(context, uri, archiveMimeType(levelExtension))
        if (intent == null) {
            toast("This archive's location cannot be handed to another app")
            return
        }
        launchArchiveChooser(
            context = context,
            target = intent,
            title = "Open $levelName with",
            failure = "No app on this device can open this archive"
        )
    }

    /* ------------------------------- frame ----------------------------- */

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    // Chrome floats above the listing, so the page reserves only
                    // a slim top inset and keeps its breathing room in the
                    // scroll content so the last row clears the action pill.
                    .padding(top = VennelaTokens.viewerTopInset),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = VennelaTokens.spaceXs,
                    bottom = VennelaTokens.spaceHuge + VennelaTokens.spaceXl
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                item(key = "trail") {
                    ArchiveTrailBar(
                        crumbs = crumbs,
                        nestedDepth = nestedLevels.size,
                        scrollState = crumbScroll,
                        onCrumb = { crumb ->
                            currentPathPrefix = crumb.prefix
                            scope.launch { runCatching { listState.scrollToItem(0) } }
                        },
                        onUp = if (hasParent || nestedLevels.isNotEmpty()) {
                            { navigateUp() }
                        } else {
                            null
                        }
                    )
                }

                item(key = "summary") {
                    ArchiveSummaryRow(
                        items = visibleEntries.size,
                        folders = folderCount,
                        files = fileCount,
                        sortLabel = sortMode.label,
                        filterLabel = typeFilter.label
                    )
                }

                if (stagingPath != null) {
                    item(key = "staging") {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.38f
                        ) {
                            VennelaLoadingState(label = "Extracting entry…")
                        }
                    }
                }

                if (nestedLoading) {
                    item(key = "nested-loading") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = VennelaTokens.touchTargetCompact),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = accent.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(VennelaTokens.iconSm)
                            )
                            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                            Text(
                                text = "Reading the nested archive index…",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.micro
                            )
                        }
                    }
                }

                // A failed nested drill-in must never hide the listing that is
                // still perfectly browsable, so it sits above it.
                if (nestedError != null) {
                    item(key = "nested-error") {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.38f
                        ) {
                            VennelaErrorState(
                                title = "Couldn't open this nested archive",
                                message = nestedError.orEmpty(),
                                onRetry = { nestedError = null },
                                onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                            )
                        }
                    }
                }

                when {
                    failure != null -> item(key = "error") {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.38f
                        ) {
                            VennelaErrorState(
                                title = "Couldn't read this archive",
                                message = failure.reason,
                                onRetry = if (failure.canRetry && archiveFile != null) {
                                    { viewModel.openFile(archiveFile) }
                                } else {
                                    null
                                },
                                onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                            )
                        }
                    }

                    (isLoading || nestedLoading) && levelEntries.isEmpty() -> item(key = "loading") {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.38f
                        ) {
                            VennelaLoadingState(
                                label = if (nestedLoading) "Reading nested archive index…" else "Reading archive index…"
                            )
                        }
                    }

                    visibleEntries.isEmpty() -> item(key = "empty") {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.38f
                        ) {
                            when {
                                archiveFile == null -> VennelaEmptyState(
                                    title = "No archive open",
                                    hint = "Open a ZIP, TAR, 7Z or GZ file from Files to browse its " +
                                        "entries without extracting it.",
                                    icon = Icons.Default.FolderZip,
                                    actionLabel = "Browse files",
                                    onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                                )

                                levelEntries.isEmpty() -> VennelaEmptyState(
                                    title = "This archive is empty",
                                    hint = "The container opened successfully but holds no entries. " +
                                        "The original file is untouched.",
                                    icon = Icons.Default.FolderOpen,
                                    actionLabel = "File properties",
                                    onAction = { viewModel.navigateTo(Screen.FileMetadata) }
                                )

                                isSearching -> VennelaEmptyState(
                                    title = "No matches for \"$searchQuery\"",
                                    hint = "This level holds ${levelEntries.size} entries — clear the " +
                                        "search to browse them all.",
                                    icon = Icons.Default.Search,
                                    actionLabel = "Clear search",
                                    onAction = { searchQuery = "" }
                                )

                                else -> VennelaEmptyState(
                                    title = "Nothing at this level",
                                    hint = "This folder is empty or filtered out. Jump back to the " +
                                        "archive root to keep browsing.",
                                    icon = Icons.Default.FolderOpen,
                                    actionLabel = "Archive root",
                                    onAction = {
                                        currentPathPrefix = ""
                                        typeFilter = ArchiveTypeFilter.ALL
                                    }
                                )
                            }
                        }
                    }

                    else -> itemsIndexed(
                        items = renderedEntries,
                        key = { index, _ -> "entry_$index" }
                    ) { index, entry ->
                        ArchiveEntryRow(
                            entry = entry,
                            index = index,
                            subtitle = archiveEntrySubtitle(entry, isSearching),
                            canExtract = levelUri != null,
                            isNestedContainer = entry.extension.lowercase(Locale.US) in ArchiveContainerExtensions,
                            onOpen = { openEntry(entry) },
                            onExtract = { stageAndDeliver(entry, openWith = false) },
                            onOpenWith = { stageAndDeliver(entry, openWith = true) }
                        )
                    }
                }

                if (visibleEntries.size > renderedEntries.size) {
                    item(key = "more") {
                        Text(
                            text = "Showing the first ${renderedEntries.size} of ${visibleEntries.size} " +
                                "entries in this level. Narrow the search to see the rest.",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.micro,
                            modifier = Modifier.padding(top = VennelaTokens.spaceXs)
                        )
                    }
                }

                item(key = "footer") {
                    Text(
                        text = buildString {
                            append("Extracted copies stay on this device only · ")
                            append("$fileCount files at this level")
                            if (nestedLevels.isNotEmpty()) {
                                append(" · nested level ${nestedLevels.size} of $MaxNestedDepth")
                            }
                        },
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro,
                        modifier = Modifier.padding(top = VennelaTokens.spaceXs)
                    )
                }
            }

            // Hairline progress, Google Docs style.
            ReaderProgressRule(
                progress = progress,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Floating toolbar — hides while scrolling forward.
            ReaderChromeVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopToolbar(
                    title = levelName,
                    subtitle = buildString {
                        if (nestedLevels.isNotEmpty()) append("Nested · ")
                        append(archiveFile?.formatInfo?.name ?: "Archive")
                        val size = archiveFile?.formattedSize
                        if (!size.isNullOrBlank()) append(" • ").append(size)
                        append(" • ").append(levelEntries.size).append(" entries")
                    },
                    onBack = { navigateUp() }
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = if (isSearchVisible) {
                            "Close archive search"
                        } else {
                            "Search inside this archive"
                        },
                        active = isSearchVisible,
                        onClick = {
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) searchQuery = ""
                            chromeVisible = true
                        }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.SwapVert,
                        contentDescription = "Sort by ${sortMode.label}",
                        active = sortMode != ArchiveSortMode.NAME,
                        onClick = {
                            val next = when (sortMode) {
                                ArchiveSortMode.NAME -> ArchiveSortMode.SIZE
                                ArchiveSortMode.SIZE -> ArchiveSortMode.TYPE
                                ArchiveSortMode.TYPE -> ArchiveSortMode.NAME
                            }
                            sortMode = next
                            toast("Sorted by ${next.label}")
                        }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share this archive",
                        onClick = { shareArchive() }
                    )
                }
            }

            // Floating action pill — also hides while reading.
            ReaderChromeVisibility(
                visible = chromeVisible,
                fromTop = false,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomPill(
                    status = if (visibleEntries.isEmpty()) {
                        "No entries"
                    } else {
                        "${visibleEntries.size} items"
                    }
                ) {
                    ReaderIconButton(
                        icon = if (typeFilter == ArchiveTypeFilter.FOLDERS) {
                            Icons.Default.Folder
                        } else {
                            Icons.Default.FolderOpen
                        },
                        contentDescription = "Filter: ${typeFilter.label}",
                        active = typeFilter != ArchiveTypeFilter.ALL,
                        onClick = {
                            typeFilter = when (typeFilter) {
                                ArchiveTypeFilter.ALL -> ArchiveTypeFilter.FOLDERS
                                ArchiveTypeFilter.FOLDERS -> ArchiveTypeFilter.FILES
                                ArchiveTypeFilter.FILES -> ArchiveTypeFilter.ALL
                            }
                        }
                    )
                    if (hasParent || nestedLevels.isNotEmpty()) {
                        ReaderIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Up one level",
                            onClick = { navigateUp() }
                        )
                    }
                    ReaderIconButton(
                        icon = Icons.Default.FolderZip,
                        contentDescription = "Archive properties",
                        onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open this archive in another app",
                        onClick = { openArchiveWith() }
                    )
                }
            }

            // Find bar floats under the toolbar instead of pushing the page down.
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = VennelaTokens.viewerTopInset + VennelaTokens.spaceXl)
                    .padding(horizontal = VennelaTokens.gutter)
            ) {
                ArchiveFindBar(
                    query = searchQuery,
                    matchCount = visibleEntries.size,
                    scopeLabel = if (nestedLevels.isNotEmpty()) levelName else archiveFile?.name ?: "archive",
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Trail bar                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun ArchiveTrailBar(
    crumbs: List<ArchiveCrumb>,
    nestedDepth: Int,
    scrollState: ScrollState,
    onCrumb: (ArchiveCrumb) -> Unit,
    onUp: (() -> Unit)?
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.touchTargetCompact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FolderZip,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
            crumbs.forEachIndexed { index, crumb ->
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
                val isLast = index == crumbs.lastIndex
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .then(
                            if (isLast) {
                                Modifier
                            } else {
                                Modifier.pressScale(pressedScale = 0.96f) { onCrumb(crumb) }
                            }
                        )
                        .padding(horizontal = VennelaTokens.spaceXs, vertical = VennelaTokens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = crumb.label,
                        color = if (isLast) palette.textPrimary else accent.secondary,
                        fontSize = VennelaTokens.micro,
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (nestedDepth > 0) {
                Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                Text(
                    text = "level ${nestedDepth + 1}",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.nano,
                    maxLines = 1
                )
            }
        }
        if (onUp != null) {
            Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
            ReaderIconButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up one level",
                onClick = onUp
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Summary row                                                                */
/* ------------------------------------------------------------------------- */

@Composable
private fun ArchiveSummaryRow(
    items: Int,
    folders: Int,
    files: Int,
    sortLabel: String,
    filterLabel: String
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    if (items <= 0) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CONTENTS",
            color = accent.primary,
            fontSize = VennelaTokens.nano,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = "$items items • $folders folders • $files files",
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$sortLabel • $filterLabel",
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            maxLines = 1
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Entry row                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun ArchiveEntryRow(
    entry: ArchiveEntry,
    index: Int,
    subtitle: String,
    canExtract: Boolean,
    isNestedContainer: Boolean,
    onOpen: () -> Unit,
    onExtract: () -> Unit,
    onOpenWith: () -> Unit
) {
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index, enabled = index < 12)
            .pressScale(pressedScale = 0.975f, onClick = onOpen),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        alpha = 0.40f,
        elevation = VennelaTokens.elevationRaised
    ) {
        GlassListRow(
            title = entry.name,
            subtitle = subtitle,
            modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
            leading = {
                if (entry.isDirectory) {
                    IconBadge(
                        icon = Icons.Default.Folder,
                        colors = listOf(accent.primary, accent.secondary),
                        size = VennelaTokens.badgeSm,
                        radius = VennelaTokens.radiusChip,
                        contentDescription = "Folder"
                    )
                } else {
                    FileBrandBadge(
                        extension = entry.extension.ifBlank { "bin" },
                        size = VennelaTokens.badgeSm
                    )
                }
            },
            trailing = {
                if (!entry.isDirectory && canExtract) {
                    ReaderIconButton(
                        icon = Icons.Default.FileDownload,
                        contentDescription = "Extract and share ${entry.name}",
                        tint = accent.secondary,
                        onClick = onExtract
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXxs))
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open ${entry.name} in another app",
                        onClick = onOpenWith
                    )
                }
            },
            showChevron = entry.isDirectory || isNestedContainer
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Find bar                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
private fun ArchiveFindBar(
    query: String,
    matchCount: Int,
    scopeLabel: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.52f,
        elevation = VennelaTokens.elevationRaised,
        sheen = 0.14f
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusControl))
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search inside this archive…",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.bodySmall,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(accent.secondary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VennelaTokens.touchTargetCompact)
                )
            }
            ReaderIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Clear archive search",
                onClick = onClear
            )
        }
        Text(
            text = when {
                query.isBlank() -> "$scopeLabel • type to filter entries"
                matchCount == 0 -> "No matches in this archive"
                else -> "$matchCount matches in $scopeLabel"
            },
            color = if (matchCount == 0 && query.isNotBlank()) palette.textTertiary else accent.secondary,
            fontSize = VennelaTokens.nano,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                start = VennelaTokens.iconSm + VennelaTokens.spaceSm,
                top = VennelaTokens.spaceXxs
            )
        )
    }
}
