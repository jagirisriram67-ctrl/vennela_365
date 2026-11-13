package com.vennela365.viewer.ui.screens

import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.CatAmber
import com.vennela365.viewer.theme.CatBlue
import com.vennela365.viewer.theme.CatCyan
import com.vennela365.viewer.theme.CatEmerald
import com.vennela365.viewer.theme.CatOrange
import com.vennela365.viewer.theme.CatRose
import com.vennela365.viewer.theme.CatViolet
import com.vennela365.viewer.theme.errorCoral
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidIconButton
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.VennelaMetadataRow
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.BrowseCrumb
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import com.vennela365.viewer.viewmodel.SortMode
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.vennela365.viewer.storage.ShareUri

private val imageThumbExtensions = setOf(
    "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "jfif"
)

/**
 * Folders are identified by a coloured gradient badge. Colours come from the
 * named category tokens in the theme — never raw hex — so the badge re-tunes
 * with the active theme.
 */
private data class FolderVisual(val icon: ImageVector, val start: Color, val end: Color)

private fun folderVisual(name: String): FolderVisual = when {
    name.contains("doc", true) -> FolderVisual(Icons.Default.Description, CatAmber, CatOrange)
    name.contains("download", true) -> FolderVisual(Icons.Default.Download, CatBlue, CatCyan)
    name.contains("picture", true) || name.contains("dcim", true) || name.contains("image", true) ->
        FolderVisual(Icons.Default.Image, CatViolet, CatBlue)
    name.contains("movie", true) || name.contains("video", true) ->
        FolderVisual(Icons.Default.Movie, CatEmerald, CatCyan)
    name.contains("music", true) || name.contains("audio", true) ->
        FolderVisual(Icons.Default.MusicNote, CatRose, CatOrange)
    name.contains("android", true) -> FolderVisual(Icons.Default.Android, CatEmerald, CatBlue)
    name.contains("app", true) -> FolderVisual(Icons.Default.Apps, CatViolet, CatRose)
    name.contains("camera", true) -> FolderVisual(Icons.Default.PhotoCamera, CatCyan, CatBlue)
    else -> FolderVisual(Icons.Default.Folder, CatBlue, CatCyan)
}

