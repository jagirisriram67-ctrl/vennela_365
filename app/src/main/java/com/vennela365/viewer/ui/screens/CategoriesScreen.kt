package com.vennela365.viewer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.registry.FormatInfo
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.registry.SupportLevel
import com.vennela365.viewer.theme.CatAmber
import com.vennela365.viewer.theme.CatBlue
import com.vennela365.viewer.theme.CatCyan
import com.vennela365.viewer.theme.CatEmerald
import com.vennela365.viewer.theme.CatSlate
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidIconButton
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaSearchField
import com.vennela365.viewer.ui.components.getCategoryColor
import com.vennela365.viewer.ui.components.getCategoryIcon
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

/** Gradient pair + live format count for one category card in the grid. */
private data class FormatCategoryTile(
    val category: FormatCategory,
    val colors: List<Color>,
    val formatCount: Int
)

/** Per-category gradient pair, derived from the shared category colour tokens. */
private fun tileColors(category: FormatCategory): List<Color> {
    val start = getCategoryColor(category)
    // The second stop leans toward white so every badge reads luminous, not muddy.
    return listOf(start, lerp(start, Color.White, 0.28f).copy(alpha = 0.88f))
}

private fun countLabel(count: Int): String = if (count == 1) "1 format" else "$count formats"

@Composable
fun CategoriesScreen(viewModel: MainViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var query by remember { mutableStateOf("") }

    val activeCategory = selectedCategory

    val tiles = remember {
        FormatCategory.entries.map { category ->
            FormatCategoryTile(
                category = category,
                colors = tileColors(category),
                formatCount = FormatRegistry.getByCategory(category).size
            )
        }
    }

    val cleanedQuery = query.trim()
    val searchActive = cleanedQuery.isNotEmpty() && activeCategory == null

    val matchingFormats = remember(cleanedQuery, searchActive) {
        if (!searchActive) emptyList() else FormatRegistry.search(cleanedQuery).take(64)
    }
    val visibleTiles = remember(tiles, cleanedQuery, searchActive) {
        if (!searchActive) tiles
        else tiles.filter { it.category.displayName.contains(cleanedQuery, ignoreCase = true) }
    }
    val selectedFormats = remember(activeCategory) {
        activeCategory?.let { FormatRegistry.getByCategory(it) } ?: emptyList()
    }

    LiquidPage(
        title = activeCategory?.displayName ?: "File Types",
        subtitle = if (activeCategory != null) {
            "${countLabel(selectedFormats.size)} · ${FormatRegistry.totalCount} in the registry"
        } else {
            "Explore supported formats (${FormatRegistry.totalCount} registered)"
        },
        onBack = { viewModel.navigateBack() },
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
        },
        topBarActions = {
            LiquidIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search files",
                onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
            )
        }
    ) { pagePadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceXs,
                bottom = pagePadding.calculateBottomPadding()
            ),
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                VennelaSearchField(
                    value = query,
                    onValueChange = { next ->
                        query = next
                        // A search always looks across every category.
                        if (next.isNotBlank() && activeCategory != null) viewModel.setCategory(null)
                    },
                    placeholder = "Search ${FormatRegistry.totalCount} formats…"
                )
            }

            if (activeCategory != null) {
                item(key = "back-chip", span = { GridItemSpan(maxLineSpan) }) {
                    LiquidChip(
                        text = "All categories",
                        icon = Icons.Default.Category,
                        isActive = true,
                        onClick = { viewModel.setCategory(null) }
                    )
                }

                item(key = "detail-header", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryDetailHeader(
                        category = activeCategory,
                        formats = selectedFormats
                    )
                }

                if (selectedFormats.isEmpty()) {
                    item(key = "detail-empty", span = { GridItemSpan(maxLineSpan) }) {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
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
                            VennelaEmptyState(
                                title = "No formats here yet",
                                hint = "This category is empty in the registry — nothing was invented to fill it.",
                                icon = Icons.Default.Category,
                                actionLabel = "Back to all categories",
                                onAction = { viewModel.setCategory(null) }
                            )
                        }
                    }
                } else {
                    formatRows(
                        formats = selectedFormats,
                        keyPrefix = "detail",
                        onFormatClick = { format ->
                            viewModel.setSearchQuery(format.extension)
                            viewModel.navigateTo(Screen.UniversalSearch)
                        }
                    )
                }
            } else {
                item(key = "grid-header", span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (searchActive) {
                                "${visibleTiles.size} of ${tiles.size} categories"
                            } else {
                                "${tiles.size} categories"
                            },
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (searchActive) {
                            Text(
                                text = "${matchingFormats.size} formats match “$cleanedQuery”",
                                color = accent.secondary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(items = visibleTiles, key = { it.category.name }) { tile ->
                    CategoryTile(
                        tile = tile,
                        index = visibleTiles.indexOf(tile),
                        onClick = { viewModel.setCategory(tile.category) }
                    )
                }

                if (searchActive && matchingFormats.isNotEmpty()) {
                    item(key = "match-header", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Formats matching “$cleanedQuery”",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = VennelaTokens.spaceSm,
                                bottom = VennelaTokens.spaceXxs
                            )
                        )
                    }
                    formatRows(
                        formats = matchingFormats,
                        keyPrefix = "match",
                        onFormatClick = { format ->
                            viewModel.setSearchQuery(format.extension)
                            viewModel.navigateTo(Screen.UniversalSearch)
                        }
                    )
                }

                if (visibleTiles.isEmpty() && matchingFormats.isEmpty()) {
                    item(key = "search-empty", span = { GridItemSpan(maxLineSpan) }) {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
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
                            VennelaEmptyState(
                                title = "No format matches “$cleanedQuery”",
                                hint = "Try a shorter query such as “doc”, “png” or “json” — the registry covers ${FormatRegistry.totalCount} patterns.",
                                icon = Icons.Default.Search,
                                actionLabel = "Clear search",
                                onAction = { query = "" }
                            )
                        }
                    }
                }
            }

            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (activeCategory != null) {
                        "Tap a format to find files of that type · counts are live from the registry."
                    } else {
                        "Counts are live from the format registry · ${FormatRegistry.totalCount} patterns recognised."
                    },
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro,
                    modifier = Modifier.padding(top = VennelaTokens.spaceXs)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Lazy grid helper                                                           */
