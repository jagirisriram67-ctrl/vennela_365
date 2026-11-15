package com.vennela365.viewer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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

private data class DockEntry(
    val id: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val dockEntries = listOf(
    DockEntry("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    DockEntry("files", "Files", Icons.Filled.Folder, Icons.Outlined.Folder),
    DockEntry("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    DockEntry("favorites", "Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    DockEntry("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * Google Android 16 Material 3 Expressive + Glassmorphic Navigation Bar.
 *
 * Features:
 * - Authentic Material 3 Expressive `64.dp x 32.dp` **Stadium Active Pill Indicator**
 *   that spring-expands horizontally around the active icon (`primaryContainer` + frosted
 *   glass rim), paired with a crisp M3 label underneath — just like Google Files & Pixel UI.
 * - Full Material 3 Ripple feedback + hyper-reactive spring bounce on press and selection.
 * - Frosted glassmorphic M3 `surfaceContainerHigh` bar (`68.dp` height, `28.dp` M3 Extra-Large corner radius).
 */
@Composable
fun FloatingDockNav(
    activeTab: String,
    onTabSelected: (String) -> Unit = {}
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(28.dp)

    val tonalTint = lerp(
        palette.cardSurfaceStrong,
        accent.primary,
        if (palette.isDark) 0.12f else 0.06f
    )

    val spec = LiquidGlassSpec(
        tint = if (palette.isDark) {
            tonalTint.copy(alpha = 0.88f)
        } else {
            Color.White.copy(alpha = 0.92f)
        },
        tintEnd = if (palette.isDark) {
            palette.cardSurface.copy(alpha = 0.84f)
        } else {
            tonalTint.copy(alpha = 0.88f)
        },
        frost = VennelaTokens.frostHeavy,
        backdropAlpha = 0.88f,
        rim = if (palette.isDark) {
            accent.primary.copy(alpha = 0.34f)
        } else {
            accent.primary.copy(alpha = 0.24f)
        },
        rimWidth = 1.dp,
        sheen = 0.22f,
        elevation = VennelaTokens.elevationDock,
        shadowColor = if (palette.isDark) {
            Color.Black.copy(alpha = 0.56f)
        } else {
            accent.primary.copy(alpha = 0.16f)
        },
        highlight = 0.18f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = 2.dp,
                bottom = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(68.dp)
                .liquidGlass(spec, shape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockEntries.forEach { entry ->
                M3ExpressiveNavBarItem(
                    entry = entry,
                    isActive = activeTab == entry.id,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dock_tab_${entry.id}"),
                    onClick = { onTabSelected(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun M3ExpressiveNavBarItem(
    entry: DockEntry,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "m3NavItemScale"
    )

    // Material 3 Expressive Stadium Pill width expansion (32.dp -> 58.dp)
    val pillWidth by animateDpAsState(
        targetValue = if (isActive) 56.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 460f
        ),
        label = "m3NavPillWidth"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "m3NavPillAlpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            isActive -> 1.10f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = 0.54f,
            stiffness = 500f
        ),
        label = "m3NavIconScale"
    )

    val activeIconTint = if (palette.isDark) Color.White else accent.primary
    val activeLabelTint = if (palette.isDark) accent.secondary else accent.primary
    val inactiveTint = palette.textSecondary.copy(alpha = if (palette.isDark) 0.82f else 0.76f)

    val iconTint by animateColorAsState(
        targetValue = if (isActive) activeIconTint else inactiveTint,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "m3NavIconTint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (isActive) activeLabelTint else inactiveTint,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "m3NavLabelTint"
    )

    val pillFillBrush = Brush.horizontalGradient(
        colors = if (palette.isDark) {
            listOf(
                accent.primary.copy(alpha = 0.42f * pillAlpha),
                accent.secondary.copy(alpha = 0.32f * pillAlpha)
            )
        } else {
            listOf(
                accent.primary.copy(alpha = 0.22f * pillAlpha),
                accent.secondary.copy(alpha = 0.16f * pillAlpha)
            )
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(RoundedCornerShape(18.dp))
            .semantics {
                selected = isActive
                contentDescription = entry.label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accent.primary),
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 2.dp)
    ) {
        // Material 3 Expressive 30.dp Stadium Indicator Pill around Icon
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(30.dp)
                .clip(CircleShape)
                .background(pillFillBrush, CircleShape)
                .then(
                    if (isActive) {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (palette.isDark) 0.46f else 0.85f),
                                    accent.secondary.copy(alpha = 0.50f)
                                )
                            ),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) entry.activeIcon else entry.inactiveIcon,
                contentDescription = entry.label,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = entry.label,
            color = labelTint,
            fontFamily = VennelaFonts.OnePlusDisplay,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
