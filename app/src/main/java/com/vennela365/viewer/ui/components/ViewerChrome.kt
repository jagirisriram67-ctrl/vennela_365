package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec

/* ------------------------------------------------------------------------- */
/* Compact viewer chrome                                                      */
/*                                                                            */
/* Every full-screen viewer shares this chrome. It is deliberately thinner    */
/* than the library chrome: a 40dp floating bar, 30dp controls and an         */
/* icon-only 42dp dock, so the document, page or slide owns the screen.       */
/* ------------------------------------------------------------------------- */

/**
 * The floating document/page/slide top bar.
 *
 * Renders a single glass pill: back affordance, one-line title, an optional
 * quiet subtitle and up to three compact actions. A hairline progress rule
 * sits inside the pill's lower edge so reading position costs no extra space.
 */
/**
 * The one top bar in the app.
 *
 * Every screen — library, viewer, settings, editor — renders this same bar, so
 * the chrome never changes shape between destinations. It is a single glass
 * pill: a compact back affordance, an optional leading glyph, a one-line
 * title, a quiet subtitle and a row of 30dp actions. A hairline progress rule
 * sits inside the pill's lower edge, so reading position costs no extra space.
 *
 * Set [statusInset] when the bar floats over the page rather than sitting
 * under a status-bar spacer.
 */
@Composable
fun ViewerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Color.Unspecified,
    progress: Float? = null,
    statusInset: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.40f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationBar,
        sheen = 0.18f
    )
    val resolvedLeadingTint =
        if (leadingIconTint == Color.Unspecified) accent.secondary else leadingIconTint

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationFast))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = (1f - entrance.value) * -16f
                alpha = 0.4f + 0.6f * entrance.value
            }
            .then(
                if (statusInset) {
                    Modifier.windowInsetsPadding(WindowInsets.statusBars)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceXs)
            .height(VennelaTokens.viewerBar)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.viewerControl)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.primary.copy(alpha = 0.90f),
                                    accent.secondary.copy(alpha = 0.70f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        .pressScale(pressedScale = 0.90f, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(VennelaTokens.viewerIcon)
                    )
                }
                Spacer(Modifier.width(VennelaTokens.spaceXs))
            } else {
                Spacer(Modifier.width(VennelaTokens.spaceSm))
            }

            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.viewerControl)
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(resolvedLeadingTint.copy(alpha = 0.18f))
                        .border(1.dp, resolvedLeadingTint.copy(alpha = 0.42f), RoundedCornerShape(VennelaTokens.radiusChip)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = resolvedLeadingTint,
                        modifier = Modifier.size(VennelaTokens.viewerIcon)
                    )
                }
                Spacer(Modifier.width(VennelaTokens.spaceXs))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.nano,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }

        if (progress != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(palette.cardBorder.copy(alpha = 0.22f))
            ) {
                val fraction by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = LiquidMotion.glide(LiquidMotion.durationFast),
                    label = "viewerChromeProgress"
                )
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.primary, accent.secondary, Color.White.copy(alpha = 0.9f))
                            )
                        )
                )
            }
        }
    }
}

/**
 * A compact circular action for viewer bars and trays.
 *
 * A thin alias over the app's single [ReaderIconButton].
 */
@Composable
fun ViewerIconAction(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ReaderIconButton(
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        active = active,
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * The floating icon dock. Icon-only by design: labels made the dock read like
 * a second navigation bar and stole ~20dp of content on every screen.
 */
@Composable
fun ViewerDock(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val spec = liquidGlassSpec(
        alpha = 0.36f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationDock,
        sheen = 0.18f
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceXs)
            .height(VennelaTokens.viewerDock)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(horizontal = VennelaTokens.spaceXs, vertical = VennelaTokens.spaceXxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXxs),
        content = content
    )
}

/** One dock action: equal width, capsule highlight when active, no label. */
@Composable
fun RowScope.ViewerDockAction(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val activeScale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = LiquidMotion.hero(),
        label = "dockActionScale"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = VennelaTokens.touchTargetCompact)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .then(
                if (active) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.primary.copy(alpha = 0.88f),
                                    accent.secondary.copy(alpha = 0.62f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.34f),
                            RoundedCornerShape(VennelaTokens.radiusControl)
                        )
                } else {
                    Modifier
                }
            )
            .pressScale(pressedScale = 0.93f, onClick = if (enabled) onClick else null),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> palette.textTertiary
                active -> Color.White
                else -> palette.textSecondary
            },
            modifier = Modifier
                .size(VennelaTokens.viewerIcon)
                .graphicsLayer {
                    scaleX = activeScale
                    scaleY = activeScale
                }
        )
    }
}

/** Hairline separator between dock sections. */
@Composable
fun ViewerDockDivider() {
    val palette = LocalVennelaPalette.current
    Box(
        Modifier
            .width(1.dp)
            .height(18.dp)
            .background(palette.cardBorder.copy(alpha = 0.35f))
    )
}

/** The compact tray that hosts a viewer's panels above the dock. */
@Composable
fun ViewerSheet(
    visible: Boolean,
    icon: ImageVector? = null,
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.liquid()),
        exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit())
    ) {
        Box(Modifier.fillMaxWidth().padding(bottom = VennelaTokens.spaceXs)) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(VennelaTokens.radiusSheet),
                elevation = VennelaTokens.elevationOverlay
            ) {
                Column(Modifier.padding(VennelaTokens.viewerSheetPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = accent.secondary,
                                modifier = Modifier.size(VennelaTokens.iconSm)
                            )
                            Spacer(Modifier.width(VennelaTokens.spaceSm))
                        }
                        Text(
                            text = title,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.caption,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!trailing.isNullOrBlank()) {
                            Text(
                                text = trailing,
                                color = palette.textTertiary,
                                fontSize = VennelaTokens.nano,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    content()
                }
            }
        }
    }
}

/** Small status text used inside compact trays. */
@Composable
fun ViewerHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalVennelaPalette.current.textTertiary,
        fontSize = VennelaTokens.nano,
        modifier = modifier
    )
}

/** A compact accent dot used as an inline bullet in viewer chrome. */
@Composable
fun ViewerDot(color: Color = LocalVennelaAccent.current.secondary, size: Int = 6) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}
