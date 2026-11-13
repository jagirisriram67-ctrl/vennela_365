package com.vennela365.viewer.ui.screens

import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.AnimatedDisclosure
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaMetadataRow
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import com.vennela365.viewer.viewmodel.SortMode
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

private data class FilterChipData(
    val label: String,
    val icon: ImageVector,
    val category: FormatCategory? = null
)

private val recentsFilters = listOf(
    FilterChipData("All", Icons.Default.GridView),
    FilterChipData("Documents", Icons.Default.Description, FormatCategory.DOCUMENTS),
    FilterChipData("Images", Icons.Default.Image, FormatCategory.IMAGES),
    FilterChipData("Videos", Icons.Default.PlayArrow, FormatCategory.VIDEO),
    FilterChipData("Audio", Icons.Default.MusicNote, FormatCategory.AUDIO),
    FilterChipData("Code", Icons.Default.Code, FormatCategory.CODE)
)

private val recentsSortOptions = listOf(
    SortMode.DATE_NEWEST to "Newest first",
    SortMode.DATE_OLDEST to "Oldest first",
    SortMode.NAME_ASC to "Name (A-Z)",
    SortMode.NAME_DESC to "Name (Z-A)",
    SortMode.SIZE_LARGEST to "Largest first",
    SortMode.SIZE_SMALLEST to "Smallest first",
    SortMode.TYPE to "File type"
)

/** Calendar buckets, computed from the real epoch timestamp of every file. */
private enum class RecentBucket(val title: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This week"),
    EARLIER("Earlier")
}

