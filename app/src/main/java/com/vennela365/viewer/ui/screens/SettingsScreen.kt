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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewQuilt
import com.vennela365.viewer.theme.AccentOption
import com.vennela365.viewer.theme.AppFontStyle
import com.vennela365.viewer.theme.AppTheme
import com.vennela365.viewer.theme.DarkVennelaPalette
import com.vennela365.viewer.theme.FidelityPreference
import com.vennela365.viewer.theme.FidelityTier
import com.vennela365.viewer.theme.LightVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.ThemeMode
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.tinted
import com.vennela365.viewer.theme.toAccent
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.rememberLazyScrollSignal
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import java.io.File
import java.util.Locale

/**
 * Apple iOS 26 Liquid Glass Settings & Appearance Studio.
 *
 * Every surface responds instantaneously on the same frame when switching
 * between Dark, Light, and System modes or selecting any theme/accent.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val activeTheme by viewModel.activeTheme.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val activeAccent by viewModel.accent.collectAsState()
    val fidelityPref by viewModel.fidelityPreference.collectAsState()
    val fidelityTier by viewModel.fidelityGovernorTier.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()
    val preferredSuite by viewModel.preferredSuiteStyle.collectAsState()
    val appFontStyle by viewModel.appFontStyle.collectAsState()

    val context = LocalContext.current
    var cacheBytes by remember { mutableStateOf(cacheSizeBytes(context)) }
    var cacheCleared by remember { mutableStateOf(false) }

    val settingsListState = rememberLazyListState()

    LiquidPage(
        modifier = modifier,
        title = "Settings",
        subtitle = "${activeTheme.title} • ${themeMode.label} Mode",
        onBack = { viewModel.navigateBack() },
        scrollState = rememberLazyScrollSignal(settingsListState),
        topBarActions = {
            ThemeModeCycleButton(
                mode = themeMode,
                modifier = Modifier.testTag("settings_theme_cycle_button"),
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = settingsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = pagePadding.calculateTopPadding() + VennelaTokens.spaceSm,
                    bottom = pagePadding.calculateBottomPadding()
                )
            ) {
                item(key = "appearance-header") {
                    SectionHeader(
                        title = "Appearance & Liquid Glass",
                        actionLabel = "All 10 Themes",
                        onAction = { viewModel.navigateTo(Screen.ThemeSelection) }
                    )
                }
                item(key = "appearance") {
                    AppearanceSection(
                        activeTheme = activeTheme,
                        themeMode = themeMode,
                        activeAccent = activeAccent,
                        fidelityPref = fidelityPref,
                        fidelityTier = fidelityTier,
                        onThemeSelect = { viewModel.setTheme(it) },
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        onAccentChange = { viewModel.setAccent(it) },
                        onClearAccent = { viewModel.clearAccent() },
                        onFidelityChange = { viewModel.setFidelityPreference(it) },
                        onOpenThemes = { viewModel.navigateTo(Screen.ThemeSelection) }
                    )
                }

                item(key = "reading-header") { SectionHeader(title = "Viewing Suite & OnePlus Typography") }
                item(key = "reading") {
                    SettingsGroup(index = 1) {
                        // Default Multi-Suite File Viewing Style Selector
                        Column(
                            modifier = Modifier.padding(
                                horizontal = VennelaTokens.spaceLg,
                                vertical = VennelaTokens.spaceMd
                            )
                        ) {
                            SettingsBlockLabel(
                                icon = Icons.Default.ViewQuilt,
                                title = "Default file viewing suite",
                                value = "${preferredSuite.badge} • ${preferredSuite.subtitle}"
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                            ) {
                                items(ViewerSuiteStyle.entries, key = { it.id }) { suite ->
                                    LiquidChip(
                                        text = "${suite.shortTitle} (${suite.badge})",
                                        isActive = preferredSuite == suite,
                                        onClick = { viewModel.setPreferredSuiteStyle(suite) },
                                        modifier = Modifier.testTag("settings_suite_${suite.id}")
                                    )
                                }
                            }
                        }
                        GlassDivider()
                        // App-Wide Typography Engine (OnePlus Sans Default)
                        Column(
                            modifier = Modifier.padding(
                                horizontal = VennelaTokens.spaceLg,
                                vertical = VennelaTokens.spaceMd
                            )
                        ) {
                            SettingsBlockLabel(
                                icon = Icons.Default.TextFields,
                                title = "App & reader typography",
                                value = "${appFontStyle.title} • ${appFontStyle.subtitle}"
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                            ) {
                                items(AppFontStyle.entries, key = { it.id }) { fontStyle ->
                                    LiquidChip(
                                        text = fontStyle.title,
                                        isActive = appFontStyle == fontStyle,
                                        onClick = { viewModel.setAppFontStyle(fontStyle) },
                                        modifier = Modifier.testTag("settings_font_${fontStyle.id}")
                                    )
                                }
                            }
                        }
                        GlassDivider()
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
                                    fontFamily = VennelaFonts.Mono,
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
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.caption
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                            LiquidButton(
                                label = if (cacheCleared) "Cache cleared" else "Clear cache",
                                icon = Icons.Default.CleaningServices,
                                modifier = Modifier.testTag("settings_clear_cache_button"),
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
                    val switchTrack by animateColorAsState(
                        targetValue = if (offlineMode) accent.primary else palette.cardSurfaceStrong,
                        animationSpec = tween(LiquidMotion.durationBase),
                        label = "offlineTrack"
                    )
                    val switchThumb by animateColorAsState(
                        targetValue = if (offlineMode) Color.White else palette.textTertiary,
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
                                    modifier = Modifier
                                        .testTag("settings_offline_switch")
                                        .semantics {
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
                        fontFamily = VennelaFonts.Sans,
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
}

/* ------------------------------------------------------------------------- */
/* Appearance & Liquid Glass Control Center                                   */
/* ------------------------------------------------------------------------- */

