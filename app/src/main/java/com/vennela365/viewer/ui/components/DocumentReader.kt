package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import kotlin.math.roundToInt

/* ------------------------------------------------------------------------- */
/* Reader chrome                                                              */
/* ------------------------------------------------------------------------- */

/** Hairline reading progress, pinned to the top edge like Google Docs'. */
@Composable
fun ReaderProgressRule(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val accent = LocalVennelaAccent.current
    val fraction by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "readerProgress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(paletteScrim())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(accent.primary, accent.secondary))
                )
        )
    }
}

@Composable
private fun paletteScrim(): Color = LocalVennelaPalette.current.cardBorder.copy(alpha = 0.35f)

/**
 * The floating top toolbar.
 *
 * A thin alias over the app's single [ViewerTopBar] so a reader, a library
 * page and a settings page all present the exact same chrome.
 */
@Composable
fun ReaderTopToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    ViewerTopBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        onBack = onBack,
        statusInset = true,
        actions = actions
    )
}

/**
 * The one compact icon button in the app.
 *
 * A 32dp glass disc that fits inside a 40dp bar without inflating it, with an
 * accent-lit active state. Every toolbar, dock and tray uses this.
 */
@Composable
fun ReaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val resolved = tint ?: when {
        !enabled -> palette.textTertiary
        active -> if (palette.isDark) Color.White else accent.primary
        else -> palette.textPrimary
    }
    val fillBrush = when {
        active && palette.isDark -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                accent.primary.copy(alpha = 0.42f),
                accent.secondary.copy(alpha = 0.28f)
            )
        )
        active -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                accent.primary.copy(alpha = 0.24f)
            )
        )
        palette.isDark -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.06f)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                palette.cardSurface.copy(alpha = 0.72f)
            )
        )
    }
    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (active) 0.62f else if (palette.isDark) 0.28f else 0.85f),
            if (active) accent.secondary.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.10f)
        )
    )
    Box(
        modifier = modifier
            .size(VennelaTokens.controlSm)
            .clip(CircleShape)
            .background(fillBrush, CircleShape)
            .border(1.dp, rimBrush, CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = if (enabled) onClick else null),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolved,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * The floating bottom pill. Actions sit inline with an optional status line
 * so the reader never gives up a full row to chrome.
 */
@Composable
fun ReaderBottomPill(
    modifier: Modifier = Modifier,
    status: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val spec = liquidGlassSpec(
        alpha = 0.72f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationDock,
        sheen = 0.24f,
        highlight = 0.20f
    )
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = VennelaTokens.spaceSm, vertical = 6.dp)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!status.isNullOrBlank()) {
            Text(
                text = status,
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        content()
    }
}

/**
 * The paper the document is printed on: a centred, soft-shadowed sheet that
 * never exceeds a comfortable measure, exactly like a Google Doc page.
 */