private val sortOptions: List<Pair<SortMode, String>> = listOf(
    SortMode.NAME_ASC to "Name (A-Z)",
    SortMode.NAME_DESC to "Name (Z-A)",
    SortMode.DATE_NEWEST to "Newest first",
    SortMode.DATE_OLDEST to "Oldest first",
    SortMode.SIZE_LARGEST to "Largest first",
    SortMode.SIZE_SMALLEST to "Smallest first",
    SortMode.TYPE to "File type"
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(viewModel: MainViewModel) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val browseItems by viewModel.browseItems.collectAsState()
    val crumbs by viewModel.browseCrumbs.collectAsState()
    val rootUri by viewModel.browseRootUri.collectAsState()
    val loading by viewModel.browseLoading.collectAsState()
    val error by viewModel.browseError.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    var gridMode by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(emptySet<String>()) }
    var infoFile by remember { mutableStateOf<FileItem?>(null) }
    var pendingDelete by remember { mutableStateOf(emptyList<FileItem>()) }

    val openTree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.setBrowseRoot(it) }
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.openUri(it, null) }
    }

    BackHandler(enabled = crumbs.size > 1) { viewModel.browseBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val storage = remember { explorerStorageStats() }
    val isBrowsing = rootUri != null
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    // Real data, sorted through the shared view-model sort mode.
    val sourceItems = if (isBrowsing) browseItems else recentFiles
    val displayItems = remember(sourceItems, sortMode) { viewModel.sortedFiles(sourceItems) }
    val selectedFiles = remember(selection, displayItems) { displayItems.filter { it.id in selection } }
    val selectionActive = selection.isNotEmpty()
    val canDelete = isBrowsing && selectedFiles.isNotEmpty() && selectedFiles.all { !it.isDirectory }

    fun toggleSelection(file: FileItem) {
        selection = if (file.id in selection) selection - file.id else selection + file.id
    }

    fun openOrSelect(file: FileItem) {
        if (selectionActive) {
            toggleSelection(file)
        } else if (file.isDirectory) {
            viewModel.browseInto(file)
        } else {
            viewModel.openFile(file)
        }
    }

    // The header bar slides away while the list scrolls down, so the files
    // own the screen.
    val explorerGridState = rememberLazyGridState()

    fun shareSelection() {
        val uris = selectedFiles.mapNotNull { it.uri }
        if (uris.isEmpty()) {
            Toast.makeText(context, "These items have no shareable location", Toast.LENGTH_SHORT).show()
            return
        }
        // Direct-path items carry file:// URIs; export them through the
        // FileProvider so the receiving app can actually read the stream.
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply { type = "*/*" }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "*/*" }
        }
        val attached = if (uris.size == 1) {
            ShareUri.attachStream(context, intent, uris.first())
        } else {
            ShareUri.attachStreams(context, intent, uris)
        }
        if (!attached) {
            Toast.makeText(context, "These items have no shareable location", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent.createChooser(intent, "Share ${uris.size} item(s)"))
    }

    fun copySelection() {
        val text = selectedFiles.joinToString("\n") { item ->
            item.path.ifBlank { item.uri?.toString() ?: item.name }
        }
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "Copied ${selectedFiles.size} name(s)", Toast.LENGTH_SHORT).show()
    }

    fun confirmDelete() {
        val doomed = selectedFiles
        pendingDelete = emptyList()
        scope.launch {
            val failed = withContext(Dispatchers.IO) {
                doomed.filter { item ->
                    val uri = item.uri
                    if (uri == null) {
                        true
                    } else {
                        runCatching { !DocumentsContract.deleteDocument(context.contentResolver, uri) }
                            .getOrDefault(true)
                    }
                }
            }
            selection = emptySet()
            if (failed.isEmpty()) {
                Toast.makeText(context, "Deleted ${doomed.size} item(s)", Toast.LENGTH_SHORT).show()
                viewModel.refreshBrowse()
            } else {
                Toast.makeText(
                    context,
                    "Could not delete ${failed.size} item(s) - access is read-only",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LiquidPage(
        title = if (isBrowsing) (crumbs.lastOrNull()?.name ?: "Folder") else "Your library",
        subtitle = when {
            selectionActive -> "${selection.size} selected"
            isBrowsing -> "On-device folder - read only"
            else -> "Recently opened files"
        },
        onBack = { if (!viewModel.browseBack()) viewModel.navigateBack() },
        scrollState = explorerGridState,
        dock = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                if (selectionActive) {
                    ExplorerSelectionBar(
                        count = selection.size,
                        canDelete = canDelete,
                        onShare = { shareSelection() },
                        onDelete = { pendingDelete = selectedFiles },
                        onInfo = { infoFile = selectedFiles.firstOrNull() },
                        onCopy = { copySelection() },
                        onClear = { selection = emptySet() },
                        modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
                    )
                } else {
                    GlassAddButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.padding(end = VennelaTokens.gutter, bottom = VennelaTokens.spaceSm)
                    )
                }
                FloatingDockNav(
                    activeTab = "files",
                    onTabSelected = { tab ->
                        when (tab) {
                            "home" -> viewModel.navigateTo(Screen.Home)
                            "files" -> Unit
                            "search" -> viewModel.navigateTo(Screen.UniversalSearch)
                            "favorites" -> viewModel.navigateTo(Screen.Favorites)
                            "settings" -> viewModel.navigateTo(Screen.Settings)
                        }
                    }
                )
            }
        },
        topBarActions = {
            LiquidIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search files",
                onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
            )
            LiquidIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "More options",
                onClick = { showAddSheet = true }
            )
        }
    ) { pagePadding ->
        LazyVerticalGrid(
            columns = if (gridMode) GridCells.Fixed(2) else GridCells.Fixed(1),
            state = explorerGridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceXs,
                bottom = pagePadding.calculateBottomPadding() + 64.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            // ---- Frosted breadcrumb capsule + animated list/grid toggle ----
            item(key = "crumbs", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBrowsing) {
                        BreadcrumbCapsule(
                            crumbs = crumbs,
                            onCrumbClick = { viewModel.browseToCrumb(it) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        GlassSurface(
                            shape = RoundedCornerShape(VennelaTokens.radiusPill),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = VennelaTokens.spaceLg,
                                    vertical = VennelaTokens.spaceMd
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = accent.secondary,
                                    modifier = Modifier.size(VennelaTokens.iconSm)
                                )
                                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                                Text(
                                    "Files you opened",
                                    color = palette.textPrimary,
                                    fontSize = VennelaTokens.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))

                    ViewModeToggle(
                        gridMode = gridMode,
                        onModeChange = { gridMode = it }
                    )
                }
            }

            // ---- Storage usage bar ----
            item(key = "storage", span = { GridItemSpan(maxLineSpan) }) {
                StorageUsageCard(
                    storage = storage,
                    itemCount = displayItems.size,
                    isBrowsing = isBrowsing,
                    sortLabel = sortOptions.firstOrNull { it.first == sortMode }?.second ?: "Sort",
                    onSortClick = { showSortSheet = true }
                )
            }

            // ---- Selection summary ----
            if (selectionActive) {
                item(key = "selection", span = { GridItemSpan(maxLineSpan) }) {
                    GlassSurface(shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = VennelaTokens.spaceLg,
                                    vertical = VennelaTokens.spaceMd
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selection.size} of ${displayItems.size} selected",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Select all",
                                color = accent.secondary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .pressScale(0.94f) { selection = displayItems.map { it.id }.toSet() }
                                    .padding(VennelaTokens.spaceSm)
                            )
                            Text(
                                "Clear",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall,
                                modifier = Modifier
                                    .pressScale(0.94f) { selection = emptySet() }
                                    .padding(VennelaTokens.spaceSm)
                            )
                        }
                    }
                }
            }

            // ---- States ----
            if (isBrowsing) {
                if (loading) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                        VennelaLoadingState(label = "Reading folder...")
                    }
                } else if (error != null) {
                    item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                        VennelaErrorState(
                            title = "Folder unavailable",
                            message = error ?: "Grant access again to continue.",
                            onRetry = { viewModel.refreshBrowse() },
                            onDetails = { openTree.launch(rootUri) }
                        )
                    }
                } else if (browseItems.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        VennelaEmptyState(
                            title = "This folder is empty",
                            hint = "Nothing to preview here. Pick another folder or open a file directly.",
                            icon = Icons.Default.FolderOpen,
                            actionLabel = "Open a file",
                            onAction = { openDocument.launch(arrayOf("*/*")) }
                        )
                    }
                }
            } else {
                item(key = "recent-header", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Files",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accent.primary, accent.secondary)
                                    )
                                )
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.32f),
                                    RoundedCornerShape(VennelaTokens.radiusPill)
                                )
                                .pressScale(0.94f) { openTree.launch(null) }
                                .padding(
                                    horizontal = VennelaTokens.spaceMd,
                                    vertical = VennelaTokens.spaceSm
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(VennelaTokens.iconSm)
                            )
                            Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                            Text(
                                "Browse a folder",
                                color = Color.White,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (recentFiles.isEmpty()) {
                    item(key = "recent-empty", span = { GridItemSpan(maxLineSpan) }) {
                        VennelaEmptyState(
                            title = "No files opened yet",
                            hint = "Grant folder access to browse everything, or pick a single file. Access stays on this device.",
                            icon = Icons.Default.FolderOpen,
                            actionLabel = "Grant folder access",
                            onAction = { openTree.launch(null) }
                        )
                    }
                }
            }

            // ---- Content ----
            itemsIndexed(displayItems, key = { _, file -> file.id }) { index, file ->
                if (file.isDirectory) {
                    if (gridMode) {
                        ExplorerFolderCard(
                            file = file,
                            selected = file.id in selection,
                            selectionActive = selectionActive,
                            modifier = Modifier.staggeredEntrance(index),
                            onClick = { openOrSelect(file) },
                            onLongClick = { toggleSelection(file) }
                        )
                    } else {
                        ExplorerFolderRow(
                            file = file,
                            selected = file.id in selection,
                            selectionActive = selectionActive,
                            modifier = Modifier.staggeredEntrance(index),
                            onClick = { openOrSelect(file) },
                            onLongClick = { toggleSelection(file) }
                        )
                    }
                } else {
                    if (gridMode) {
                        ExplorerFileCard(
                            file = file,
                            selected = file.id in selection,
                            selectionActive = selectionActive,
                            modifier = Modifier.staggeredEntrance(index),
                            onClick = { openOrSelect(file) },
                            onLongClick = { toggleSelection(file) }
                        )
                    } else {
                        ExplorerFileRow(
                            file = file,
                            selected = file.id in selection,
                            selectionActive = selectionActive,
                            modifier = Modifier.staggeredEntrance(index),
                            onClick = { openOrSelect(file) },
                            onLongClick = { toggleSelection(file) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceXl)
                    .padding(bottom = VennelaTokens.spaceXxl),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                Text(
                    "Add to Vennela",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Read-only access. Nothing is uploaded or modified.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                SheetAction(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    title = "Open a file",
                    subtitle = "Pick any file - 1,000+ formats recognized"
                ) {
                    showAddSheet = false
                    openDocument.launch(arrayOf("*/*"))
                }
                SheetAction(
                    icon = Icons.Default.CreateNewFolder,
                    title = "Grant folder access",
                    subtitle = "Browse a folder tree with real names and sizes"
                ) {
                    showAddSheet = false
                    openTree.launch(null)
                }
                if (isBrowsing) {
                    SheetAction(
                        icon = Icons.Default.Refresh,
                        title = "Refresh folder",
                        subtitle = "Re-read the current folder"
                    ) {
                        showAddSheet = false
                        viewModel.refreshBrowse()
                    }
                    SheetAction(
                        icon = Icons.Default.LinkOff,
                        title = "Forget this folder",
                        subtitle = "Release the saved read-only grant"
                    ) {
                        showAddSheet = false
                        viewModel.clearBrowseRoot()
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceXl)
                    .padding(bottom = VennelaTokens.spaceXxl),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                Text(
                    "Sort files",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Applies to every folder and to your recent files.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                sortOptions.forEach { (mode, label) ->
                    val isActive = mode == sortMode
                    SheetAction(
                        icon = if (isActive) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Sort,
                        title = label,
                        subtitle = if (isActive) "Currently applied" else "Tap to apply",
                        active = isActive
                    ) {
                        viewModel.setSortMode(mode)
                        showSortSheet = false
                    }
                }
            }
        }
    }

    infoFile?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { infoFile = null },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceXl)
                    .padding(bottom = VennelaTokens.spaceXxl)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (file.isDirectory) {
                        IconBadge(
                            icon = folderVisual(file.name).icon,
                            colors = listOf(folderVisual(file.name).start, folderVisual(file.name).end),
                            size = VennelaTokens.badgeMd,
                            radius = VennelaTokens.radiusChip,
                            contentDescription = null
                        )
                    } else {
                        FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeMd)
                    }
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (file.isDirectory) "Folder" else file.formatInfo.name,
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.caption
                        )
                    }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                GlassDivider()
                VennelaMetadataRow("Name", file.name)
                VennelaMetadataRow(
                    "Kind",
                    if (file.isDirectory) "Folder" else "${file.extension.uppercase()} - ${file.formatInfo.category.displayName}"
                )
                VennelaMetadataRow("Size", file.formattedSize)
                VennelaMetadataRow("Modified", file.formattedDate)
                VennelaMetadataRow("Location", file.path.ifBlank { file.uri?.toString() ?: "In-app result" })
                if (file.isDirectory && file.itemCount > 0) {
                    VennelaMetadataRow("Items", "${file.itemCount}")
                }
            }
        }
    }

    if (pendingDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingDelete = emptyList() },
            containerColor = palette.cardSurfaceStrong,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Delete ${pendingDelete.size} item(s)?") },
            text = {
                Text(
                    pendingDelete.take(4).joinToString("\n") { it.name } +
                        if (pendingDelete.size > 4) "\n+${pendingDelete.size - 4} more" else ""
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete() }) {
                    Text("Delete", color = errorCoral, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = emptyList() }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Chrome                                                                     */
/* ------------------------------------------------------------------------- */

/** A frosted capsule holding the SAF breadcrumb trail; scrolls, never clips. */
@Composable
private fun BreadcrumbCapsule(
    crumbs: List<BrowseCrumb>,
    onCrumbClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val scroll = rememberScrollState()

    LaunchedEffect(crumbs.size) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    GlassSurface(shape = RoundedCornerShape(VennelaTokens.radiusPill), modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
            crumbs.forEachIndexed { index, crumb ->
                if (index > 0) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
                val isLast = index == crumbs.lastIndex
                Text(
                    text = crumb.name,
                    color = if (isLast) palette.textPrimary else accent.secondary,
                    fontSize = VennelaTokens.bodySmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .pressScale(0.94f) { onCrumbClick(index) }
                        .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceSm)
                )
            }
        }
    }
}

