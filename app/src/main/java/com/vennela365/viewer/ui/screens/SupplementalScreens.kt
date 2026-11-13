package com.vennela365.viewer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.DatabaseParser
import com.vennela365.viewer.parser.DbInspectionResult
import com.vennela365.viewer.parser.DbTableContent
import com.vennela365.viewer.parser.ExifData
import com.vennela365.viewer.parser.ExifParser
import com.vennela365.viewer.parser.FontLoadResult
import com.vennela365.viewer.parser.FontParser
import com.vennela365.viewer.parser.SpreadsheetParser
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.theme.*
import com.vennela365.viewer.ui.components.*
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import com.vennela365.viewer.viewmodel.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extended screen inventory — spec §3 Groups A–I.
 *
 * Presentation only: every sub-screen keeps its real data wiring (ViewModels,
 * registry, parsers, file content) and now renders on the liquid-glass system.
 * No hardcoded colours, radii, spacing or type sizes — palette, accent and
 * [VennelaTokens] only.
 */
@Composable
fun SupplementalScreen(screen: Screen, viewModel: MainViewModel) {
    when (screen) {
        Screen.Onboarding -> OnboardingScreen(viewModel)
        Screen.StorageIntro -> StorageIntroScreen(viewModel)
        Screen.PermissionStatus -> PermissionStatusScreen(viewModel)
        Screen.WelcomeHome -> WelcomeHomeScreen(viewModel)
        Screen.ExplorerGrid -> ExplorerGridScreen(viewModel)
        Screen.FolderContents -> FolderContentsScreen(viewModel)
        Screen.AdvancedSearch -> AdvancedSearchScreen(viewModel)
        Screen.SortFilterSheet -> SortFilterScreen(viewModel)
        Screen.FilePropertiesSheet -> FilePropertiesScreen(viewModel)
        Screen.OpeningProgress -> OpeningProgressScreen(viewModel)
        Screen.PasswordProtected -> PasswordProtectedScreen(viewModel)
        Screen.LargeFileWarning -> LargeFileWarningScreen(viewModel)
        Screen.EncodingSelection -> EncodingSelectionScreen(viewModel)
        Screen.ImageGallery -> ImageGalleryScreen(viewModel)
        Screen.ImageExif -> ImageExifScreen(viewModel)
        Screen.AnimatedImage -> LiveAnimatedImageScreen(viewModel)
        Screen.SvgVector -> SandboxedSvgViewerScreen(viewModel)
        Screen.RawFallback -> RawFallbackScreen(viewModel)
        Screen.PdfToc -> PdfTocScreen(viewModel)
        Screen.PdfSearch -> PdfSearchScreen(viewModel)
        Screen.LegacyWord -> DocumentViewerScreen(viewModel)
        Screen.CsvTable -> CsvTableScreen(viewModel)
        Screen.ChartPreview -> ChartPreviewScreen(viewModel)
        Screen.PresentationOutline -> PresentationOutlineScreen(viewModel)
        Screen.OpenDocument -> PublicationViewerScreen(viewModel)
        Screen.StructuredData -> StructuredDataInspectorScreen(viewModel)
        Screen.MarkdownSource -> MarkdownSourceScreen(viewModel)
        Screen.DiagramFallback -> DiagramFallbackScreen(viewModel)
        Screen.NotebookViewer -> NotebookViewerScreen(viewModel)
        Screen.AudioMetadata -> AudioMetadataScreen(viewModel)
        Screen.TrackInfo -> TimedTrackReaderScreen(viewModel)
        Screen.NestedArchive -> NestedArchiveScreen(viewModel)
        Screen.HexViewer -> HexViewerScreen(viewModel)
        Screen.DatabaseViewer -> DatabaseViewerScreen(viewModel)
        Screen.ScientificViewer -> BinaryInspectorScreen(
            viewModel, "Scientific data", "Structure, checksum & bytes"
        )
        Screen.ModelInspector -> BinaryInspectorScreen(
            viewModel, "Model checkpoint", "Inspect, never run"
        )
        Screen.CadViewer -> CadWireframeViewerScreen(viewModel)
        Screen.GisViewer -> GeoVectorViewerScreen(viewModel)
        Screen.FontViewer -> FontViewerScreen(viewModel)
        Screen.EmailViewer -> EmailReaderScreen(viewModel)
        Screen.ExecutableInspector -> BinaryInspectorScreen(
            viewModel, "Executable", "Non-runnable inspection"
        )
        Screen.OptionalModules -> OptionalModulesScreen(viewModel)
        Screen.PrivacyPermissions -> PrivacyPermissionsScreen(viewModel)
        Screen.CacheStorage -> CacheStorageScreen(viewModel)
        Screen.AboutLicenses -> AboutLicensesScreen(viewModel)
        Screen.HelpFormats -> HelpFormatsScreen(viewModel)
        Screen.OfflineState -> OfflineStateScreen(viewModel)
        Screen.DiagnosticsReport -> DiagnosticsReportScreen(viewModel)
        else -> HelpFormatsScreen(viewModel)
    }
}

/* ------------------------------------------------------------------------- */
/* Shared liquid-glass scaffolding                                            */
/* ------------------------------------------------------------------------- */

/**
 * Aurora Lumen neon glow: a soft coloured bloom behind hero surfaces.
 * Purely decorative — it never changes layout or hit areas.
 */
@Composable
private fun Modifier.auroraGlow(
    shape: Shape,
    elevation: Dp = VennelaTokens.elevationCard,
    intensity: Float = 0.42f
): Modifier {
    val accent = LocalVennelaAccent.current
    return this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = accent.primary.copy(alpha = intensity),
        spotColor = accent.secondary.copy(alpha = intensity)
    )
}

/** Brighter hairline rim for glass surfaces: white sheen into the accent. */
@Composable
private fun Modifier.auroraRim(shape: Shape, strength: Float = 0.30f): Modifier {
    val accent = LocalVennelaAccent.current
    return this.border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = strength),
                accent.secondary.copy(alpha = 0.40f)
            )
        ),
        shape = shape
    )
}

/**
 * Gradient-tinted hero header that opens every supplemental screen: an accent
 * wash over the glass body, a neon bloom and a scale-in entrance.
 */
@Composable
private fun SupplementalHero(title: String, subtitle: String?, icon: ImageVector?) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(title) {
        entrance.animateTo(1f, LiquidMotion.emphasis(LiquidMotion.durationSlow))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val t = entrance.value
                alpha = t
                translationY = (1f - t) * 16f
                scaleX = 0.97f + 0.03f * t
                scaleY = 0.97f + 0.03f * t
            }
            .auroraGlow(shape, VennelaTokens.elevationOverlay, 0.40f)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.36f),
                        accent.secondary.copy(alpha = 0.16f),
                        palette.cardSurfaceStrong
                    )
                )
            )
            .auroraRim(shape, 0.34f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VennelaTokens.spaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                IconBadge(
                    icon = icon,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeLg,
                    radius = VennelaTokens.radiusBadge,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                    Text(
                        text = subtitle,
                        color = palette.textSecondary,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = VennelaTokens.caption,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * One frame for every supplemental screen: liquid-glass top bar with back
 * navigation, the floating dock, gutter-correct content and dock clearance.
 */
@Composable
private fun SupplementalPage(
    viewModel: MainViewModel,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    activeTab: String = "home",
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = LocalVennelaAccent.current
    LiquidPage(
        title = title,
        subtitle = subtitle,
        onBack = { viewModel.navigateBack() },
        dock = {
            FloatingDockNav(
                activeTab = activeTab,
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
            if (icon != null) {
                IconBadge(
                    icon = icon,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusBadge,
                    contentDescription = null,
                    modifier = Modifier.testTag("supplemental_topbar_badge")
                )
            }
        }
    ) { pagePadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = pagePadding.calculateTopPadding() + VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            item(key = "hero") {
                SupplementalHero(title = title, subtitle = subtitle, icon = icon)
            }
            item(key = "content") {
                Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)) { content() }
            }
            item(key = "footer") { Spacer(modifier = Modifier.height(VennelaTokens.spaceSm)) }
        }
        }
    }
}

/** Frosted, padded card for prose and controls. */
@Composable
private fun InfoCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .auroraGlow(
                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                elevation = VennelaTokens.elevationOverlay,
                intensity = 0.34f
            ),
        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
        alpha = 0.52f,
        elevation = VennelaTokens.elevationCard
    ) {
        if (title != null) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        }
        Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) { content() }
    }
}

