package com.vennela365.viewer.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.theme.AccentOption
import com.vennela365.viewer.theme.LiquidGlassSpec
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.ThemeMode
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import java.io.File
import java.util.Locale

/**
 * Settings, rebuilt on the liquid-glass system.
 *
 * One [LiquidPage] frame (atmosphere, insets, frosted top bar, dock clearance),
 * every surface from the glass component library, every colour from the palette
 * / accent composition locals, and every number from [VennelaTokens]. All
 * controls stay wired to the real [MainViewModel].
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val themeMode by viewModel.themeMode.collectAsState()
    val activeAccent by viewModel.accent.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()

    val context = LocalContext.current
    var cacheBytes by remember { mutableStateOf(cacheSizeBytes(context)) }
    var cacheCleared by remember { mutableStateOf(false) }

    LiquidPage(
        modifier = modifier,
        title = "Settings",
        subtitle = "Customize your experience",
        onBack = { viewModel.navigateBack() },
        topBarActions = {
            ThemeModeCycleButton(
                mode = themeMode,
                onCycle = { viewModel.setThemeMode(themeMode.next()) }
            )
        },
        dock = {
            FloatingDockNav(
                activeTab = "settings",
                onTabSelected = { tab ->
                    when (tab) {
                        "home" -> viewModel.navigateTo(Screen.Home)
                        "files" -> viewModel.navigateTo(Screen.AllFiles)
                        "search" -> viewModel.navigateTo(Screen.UniversalSearch)
                        "favorites" -> viewModel.navigateTo(Screen.Favorites)
                        "settings" -> Unit
                    }
                }
            )
        }
    ) { pagePadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = pagePadding.calculateTopPadding() + VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            )
        ) {
            item(key = "appearance-header") { SectionHeader(title = "Appearance") }
            item(key = "appearance") {
                AppearanceSection(
                    themeMode = themeMode,
                    activeAccent = activeAccent,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onAccentChange = { viewModel.setAccent(it) },
                    onClearAccent = { viewModel.clearAccent() },
                    onOpenThemes = { viewModel.navigateTo(Screen.ThemeSelection) }
                )
            }

            item(key = "reading-header") { SectionHeader(title = "Reading & Viewing") }
            item(key = "reading") {
                SettingsGroup(index = 1) {
                    SettingsLinkRow(
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        title = "File Associations",
                        subtitle = "Manage supported file types",
                        onClick = { viewModel.navigateTo(Screen.Categories) }
                    )
                    GlassDivider()
                    SettingsLinkRow(
                        icon = Icons.Default.Translate,
                        title = "Text Encoding",
                        subtitle = "UTF-8 default • change per file",
                        onClick = { viewModel.navigateTo(Screen.EncodingSelection) }
                    )
                    GlassDivider()
                    SettingsLinkRow(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        title = "Sort & Filter",
                        subtitle = "Name, date, size or type",
                        onClick = { viewModel.navigateTo(Screen.SortFilterSheet) }
                    )
                    GlassDivider()
                    SettingsLinkRow(
                        icon = Icons.AutoMirrored.Filled.HelpCenter,
                        title = "Supported Formats",
                        subtitle = "Search the live format registry",
                        onClick = { viewModel.navigateTo(Screen.HelpFormats) }
                    )
                    GlassDivider()
                    SettingsLinkRow(
                        icon = Icons.Default.Palette,
                        title = "Manage Modules",
                        subtitle = "On-demand renderers, never silent downloads",
                        onClick = { viewModel.navigateTo(Screen.OptionalModules) }
                    )
                }
            }

            item(key = "storage-header") { SectionHeader(title = "Storage") }
            item(key = "storage") {
                SettingsGroup(index = 2) {
                    SettingsLinkRow(
                        icon = Icons.Default.Storage,
                        title = "Cache Usage",
                        subtitle = "Thumbnails and temporary previews",
                        trailing = {
                            Text(
                                text = formatBytes(cacheBytes),
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                        },
                        onClick = { viewModel.navigateTo(Screen.CacheStorage) }
                    )
                    GlassDivider()
                    Column(
                        modifier = Modifier.padding(
                            horizontal = VennelaTokens.spaceLg,
                            vertical = VennelaTokens.spaceMd
                        )
                    ) {
                        Text(
                            text = if (cacheCleared) {
                                "Cache cleared. Your documents were untouched."
                            } else {
                                "Clearing removes thumbnails and temporary previews only — your documents are never touched."
                            },
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.caption
                        )
                        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                        LiquidButton(
                            label = if (cacheCleared) "Cache cleared" else "Clear cache",
                            icon = Icons.Default.CleaningServices,
                            onClick = {
                                clearCache(context)
                                cacheBytes = cacheSizeBytes(context)
                                cacheCleared = true
                            }
                        )
                    }
                }
            }

            item(key = "privacy-header") { SectionHeader(title = "Privacy") }
            item(key = "privacy") {
                val accent = LocalVennelaAccent.current
                // Toggle states animate instead of snapping.
                val switchTrack by animateColorAsState(
                    targetValue = if (offlineMode) accent.primary else palette.cardSurface,
                    animationSpec = tween(LiquidMotion.durationBase),
                    label = "offlineTrack"
                )
                val switchThumb by animateColorAsState(
                    targetValue = if (offlineMode) palette.textPrimary else palette.textTertiary,
                    animationSpec = tween(LiquidMotion.durationBase),
                    label = "offlineThumb"
                )
                SettingsGroup(index = 3) {
                    GlassListRow(
                        title = "Offline Mode",
                        subtitle = if (offlineMode) {
                            "On — no files leave your device"
                        } else {
                            "Off — network features are available"
                        },
                        leading = { SettingsIconBadge(Icons.Default.WifiOff) },
                        trailing = {
                            Switch(
                                checked = offlineMode,
                                onCheckedChange = { viewModel.setOfflineMode(it) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Offline mode, no files leave this device"
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = switchThumb,
                                    checkedTrackColor = switchTrack,
                                    checkedBorderColor = accent.secondary,
                                    uncheckedThumbColor = switchThumb,
                                    uncheckedTrackColor = switchTrack,
                                    uncheckedBorderColor = palette.cardBorder
                                )
                            )
                        },
                        showChevron = false,
                        onClick = { viewModel.setOfflineMode(!offlineMode) }
                    )
                    GlassDivider()
                    SettingsLinkRow(
                        icon = Icons.Default.Security,
                        title = "Permissions",
                        subtitle = "Storage grants and access status",
                        onClick = { viewModel.navigateTo(Screen.Permissions) }
                    )
                    GlassDivider()
                    SettingsLinkRow(
                        icon = Icons.Default.BugReport,
                        title = "Diagnostics",
                        subtitle = "Registry health and honest capability counts",
                        onClick = { viewModel.navigateTo(Screen.DiagnosticsReport) }
                    )
                }
            }

            item(key = "about-header") { SectionHeader(title = "About") }
            item(key = "about") {
                SettingsGroup(index = 4) {
                    SettingsLinkRow(
                        icon = Icons.Default.Info,
                        title = "About Vennela 365",
                        subtitle = "Version 1.0 • వెన్నెల 365",
                        onClick = { viewModel.navigateTo(Screen.AboutLicenses) }
                    )
                }
            }

            item(key = "footer") {
                Text(
                    text = "Every preference is stored on this device · Vennela 365 works fully offline.",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro,
                    modifier = Modifier.padding(
                        start = VennelaTokens.spaceXs,
                        end = VennelaTokens.spaceXs,
                        top = VennelaTokens.spaceMd
                    )
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Appearance                                                                 */
/* ------------------------------------------------------------------------- */

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    activeAccent: AccentOption?,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentOption) -> Unit,
    onClearAccent: () -> Unit,
    onOpenThemes: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val trackSpec = liquidGlassSpec(
        alpha = 0.20f,
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationFlat,
        sheen = 0.08f,
        highlight = 0.06f
    )

    SettingsGroup(index = 0) {
        // Gradient hero header: the appearance entry point is the brightest
        // surface on the page.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceMd)
                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.34f),
                            accent.secondary.copy(alpha = 0.20f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.24f),
                    RoundedCornerShape(VennelaTokens.radiusCardLarge)
                )
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    clip = false,
                    ambientColor = accent.primary.copy(alpha = 0.45f),
                    spotColor = accent.secondary.copy(alpha = 0.45f)
                )
        ) {
            SettingsLinkRow(
                icon = Icons.Default.Palette,
                title = "Theme Gallery",
                subtitle = "10 handcrafted lunar and nature themes",
                onClick = onOpenThemes
            )
        }
        Spacer(Modifier.height(VennelaTokens.spaceSm))
        GlassDivider()

        Column(
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            )
        ) {
            SettingsBlockLabel(
                icon = Icons.Default.DarkMode,
                title = "Theme mode",
                value = themeMode.label
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(trackSpec, RoundedCornerShape(VennelaTokens.radiusControl))
                    .padding(VennelaTokens.spaceXs),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
            ) {
                ThemeMode.entries.forEach { mode ->
                    ThemeModeSegment(
                        mode = mode,
                        isSelected = themeMode == mode,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeModeChange(mode) }
                    )
                }
            }
        }
        GlassDivider()

        Column(
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            )
        ) {
            SettingsBlockLabel(
                icon = Icons.Default.ColorLens,
                title = "Accent colour",
                value = activeAccent?.label ?: "Theme default"
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                items(AccentOption.entries, key = { it.id }) { option ->
                    AccentSwatch(
                        option = option,
                        isSelected = activeAccent?.id == option.id,
                        onClick = { onAccentChange(option) }
                    )
                }
            }
            if (activeAccent != null) {
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                LiquidChip(
                    text = "Use theme colours",
                    icon = Icons.Default.Restore,
                    modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                    onClick = onClearAccent
                )
            }
        }
    }
}

