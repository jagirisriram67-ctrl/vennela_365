package com.vennela365.viewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.storage.AllFilesAccess
import com.vennela365.viewer.storage.StorageShortcut
import com.vennela365.viewer.storage.StorageVolumeInfo
import com.vennela365.viewer.storage.VolumeKind
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import com.vennela365.viewer.viewmodel.SortMode

/**
 * The restriction-less file manager.
 *
 * With all-files access granted it browses every folder on internal storage,
 * SD cards and USB/OTG volumes directly through real filesystem paths. Without
 * it, it still lists whatever the platform exposes and offers a one-tap route
 * to the grant, so the screen is never a dead end.
 */
@Composable
fun DeviceStorageScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val volumes by viewModel.volumes.collectAsStateWithLifecycle()
    val shortcuts by viewModel.storageShortcuts.collectAsStateWithLifecycle()
    val granted by viewModel.allFilesGranted.collectAsStateWithLifecycle()
    val devicePath by viewModel.devicePath.collectAsStateWithLifecycle()
    val trail by viewModel.deviceTrail.collectAsStateWithLifecycle()
    val items by viewModel.deviceItems.collectAsStateWithLifecycle()
    val loading by viewModel.deviceLoading.collectAsStateWithLifecycle()
    val error by viewModel.deviceError.collectAsStateWithLifecycle()
    val showHidden by viewModel.showHiddenFiles.collectAsStateWithLifecycle()
    val selected by viewModel.selectedPaths.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val recents by viewModel.recentFiles.collectAsStateWithLifecycle()
    val deepQuery by viewModel.deepQuery.collectAsStateWithLifecycle()
    val deepResults by viewModel.deepResults.collectAsStateWithLifecycle()
    val deepSearching by viewModel.deepSearching.collectAsStateWithLifecycle()
    val deepScanned by viewModel.deepScanned.collectAsStateWithLifecycle()

    var gridMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var deepMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshStorage() }

    // Re-check the grant when the user comes back from the system settings page.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAllFilesGrant()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val inFolder = devicePath != null
    val sorted = remember(items, query, sortMode) {
        val filtered = if (query.isBlank()) items
        else items.filter { it.name.contains(query, ignoreCase = true) }
        viewModel.sortedFiles(filtered)
    }
    val fileCount = remember(sorted) { sorted.count { !it.isDirectory } }

    val title = when {
        !inFolder -> "All files"
        trail.isNotEmpty() -> trail.last().first.ifBlank { "Storage" }
        else -> "Storage"
    }
    val subtitle = when {
        !inFolder -> if (granted) "Every folder on this device" else "Limited access"
        loading -> "Reading…"
        else -> "${sorted.size - fileCount} folders · $fileCount files"
    }

    LiquidPage(
        title = title,
        subtitle = subtitle,
        onBack = {
            if (selected.isNotEmpty()) viewModel.clearSelection()
            else if (!viewModel.deviceUp()) viewModel.navigateBack()
        },
        dock = {
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
        },
        topBarActions = {
            if (inFolder) {
                IconBadge(
                    icon = Icons.Default.ArrowUpward,
                    colors = listOf(accent.primary, accent.secondary),
                    contentDescription = "Go up one folder",
                    onClick = { viewModel.deviceUp() }
                )
            }
            IconBadge(
                icon = if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                colors = listOf(accent.secondary, accent.primary),
                contentDescription = if (gridMode) "Switch to list view" else "Switch to grid view",
                onClick = { gridMode = !gridMode }
            )
            IconBadge(
                icon = if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                colors = listOf(accent.primary, accent.secondary),
                contentDescription = if (showHidden) "Hide hidden files" else "Show hidden files",
                onClick = { viewModel.toggleShowHiddenFiles() }
            )
        }
    ) { pagePadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Pinned above the list so the field keeps focus and the IME
            // connection while results stream in beneath it. Inside a
            // LazyColumn the item can be recycled as results grow, which
            // silently drops every keystroke after the first.
            if (!inFolder) {
                DeviceSearchBar(
                    query = deepQuery,
                    searching = deepSearching,
                    scanned = deepScanned,
                    resultCount = deepResults.size,
                    modifier = Modifier.padding(
                        start = VennelaTokens.gutter,
                        end = VennelaTokens.gutter,
                        top = pagePadding.calculateTopPadding() + VennelaTokens.spaceSm
                    ),
                    onQueryChange = {
                        deepMode = it.isNotBlank()
                        viewModel.startDeepSearch(it)
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = VennelaTokens.spaceSm,
                    bottom = pagePadding.calculateBottomPadding() +
                        if (selected.isNotEmpty()) 72.dp else 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                if (!granted) {
                    item(key = "grant") {
                        GrantBanner(
                            onGrant = { AllFilesAccess.launchSettings(context) },
                            onRefresh = { viewModel.refreshStorage() }
                        )
                    }
                }

                if (!inFolder) {
                    if (deepMode || deepQuery.isNotBlank()) {
                        when {
                            deepSearching && deepResults.isEmpty() -> item(key = "deepLoading") {
                                LoadingRows()
                            }

                            deepResults.isEmpty() -> item(key = "deepEmpty") {
                                EmptyCard(
                                    title = if (deepSearching) "Searching…" else "No matches",
                                    hint = if (deepSearching) {
                                        "Walking every folder on every volume."
                                    } else {
                                        "Nothing on this device matches \u201C$deepQuery\u201D. Enable hidden files to include dotfiles."
                                    }
                                )
                            }

                            else -> itemsIndexed(deepResults, key = { _, item -> "deep-" + item.id }) { index, file ->
                                Box(modifier = Modifier.staggeredEntrance(index)) {
                                    FileRowCard(
                                        file = file,
                                        isSelected = file.id in selected,
                                        onOpen = {
                                            if (selected.isNotEmpty()) viewModel.toggleSelection(file.id)
                                            else viewModel.openDeviceFile(file)
                                        },
                                        onLongPress = { viewModel.toggleSelection(file.id) }
                                    )
                                }
                            }
                        }

                        if (deepSearching && deepResults.isNotEmpty()) {
                            item(key = "deepProgress") {
                                Text(
                                    text = "Still searching… ${deepResults.size} found",
                                    color = palette.textTertiary,
                                    fontSize = VennelaTokens.micro,
                                    modifier = Modifier.padding(start = VennelaTokens.spaceXs)
                                )
                            }
                        }
                        return@LazyColumn
                    }

                    item(key = "hubTitle") { SectionHeader(title = "This device") }

                    if (volumes.isEmpty()) {
                        item(key = "noVolumes") {
                            EmptyCard(
                                title = "No volumes reported",
                                hint = "The platform returned no storage volumes. Pull to refresh, or grant all files access."
                            )
                        }
                    } else {
                        itemsIndexed(volumes, key = { _, item -> "vol-" + item.id }) { index, volume ->
                            Box(modifier = Modifier.staggeredEntrance(index)) {
                                VolumeCard(volume = volume, onOpen = {
                                    volume.rootPath?.let { viewModel.openDevicePath(it) }
                                })
                            }
                        }
                    }

                    item(key = "quickHead") { SectionHeader(title = "Quick access") }
                    if (shortcuts.isEmpty()) {
                        item(key = "noShortcuts") {
                            EmptyCard(
                                title = "No folders available",
                                hint = "Grant all files access to see Downloads, Documents, Pictures and the rest of your storage."
                            )
                        }
                    } else {
                        itemsIndexed(shortcuts, key = { _, item -> "sc-" + item.id }) { index, shortcut ->
                            Box(modifier = Modifier.staggeredEntrance(index)) {
                                ShortcutRow(shortcut = shortcut, onOpen = {
                                    viewModel.openDevicePath(shortcut.path)
                                })
                            }
                        }
                    }

                    item(key = "recentHead") { SectionHeader(title = "Recently opened") }
                    if (recents.isEmpty()) {
                        item(key = "recentEmpty") {
                            EmptyCard(
                                title = "Nothing opened yet",
                                hint = "Open any file and it appears here for one-tap return."
                            )
                        }
                    } else {
                        itemsIndexed(recents.take(8), key = { _, item -> "rec-" + item.id }) { index, file ->
                            Box(modifier = Modifier.staggeredEntrance(index)) {
                                FileRowCard(file = file, onOpen = { viewModel.openDeviceFile(file) })
                            }
                        }
                    }
                } else {
                    item(key = "trail") {
                        TrailBar(
                            trail = trail,
                            onCrumb = { path -> viewModel.openDevicePath(path) },
                            onRoot = { viewModel.openDevicePath(null) }
                        )
                    }

                    item(key = "tools") {
                        ToolsBar(
                            query = query,
                            onQueryChange = { query = it },
                            sortMode = sortMode,
                            onSortChange = { viewModel.setSortMode(it) }
                        )
                    }

                    when {
                        loading && items.isEmpty() -> item(key = "loading") { LoadingRows() }

                        error != null -> item(key = "error") {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Could not open this folder",
                                    color = palette.textPrimary,
                                    fontSize = VennelaTokens.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                Text(
                                    text = error.orEmpty(),
                                    color = palette.textSecondary,
                                    fontSize = VennelaTokens.bodySmall
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                                LiquidButton(
                                    label = "Try again",
                                    icon = Icons.Default.Refresh,
                                    onClick = { viewModel.openDevicePath(devicePath) }
                                )
                            }
                        }

                        sorted.isEmpty() && query.isNotBlank() -> item(key = "noMatch") {
                            EmptyCard(
                                title = "No matches",
                                hint = "Nothing in this folder matches \u201C$query\u201D."
                            )
                        }

                        sorted.isEmpty() -> item(key = "empty") {
                            EmptyCard(
                                title = "This folder is empty",
                                hint = if (showHidden) "There is genuinely nothing in here."
                                else "Nothing visible. Hidden files are filtered out."
                            )
                        }

                        gridMode -> item(key = "grid") {
                            FileGrid(sorted = sorted, selected = selected, viewModel = viewModel)
                        }

                        else -> itemsIndexed(sorted, key = { _, item -> item.id }) { index, file ->
                            Box(modifier = Modifier.staggeredEntrance(index)) {
                                FileRowCard(
                                    file = file,
                                    isSelected = file.id in selected,
                                    onOpen = {
                                        if (selected.isNotEmpty()) viewModel.toggleSelection(file.id)
                                        else viewModel.openDeviceFile(file)
                                    },
                                    onLongPress = { viewModel.toggleSelection(file.id) }
                                )
                            }
                        }
                    }
                }
            }
            }

            if (selected.isNotEmpty()) {
                SelectionBar(
                    count = selected.size,
                    onClear = { viewModel.clearSelection() },
                    onDelete = { viewModel.deleteSelected() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = VennelaTokens.gutter,
                            end = VennelaTokens.gutter,
                            bottom = pagePadding.calculateBottomPadding()
                        )
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Hub pieces                                                                 */
/* ------------------------------------------------------------------------- */

/**
 * Device-wide search entry. Results stream in while the walk is still running,
 * so the field is useful immediately rather than after a full scan.
 */
@Composable
private fun DeviceSearchBar(
    query: String,
    searching: Boolean,
    scanned: Int,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    // The field breathes accent colour while the device walk is running.
    val scanTint by animateColorAsState(
        targetValue = if (searching) accent.secondary else palette.textTertiary,
        animationSpec = LiquidMotion.glide(LiquidMotion.durationFast),
        label = "scanTint"
    )
    val shape = RoundedCornerShape(VennelaTokens.radiusTile)
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = if (searching) 0.50f else 0.28f),
                spotColor = accent.primary.copy(alpha = if (searching) 0.50f else 0.28f)
            ),
        shape = shape,
        elevation = VennelaTokens.elevationBar,
        alpha = 0.56f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceSm)
                .heightIn(min = VennelaTokens.touchTargetMin),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = scanTint,
                modifier = Modifier
                    .padding(start = VennelaTokens.spaceSm)
                    .size(VennelaTokens.iconMd)
            )
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Search every folder on this device",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.bodySmall
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = accent.primary
                )
            )
            if (query.isNotEmpty()) {
                IconBadge(
                    icon = Icons.Default.Close,
                    colors = listOf(palette.textTertiary, palette.textSecondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusChip,
                    contentDescription = "Clear search",
                    onClick = { onQueryChange("") }
                )
            }
        }

        if (query.isNotEmpty()) {
            Text(
                text = when {
                    searching -> "Searching… $resultCount found so far"
                    resultCount == 0 -> "No matches"
                    else -> "$resultCount matches"
                },
                color = palette.textTertiary,
                fontSize = VennelaTokens.micro,
                modifier = Modifier.padding(
                    start = VennelaTokens.spaceLg,
                    bottom = VennelaTokens.spaceSm
                )
            )
        } else if (scanned > 0) {
            Text(
                text = "Last search scanned $scanned items",
                color = palette.textTertiary,
                fontSize = VennelaTokens.micro,
                modifier = Modifier.padding(
                    start = VennelaTokens.spaceLg,
                    bottom = VennelaTokens.spaceSm
                )
            )
        }
    }
}