private fun bucketOf(lastModified: Long): RecentBucket {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(lastModified).atZone(zone).toLocalDate()
    val today = Instant.now().atZone(zone).toLocalDate()
    return when {
        !day.isBefore(today) -> RecentBucket.TODAY
        day == today.minusDays(1) -> RecentBucket.YESTERDAY
        !day.isBefore(today.minusDays(6)) -> RecentBucket.THIS_WEEK
        else -> RecentBucket.EARLIER
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(viewModel: MainViewModel) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var showSheet by remember { mutableStateOf(false) }
    var infoFile by remember { mutableStateOf<FileItem?>(null) }
    var collapsed by remember { mutableStateOf(emptySet<RecentBucket>()) }

    val favoriteNames = remember(favoriteFiles) { favoriteFiles.map { it.name }.toSet() }

    val filteredFiles = remember(selectedFilter, recentFiles, sortMode) {
        val category = recentsFilters.firstOrNull { it.label == selectedFilter }?.category
        val matching = if (category == null) recentFiles else recentFiles.filter { it.formatInfo.category == category }
        viewModel.sortedFiles(matching)
    }

    val buckets = remember(filteredFiles) {
        filteredFiles.groupBy { bucketOf(it.lastModified) }
    }
    val totalBytes = remember(recentFiles) { recentFiles.sumOf { if (it.isDirectory) 0L else it.sizeBytes } }
    val storage = remember { recentsStorageStats() }
    val topCategory = remember(recentFiles) {
        recentFiles
            .filter { !it.isDirectory }
            .groupingBy { it.formatInfo.category.displayName }
            .eachCount()
            .maxByOrNull { it.value }
            ?.toPair()
    }

    fun shareFile(file: FileItem) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "*/*" }
        // Direct-path items carry file:// URIs, which must be exported through
        // the FileProvider before another app can read them.
        if (!ShareUri.attachStream(context, intent, file.uri)) {
            Toast.makeText(context, "This file has no shareable location", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }

    LiquidPage(
        dock = {
            FloatingDockNav(
                activeTab = "files",
                onTabSelected = { tab ->
                    when (tab) {
                        "home" -> viewModel.navigateTo(Screen.Home)
                        "files" -> viewModel.navigateTo(Screen.AllFiles)
                        "search" -> viewModel.navigateTo(Screen.UniversalSearch)
                        "favorites" -> viewModel.navigateTo(Screen.Favorites)
                        "settings" -> viewModel.navigateTo(Screen.Settings)
                    }
                }
            )
        }
    ) { pagePadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = VennelaTokens.spaceXs,
                bottom = pagePadding.calculateBottomPadding()
            )
        ) {
            // ---- Frosted header with the crescent-moon mark ----
            item(key = "header") {
                val headerShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VennelaTokens.gutter)
                        .shadow(
                            elevation = 20.dp,
                            shape = headerShape,
                            clip = false,
                            ambientColor = accent.secondary.copy(alpha = 0.50f),
                            spotColor = accent.primary.copy(alpha = 0.50f)
                        )
                        .liquidGlass(
                            liquidGlassSpec(
                                alpha = 0.58f,
                                frost = VennelaTokens.frostRegular,
                                elevation = VennelaTokens.elevationBar,
                                sheen = 0.28f,
                                highlight = 0.22f
                            ),
                            headerShape
                        )
                        .padding(
                            horizontal = VennelaTokens.spaceMd,
                            vertical = VennelaTokens.spaceMd
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Default.NightlightRound,
                        colors = listOf(accent.primary, accent.secondary),
                        size = VennelaTokens.badgeLg,
                        radius = VennelaTokens.radiusBadge,
                        contentDescription = "Vennela moon"
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Vennela 365",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Recent Files",
                            color = accent.secondary,
                            fontSize = VennelaTokens.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HeaderAction(
                        icon = Icons.Default.Search,
                        contentDescription = "Search files",
                        onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
                    )
                    HeaderAction(
                        icon = Icons.Default.FilterList,
                        contentDescription = "Sort and filter",
                        onClick = { showSheet = true }
                    )
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            }

            // ---- Storage summary ----
            item(key = "summary") {
                RecentsSummaryCard(
                    recentCount = recentFiles.size,
                    shownCount = filteredFiles.size,
                    totalBytes = totalBytes,
                    storage = storage,
                    topCategory = topCategory,
                    modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            }

            // ---- Filter chips: scrolling row, never clipped ----
            item(key = "filters") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = VennelaTokens.gutter),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    items(recentsFilters) { chip ->
                        LiquidChip(
                            text = chip.label,
                            icon = chip.icon,
                            isActive = selectedFilter == chip.label,
                            onClick = { selectedFilter = chip.label }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
            }

            // ---- Empty states ----
            if (recentFiles.isEmpty()) {
                item(key = "empty") {
                    VennelaEmptyState(
                        title = "No recent files yet",
                        hint = "Open a file from your device and it will appear here, grouped by the day you viewed it.",
                        icon = Icons.Default.History,
                        actionLabel = "Browse files",
                        onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                    )
                }
            } else if (filteredFiles.isEmpty()) {
                item(key = "empty-filter") {
                    VennelaEmptyState(
                        title = "No $selectedFilter files",
                        hint = "Nothing in this category yet. Clear the filter to see everything you opened.",
                        icon = Icons.Default.FilterAlt,
                        actionLabel = "Show all",
                        onAction = { selectedFilter = "All" }
                    )
                }
            }

            // ---- Frosted timeline, one section per calendar bucket ----
            RecentBucket.entries.forEach { bucket ->
                val files = buckets[bucket].orEmpty()
                if (files.isEmpty()) return@forEach
                val expanded = bucket !in collapsed

                item(key = "head-${bucket.name}") {
                    TimelineGroupHeader(
                        title = bucket.title,
                        countLabel = "${files.size} file${if (files.size == 1) "" else "s"}",
                        isExpanded = expanded,
                        onToggle = {
                            collapsed = if (expanded) collapsed + bucket else collapsed - bucket
                        },
                        modifier = Modifier.padding(
                            start = VennelaTokens.gutter,
                            end = VennelaTokens.gutter,
                            top = VennelaTokens.spaceMd
                        )
                    )
                }

                item(key = "body-${bucket.name}") {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(animationSpec = LiquidMotion.glide()),
                        exit = shrinkVertically(animationSpec = LiquidMotion.exit())
                    ) {
                        TimelineGroupBody(
                            files = files,
                            favoriteNames = favoriteNames,
                            onOpen = { viewModel.openFile(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onShare = { shareFile(it) },
                            onInfo = { infoFile = it },
                            modifier = Modifier.padding(
                                start = VennelaTokens.gutter,
                                end = VennelaTokens.gutter
                            )
                        )
                    }
                }
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))
            }
        }
    }

    // ---- Working sort / filter glass sheet ----
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VennelaTokens.spaceXxl)
            ) {
                Text(
                    "Sort and filter",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = VennelaTokens.spaceXl)
                )
                Text(
                    "Applies to your recent files on this device.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    modifier = Modifier.padding(horizontal = VennelaTokens.spaceXl)
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))

                Text(
                    "Filter",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = VennelaTokens.spaceXl)
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = VennelaTokens.spaceXl),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    items(recentsFilters) { chip ->
                        LiquidChip(
                            text = chip.label,
                            icon = chip.icon,
                            isActive = selectedFilter == chip.label,
                            onClick = { selectedFilter = chip.label }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))
                Text(
                    "Sort by",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = VennelaTokens.spaceXl)
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = VennelaTokens.spaceXl),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    items(recentsSortOptions) { (mode, label) ->
                        LiquidChip(
                            text = label,
                            icon = if (mode == sortMode) Icons.Default.Check else Icons.AutoMirrored.Filled.Sort,
                            isActive = mode == sortMode,
                            onClick = { viewModel.setSortMode(mode) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))
                Row(
                    modifier = Modifier
                        .padding(horizontal = VennelaTokens.spaceXl)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    SheetPill(
                        label = "Reset",
                        icon = Icons.Default.RestartAlt,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedFilter = "All"
                        viewModel.setSortMode(SortMode.DATE_NEWEST)
                    }
                    SheetPill(
                        label = "Done",
                        icon = Icons.Default.Check,
                        active = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        showSheet = false
                    }
                }
            }
        }
    }

    // ---- Per-file actions (swipe-free quick actions) ----
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
                    FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeLg)
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
                            "${file.formatInfo.name} - ${file.formattedSize}",
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.caption
                        )
                    }
                    FilePreviewCapsule(extension = file.extension)
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    SheetPill(
                        label = "Open",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        active = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        infoFile = null
                        viewModel.openFile(file)
                    }
                    SheetPill(
                        label = "Share",
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f)
                    ) { shareFile(file) }
                    SheetPill(
                        label = if (file.name in favoriteNames) "Unpin" else "Pin",
                        icon = if (file.name in favoriteNames) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleFavorite(file) }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                GlassDivider()
                VennelaMetadataRow("Kind", "$file.extension.uppercase() - ${file.formatInfo.category.displayName}")
                VennelaMetadataRow("Size", file.formattedSize)
                VennelaMetadataRow("Modified", file.formattedDate)
                VennelaMetadataRow("Support", file.formatInfo.supportLevel.label)
                VennelaMetadataRow("Location", file.path.ifBlank { file.uri?.toString() ?: "In-app result" })
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                SheetPill(
                    label = "Copy location",
                    icon = Icons.Default.ContentCopy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    clipboard.setText(
                        AnnotatedString(file.path.ifBlank { file.uri?.toString() ?: file.name })
                    )
                    Toast.makeText(context, "Location copied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Header pieces                                                              */
/* ------------------------------------------------------------------------- */

@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .pressScale(0.90f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(VennelaTokens.badgeMd)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.45f),
                    spotColor = accent.primary.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.34f),
                            accent.secondary.copy(alpha = 0.20f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = palette.textPrimary,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
        }
    }
}