/**
 * One segment of the theme-mode control. The selected segment carries an
 * accent-tinted glass body; its foreground is `palette.textPrimary`, never a
 * hardcoded white, so light and dark both stay legible.
 */
@Composable
private fun ThemeModeSegment(
    mode: ThemeMode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusBadge)
    // The selected segment brightens and grows a soft neon halo.
    val tintStart by animateColorAsState(
        targetValue = if (isSelected) accent.primary.copy(alpha = 0.62f) else accent.primary.copy(alpha = 0.18f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "segmentTint"
    )
    val tintEnd by animateColorAsState(
        targetValue = if (isSelected) accent.secondary.copy(alpha = 0.50f) else accent.secondary.copy(alpha = 0.12f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "segmentTintEnd"
    )
    val rim by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.62f) else accent.secondary.copy(alpha = 0.24f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "segmentRim"
    )
    val halo by animateFloatAsState(
        targetValue = if (isSelected) 12f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "segmentHalo"
    )
    val foreground = if (isSelected) palette.textPrimary else palette.textSecondary
    val spec = LiquidGlassSpec(
        tint = tintStart,
        tintEnd = tintEnd,
        frost = VennelaTokens.frostLight,
        backdropAlpha = 0.5f,
        rim = rim,
        sheen = if (isSelected) 0.30f else 0.10f,
        elevation = VennelaTokens.elevationRaised,
        highlight = if (isSelected) 0.22f else 0.08f
    )

    Row(
        modifier = modifier
            .heightIn(min = VennelaTokens.touchTargetMin)
            .shadow(
                elevation = halo.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.55f),
                spotColor = accent.secondary.copy(alpha = 0.55f)
            )
            .then(if (isSelected) Modifier.liquidGlass(spec, shape) else Modifier)
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .semantics {
                selected = isSelected
                contentDescription = "${mode.label} theme mode"
            }
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceSm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = themeModeIcon(mode),
            contentDescription = null,
            tint = if (isSelected) palette.textPrimary else palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = mode.label,
            color = foreground,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccentSwatch(
    option: AccentOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val glow by animateFloatAsState(
        targetValue = if (isSelected) 18f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "accentSwatchGlow"
    )
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.92f) else palette.cardBorder,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "accentSwatchRing"
    )
    Box(
        modifier = Modifier
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
                width = if (isSelected) 2.dp else 1.dp,
                color = ringColor,
                shape = CircleShape
            )
            .pressScale(pressedScale = 0.92f, onClick = onClick)
            .semantics {
                selected = isSelected
                contentDescription = "${option.label} accent colour"
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
        }
    }
}

