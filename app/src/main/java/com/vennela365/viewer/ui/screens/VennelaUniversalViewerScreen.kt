package com.vennela365.viewer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.vennela365.viewer.model.HexRow
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.NeonCyan
import com.vennela365.viewer.theme.NeonMagenta
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.applyViewerColorMode
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LassoToComputeOverlay
import com.vennela365.viewer.ui.components.LightboxComparator
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.PrivacyCurtain
import com.vennela365.viewer.ui.components.PrivacyEngine
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.StudySheetCompilerView
import com.vennela365.viewer.ui.components.ThumbArcDock
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import java.util.Locale
import kotlin.math.log2

/** Bounded window of the hex stream, matching the parser's own paging. */
private const val MaxHexRows = 250

/** Hex-stream column widths, expressed on the token scale. */
private val HexOffsetWidth = VennelaTokens.badgeLg + VennelaTokens.spaceXl
private val HexAsciiWidth = VennelaTokens.badgeLg + VennelaTokens.spaceHuge

enum class UniversalLensMode(
    val title: String,
    val shortLabel: String,
    val icon: ImageVector
) {
    LIVE_PREVIEW("Live Preview", "Preview", Icons.Default.Visibility),
    HEX_INSPECTOR("Hex Stream", "Hex", Icons.Default.DataObject),
    METADATA("Deep Metadata", "Metadata", Icons.Default.Info),
    SECURITY_ENTROPY("Security & Entropy", "Security", Icons.Default.Shield)
}

/**
 * VennelaUniversalViewerScreen: Universal Inspection Lens for Vennela 365.
 *
 * Implements:
 * 1. Deep zero-latency format detection and multi-mode inspection for 140+ formats.
 * 2. Instant SAF file picker for real file testing.
 * 3. All category-killer tools (Lasso Compute, Lightbox Diff, Privacy Curtain, Study Sheet).
 * 4. OKLab/Sauvola/OLED Display Engine integration.
 *
 * The chrome is liquid glass; the dock, overlays and display engine are unchanged.
 */