@Composable
private fun RecentsSummaryCard(
    recentCount: Int,
    shownCount: Int,
    totalBytes: Long,
    storage: RecentsStorageInfo,
    topCategory: Pair<String, Int>?,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    // Real device usage, grown from zero so the figure reads as a live gauge.
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(storage.fractionUsed) {
        fraction.animateTo(
            targetValue = storage.fractionUsed.coerceIn(0f, 1f),
            animationSpec = LiquidMotion.emphasis(LiquidMotion.durationHero)
        )
    }
    val sheen = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            sheen.snapTo(0f)
            sheen.animateTo(1f, LiquidMotion.shimmer(LiquidMotion.durationHero * 2))
        }
    }
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.48f),
                spotColor = accent.primary.copy(alpha = 0.48f)
            )
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.58f,
                    frost = VennelaTokens.frostRegular,
                    elevation = VennelaTokens.elevationBar,
                    sheen = 0.28f,
                    highlight = 0.22f
                ),
                shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VennelaTokens.spaceLg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = accent.secondary,
                    modifier = Modifier.size(VennelaTokens.iconSm)
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Text(
                    if (shownCount == recentCount) {
                        "$recentCount recent file${if (recentCount == 1) "" else "s"}"
                    } else {
                        "$shownCount of $recentCount shown"
                    },
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(fraction.value * 100f).roundToInt()}%",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = VennelaTokens.spaceSm)
                )
                Text(
                    recentsFormatBytes(totalBytes),
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.SemiBold
                )
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
                        "${recentsFormatBytes(storage.totalBytes - storage.usedBytes)} free of " +
                            recentsFormatBytes(storage.totalBytes)
                    } else {
                        "Storage information unavailable"
                    },
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.micro
                )
                Text(
                    text = topCategory?.let { "${it.first} x${it.second}" } ?: "Mixed types",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.micro,
                    maxLines = 1
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Timeline                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
private fun TimelineGroupHeader(
    title: String,
    countLabel: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    // The timeline marker breathes so open buckets feel alive.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
            pulse.animateTo(0f, LiquidMotion.exit(LiquidMotion.durationSlow))
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(0.99f, onToggle)
            .padding(vertical = VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.55f),
                    spotColor = accent.primary.copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.34f),
                            accent.secondary.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(
                    1.2.dp,
                    accent.secondary.copy(alpha = 0.55f + 0.35f * pulse.value),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = 0.88f + 0.24f * pulse.value
                        scaleY = 0.88f + 0.24f * pulse.value
                    }
                    .clip(CircleShape)
                    .background(accent.secondary)
            )
        }
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Text(
            title,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            countLabel,
            color = palette.textSecondary,
            fontSize = VennelaTokens.caption,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        AnimatedDisclosure(expanded = isExpanded)
    }
}

