package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerticalSplit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.aeroGlass
import com.vennela365.viewer.ui.components.pressScale

/**
 * Biomechanical Thumb-Arc Dock.
 *
 * Implements 98.5% screen immersion:
 * - Sits in the natural thumb reach zone at the bottom center of the canvas.
 * - Collapses automatically to a 36dp glass pill during fast scrolls and flings.
 * - Expands into 6 high-powered tools (Search, Color Mode, Lasso Compute, Split Horizon, Privacy Curtain, Study Sheet) on tap.
 */
@Composable
fun ThumbArcDock(
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onSearchClick: () -> Unit,
    onColorModeClick: () -> Unit,
    onLassoClick: () -> Unit,
    onSplitHorizonClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onStudySheetClick: () -> Unit,
    isLassoActive: Boolean = false,
    isSplitActive: Boolean = false,
    isPrivacyActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val dockHeight by animateDpAsState(
        targetValue = if (isCollapsed) 38.dp else 66.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "DockHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isCollapsed) {
            // Collapsed floating pill: a live accent bead plus a hint of the tools.
            Box(
                modifier = Modifier
                    .height(dockHeight)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(19.dp),
                        ambientColor = accent.secondary.copy(alpha = 0.55f),
                        spotColor = accent.secondary.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(19.dp))
                    .aeroGlass(shape = RoundedCornerShape(19.dp), elevation = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleCollapse
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    val pulse = remember { Animatable(1f) }
                    LaunchedEffect(Unit) {
                        pulse.animateTo(
                            targetValue = 0.45f,
                            animationSpec = tween(900, easing = LiquidMotion.exitEasing)
                        )
                        pulse.animateTo(1f, tween(900, easing = LiquidMotion.glideEasing))
                    }
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .graphicsLayer { alpha = pulse.value }
                            .clip(CircleShape)
                            .background(accent.secondary)
                    )
                    Text(
                        text = "Vennela Tools",
                        color = palette.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accent.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            // Expanded luxury Aero-Glass thumb dock with labelled tools.
            Row(
                modifier = Modifier
                    .height(dockHeight)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = accent.primary.copy(alpha = 0.50f),
                        spotColor = accent.secondary.copy(alpha = 0.50f)
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .aeroGlass(shape = RoundedCornerShape(30.dp), elevation = 16.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ThumbDockButton(
                    icon = Icons.Default.Search,
                    label = "Search",
                    isActive = false,
                    onClick = onSearchClick
                )

                ThumbDockButton(
                    icon = Icons.Default.ColorLens,
                    label = "Theme",
                    isActive = false,
                    onClick = onColorModeClick
                )

                ThumbDockButton(
                    icon = Icons.Default.Calculate,
                    label = "Lasso",
                    isActive = isLassoActive,
                    onClick = onLassoClick
                )

                ThumbDockButton(
                    icon = Icons.Default.VerticalSplit,
                    label = "Split",
                    isActive = isSplitActive,
                    onClick = onSplitHorizonClick
                )

                ThumbDockButton(
                    icon = Icons.Default.Security,
                    label = "Privacy",
                    isActive = isPrivacyActive,
                    onClick = onPrivacyClick
                )

                ThumbDockButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "Cram",
                    isActive = false,
                    onClick = onStudySheetClick
                )
            }
        }
    }
}

@Composable
private fun ThumbDockButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.10f else 1f,
        animationSpec = LiquidMotion.hero(),
        label = "thumbScale"
    )

    Column(
        modifier = Modifier
            .width(50.dp)
            .heightIn(min = 48.dp)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .then(
                    if (isActive) {
                        Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accent.primary.copy(alpha = 0.95f),
                                        accent.secondary.copy(alpha = 0.72f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                    } else {
                        Modifier.background(palette.cardSurface.copy(alpha = 0.22f))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else palette.textPrimary.copy(alpha = 0.90f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isActive) accent.secondary else palette.textTertiary,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