/** Segmented list/grid switch whose glowing lozenge slides between the two. */
@Composable
private fun ViewModeToggle(
    gridMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current
    val spec = liquidGlassSpec(
        alpha = 0.46f,
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationBar,
        sheen = 0.24f,
        highlight = 0.20f
    ).copy(
        rim = accent.secondary.copy(alpha = 0.56f),
        shadowColor = accent.primary.copy(alpha = 0.36f)
    )
    val shape = RoundedCornerShape(VennelaTokens.radiusTile)

    BoxWithConstraints(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.40f),
                spotColor = accent.primary.copy(alpha = 0.40f)
            )
            .liquidGlass(spec, shape)
            .padding(VennelaTokens.spaceXs)
    ) {
        val half = maxWidth / 2
        val offset by animateDpAsState(
            targetValue = if (gridMode) half else 0.dp,
            animationSpec = LiquidMotion.liquid(),
            label = "viewModeOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(half)
                .fillMaxHeight()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(VennelaTokens.radiusControl),
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.60f),
                    spotColor = accent.primary.copy(alpha = 0.60f)
                )
                .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.38f),
                    RoundedCornerShape(VennelaTokens.radiusControl)
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            ToggleSegment(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "List view",
                active = !gridMode,
                tint = if (!gridMode) palette.textPrimary else palette.textTertiary,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(false) }
            )
            ToggleSegment(
                icon = Icons.Default.GridView,
                contentDescription = "Grid view",
                active = gridMode,
                tint = if (gridMode) palette.textPrimary else palette.textTertiary,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(true) }
            )
        }
    }
}

