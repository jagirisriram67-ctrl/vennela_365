package com.vennela365.viewer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidGlassSpec
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec

/**
 * Standard frosted card. The workhorse surface of the whole app.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(VennelaTokens.radiusCard),
    frost: Dp = VennelaTokens.frostRegular,
    elevation: Dp = VennelaTokens.elevationCard,
    alpha: Float = 0.42f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = liquidGlassSpec(alpha = alpha, frost = frost, elevation = elevation)
    Column(
        modifier = modifier
            .liquidGlass(spec, shape)
            .then(if (onClick != null) Modifier.pressScale(onClick = onClick) else Modifier)
            .padding(VennelaTokens.spaceLg),
        content = content
    )
}

/** Frosted container without inner padding, for lists and custom layouts. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(VennelaTokens.radiusCard),
    frost: Dp = VennelaTokens.frostRegular,
    elevation: Dp = VennelaTokens.elevationCard,
    alpha: Float = 0.42f,
    content: @Composable BoxScope.() -> Unit
) {
    val spec = liquidGlassSpec(alpha = alpha, frost = frost, elevation = elevation)
    Box(modifier = modifier.liquidGlass(spec, shape), content = content)
}

/* ------------------------------------------------------------------------- */
/* Page scaffold                                                              */
/* ------------------------------------------------------------------------- */

/**
 * Unified page frame: atmosphere, edge-to-edge status/navigation insets,
 * adaptive tablet centering, collapsible frosted top bar, and floating dock.
 */
@Composable
fun LiquidPage(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    dock: (@Composable () -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    scrollState: ScrollSignal? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockClearance = if (dock != null) 78.dp else 0.dp

    VennelaBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title != null) {
                    Spacer(modifier = Modifier.height(statusInset + VennelaTokens.spaceXxs))
                    LiquidTopBar(
                        title = title,
                        subtitle = subtitle,
                        onBack = onBack,
                        actions = topBarActions
                    )
                } else {
                    Spacer(modifier = Modifier.height(statusInset))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 880.dp).fillMaxWidth()) {
                        content(
                            PaddingValues(
                                bottom = navInset + dockClearance + VennelaTokens.spaceSm,
                                top = VennelaTokens.spaceXs
                            )
                        )
                    }
                }
            }

            if (dock != null) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    dock()
                }
            }
        }
    }
}

/**
 * What the page frame needs to know about a scroller to decide whether the
 * chrome should step aside.
 *
 * Reading the state lazily keeps the frame independent of whether the content
 * below is a list or a grid.
 */
class ScrollSignal(
    val isScrolling: () -> Boolean,
    val lastScrolledBackward: () -> Boolean,
    val isAtTop: () -> Boolean
)

/** Builds a [ScrollSignal] from a lazy list. */
@Composable
fun rememberLazyScrollSignal(state: LazyListState?): ScrollSignal? {
    if (state == null) return null
    return remember(state) {
        ScrollSignal(
            isScrolling = { state.isScrollInProgress },
            lastScrolledBackward = { state.lastScrolledBackward },
            isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
        )
    }
}

/** Builds a [ScrollSignal] from a lazy grid. */
@Composable
fun rememberLazyGridScrollSignal(state: LazyGridState?): ScrollSignal? {
    if (state == null) return null
    return remember(state) {
        ScrollSignal(
            isScrolling = { state.isScrollInProgress },
            lastScrolledBackward = { state.lastScrolledBackward },
            isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
        )
    }
}

/**
 * Tracks whether the page chrome should hide.
 */
@Composable
fun rememberChromeHideState(signal: ScrollSignal?): Boolean {
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(signal) {
        val source = signal ?: return@LaunchedEffect
        snapshotFlow {
            Triple(
                source.isScrolling(),
                source.lastScrolledBackward(),
                source.isAtTop()
            )
        }.collect { (scrolling, backward, atTop) ->
            when {
                atTop -> hidden = false
                scrolling -> hidden = !backward
            }
        }
    }
    return hidden
}

/* ------------------------------------------------------------------------- */
/* Top bar                                                                    */
/* ------------------------------------------------------------------------- */

/**
 * A compact icon button.
 *
 * A thin alias over the app's single [ReaderIconButton], so a button in a
 * library header looks exactly like the same button in a reader toolbar.
 */
@Composable
fun LiquidIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    active: Boolean = false
) {
    ReaderIconButton(
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        active = active,
        tint = tint,
        onClick = onClick
    )
}

/**
 * The page's top bar.
 *
 * A thin alias over the app's single [ViewerTopBar]. The page frame already
 * reserves the status-bar inset, so this variant leaves it off.
 */
@Composable
fun LiquidTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    ViewerTopBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        onBack = onBack,
        statusInset = false,
        actions = actions
    )
}

/* ------------------------------------------------------------------------- */
/* Section header                                                             */
/* ------------------------------------------------------------------------- */

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = VennelaTokens.spaceLg,
                bottom = VennelaTokens.spaceSm,
                start = VennelaTokens.spaceXs,
                end = VennelaTokens.spaceXs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = palette.textPrimary,
            fontFamily = VennelaFonts.OnePlusDisplay,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp
        )
        if (trailing != null) {
            trailing()
        } else if (actionLabel != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(
                        if (palette.isDark) Color.White.copy(alpha = 0.10f)
                        else accent.primary.copy(alpha = 0.10f)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.72f),
                        RoundedCornerShape(VennelaTokens.radiusPill)
                    )
                    .pressScale(pressedScale = 0.94f, onClick = onAction)
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = if (palette.isDark) accent.secondary else accent.primary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (palette.isDark) accent.secondary else accent.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Icon badge                                                                 */
/* ------------------------------------------------------------------------- */

