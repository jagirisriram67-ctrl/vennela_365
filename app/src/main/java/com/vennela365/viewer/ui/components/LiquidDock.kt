package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidGlassSpec
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass

private data class DockTab(
    val id: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector
)

private val dockTabs = listOf(
    DockTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    DockTab("files", "Files", Icons.Filled.Folder, Icons.Outlined.FolderOpen),
    DockTab("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    DockTab("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    DockTab("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * The floating bottom dock — the app's primary menubar.
 *
 * A single neon gradient lozenge *slides* between tabs instead of snapping,
 * the active icon morphs outlined -> filled with a spring overshoot, and the
 * whole bar rises into view when its screen opens. Touching a tab delivers a
 * light haptic tick so the interaction is felt as well as seen.
 */
@Composable
fun FloatingDockNav(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val haptics = LocalHapticFeedback.current

    val spec = LiquidGlassSpec(
        tint = palette.cardSurfaceStrong.copy(alpha = 0.34f),
        tintEnd = palette.cardSurface.copy(alpha = 0.26f),
        frost = VennelaTokens.frostHeavy,
        backdropAlpha = 0.68f,
        rim = accent.secondary.copy(alpha = if (palette.isDark) 0.40f else 0.46f),
        rimWidth = 1.dp,
        sheen = 0.20f,
        elevation = VennelaTokens.elevationDock,
        shadowColor = palette.backgroundBottom.copy(alpha = if (palette.isDark) 0.72f else 0.20f),
        highlight = 0.14f
    )

    // The bar rises into place once per screen open.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = LiquidMotion.shared()
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = (1f - entrance.value) * 26f
                alpha = 0.35f + 0.65f * entrance.value
            }
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceXxs)
            // Explicit height: the sliding indicator uses fillMaxHeight, so the
            // container must define its own height or the glass would stretch
            // to the whole screen.
            .height(48.dp)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusDock))
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        val itemWidth = maxWidth / dockTabs.size
        val selectedIndex = dockTabs.indexOfFirst { it.id == activeTab }.coerceAtLeast(0)

        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = LiquidMotion.shared(),
            label = "dockIndicatorX"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // Sliding neon lozenge.
            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = accent.secondary.copy(alpha = 0.55f),
                        spotColor = accent.secondary.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.primary,
                                accent.primary.copy(alpha = 0.86f),
                                accent.secondary
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
            )

            // Tabs.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockTabs.forEach { tab ->
                    DockItem(
                        tab = tab,
                        selected = activeTab == tab.id,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (activeTab != tab.id) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    tab: DockTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val interaction = remember { MutableInteractionSource() }

    val lift by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium),
        label = "dockLift"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = LiquidMotion.hero(),
        label = "dockIconScale"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White else palette.dockTextMuted,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "dockLabelColor"
    )

    Column(
        modifier = modifier
            .height(42.dp)
            .offset(y = lift)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (scaleIn(initialScale = 0.5f, animationSpec = LiquidMotion.press()) +
                    fadeIn(tween(LiquidMotion.durationFast))) togetherWith
                    (scaleOut(targetScale = 0.55f, animationSpec = LiquidMotion.exit()) +
                        fadeOut(tween(LiquidMotion.durationInstant)))
            },
            label = "dockIconMorph"
        ) { sel ->
            Icon(
                imageVector = if (sel) tab.filled else tab.outlined,
                contentDescription = tab.label,
                tint = if (sel) Color.White else palette.dockTextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
