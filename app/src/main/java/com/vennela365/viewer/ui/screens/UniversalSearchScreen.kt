package com.vennela365.viewer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaMetadataRow
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

private data class SearchFilterChip(
    val label: String,
    val icon: ImageVector,
    val category: FormatCategory? = null
)

private val searchFilters = listOf(
    SearchFilterChip("All", Icons.Default.GridView),
    SearchFilterChip("Documents", Icons.Default.Description, FormatCategory.DOCUMENTS),
    SearchFilterChip("Images", Icons.Default.Image, FormatCategory.IMAGES),
    SearchFilterChip("Videos", Icons.Default.PlayArrow, FormatCategory.VIDEO),
    SearchFilterChip("Audio", Icons.Default.MusicNote, FormatCategory.AUDIO),
    SearchFilterChip("Code", Icons.Default.Code, FormatCategory.CODE),
    SearchFilterChip("Archives", Icons.Default.FolderZip, FormatCategory.ARCHIVES)
)

/** Stable ordering so live results read the same way every keystroke. */
private val searchCategoryOrder = listOf(
    FormatCategory.DOCUMENTS,
    FormatCategory.SPREADSHEETS,
    FormatCategory.PRESENTATIONS,
    FormatCategory.IMAGES,
    FormatCategory.VIDEO,
    FormatCategory.AUDIO,
    FormatCategory.CODE,
    FormatCategory.MARKDOWN,
    FormatCategory.ARCHIVES,
    FormatCategory.EBOOKS,
    FormatCategory.DATABASES,
    FormatCategory.FONTS,
    FormatCategory.CONFIG,
    FormatCategory.LOGS,
    FormatCategory.CAD_3D,
    FormatCategory.EMAIL_CONTACTS,
    FormatCategory.BINARIES,
    FormatCategory.OTHERS
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(viewModel: MainViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val recent by viewModel.recentFiles.collectAsState()
    val browse by viewModel.browseItems.collectAsState()
    val favorites by viewModel.favoriteFiles.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val focusManager = LocalFocusManager.current

    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var recentSearches by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var infoFile by remember { mutableStateOf<FileItem?>(null) }

    // Search what is actually reachable: the open folder, favourites, then recents.
    val allFiles = remember(browse, favorites, recent) {
        (browse + favorites + recent)
            .distinctBy { it.uri?.toString() ?: "${it.name}|${it.extension}" }
    }

    val trimmedQuery = query.trim()

    val filteredFiles = remember(trimmedQuery, selectedFilter, allFiles) {
        val category = searchFilters.firstOrNull { it.label == selectedFilter }?.category
        allFiles
            .filter { file ->
                val matchesQuery = trimmedQuery.isEmpty() ||
                    file.name.contains(trimmedQuery, ignoreCase = true) ||
                    file.extension.contains(trimmedQuery, ignoreCase = true)
                val matchesCategory = category == null || file.formatInfo.category == category
                matchesQuery && matchesCategory
            }
            .sortedWith(
                compareByDescending<FileItem> {
                    trimmedQuery.isNotEmpty() && it.name.startsWith(trimmedQuery, ignoreCase = true)
                }.thenBy { it.name.lowercase() }
            )
    }

    // Also match the format catalogue, so "heic" or "parquet" explains itself.
    val matchingFormats = remember(trimmedQuery) {
        if (trimmedQuery.length < 2) emptyList() else FormatRegistry.search(trimmedQuery).take(6)
    }

    val groupedResults = remember(filteredFiles) {
        val byCategory = filteredFiles.groupBy { it.formatInfo.category }
        searchCategoryOrder.mapNotNull { category ->
            byCategory[category]?.takeIf { it.isNotEmpty() }?.let { category to it }
        }
    }

    fun rememberQuery(value: String) {
        val text = value.trim()
        if (text.length < 2) return
        recentSearches = (listOf(text) + recentSearches.filterNot { it.equals(text, true) }).take(6)
    }

    fun submitQuery() {
        rememberQuery(query)
        focusManager.clearFocus()
    }

    fun openResult(file: FileItem) {
        rememberQuery(query)
        viewModel.openFile(file)
    }

    LiquidPage(
        dock = {
            FloatingDockNav(
                activeTab = "search",
                onTabSelected = { tab ->
                    when (tab) {
                        "home" -> viewModel.navigateTo(Screen.Home)
                        "files" -> viewModel.navigateTo(Screen.AllFiles)
                        "search" -> Unit
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
            // ---- Large frosted header ----
            item(key = "header") {
                val heroShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
                // The hero panel glides in once per screen entry.
                val entrance = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = VennelaTokens.gutter)
                        .graphicsLayer {
                            val t = entrance.value
                            translationY = (1f - t) * 14f
                            alpha = t
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(VennelaTokens.badgeMd)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = accent.secondary.copy(alpha = 0.65f),
                                spotColor = accent.primary.copy(alpha = 0.65f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accent.primary.copy(alpha = 0.88f),
                                        accent.secondary.copy(alpha = 0.66f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                            .pressScale(0.90f) { viewModel.navigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(VennelaTokens.iconMd)
                        )
                    }
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            .padding(
                                horizontal = VennelaTokens.spaceLg,
                                vertical = VennelaTokens.spaceMd
                            )
                    ) {
                        Column {
                            Text(
                                text = "Search",
                                color = palette.textPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Find anything in your files",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))
                }
            }

            // ---- Focus-animated search field ----
            item(key = "field") {
                LargeSearchField(
                    query = query,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onSubmit = { submitQuery() },
                    modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            }

            // ---- Recent searches (only while the field is idle) ----
            if (trimmedQuery.isEmpty() && recentSearches.isNotEmpty()) {
                item(key = "recent-header") {
                    SectionHeader(
                        title = "Recent searches",
                        modifier = Modifier.padding(horizontal = VennelaTokens.gutter),
                        actionLabel = "Clear",
                        onAction = { recentSearches = emptyList() }
                    )
                }
                item(key = "recent-chips") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = VennelaTokens.gutter),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                    ) {
                        items(recentSearches, key = { it }) { term ->
                            LiquidChip(
                                text = term,
                                icon = Icons.Default.History,
                                modifier = Modifier.staggeredEntrance(recentSearches.indexOf(term)),
                                onClick = { viewModel.setSearchQuery(term) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                }
            }

            // ---- Filter chips: a real scrolling row, never clipped ----
            item(key = "filters") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = VennelaTokens.gutter),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    items(searchFilters) { chip ->
                        LiquidChip(
                            text = chip.label,
                            icon = chip.icon,
                            isActive = selectedFilter == chip.label,
                            modifier = Modifier.staggeredEntrance(searchFilters.indexOf(chip)),
                            onClick = { selectedFilter = chip.label }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            }

            // ---- Result summary ----
            if (trimmedQuery.isNotEmpty() &&
                (filteredFiles.isNotEmpty() || matchingFormats.isNotEmpty())
            ) {
                item(key = "summary") {
                    Text(
                        text = buildString {
                            append(filteredFiles.size)
                            append(if (filteredFiles.size == 1) " file" else " files")
                            if (matchingFormats.isNotEmpty()) {
                                append(" - ")
                                append(matchingFormats.size)
                                append(" format")
                                if (matchingFormats.size != 1) append("s")
                            }
                            append(" match \"")
                            append(trimmedQuery)
                            append("\"")
                        },
                        color = accent.secondary,
                        fontSize = VennelaTokens.caption,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = VennelaTokens.gutter,
                            vertical = VennelaTokens.spaceSm
                        )
                    )
                }
            }

            // ---- Format catalogue matches (real registry) ----
            if (matchingFormats.isNotEmpty()) {
                item(key = "formats-header") {
                    SectionHeader(
                        title = "Formats",
                        modifier = Modifier.padding(horizontal = VennelaTokens.gutter),
                        trailing = {
                            Text(
                                "${matchingFormats.size}",
                                color = accent.secondary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
                items(matchingFormats, key = { "fmt-${it.extension}" }) { format ->
                    FormatRow(
                        extension = format.extension,
                        title = ".${format.extension} - ${format.name}",
                        subtitle = "${format.category.displayName} - ${format.supportLevel.label}",
                        modifier = Modifier.padding(
                            horizontal = VennelaTokens.gutter,
                            vertical = VennelaTokens.spaceXs
                        )
                    ) {
                        rememberQuery(format.extension)
                        viewModel.setCategory(format.category)
                        viewModel.navigateTo(Screen.Categories)
                    }
                }
            }

            // ---- Grouped live results, matching text highlighted ----
            groupedResults.forEach { (category, files) ->
                item(key = "group-${category.name}") {
                    SectionHeader(
                        title = category.displayName,
                        modifier = Modifier.padding(horizontal = VennelaTokens.gutter),
                        trailing = {
                            Text(
                                "${files.size}",
                                color = accent.secondary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
                items(files, key = { "file-${it.id}" }) { file ->
                    SearchResultCard(
                        file = file,
                        query = trimmedQuery,
                        modifier = Modifier
                            .padding(
                                horizontal = VennelaTokens.gutter,
                                vertical = VennelaTokens.spaceXs
                            )
                            .staggeredEntrance(files.indexOf(file)),
                        onClick = { openResult(file) },
                        onOptionsClick = { infoFile = file }
                    )
                }
            }

            // ---- Empty / no-results states ----
            if (trimmedQuery.isNotEmpty() && filteredFiles.isEmpty() && matchingFormats.isEmpty()) {
                item(key = "no-results") {
                    VennelaEmptyState(
                        title = "No results for \"$trimmedQuery\"",
                        hint = "Try a shorter word, another filter, or search a different folder.",
                        icon = Icons.Default.SearchOff,
                        actionLabel = "Clear search",
                        onAction = { viewModel.setSearchQuery("") }
                    )
                }
            }

            if (trimmedQuery.isEmpty() && allFiles.isEmpty()) {
                item(key = "empty-library") {
                    VennelaEmptyState(
                        title = "Nothing to search yet",
                        hint = "Grant folder access or open a file, then search across names, types and sizes.",
                        icon = Icons.Default.FolderOpen,
                        actionLabel = "Go to files",
                        onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                    )
                }
            }

            if (trimmedQuery.isEmpty() && allFiles.isNotEmpty() && filteredFiles.isEmpty()) {
                item(key = "empty-filter") {
                    VennelaEmptyState(
                        title = "No $selectedFilter files",
                        hint = "Nothing in this category so far. Pick another filter to keep searching.",
                        icon = Icons.Default.FilterAlt,
                        actionLabel = "Show all",
                        onAction = { selectedFilter = "All" }
                    )
                }
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXl))
            }
        }
    }

    infoFile?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { infoFile = null },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary,
            shape = RoundedCornerShape(
                topStart = VennelaTokens.radiusSheet,
                topEnd = VennelaTokens.radiusSheet
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceXl)
                    .padding(bottom = VennelaTokens.spaceXxl)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeMd)
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            file.formatInfo.name,
                            color = accent.secondary,
                            fontSize = VennelaTokens.caption,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                GlassDivider()
                VennelaMetadataRow("Type", "$file.extension.uppercase() - ${file.formatInfo.category.displayName}")
                VennelaMetadataRow("Size", file.formattedSize)
                VennelaMetadataRow("Modified", file.formattedDate)
                VennelaMetadataRow("Support", file.formatInfo.supportLevel.label)
                VennelaMetadataRow("Location", file.path.ifBlank { file.uri?.toString() ?: "In-app result" })
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(VennelaTokens.radiusPill),
                            clip = false,
                            ambientColor = accent.secondary.copy(alpha = 0.70f),
                            spotColor = accent.primary.copy(alpha = 0.70f)
                        )
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.primary,
                                    accent.primary.copy(alpha = 0.90f),
                                    accent.secondary
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.34f),
                            RoundedCornerShape(VennelaTokens.radiusPill)
                        )
                        .pressScale(0.97f) {
                            infoFile = null
                            openResult(file)
                        }
                        .padding(
                            horizontal = VennelaTokens.spaceLg,
                            vertical = VennelaTokens.spaceMd
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                    Text(
                        "Open this file",
                        color = Color.White,
                        fontSize = VennelaTokens.body,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Search field                                                               */
/* ------------------------------------------------------------------------- */

/**
 * The hero search capsule. Focus swells the glass: the rim brightens, the body
 * thickens and the whole pill lifts a hair, so focusing is visible before the
 * keyboard even finishes opening.
 */
@Composable
private fun LargeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val focusProgress by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = LiquidMotion.glide(LiquidMotion.durationFast),
        label = "searchFocus"
    )
    val rimWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 1.dp,
        animationSpec = LiquidMotion.press(),
        label = "searchRim"
    )

    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    val base = liquidGlassSpec(
        alpha = 0.48f + 0.22f * focusProgress,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationBar,
        sheen = 0.22f + 0.16f * focusProgress,
        highlight = 0.18f + 0.12f * focusProgress
    )
    val spec = base.copy(
        rim = lerp(base.rim, accent.secondary.copy(alpha = 0.92f), focusProgress),
        shadowColor = lerp(base.shadowColor, accent.primary.copy(alpha = 0.45f), focusProgress)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val grow = 1f + 0.012f * focusProgress
                scaleX = grow
                scaleY = grow
            }
            .shadow(
                elevation = 14.dp + 10.dp * focusProgress,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.40f + 0.35f * focusProgress),
                spotColor = accent.primary.copy(alpha = 0.40f + 0.35f * focusProgress)
            )
            .liquidGlass(spec, shape)
            .border(
                rimWidth,
                accent.secondary.copy(alpha = 0.24f + 0.56f * focusProgress),
                shape
            )
            .padding(start = VennelaTokens.spaceLg, end = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = if (focused) accent.secondary else palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconLg)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            interactionSource = interaction,
            textStyle = TextStyle(color = palette.textPrimary, fontSize = 17.sp),
            cursorBrush = SolidColor(accent.secondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search files, folders, formats...",
                            color = palette.textTertiary,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(VennelaTokens.touchTargetMin)
                    .clip(CircleShape)
                    .pressScale(0.88f) { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.secondary.copy(alpha = 0.20f))
                        .border(1.dp, accent.secondary.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = accent.secondary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Result rows                                                                */
/* ------------------------------------------------------------------------- */

/** Highlights every occurrence of the live query inside a file name. */
@Composable
private fun highlightedName(name: String, query: String): AnnotatedString {
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(name)
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < name.length) {
            val found = name.indexOf(needle, cursor, ignoreCase = true)
            if (found < 0) {
                append(name.substring(cursor))
                break
            }
            append(name.substring(cursor, found))
            withStyle(
                SpanStyle(
                    color = if (palette.isDark) accent.secondary else accent.primary,
                    background = accent.primary.copy(alpha = 0.22f),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(name.substring(found, found + needle.length))
            }
            cursor = found + needle.length
        }
    }
}

@Composable
private fun FormatRow(
    extension: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val formatShape = RoundedCornerShape(VennelaTokens.radiusTile)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = formatShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.42f),
                spotColor = accent.primary.copy(alpha = 0.42f)
            )
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.46f,
                    frost = VennelaTokens.frostLight,
                    elevation = VennelaTokens.elevationRaised,
                    sheen = 0.22f,
                    highlight = 0.16f
                ),
                formatShape
            )
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.14f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, accent.secondary.copy(alpha = 0.28f), formatShape)
            .pressScale(0.985f, onClick)
            .padding(VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileBrandBadge(extension = extension, size = VennelaTokens.badgeSm)
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = palette.textSecondary,
                fontSize = VennelaTokens.micro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = accent.secondary.copy(alpha = 0.85f),
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
    }
}

@Composable
fun SearchResultCard(
    file: FileItem,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val cardShape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.48f),
                spotColor = accent.primary.copy(alpha = 0.48f)
            )
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.50f,
                    frost = VennelaTokens.frostRegular,
                    elevation = VennelaTokens.elevationCard,
                    sheen = 0.24f,
                    highlight = 0.18f
                ),
                cardShape
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.16f),
                        Color.Transparent,
                        accent.secondary.copy(alpha = 0.10f)
                    )
                )
            )
            .border(1.dp, accent.secondary.copy(alpha = 0.34f), cardShape)
            .pressScale(0.985f, onClick)
            .padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeMd)
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightedName(file.name, query),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        FilePreviewCapsule(extension = file.extension)
        IconButton(onClick = onOptionsClick, modifier = Modifier.size(VennelaTokens.touchTargetMin)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options for ${file.name}",
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
        }
    }
}
