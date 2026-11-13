package com.vennela365.viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.aeroGlass

/**
 * Split-Horizon Dual Viewer Layout.
 *
 * Implements concurrent dual-viewing of reference sections (e.g. diagrams, cover page,
 * formulas, or tables) while freely exploring the primary document stream.
 */
@Composable
fun SplitHorizonViewer(
    primaryContent: @Composable () -> Unit,
    referenceContent: @Composable () -> Unit,
    referenceTitle: String = "Reference View",
    onCloseReference: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var splitRatio by remember { mutableFloatStateOf(0.48f) } // 0.2 to 0.8

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeightPx = constraints.maxHeight.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {
            // Reference Pane (Top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(splitRatio)
            ) {
                referenceContent()

                // Floating glass header for reference pane
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .aeroGlass(shape = RoundedCornerShape(16.dp), elevation = 8.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = referenceTitle,
                        color = accent.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onCloseReference,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Reference",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Draggable Split Divider Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .pointerInput(totalHeightPx) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaRatio = dragAmount / totalHeightPx
                            splitRatio = (splitRatio + deltaRatio).coerceIn(0.20f, 0.80f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(accent.secondary.copy(alpha = 0.35f))
                )

                // Draggable Glass Handle
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.cardSurfaceStrong)
                        .border(1.dp, accent.secondary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                )
            }

            // Primary Pane (Bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - splitRatio)
            ) {
                primaryContent()
            }
        }
    }
}