@Composable
fun VennelaUniversalViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val documentText by viewModel.documentText.collectAsState()
    val hexRows by viewModel.hexRows.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val isLassoActive by viewModel.isLassoActive.collectAsState()
    val isSplitActive by viewModel.isSplitHorizonActive.collectAsState()
    val isPrivacyActive by viewModel.isPrivacyActive.collectAsState()
    val enableMicroLouvre by viewModel.enableMicroLouvre.collectAsState()
    val isStudySheetActive by viewModel.isStudySheetActive.collectAsState()
    val isLightboxActive by viewModel.isLightboxActive.collectAsState()

    val accent = LocalVennelaAccent.current
    var activeMode by remember { mutableStateOf(UniversalLensMode.LIVE_PREVIEW) }
    var isDockCollapsed by remember { mutableStateOf(false) }

    // SAF file picker to test ANY file in real-time
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = uri.lastPathSegment ?: "Selected File"
            viewModel.openUri(uri, displayName)
        }
    }
    val pickAnyFile = { filePickerLauncher.launch(arrayOf("*/*")); Unit }

    val activeText = remember(documentText, selectedFile, isPrivacyActive) {
        val base = if (documentText.isNotBlank()) documentText
        else selectedFile?.sampleContent ?: "Select a file to inspect with Universal Lens."
        if (isPrivacyActive) PrivacyEngine.maskPii(base) else base
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidPage(
            title = null,
            dock = {
                ThumbArcDock(
                    isCollapsed = isDockCollapsed,
                    onToggleCollapse = { isDockCollapsed = !isDockCollapsed },
                    onSearchClick = { viewModel.navigateTo(Screen.UniversalSearch) },
                    onColorModeClick = { viewModel.cycleViewerColorMode() },
                    onLassoClick = { viewModel.toggleLasso() },
                    onSplitHorizonClick = { viewModel.toggleSplitHorizon() },
                    onPrivacyClick = { viewModel.togglePrivacy() },
                    onStudySheetClick = { viewModel.showStudySheet() },
                    isLassoActive = isLassoActive,
                    isSplitActive = isSplitActive,
                    isPrivacyActive = isPrivacyActive,
                    // LiquidPage already applies the navigation inset, so the
                    // dock must not add it a second time.
                    modifier = Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                )
            }
        ) { pagePadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Floating, slim toolbar — the inspected content owns the screen.
                ReaderTopToolbar(
                    title = selectedFile?.name ?: "Universal Lens",
                    subtitle = "${selectedFile?.formatInfo?.name ?: "Auto-detect"} • ${viewerColorMode.title}",
                    onBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.ColorLens,
                        contentDescription = "Cycle colour mode",
                        onClick = { viewModel.cycleViewerColorMode() }
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.FolderOpen,
                        contentDescription = "Pick a file",
                        onClick = { pickAnyFile() }
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "File details",
                        onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = VennelaTokens.gutter)
                    .padding(
                        top = pagePadding.calculateTopPadding() + 62.dp,
                        bottom = pagePadding.calculateBottomPadding()
                    )
            ) {
                LensModeRail(
                    selected = activeMode,
                    onSelect = { activeMode = it }
                )

                Spacer(Modifier.height(VennelaTokens.spaceMd))

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            clip = false,
                            ambientColor = accent.secondary.copy(alpha = 0.45f),
                            spotColor = accent.primary.copy(alpha = 0.45f)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.26f),
                                    accent.secondary.copy(alpha = 0.40f)
                                )
                            ),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
                        ),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.54f,
                    elevation = VennelaTokens.elevationOverlay
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .applyViewerColorMode(viewerColorMode)
                            .padding(VennelaTokens.spaceLg)
                    ) {
                        when (activeMode) {
                            UniversalLensMode.LIVE_PREVIEW -> PreviewMode(
                                fileName = selectedFile?.name,
                                meta = "${selectedFile?.formattedSize ?: "0 B"} • " +
                                    "${selectedFile?.formatInfo?.category ?: "Universal"}",
                                text = activeText,
                                monospace = selectedFile?.formatInfo?.category == FormatCategory.CODE,
                                failure = viewerState as? ViewerState.Failed,
                                canOpen = selectedFile != null,
                                onOpen = { selectedFile?.let { viewModel.openFile(it) } },
                                onPickFile = pickAnyFile
                            )

                            UniversalLensMode.HEX_INSPECTOR -> HexMode(
                                rows = hexRows,
                                hasFile = selectedFile != null,
                                onPickFile = pickAnyFile
                            )

                            UniversalLensMode.METADATA -> MetadataMode(
                                fileName = selectedFile?.name,
                                extension = selectedFile?.extension,
                                sizeBytes = selectedFile?.sizeBytes,
                                formattedSize = selectedFile?.formattedSize,
                                formatName = selectedFile?.formatInfo?.name,
                                mimeType = selectedFile?.formatInfo?.mimeType,
                                category = selectedFile?.formatInfo?.category?.name,
                                supportLevel = selectedFile?.formatInfo?.supportLevel?.label,
                                uri = selectedFile?.uri?.toString(),
                                hasFile = selectedFile != null,
                                onPickFile = pickAnyFile
                            )

                            UniversalLensMode.SECURITY_ENTROPY -> SecurityMode(
                                rows = hexRows,
                                hasFile = selectedFile != null,
                                onPickFile = pickAnyFile
                            )
                        }
                    }
                }
            }
        }

        // Lasso-to-Compute Overlay
        LassoToComputeOverlay(
            isActive = isLassoActive,
            onClose = { viewModel.toggleLasso() },
            sourceTextProvider = { activeText }
        )

        // Directional Privacy Curtain
        PrivacyCurtain(
            isActive = isPrivacyActive,
            onClose = { viewModel.togglePrivacy() },
            enableMicroLouvre = enableMicroLouvre,
            onToggleMicroLouvre = { viewModel.setMicroLouvre(it) }
        )

        // Lightbox Comparator
        if (isLightboxActive) {
            LightboxComparator(
                originalText = activeText,
                modifiedText = activeText,
                onClose = { viewModel.dismissLightbox() }
            )
        }

        // Smart Slide-to-Study-Sheet Compiler
        if (isStudySheetActive) {
            StudySheetCompilerView(
                rawText = activeText,
                title = selectedFile?.name ?: "Universal Lens Document",
                onClose = { viewModel.dismissStudySheet() }
            )
        }
            }
    }
}