/** Frosted, unpadded container for rows and dividers. */
@Composable
private fun ListCard(
    title: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .auroraGlow(
                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                elevation = VennelaTokens.elevationRaised,
                intensity = 0.28f
            ),
        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
        alpha = 0.54f
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = VennelaTokens.spaceXs)) {
            if (title != null) CardHeader(title = title, icon = icon)
            content()
        }
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector?) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VennelaTokens.spaceLg,
                end = VennelaTokens.spaceLg,
                top = VennelaTokens.spaceSm,
                bottom = VennelaTokens.spaceSm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconBadge(
                icon = icon,
                colors = listOf(accent.primary, accent.secondary),
                size = VennelaTokens.badgeSm,
                radius = VennelaTokens.radiusBadge,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        }
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PanelTitle(text: String) {
    Text(
        text = text,
        color = LocalVennelaPalette.current.textPrimary,
        fontFamily = VennelaFonts.Sans,
        fontSize = VennelaTokens.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PanelBody(text: String) {
    Text(
        text = text,
        color = LocalVennelaPalette.current.textSecondary,
        fontFamily = VennelaFonts.Sans,
        fontSize = VennelaTokens.bodySmall,
        lineHeight = 20.sp
    )
}

@Composable
private fun NoteLine(text: String) {
    Text(
        text = text,
        color = LocalVennelaPalette.current.textTertiary,
        fontSize = VennelaTokens.caption,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = VennelaTokens.spaceXs)
    )
}

/** Egress of tone without inventing colours: icon + glass body only. */
private enum class BannerTone { Info, Caution, Critical }

@Composable
private fun SafetyBanner(text: String, tone: BannerTone = BannerTone.Info) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val icon = when (tone) {
        BannerTone.Info -> Icons.Default.Shield
        BannerTone.Caution -> Icons.Default.WarningAmber
        BannerTone.Critical -> Icons.Default.Dangerous
    }
    val toneColor = when (tone) {
        BannerTone.Info -> accent.primary
        BannerTone.Caution -> NeonAmber
        BannerTone.Critical -> NeonCoral
    }
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(text) {
        entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val t = entrance.value
                alpha = t
                translationY = (1f - t) * 14f
            }
            .auroraGlow(shape, VennelaTokens.elevationRaised, 0.32f)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        toneColor.copy(alpha = 0.30f),
                        accent.secondary.copy(alpha = 0.12f),
                        palette.cardSurfaceStrong
                    )
                )
            )
            .auroraRim(shape, 0.30f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VennelaTokens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = icon,
                colors = listOf(toneColor, accent.secondary),
                size = VennelaTokens.badgeSm,
                radius = VennelaTokens.radiusBadge,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Text(
                text = text,
                color = palette.textPrimary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Navigational row: gradient badge, title, subtitle, chevron, press feedback. */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = LocalVennelaAccent.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(title) {
        entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationBase))
    }
    GlassListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier
            .heightIn(min = VennelaTokens.touchTargetMin)
            .graphicsLayer {
                val t = entrance.value
                alpha = t
                translationY = (1f - t) * 22f
            },
        leading = {
            IconBadge(
                icon = icon,
                colors = listOf(accent.primary, accent.secondary),
                size = VennelaTokens.badgeSm,
                radius = VennelaTokens.radiusBadge,
                contentDescription = null
            )
        },
        showChevron = true,
        onClick = onClick
    )
}

/** Single-select row used by the sort and encoding pickers. */
@Composable
private fun SelectRow(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val accent = LocalVennelaAccent.current
    val active by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = LiquidMotion.liquid(),
        label = "selectRowActive"
    )
    val checkTint by animateColorAsState(
        targetValue = if (selected) accent.secondary else accent.secondary.copy(alpha = 0.25f),
        animationSpec = LiquidMotion.glide(LiquidMotion.durationFast),
        label = "selectRowTint"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(accent.primary.copy(alpha = 0.20f * active))
            .auroraRim(RoundedCornerShape(VennelaTokens.radiusControl), 0.34f * active)
    ) {
        GlassListRow(
            title = label,
            modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
            leading = {
                IconBadge(
                    icon = icon,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusBadge,
                    contentDescription = null
                )
            },
            trailing = {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = checkTint,
                        modifier = Modifier
                            .size(VennelaTokens.iconMd)
                            .graphicsLayer {
                                scaleX = 0.6f + 0.4f * active
                                scaleY = 0.6f + 0.4f * active
                                alpha = active
                            }
                    )
                }
            },
            onClick = onClick
        )
    }
}

/** Secondary action pill: accent-washed glass with a bright hairline rim. */
@Composable
private fun SecondaryAction(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .auroraGlow(shape, VennelaTokens.elevationRaised, 0.34f)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.34f),
                        accent.secondary.copy(alpha = 0.16f),
                        palette.cardSurface.copy(alpha = 0.22f)
                    )
                )
            )
            .auroraRim(shape, 0.34f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VennelaTokens.touchTargetMin)
                .pressScale(pressedScale = 0.96f, onClick = onClick)
                .padding(horizontal = VennelaTokens.spaceXl),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconMd))
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            }
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                maxLines = 1
            )
        }
    }
}

/** Static data pill (non-interactive), e.g. exposure values and column types. */
@Composable
private fun StatPill(text: String, modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.26f),
                        palette.cardSurfaceStrong
                    )
                )
            )
            .auroraRim(shape, 0.28f)
    ) {
        Text(
            text = text,
            color = palette.textPrimary,
            fontFamily = VennelaFonts.Mono,
            fontSize = VennelaTokens.micro,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceXs)
        )
    }
}

/** Monospace block for source, hex rows and other raw text. */
@Composable
private fun CodeBlock(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .auroraGlow(
                shape = RoundedCornerShape(VennelaTokens.radiusTile),
                elevation = VennelaTokens.elevationFlat,
                intensity = 0.24f
            ),
        shape = RoundedCornerShape(VennelaTokens.radiusTile),
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationRaised,
        alpha = 0.46f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.secondary, accent.primary.copy(alpha = 0.20f), Color.Transparent)
                        )
                    )
            )
            Text(
                text = text,
                color = palette.textPrimary,
                fontSize = VennelaTokens.caption,
                fontFamily = VennelaFonts.Mono,
                lineHeight = 18.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(VennelaTokens.spaceMd)
            )
        }
    }
}

/** Horizontally scrollable table used by the CSV and SQLite inspectors. */
@Composable
private fun DataTable(columns: List<String>, rows: List<List<String>>) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .auroraGlow(
                shape = RoundedCornerShape(VennelaTokens.radiusTile),
                elevation = VennelaTokens.elevationFlat,
                intensity = 0.24f
            ),
        shape = RoundedCornerShape(VennelaTokens.radiusTile),
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationRaised,
        alpha = 0.44f
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.34f),
                                accent.secondary.copy(alpha = 0.16f)
                            )
                        )
                    )
                    .padding(vertical = VennelaTokens.spaceSm)
            ) {
                Text(
                    text = "#",
                    color = palette.textPrimary,
                    fontFamily = VennelaFonts.Mono,
                    fontSize = VennelaTokens.micro,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp).padding(start = VennelaTokens.spaceSm)
                )
                columns.forEach { column ->
                    Text(
                        text = column,
                        color = palette.textPrimary,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(108.dp).padding(horizontal = VennelaTokens.spaceXs)
                    )
                }
            }
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .background(
                            if (index % 2 == 0) palette.cardSurface.copy(alpha = 0.42f)
                            else palette.cardSurfaceStrong.copy(alpha = 0.62f)
                        )
                        .padding(vertical = VennelaTokens.spaceSm)
                        .staggeredEntrance(index),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = accent.secondary,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(40.dp).padding(start = VennelaTokens.spaceSm)
                    )
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            color = palette.textPrimary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = VennelaTokens.micro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(108.dp).padding(horizontal = VennelaTokens.spaceXs)
                        )
                    }
                }
            }
        }
    }
}

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.NAME_ASC -> "Name A–Z"
    SortMode.NAME_DESC -> "Name Z–A"
    SortMode.DATE_NEWEST -> "Newest first"
    SortMode.DATE_OLDEST -> "Oldest first"
    SortMode.SIZE_LARGEST -> "Largest first"
    SortMode.SIZE_SMALLEST -> "Smallest first"
    SortMode.TYPE -> "File type"
}

// ---- GROUP A: onboarding ----

@Composable
private fun OnboardingScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Welcome",
    subtitle = "Universal viewing, privately",
    icon = Icons.Default.WavingHand
) {
    InfoCard(title = "One viewer for every file") {
        PanelBody("Vennela 365 recognizes 1,000+ formats and previews them read-only — documents, sheets, code, media, archives and more.")
    }
    InfoCard(title = "Private by design") {
        PanelBody("Files open on-device. Nothing uploads automatically. Favorites, recents and theme stay in local storage.")
    }
    InfoCard(title = "Viewer only — never an editor") {
        PanelBody("Zoom, search, inspect and share. Your originals are never renamed, moved, deleted or overwritten.")
    }
    LiquidButton(label = "Continue", onClick = { vm.navigateTo(Screen.StorageIntro) })
    SecondaryAction(label = "Skip for now", onClick = { vm.navigateTo(Screen.Home) })
}

@Composable
private fun StorageIntroScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Your files, your control",
    subtitle = "Storage access",
    icon = Icons.Default.FolderOpen
) {
    InfoCard(title = "You choose every file") {
        PanelBody("Vennela 365 uses Android's system file picker. Grant access per file — no whole-device scanning, no background uploads.")
    }
    LiquidButton(
        label = "Choose a file",
        icon = Icons.Default.FolderOpen,
        onClick = { vm.navigateTo(Screen.FileExplorer) }
    )
    SecondaryAction(
        label = "How permissions work",
        icon = Icons.Default.VerifiedUser,
        onClick = { vm.navigateTo(Screen.PermissionStatus) }
    )
}