@Composable
private fun ToggleSegment(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.14f else 1f,
        animationSpec = LiquidMotion.press(),
        label = "toggleSegment"
    )
    Box(
        modifier = modifier
            .pressScale(0.90f, onClick)
            .padding(VennelaTokens.spaceXs),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(VennelaTokens.iconMd)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
private fun StorageUsageCard(
    storage: ExplorerStorageInfo,
    itemCount: Int,
    isBrowsing: Boolean,
    sortLabel: String,
    onSortClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    // The bar grows from empty whenever the real figure changes, so storage
    // pressure is felt as motion instead of read as a static label.
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(storage.fractionUsed) {
        fraction.animateTo(
            targetValue = storage.fractionUsed.coerceIn(0f, 1f),
            animationSpec = LiquidMotion.emphasis(LiquidMotion.durationHero)
        )
    }
    // Slow aurora sheen travelling along the hero bar.
    val sheen = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            sheen.snapTo(0f)
            sheen.animateTo(1f, LiquidMotion.shimmer(LiquidMotion.durationHero * 2))
        }
    }
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.44f),
                spotColor = accent.primary.copy(alpha = 0.44f)
            )
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.56f,
                    frost = VennelaTokens.frostRegular,
                    elevation = VennelaTokens.elevationCard,
                    sheen = 0.26f,
                    highlight = 0.22f
                ),
                shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = VennelaTokens.spaceLg,
                    vertical = VennelaTokens.spaceLg
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = accent.secondary,
                    modifier = Modifier.size(VennelaTokens.iconMd)
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Text(
                    "Internal storage",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(fraction.value * 100f).roundToInt()}%",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = VennelaTokens.spaceSm)
                )
                SortCapsule(label = sortLabel, onClick = onSortClick)
            }

            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(palette.textPrimary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.secondary, accent.primary, accent.secondary)
                            )
                        )
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            if (w > 0f) {
                                val x = -w + sheen.value * (w * 3f)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.45f),
                                            Color.Transparent
                                        ),
                                        startX = x - w * 0.45f,
                                        endX = x + w * 0.45f
                                    )
                                )
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (storage.totalBytes > 0) {
                        "${explorerFormatBytes(storage.usedBytes)} of ${explorerFormatBytes(storage.totalBytes)} used"
                    } else {
                        "Storage information unavailable"
                    },
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = buildString {
                        append("$itemCount item")
                        if (itemCount != 1) append("s")
                        if (!isBrowsing) append(" in recents")
                        if (storage.totalBytes > 0) {
                            append(" - ")
                            append(explorerFormatBytes(storage.totalBytes - storage.usedBytes))
                            append(" available")
                        }
                    },
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.micro,
                    maxLines = 1
                )
            }
        }
    }
}