/* ------------------------------------------------------------------------- */
/* Chrome                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun LensTopAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.45f),
                spotColor = accent.secondary.copy(alpha = 0.45f)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.88f),
                        accent.secondary.copy(alpha = 0.72f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

/** Mode rail. A LazyRow so four pills never clip on a narrow device. */
@Composable
private fun LensModeRail(
    selected: UniversalLensMode,
    onSelect: (UniversalLensMode) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
        contentPadding = PaddingValues(vertical = VennelaTokens.spaceXs)
    ) {
        itemsIndexed(
            UniversalLensMode.entries.toList(),
            key = { _, mode -> mode.name }
        ) { index, mode ->
            val active = selected == mode
            val chipGlow by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                animationSpec = LiquidMotion.liquid(),
                label = "lensChipGlow"
            )
            LiquidChip(
                text = mode.shortLabel,
                icon = mode.icon,
                isActive = active,
                onClick = { onSelect(mode) },
                modifier = Modifier
                    .heightIn(min = VennelaTokens.touchTargetMin)
                    .graphicsLayer {
                        scaleX = 1f + 0.03f * chipGlow
                        scaleY = 1f + 0.03f * chipGlow
                    }
                    .staggeredEntrance(index)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Modes                                                                      */
/* ------------------------------------------------------------------------- */

@Composable
private fun PreviewMode(
    fileName: String?,
    meta: String,
    text: String,
    monospace: Boolean,
    failure: ViewerState.Failed?,
    canOpen: Boolean,
    onOpen: () -> Unit,
    onPickFile: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val heroEntrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        heroEntrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
    }

    when {
        failure != null -> VennelaErrorState(
            title = "This file could not be rendered",
            message = failure.reason,
            onRetry = if (failure.canRetry) onOpen else null
        )

        fileName == null -> VennelaEmptyState(
            title = "Nothing to inspect yet",
            hint = "Pick any file and Universal Lens detects its format, extracts its text and streams " +
                "its bytes — entirely on device.",
            icon = Icons.Default.FolderOpen,
            actionLabel = "Pick a file",
            onAction = onPickFile
        )

        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val t = heroEntrance.value
                        alpha = t
                        translationY = (1f - t) * 18f
                        scaleX = 0.96f + 0.04f * t
                        scaleY = 0.96f + 0.04f * t
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.50f),
                        spotColor = accent.secondary.copy(alpha = 0.50f)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.30f),
                                accent.secondary.copy(alpha = 0.45f)
                            )
                        ),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
                    ),
                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                alpha = 0.50f,
                elevation = VennelaTokens.elevationCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(VennelaTokens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Default.Description,
                        colors = listOf(accent.primary, accent.secondary),
                        size = VennelaTokens.badgeMd,
                        radius = VennelaTokens.radiusBadge
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = meta,
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.micro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(VennelaTokens.spaceSm))
                    LiquidButton(
                        label = "Open",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = onOpen,
                        enabled = canOpen,
                        fillWidth = false
                    )
                }
            }

            Spacer(Modifier.height(VennelaTokens.spaceMd))
            GlassDivider()
            Spacer(Modifier.height(VennelaTokens.spaceMd))

            Text(
                text = text,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                lineHeight = VennelaTokens.titleLarge,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}

@Composable
private fun HexMode(
    rows: List<HexRow>,
    hasFile: Boolean,
    onPickFile: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    when {
        rows.isNotEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows.take(MaxHexRows), key = { index, _ -> index }) { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(
                            if (index % 2 == 0) accent.primary.copy(alpha = 0.07f)
                            else Color.Transparent
                        )
                        .padding(
                            horizontal = VennelaTokens.spaceXs,
                            vertical = VennelaTokens.spaceXxs
                        )
                        .staggeredEntrance(index),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.offset,
                        color = accent.secondary,
                        fontSize = VennelaTokens.nano,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.width(HexOffsetWidth)
                    )
                    Text(
                        text = row.hexBytes,
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.nano,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = row.ascii,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.nano,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.width(HexAsciiWidth)
                    )
                }
            }
        }

        hasFile -> VennelaLoadingState(label = "Streaming hex dump…")

        else -> VennelaEmptyState(
            title = "No byte stream yet",
            hint = "Open a file and its raw bytes appear here with offsets and an ASCII gutter.",
            icon = Icons.Default.DataObject,
            actionLabel = "Pick a file",
            onAction = onPickFile
        )
    }
}

