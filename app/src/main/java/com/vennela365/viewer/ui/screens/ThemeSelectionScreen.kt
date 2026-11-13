package com.vennela365.viewer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.AccentOption
import com.vennela365.viewer.theme.AppTheme
import com.vennela365.viewer.theme.DarkVennelaPalette
import com.vennela365.viewer.theme.FidelityPreference
import com.vennela365.viewer.theme.LightVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.ThemeMode
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.tinted
import com.vennela365.viewer.theme.toAccent
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private val themeFilters = listOf("All", "Light", "Dark", "Nature", "Minimal", "Colorful")

/** One grid item that spans the full width of the theme grid. */
private val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

private fun themesFor(filter: String): List<AppTheme> = when (filter) {
    "Light" -> AppTheme.entries.filter { !it.isDark }
    "Dark" -> AppTheme.entries.filter { it.isDark }
    "Nature" -> AppTheme.entries.filter {
        it in listOf(AppTheme.FOREST_GREEN, AppTheme.OCEAN_BLUE, AppTheme.SUNSET, AppTheme.SAKURA_PINK)
    }
    "Minimal" -> AppTheme.entries.filter { it in listOf(AppTheme.MINIMAL_LIGHT, AppTheme.MINIMAL_DARK) }
    "Colorful" -> AppTheme.entries.filter { it in listOf(AppTheme.AURORA, AppTheme.GALAXY, AppTheme.SUNRISE_GLOW) }
    else -> AppTheme.entries
}

private fun fidelityDescription(pref: FidelityPreference): String = when (pref) {
    FidelityPreference.AUTO ->
        "Measures frame time, heap pressure and thermal state, then drops or restores glass passes automatically."
    FidelityPreference.FORCE_ULTRA ->
        "Always full chromatic dispersion and specular highlights. Best looking, heaviest on battery."
    FidelityPreference.FORCE_PERFORMANCE ->
        "No per-frame blur passes at all. Flattest glass, lowest power draw, safest on low-end panels."
}