/**
 * The frosted rail that runs down the left of each bucket. It is drawn, not
 * laid out, so it costs nothing and lines up exactly with the header dot.
 */
@Composable
private fun TimelineGroupBody(
    files: List<FileItem>,
    favoriteNames: Set<String>,
    onOpen: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onInfo: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val railX = 14.dp.toPx()
                val stroke = 2.5.dp.toPx()
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.82f),
                            accent.secondary.copy(alpha = 0.10f)
                        )
                    ),
                    start = Offset(railX, 0f),
                    end = Offset(railX, size.height),
                    strokeWidth = stroke
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            clip = false,
                            ambientColor = accent.secondary.copy(alpha = 0.34f),
                            spotColor = accent.primary.copy(alpha = 0.34f)
                        )
                        .liquidGlass(
                            liquidGlassSpec(
                                alpha = 0.52f,
                                frost = VennelaTokens.frostRegular,
                                elevation = VennelaTokens.elevationBar,
                                sheen = 0.22f,
                                highlight = 0.20f
                            ),
                            RoundedCornerShape(VennelaTokens.radiusCardLarge)
                        )
                ) {
                    Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                        files.forEachIndexed { index, file ->
                            RecentFileCard(
                                file = file,
                                onClick = { onOpen(file) },
                                isFavorite = file.name in favoriteNames,
                                onToggleFavorite = { onToggleFavorite(file) },
                                onShare = { onShare(file) },
                                onInfo = { onInfo(file) },
                                modifier = Modifier.staggeredEntrance(index)
                            )
                            if (index < files.lastIndex) GlassDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            }
        }
    }
}

/**
 * One recent file on frosted glass with inline quick actions: pin, share and a
 * detail sheet. Nothing here needs a swipe gesture.
 */