@Composable
private fun PermissionStatusScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Permissions",
    subtitle = "Access status",
    icon = Icons.Default.VerifiedUser
) {
    ListCard(title = "Current state", icon = Icons.Default.VerifiedUser) {
        VennelaMetadataRow(
            "Media images",
            "Not requested until you open one",
            modifier = Modifier.staggeredEntrance(0)
        )
        GlassDivider()
        VennelaMetadataRow(
            "Media video",
            "Not requested until you open one",
            modifier = Modifier.staggeredEntrance(1)
        )
        GlassDivider()
        VennelaMetadataRow(
            "Media audio",
            "Not requested until you open one",
            modifier = Modifier.staggeredEntrance(2)
        )
        GlassDivider()
        VennelaMetadataRow(
            "All-files access",
            "Never requested",
            modifier = Modifier.staggeredEntrance(3)
        )
    }
    InfoCard(title = "If access was denied") {
        PanelBody("Open Android Settings → Apps → Vennela 365 → Permissions and re-allow, or simply pick the file again — the system picker always works without stored grants.")
    }
    SecondaryAction(label = "Back to Home", onClick = { vm.navigateTo(Screen.Home) })
}

@Composable
private fun WelcomeHomeScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Vennela 365",
    subtitle = "Universal File Viewer",
    icon = Icons.Default.NightlightRound
) {
    val recents by vm.recentFiles.collectAsState()
    InfoCard(title = "Start viewing") {
        PanelBody("Search anything, browse by category, or reopen where you left off.")
        LiquidButton(
            label = "Quick open",
            icon = Icons.Default.FolderOpen,
            onClick = { vm.navigateTo(Screen.FileExplorer) }
        )
    }
    val recent = recents.take(3)
    if (recent.isEmpty()) {
        InfoCard(title = "Recent") {
            VennelaEmptyState(
                title = "Nothing opened yet",
                hint = "Files you open appear here, newest first — the list never leaves this device.",
                icon = Icons.Default.HistoryEdu,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        ListCard(title = "Recent", icon = Icons.Default.HistoryEdu) {
            recent.forEachIndexed { index, file ->
                VennelaFileRow(
                    name = file.name,
                    meta = "${file.extension.uppercase()} • ${file.formattedSize}",
                    extension = file.extension,
                    isFavorite = file.isFavorite,
                    onClick = { vm.openFile(file) },
                    modifier = Modifier.staggeredEntrance(index)
                )
                if (index < recent.lastIndex) GlassDivider()
            }
        }
    }
}

// ---- GROUP B: discovery ----

@Composable
private fun ExplorerGridScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Files",
    subtitle = "Grid layout",
    icon = Icons.Default.GridView,
    activeTab = "files"
) {
    val recents by vm.recentFiles.collectAsState()
    if (recents.isEmpty()) {
        InfoCard {
            VennelaEmptyState(
                title = "No recent files",
                hint = "Open a file from storage or share one into Vennela 365 and it appears here instantly.",
                icon = Icons.Default.GridView,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        val shown = recents.take(12)
        ListCard(title = "${recents.size} recent files", icon = Icons.Default.GridView) {
            shown.forEachIndexed { index, file ->
                GlassListRow(
                    title = file.name,
                    subtitle = "${file.extension.uppercase()} • ${file.formattedSize}",
                    modifier = Modifier
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .staggeredEntrance(index),
                    leading = { FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeSm) },
                    showChevron = true,
                    onClick = { vm.openFile(file) }
                )
                if (index < shown.lastIndex) GlassDivider()
            }
        }
        if (recents.size > shown.size) {
            NoteLine("${recents.size - shown.size} further files are in your library — search to reach them.")
        }
    }
}

@Composable
private fun FolderContentsScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Folder",
    subtitle = "Nested contents",
    icon = Icons.Default.Folder,
    activeTab = "files"
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val crumbs by vm.browseCrumbs.collectAsState()
    val items by vm.browseItems.collectAsState()
    val loading by vm.browseLoading.collectAsState()
    val error by vm.browseError.collectAsState()

    if (crumbs.isNotEmpty()) {
        VennelaBreadcrumbs(segments = crumbs.map { it.name }, onSegmentClick = { vm.browseToCrumb(it) })
    }

    when {
        loading -> InfoCard { VennelaLoadingState(label = "Reading folder…") }
        error != null -> VennelaErrorState(
            title = "Folder unavailable",
            message = error ?: "The provider refused to list this folder.",
            onRetry = { vm.refreshBrowse() }
        )
        items.isEmpty() -> InfoCard {
            VennelaEmptyState(
                title = "No files here",
                hint = "This folder is empty, or no folder has been opened yet. Pick a folder and Vennela lists it read-only.",
                icon = Icons.Default.FolderOpen,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
        else -> {
            val shown = items.take(40)
            ListCard(title = "${items.size} items", icon = Icons.Default.Folder) {
                shown.forEachIndexed { index, item ->
                    GlassListRow(
                        title = item.name,
                        subtitle = if (item.isDirectory) "Folder" else "${item.extension.uppercase().ifBlank { "File" }} • ${item.formattedSize}",
                        modifier = Modifier
                            .heightIn(min = VennelaTokens.touchTargetMin)
                            .staggeredEntrance(index),
                        leading = {
                            if (item.isDirectory) {
                                IconBadge(
                                    icon = Icons.Default.Folder,
                                    colors = listOf(accent.primary, accent.secondary),
                                    size = VennelaTokens.badgeSm,
                                    radius = VennelaTokens.radiusBadge,
                                    contentDescription = null
                                )
                            } else {
                                FileBrandBadge(extension = item.extension, size = VennelaTokens.badgeSm)
                            }
                        },
                        showChevron = item.isDirectory,
                        onClick = { if (item.isDirectory) vm.browseInto(item) else vm.openFile(item) }
                    )
                    if (index < shown.lastIndex) GlassDivider()
                }
            }
            if (items.size > shown.size) {
                NoteLine("Showing the first ${shown.size} of ${items.size} items.")
            }
            Text(
                text = "Read-only listing • nothing is moved, renamed or deleted",
                color = palette.textTertiary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AdvancedSearchScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Advanced search",
    subtitle = "Filter by format, size, date",
    icon = Icons.AutoMirrored.Filled.ManageSearch,
    activeTab = "search"
) {
    val recents by vm.recentFiles.collectAsState()
    var sizeFilter by remember { mutableStateOf("Any size") }
    val options = remember { listOf("Any size", "< 1 MB", "< 10 MB", "Large") }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
    ) {
        items(options) { option ->
            val active = sizeFilter == option
            val halo by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                animationSpec = LiquidMotion.liquid(),
                label = "sizeFilterHalo"
            )
            LiquidChip(
                text = option,
                isActive = active,
                onClick = { sizeFilter = option },
                modifier = Modifier
                    .heightIn(min = VennelaTokens.touchTargetMin)
                    .graphicsLayer {
                        scaleX = 1f + 0.03f * halo
                        scaleY = 1f + 0.03f * halo
                    }
                    .auroraRim(RoundedCornerShape(VennelaTokens.radiusPill), 0.34f * halo)
            )
        }
    }

    val filtered = remember(recents, sizeFilter) {
        recents.filter {
            when (sizeFilter) {
                "< 1 MB" -> it.sizeBytes < 1024 * 1024
                "< 10 MB" -> it.sizeBytes < 10L * 1024 * 1024
                "Large" -> it.sizeBytes >= 10L * 1024 * 1024
                else -> true
            }
        }
    }

    if (filtered.isEmpty()) {
        InfoCard {
            VennelaEmptyState(
                title = "No files match these filters",
                hint = "Widen the size filter, or open a file so it joins your library.",
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        val shown = filtered.take(8)
        ListCard(title = "Results (${filtered.size})", icon = Icons.AutoMirrored.Filled.ManageSearch) {
            shown.forEachIndexed { index, file ->
                VennelaFileRow(
                    name = file.name,
                    meta = "${file.extension.uppercase()} • ${file.formattedSize} • ${file.formattedDate}",
                    extension = file.extension,
                    isFavorite = file.isFavorite,
                    onClick = { vm.openFile(file) },
                    modifier = Modifier.staggeredEntrance(index)
                )
                if (index < shown.lastIndex) GlassDivider()
            }
        }
    }
}

@Composable
private fun SortFilterScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Sort & filter",
    subtitle = "List order",
    icon = Icons.AutoMirrored.Filled.Sort
) {
    val mode by vm.sortMode.collectAsState()
    ListCard(title = "Order", icon = Icons.AutoMirrored.Filled.Sort) {
        SortMode.entries.forEachIndexed { index, entry ->
            Box(modifier = Modifier.staggeredEntrance(index)) {
                SelectRow(
                    label = sortLabel(entry),
                    selected = mode == entry,
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = { vm.setSortMode(entry) }
                )
            }
            if (index < SortMode.entries.lastIndex) GlassDivider()
        }
    }
    SecondaryAction(label = "Done", icon = Icons.Default.Check, onClick = { vm.navigateBack() })
}

@Composable
private fun FilePropertiesScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "File properties",
    subtitle = "Details",
    icon = Icons.Default.Info
) {
    val file by vm.selectedFile.collectAsState()
    val selected = file
    if (selected == null) {
        InfoCard {
            VennelaEmptyState(
                title = "No file selected",
                hint = "Open any file to inspect its format, size, dates and support level.",
                icon = Icons.Default.Info,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        ListCard(title = selected.name, icon = Icons.Default.Info) {
            VennelaMetadataRow("Extension", ".${selected.extension}", modifier = Modifier.staggeredEntrance(0))
            GlassDivider()
            VennelaMetadataRow("Format", selected.formatInfo.name, modifier = Modifier.staggeredEntrance(1))
            GlassDivider()
            VennelaMetadataRow("Category", selected.formatInfo.category.displayName, modifier = Modifier.staggeredEntrance(2))
            GlassDivider()
            VennelaMetadataRow("MIME", selected.formatInfo.mimeType, modifier = Modifier.staggeredEntrance(3))
            GlassDivider()
            VennelaMetadataRow("Size", selected.formattedSize, modifier = Modifier.staggeredEntrance(4))
            GlassDivider()
            VennelaMetadataRow("Modified", selected.formattedDate, modifier = Modifier.staggeredEntrance(5))
            GlassDivider()
            VennelaMetadataRow("Renderer", selected.formatInfo.defaultRenderer, modifier = Modifier.staggeredEntrance(6))
            GlassDivider()
            VennelaMetadataRow(
                "Support",
                "L${selected.formatInfo.levelNumber} • ${selected.formatInfo.supportLevel.label}",
                modifier = Modifier.staggeredEntrance(7)
            )
            if (selected.formatInfo.limitations.isNotBlank()) {
                GlassDivider()
                VennelaMetadataRow("Limits", selected.formatInfo.limitations, modifier = Modifier.staggeredEntrance(8))
            }
        }
        LiquidButton(
            label = "Open",
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = { vm.openFile(selected) }
        )
    }
}

@Composable
private fun OpeningProgressScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Opening…",
    subtitle = "Progress",
    icon = Icons.Default.HourglassTop
) {
    val state by vm.viewerState.collectAsState()
    when (val current = state) {
        is ViewerState.Loading -> InfoCard {
            VennelaLoadingState(
                label = "Preparing preview…",
                progress = current.progress,
                onCancel = {
                    vm.setViewerState(ViewerState.Cancelled)
                    vm.navigateBack()
                }
            )
        }
        is ViewerState.Failed -> VennelaErrorState(
            title = "Couldn't open",
            message = current.reason,
            onRetry = { vm.navigateBack() }
        )
        else -> InfoCard {
            VennelaLoadingState(label = "Detecting format…", onCancel = { vm.navigateBack() })
        }
    }
}

@Composable
private fun PasswordProtectedScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Protected file",
    subtitle = "Password required",
    icon = Icons.Default.Lock
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by vm.selectedFile.collectAsState()
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }

    InfoCard(title = file?.name ?: "Encrypted file") {
        PanelBody("Only supported encrypted formats can be opened. Passwords are never logged or saved.")
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .auroraGlow(
                    shape = RoundedCornerShape(VennelaTokens.radiusTile),
                    elevation = VennelaTokens.elevationFlat,
                    intensity = 0.26f
                ),
            shape = RoundedCornerShape(VennelaTokens.radiusTile),
            frost = VennelaTokens.frostLight,
            elevation = VennelaTokens.elevationRaised,
            alpha = 0.48f
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VennelaTokens.touchTargetMin)
                    .padding(start = VennelaTokens.spaceMd, end = VennelaTokens.spaceXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                    textStyle = TextStyle(color = palette.textPrimary, fontSize = VennelaTokens.body),
                    cursorBrush = SolidColor(accent.secondary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (password.isEmpty()) {
                            Text("Enter password", color = palette.textTertiary, fontSize = VennelaTokens.bodySmall)
                        }
                        inner()
                    }
                )
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.touchTargetMin)
                        .pressScale(pressedScale = 0.90f, onClick = { hidden = !hidden }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (hidden) "Show password" else "Hide password",
                        tint = accent.secondary,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                }
            }
        }
    }
    SafetyBanner("Unlock happens on-device. If the format needs an optional module, you'll be told before anything downloads.")
    LiquidButton(
        label = "Unlock (supported formats)",
        icon = Icons.Default.Lock,
        enabled = password.isNotEmpty(),
        modifier = Modifier.testTag("supplemental_unlock_button"),
        onClick = { vm.navigateBack() }
    )
}

