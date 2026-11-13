package com.vennela365.viewer.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The real runtime-permission situation on this device. */
private sealed interface GrantState {
    /** Reading the device's current grant status. */
    data object Checking : GrantState

    /** This Android version needs no runtime permission at all. */
    data object NotRequired : GrantState

    /** Every permission this screen asks for is already granted. */
    data object Granted : GrantState

    /** Some permissions are still missing. */
    data class Missing(val permissions: List<String>) : GrantState

    /** The user declined; only the system settings screen can change that now. */
    data object Denied : GrantState
}

/** One explanatory point shown in the permission rationale card. */
private data class PermissionPointItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * The runtime permissions this screen is responsible for on the running OS
 * version. Empty means Android grants access at install time.
 */
private fun requiredPermissions(): List<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    )

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    else -> emptyList()
}

private fun missingPermissions(context: Context, required: List<String>): List<String> =
    required.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

private fun permissionLabel(permission: String): String = when (permission) {
    Manifest.permission.READ_MEDIA_IMAGES -> "Photos"
    Manifest.permission.READ_MEDIA_VIDEO -> "Videos"
    Manifest.permission.READ_MEDIA_AUDIO -> "Audio"
    Manifest.permission.READ_EXTERNAL_STORAGE -> "Files"
    else -> permission.substringAfterLast('.')
}