@Composable
private fun ThemeModeCycleButton(mode: ThemeMode, onCycle: () -> Unit) {
    val accent = LocalVennelaAccent.current
    IconBadge(
        icon = themeModeIcon(mode),
        colors = listOf(accent.primary, accent.secondary),
        size = VennelaTokens.touchTargetMin,
        radius = VennelaTokens.radiusPill,
        contentDescription = "Theme mode: ${mode.label}. Tap to switch.",
        modifier = Modifier.pressScale(pressedScale = 0.90f, onClick = onCycle)
    )
}

@Composable
private fun SettingsBlockLabel(icon: ImageVector, title: String, value: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingsIconBadge(icon)
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = accent.secondary,
                fontSize = VennelaTokens.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Shared building blocks                                                     */
/* ------------------------------------------------------------------------- */

/** A settings group: one bright frosted panel holding rows and dividers. */
@Composable
private fun SettingsGroup(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = LocalVennelaAccent.current
    val glow by animateFloatAsState(
        targetValue = 12f,
        animationSpec = tween(LiquidMotion.durationSlow),
        label = "settingsGroupGlow"
    )
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = glow.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusCard),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.28f),
                spotColor = accent.secondary.copy(alpha = 0.28f)
            )
            .border(
                1.dp,
                accent.secondary.copy(alpha = 0.20f),
                RoundedCornerShape(VennelaTokens.radiusCard)
            )
            .staggeredEntrance(index),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationCard,
        alpha = 0.46f
    ) {
        Column(
            modifier = Modifier.padding(vertical = VennelaTokens.spaceXs),
            content = content
        )
    }
}