@Composable
fun ThemeSelectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTheme by viewModel.activeTheme.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val fidelityPref by viewModel.fidelityPreference.collectAsState()
    val fidelityTier by viewModel.fidelityGovernorTier.collectAsState()
    val activeAccent by viewModel.accent.collectAsState()

    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var selectedFilter by remember { mutableStateOf("All") }
    val themes = remember(selectedFilter) { themesFor(selectedFilter) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Item indexes inside the grid, used by the "Customize" bar to jump to the swatches.
    val accentSectionIndex = 2 + themes.size

    LiquidPage(
        modifier = modifier,
        title = "Theme Selection",
        subtitle = "Personalize Vennela 365 with beautiful themes",
        onBack = { viewModel.navigateBack() },
        topBarActions = {
            ThemeModeCycleButton(
                mode = themeMode,
                onCycle = {
                    val next = when (themeMode) {
                        ThemeMode.DARK -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.SYSTEM
                        ThemeMode.SYSTEM -> ThemeMode.DARK
                    }
                    viewModel.setThemeMode(next)
                }
            )
        }
    ) { pagePadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = pagePadding.calculateTopPadding() + VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            /* ---------- Appearance mode (dark / light / system) ---------- */
            item(span = fullSpan, key = "mode") {
                GlassCard {
                    Text(
                        text = "Appearance",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            LiquidChip(
                                text = mode.label,
                                icon = when (mode) {
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                },
                                isActive = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Text(
                        text = when (themeMode) {
                            ThemeMode.DARK -> "Moonlit surfaces stay dark regardless of the system setting."
                            ThemeMode.LIGHT -> "Daylight surfaces, designed rather than inverted."
                            ThemeMode.SYSTEM -> "Follows the Android system setting and switches live."
                        },
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption
                    )
                }
            }

            /* ---------- Category filter (horizontally scrollable) ---------- */
            item(span = fullSpan, key = "filter") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                    items(themeFilters, key = { it }) { filter ->
                        LiquidChip(
                            text = filter,
                            isActive = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }

            /* ---------- Theme tiles with live app miniature ---------- */
            items(themes, key = { it.id }) { theme ->
                val index = themes.indexOf(theme)
                ThemeTile(
                    theme = theme,
                    selected = activeTheme == theme,
                    modifier = Modifier.staggeredEntrance(index),
                    onClick = { viewModel.setTheme(theme) }
                )
            }

            /* ---------- Accent swatches ---------- */
            item(span = fullSpan, key = "accents") {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Palette,
                            colors = listOf(accent.primary, accent.secondary),
                            size = VennelaTokens.badgeSm,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Accent highlights",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = activeAccent?.label?.let { "Override: $it" } ?: "Following the selected theme",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.caption
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(VennelaTokens.touchTargetMin)
                                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                .background(palette.cardSurfaceStrong)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusPill))
                                .semantics {
                                    contentDescription = "Reset accent to the theme default"
                                }
                                .pressScale(0.95f) { viewModel.clearAccent() }
                                .padding(horizontal = VennelaTokens.spaceMd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Theme default",
                                color = accent.secondary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceMd))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)) {
                        items(AccentOption.entries, key = { it.id }) { option ->
                            AccentSwatch(
                                option = option,
                                selected = activeAccent?.id == option.id,
                                onClick = { viewModel.setAccent(option) }
                            )
                        }
                    }
                }
            }

            /* ---------- Color matrix ---------- */
            item(span = fullSpan, key = "colormatrix") {
                GlassCard {
                    Text(
                        text = "Display engine & colour matrices",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Hue-preserving inversion, binarization and colour-vision compensation.",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                        items(ViewerColorMode.entries, key = { it.id }) { mode ->
                            LiquidChip(
                                text = mode.title,
                                isActive = viewerColorMode == mode,
                                onClick = { viewModel.setViewerColorMode(mode) }
                            )
                        }
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Text(
                        text = viewerColorMode.description,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption
                    )
                }
            }

            /* ---------- Fidelity ---------- */
            item(span = fullSpan, key = "fidelity-header") {
                SectionHeader(
                    title = "Hardware fidelity (Φ)",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = accent.secondary,
                                modifier = Modifier.size(VennelaTokens.iconSm)
                            )
                            Spacer(Modifier.width(VennelaTokens.spaceXs))
                            Text(
                                text = fidelityTier.label,
                                color = accent.secondary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }

            item(span = fullSpan, key = "fidelity-active") {
                GlassCard {
                    Text(
                        text = "Active pipeline",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = fidelityTier.description,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    GlassDivider()
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                    ) {
                        TierFact("Target", "${fidelityTier.maxFps} fps", Modifier.weight(1f))
                        TierFact("Blur passes", fidelityTier.blurPasses.toString(), Modifier.weight(1f))
                        TierFact(
                            label = "Dispersion",
                            value = if (fidelityTier.enablesChromaticDispersion) "On" else "Off",
                            modifier = Modifier.weight(1f)
                        )
                        TierFact(
                            label = "Specular",
                            value = if (fidelityTier.enablesSpecularHighlights) "On" else "Off",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            items(FidelityPreference.entries.toList(), key = { it.id }) { pref ->
                FidelityOptionCard(
                    pref = pref,
                    selected = fidelityPref == pref,
                    onSelect = { viewModel.setFidelityPreference(pref) }
                )
            }

            /* ---------- Customize ---------- */
            item(span = fullSpan, key = "customize") {
                GlassCard(onClick = {
                    scope.launch {
                        gridState.animateScrollToItem(accentSectionIndex.coerceAtLeast(0))
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Palette,
                            colors = listOf(accent.primary, accent.secondary),
                            size = VennelaTokens.badgeMd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Customize theme",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Jump to the accent swatches and build your own style",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.caption
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textTertiary,
                            modifier = Modifier.size(VennelaTokens.iconMd)
                        )
                    }
                }
            }

            item(span = fullSpan, key = "footer") { Spacer(Modifier.height(VennelaTokens.spaceSm)) }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Pieces                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun ThemeModeCycleButton(mode: ThemeMode, onCycle: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .background(accent.primary.copy(alpha = 0.24f))
            .border(1.dp, accent.secondary.copy(alpha = 0.4f), CircleShape)
            .pressScale(0.9f, onCycle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (mode) {
                ThemeMode.DARK -> Icons.Default.DarkMode
                ThemeMode.LIGHT -> Icons.Default.LightMode
                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
            },
            contentDescription = "Appearance: ${mode.label}. Tap to change.",
            tint = palette.textPrimary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

/**
 * A miniature of the app rendered in the theme's own colours: glass top bar,
 * three floating file tiles and the dock, so the tile previews the real mood
 * instead of showing a flat swatch.
 *
 * The preview goes through the same resolution path as the live app — the real
 * palette for that brightness, tinted with the theme's own accent pair — so it
 * can never drift from what selecting the theme actually does.
 */
@Composable
private fun ThemeTile(
    theme: AppTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCard)

    // Selection animates rather than snapping: the body brightens, the neon
    // glow swells and the check badge springs in.
    val ringColor by animateColorAsState(
        targetValue = if (selected) accent.secondary else palette.cardBorder.copy(alpha = 0.45f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "themeTileRing"
    )
    val glowElevation by animateFloatAsState(
        targetValue = if (selected) 26f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "themeTileGlow"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "themeTileBadge"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.96f, onClick = onClick)
            .shadow(
                elevation = glowElevation.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.55f),
                spotColor = accent.primary.copy(alpha = 0.55f)
            )
            .semantics {
                contentDescription = "${theme.title} theme, ${theme.subtitle}" +
                    if (selected) ", currently selected" else ""
            }
    ) {
        GlassCard(alpha = if (selected) 0.56f else 0.42f) {
            // Signature gradient preview band: the theme's own accent pair.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VennelaTokens.badgeLg)
                    .clip(RoundedCornerShape(VennelaTokens.radiusBadge))
                    .background(
                        Brush.linearGradient(
                            listOf(theme.primaryAccent, theme.secondaryAccent)
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.30f),
                        RoundedCornerShape(VennelaTokens.radiusBadge)
                    ),
                contentAlignment = Alignment.Center
            ) {
                ThemeSwatchPair(theme = theme)
            }

            Spacer(Modifier.height(VennelaTokens.spaceSm))

            ThemeMiniature(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VennelaTokens.badgeLg * 2)
            )
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${theme.id} · ${theme.title}",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = theme.subtitle,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.nano,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(VennelaTokens.spaceSm))
                ThemeSwatchPair(theme = theme)
            }
        }

        // White hairline ring + outer glow on the selected tile.
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = glowElevation.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.60f),
                    spotColor = theme.primaryAccent.copy(alpha = 0.60f)
                )
                .clip(shape)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = ringColor,
                    shape = shape
                )
        )

        // Check badge springs in only when this theme is the live one.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(VennelaTokens.spaceSm)
                .graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                    alpha = badgeScale
                }
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(VennelaTokens.radiusChip),
                    clip = false,
                    ambientColor = theme.primaryAccent.copy(alpha = 0.60f),
                    spotColor = theme.secondaryAccent.copy(alpha = 0.60f)
                )
        ) {
            IconBadge(
                icon = Icons.Default.Check,
                colors = listOf(theme.primaryAccent, theme.secondaryAccent),
                size = VennelaTokens.iconLg + VennelaTokens.spaceXs,
                radius = VennelaTokens.radiusChip,
                contentDescription = "${theme.title} theme selected"
            )
        }
    }
}