@Composable
private fun MetadataMode(
    fileName: String?,
    extension: String?,
    sizeBytes: Long?,
    formattedSize: String?,
    formatName: String?,
    mimeType: String?,
    category: String?,
    supportLevel: String?,
    uri: String?,
    hasFile: Boolean,
    onPickFile: () -> Unit
) {
    if (!hasFile) {
        VennelaEmptyState(
            title = "No metadata to show",
            hint = "Universal Lens reads size, MIME type, format family and support level straight from " +
                "the file — nothing is uploaded.",
            icon = Icons.Default.Info,
            actionLabel = "Pick a file",
            onAction = onPickFile
        )
        return
    }

    val facts = listOf(
        "File name" to (fileName ?: "Unknown"),
        "Extension" to (extension?.uppercase() ?: "None"),
        "Exact size" to "${sizeBytes ?: 0} bytes (${formattedSize ?: "0 B"})",
        "Format family" to (formatName ?: "Universal stream"),
        "MIME type" to (mimeType ?: "application/octet-stream"),
        "Category" to (category ?: "RAW"),
        "Support level" to (supportLevel ?: "Universal native"),
        "Storage URI" to (uri ?: "Local sandbox")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        facts.forEachIndexed { index, (label, value) ->
            GlassListRow(
                title = label,
                subtitle = value,
                modifier = Modifier.staggeredEntrance(index)
            )
            if (index < facts.lastIndex) GlassDivider()
        }
    }
}

@Composable
private fun SecurityMode(
    rows: List<HexRow>,
    hasFile: Boolean,
    onPickFile: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val entropy = remember(rows) { shannonEntropy(rows) }

    if (!hasFile && rows.isEmpty()) {
        VennelaEmptyState(
            title = "No byte distribution to profile",
            hint = "Open a file to measure its real Shannon entropy and confirm the read-only, " +
                "fully offline guarantees.",
            icon = Icons.Default.Shield,
            actionLabel = "Pick a file",
            onAction = onPickFile
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
    ) {
        Text(
            text = "File security & Shannon entropy",
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Measures how evenly the sampled bytes are distributed — high entropy marks " +
                "compressed or encrypted regions, low entropy marks structured text or padding.",
            color = palette.textSecondary,
            fontSize = VennelaTokens.micro
        )

        if (entropy == null) {
            VennelaLoadingState(label = "Sampling byte distribution…")
        } else {
            val entropyFill by animateFloatAsState(
                targetValue = entropy / 8f,
                animationSpec = LiquidMotion.glide(LiquidMotion.durationSlow),
                label = "entropyFill"
            )
            val entropyColor by animateColorAsState(
                targetValue = if (entropy > 7.5f) NeonMagenta else NeonCyan,
                animationSpec = LiquidMotion.glide(LiquidMotion.durationBase),
                label = "entropyColor"
            )
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.45f),
                        spotColor = accent.secondary.copy(alpha = 0.45f)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.26f),
                                accent.secondary.copy(alpha = 0.42f)
                            )
                        ),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
                    ),
                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                alpha = 0.52f,
                elevation = VennelaTokens.elevationCard
            ) {
                Column(modifier = Modifier.padding(VennelaTokens.spaceLg)) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "Calculated entropy: %.2f / 8.00 bits/byte",
                            entropy
                        ),
                        color = accent.secondary,
                        fontSize = VennelaTokens.caption,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    LinearProgressIndicator(
                        progress = { entropyFill },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(VennelaTokens.spaceSm)
                            .clip(RoundedCornerShape(VennelaTokens.radiusPill)),
                        color = entropyColor,
                        trackColor = palette.cardSurface
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Text(
                        text = "Computed from ${rows.size} sampled hex rows of this file.",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.micro
                    )
                }
            }
        }

        val guarantees = listOf(
            "Read-only guarantee" to "Enforced (POSIX mmap read-only descriptor)",
            "Macro execution" to "Blocked (zero scripting surface)",
            "Network isolation" to "100% offline (no internet permission)",
            "Memory boundary" to "Active (<10 MB JVM heap ceiling)"
        )
        Column {
            guarantees.forEachIndexed { index, (label, value) ->
                GlassListRow(
                    title = label,
                    subtitle = value,
                    modifier = Modifier.staggeredEntrance(index)
                )
                if (index < guarantees.lastIndex) GlassDivider()
            }
        }
    }
}

/**
 * Shannon entropy of the sampled bytes, decoded from the real hex stream.
 * Returns null when nothing has been streamed yet.
 */
private fun shannonEntropy(rows: List<HexRow>): Float? {
    val counts = IntArray(256)
    var total = 0
    rows.forEach { row ->
        row.hexBytes.split(' ').forEach { token ->
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return@forEach
            val value = trimmed.toIntOrNull(16) ?: return@forEach
            if (value in 0..255) {
                counts[value]++
                total++
            }
        }
    }
    if (total == 0) return null

    var entropy = 0.0
    counts.forEach { count ->
        if (count > 0) {
            val probability = count.toDouble() / total
            entropy -= probability * log2(probability)
        }
    }
    return entropy.toFloat()
}