/**
 * Google Android 16 Material 3 Expressive squircle icon badge with frosted glass rim.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = VennelaTokens.badgeSm,
    radius: Dp = 12.dp,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val badge = @Composable {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(radius),
                    clip = false,
                    ambientColor = colors.first().copy(alpha = 0.32f),
                    spotColor = colors.last().copy(alpha = 0.32f)
                )
                .clip(RoundedCornerShape(radius))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.first(),
                            colors.last().copy(alpha = 0.90f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.54f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    RoundedCornerShape(radius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.50f)
            )
        }
    }

    if (onClick == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { badge() }
        return
    }

    Box(
        modifier = modifier
            .size(maxOf(size, VennelaTokens.touchTargetMin))
            .pressScale(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        badge()
    }
}

/* ------------------------------------------------------------------------- */
/* Motion helpers                                                             */
/* ------------------------------------------------------------------------- */

/**
 * Google Android 16 Material 3 Expressive reactive press feedback:
 * Combines native Material 3 bounded ripple illumination with a spring-damped
 * physical scale response (`dampingRatio = 0.56f, stiffness = 480f`).
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 480f),
        label = "m3ExpressivePressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            enabled = onClick != null,
            onClick = { onClick?.invoke() }
        )
}

/**
 * Staggered entrance so lists cascade in instead of popping all at once.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val visible = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        visible.animateTo(
            targetValue = 1f,
            animationSpec = LiquidMotion.cascade(index)
        )
    }
    return this
        .alpha(visible.value)
        .graphicsLayer {
            val t = visible.value
            translationY = (1f - t) * 24f
            scaleX = 0.95f + 0.05f * t
            scaleY = 0.95f + 0.05f * t
        }
}

/* ------------------------------------------------------------------------- */
/* Chips                                                                      */
/* ------------------------------------------------------------------------- */

@Composable
fun LiquidChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val bg by animateColorAsState(
        targetValue = when {
            isActive && palette.isDark -> accent.primary.copy(alpha = 0.44f)
            isActive -> accent.primary.copy(alpha = 0.20f)
            palette.isDark -> Color(0xFF171F35).copy(alpha = 0.62f)
            else -> Color.White.copy(alpha = 0.82f)
        },
        animationSpec = tween(LiquidMotion.durationFast),
        label = "chipBg"
    )
    val border by animateColorAsState(
        targetValue = when {
            isActive -> accent.secondary.copy(alpha = if (palette.isDark) 0.72f else 0.56f)
            palette.isDark -> Color.White.copy(alpha = 0.20f)
            else -> Color.White.copy(alpha = 0.88f)
        },
        animationSpec = tween(LiquidMotion.durationFast),
        label = "chipBorder"
    )
    val spec = LiquidGlassSpec(
        tint = if (isActive && palette.isDark) Color.White.copy(alpha = 0.18f) else bg,
        tintEnd = if (isActive) accent.secondary.copy(alpha = if (palette.isDark) 0.28f else 0.16f) else bg,
        frost = VennelaTokens.frostLight,
        backdropAlpha = 0.64f,
        rim = border,
        rimWidth = 1.dp,
        sheen = if (isActive) 0.28f else 0.18f,
        elevation = if (isActive) 8.dp else 4.dp,
        shadowColor = if (isActive) accent.primary.copy(alpha = 0.32f) else Color(0x33000000),
        highlight = if (isActive) 0.22f else 0.14f
    )
    val fgColor = when {
        isActive && palette.isDark -> Color.White
        isActive -> accent.primary
        else -> palette.textPrimary
    }
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(pressedScale = 0.93f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) fgColor else accent.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = fgColor,
            fontFamily = VennelaFonts.Sans,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/* ------------------------------------------------------------------------- */
/* List rows                                                                  */
/* ------------------------------------------------------------------------- */

/**
 * Google Android 16 Material 3 Expressive glassmorphic list row.
 */
@Composable
fun GlassListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale(0.985f, onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Hairline divider that fades at both ends, matching the liquid aesthetic. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        palette.cardBorder.copy(alpha = if (palette.isDark) 0.55f else 0.70f),
                        Color.Transparent
                    )
                )
            )
    )
}

/* ------------------------------------------------------------------------- */
/* Primary button                                                             */
/* ------------------------------------------------------------------------- */

@Composable
fun LiquidButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = true
) {
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusPill)
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 46.dp)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.45f),
                spotColor = accent.secondary.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.94f),
                        accent.primary
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.58f),
                            Color.White.copy(alpha = 0.16f)
                        )
                    )
                ),
                shape
            )
            .pressScale(pressedScale = 0.96f, onClick = if (enabled) onClick else null)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = Color.White,
            fontFamily = VennelaFonts.Sans,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp
        )
    }
}

/** Rotating chevron used as a disclosure affordance. */
@Composable
fun AnimatedDisclosure(expanded: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    val angle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = LiquidMotion.liquid(),
        label = "disclosure"
    )
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = palette.textTertiary,
        modifier = modifier
            .size(VennelaTokens.iconSm)
            .rotate(angle)
    )
}