@Composable
private fun AppearanceSection(
    activeTheme: AppTheme,
    themeMode: ThemeMode,
    activeAccent: AccentOption?,
    fidelityPref: FidelityPreference,
    fidelityTier: FidelityTier,
    onThemeSelect: (AppTheme) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentOption) -> Unit,
    onClearAccent: () -> Unit,
    onFidelityChange: (FidelityPreference) -> Unit,
    onOpenThemes: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    SettingsGroup(index = 0) {
        // 1. Live Interactive Appearance Banner + Theme Gallery Launcher
        Box(
            modifier = Modifier
                .testTag("settings_theme_gallery_row")
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm)
                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = if (palette.isDark) 0.28f else 0.16f),
                            accent.secondary.copy(alpha = if (palette.isDark) 0.18f else 0.10f)
                        )
                    )
                )
                .border(
                    1.dp,
                    accent.secondary.copy(alpha = if (palette.isDark) 0.38f else 0.28f),
                    RoundedCornerShape(VennelaTokens.radiusCardLarge)
                )
                .pressScale(pressedScale = 0.98f, onClick = onOpenThemes)
                .padding(VennelaTokens.spaceMd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                LiveAppearanceMiniPreview(
                    isDark = palette.isDark,
                    primary = accent.primary,
                    secondary = accent.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Theme Gallery",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                .background(accent.primary.copy(alpha = if (palette.isDark) 0.28f else 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeTheme.title,
                                color = if (palette.isDark) accent.secondary else accent.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${themeMode.label} mode • ${activeAccent?.label ?: "Theme palette"} • ${fidelityTier.label}",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Theme Gallery",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        GlassDivider()

        // 2. Apple iOS 26 Segmented Appearance Mode Control (Dark / Light / System)
        Column(
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            )
        ) {
            SettingsBlockLabel(
                icon = themeModeIcon(themeMode),
                title = "Appearance mode",
                value = when (themeMode) {
                    ThemeMode.DARK -> "Dark • Obsidian Lunar Glass"
                    ThemeMode.LIGHT -> "Light • Daylight Pearl Glass"
                    ThemeMode.SYSTEM -> "System • Follows Android Display"
                }
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                    .background(
                        if (palette.isDark) Color(0xFF0E1320).copy(alpha = 0.72f)
                        else Color(0xFFE4EAF4).copy(alpha = 0.85f)
                    )
                    .border(
                        1.dp,
                        palette.cardBorder.copy(alpha = if (palette.isDark) 0.35f else 0.45f),
                        RoundedCornerShape(VennelaTokens.radiusControl)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    ThemeModeSegment(
                        mode = mode,
                        isSelected = themeMode == mode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_theme_mode_${mode.name.lowercase()}"),
                        onClick = { onThemeModeChange(mode) }
                    )
                }
            }
        }

        GlassDivider()

        // 3. Instant Quick-Theme Carousel right inside Settings
        Column(
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            )
        ) {
            SettingsBlockLabel(
                icon = Icons.Default.AutoAwesome,
                title = "Instant theme preset",
                value = "${activeTheme.title} (${if (activeTheme.isDark) "Dark" else "Light"})"
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                items(AppTheme.entries, key = { it.id }) { theme ->
                    QuickThemePill(
                        theme = theme,
                        isSelected = activeTheme == theme,
                        onClick = { onThemeSelect(theme) }
                    )
                }
            }
        }

        GlassDivider()

        // 4. Accent Colour Swatches
        Column(
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            )
        ) {
            SettingsBlockLabel(
                icon = Icons.Default.ColorLens,
                title = "Accent colour",
                value = activeAccent?.label ?: "Theme default (${activeTheme.title})"
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

        GlassDivider()

        // 5. Optical Fidelity (Φ) Quick Selector
        Column(
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            )
        ) {
            SettingsBlockLabel(
                icon = Icons.Default.Speed,
                title = "Liquid Glass optical fidelity (Φ)",
                value = "${fidelityPref.label} • ${fidelityTier.maxFps} fps target"
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                FidelityPreference.entries.forEach { pref ->
                    LiquidChip(
                        text = pref.label,
                        isActive = fidelityPref == pref,
                        onClick = { onFidelityChange(pref) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Live miniature preview widget inside the Settings Appearance header showing
 * the active surface brightness, card frost, and accent gradient in real time.
 */
@Composable
private fun LiveAppearanceMiniPreview(
    isDark: Boolean,
    primary: Color,
    secondary: Color
) {
    val bgTop = if (isDark) Color(0xFF0C101C) else Color(0xFFF3F7FE)
    val bgBottom = if (isDark) Color(0xFF151C2E) else Color(0xFFE4ECF9)
    val cardFill = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.90f)
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(primary, secondary)),
                RoundedCornerShape(14.dp)
            )
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(primary, secondary)))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(secondary)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(cardFill)
                    .border(
                        0.75.dp,
                        primary.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cardFill),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (idx == 0) primary else secondary.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

/**
 * Interactive pill in Settings for switching between all 10 themes with live
 * dual-swatch preview and instant mode synchronization.
 */
@Composable
private fun QuickThemePill(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val previewPalette = remember(theme) {
        (if (theme.isDark) DarkVennelaPalette else LightVennelaPalette).tinted(theme.toAccent())
    }
    val shape = RoundedCornerShape(16.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) theme.primaryAccent else palette.cardBorder.copy(alpha = 0.45f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "quickThemeBorder"
    )
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(
                            theme.primaryAccent.copy(alpha = if (palette.isDark) 0.28f else 0.16f),
                            theme.secondaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.12f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            palette.cardSurfaceStrong.copy(alpha = if (palette.isDark) 0.65f else 0.90f),
                            palette.cardSurface.copy(alpha = if (palette.isDark) 0.55f else 0.85f)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .pressScale(pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(previewPalette.backgroundTop, theme.primaryAccent, theme.secondaryAccent)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Column {
            Text(
                text = theme.title,
                color = palette.textPrimary,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = if (theme.isDark) "Dark" else "Light",
                color = if (isSelected) theme.primaryAccent else palette.textTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * One segment of the theme-mode control. Uses clean high-contrast pill surfaces
 * without stacked shadows so Light and Dark modes both render crisp and clear.
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

    val bgStart by animateColorAsState(
        targetValue = when {
            isSelected && palette.isDark -> accent.primary.copy(alpha = 0.85f)
            isSelected -> accent.primary
            else -> Color.Transparent
        },
        animationSpec = tween(LiquidMotion.durationFast),
        label = "segmentTint"
    )
    val bgEnd by animateColorAsState(
        targetValue = when {
            isSelected && palette.isDark -> accent.secondary.copy(alpha = 0.75f)
            isSelected -> accent.secondary.copy(alpha = 0.90f)
            else -> Color.Transparent
        },
        animationSpec = tween(LiquidMotion.durationFast),
        label = "segmentTintEnd"
    )
    val rim by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.55f) else Color.Transparent,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "segmentRim"
    )
    val foreground = if (isSelected) Color.White else palette.textSecondary

    Row(
        modifier = modifier
            .heightIn(min = VennelaTokens.touchTargetMin)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(bgStart, bgEnd)), shape)
            .border(width = if (isSelected) 1.dp else 0.dp, color = rim, shape = shape)
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
            tint = if (isSelected) Color.White else palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = mode.label,
            color = foreground,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
        targetValue = if (isSelected) 10f else 0f,
        animationSpec = LiquidMotion.hero(),
        label = "accentSwatchGlow"
    )
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (palette.isDark) Color.White else palette.textPrimary
        } else {
            palette.cardBorder
        },
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
                ambientColor = option.primary.copy(alpha = 0.50f),
                spotColor = option.secondary.copy(alpha = 0.50f)
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(option.primary, option.secondary)))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
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
private fun ThemeModeCycleButton(
    mode: ThemeMode,
    modifier: Modifier = Modifier,
    onCycle: () -> Unit
) {
    val accent = LocalVennelaAccent.current
    IconBadge(
        icon = themeModeIcon(mode),
        colors = listOf(accent.primary, accent.secondary),
        size = VennelaTokens.touchTargetMin,
        radius = VennelaTokens.radiusPill,
        contentDescription = "Theme mode: ${mode.label}. Tap to switch.",
        modifier = modifier.pressScale(pressedScale = 0.90f, onClick = onCycle)
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
                color = if (palette.isDark) accent.secondary else accent.primary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Shared building blocks                                                     */
/* ------------------------------------------------------------------------- */

/**
 * A settings group: one clean frosted panel holding rows and dividers.
 * Does NOT stack duplicate outer shadows on top of [GlassPanel], preventing
 * dark hardware shadow bleed in Light mode.
 */
@Composable
private fun SettingsGroup(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(index),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationCard,
        alpha = 0.56f
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
    IconBadge(
        icon = icon,
        colors = listOf(accent.primary, accent.secondary),
        size = VennelaTokens.badgeSm,
        radius = VennelaTokens.radiusChip,
        contentDescription = null
    )
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