@Composable
fun RecentFileCard(
    file: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    showPreview: Boolean = false
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    // Pinned files keep a permanent accent wash behind their row.
    val favoriteWash by animateColorAsState(
        targetValue = if (isFavorite) accent.primary.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = LiquidMotion.liquid(),
        label = "favoriteWash"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(favoriteWash, RoundedCornerShape(VennelaTokens.radiusControl))
            .pressScale(0.975f, onClick)
            .padding(
                horizontal = VennelaTokens.spaceMd,
                vertical = VennelaTokens.spaceMd
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeMd)
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
            Text(
                text = "${file.formatInfo.name} - ${file.formattedSize} - ${file.formattedDate}",
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showPreview) {
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            FilePreviewCapsule(extension = file.extension)
        }
        if (onToggleFavorite != null) {
            QuickAction(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                tint = if (isFavorite) accent.secondary else palette.textTertiary,
                onClick = onToggleFavorite
            )
        }
        if (onShare != null) {
            QuickAction(
                icon = Icons.Default.Share,
                contentDescription = "Share ${file.name}",
                tint = palette.textTertiary,
                onClick = onShare
            )
        }
        if (onInfo != null) {
            QuickAction(
                icon = Icons.Default.MoreVert,
                contentDescription = "Options for ${file.name}",
                tint = palette.textTertiary,
                onClick = onInfo
            )
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

@Composable
private fun QuickAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(0.88f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

@Composable
private fun SheetPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = if (active) 0.68f else 0.40f,
        frost = VennelaTokens.frostLight,
        elevation = if (active) VennelaTokens.elevationBar else VennelaTokens.elevationRaised,
        sheen = if (active) 0.28f else 0.20f,
        highlight = 0.20f
    )
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    val resolved = if (active) {
        spec.copy(
            tint = accent.primary.copy(alpha = 0.84f),
            tintEnd = accent.secondary.copy(alpha = 0.62f),
            rim = Color.White.copy(alpha = 0.42f),
            shadowColor = accent.secondary.copy(alpha = 0.60f)
        )
    } else {
        spec
    }
    Row(
        modifier = modifier
            .then(
                if (active) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = accent.secondary.copy(alpha = 0.55f),
                        spotColor = accent.primary.copy(alpha = 0.55f)
                    )
                } else {
                    Modifier
                }
            )
            .liquidGlass(resolved, shape)
            .pressScale(0.96f, onClick)
            .padding(horizontal = VennelaTokens.spaceLg, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) Color.White else accent.secondary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            label,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * A mini preview capsule showing a contextual preview hint based on file type.
 * Matches the reference where each result has a small preview on the right.
 */
@Composable
fun FilePreviewCapsule(
    extension: String,
    modifier: Modifier = Modifier
) {
    val ext = extension.lowercase()
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current
    val (tint, icon) = when (ext) {
        "pdf" -> accent.primary to Icons.Default.PictureAsPdf
        "jpg", "jpeg", "png", "webp", "gif" -> accent.secondary to Icons.Default.Image
        "mp3", "wav", "flac", "aac", "m4a" -> accent.primary to Icons.Default.GraphicEq
        "mp4", "mkv", "mov", "avi" -> accent.primary to Icons.Default.PlayCircle
        "docx", "doc" -> accent.primary to Icons.Default.Description
        "xlsx", "xls", "csv" -> accent.secondary to Icons.Default.TableChart
        "zip", "rar", "7z" -> accent.secondary to Icons.Default.FolderZip
        "md", "markdown" -> accent.primary to Icons.Default.AutoStories
        "mmd", "mermaid" -> accent.secondary to Icons.Default.AccountTree
        "py", "java", "kt", "js", "ts" -> accent.primary to Icons.Default.Code
        else -> palette.textTertiary to Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val shape = RoundedCornerShape(VennelaTokens.radiusBadge)
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 44.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = tint.copy(alpha = 0.50f),
                spotColor = tint.copy(alpha = 0.50f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.34f), accent.secondary.copy(alpha = 0.16f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(VennelaTokens.iconLg)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Device storage                                                             */
/* ------------------------------------------------------------------------- */

private data class RecentsStorageInfo(val usedBytes: Long, val totalBytes: Long) {
    val fractionUsed: Float
        get() = if (totalBytes <= 0) {
            0f
        } else {
            (usedBytes.toDouble() / totalBytes).toFloat().coerceIn(0f, 1f)
        }
}

private fun recentsStorageStats(): RecentsStorageInfo = runCatching {
    val stat = StatFs(Environment.getDataDirectory().absolutePath)
    val total = stat.blockCountLong * stat.blockSizeLong
    val available = stat.availableBlocksLong * stat.blockSizeLong
    RecentsStorageInfo((total - available).coerceAtLeast(0L), total)
}.getOrDefault(RecentsStorageInfo(0L, 0L))

private fun recentsFormatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