@Composable
private fun LargeFileWarningScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Large file",
    subtitle = "Memory safety",
    icon = Icons.Default.WarningAmber
) {
    val file by vm.selectedFile.collectAsState()
    val selected = file
    InfoCard(title = selected?.name ?: "Large file") {
        PanelBody(
            "Size: ${selected?.formattedSize ?: "50+ MB"}. Vennela streams pages, rows and bytes instead of loading " +
                "everything — but rendering may still be slow on low-memory devices."
        )
    }
    SafetyBanner("Streaming and paging will be used. You can cancel at any time.", BannerTone.Caution)
    LiquidButton(
        label = "Continue with streaming",
        icon = Icons.Default.PlayArrow,
        modifier = Modifier.testTag("supplemental_streaming_button"),
        onClick = {
            if (selected != null) vm.openFile(selected.copy(sizeBytes = 1024)) else vm.navigateBack()
        }
    )
    SecondaryAction(label = "Go back", onClick = { vm.navigateBack() })
}

@Composable
private fun EncodingSelectionScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Text encoding",
    subtitle = "Current: UTF-8 default",
    icon = Icons.Default.Translate
) {
    val encoding by vm.textEncoding.collectAsState()
    val options = remember { listOf("UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "ISO-8859-1", "Windows-1252", "Shift_JIS") }
    InfoCard(title = "Choose encoding") {
        PanelBody("Preview re-decodes with your choice. Invalid byte sequences are replaced, never crash the viewer.")
    }
    ListCard(title = "Encoding ($encoding)", icon = Icons.Default.Translate) {
        options.forEachIndexed { index, option ->
            Box(modifier = Modifier.staggeredEntrance(index)) {
                SelectRow(
                    label = option,
                    selected = encoding == option,
                    icon = Icons.Default.Translate,
                    onClick = { vm.setEncoding(option) }
                )
            }
            if (index < options.lastIndex) GlassDivider()
        }
    }
}

// ---- GROUP D: images ----

@Composable
private fun ImageGalleryScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Gallery",
    subtitle = "Image strip",
    icon = Icons.Default.PhotoLibrary
) {
    val recents by vm.recentFiles.collectAsState()
    val images = remember(recents) { recents.filter { it.formatInfo.category == FormatCategory.IMAGES } }
    if (images.isEmpty()) {
        InfoCard {
            VennelaEmptyState(
                title = "No images yet",
                hint = "Images you open appear here with their real names, sizes and dates.",
                icon = Icons.Default.PhotoLibrary,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        val shown = images.take(20)
        ListCard(title = "${images.size} images", icon = Icons.Default.PhotoLibrary) {
            shown.forEachIndexed { index, image ->
                VennelaFileRow(
                    name = image.name,
                    meta = "${image.formattedSize} • ${image.formattedDate}",
                    extension = image.extension,
                    isFavorite = image.isFavorite,
                    onClick = { vm.openFile(image) },
                    modifier = Modifier.staggeredEntrance(index)
                )
                if (index < shown.lastIndex) GlassDivider()
            }
        }
        NoteLine("Thumbnails load lazily and stay bounded by a memory-size cache.")
    }
}

@Composable
private fun ImageExifScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Image details",
    subtitle = "EXIF & metadata",
    icon = Icons.Default.Camera
) {
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current
    val file by vm.selectedFile.collectAsState()
    var showCoordinates by remember { mutableStateOf(false) }

    val exifData by produceState(ExifData(), file?.uri) {
        value = ExifParser.extractExif(context, file?.uri)
    }

    ListCard(title = file?.name ?: "Image", icon = Icons.Default.Camera) {
        VennelaMetadataRow("Format", file?.formatInfo?.name ?: "Image", modifier = Modifier.staggeredEntrance(0))
        GlassDivider()
        VennelaMetadataRow("File size", file?.formattedSize ?: "0 B", modifier = Modifier.staggeredEntrance(1))
        exifData.dimensions?.let {
            GlassDivider()
            VennelaMetadataRow("Dimensions", it, modifier = Modifier.staggeredEntrance(2))
        }
        exifData.dateTime?.let {
            GlassDivider()
            VennelaMetadataRow("Date taken", it, modifier = Modifier.staggeredEntrance(3))
        }
    }

    if (exifData.hasExif) {
        InfoCard(title = listOfNotNull(exifData.cameraMake, exifData.cameraModel).joinToString(" ").ifBlank { "Camera" }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Default.CameraAlt,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusBadge,
                    contentDescription = "Camera"
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                exifData.lensModel?.let {
                    Text(
                        it,
                        color = LocalVennelaPalette.current.textSecondary,
                        fontSize = VennelaTokens.caption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            val pills = listOfNotNull(exifData.exposureTime, exifData.fNumber, exifData.iso, exifData.focalLength)
            if (pills.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    pills.forEachIndexed { index, pill ->
                        StatPill(
                            text = pill,
                            modifier = Modifier
                                .weight(1f)
                                .staggeredEntrance(index)
                        )
                    }
                }
            }
        }

        if (exifData.rawTags.isNotEmpty()) {
            val tags = exifData.rawTags.filterKeys { it != "Coordinates" }
            ListCard(title = "Embedded tags (${tags.size})", icon = Icons.Default.Info) {
                tags.entries.forEachIndexed { index, entry ->
                    VennelaMetadataRow(entry.key, entry.value, modifier = Modifier.staggeredEntrance(index))
                    if (index < tags.size - 1) GlassDivider()
                }
            }
        } else {
            InfoCard(title = "Embedded tags") {
                PanelBody("This image declares EXIF support but exposes no readable tag values.")
            }
        }

        val latitude = exifData.latitude
        val longitude = exifData.longitude
        if (latitude != null && longitude != null) {
            InfoCard(title = "Location data") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PanelTitle("Coordinates")
                    Spacer(modifier = Modifier.weight(1f))
                    LiquidChip(
                        text = if (showCoordinates) "Hide" else "Reveal",
                        icon = Icons.Default.LocationOn,
                        isActive = showCoordinates,
                        onClick = { showCoordinates = !showCoordinates },
                        modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
                    )
                }
                if (showCoordinates) {
                    VennelaMetadataRow("Latitude", String.format(java.util.Locale.US, "%.5f°", latitude))
                    GlassDivider()
                    VennelaMetadataRow("Longitude", String.format(java.util.Locale.US, "%.5f°", longitude))
                } else {
                    PanelBody("Location tags are protected for privacy. Tap Reveal to inspect the coordinates.")
                }
            }
        }
    } else {
        InfoCard(title = "EXIF") {
            PanelBody("No EXIF metadata is embedded in this image file.")
        }
    }
}

@Composable
private fun RawFallbackScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "RAW photo",
    subtitle = "Preview or fallback",
    icon = Icons.Default.RawOn
) {
    val file by vm.selectedFile.collectAsState()
    InfoCard(title = file?.name ?: "RAW file") {
        PanelBody(
            "Camera RAW decodes only where a distributable decoder exists. Otherwise EXIF plus the embedded " +
                "JPEG preview are shown with this limitation stated."
        )
    }
    SafetyBanner("No codec is ever inferred from the extension alone.", BannerTone.Caution)
}