/** A navigable settings row with a gradient icon badge and a chevron. */
@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    GlassListRow(
        title = title,
        subtitle = subtitle,
        leading = { SettingsIconBadge(icon) },
        trailing = trailing,
        showChevron = showChevron,
        onClick = onClick
    )
}

@Composable
private fun SettingsIconBadge(icon: ImageVector) {
    val accent = LocalVennelaAccent.current
    // Gradient badge with a soft neon halo so the leading edge of every row
    // reads as luminous rather than flat.
    Box(
        modifier = Modifier
            .size(VennelaTokens.badgeSm)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusChip),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.55f),
                spotColor = accent.secondary.copy(alpha = 0.55f)
            ),
        contentAlignment = Alignment.Center
    ) {
        IconBadge(
            icon = icon,
            colors = listOf(accent.primary, accent.secondary),
            size = VennelaTokens.badgeSm,
            radius = VennelaTokens.radiusChip,
            contentDescription = null
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Small helpers                                                              */
/* ------------------------------------------------------------------------- */

private fun themeModeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.DARK -> Icons.Default.DarkMode
    ThemeMode.LIGHT -> Icons.Default.LightMode
    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
}

private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.DARK -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.SYSTEM
    ThemeMode.SYSTEM -> ThemeMode.DARK
}

private fun cacheSizeBytes(context: Context): Long {
    fun sizeOf(file: File): Long = runCatching {
        if (!file.exists()) 0L
        else if (file.isFile) file.length()
        else file.listFiles()?.sumOf { sizeOf(it) } ?: 0L
    }.getOrDefault(0L)
    return sizeOf(context.cacheDir)
}

private fun clearCache(context: Context) {
    runCatching {
        context.cacheDir.listFiles()?.forEach { child ->
            runCatching { child.deleteRecursively() }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