/** Frosted pill that names the active sort and opens the sort sheet. */
@Composable
private fun SortCapsule(label: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.48f,
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationRaised,
        sheen = 0.22f,
        highlight = 0.18f
    ).copy(
        rim = accent.secondary.copy(alpha = 0.62f),
        shadowColor = accent.primary.copy(alpha = 0.34f)
    )
    Row(
        modifier = Modifier
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(0.94f, onClick)
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Sort,
            contentDescription = null,
            tint = accent.secondary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Text(
            label,
            color = palette.textPrimary,
            fontSize = VennelaTokens.caption,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** The floating frosted "+" that grants files and folders. */
@Composable
private fun GlassAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.74f,
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationDock,
        sheen = 0.30f,
        highlight = 0.24f
    ).copy(
        tint = accent.primary.copy(alpha = 0.88f),
        tintEnd = accent.secondary.copy(alpha = 0.66f),
        rim = Color.White.copy(alpha = 0.52f),
        shadowColor = accent.secondary.copy(alpha = 0.60f)
    )

    Box(
        modifier = modifier
            .size(62.dp)
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.66f),
                spotColor = accent.primary.copy(alpha = 0.66f)
            )
            .liquidGlass(spec, CircleShape)
            .pressScale(0.92f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add files or folders",
            tint = LocalVennelaPalette.current.textPrimary,
            modifier = Modifier.size(VennelaTokens.iconXl)
        )
    }
}