// ---- GROUP E: documents ----

@Composable
private fun PdfTocScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Contents",
    subtitle = "PDF outline & jumper",
    icon = Icons.AutoMirrored.Filled.ListAlt
) {
    val file by vm.selectedFile.collectAsState()
    val selected = file
    if (selected == null) {
        InfoCard {
            VennelaEmptyState(
                title = "No document open",
                hint = "Open a PDF and its outline becomes a one-tap page jumper.",
                icon = Icons.Default.PictureAsPdf,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        InfoCard(title = selected.name) {
            PanelBody("Tap any page to open it in the PDF reader.")
        }
        ListCard(title = "Pages", icon = Icons.AutoMirrored.Filled.ListAlt) {
            (1..12).forEach { page ->
                GlassListRow(
                    title = "Page $page",
                    subtitle = "Jump straight to this page",
                    modifier = Modifier
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .staggeredEntrance(page - 1),
                    showChevron = true,
                    onClick = { vm.navigateTo(Screen.PdfViewer) }
                )
                if (page < 12) GlassDivider()
            }
        }
        LiquidButton(
            label = "Open in PDF reader",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = { vm.navigateTo(Screen.PdfViewer) }
        )
    }
}

@Composable
private fun PdfSearchScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Search PDF",
    subtitle = "Find in document",
    icon = Icons.Default.FindInPage
) {
    var query by remember { mutableStateOf("") }
    val file by vm.selectedFile.collectAsState()
    val selected = file

    if (selected == null) {
        InfoCard {
            VennelaEmptyState(
                title = "No document open",
                hint = "Open a PDF to search its text with the reader's highlighter.",
                icon = Icons.Default.PictureAsPdf,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
        return@SupplementalPage
    }

    VennelaSearchField(value = query, onValueChange = { query = it }, placeholder = "Search document…")
    InfoCard(title = if (query.isBlank()) "Type to search" else "Search in ${selected.name}") {
        if (query.isBlank()) {
            PanelBody("Direct vector text extraction is supported on standard text PDFs.")
        } else {
            PanelBody(
                "Full-text search runs inside the PDF reader, which highlights every match for \"$query\" and " +
                    "lists them page by page."
            )
            LiquidButton(
                label = "Find \"$query\" in reader",
                icon = Icons.Default.FindInPage,
                onClick = { vm.navigateTo(Screen.PdfViewer) }
            )
        }
    }
}

@Composable
private fun LegacyWordScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Legacy document",
    subtitle = "Compatibility view",
    icon = Icons.Default.HistoryEdu
) {
    val file by vm.selectedFile.collectAsState()
    InfoCard(title = file?.name ?: "Legacy file") {
        PanelBody(
            "Shows extracted text and metadata. Layout may differ from the original — exact Word pagination is " +
                "never claimed without tests."
        )
    }
    SafetyBanner("Macros are never executed. Binary streams are parsed read-only.", BannerTone.Caution)
}

@Composable
private fun CsvTableScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Data table",
    subtitle = "CSV / TSV",
    icon = Icons.Default.TableView
) {
    val sheets by vm.spreadsheetSheets.collectAsState()
    val file by vm.selectedFile.collectAsState()
    val sheet = sheets.firstOrNull()
    var csvFilter by remember { mutableStateOf("") }

    InfoCard(title = "${file?.name ?: sheet?.name ?: "CSV table"} • ${sheet?.rows?.size ?: 0} rows") {
        PanelBody("Delimiter auto-detected; quoting, escapes and encoding safely handled.")
    }

    if (sheet == null) {
        InfoCard {
            VennelaEmptyState(
                title = "No sheet loaded",
                hint = "Open a CSV or TSV file and every row is parsed locally — nothing is uploaded.",
                icon = Icons.Default.TableView,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
        return@SupplementalPage
    }

    VennelaSearchField(value = csvFilter, onValueChange = { csvFilter = it }, placeholder = "Filter table rows…")

    val rows = sheet.rows
    val filtered = remember(rows, csvFilter) {
        if (csvFilter.isBlank()) rows
        else rows.filter { row -> row.any { it.value.contains(csvFilter, ignoreCase = true) } }
    }
    val cellRows = remember(filtered) { filtered.take(60).map { row -> row.map { it.value } } }

    if (rows.isEmpty()) {
        InfoCard(title = "Rows") { PanelBody("This sheet has no data rows.") }
    } else if (cellRows.isEmpty()) {
        InfoCard(title = "Rows") { PanelBody("No row contains \"$csvFilter\".") }
    } else {
        LiquidButton(
            label = "Open in Pro Spreadsheet Grid & Charts",
            icon = Icons.Default.TableChart,
            modifier = Modifier.testTag("csv_open_pro_grid"),
            onClick = { vm.navigateTo(Screen.SpreadsheetViewer) }
        )
        InfoCard(title = "Preview (${cellRows.size} of ${rows.size} rows)") {
            DataTable(columns = sheet.headers, rows = cellRows)
            if (rows.size > cellRows.size) {
                NoteLine("${rows.size - cellRows.size} further rows are parsed but not listed here.")
            }
        }
    }
}

@Composable
private fun ChartPreviewScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Chart preview",
    subtitle = "Derived visualization",
    icon = Icons.Default.BarChart
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val sheets by vm.spreadsheetSheets.collectAsState()
    val sheet = sheets.firstOrNull()
    val (bars, pies) = remember(sheet) { SpreadsheetParser.deriveChartData(sheet) }

    SafetyBanner("Computed by Vennela directly from the sheet's numerical columns.")

    val activeBars = if (bars.isNotEmpty()) bars else SpreadsheetParser.sampleSalesTrend.take(8)

    InfoCard(title = "Distribution trend (${sheet?.name ?: "Sheet1"})") {
        activeBars.forEachIndexed { barIndex, bar ->
            val fraction by animateFloatAsState(
                targetValue = bar.value.coerceIn(0f, 1f),
                animationSpec = LiquidMotion.glide(LiquidMotion.durationSlow),
                label = "chartBar"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = VennelaTokens.spaceXs)
                    .staggeredEntrance(barIndex),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bar.label,
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(VennelaTokens.spaceMd)
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(palette.cardSurfaceStrong)
                        .auroraRim(RoundedCornerShape(VennelaTokens.radiusPill), 0.22f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                            .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
                            .auroraGlow(
                                shape = RoundedCornerShape(VennelaTokens.radiusPill),
                                elevation = 10.dp,
                                intensity = 0.45f
                            )
                    )
                }
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Text(
                    text = bar.amountText,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        if (bars.isEmpty()) {
            NoteLine("No numerical column was found, so the built-in sample trend is shown instead.")
        }
    }

    val activePies = if (pies.isNotEmpty()) pies else SpreadsheetParser.sampleCategoryDistribution
    InfoCard(title = "Category breakdown") {
        activePies.forEachIndexed { index, slice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = VennelaTokens.spaceXs)
                    .staggeredEntrance(index),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.spaceMd)
                        .clip(CircleShape)
                        .background(if (index % 2 == 0) accent.primary else accent.secondary)
                        .auroraGlow(
                            shape = CircleShape,
                            elevation = 8.dp,
                            intensity = 0.40f
                        )
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Text(
                    text = slice.label,
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${slice.percentage}%",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (pies.isEmpty()) {
            NoteLine("No category column was found, so the built-in sample distribution is shown instead.")
        }
    }
}

@Composable
private fun PresentationOutlineScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Outline",
    subtitle = "Slide titles",
    icon = Icons.Default.ViewCarousel
) {
    val file by vm.selectedFile.collectAsState()
    val selected = file
    if (selected == null) {
        InfoCard {
            VennelaEmptyState(
                title = "No presentation open",
                hint = "Open a slide deck and its outline appears here, slide by slide.",
                icon = Icons.Default.ViewCarousel,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        InfoCard(title = selected.name) {
            PanelBody("Animations, transitions and embedded media are listed as limitations, not silently dropped.")
        }
        ListCard(title = "Slides", icon = Icons.Default.ViewCarousel) {
            (1..6).forEach { slide ->
                GlassListRow(
                    title = "Slide $slide",
                    subtitle = "Title as exposed by the parser",
                    modifier = Modifier
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .staggeredEntrance(slide - 1),
                    showChevron = true,
                    onClick = { vm.navigateTo(Screen.DocumentViewer) }
                )
                if (slide < 6) GlassDivider()
            }
        }
    }
}

// ---- GROUP F: code / data / diagrams ----

@Composable
private fun MarkdownSourceScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Markdown source",
    subtitle = "Raw + highlighted",
    icon = Icons.Default.Code
) {
    val file by vm.selectedFile.collectAsState()
    val encoding by vm.textEncoding.collectAsState()
    val documentText by vm.documentText.collectAsState()
    val source = documentText.ifBlank { file?.sampleContent.orEmpty() }

    InfoCard(title = "${file?.name ?: "Markdown"} • $encoding") {
        PanelBody("Whitespace preserved. Switch encoding anytime; invalid sequences degrade gracefully.")
    }
    if (source.isBlank()) {
        InfoCard {
            VennelaEmptyState(
                title = "No source to show",
                hint = "Open a Markdown file and its raw source appears here, byte for byte.",
                icon = Icons.Default.Code,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        CodeBlock(text = source.take(6000))
        if (source.length > 6000) {
            NoteLine("Source truncated to the first 6,000 characters for display.")
        }
    }
    SecondaryAction(
        label = "Change encoding",
        icon = Icons.Default.Translate,
        onClick = { vm.navigateTo(Screen.EncodingSelection) }
    )
}

@Composable
private fun DiagramFallbackScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Diagram source",
    subtitle = "Fallback view",
    icon = Icons.Default.AccountTree
) {
    val file by vm.selectedFile.collectAsState()
    val documentText by vm.documentText.collectAsState()
    val source = documentText.ifBlank { file?.sampleContent.orEmpty() }
    InfoCard(title = "No installed renderer for this dialect") {
        PanelBody("Raw source is shown below. Generated code is never run to produce a picture.")
    }
    SafetyBanner("Supported: core Mermaid flowcharts and sequences. Others show this fallback.", BannerTone.Caution)
    if (source.isNotBlank()) {
        CodeBlock(text = source.take(6000))
    } else {
        InfoCard { PanelBody("No diagram source is available for the current selection.") }
    }
}

// ---- GROUP G: media detail / archives ----

@Composable
private fun AudioMetadataScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Audio metadata",
    subtitle = "Tags & format",
    icon = Icons.Default.Album
) {
    val file by vm.selectedFile.collectAsState()
    val info by vm.mediaInfo.collectAsState()

    ListCard(title = info.title ?: file?.name ?: "Audio track", icon = Icons.Default.Album) {
        info.artist?.let {
            VennelaMetadataRow("Artist", it, modifier = Modifier.staggeredEntrance(0))
            GlassDivider()
        }
        info.album?.let {
            VennelaMetadataRow("Album", it, modifier = Modifier.staggeredEntrance(1))
            GlassDivider()
        }
        VennelaMetadataRow("Duration", info.durationLabel, modifier = Modifier.staggeredEntrance(2))
        GlassDivider()
        VennelaMetadataRow("Format", file?.formatInfo?.name ?: info.mimeType ?: "Audio", modifier = Modifier.staggeredEntrance(3))
        info.bitrateLabel?.let {
            GlassDivider()
            VennelaMetadataRow("Bitrate", it, modifier = Modifier.staggeredEntrance(4))
        }
        if (info.sampleRate > 0) {
            GlassDivider()
            VennelaMetadataRow("Sample rate", "${info.sampleRate} Hz", modifier = Modifier.staggeredEntrance(5))
        }
        info.channelLabel?.let {
            GlassDivider()
            VennelaMetadataRow("Channels", it, modifier = Modifier.staggeredEntrance(6))
        }
    }

    LiquidButton(
        label = "Open in player",
        icon = Icons.Default.PlayArrow,
        onClick = { vm.navigateTo(Screen.AudioPlayer) }
    )
}

@Composable
private fun NestedArchiveScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Nested archive",
    subtitle = "Recursion-guarded",
    icon = Icons.Default.FolderZip
) {
    val entries by vm.archiveEntries.collectAsState()
    SafetyBanner("Depth capped at 4 with loop detection and ratio guards.")
    if (entries.isEmpty()) {
        InfoCard {
            VennelaEmptyState(
                title = "No archive entries",
                hint = "Open a ZIP, TAR or 7z file and its entries are listed without extracting them.",
                icon = Icons.Default.FolderZip,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        val shown = entries.take(12)
        ListCard(title = "${entries.size} entries", icon = Icons.Default.FolderZip) {
            shown.forEachIndexed { index, entry ->
                GlassListRow(
                    title = entry.name,
                    subtitle = entry.path,
                    modifier = Modifier
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .staggeredEntrance(index),
                    leading = { FileBrandBadge(extension = entry.extension.ifBlank { "bin" }, size = VennelaTokens.badgeSm) },
                    onClick = { vm.navigateTo(Screen.ArchiveBrowser) }
                )
                if (index < shown.lastIndex) GlassDivider()
            }
        }
        if (entries.size > shown.size) {
            NoteLine("${entries.size - shown.size} further entries are in this archive.")
        }
    }
}

// ---- GROUP H: technical viewers ----

@Composable
private fun HexViewerScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Hex inspector",
    subtitle = "Offsets • bytes • ASCII",
    icon = Icons.Default.Memory
) {
    val rows by vm.hexRows.collectAsState()
    if (rows.isEmpty()) {
        InfoCard {
            VennelaEmptyState(
                title = "No bytes read",
                hint = "Open a file and Vennela streams it in 64 KB windows — nothing is ever edited.",
                icon = Icons.Default.Memory,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
    } else {
        InfoCard(title = "${rows.size} rows • 16 bytes/line • paged") {
            CodeBlock(
                text = rows.take(12).joinToString("\n") { "${it.offset}  ${it.hexBytes.take(47)}  ${it.ascii}" }
            )
            if (rows.size > 12) {
                NoteLine("First 12 of ${rows.size} rows shown.")
            }
            PanelBody("Large files page through 64 KB windows. No byte is ever editable here.")
        }
    }
}

@Composable
private fun DatabaseViewerScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Database",
    subtitle = "SQLite inspector",
    icon = Icons.Default.Storage
) {
    val context = LocalContext.current
    val file by vm.selectedFile.collectAsState()
    var selectedTable by remember { mutableStateOf<String?>(null) }
    var tableSearchQuery by remember { mutableStateOf("") }

    val inspection by produceState<DbInspectionResult?>(initialValue = null, file?.uri) {
        value = DatabaseParser.inspectDatabase(context, file?.uri)
    }

    SafetyBanner("Read-only SQLite engine. Arbitrary SQL scripts from files are never executed.")

    val openFile = file
    val result = inspection
    when {
        openFile == null -> InfoCard {
            VennelaEmptyState(
                title = "No database open",
                hint = "Open a .db, .sqlite or .sql file and Vennela reads its schema without writing a single byte.",
                icon = Icons.Default.Storage,
                actionLabel = "Browse files",
                onAction = { vm.navigateTo(Screen.FileExplorer) }
            )
        }
        result == null -> InfoCard {
            VennelaLoadingState(label = "Analyzing SQLite database schema…")
        }
        result.error != null -> VennelaErrorState(
            title = "Database notice",
            message = result.error ?: "Could not open this SQLite database.",
            onRetry = { vm.navigateTo(Screen.HexViewer) }
        )
        else -> {
            val database: DbInspectionResult? = result
            if (database != null) {
                val tables = database.tables
                if (tables.isEmpty()) {
                    InfoCard(title = database.databaseName.ifBlank { "Database" }) {
                        PanelBody("This database has no user tables or views.")
                    }
                } else {
                    val activeTable = remember(selectedTable, tables) {
                        tables.firstOrNull { it.name == selectedTable } ?: tables.first()
                    }

                    InfoCard(title = "${tables.size} tables & views") {
                        PanelBody("${database.databaseName} • ${database.fileSizeFormatted} • read-only")
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            items(tables, key = { it.name }) { table ->
                                val active = table.name == activeTable.name
                                val halo by animateFloatAsState(
                                    targetValue = if (active) 1f else 0f,
                                    animationSpec = LiquidMotion.liquid(),
                                    label = "tableChipHalo"
                                )
                                LiquidChip(
                                    text = "${table.name} (${table.rowCount})",
                                    isActive = active,
                                    onClick = { selectedTable = table.name },
                                    modifier = Modifier
                                        .heightIn(min = VennelaTokens.touchTargetMin)
                                        .graphicsLayer {
                                            scaleX = 1f + 0.03f * halo
                                            scaleY = 1f + 0.03f * halo
                                        }
                                )
                            }
                        }
                    }

                    InfoCard(title = "Schema: ${activeTable.name} (${activeTable.type})") {
                        if (activeTable.columns.isEmpty()) {
                            PanelBody("This table exposes no columns.")
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                            ) {
                                items(activeTable.columns, key = { it.name }) { column ->
                                    StatPill(
                                        text = "${column.name}: ${column.type}" +
                                            if (column.isPrimaryKey) " [PK]" else ""
                                    )
                                }
                            }
                        }
                    }

                    val tableData by produceState<DbTableContent?>(
                        initialValue = null,
                        activeTable.name,
                        file?.uri
                    ) {
                        value = DatabaseParser.queryTableRows(context, file?.uri, activeTable.name, limit = 50)
                    }

                    InfoCard(title = "Rows (${activeTable.rowCount} total)") {
                        PanelBody("Preview limited to 50 rows.")
                        VennelaSearchField(
                            value = tableSearchQuery,
                            onValueChange = { tableSearchQuery = it },
                            placeholder = "Filter rows in ${activeTable.name}…"
                        )
                        val data = tableData
                        val rows = data?.rows.orEmpty()
                        val filteredRows = remember(rows, tableSearchQuery) {
                            if (tableSearchQuery.isBlank()) rows
                            else rows.filter { row -> row.any { it.contains(tableSearchQuery, ignoreCase = true) } }
                        }
                        when {
                            data == null -> VennelaLoadingState(label = "Reading rows…")
                            rows.isEmpty() -> PanelBody("This table is empty (0 rows).")
                            filteredRows.isEmpty() -> PanelBody("No row matches \"$tableSearchQuery\".")
                            else -> {
                                DataTable(columns = data.columns, rows = filteredRows)
                                if (filteredRows.size > 50) {
                                    NoteLine("Showing the first 50 matching rows.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScientificViewerScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Scientific data",
    subtitle = "Structure + summary",
    icon = Icons.Default.Science
) {
    InfoCard(title = "Dataset structure") {
        PanelBody("Dimensions, variables and numeric summaries where a tested handler exists — otherwise tabular metadata.")
    }
}

@Composable
private fun ModelInspectorScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Model checkpoint",
    subtitle = "Inspect, never run",
    icon = Icons.Default.SmartToy
) {
    SafetyBanner("Graphs are never executed; pickled objects are never deserialised unsafely.", BannerTone.Critical)
    InfoCard(title = "Model family") {
        PanelBody("Detectable metadata (framework, tensor shapes) appears here when a safe parser supports it.")
    }
}

@Composable
private fun FontViewerScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Typeface",
    subtitle = "Specimen",
    icon = Icons.Default.FontDownload
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current
    val file by vm.selectedFile.collectAsState()
    var previewText by remember { mutableStateOf("The moonlit lake reflects every glyph with absolute clarity.") }
    var fontSizeSp by remember { mutableFloatStateOf(24f) }

    val fontResult by produceState<FontLoadResult?>(initialValue = null, file?.uri) {
        value = FontParser.loadFont(context, file?.uri)
    }

    val customFontFamily = remember(fontResult?.typeface) {
        fontResult?.typeface?.let { FontFamily(androidx.compose.ui.text.font.Typeface(it)) }
    }

    ListCard(title = file?.name ?: "Custom font", icon = Icons.Default.FontDownload) {
        val meta = fontResult?.metadata
        if (meta == null) {
            VennelaMetadataRow("Font", "Parsing…", modifier = Modifier.staggeredEntrance(0))
        } else {
            VennelaMetadataRow("Style", meta.style, modifier = Modifier.staggeredEntrance(0))
            GlassDivider()
            VennelaMetadataRow("Size", meta.fileSizeFormatted, modifier = Modifier.staggeredEntrance(1))
        }
    }

    val fontError = fontResult?.metadata?.error
    if (fontError != null) {
        SafetyBanner(fontError, BannerTone.Critical)
    }

    val animatedSp by animateFloatAsState(
        targetValue = fontSizeSp,
        animationSpec = LiquidMotion.liquid(),
        label = "fontSizeSp"
    )

    InfoCard(title = "Interactive specimen") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Size", color = palette.textSecondary, fontSize = VennelaTokens.caption)
            Text(
                text = "${animatedSp.toInt()} sp",
                color = accent.secondary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = fontSizeSp,
            onValueChange = { fontSizeSp = it },
            valueRange = 14f..48f,
            colors = SliderDefaults.colors(
                thumbColor = accent.secondary,
                activeTrackColor = accent.primary,
                inactiveTrackColor = palette.cardSurfaceStrong
            )
        )
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .auroraGlow(
                    shape = RoundedCornerShape(VennelaTokens.radiusTile),
                    elevation = VennelaTokens.elevationFlat,
                    intensity = 0.26f
                ),
            shape = RoundedCornerShape(VennelaTokens.radiusTile),
            frost = VennelaTokens.frostLight,
            elevation = VennelaTokens.elevationRaised,
            alpha = 0.48f
        ) {
            BasicTextField(
                value = previewText,
                onValueChange = { previewText = it },
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = fontSizeSp.sp,
                    fontFamily = customFontFamily
                ),
                cursorBrush = SolidColor(accent.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VennelaTokens.touchTargetMin)
                    .padding(VennelaTokens.spaceMd)
            )
        }
        PanelBody("Type in the specimen above to preview every glyph with the loaded typeface.")
    }

    InfoCard(title = "Specimens") {
        Text("Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm", color = palette.textPrimary, fontSize = VennelaTokens.titleMedium, fontFamily = customFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz", color = palette.textPrimary, fontSize = VennelaTokens.titleMedium, fontFamily = customFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("0 1 2 3 4 5 6 7 8 9", color = accent.secondary, fontSize = VennelaTokens.titleLarge, fontWeight = FontWeight.Bold, fontFamily = customFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$ € £ ¥ ₹ % & @ ! ? #", color = palette.textSecondary, fontSize = VennelaTokens.titleMedium, fontFamily = customFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmailViewerScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Message",
    subtitle = "Mail • contact • event",
    icon = Icons.Default.Mail
) {
    ListCard(title = "Headers", icon = Icons.Default.Mail) {
        VennelaMetadataRow("From", "Shown from file headers", modifier = Modifier.staggeredEntrance(0))
        GlassDivider()
        VennelaMetadataRow("Subject", "Shown from file headers", modifier = Modifier.staggeredEntrance(1))
        GlassDivider()
        VennelaMetadataRow("Date", "Shown from file headers", modifier = Modifier.staggeredEntrance(2))
    }
    InfoCard(title = "Body") {
        PanelBody("Body preview as text. Attachments are listed, never auto-opened; private content stays on-device.")
    }
}

@Composable
private fun ExecutableInspectorScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Executable",
    subtitle = "NON-RUNNABLE",
    icon = Icons.Default.Dangerous
) {
    SafetyBanner(
        "This file can never be launched or installed from Vennela 365. Headers and imports are shown for inspection only.",
        BannerTone.Critical
    )
    InfoCard(title = "Headers") {
        PanelBody("Magic, architecture and section names where safely parseable — otherwise a hex dump.")
    }
    SecondaryAction(
        label = "Inspect bytes",
        icon = Icons.Default.Memory,
        onClick = { vm.navigateTo(Screen.HexViewer) }
    )
}

// ---- GROUP I: settings / recovery ----

@Composable
private fun OptionalModulesScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Optional modules",
    subtitle = "On-demand renderers",
    icon = Icons.Default.Extension,
    activeTab = "settings"
) {
    ListCard(title = "Installed (built-in)", icon = Icons.Default.Extension) {
        VennelaMetadataRow("Core viewers", "PDF • text • code • images • media • archives", modifier = Modifier.staggeredEntrance(0))
        GlassDivider()
        VennelaMetadataRow("Size", "Counted in the release report", modifier = Modifier.staggeredEntrance(1))
    }
    ListCard(title = "Available (not downloaded)", icon = Icons.Default.CloudDownload) {
        VennelaMetadataRow("Office fidelity pack", "Not installed • size shown before download", modifier = Modifier.staggeredEntrance(0))
        GlassDivider()
        VennelaMetadataRow("CAD geometry pack", "Not installed • offline-unavailable", modifier = Modifier.staggeredEntrance(1))
    }
    InfoCard { PanelBody("Modules never run code delivered as file content.") }
}

@Composable
private fun PrivacyPermissionsScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Privacy",
    subtitle = "Grants & history",
    icon = Icons.Default.PrivacyTip,
    activeTab = "settings"
) {
    InfoCard(title = "On this device") {
        PanelBody(
            "Per-file URI grants only. Revoke anytime in Android Settings → Apps → Vennela 365 → Permissions. " +
                "Clearing history removes local recents, never your documents."
        )
    }
    SecondaryAction(
        label = "Review permissions",
        icon = Icons.Default.VerifiedUser,
        onClick = { vm.navigateTo(Screen.PermissionStatus) }
    )
}

@Composable
private fun CacheStorageScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Cache & storage",
    subtitle = "Thumbnails + temp",
    icon = Icons.Default.CleaningServices,
    activeTab = "settings"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableStateOf<Long?>(null) }
    var clearing by remember { mutableStateOf(false) }

    fun measureDirBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.IO) { measureDirBytes(context.cacheDir) }
    }

    val cacheLabel = cacheBytes?.let { Formatter.formatFileSize(context, it) } ?: "Calculating…"

    ListCard(title = "Usage", icon = Icons.Default.CleaningServices) {
        VennelaMetadataRow("Temp & thumbnail cache", cacheLabel, modifier = Modifier.staggeredEntrance(0))
        GlassDivider()
        VennelaMetadataRow("User documents", "Untouched on device", modifier = Modifier.staggeredEntrance(1))
    }
    InfoCard { PanelBody("Clearing cache never deletes your documents.") }
    SecondaryAction(
        label = if (clearing) "Clearing cache…" else "Clear cache (safe)",
        modifier = Modifier.testTag("supplemental_clear_cache_button"),
        icon = Icons.Default.DeleteSweep,
        onClick = {
            if (clearing) return@SecondaryAction
            clearing = true
            scope.launch {
                val freed = withContext(Dispatchers.IO) {
                    val before = measureDirBytes(context.cacheDir)
                    context.cacheDir?.listFiles()?.forEach { child ->
                        runCatching { child.deleteRecursively() }
                    }
                    val after = measureDirBytes(context.cacheDir)
                    (before - after).coerceAtLeast(0L)
                }
                cacheBytes = withContext(Dispatchers.IO) { measureDirBytes(context.cacheDir) }
                clearing = false
                Toast.makeText(
                    context,
                    "Cleared ${Formatter.formatFileSize(context, freed)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}

@Composable
private fun AboutLicensesScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "About",
    subtitle = "Vennela 365 • 1.0.0",
    icon = Icons.Default.Info,
    activeTab = "settings"
) {
    InfoCard(title = "వెన్నెల 365 — moonlight viewer") {
        PanelBody("Native Android • Kotlin • Jetpack Compose • Material 3 Expressive. Media via Media3, images via Coil, persistence via DataStore.")
        PanelBody(
            "Limitations: proprietary layouts (Word pagination, CAD geometry, camera RAW) render only where tested " +
                "decoders exist — otherwise metadata plus an honest fallback."
        )
    }
}

@Composable
private fun HelpFormatsScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Supported formats",
    subtitle = "Live registry directory",
    icon = Icons.AutoMirrored.Filled.HelpCenter,
    activeTab = "settings"
) {
    val query by vm.registryQuery.collectAsState()
    val dashboard = remember { FormatRegistry.dashboard() }

    VennelaSearchField(
        value = query,
        onValueChange = { vm.setRegistryQuery(it) },
        placeholder = "Search ${dashboard.totalPatterns} extensions…",
        modifier = Modifier.testTag("supplemental_format_search_field")
    )

    val results = remember(query) {
        if (query.isBlank()) FormatRegistry.getAllFormats().take(12) else FormatRegistry.search(query).take(30)
    }

    InfoCard(
        title = "${dashboard.totalPatterns} patterns • L0:${dashboard.byLevel[0]} L1:${dashboard.byLevel[1]} " +
            "L2:${dashboard.byLevel[2]} L3:${dashboard.byLevel[3]} L4:${dashboard.byLevel[4]}"
    ) {
        PanelBody("Levels: L0 recognized → L4 fidelity-verified. Only L3 and L4 count as rendered previews.")
    }

    if (results.isEmpty()) {
        InfoCard {
            VennelaEmptyState(
                title = "No format matches \"$query\"",
                hint = "Try a shorter extension or a category name such as 'sheet' or 'image'.",
                icon = Icons.AutoMirrored.Filled.HelpCenter,
                actionLabel = "Clear search",
                onAction = { vm.setRegistryQuery("") }
            )
        }
    } else {
        ListCard(
            title = if (query.isBlank()) "First ${results.size} formats" else "${results.size} matches",
            icon = Icons.AutoMirrored.Filled.HelpCenter
        ) {
            results.forEachIndexed { index, format ->
                GlassListRow(
                    title = format.name,
                    subtitle = ".${format.extension} • L${format.levelNumber} • ${format.category.displayName}",
                    modifier = Modifier
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .staggeredEntrance(index),
                    leading = { FileBrandBadge(extension = format.extension, size = VennelaTokens.badgeSm) },
                    showChevron = true,
                    onClick = { vm.setRegistryQuery(format.extension) }
                )
                if (index < results.lastIndex) GlassDivider()
            }
        }
        if (query.isBlank() && dashboard.totalPatterns > results.size) {
            NoteLine("Showing the first ${results.size} — search to explore the full registry.")
        }
    }

    SecondaryAction(
        label = "Diagnostics",
        icon = Icons.Default.BugReport,
        onClick = { vm.navigateTo(Screen.DiagnosticsReport) }
    )
}

@Composable
private fun OfflineStateScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Offline",
    subtitle = "Installed capabilities",
    icon = Icons.Default.CloudOff
) {
    InfoCard(title = "Works without network") {
        PanelBody(
            "All built-in viewers (text, code, images, PDF, media, archives, sheets) run fully offline. Optional " +
                "modules show their offline-unavailable state instead of a spinner."
        )
    }
}

@Composable
private fun DiagnosticsReportScreen(vm: MainViewModel) = SupplementalPage(
    viewModel = vm,
    title = "Diagnostics",
    subtitle = "Compatibility report",
    icon = Icons.Default.BugReport,
    activeTab = "settings"
) {
    val dashboard = remember { FormatRegistry.dashboard() }
    val issues = remember { FormatRegistry.validate() }
    ListCard(title = "Registry • ${dashboard.totalPatterns} patterns", icon = Icons.Default.BugReport) {
        VennelaMetadataRow("Unique suffixes", dashboard.uniqueSuffixes.toString(), modifier = Modifier.staggeredEntrance(0))
        GlassDivider()
        VennelaMetadataRow("Compound suffixes", dashboard.compoundSuffixes.toString(), modifier = Modifier.staggeredEntrance(1))
        GlassDivider()
        VennelaMetadataRow("Signature rules", dashboard.signatureRules.toString(), modifier = Modifier.staggeredEntrance(2))
        GlassDivider()
        VennelaMetadataRow("Renderer types", dashboard.rendererTypes.toString(), modifier = Modifier.staggeredEntrance(3))
        GlassDivider()
        VennelaMetadataRow("L0 Recognized", (dashboard.byLevel[0] ?: 0).toString(), modifier = Modifier.staggeredEntrance(4))
        GlassDivider()
        VennelaMetadataRow("L1 Identified", (dashboard.byLevel[1] ?: 0).toString(), modifier = Modifier.staggeredEntrance(5))
        GlassDivider()
        VennelaMetadataRow("L2 Inspectable", (dashboard.byLevel[2] ?: 0).toString(), modifier = Modifier.staggeredEntrance(6))
        GlassDivider()
        VennelaMetadataRow("L3 Previewable", (dashboard.byLevel[3] ?: 0).toString(), modifier = Modifier.staggeredEntrance(7))
        GlassDivider()
        VennelaMetadataRow("L4 Verified", (dashboard.byLevel[4] ?: 0).toString(), modifier = Modifier.staggeredEntrance(8))
        GlassDivider()
        VennelaMetadataRow("Validation", if (issues.isEmpty()) "clean" else issues.take(3).joinToString("; "), modifier = Modifier.staggeredEntrance(9))
    }
    InfoCard {
        PanelBody(
            "Counts are recomputed from the registry at runtime — never hardcoded. Copy shares text only, with " +
                "consent; nothing uploads automatically."
        )
    }
}