@Composable
fun ReaderPaper(
    modifier: Modifier = Modifier,
    maxWidth: Dp? = 720.dp,
    cornerRadius: Dp = VennelaTokens.radiusCard,
    contentPadding: PaddingValues = PaddingValues(VennelaTokens.spaceXl),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
            .shadow(
                elevation = VennelaTokens.elevationOverlay,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = if (palette.isDark) Color(0x99000000) else Color(0x220B142B),
                spotColor = if (palette.isDark) Color(0x99000000) else Color(0x220B142B)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (palette.isDark) {
                    Brush.verticalGradient(
                        listOf(
                            palette.cardSurfaceStrong.copy(alpha = 0.94f),
                            palette.cardSurface.copy(alpha = 0.90f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFF7FAFF))
                    )
                }
            )
            .border(1.dp, palette.cardBorder, RoundedCornerShape(cornerRadius))
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

/* ------------------------------------------------------------------------- */
/* Reading settings sheet                                                     */
/* ------------------------------------------------------------------------- */

/** One selectable colour treatment for the page. */
data class ReaderColorOption(
    val label: String,
    val mode: ViewerColorMode,
    val icon: ImageVector
)

/**
 * The reading-settings sheet (Google Docs' "Formatting" panel): text size,
 * line spacing, page tint, wrapping and screen-awake, all in one place so the
 * reader chrome itself can stay out of the way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    onDismiss: () -> Unit,
    fontScale: Float,
    onFontScale: (Float) -> Unit,
    spacing: Float,
    onSpacing: (Float) -> Unit,
    colorOptions: List<ReaderColorOption>,
    activeColorMode: ViewerColorMode,
    onColorMode: (ViewerColorMode) -> Unit,
    wrap: Boolean,
    onWrap: (Boolean) -> Unit,
    keepAwake: Boolean,
    onKeepAwake: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurfaceStrong,
        contentColor = palette.textPrimary,
        dragHandle = { SheetDragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceLg)
                .padding(bottom = VennelaTokens.spaceXxl),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            Text(
                text = "Reading settings",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Text size
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReaderIconButton(
                    icon = Icons.Default.TextDecrease,
                    contentDescription = "Smaller text",
                    onClick = { onFontScale((fontScale - 0.1f).coerceAtLeast(0.8f)) }
                )
                Text(
                    text = "Text size  ${(fontScale * 100).roundToInt()}%",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = VennelaTokens.spaceMd)
                )
                ReaderIconButton(
                    icon = Icons.Default.TextIncrease,
                    contentDescription = "Larger text",
                    onClick = { onFontScale((fontScale + 0.1f).coerceAtMost(1.8f)) }
                )
            }

            // Line spacing
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReaderIconButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tighter spacing",
                    onClick = { onSpacing((spacing - 0.1f).coerceAtLeast(0.8f)) }
                )
                Text(
                    text = "Line spacing  ${String.format(java.util.Locale.US, "%.1f×", spacing)}",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = VennelaTokens.spaceMd)
                )
                ReaderIconButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Looser spacing",
                    onClick = { onSpacing((spacing + 0.1f).coerceAtMost(2.2f)) }
                )
            }

            // Page tint
            Text(
                text = "Page tint",
                color = palette.textTertiary,
                fontSize = VennelaTokens.nano,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                colorOptions.forEach { option ->
                    val selected = option.mode == activeColorMode
                    Box(
                        modifier = Modifier
                            .size(VennelaTokens.badgeXs)
                            .clip(CircleShape)
                            .background(
                                if (selected) accent.primary else palette.cardSurface
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accent.secondary else palette.cardBorder,
                                shape = CircleShape
                            )
                            .clickable { onColorMode(option.mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.label,
                            tint = if (selected) Color.White else palette.textSecondary,
                            modifier = Modifier.size(VennelaTokens.iconSm)
                        )
                    }
                }
            }

            // Toggles
            ReaderToggleRow(
                label = "Wrap long lines",
                checked = wrap,
                onCheckedChange = onWrap
            )
            ReaderToggleRow(
                label = "Keep screen awake",
                checked = keepAwake,
                onCheckedChange = onKeepAwake
            )
        }
    }
}

@Composable
private fun ReaderToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(palette.cardSurface.copy(alpha = 0.40f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .clip(CircleShape)
                .background(if (checked) accent.primary else palette.cardBorder),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun SheetDragHandle() {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = Modifier
            .padding(vertical = VennelaTokens.spaceSm)
            .size(width = 34.dp, height = 4.dp)
            .clip(CircleShape)
            .background(palette.cardBorder)
    )
}

/** Slides the reader chrome in and out with the page. */
@Composable
fun ReaderChromeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    fromTop: Boolean = true,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(LiquidMotion.durationFast)) +
            if (fromTop) slideInVertically(tween(LiquidMotion.durationFast)) { -it } else {
                slideInVertically(tween(LiquidMotion.durationFast)) { it }
            },
        exit = fadeOut(tween(LiquidMotion.durationFast)) +
            if (fromTop) slideOutVertically(tween(LiquidMotion.durationFast)) { -it } else {
                slideOutVertically(tween(LiquidMotion.durationFast)) { it }
            },
        content = content
    )
}
