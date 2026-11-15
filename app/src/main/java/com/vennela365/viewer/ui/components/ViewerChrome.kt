package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LiquidGlassSpec
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec

/**
 * Google Android 16 Material 3 Expressive Glassmorphic Top App Bar.
 *
 * Floating frosted M3 surface container (`52.dp` height) with:
 * - Tonal M3 circular Back button (`36.dp`) with reactive ripple + spring scale,
 * - Optional leading Material 3 squircle container badge,
 * - Cleanly proportioned OnePlus Display title (`15.sp` Bold) and subtitle (`11.sp` Medium),
 * - Optional Material 3 linear progress indicator along the bottom edge,
 * - Precision-spaced trailing action icons (`ReaderIconButton` / `ViewerIconAction`).
 */
@Composable
fun ViewerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Color.Unspecified,
    leadingTint: Color? = null,
    progress: Float? = null,
    statusInset: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val barShape = RoundedCornerShape(VennelaTokens.radiusPill)

    val spec = LiquidGlassSpec(
        tint = if (palette.isDark) {
            Color(0xFF171E33).copy(alpha = 0.78f)
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        tintEnd = if (palette.isDark) {
            Color(0xFF0E1424).copy(alpha = 0.70f)
        } else {
            palette.cardSurface.copy(alpha = 0.80f)
        },
        frost = VennelaTokens.frostHeavy,
        backdropAlpha = 0.80f,
        rim = if (palette.isDark) {
            Color.White.copy(alpha = 0.24f)
        } else {
            Color.White.copy(alpha = 0.90f)
        },
        rimWidth = 1.dp,
        sheen = 0.24f,
        elevation = VennelaTokens.elevationBar,
        shadowColor = if (palette.isDark) {
            Color.Black.copy(alpha = 0.50f)
        } else {
            palette.backgroundBottom.copy(alpha = 0.14f)
        },
        highlight = 0.20f
    )

    val insetMod = if (statusInset) {
        Modifier.windowInsetsPadding(WindowInsets.statusBars)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(insetMod)
            .padding(
                horizontal = VennelaTokens.gutter,
                vertical = VennelaTokens.spaceXs
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .height(VennelaTokens.viewerBar)
                .liquidGlass(spec, barShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VennelaTokens.viewerBar)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    val backFill = if (palette.isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                accent.primary.copy(alpha = 0.22f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.92f),
                                accent.primary.copy(alpha = 0.14f)
                            )
                        )
                    }
                    val backRim = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.48f else 0.92f),
                            accent.secondary.copy(alpha = 0.30f)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .testTag("viewer_back_button")
                            .size(VennelaTokens.viewerControl)
                            .clip(CircleShape)
                            .background(backFill)
                            .border(1.dp, backRim, CircleShape)
                            .pressScale(pressedScale = 0.90f, onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                }

                if (leadingIcon != null) {
                    val resolvedTint = when {
                        leadingTint != null -> leadingTint
                        leadingIconTint != Color.Unspecified -> leadingIconTint
                        else -> accent.secondary
                    }
                    val badgeShape = RoundedCornerShape(11.dp)
                    Box(
                        modifier = Modifier
                            .size(VennelaTokens.viewerControl)
                            .clip(badgeShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        resolvedTint.copy(alpha = 0.26f),
                                        resolvedTint.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.42f),
                                        resolvedTint.copy(alpha = 0.34f)
                                    )
                                ),
                                badgeShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = resolvedTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        fontFamily = VennelaFonts.OnePlusDisplay,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.15).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions
                )
            }

            if (progress != null) {
                val fraction by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(LiquidMotion.durationFast),
                    label = "viewerBarProgress"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.primary, accent.secondary)
                            )
                        )
                )
            }
        }
    }
}

/**
 * Compact circular action icon button inside a [ViewerTopBar].
 */
@Composable
fun ViewerIconAction(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit
) {
    ReaderIconButton(
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        active = active,
        enabled = enabled,
        tint = tint,
        onClick = onClick
    )
}

/**
 * Google Android 16 Material 3 Expressive Glassmorphic Floating Toolbar Dock.
 *
 * Houses primary viewer tools (search, zoom, TOC, theme, share, info) inside
 * a frosted M3 `surfaceContainerHigh` capsule (`54.dp` height).
 */