@Composable
private fun GrantBanner(onGrant: () -> Unit, onRefresh: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.46f),
                spotColor = accent.primary.copy(alpha = 0.46f)
            ),
        shape = shape,
        elevation = VennelaTokens.elevationDock,
        alpha = 0.58f
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = Icons.Default.LockOpen,
                colors = listOf(accent.primary, accent.secondary),
                size = VennelaTokens.badgeLg,
                radius = VennelaTokens.radiusBadge,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock every folder",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grant all files access to browse internal storage, SD cards and USB drives without limits.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption
                )
            }
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
            LiquidButton(label = "Grant access", icon = Icons.Default.LockOpen, onClick = onGrant)
            LiquidButton(label = "Re-check", icon = Icons.Default.Refresh, onClick = onRefresh)
        }
    }
}

@Composable
private fun VolumeCard(volume: StorageVolumeInfo, onOpen: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val icon = when (volume.kind) {
        VolumeKind.INTERNAL -> Icons.Default.Memory
        VolumeKind.SD_CARD -> Icons.Default.SdCard
        VolumeKind.USB_OTG -> Icons.Default.Usb
        VolumeKind.UNKNOWN -> Icons.Default.Folder
    }
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(volume.usedFraction) {
        fraction.animateTo(
            targetValue = volume.usedFraction.coerceIn(0f, 1f),
            animationSpec = LiquidMotion.emphasis(LiquidMotion.durationHero)
        )
    }
    // Aurora sheen so every volume gauge feels lit rather than printed.
    val sheen = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            sheen.snapTo(0f)
            sheen.animateTo(1f, LiquidMotion.shimmer(LiquidMotion.durationHero * 2))
        }
    }
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.36f),
                spotColor = accent.primary.copy(alpha = 0.36f)
            ),
        shape = shape,
        elevation = VennelaTokens.elevationBar,
        alpha = 0.56f,
        onClick = if (volume.isBrowsable) onOpen else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = icon,
                colors = listOf(accent.primary, accent.secondary),
                size = VennelaTokens.badgeLg,
                radius = VennelaTokens.radiusBadge,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.label,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (volume.isRemovable) "Removable" else "Built-in")
                        if (!volume.isMounted) append(" · not mounted")
                        else if (!volume.isBrowsable) append(" · no direct path")
                    },
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro
                )
            }
            if (volume.totalBytes > 0L) {
                Text(
                    text = "${(fraction.value * 100f).toInt()}% used",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (volume.totalBytes > 0L) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
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
                                listOf(accent.primary, accent.secondary, accent.primary)
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
                                            Color.White.copy(alpha = 0.42f),
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = formatFileBytes(volume.usedBytes) + " used",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatFileBytes(volume.freeBytes) + " free",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: StorageShortcut, onOpen: () -> Unit) {
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.30f),
                spotColor = accent.primary.copy(alpha = 0.30f)
            ),
        shape = shape,
        elevation = VennelaTokens.elevationBar,
        alpha = 0.52f
    ) {
        GlassListRow(
            title = shortcut.label,
            subtitle = shortcut.path,
            leading = {
                IconBadge(
                    icon = if (shortcut.isRemovable) Icons.Default.SdCard else Icons.Default.FolderOpen,
                    colors = listOf(accent.secondary, accent.primary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusChip,
                    contentDescription = null
                )
            },
            showChevron = true,
            onClick = onOpen
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Folder pieces                                                              */
/* ------------------------------------------------------------------------- */

@Composable
private fun TrailBar(
    trail: List<Pair<String, String>>,
    onCrumb: (String) -> Unit,
    onRoot: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)) {
        item(key = "root") {
            LiquidChip(
                text = "Storage",
                icon = Icons.Default.Memory,
                isActive = trail.isEmpty(),
                onClick = onRoot
            )
        }
        items(trail, key = { it.second }) { crumb ->
            LiquidChip(
                text = crumb.first.ifBlank { "Root" },
                isActive = crumb == trail.lastOrNull(),
                onClick = { onCrumb(crumb.second) }
            )
        }
    }
}

@Composable
private fun ToolsBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: SortMode,
    onSortChange: (SortMode) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var sortOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(VennelaTokens.radiusTile),
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.28f),
                    spotColor = accent.primary.copy(alpha = 0.28f)
                ),
            shape = RoundedCornerShape(VennelaTokens.radiusTile),
            elevation = VennelaTokens.elevationBar,
            alpha = 0.52f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceSm)
                    .heightIn(min = VennelaTokens.touchTargetMin),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Filter this folder",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.bodySmall
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        cursorColor = accent.primary
                    )
                )
                if (query.isNotEmpty()) {
                    IconBadge(
                        icon = Icons.Default.Close,
                        colors = listOf(palette.textTertiary, palette.textSecondary),
                        size = VennelaTokens.badgeSm,
                        radius = VennelaTokens.radiusChip,
                        contentDescription = "Clear filter",
                        onClick = { onQueryChange("") }
                    )
                }
                IconBadge(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusChip,
                    contentDescription = "Change sort order",
                    onClick = { sortOpen = !sortOpen }
                )
            }
        }

        AnimatedVisibility(visible = sortOpen) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)) {
                items(SortMode.entries.toList(), key = { it.name }) { mode ->
                    LiquidChip(
                        text = sortLabelFor(mode),
                        isActive = mode == sortMode,
                        onClick = { onSortChange(mode) }
                    )
                }
            }
        }
    }
}