@Composable
fun PermissionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val required = remember { requiredPermissions() }
    var state by remember { mutableStateOf<GrantState>(GrantState.Checking) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedAll = permissions.isNotEmpty() && permissions.values.all { it }
        if (grantedAll) {
            state = GrantState.Granted
            // Permission flow finished successfully — continue into the app.
            viewModel.navigateTo(Screen.Home)
        } else {
            state = GrantState.Denied
        }
    }

    fun openSystemSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        runCatching { context.startActivity(intent) }
    }

    fun requestAccess() {
        if (required.isEmpty()) {
            viewModel.navigateTo(Screen.Home)
            return
        }
        permissionLauncher.launch(required.toTypedArray())
    }

    LaunchedEffect(required) {
        state = if (required.isEmpty()) {
            GrantState.NotRequired
        } else {
            val missing = withContext(Dispatchers.IO) { missingPermissions(context, required) }
            if (missing.isEmpty()) GrantState.Granted else GrantState.Missing(missing)
        }
    }

    val subtitle = when (val current = state) {
        GrantState.Checking -> "Checking what this device requires…"
        GrantState.NotRequired -> "No permission needed on this Android version"
        GrantState.Granted -> "Access granted • read-only viewer"
        GrantState.Denied -> "Access declined — you can still change this"
        is GrantState.Missing -> "${current.permissions.size} of ${required.size} permissions still needed"
    }

    LiquidPage(
        modifier = modifier,
        title = "Storage access",
        subtitle = subtitle
    ) { pagePadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            when (val current = state) {
                GrantState.Checking -> item(key = "checking") {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.38f
                    ) {
                        VennelaLoadingState(label = "Reading this device's permission state…")
                    }
                }

                GrantState.NotRequired -> item(key = "not-required") {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                        alpha = 0.38f
                    ) {
                        VennelaEmptyState(
                            title = "Nothing to grant",
                            hint = "Android on this device hands file access to apps at install time, so there is nothing left to approve.",
                            icon = Icons.Default.Shield,
                            actionLabel = "Continue",
                            onAction = { viewModel.navigateTo(Screen.Home) }
                        )
                    }
                }

                GrantState.Denied -> item(key = "denied") {
                    VennelaErrorState(
                        title = "Access not granted",
                        message = "Vennela 365 can still open files you hand it directly, but it cannot list your library. Grant access from system settings to browse everything.",
                        onRetry = { openSystemSettings() },
                        onDetails = { viewModel.navigateTo(Screen.Home) }
                    )
                }

                is GrantState.Missing -> {
                    item(key = "hero") { PermissionHero() }

                    item(key = "intro") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Open the files you choose",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.titleDisplay,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            Text(
                                text = "Vennela 365 reads only what you pick. Nothing is uploaded, nothing is edited, and nothing leaves this device.",
                                color = palette.textSecondary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item(key = "chips") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                            contentPadding = PaddingValues(vertical = VennelaTokens.spaceXs)
                        ) {
                            itemsIndexed(items = current.permissions, key = { _, permission -> permission }) { _, permission ->
                                val tagSuffix = permission.substringAfterLast('.').lowercase()
                                LiquidChip(
                                    text = permissionLabel(permission),
                                    icon = Icons.Default.FolderOpen,
                                    isActive = true,
                                    modifier = Modifier
                                        .heightIn(min = VennelaTokens.touchTargetMin)
                                        .testTag("permissions_chip_$tagSuffix"),
                                    onClick = { requestAccess() }
                                )
                            }
                        }
                    }

                    item(key = "rationale-header") {
                        SectionHeader(title = "What this allows")
                    }

                    item(key = "rationale") {
                        val points = remember {
                            listOf(
                                PermissionPointItem(
                                    icon = Icons.Default.FolderOpen,
                                    title = "Read files you select",
                                    description = "Open and view documents, images, audio and video."
                                ),
                                PermissionPointItem(
                                    icon = Icons.Default.Security,
                                    title = "Files stay on your device",
                                    description = "Every parse happens locally — no upload, no account."
                                ),
                                PermissionPointItem(
                                    icon = Icons.Default.Lock,
                                    title = "No editing or deletion",
                                    description = "Vennela 365 is a read-only viewer, always."
                                )
                            )
                        }
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                                points.forEachIndexed { index, point ->
                                    GlassListRow(
                                        title = point.title,
                                        subtitle = point.description,
                                        modifier = Modifier.staggeredEntrance(index),
                                        leading = {
                                            IconBadge(
                                                icon = point.icon,
                                                colors = listOf(accent.primary, accent.secondary),
                                                size = VennelaTokens.badgeSm,
                                                radius = VennelaTokens.radiusChip,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    if (index != points.lastIndex) GlassDivider()
                                }
                            }
                        }
                    }

                    item(key = "actions") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            GlowHeroButton {
                                LiquidButton(
                                    label = "Grant access",
                                    icon = Icons.Default.Shield,
                                    modifier = Modifier.testTag("permissions_grant_button"),
                                    onClick = { requestAccess() }
                                )
                            }
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            QuietPillAction(
                                label = "Not now",
                                modifier = Modifier.testTag("permissions_not_now_button"),
                                onClick = { viewModel.navigateTo(Screen.Home) }
                            )
                        }
                    }
                }

                GrantState.Granted -> {
                    item(key = "granted") {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.42f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(VennelaTokens.spaceLg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconBadge(
                                    icon = Icons.Default.Shield,
                                    colors = listOf(accent.primary, accent.secondary),
                                    size = VennelaTokens.badgeLg,
                                    radius = VennelaTokens.radiusBadge,
                                    contentDescription = "Access granted"
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                                Text(
                                    text = "Access granted",
                                    color = palette.textPrimary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                Text(
                                    text = "Vennela 365 can now read the files you choose, and nothing else.",
                                    color = palette.textSecondary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    item(key = "granted-list") {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                                required.forEachIndexed { index, permission ->
                                    GlassListRow(
                                        title = permissionLabel(permission),
                                        subtitle = "Granted to Vennela 365",
                                        modifier = Modifier.staggeredEntrance(index),
                                        leading = {
                                            IconBadge(
                                                icon = Icons.Default.FolderOpen,
                                                colors = listOf(accent.primary, accent.secondary),
                                                size = VennelaTokens.badgeSm,
                                                radius = VennelaTokens.radiusChip,
                                                contentDescription = null
                                            )
                                        },
                                        trailing = {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "${permissionLabel(permission)} access is granted",
                                                tint = accent.secondary,
                                                modifier = Modifier.size(VennelaTokens.iconMd)
                                            )
                                        }
                                    )
                                    if (index != required.lastIndex) GlassDivider()
                                }
                            }
                        }
                    }

                    item(key = "granted-actions") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LiquidButton(
                                label = "Continue to library",
                                icon = Icons.Default.FolderOpen,
                                modifier = Modifier.testTag("permissions_continue_button"),
                                onClick = { viewModel.navigateTo(Screen.Home) }
                            )
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            QuietPillAction(
                                label = "Review in system settings",
                                modifier = Modifier.testTag("permissions_settings_button"),
                                onClick = { openSystemSettings() }
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
/* Hero                                                                       */
/* ------------------------------------------------------------------------- */

/**
 * The quieter secondary action: a faint glass pill with a brighter hairline
 * edge, so it reads as a real control next to the glowing primary CTA.
 */
@Composable
private fun QuietPillAction(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    val border by animateColorAsState(
        targetValue = accent.secondary.copy(alpha = 0.32f),
        animationSpec = tween(LiquidMotion.durationBase),
        label = "quietPillBorder"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.touchTargetMin)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.cardSurface.copy(alpha = 0.46f),
                        accent.primary.copy(alpha = 0.14f)
                    )
                )
            )
            .border(1.dp, border, shape)
            .pressScale(pressedScale = 0.97f, onClick = onClick)
            .padding(vertical = VennelaTokens.spaceMd),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontFamily = VennelaFonts.Sans,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Wraps the grant CTA so it reads as the one luminous object on the page: a
 * breathing halo behind the pill plus a hairline white rim of its own.
 */
@Composable
private fun GlowHeroButton(content: @Composable () -> Unit) {
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    val glowTransition = rememberInfiniteTransition(label = "ctaGlow")
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ctaPulse"
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        // Soft aura bleeding past the pill.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = 1.04f + 0.05f * glowPulse
                    scaleY = 1.14f + 0.16f * glowPulse
                    alpha = 0.55f + 0.35f * glowPulse
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.50f),
                            accent.primary.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 24.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = accent.primary.copy(alpha = 0.75f),
                    spotColor = accent.secondary.copy(alpha = 0.75f)
                )
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
        )
        content()
    }
}