@Composable
fun ViewerDock(
    modifier: Modifier = Modifier,
    navInset: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val dockShape = RoundedCornerShape(VennelaTokens.radiusPill)

    val spec = LiquidGlassSpec(
        tint = if (palette.isDark) {
            Color(0xFF171E33).copy(alpha = 0.80f)
        } else {
            Color.White.copy(alpha = 0.88f)
        },
        tintEnd = if (palette.isDark) {
            Color(0xFF0E1424).copy(alpha = 0.72f)
        } else {
            palette.cardSurface.copy(alpha = 0.82f)
        },
        frost = VennelaTokens.frostHeavy,
        backdropAlpha = 0.82f,
        rim = if (palette.isDark) {
            Color.White.copy(alpha = 0.24f)
        } else {
            Color.White.copy(alpha = 0.90f)
        },
        rimWidth = 1.dp,
        sheen = 0.26f,
        elevation = VennelaTokens.elevationDock,
        shadowColor = if (palette.isDark) {
            Color.Black.copy(alpha = 0.54f)
        } else {
            palette.backgroundBottom.copy(alpha = 0.16f)
        },
        highlight = 0.22f
    )

    val insetMod = if (navInset) {
        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(insetMod)
            .padding(
                horizontal = VennelaTokens.gutter,
                vertical = 6.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(VennelaTokens.viewerDock)
                .liquidGlass(spec, dockShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

/**
 * Compact circular action icon button inside a [ViewerDock].
 */
@Composable
fun ViewerDockAction(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit
) {
    ReaderIconButton(
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        active = active,
        enabled = enabled,
        tint = tint,
        onClick = onClick
    )
}

/**
 * Interactive tool item inside a [ViewerDock] with optional text label.
 */
@Composable
fun ViewerDockItem(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)

    val fg by animateColorAsState(
        targetValue = when {
            !enabled -> palette.textTertiary
            active -> if (palette.isDark) Color.White else accent.primary
            else -> palette.textPrimary
        },
        animationSpec = tween(LiquidMotion.durationFast),
        label = "viewerDockFg"
    )

    val activeFill = if (palette.isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                accent.primary.copy(alpha = 0.36f),
                accent.secondary.copy(alpha = 0.24f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                accent.primary.copy(alpha = 0.22f),
                accent.secondary.copy(alpha = 0.16f)
            )
        )
    }

    val activeBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (palette.isDark) 0.56f else 0.92f),
            accent.secondary.copy(alpha = 0.42f)
        )
    )

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .then(
                if (active) {
                    Modifier
                        .background(activeFill, shape)
                        .border(1.dp, activeBorder, shape)
                } else {
                    Modifier
                }
            )
            .pressScale(pressedScale = 0.92f, onClick = if (enabled) onClick else null)
            .padding(
                horizontal = if (label != null) 12.dp else 10.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(VennelaTokens.viewerIcon)
        )
        if (!label.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = fg,
                fontFamily = VennelaFonts.Sans,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Expandable frosted glass tray floating directly above [ViewerDock].
 */
@Composable
fun ViewerSheet(
    visible: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusCardLarge)
    val spec = liquidGlassSpec(
        alpha = 0.80f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationOverlay,
        sheen = 0.24f,
        highlight = 0.20f
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(LiquidMotion.durationFast)) +
            expandVertically(tween(LiquidMotion.durationFast)),
        exit = fadeOut(tween(LiquidMotion.durationFast)) +
            shrinkVertically(tween(LiquidMotion.durationFast))
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = VennelaTokens.gutter, vertical = 4.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .liquidGlass(spec, shape)
                .padding(VennelaTokens.viewerSheetPadding)
        ) {
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!trailing.isNullOrBlank()) {
                        Text(
                            text = trailing,
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
        }
    }
}

/**
 * Compact glass search bar that drops under [ViewerTopBar] when in-document
 * search is active.
 */
@Composable
fun ViewerFindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchIndex: Int,
    matchCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Find in document…"
) {
    val palette = LocalVennelaPalette.current
    val spec = liquidGlassSpec(
        alpha = 0.68f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationBar,
        sheen = 0.24f,
        highlight = 0.20f,
        accentRim = true
    )
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceXs)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(
                start = VennelaTokens.spaceSm,
                end = VennelaTokens.spaceXs,
                top = VennelaTokens.spaceXs,
                bottom = VennelaTokens.spaceXs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
    ) {
        VennelaSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = placeholder,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        val countText = when {
            query.isBlank() -> ""
            matchCount == 0 -> "0/0"
            else -> "${(matchIndex + 1).coerceIn(1, matchCount)}/$matchCount"
        }
        if (countText.isNotEmpty()) {
            Text(
                text = countText,
                color = palette.textSecondary,
                fontFamily = VennelaFonts.Mono,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        ReaderIconButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Previous match",
            enabled = matchCount > 0,
            onClick = onPrev
        )
        ReaderIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Next match",
            enabled = matchCount > 0,
            onClick = onNext
        )
        ReaderIconButton(
            icon = Icons.Default.Close,
            contentDescription = "Close find",
            onClick = onClose
        )
    }
}

/**
 * Translucent iOS-style modal bottom sheet used by viewers for Contents,
 * Display options, Bookmarks, and Details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerGlassSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val shape = RoundedCornerShape(
        topStart = VennelaTokens.radiusSheet,
        topEnd = VennelaTokens.radiusSheet
    )
    val spec = liquidGlassSpec(
        alpha = 0.82f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationOverlay,
        sheen = 0.24f,
        highlight = 0.20f
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = palette.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(spec, shape)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = VennelaTokens.viewerSheetPadding,
                    end = VennelaTokens.viewerSheetPadding,
                    top = VennelaTokens.spaceSm,
                    bottom = VennelaTokens.spaceLg
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 38.dp, height = 5.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(palette.textTertiary.copy(alpha = 0.45f))
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = VennelaTokens.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = VennelaTokens.caption
                        )
                    }
                }
                ReaderIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss
                )
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            GlassDivider()
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            content()
        }
    }
}
