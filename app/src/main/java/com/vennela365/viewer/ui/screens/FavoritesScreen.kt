package com.vennela365.viewer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.theme.CatAmber
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.getCategoryIcon
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import com.vennela365.viewer.viewmodel.SortMode

/**
 * One filter chip in the favorites filter rail.
 *
 * [category] is null for the "All" chip. Counts are derived live from the
 * user's real favorites, so a chip can never promise an empty result.
 */
private data class FavoriteFilter(
    val label: String,
    val category: FormatCategory?,
    val icon: ImageVector?,
    val count: Int
)

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.NAME_ASC -> "Name A–Z"
    SortMode.NAME_DESC -> "Name Z–A"
    SortMode.DATE_NEWEST -> "Newest first"
    SortMode.DATE_OLDEST -> "Oldest first"
    SortMode.SIZE_LARGEST -> "Largest first"
    SortMode.SIZE_SMALLEST -> "Smallest first"
    SortMode.TYPE -> "File type"
}

@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var selectedFilter by remember { mutableStateOf<FormatCategory?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val sortedFiles = remember(favoriteFiles, sortMode) { viewModel.sortedFiles(favoriteFiles) }

    // Chips are built from the categories actually present in the library, so
    // every chip is a promise the screen can keep.
    val filters = remember(sortedFiles) {
        buildList {
            add(FavoriteFilter("All", null, null, sortedFiles.size))
            FormatCategory.entries.forEach { category ->
                val count = sortedFiles.count { it.formatInfo.category == category }
                if (count > 0) {
                    add(FavoriteFilter(category.displayName, category, getCategoryIcon(category), count))
                }
            }
        }
    }

    LaunchedEffect(filters) {
        if (selectedFilter != null && filters.none { it.category == selectedFilter }) {
            selectedFilter = null
        }
    }

    val visibleFiles = remember(sortedFiles, selectedFilter) {
        if (selectedFilter == null) sortedFiles
        else sortedFiles.filter { it.formatInfo.category == selectedFilter }
    }

    LiquidPage(
        dock = {
            FloatingDockNav(
                activeTab = "favorites",
                onTabSelected = { tab ->
                    when (tab) {
                        "home" -> viewModel.navigateTo(Screen.Home)
                        "files" -> viewModel.navigateTo(Screen.AllFiles)
                        "search" -> viewModel.navigateTo(Screen.UniversalSearch)
                        "favorites" -> Unit
                        "settings" -> viewModel.navigateTo(Screen.Settings)
                    }
                }
            )
        }
    ) { pagePadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            item(key = "identity") { FavoritesIdentityRow() }

            item(key = "hero") {
                FavoritesHeroHeader(
                    count = favoriteFiles.size,
                    sortMode = sortMode,
                    sortMenuOpen = sortMenuOpen,
                    onSortMenuOpenChange = { sortMenuOpen = it },
                    onSearch = { viewModel.navigateTo(Screen.UniversalSearch) },
                    onSortSelected = { viewModel.setSortMode(it) }
                )
            }

            if (favoriteFiles.isNotEmpty()) {
                item(key = "filters") {
                    // LazyRow + gutter contentPadding: the rail scrolls instead
                    // of clipping the last chips off screen.
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = VennelaTokens.gutter),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                    ) {
                        items(items = filters, key = { it.label }) { filter ->
                            LiquidChip(
                                text = "${filter.label} · ${filter.count}",
                                icon = filter.icon,
                                isActive = selectedFilter == filter.category,
                                modifier = Modifier.staggeredEntrance(filters.indexOf(filter)),
                                onClick = { selectedFilter = filter.category }
                            )
                        }
                    }
                }
            }

            if (visibleFiles.isEmpty()) {
                item(key = "empty") {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VennelaTokens.gutter)
                            .shadow(
                                elevation = 14.dp,
                                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                clip = false,
                                ambientColor = accent.secondary.copy(alpha = 0.45f),
                                spotColor = accent.primary.copy(alpha = 0.45f)
                            ),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.46f
                    ) {
                        if (favoriteFiles.isEmpty()) {
                            VennelaEmptyState(
                                title = "No favorites yet",
                                hint = "Tap the star on any file and it waits for you here — fully offline, always one tap away.",
                                icon = Icons.Default.FavoriteBorder,
                                actionLabel = "Browse storage",
                                onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                            )
                        } else {
                            VennelaEmptyState(
                                title = "Nothing in ${selectedFilter?.displayName ?: "this filter"}",
                                hint = "Your other favorites are still saved. Clear the filter to see all ${favoriteFiles.size} of them.",
                                icon = Icons.Default.FilterList,
                                actionLabel = "Show all favorites",
                                onAction = { selectedFilter = null }
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(items = visibleFiles, key = { _, file -> file.id }) { index, file ->
                    FavoriteFileCard(
                        file = file,
                        index = index,
                        onClick = { viewModel.openFile(file) },
                        onToggleStar = { viewModel.toggleFavorite(file) },
                        onMoreClick = { viewModel.openFile(file) }
                    )
                }
            }

            if (visibleFiles.isNotEmpty()) {
                item(key = "footer") {
                    Text(
                        text = "Tap the star to remove a file · ${favoriteFiles.size} saved on this device",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro,
                        modifier = Modifier.padding(
                            start = VennelaTokens.gutter,
                            end = VennelaTokens.gutter,
                            top = VennelaTokens.spaceXxs
                        )
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Header                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun FavoritesIdentityRow() {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = VennelaTokens.spaceXs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.70f),
                    spotColor = CatAmber.copy(alpha = 0.60f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.44f),
                            palette.backgroundBottom.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = "Vennela moon",
                tint = CatAmber,
                modifier = Modifier.size(VennelaTokens.iconLg)
            )
        }
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vennela 365",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Universal File Viewer",
                color = accent.secondary.copy(alpha = 0.90f),
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FavoritesHeroHeader(
    count: Int,
    sortMode: SortMode,
    sortMenuOpen: Boolean,
    onSortMenuOpenChange: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onSortSelected: (SortMode) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val heroShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    // The hero panel glides and lifts in once per screen entry.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter)
            .graphicsLayer {
                val t = entrance.value
                translationY = (1f - t) * 14f
                alpha = t
            }
            .shadow(
                elevation = 18.dp,
                shape = heroShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.55f),
                spotColor = accent.primary.copy(alpha = 0.55f)
            )
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.50f,
                    frost = VennelaTokens.frostRegular,
                    elevation = VennelaTokens.elevationBar,
                    sheen = 0.26f,
                    highlight = 0.20f
                ),
                heroShape
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.26f),
                        Color.Transparent,
                        accent.secondary.copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), heroShape)
            .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Favorites",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleHero,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (count == 0) "Nothing saved yet" else "$count ${if (count == 1) "file" else "files"} · ${sortLabel(sortMode)}",
                color = palette.textSecondary,
                fontSize = VennelaTokens.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FavoritesActionButton(
            icon = Icons.Default.Search,
            description = "Search files",
            onClick = onSearch
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Box {
            FavoritesActionButton(
                icon = Icons.AutoMirrored.Filled.Sort,
                description = "Sort favorites",
                onClick = { onSortMenuOpenChange(true) }
            )
            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { onSortMenuOpenChange(false) }
            ) {
                SortMode.entries.forEach { mode ->
                    val selected = mode == sortMode
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sortLabel(mode),
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        trailingIcon = {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accent.secondary,
                                    modifier = Modifier.size(VennelaTokens.iconSm)
                                )
                            }
                        },
                        onClick = {
                            onSortSelected(mode)
                            onSortMenuOpenChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.55f),
                spotColor = accent.primary.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.42f),
                        accent.secondary.copy(alpha = 0.26f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = palette.textPrimary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Card                                                                       */
/* ------------------------------------------------------------------------- */

@Composable
fun FavoriteFileCard(
    file: FileItem,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onMoreClick: () -> Unit,
    index: Int = 0
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val cardShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter)
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.48f),
                spotColor = accent.primary.copy(alpha = 0.48f)
            )
            .staggeredEntrance(index)
            .pressScale(pressedScale = 0.975f, onClick = onClick),
        shape = cardShape,
        alpha = 0.50f,
        elevation = VennelaTokens.elevationCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.16f),
                            Color.Transparent,
                            accent.secondary.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.dp, accent.secondary.copy(alpha = 0.30f), cardShape)
                .padding(
                    start = VennelaTokens.spaceLg,
                    end = VennelaTokens.spaceSm,
                    top = VennelaTokens.spaceMd,
                    bottom = VennelaTokens.spaceMd
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
                    text = "${file.extension.uppercase()} • ${file.formattedSize} • ${file.formattedDate}",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleStar) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.badgeSm)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = CatAmber.copy(alpha = 0.75f),
                            spotColor = CatAmber.copy(alpha = 0.75f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    CatAmber.copy(alpha = 0.90f),
                                    CatAmber.copy(alpha = 0.55f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Remove ${file.name} from favorites",
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
            }
            IconButton(onClick = onMoreClick) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.badgeSm)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = accent.secondary.copy(alpha = 0.70f),
                            spotColor = accent.primary.copy(alpha = 0.70f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.primary.copy(alpha = 0.80f),
                                    accent.secondary.copy(alpha = 0.60f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Open ${file.name}",
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
            }
        }
    }
}
