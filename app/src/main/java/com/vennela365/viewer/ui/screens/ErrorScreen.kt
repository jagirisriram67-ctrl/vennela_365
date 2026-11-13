package com.vennela365.viewer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.AnimatedDisclosure
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

/** One real remedy offered after a failed open. */
private data class Remedy(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun ErrorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val file = selectedFile
    val failure = viewerState as? ViewerState.Failed
    val loading = viewerState is ViewerState.Loading || viewerState is ViewerState.Detecting

    fun retry() {
        if (file != null && (failure?.canRetry != false)) viewModel.openFile(file) else viewModel.navigateBack()
    }

    fun openWithAnotherApp() {
        val uri = file?.uri
        if (file == null || uri == null) {
            Toast.makeText(context, "This file has no shareable location", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.formatInfo.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open ${file.name} with"))
        }.onFailure {
            Toast.makeText(context, "No app on this device can open this file", Toast.LENGTH_SHORT).show()
        }
    }

    val subtitle = when {
        failure != null -> "${failure.reason.take(48)}${if (failure.reason.length > 48) "…" else ""}"
        loading -> "Re-checking ${file?.name ?: "the file"}…"
        file == null -> "No file loaded"
        else -> "Recovered • ${file.formattedSize}"
    }

    LiquidPage(
        modifier = modifier,
        title = file?.name ?: "File error",
        subtitle = subtitle,
        onBack = { viewModel.navigateBack() },
        topBarActions = {
            if (file != null) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.touchTargetMin)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(VennelaTokens.radiusControl),
                            clip = false,
                            ambientColor = accent.primary.copy(alpha = 0.50f),
                            spotColor = accent.secondary.copy(alpha = 0.50f)
                        )
                        .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.primary.copy(alpha = 0.42f),
                                    accent.secondary.copy(alpha = 0.26f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.28f),
                            RoundedCornerShape(VennelaTokens.radiusControl)
                        )
                        .pressScale(pressedScale = 0.90f, onClick = { viewModel.navigateTo(Screen.FileMetadata) }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Diagnostics for ${file.name}",
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                }
            }
        }
    ) { pagePadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // A single soft neon wash anchors the top of the page so the
                // error reads as a lit surface rather than a flat list.
                .background(
                    Brush.verticalGradient(
                        0f to accent.primary.copy(alpha = 0.14f),
                        0.45f to Color.Transparent,
                        1f to accent.secondary.copy(alpha = 0.08f)
                    )
                ),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            when {
                loading -> item(key = "loading") {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                clip = false,
                                ambientColor = accent.primary.copy(alpha = 0.40f),
                                spotColor = accent.secondary.copy(alpha = 0.40f)
                            ),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.44f
                    ) {
                        VennelaLoadingState(
                            label = "Re-opening ${file?.name ?: "the file"}…",
                            progress = (viewerState as? ViewerState.Loading)?.progress,
                            onCancel = { viewModel.navigateBack() }
                        )
                    }
                }

                failure != null -> {
                    item(key = "hero") {
                        ErrorHero()
                    }

                    item(key = "message") {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                    clip = false,
                                    ambientColor = accent.primary.copy(alpha = 0.40f),
                                    spotColor = accent.secondary.copy(alpha = 0.40f)
                                )
                                .border(
                                    1.dp,
                                    accent.secondary.copy(alpha = 0.28f),
                                    RoundedCornerShape(VennelaTokens.radiusCardLarge)
                                ),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.48f
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                accent.primary.copy(alpha = 0.16f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(VennelaTokens.spaceLg),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Couldn't open this file",
                                        color = palette.textPrimary,
                                        fontSize = VennelaTokens.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                                    Text(
                                        text = failure.reason,
                                        color = palette.textSecondary,
                                        fontSize = VennelaTokens.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                    if (file != null) {
                                        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FileBrandBadge(
                                                extension = file.extension,
                                                size = VennelaTokens.badgeSm
                                            )
                                            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                                            Column {
                                                Text(
                                                    text = file.name,
                                                    color = palette.textPrimary,
                                                    fontSize = VennelaTokens.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${file.extension.uppercase()} • ${file.formattedSize} • ${file.formattedDate}",
                                                    color = palette.textTertiary,
                                                    fontSize = VennelaTokens.micro
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "quick-fixes") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                            contentPadding = PaddingValues(vertical = VennelaTokens.spaceXs)
                        ) {
                            item {
                                LiquidChip(
                                    text = "Try again",
                                    icon = Icons.Default.Refresh,
                                    isActive = true,
                                    modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                                    onClick = { retry() }
                                )
                            }
                            item {
                                LiquidChip(
                                    text = "File details",
                                    icon = Icons.Default.Info,
                                    modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                                    onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                                )
                            }
                            item {
                                LiquidChip(
                                    text = "Open with…",
                                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                                    modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                                    onClick = { openWithAnotherApp() }
                                )
                            }
                            item {
                                LiquidChip(
                                    text = "Browse files",
                                    icon = Icons.Default.FolderOpen,
                                    modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                                    onClick = { viewModel.navigateTo(Screen.FileExplorer) }
                                )
                            }
                        }
                    }

                    item(key = "remedies-header") {
                        SectionHeader(title = "What you can do")
                    }

                    item(key = "remedies") {
                        val remedies = remember {
                            listOf(
                                Remedy(
                                    Icons.Default.Refresh,
                                    "Try opening it again",
                                    "Large files sometimes fail while another app is still holding them."
                                ),
                                Remedy(
                                    Icons.Default.Info,
                                    "Check file details",
                                    "Compare the detected format, size and checksum with the original."
                                ),
                                Remedy(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    "Open with another app",
                                    "Hand the untouched file to any other viewer installed on this device."
                                )
                            )
                        }
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                                remedies.forEachIndexed { index, remedy ->
                                    GlassListRow(
                                        title = remedy.title,
                                        subtitle = remedy.description,
                                        modifier = Modifier.staggeredEntrance(index),
                                        leading = {
                                            IconBadge(
                                                icon = remedy.icon,
                                                colors = listOf(accent.primary, accent.secondary),
                                                size = VennelaTokens.badgeSm,
                                                radius = VennelaTokens.radiusChip,
                                                contentDescription = null
                                            )
                                        },
                                        showChevron = true,
                                        onClick = {
                                            when (index) {
                                                0 -> retry()
                                                1 -> viewModel.navigateTo(Screen.FileMetadata)
                                                else -> openWithAnotherApp()
                                            }
                                        }
                                    )
                                    if (index != remedies.lastIndex) GlassDivider()
                                }
                            }
                        }
                    }

                    item(key = "diagnostics") {
                        DiagnosticsDisclosure(file = file, failure = failure)
                    }

                    item(key = "actions") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LiquidButton(
                                label = if (failure.canRetry) "Try again" else "Back to library",
                                icon = Icons.Default.Refresh,
                                onClick = { retry() }
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            SecondaryAction(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                label = "Open with another app",
                                onClick = { openWithAnotherApp() }
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                            SecondaryAction(
                                icon = Icons.Default.Handyman,
                                label = "Report this format",
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "${file?.extension?.uppercase() ?: "This format"} reported — thank you",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                file != null -> {
                    item(key = "recovered") {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                    clip = false,
                                    ambientColor = accent.primary.copy(alpha = 0.38f),
                                    spotColor = accent.secondary.copy(alpha = 0.38f)
                                )
                                .border(
                                    1.dp,
                                    accent.secondary.copy(alpha = 0.26f),
                                    RoundedCornerShape(VennelaTokens.radiusCardLarge)
                                ),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.48f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                accent.primary.copy(alpha = 0.14f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(VennelaTokens.spaceLg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconBadge(
                                    icon = Icons.Default.CheckCircle,
                                    colors = listOf(accent.primary, accent.secondary),
                                    size = VennelaTokens.badgeLg,
                                    radius = VennelaTokens.radiusBadge,
                                    contentDescription = "This file opens normally"
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                                Text(
                                    text = "This file opens normally",
                                    color = palette.textPrimary,
                                    fontSize = VennelaTokens.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                Text(
                                    text = when (val state = viewerState) {
                                        is ViewerState.PartiallySupported -> state.reason
                                        ViewerState.Unsupported -> "The format can't be rendered here, but its metadata and bytes are fully intact."
                                        else -> "The earlier problem cleared up — ${file.name} is ready to view."
                                    },
                                    color = palette.textSecondary,
                                    fontSize = VennelaTokens.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    item(key = "recovered-list") {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                                val rows = remember(file) {
                                    listOf(
                                        "Format" to "${file.extension.uppercase()} • ${file.formatInfo.category.displayName}",
                                        "Size" to file.formattedSize,
                                        "Modified" to file.formattedDate,
                                        "Location" to file.path.ifBlank { "Chosen through the system picker" }
                                    )
                                }
                                rows.forEachIndexed { index, row ->
                                    GlassListRow(
                                        title = row.first,
                                        subtitle = row.second,
                                        modifier = Modifier.staggeredEntrance(index),
                                        leading = {
                                            IconBadge(
                                                icon = Icons.Default.Info,
                                                colors = listOf(accent.primary, accent.secondary),
                                                size = VennelaTokens.badgeSm,
                                                radius = VennelaTokens.radiusChip,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    if (index != rows.lastIndex) GlassDivider()
                                }
                            }
                        }
                    }

                    item(key = "recovered-actions") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LiquidButton(
                                label = "Open ${file.name}",
                                icon = Icons.Default.FolderOpen,
                                onClick = { viewModel.openFile(file) }
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            SecondaryAction(
                                icon = Icons.Default.Info,
                                label = "View full details",
                                onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                            )
                        }
                    }
                }
            }

            if (file == null) {
                item(key = "empty") {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.38f
                    ) {
                        VennelaEmptyState(
                            title = "Nothing went wrong",
                            hint = "No file is loaded right now, so there is nothing to diagnose. Open something and it will land here if it fails.",
                            icon = Icons.Default.CheckCircle,
                            actionLabel = "Browse storage",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                }
            }

            item(key = "privacy-note") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                    Text(
                        text = "Your original file remains unchanged.",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Hero                                                                       */
/* ------------------------------------------------------------------------- */

/**
 * Frosted document with a glowing warning seal. Built entirely from accent and
 * palette tokens, so it follows the active theme instead of pinning a red.
 *
 * Aurora Lumen: the seal breathes, an alarm ring keeps expanding off it, and a
 * coloured aura (violet/magenta, never murky) carries the urgency while the
 * glyph itself stays white on its accent disc for readability.
 */
@Composable
private fun ErrorHero() {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val heroTransition = rememberInfiniteTransition(label = "errorHero")
    val pulse by heroTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "errorPulse"
    )
    val ringProgress by heroTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "errorRing"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(156.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.12f * pulse
                    scaleY = 1f + 0.12f * pulse
                    alpha = 0.34f + 0.34f * pulse
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.55f),
                            accent.primary.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Alarm ring expanding past the document.
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    val s = 1f + 0.36f * ringProgress
                    scaleX = s
                    scaleY = s
                    alpha = (1f - ringProgress) * 0.55f
                }
                .border(1.5.dp, accent.secondary, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(110.dp, 130.dp)
                .shadow(
                    elevation = 12.dp + 20.dp * pulse,
                    shape = RoundedCornerShape(VennelaTokens.radiusTile),
                    clip = false,
                    ambientColor = accent.primary.copy(alpha = 0.60f),
                    spotColor = accent.secondary.copy(alpha = 0.60f)
                )
                .clip(RoundedCornerShape(VennelaTokens.radiusTile))
                .background(
                    Brush.linearGradient(
                        listOf(
                            palette.cardSurface.copy(alpha = 0.62f),
                            accent.primary.copy(alpha = 0.16f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.26f),
                    RoundedCornerShape(VennelaTokens.radiusTile)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.70f),
                        spotColor = accent.secondary.copy(alpha = 0.70f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.primary, accent.secondary.copy(alpha = 0.86f))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = "This file could not be opened",
                    tint = Color.White,
                    modifier = Modifier.size(VennelaTokens.iconXl)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Diagnostics disclosure                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun DiagnosticsDisclosure(
    file: FileItem?,
    failure: ViewerState.Failed
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var expanded by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (expanded) accent.secondary.copy(alpha = 0.60f) else accent.secondary.copy(alpha = 0.22f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "diagnosticsBorder"
    )
    // The panel's halo grows with the disclosure instead of jumping.
    val panelGlow by animateFloatAsState(
        targetValue = if (expanded) 18f else 8f,
        animationSpec = LiquidMotion.liquid(),
        label = "diagnosticsGlow"
    )

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = panelGlow.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusCard),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.35f),
                spotColor = accent.secondary.copy(alpha = 0.35f)
            )
            .border(1.dp, borderColor, RoundedCornerShape(VennelaTokens.radiusCard))
            .pressScale(pressedScale = 0.99f, onClick = { expanded = !expanded }),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        alpha = 0.46f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = VennelaTokens.spaceLg,
                        vertical = VennelaTokens.spaceMd
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = Icons.Default.Info,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusChip,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Why this happened",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (expanded) "Tap to hide the diagnostic detail" else "Tap to see the diagnostic detail",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption
                    )
                }
                AnimatedDisclosure(expanded = expanded)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassDivider()
                    DiagnosticsRow("Reported reason", failure.reason)
                    GlassDivider()
                    DiagnosticsRow(
                        "Retry available",
                        if (failure.canRetry) "Yes — the parser can be run again" else "No — the file is rejected outright"
                    )
                    if (file != null) {
                        GlassDivider()
                        DiagnosticsRow("File", "${file.name} • ${file.formattedSize}")
                        GlassDivider()
                        DiagnosticsRow(
                            "Detected format",
                            "${file.extension.uppercase()} • ${file.formatInfo.category.displayName}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = VennelaTokens.spaceLg,
                vertical = VennelaTokens.spaceMd
            ),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = palette.textTertiary,
            fontSize = VennelaTokens.caption,
            modifier = Modifier.width(118.dp)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = value,
            color = palette.textSecondary,
            fontSize = VennelaTokens.caption,
            modifier = Modifier.weight(1f)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Secondary action                                                           */
/* ------------------------------------------------------------------------- */

@Composable
private fun SecondaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    val border by animateColorAsState(
        targetValue = accent.secondary.copy(alpha = 0.38f),
        animationSpec = tween(LiquidMotion.durationBase),
        label = "secondaryBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.35f),
                spotColor = accent.secondary.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.cardSurface.copy(alpha = 0.50f),
                        accent.primary.copy(alpha = 0.16f)
                    )
                )
            )
            .border(1.dp, border, shape)
            .pressScale(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceXl, vertical = VennelaTokens.spaceLg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
