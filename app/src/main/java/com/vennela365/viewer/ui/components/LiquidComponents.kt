package com.vennela365.viewer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
 * One consistent page frame: atmosphere, status/navigation insets, an optional
 * frosted top bar and an optional floating dock.
 *
 * Screens use this instead of hand-rolling `Scaffold` + `VennelaBackground` so
 * insets, gutters and dock clearance can never drift apart again.
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
    val dockClearance = if (dock != null) 54.dp else 0.dp
    val barHidden = rememberChromeHideState(scrollState)

    // The page owns the atmosphere *and* the backdrop bitmap, so every glass
    // surface below it frosts the real scenery instead of a flat fill.
    VennelaBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (title != null) {
                    Spacer(modifier = Modifier.height(statusInset + VennelaTokens.spaceXxs))
                    // The bar slides away while the reader scrolls down, the way
                    // Google Photos and Drive hand the screen back to the
                    // content, and returns the moment they scroll up.
                    AnimatedVisibility(
                        visible = !barHidden,
                        enter = fadeIn(tween(LiquidMotion.durationFast)) +
                            expandVertically(tween(LiquidMotion.durationFast)),
                        exit = fadeOut(tween(LiquidMotion.durationFast)) +
                            shrinkVertically(tween(LiquidMotion.durationFast))
                    ) {
                        LiquidTopBar(
                            title = title,
                            subtitle = subtitle,
                            onBack = onBack,
                            actions = topBarActions
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(statusInset))
                }
                content(
                    PaddingValues(
                        bottom = navInset + dockClearance + VennelaTokens.spaceSm,
                        top = VennelaTokens.spaceXs
                    )
                )
            }

            if (dock != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navInset)
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
 *
 * Scrolling down hides it and scrolling up brings it back; returning to the
 * very top always reveals it again so the user is never stranded without
 * navigation.
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
                top = VennelaTokens.spaceXl,
                bottom = VennelaTokens.spaceMd,
                start = VennelaTokens.spaceXs,
                end = VennelaTokens.spaceXs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Neon accent bar — the section's signature.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.secondary, accent.primary.copy(alpha = 0.72f))
                        )
                    )
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (trailing != null) {
            trailing()
        } else if (actionLabel != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(accent.primary.copy(alpha = 0.14f))
                    .border(1.dp, accent.secondary.copy(alpha = 0.28f), RoundedCornerShape(VennelaTokens.radiusPill))
                    .pressScale(pressedScale = 0.94f, onClick = onAction)
                    .padding(horizontal = VennelaTokens.spaceSm + VennelaTokens.spaceXxs, vertical = VennelaTokens.spaceXs)
            ) {
                Text(
                    text = actionLabel,
                    color = accent.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent.secondary,
                    modifier = Modifier.size(VennelaTokens.iconSm)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Icon badge                                                                 */
/* ------------------------------------------------------------------------- */

/**
 * The coloured, gradient-filled rounded square that identifies a file type or
 * category. Shared by the home tiles, file rows and category screens so the
 * whole app speaks one visual language.
 *
 * When [onClick] is supplied the badge becomes an action and grows an invisible
 * touch target of at least [VennelaTokens.touchTargetMin] without changing the
 * painted size, so top-bar actions stay both pretty and accessible.
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
                .clip(RoundedCornerShape(radius))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.first(),
                            colors.last().copy(alpha = 0.86f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f))
                    ),
                    RoundedCornerShape(radius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.48f)
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
 * Liquid press feedback: a spring-damped scale plus a brightness lift, which
 * reads as the surface physically yielding to the finger.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.965f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = LiquidMotion.press(),
        label = "pressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = onClick != null,
            onClick = { onClick?.invoke() }
        )
}

/**
 * Staggered entrance so lists cascade in instead of popping all at once.
 * Rows rise, scale up and fade in on a shared rhythm.
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
            translationY = (1f - t) * 30f
            scaleX = 0.94f + 0.06f * t
            scaleY = 0.94f + 0.06f * t
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
        targetValue = if (isActive) accent.primary.copy(alpha = 0.46f)
        else palette.cardSurface.copy(alpha = 0.36f),
        animationSpec = androidx.compose.animation.core.tween(LiquidMotion.durationFast),
        label = "chipBg"
    )
    val border by animateColorAsState(
        targetValue = if (isActive) accent.secondary.copy(alpha = 0.95f)
        else palette.cardBorder.copy(alpha = 0.60f),
        animationSpec = androidx.compose.animation.core.tween(LiquidMotion.durationFast),
        label = "chipBorder"
    )
    val spec = LiquidGlassSpec(
        tint = bg,
        tintEnd = if (isActive) accent.secondary.copy(alpha = 0.30f) else bg,
        frost = VennelaTokens.frostLight,
        backdropAlpha = 0.5f,
        rim = border,
        rimWidth = 1.dp,
        sheen = if (isActive) 0.30f else 0.16f,
        elevation = if (isActive) 10.dp else 4.dp,
        shadowColor = if (isActive) accent.secondary.copy(alpha = 0.45f) else Color(0x66000000),
        highlight = if (isActive) 0.22f else 0.10f
    )
    Row(
        modifier = modifier
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(pressedScale = 0.93f, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else accent.secondary.copy(alpha = 0.85f),
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
        }
        Text(
            text = text,
            color = if (isActive) Color.White else palette.textPrimary,
            fontSize = VennelaTokens.caption,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/* ------------------------------------------------------------------------- */
/* List rows                                                                  */
/* ------------------------------------------------------------------------- */

/**
 * A file / folder / setting row on the glass system.
 *
 * Trailing chevrons rotate in on press and the leading badge lifts slightly,
 * giving the row a sense of depth without any layout cost.
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
            .padding(horizontal = VennelaTokens.spaceLg, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(VennelaTokens.iconSm)
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
            .padding(horizontal = VennelaTokens.spaceLg)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        palette.cardBorder.copy(alpha = 0.7f),
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
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .shadow(
                elevation = if (enabled) 16.dp else 0.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusPill),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.70f),
                spotColor = accent.secondary.copy(alpha = 0.70f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary,
                        accent.primary.copy(alpha = 0.90f),
                        accent.secondary
                    )
                )
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.40f)),
                RoundedCornerShape(VennelaTokens.radiusPill)
            )
            .pressScale(pressedScale = 0.955f, onClick = if (enabled) onClick else null)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = VennelaTokens.spaceXl, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(VennelaTokens.iconMd))
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = VennelaTokens.body,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
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