/* ------------------------------------------------------------------------- */

/**
 * Emits one lazily composed full-width frosted row per format, so a category
 * with hundreds of formats never inflates the composed node count.
 */
private fun LazyGridScope.formatRows(
    formats: List<FormatInfo>,
    keyPrefix: String,
    onFormatClick: (FormatInfo) -> Unit
) {
    formats.forEachIndexed { index, format ->
        item(
            key = "$keyPrefix-${format.canonicalId}-$index",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            FormatCatalogRow(
                format = format,
                index = index,
                onClick = { onFormatClick(format) }
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Category detail header                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun CategoryDetailHeader(category: FormatCategory, formats: List<FormatInfo>) {
    val palette = LocalVennelaPalette.current
    val sample = remember(formats) {
        formats.take(4).joinToString(" · ") { ".${it.extension}" }
    }
    val accent = LocalVennelaAccent.current
    val tint = tileColors(category).first()
    val heroShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    // The hero panel scales and fades in whenever a new category opens.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(category) {
        entrance.snapTo(0f)
        entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
    }
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val t = entrance.value
                translationY = (1f - t) * 16f
                scaleX = 0.95f + 0.05f * t
                scaleY = 0.95f + 0.05f * t
                alpha = t
            }
            .shadow(
                elevation = 20.dp,
                shape = heroShape,
                clip = false,
                ambientColor = tint.copy(alpha = 0.60f),
                spotColor = accent.secondary.copy(alpha = 0.55f)
            ),
        shape = heroShape,
        alpha = 0.52f,
        elevation = VennelaTokens.elevationBar
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            tint.copy(alpha = 0.26f),
                            accent.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), heroShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(VennelaTokens.spaceLg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = getCategoryIcon(category),
                    colors = tileColors(category),
                    size = VennelaTokens.badgeLg,
                    radius = VennelaTokens.radiusTile,
                    contentDescription = category.displayName
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = countLabel(formats.size) + " registered",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sample.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                        Text(
                            text = "$sample${if (formats.size > 4) " …" else ""}",
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Category tile                                                              */
/* ------------------------------------------------------------------------- */

@Composable
private fun CategoryTile(
    tile: FormatCategoryTile,
    index: Int = 0,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val tileShape = RoundedCornerShape(VennelaTokens.radiusCard)
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = tileShape,
                clip = false,
                ambientColor = tile.colors.first().copy(alpha = 0.60f),
                spotColor = accent.secondary.copy(alpha = 0.45f)
            )
            .staggeredEntrance(index)
            .pressScale(pressedScale = 0.955f, onClick = onClick),
        shape = tileShape,
        alpha = 0.50f,
        elevation = VennelaTokens.elevationCard
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            tile.colors.first().copy(alpha = 0.24f),
                            Color.Transparent,
                            accent.secondary.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(1.dp, tile.colors.first().copy(alpha = 0.34f), tileShape)
        ) {
            val roomForChevron = maxWidth >= 172.dp
            val icon = getCategoryIcon(tile.category)

            // Faint watermark of the same glyph, like the reference tiles.
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tile.colors.first().copy(alpha = 0.24f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(78.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = icon,
                    colors = tile.colors,
                    size = VennelaTokens.badgeLg,
                    radius = VennelaTokens.radiusBadge,
                    contentDescription = tile.category.displayName
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tile.category.displayName,
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countLabel(tile.formatCount),
                        color = tile.colors.first().copy(alpha = 0.95f),
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                if (roomForChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accent.secondary.copy(alpha = 0.85f),
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Format row                                                                 */
/* ------------------------------------------------------------------------- */

@Composable
private fun FormatCatalogRow(
    format: FormatInfo,
    index: Int,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val rowShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = rowShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.45f),
                spotColor = accent.primary.copy(alpha = 0.45f)
            )
            .staggeredEntrance(index % 8)
            .pressScale(pressedScale = 0.975f, onClick = onClick),
        shape = rowShape,
        alpha = 0.46f,
        elevation = VennelaTokens.elevationCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, accent.secondary.copy(alpha = 0.26f), rowShape)
                .padding(
                    start = VennelaTokens.spaceLg,
                    end = VennelaTokens.spaceLg,
                    top = VennelaTokens.spaceSm,
                    bottom = VennelaTokens.spaceSm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileBrandBadge(extension = format.extension, size = VennelaTokens.badgeMd)
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.name,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                Text(
                    text = ".${format.extension} • ${format.defaultRenderer} • ${format.supportLevel.label}",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.micro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            SupportLevelPill(level = format.supportLevel)
        }
    }
}

@Composable
private fun SupportLevelPill(level: SupportLevel) {
    val tint = when (level.level) {
        4 -> CatEmerald
        3 -> CatCyan
        2 -> CatBlue
        1 -> CatAmber
        else -> CatSlate
    }
    val pillShape = RoundedCornerShape(VennelaTokens.radiusBadge)
    Box(
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = pillShape,
                clip = false,
                ambientColor = tint.copy(alpha = 0.65f),
                spotColor = tint.copy(alpha = 0.65f)
            )
            .clip(pillShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = 0.32f),
                        tint.copy(alpha = 0.14f)
                    )
                )
            )
            .border(1.dp, tint.copy(alpha = 0.70f), pillShape)
            .padding(horizontal = VennelaTokens.spaceSm + VennelaTokens.spaceXxs, vertical = VennelaTokens.spaceXs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "L${level.level}",
            color = tint,
            fontSize = VennelaTokens.micro,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