/** The real accent pair this theme applies, side by side. */
@Composable
private fun ThemeSwatchPair(theme: AppTheme) {
    val palette = LocalVennelaPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)) {
        Box(
            modifier = Modifier
                .size(VennelaTokens.iconMd)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(theme.primaryAccent, theme.secondaryAccent)
                    )
                )
                .border(1.dp, palette.cardBorder, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(VennelaTokens.iconMd)
                .clip(CircleShape)
                .background(theme.secondaryAccent)
                .border(1.dp, palette.cardBorder, CircleShape)
        )
    }
}

@Composable
private fun ThemeMiniature(theme: AppTheme, modifier: Modifier = Modifier) {
    val preview = remember(theme) {
        (if (theme.isDark) DarkVennelaPalette else LightVennelaPalette)
            .tinted(theme.toAccent())
    }
    val surface = preview.cardSurface
    val onSurface = preview.textPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VennelaTokens.radiusTile))
            .background(
                Brush.verticalGradient(
                    listOf(preview.backgroundTop, preview.backgroundBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VennelaTokens.spaceSm)
        ) {
            // Miniature top bar.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.spaceLg)
                        .clip(CircleShape)
                        .background(surface)
                )
                Spacer(Modifier.width(VennelaTokens.spaceXs))
                Column(Modifier.weight(1f)) {
                    MiniBar(
                        fraction = 0.62f,
                        thickness = VennelaTokens.spaceXs,
                        color = onSurface.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceXxs))
                    MiniBar(
                        fraction = 0.4f,
                        thickness = VennelaTokens.spaceXxs,
                        color = onSurface.copy(alpha = 0.4f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.spaceLg)
                        .clip(CircleShape)
                        .background(theme.primaryAccent.copy(alpha = 0.85f))
                )
            }

            Spacer(Modifier.height(VennelaTokens.spaceSm))

            // Three floating file tiles, like the reference thumbnails.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconBadge(
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        colors = listOf(theme.primaryAccent, theme.secondaryAccent),
                        size = VennelaTokens.spaceXxl,
                        radius = VennelaTokens.spaceSm
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconBadge(
                        icon = Icons.Default.Image,
                        colors = listOf(theme.secondaryAccent, theme.primaryAccent),
                        size = VennelaTokens.spaceXxl,
                        radius = VennelaTokens.spaceSm
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconBadge(
                        icon = Icons.Default.Audiotrack,
                        colors = listOf(theme.primaryAccent, theme.secondaryAccent),
                        size = VennelaTokens.spaceXxl,
                        radius = VennelaTokens.spaceSm
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Miniature glass card + dock.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                    .background(surface)
                    .padding(
                        horizontal = VennelaTokens.spaceXs,
                        vertical = VennelaTokens.spaceXs
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.spaceSm)
                        .clip(RoundedCornerShape(VennelaTokens.spaceXxs))
                        .background(theme.secondaryAccent)
                )
                Spacer(Modifier.width(VennelaTokens.spaceXs))
                MiniBar(
                    fraction = 0.55f,
                    thickness = VennelaTokens.spaceXxs,
                    color = onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(VennelaTokens.spaceXs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(surface)
                    .padding(vertical = VennelaTokens.spaceXs),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(VennelaTokens.spaceSm)
                            .clip(CircleShape)
                            .background(
                                if (index == 0) theme.primaryAccent
                                else onSurface.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}

/** One line of "text" inside the miniature, sized from the token scale. */
@Composable
private fun MiniBar(
    fraction: Float,
    thickness: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(thickness)
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(color)
    )
}

@Composable
private fun AccentSwatch(option: AccentOption, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val glow by animateFloatAsState(
        targetValue = if (selected) 18f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "accentSwatchGlow"
    )
    val ringColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.92f) else palette.cardBorder,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "accentSwatchRing"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
    ) {
        Box(modifier = Modifier.size(VennelaTokens.touchTargetMin + VennelaTokens.spaceLg)) {
            // Outer neon glow, sized so it never shifts the row.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(VennelaTokens.touchTargetMin + VennelaTokens.spaceXl)
                    .graphicsLayer { alpha = if (selected) 0.60f else 0f }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                option.secondary.copy(alpha = 0.55f),
                                option.primary.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(VennelaTokens.touchTargetMin)
                    .shadow(
                        elevation = glow.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = option.primary.copy(alpha = 0.60f),
                        spotColor = option.secondary.copy(alpha = 0.60f)
                    )
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(option.primary, option.secondary)))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = ringColor,
                        shape = CircleShape
                    )
                    .semantics { contentDescription = "${option.label} accent" }
                    .pressScale(0.92f, onClick),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "${option.label} accent selected",
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                }
            }
        }
        Text(
            text = option.label,
            color = if (selected) palette.textPrimary else palette.textSecondary,
            fontSize = VennelaTokens.nano,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun TierFact(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .background(palette.cardSurface.copy(alpha = 0.5f))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusChip))
            .padding(vertical = VennelaTokens.spaceSm, horizontal = VennelaTokens.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = accent.secondary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = VennelaTokens.nano,
            maxLines = 1
        )
    }
}

@Composable
private fun FidelityOptionCard(
    pref: FidelityPreference,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCard)
    val ringColor by animateColorAsState(
        targetValue = if (selected) accent.secondary else palette.cardBorder.copy(alpha = 0.40f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "fidelityRing"
    )
    val glowElevation by animateFloatAsState(
        targetValue = if (selected) 18f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "fidelityGlow"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.97f, onClick = onSelect)
            .shadow(
                elevation = glowElevation.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.50f),
                spotColor = accent.secondary.copy(alpha = 0.50f)
            )
            .semantics {
                contentDescription = "${pref.label} fidelity" +
                    if (selected) ", currently active" else ""
            }
    ) {
        GlassCard(alpha = if (selected) 0.52f else 0.38f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pref.label,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent.primary else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (selected) Color.White.copy(alpha = 0.75f) else palette.textTertiary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(VennelaTokens.iconSm)
                        )
                    }
                }
            }
            Spacer(Modifier.height(VennelaTokens.spaceXs))
            Text(
                text = fidelityDescription(pref),
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption,
                lineHeight = 17.sp
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = ringColor,
                    shape = shape
                )
        )
    }
}
