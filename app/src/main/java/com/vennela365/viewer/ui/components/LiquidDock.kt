package com.vennela365.viewer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * Apple iOS 26 Liquid Glass Floating Tab Bar (Menubar).
 *
 * Features:
 * - Translucent multi-stop refractive glass bar (`62.dp` height, `31.dp` pill radius)
 *   with top-edge optical specular rim and soft elevation shadow.
 * - Spring-physics sliding **Liquid Glass Lens** indicator that refracts the active
 *   accent with a luminous glass top sheen rather than a heavy opaque block.
 * - Distinct Filled vs Outlined SF-inspired iconography and balanced `10.5.sp`
 *   labels that never clip or crowd.
 */
@Composable
fun FloatingDockNav(
    activeTab: String,
    onTabSelected: (String) -> Unit = {}
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(VennelaTokens.radiusDock)

    val spec = LiquidGlassSpec(
        tint = if (palette.isDark) {
            Color(0xFF161D31).copy(alpha = 0.80f)
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        tintEnd = if (palette.isDark) {
            Color(0xFF0D1222).copy(alpha = 0.74f)
        } else {
            palette.cardSurface.copy(alpha = 0.80f)
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
            Color.Black.copy(alpha = 0.56f)
        } else {
            palette.backgroundBottom.copy(alpha = 0.16f)
        },
        highlight = 0.22f
    )

    val activeIndex = dockEntries.indexOfFirst { it.id == activeTab }.coerceAtLeast(0)

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
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(62.dp)
                .liquidGlass(spec, shape)
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            val slotWidth = maxWidth / dockEntries.size
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * activeIndex,
                animationSpec = LiquidMotion.liquid(),
                label = "dockIndicatorOffset"
            )

            // Refractive iOS 26 Liquid Glass Lens pill for the active tab
            val lensShape = RoundedCornerShape(26.dp)
            val lensFillBrush = if (palette.isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        accent.primary.copy(alpha = 0.34f),
                        accent.secondary.copy(alpha = 0.22f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        accent.primary.copy(alpha = 0.20f),
                        accent.secondary.copy(alpha = 0.14f)
                    )
                )
            }
            val lensRimBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (palette.isDark) 0.58f else 0.95f),
                    accent.secondary.copy(alpha = if (palette.isDark) 0.42f else 0.35f),
                    Color.White.copy(alpha = 0.14f)
                )
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .height(52.dp)
                    .padding(horizontal = 2.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = lensShape,
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.36f),
                        spotColor = accent.secondary.copy(alpha = 0.36f)
                    )
                    .clip(lensShape)
                    .background(lensFillBrush)
                    .drawWithContent {
                        drawContent()
                        val sheenH = size.height * 0.45f
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.45f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = sheenH
                            ),
                            topLeft = Offset.Zero,
                            size = Size(size.width, sheenH)
                        )
                    }
                    .border(
                        width = 1.dp,
                        brush = lensRimBrush,
                        shape = lensShape
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockEntries.forEach { entry ->
                    DockItem(
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
}

@Composable
private fun DockItem(
    entry: DockEntry,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.91f
            isActive -> 1.0f
            else -> 1.0f
        },
        animationSpec = LiquidMotion.press(),
        label = "dockItemScale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.06f else 1.0f,
        animationSpec = LiquidMotion.liquid(),
        label = "dockIconScale"
    )

    val activeIconTint = if (palette.isDark) Color.White else accent.primary
    val activeLabelTint = if (palette.isDark) Color.White else palette.textPrimary
    val inactiveTint = palette.textSecondary.copy(alpha = if (palette.isDark) 0.78f else 0.72f)

    val iconTint by animateColorAsState(
        targetValue = if (isActive) activeIconTint else inactiveTint,
        animationSpec = androidx.compose.animation.core.tween(LiquidMotion.durationFast),
        label = "dockIconTint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (isActive) activeLabelTint else inactiveTint,
        animationSpec = androidx.compose.animation.core.tween(LiquidMotion.durationFast),
        label = "dockLabelTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(26.dp))
            .semantics {
                selected = isActive
                contentDescription = entry.label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = entry.label,
            color = labelTint,
            fontFamily = VennelaFonts.Sans,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.1).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