/** Glass action bar shown while a multi-selection is live. */
@Composable
private fun ExplorerSelectionBar(
    count: Int,
    canDelete: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    GlassSurface(
        shape = RoundedCornerShape(VennelaTokens.radiusDock),
        selected = true,
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$count selected",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            SelectionAction(Icons.Default.Share, "Share selected", true, accent.secondary, onShare)
            SelectionAction(Icons.Default.Delete, "Delete selected", canDelete, errorCoral, onDelete)
            SelectionAction(Icons.Default.Info, "Info for selected", true, accent.secondary, onInfo)
            SelectionAction(Icons.Default.ContentCopy, "Copy selected names", true, accent.secondary, onCopy)
            SelectionAction(Icons.Default.Close, "Exit selection", true, palette.textSecondary, onClear)
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(0.90f, if (enabled) onClick else null),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = if (active) 0.62f else 0.40f,
        frost = VennelaTokens.frostLight,
        elevation = if (active) VennelaTokens.elevationBar else VennelaTokens.elevationRaised,
        sheen = 0.22f,
        highlight = 0.18f
    )
    val shape = RoundedCornerShape(VennelaTokens.radiusTile)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (active) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = accent.secondary.copy(alpha = 0.52f),
                        spotColor = accent.primary.copy(alpha = 0.52f)
                    )
                } else {
                    Modifier
                }
            )
            .liquidGlass(
                if (active) spec.copy(rim = accent.secondary.copy(alpha = 0.90f)) else spec,
                shape
            )
            .pressScale(0.97f, onClick)
            .padding(VennelaTokens.spaceLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(VennelaTokens.badgeMd)
                .clip(RoundedCornerShape(VennelaTokens.radiusBadge))
                .background(
                    Brush.linearGradient(
                        if (active) {
                            listOf(accent.primary, accent.secondary)
                        } else {
                            listOf(accent.primary.copy(alpha = 0.42f), accent.secondary.copy(alpha = 0.30f))
                        }
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.32f),
                    RoundedCornerShape(VennelaTokens.radiusBadge)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(VennelaTokens.iconMd))
        }
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = palette.textSecondary,
                fontSize = VennelaTokens.micro
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Rows and cards                                                             */
/* ------------------------------------------------------------------------- */

