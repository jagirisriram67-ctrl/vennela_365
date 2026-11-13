package com.vennela365.viewer.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.vennela365.viewer.storage.ShareUri

/** One real, installed app that can receive an `ACTION_SEND` intent. */
private data class ShareAppTarget(
    val label: String,
    val packageName: String
)

/** A quick action offered on the share sheet. */
private data class ShareAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun ShareScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val file = selectedFile
    val apps = rememberShareTargets(context)
    val others = remember(file, recentFiles, favoriteFiles) {
        (recentFiles + favoriteFiles)
            .distinctBy { it.id }
            .filter { it.id != file?.id }
            .take(5)
    }

    val busy = viewerState is ViewerState.Loading || viewerState is ViewerState.Detecting
    val failure = viewerState as? ViewerState.Failed

    fun shareNow(target: FileItem) {
        val intent = sendIntentFor(context, target)
        if (intent == null) {
            Toast.makeText(context, "${target.name} has no shareable location", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent.createChooser(intent, "Share ${target.name}"))
    }

    LiquidPage(
        modifier = modifier,
        title = "Share",
        subtitle = file?.let { "${it.name} • ${it.formattedSize}" } ?: "Nothing selected yet",
        onBack = { viewModel.navigateBack() },
        topBarActions = {
            if (file != null) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.touchTargetMin)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(VennelaTokens.radiusControl),
                            clip = false,
                            ambientColor = accent.primary.copy(alpha = 0.60f),
                            spotColor = accent.secondary.copy(alpha = 0.60f)
                        )
                        .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.primary.copy(alpha = 0.85f),
                                    accent.secondary.copy(alpha = 0.70f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.34f),
                            RoundedCornerShape(VennelaTokens.radiusControl)
                        )
                        .pressScale(pressedScale = 0.90f, onClick = { shareNow(file) }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share ${file.name} with another app",
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                }
            }
        }
    ) { pagePadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            if (file == null) {
                item(key = "empty") {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.38f
                    ) {
                        VennelaEmptyState(
                            title = "Nothing to share yet",
                            hint = "Open a file from your device first — then pick any app here to send it, fully offline.",
                            icon = Icons.Default.FileUpload,
                            actionLabel = "Browse storage",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                }
            }

            when {
                failure != null -> item(key = "error") {
                    VennelaErrorState(
                        title = "Couldn't prepare this file",
                        message = failure.reason,
                        onRetry = if (failure.canRetry && file != null) {
                            { viewModel.openFile(file) }
                        } else {
                            null
                        },
                        onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                busy -> item(key = "loading") {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.38f
                    ) {
                        VennelaLoadingState(
                            label = "Preparing ${file?.name ?: "your file"} for sharing…",
                            progress = (viewerState as? ViewerState.Loading)?.progress,
                            onCancel = { viewModel.navigateBack() }
                        )
                    }
                }

                file != null -> {
                    item(key = "selected") {
                        SelectedFileCard(
                            file = file,
                            onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                        )
                    }

                    item(key = "actions-header") {
                        SectionHeader(title = "Quick actions")
                    }

                    item(key = "actions") {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                                val actions = listOf(
                                    ShareAction("Copy file name", "Also copies the Vennela 365 reference link", Icons.Default.ContentCopy),
                                    ShareAction("Create shareable link", "Secure link that expires after 24 hours", Icons.Default.Link),
                                    ShareAction("Compress & share", "Zip the file before sending it on", Icons.Default.FolderZip),
                                    ShareAction("Share as PDF", "Printable copy, original stays untouched", Icons.Default.PictureAsPdf)
                                )
                                actions.forEachIndexed { index, action ->
                                    // Cascade the quick actions in (note:
                                    // animateFloatAsState would start at its
                                    // target, so an Animatable is used here).
                                    val entered = remember { Animatable(0f) }
                                    LaunchedEffect(action.title) {
                                        entered.animateTo(
                                            targetValue = 1f,
                                            animationSpec = LiquidMotion.cascade(index, stepMillis = 60)
                                        )
                                    }
                                    GlassListRow(
                                        title = action.title,
                                        subtitle = action.subtitle,
                                        modifier = Modifier.graphicsLayer {
                                            val t = entered.value
                                            alpha = t
                                            translationY = (1f - t) * 18f
                                            scaleX = 0.96f + 0.04f * t
                                            scaleY = 0.96f + 0.04f * t
                                        },
                                        leading = {
                                            IconBadge(
                                                icon = action.icon,
                                                colors = listOf(accent.primary, accent.secondary),
                                                size = VennelaTokens.badgeSm,
                                                radius = VennelaTokens.radiusChip,
                                                contentDescription = null
                                            )
                                        },
                                        showChevron = true,
                                        onClick = {
                                            when (action.title) {
                                                "Copy file name" -> {
                                                    clipboardManager.setText(
                                                        AnnotatedString("vennela365://share/${file.name}")
                                                    )
                                                    Toast.makeText(context, "Share reference copied", Toast.LENGTH_SHORT).show()
                                                }
                                                else -> Toast.makeText(
                                                    context,
                                                    "${action.title} • ${file.name}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                    if (index != actions.lastIndex) GlassDivider()
                                }
                            }
                        }
                    }

                    item(key = "apps-header") {
                        SectionHeader(
                            title = "Share via",
                            actionLabel = "All apps",
                            onAction = { shareNow(file) }
                        )
                    }

                    item(key = "apps") {
                        when {
                            apps == null -> GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
                            ) {
                                VennelaLoadingState(label = "Looking for apps that accept file shares…")
                            }

                            apps.isEmpty() -> GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                alpha = 0.38f
                            ) {
                                Column(
                                    modifier = Modifier.padding(VennelaTokens.spaceLg),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No app on this device accepts file shares",
                                        color = palette.textPrimary,
                                        fontSize = VennelaTokens.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                    Text(
                                        text = "The system share sheet is still available — it offers Bluetooth and nearby transfer.",
                                        color = palette.textSecondary,
                                        fontSize = VennelaTokens.caption
                                    )
                                    Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                                    // Glowing hero CTA — the one bright action in
                                    // this otherwise empty state.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        accent.secondary.copy(alpha = 0.42f),
                                                        accent.primary.copy(alpha = 0.18f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .padding(VennelaTokens.spaceXs),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LiquidButton(
                                            label = "Open system share sheet",
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            onClick = { shareNow(file) }
                                        )
                                    }
                                }
                            }

                            else -> LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                                contentPadding = PaddingValues(vertical = VennelaTokens.spaceXs)
                            ) {
                                itemsIndexed(items = apps, key = { _, app -> app.packageName }) { index, app ->
                                    ShareAppTile(
                                        target = app,
                                        index = index,
                                        onClick = { shareNowTo(context, file, app) }
                                    )
                                }
                            }
                        }
                    }

                    if (others.isNotEmpty()) {
                        item(key = "others-header") {
                            SectionHeader(title = "Also in this library")
                        }

                        item(key = "others") {
                            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                                    others.forEachIndexed { index, other ->
                                        val entered = remember { Animatable(0f) }
                                        LaunchedEffect(other.id) {
                                            entered.animateTo(
                                                targetValue = 1f,
                                                animationSpec = LiquidMotion.cascade(index, stepMillis = 60)
                                            )
                                        }
                                        GlassListRow(
                                            title = other.name,
                                            subtitle = "${other.extension.uppercase()} • ${other.formattedSize} • ${other.formattedDate}",
                                            modifier = Modifier.graphicsLayer {
                                                val t = entered.value
                                                alpha = t
                                                translationY = (1f - t) * 18f
                                                scaleX = 0.96f + 0.04f * t
                                                scaleY = 0.96f + 0.04f * t
                                            },
                                            leading = {
                                                FileBrandBadge(
                                                    extension = other.extension,
                                                    size = VennelaTokens.badgeSm
                                                )
                                            },
                                            trailing = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(VennelaTokens.touchTargetMin)
                                                        .shadow(
                                                            elevation = 10.dp,
                                                            shape = RoundedCornerShape(VennelaTokens.radiusChip),
                                                            clip = false,
                                                            ambientColor = accent.primary.copy(alpha = 0.50f),
                                                            spotColor = accent.secondary.copy(alpha = 0.50f)
                                                        )
                                                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    accent.primary.copy(alpha = 0.42f),
                                                                    accent.secondary.copy(alpha = 0.28f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            1.dp,
                                                            Color.White.copy(alpha = 0.26f),
                                                            RoundedCornerShape(VennelaTokens.radiusChip)
                                                        )
                                                        .pressScale(pressedScale = 0.90f, onClick = { shareNow(other) }),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Share ${other.name}",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(VennelaTokens.iconMd)
                                                    )
                                                }
                                            },
                                            onClick = { shareNow(other) }
                                        )
                                        if (index != others.lastIndex) GlassDivider()
                                    }
                                }
                            }
                        }
                    }

                    item(key = "nearby") {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                    clip = false,
                                    ambientColor = accent.primary.copy(alpha = 0.42f),
                                    spotColor = accent.secondary.copy(alpha = 0.42f)
                                )
                                .border(
                                    1.dp,
                                    accent.secondary.copy(alpha = 0.28f),
                                    RoundedCornerShape(VennelaTokens.radiusCardLarge)
                                )
                                .pressScale(pressedScale = 0.98f, onClick = { shareNow(file) }),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.44f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                accent.primary.copy(alpha = 0.18f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(
                                        horizontal = VennelaTokens.spaceLg,
                                        vertical = VennelaTokens.spaceMd
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconBadge(
                                    icon = Icons.Default.Podcasts,
                                    colors = listOf(accent.primary, accent.secondary),
                                    size = VennelaTokens.badgeSm,
                                    radius = VennelaTokens.radiusChip,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nearby transfer",
                                        color = palette.textPrimary,
                                        fontSize = VennelaTokens.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Send over Bluetooth or quick share, no internet needed",
                                        color = palette.textSecondary,
                                        fontSize = VennelaTokens.caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = accent.secondary,
                                    modifier = Modifier.size(VennelaTokens.iconMd)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Selected file                                                              */
/* ------------------------------------------------------------------------- */

@Composable
private fun SelectedFileCard(
    file: FileItem,
    onDetails: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCard)

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.50f),
                spotColor = accent.secondary.copy(alpha = 0.50f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape),
        shape = shape,
        alpha = 0.48f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.20f),
                            accent.secondary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(VennelaTokens.spaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeLg)
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                Text(
                    text = "${file.extension.uppercase()} • ${file.formatInfo.category.displayName} • ${file.formattedSize}",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.path.isNotBlank()) {
                    Text(
                        text = file.path,
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                Text(
                    text = "Modified ${file.formattedDate}",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro
                )
            }
            Box(
                modifier = Modifier
                    .size(VennelaTokens.touchTargetMin)
                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                    .pressScale(pressedScale = 0.90f, onClick = onDetails),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "File details for ${file.name}",
                    tint = accent.secondary,
                    modifier = Modifier.size(VennelaTokens.iconMd)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Real installed share targets                                               */
/* ------------------------------------------------------------------------- */

@Composable
private fun ShareAppTile(
    target: ShareAppTarget,
    index: Int,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val icon = rememberAppIcon(target.packageName)
    // Each target lifts in on a cascade. (animateFloatAsState would start at
    // its target value and show no entrance, so an Animatable drives it.)
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(target.packageName) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = LiquidMotion.cascade(index)
        )
    }
    val border by animateColorAsState(
        targetValue = accent.secondary.copy(alpha = 0.40f),
        animationSpec = tween(LiquidMotion.durationBase),
        label = "shareTileBorder"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .graphicsLayer {
                val t = entrance.value
                alpha = t
                translationY = (1f - t) * 16f
            }
            .clip(RoundedCornerShape(VennelaTokens.radiusTile))
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .padding(vertical = VennelaTokens.spaceSm)
    ) {
        Box(
            modifier = Modifier
                .size(VennelaTokens.badgeLg + VennelaTokens.spaceSm)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(VennelaTokens.radiusBadge),
                    clip = false,
                    ambientColor = accent.primary.copy(alpha = 0.50f),
                    spotColor = accent.secondary.copy(alpha = 0.50f)
                )
                .clip(RoundedCornerShape(VennelaTokens.radiusBadge))
                .background(
                    Brush.linearGradient(
                        listOf(
                            palette.cardSurface.copy(alpha = 0.62f),
                            accent.primary.copy(alpha = 0.22f)
                        )
                    )
                )
                .border(
                    1.dp,
                    border,
                    RoundedCornerShape(VennelaTokens.radiusBadge)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = "Share with ${target.label}",
                    modifier = Modifier
                        .size(VennelaTokens.badgeSm)
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share with ${target.label}",
                    tint = accent.secondary,
                    modifier = Modifier.size(VennelaTokens.iconLg)
                )
            }
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
        Text(
            text = target.label,
            color = palette.textSecondary,
            fontSize = VennelaTokens.micro,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Queries the device for apps that genuinely accept a file share. Returns
 * `null` while the query runs so the rail can show a real loading state, and an
 * empty list when the device has no such app.
 */
@Composable
private fun rememberShareTargets(context: Context): List<ShareAppTarget>? {
    var targets by remember { mutableStateOf<List<ShareAppTarget>?>(null) }
    LaunchedEffect(context) {
        targets = withContext(Dispatchers.IO) {
            runCatching {
                val probe = Intent(Intent.ACTION_SEND).apply { type = "*/*" }
                context.packageManager
                    .queryIntentActivities(probe, 0)
                    .mapNotNull { resolved ->
                        val packageName = resolved.activityInfo?.packageName ?: return@mapNotNull null
                        if (packageName == context.packageName) return@mapNotNull null
                        ShareAppTarget(
                            label = resolved.loadLabel(context.packageManager).toString(),
                            packageName = packageName
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
        }
    }
    return targets
}

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = 96, height = 96)
                    .asImageBitmap()
            }.getOrNull()
        }
    }
    return icon
}

/* ------------------------------------------------------------------------- */
/* Intent helpers                                                             */
/* ------------------------------------------------------------------------- */

private fun sendIntentFor(context: Context, file: FileItem): Intent? {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = file.formatInfo.mimeType
        putExtra(Intent.EXTRA_TITLE, file.name)
    }
    // Direct-path browsing yields file:// URIs; they must be exported through
    // the FileProvider or the receiving app crashes on FileUriExposedException.
    return if (ShareUri.attachStream(context, intent, file.uri)) intent else null
}

private fun shareNowTo(context: Context, file: FileItem?, target: ShareAppTarget) {
    if (file == null) return
    val intent = sendIntentFor(context, file) ?: run {
        Toast.makeText(context, "${file.name} has no shareable location", Toast.LENGTH_SHORT).show()
        return
    }
    intent.setPackage(target.packageName)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "${target.label} can't open this file", Toast.LENGTH_SHORT).show()
        }
}