/**
 * The frosted folder cloud from the reference sheet: a central glass folder
 * with three file tiles floating off it. Every colour is an accent token, so it
 * repaints with the active theme swatch.
 */
@Composable
private fun PermissionHero() {
    val accent = LocalVennelaAccent.current

    // Aurora Lumen: the folder breathes and a ring keeps expanding off it.
    val heroTransition = rememberInfiniteTransition(label = "permissionHero")
    val pulse by heroTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroPulse"
    )
    val ringProgress by heroTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroRing"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(212.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(196.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.06f * pulse
                    scaleY = 1f + 0.06f * pulse
                    alpha = 0.26f + 0.22f * pulse
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.55f),
                            accent.primary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Travelling ring.
        Box(
            modifier = Modifier
                .size(168.dp)
                .graphicsLayer {
                    val s = 1f + 0.30f * ringProgress
                    scaleX = s
                    scaleY = s
                    alpha = (1f - ringProgress) * 0.55f
                }
                .border(1.5.dp, accent.secondary, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(168.dp)
                .border(1.dp, accent.secondary.copy(alpha = 0.26f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(108.dp, 84.dp)
                .shadow(
                    elevation = 10.dp + 20.dp * pulse,
                    shape = RoundedCornerShape(VennelaTokens.radiusTile),
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.60f),
                    spotColor = accent.primary.copy(alpha = 0.60f)
                )
                .clip(RoundedCornerShape(VennelaTokens.radiusTile))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.52f),
                            accent.primary.copy(alpha = 0.42f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.28f),
                    RoundedCornerShape(VennelaTokens.radiusTile)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Folder you grant access to",
                tint = Color.White,
                modifier = Modifier.size(VennelaTokens.iconXl)
            )
        }

        FloatingTile(
            icon = Icons.Default.Description,
            offsetX = -62.dp,
            offsetY = -36.dp,
            width = 46.dp,
            height = 56.dp
        )
        FloatingTile(
            icon = Icons.Default.Image,
            offsetX = 0.dp,
            offsetY = -70.dp,
            width = 50.dp,
            height = 44.dp
        )
        FloatingTile(
            icon = Icons.Default.Code,
            offsetX = 64.dp,
            offsetY = -32.dp,
            width = 48.dp,
            height = 58.dp
        )
    }
}

@Composable
private fun FloatingTile(
    icon: ImageVector,
    offsetX: Dp,
    offsetY: Dp,
    width: Dp,
    height: Dp
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(width, height)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusControl),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.45f),
                spotColor = accent.secondary.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.cardSurface.copy(alpha = 0.62f),
                        accent.primary.copy(alpha = 0.20f)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.26f),
                RoundedCornerShape(VennelaTokens.radiusControl)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(VennelaTokens.iconLg)
        )
    }
}