private fun sortLabelFor(mode: SortMode): String = when (mode) {
    SortMode.NAME_ASC -> "Name A\u2013Z"
    SortMode.NAME_DESC -> "Name Z\u2013A"
    SortMode.DATE_NEWEST -> "Newest"
    SortMode.DATE_OLDEST -> "Oldest"
    SortMode.SIZE_LARGEST -> "Largest"
    SortMode.SIZE_SMALLEST -> "Smallest"
    SortMode.TYPE -> "Type"
}

/**
 * One file or folder. A tap opens; a long press enters multi-select, matching
 * the interaction every mainstream file manager uses.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRowCard(
    file: FileItem,
    isSelected: Boolean = false,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCard)
    // Selection washes and lifts the tile instead of hard-swapping colours.
    val wash by animateColorAsState(
        targetValue = if (isSelected) accent.primary.copy(alpha = 0.24f) else Color.Transparent,
        animationSpec = LiquidMotion.liquid(),
        label = "rowWash"
    )
    val glow by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = LiquidMotion.liquid(),
        label = "rowGlow"
    )
    // Press yields the whole tile, ripple-free, matching the rest of the app.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScaleValue by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = LiquidMotion.press(),
        label = "rowPress"
    )
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScaleValue
                scaleY = pressScaleValue
            }
            .shadow(
                elevation = 14.dp * glow,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.50f * glow),
                spotColor = accent.primary.copy(alpha = 0.50f * glow)
            )
            .background(wash, shape),
        shape = shape,
        elevation = VennelaTokens.elevationBar,
        alpha = if (isSelected) 0.64f else 0.50f
    ) {
        Box(
            modifier = Modifier.combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = onLongPress
            )
        ) {
            GlassListRow(
                title = file.name,
                subtitle = if (file.isDirectory) file.formattedSize
                else file.formattedSize + " · " + file.formattedDate,
                leading = {
                    IconBadge(
                        icon = fileTypeIcon(file),
                        colors = listOf(accent.primary, accent.secondary),
                        size = VennelaTokens.badgeMd,
                        radius = VennelaTokens.radiusBadge,
                        contentDescription = null
                    )
                },
                trailing = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = accent.secondary,
                            modifier = Modifier.size(VennelaTokens.iconMd)
                        )
                    }
                } else null,
                showChevron = file.isDirectory && !isSelected,
                onClick = null
            )
        }
    }
}

/** A fixed three-column grid with an explicit height, safe inside a LazyColumn. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGrid(
    sorted: List<FileItem>,
    selected: Set<String>,
    viewModel: MainViewModel
) {
    val cellHeight = 148.dp
    val rows = (sorted.size + 2) / 3
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight * rows.coerceAtLeast(1)),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
        userScrollEnabled = false
    ) {
        itemsIndexed(sorted, key = { _, item -> item.id }) { index, file ->
            val isSelected = file.id in selected
            Box(modifier = Modifier.staggeredEntrance(index)) {
                FileGridCard(
                    file = file,
                    isSelected = isSelected,
                    onClick = {
                        if (selected.isNotEmpty()) viewModel.toggleSelection(file.id)
                        else viewModel.openDeviceFile(file)
                    },
                    onLongClick = { viewModel.toggleSelection(file.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridCard(
    file: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    val wash by animateColorAsState(
        targetValue = if (isSelected) accent.primary.copy(alpha = 0.26f) else Color.Transparent,
        animationSpec = LiquidMotion.liquid(),
        label = "gridWash"
    )
    val glow by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = LiquidMotion.liquid(),
        label = "gridGlow"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScaleValue by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = LiquidMotion.press(),
        label = "gridPress"
    )
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .graphicsLayer {
                scaleX = pressScaleValue
                scaleY = pressScaleValue
            }
            .shadow(
                elevation = 16.dp * glow,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.52f * glow),
                spotColor = accent.primary.copy(alpha = 0.52f * glow)
            )
            .background(wash, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        elevation = VennelaTokens.elevationBar,
        alpha = if (isSelected) 0.64f else 0.50f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VennelaTokens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
        ) {
            IconBadge(
                icon = fileTypeIcon(file),
                colors = if (isSelected) listOf(accent.secondary, accent.primary)
                else listOf(accent.primary, accent.secondary),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
            Text(
                text = file.name,
                color = palette.textPrimary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.formattedSize,
                color = palette.textTertiary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusDock)
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(0)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.58f),
                spotColor = accent.primary.copy(alpha = 0.58f)
            ),
        shape = shape,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationDock,
        alpha = 0.78f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceSm)
                .heightIn(min = VennelaTokens.touchTargetMin),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
        ) {
            Text(
                text = "$count selected",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconBadge(
                icon = Icons.Default.Delete,
                colors = listOf(palette.textSecondary, palette.textTertiary),
                contentDescription = "Delete selected items",
                onClick = onDelete
            )
            IconBadge(
                icon = Icons.Default.Close,
                colors = listOf(accent.primary, accent.secondary),
                contentDescription = "Clear selection",
                onClick = onClear
            )
        }
    }
}

@Composable
private fun EmptyCard(title: String, hint: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.30f),
                spotColor = accent.primary.copy(alpha = 0.30f)
            ),
        shape = shape,
        elevation = VennelaTokens.elevationCard,
        alpha = 0.54f
    ) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
        Text(text = hint, color = palette.textSecondary, fontSize = VennelaTokens.bodySmall)
    }
}

/** Honest skeleton while a folder is being read, so the screen never flashes empty. */
@Composable
private fun LoadingRows() {
    val palette = LocalVennelaPalette.current
    // One shared sweep so all six skeletons shimmer on the same beat.
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, LiquidMotion.shimmer(LiquidMotion.durationHero * 2))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
        repeat(6) { index ->
            val fill by animateFloatAsState(
                targetValue = if (index % 2 == 0) 0.30f else 0.46f,
                animationSpec = tween(durationMillis = 700),
                label = "skeletonFill"
            )
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VennelaTokens.badgeLg)
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        if (w > 0f) {
                            val x = -w + sweep.value * (w * 3f)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.16f),
                                        Color.Transparent
                                    ),
                                    startX = x - w * 0.5f,
                                    endX = x + w * 0.5f
                                )
                            )
                        }
                    },
                shape = RoundedCornerShape(VennelaTokens.radiusCard),
                elevation = VennelaTokens.elevationRaised,
                alpha = fill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = VennelaTokens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(VennelaTokens.badgeSm)
                            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                            .background(palette.cardBorder.copy(alpha = 0.45f))
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                            .background(palette.cardBorder.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Helpers                                                                    */
/* ------------------------------------------------------------------------- */

/** Real format-category icons so a row reads at a glance. */
private fun fileTypeIcon(file: FileItem): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    return when (file.formatInfo.category) {
        FormatCategory.IMAGES -> Icons.Default.Image
        FormatCategory.VIDEO -> Icons.Default.Movie
        FormatCategory.AUDIO -> Icons.Default.AudioFile
        FormatCategory.ARCHIVES -> Icons.Default.Archive
        FormatCategory.SPREADSHEETS -> Icons.Default.TableChart
        FormatCategory.CODE, FormatCategory.CONFIG, FormatCategory.LOGS -> Icons.Default.Code
        FormatCategory.DOCUMENTS, FormatCategory.MARKDOWN,
        FormatCategory.EBOOKS, FormatCategory.PRESENTATIONS -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun formatFileBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    if (gb < 1024) return String.format("%.2f GB", gb)
    return String.format("%.2f TB", gb / 1024.0)
}