/** One frosted surface used by every row so the glass recipe stays identical. */
@Composable
private fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val accent = LocalVennelaAccent.current
    // Selection lights the tile up instead of just swapping a border colour.
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = LiquidMotion.liquid(),
        label = "glassGlow"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) 18.dp else 6.dp,
        animationSpec = LiquidMotion.liquid(),
        label = "glassLift"
    )
    val base = liquidGlassSpec(
        alpha = if (selected) 0.62f else 0.50f,
        frost = VennelaTokens.frostRegular,
        elevation = if (selected) VennelaTokens.elevationBar else VennelaTokens.elevationCard,
        sheen = if (selected) 0.28f else 0.20f,
        highlight = 0.20f
    )
    val spec = base.copy(
        rim = accent.secondary.copy(alpha = 0.42f + 0.48f * glow),
        shadowColor = accent.primary.copy(alpha = 0.16f + 0.34f * glow)
    )
    Box(
        modifier = modifier
            .shadow(
                elevation = lift,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.14f + 0.36f * glow),
                spotColor = accent.primary.copy(alpha = 0.14f + 0.36f * glow)
            )
            .liquidGlass(spec, shape),
        content = content
    )
}

/** Press feedback plus long-press, so rows can enter multi-select. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.glassPress(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = LiquidMotion.press(),
        label = "glassPress"
    )
    val dim by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = LiquidMotion.press(),
        label = "glassPressAlpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = dim
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

@Composable
private fun FolderBadge(visual: FolderVisual, size: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(VennelaTokens.radiusBadge)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = visual.end.copy(alpha = 0.55f),
                spotColor = visual.start.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(visual.start, visual.end)))
            .border(1.dp, Color.White.copy(alpha = 0.44f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            visual.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current
    val fill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = LiquidMotion.liquid(),
        label = "selectionMark"
    )
    val ring by animateColorAsState(
        targetValue = if (selected) accent.secondary else palette.cardBorder,
        animationSpec = LiquidMotion.glide(LiquidMotion.durationFast),
        label = "selectionRing"
    )
    Box(
        modifier = Modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = 0.88f + 0.12f * fill
                scaleY = 0.88f + 0.12f * fill
            }
            .shadow(
                elevation = 12.dp * fill,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.55f * fill),
                spotColor = accent.primary.copy(alpha = 0.55f * fill)
            )
            .clip(CircleShape)
            .background(accent.primary.copy(alpha = 0.16f + 0.62f * fill))
            .border(1.2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (fill > 0.3f) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier
                    .size(VennelaTokens.iconSm)
                    .graphicsLayer { alpha = fill }
            )
        }
    }
}

@Composable
private fun ExplorerFolderRow(
    file: FileItem,
    selected: Boolean,
    selectionActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val visual = folderVisual(file.name)
    GlassSurface(
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        selected = selected,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassPress(onClick, onLongClick)
                .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FolderBadge(visual, VennelaTokens.badgeMd)
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.itemCount > 0) {
                        "Folder - ${file.itemCount} items"
                    } else {
                        "Folder - ${file.formattedDate}"
                    },
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Medium
                )
            }
            if (selectionActive) {
                SelectionMark(selected)
            } else {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Long press for options",
                    tint = palette.textTertiary,
                    modifier = Modifier.size(VennelaTokens.iconMd)
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier.size(VennelaTokens.iconSm)
                )
            }
        }
    }
}

@Composable
private fun ExplorerFileRow(
    file: FileItem,
    selected: Boolean,
    selectionActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    GlassSurface(
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        selected = selected,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassPress(onClick, onLongClick)
                .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileThumb(file = file, size = VennelaTokens.badgeMd)
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                Text(
                    "${file.extension.uppercase()} - ${file.formattedSize} - ${file.formattedDate}",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectionActive) {
                SelectionMark(selected)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier.size(VennelaTokens.iconSm)
                )
            }
        }
    }
}

@Composable
private fun ExplorerFolderCard(
    file: FileItem,
    selected: Boolean,
    selectionActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val visual = folderVisual(file.name)
    GlassSurface(
        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
        selected = selected,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPress(onClick, onLongClick)
                .padding(VennelaTokens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                FolderBadge(visual, VennelaTokens.badgeLg)
                if (selectionActive) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        SelectionMark(selected)
                    }
                }
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Text(
                file.name,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (file.itemCount > 0) "${file.itemCount} items" else "Folder",
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ExplorerFileCard(
    file: FileItem,
    selected: Boolean,
    selectionActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassSurface(
        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
        selected = selected,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPress(onClick, onLongClick)
                .padding(VennelaTokens.spaceMd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (file.uri != null && file.extension.lowercase() in imageThumbExtensions) {
                    AsyncImage(
                        model = file.uri,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                            .background(palette.textPrimary.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.20f),
                                RoundedCornerShape(VennelaTokens.radiusControl)
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accent.secondary.copy(alpha = 0.16f),
                                        accent.primary.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                accent.secondary.copy(alpha = 0.32f),
                                RoundedCornerShape(VennelaTokens.radiusControl)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeMd)
                    }
                }
                if (selectionActive) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        SelectionMark(selected)
                    }
                }
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Text(
                file.name,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                file.formattedSize,
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Real image preview when Coil can decode it, brand badge otherwise. */
@Composable
private fun FileThumb(file: FileItem, size: androidx.compose.ui.unit.Dp) {
    val palette = LocalVennelaPalette.current
    if (file.uri != null && file.extension.lowercase() in imageThumbExtensions) {
        AsyncImage(
            model = file.uri,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                .background(palette.textPrimary.copy(alpha = 0.10f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.18f),
                    RoundedCornerShape(VennelaTokens.radiusControl)
                )
        )
    } else {
        FileBrandBadge(extension = file.extension, size = size)
    }
}

/* ------------------------------------------------------------------------- */
/* Storage helpers                                                            */
/* ------------------------------------------------------------------------- */

private data class ExplorerStorageInfo(val usedBytes: Long, val totalBytes: Long) {
    val fractionUsed: Float
        get() = if (totalBytes <= 0) {
            0f
        } else {
            (usedBytes.toDouble() / totalBytes).toFloat().coerceIn(0f, 1f)
        }
}

private fun explorerStorageStats(): ExplorerStorageInfo = runCatching {
    val stat = StatFs(Environment.getDataDirectory().absolutePath)
    val total = stat.blockCountLong * stat.blockSizeLong
    val available = stat.availableBlocksLong * stat.blockSizeLong
    ExplorerStorageInfo((total - available).coerceAtLeast(0L), total)
}.getOrDefault(ExplorerStorageInfo(0L, 0L))

private fun explorerFormatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

