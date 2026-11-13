package com.vennela365.viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens

/**
 * Present mode: one slide, edge to edge, no app chrome.
 *
 * This is the deck equivalent of Google Slides' presentation view. The slide is
 * drawn on the paper surface, the left third taps back, the rest advances, and
 * only a hairline transport remains at the bottom. Nothing here invents slide
 * content: it is the same parsed title and body the normal canvas shows.
 */
@Composable
fun PresentationStage(
    title: String,
    body: List<String>,
    number: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val ink = palette.textPrimary
    val muted = palette.textSecondary
    val paper = palette.cardSurfaceStrong
    val stageScrim = palette.backgroundBottom.copy(alpha = 0.97f)

    Box(
        Modifier
            .fillMaxSize()
            .background(stageScrim)
    ) {
        // Tap zones: back on the left third, advance everywhere else.
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(0.32f)
                    .fillMaxSize()
                    .clickable(onClick = onPrevious)
            )
            Box(
                Modifier
                    .weight(0.68f)
                    .fillMaxSize()
                    .clickable(onClick = onNext)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                    .background(paper)
                    .border(
                        1.dp,
                        accent.secondary.copy(alpha = 0.20f),
                        RoundedCornerShape(VennelaTokens.radiusCardLarge)
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = title.ifBlank { "Slide $number" },
                    color = ink,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (body.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                            .background(accent.primary.copy(alpha = 0.65f))
                    )
                    Spacer(Modifier.height(12.dp))
                }
                body.take(12).forEach { line ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accent.secondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = line,
                            color = muted,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Exit affordance and slide counter: quiet, top-right.
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$number / $total",
                color = muted,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.Medium
            )
            Box(
                Modifier
                    .size(VennelaTokens.viewerControl)
                    .clip(CircleShape)
                    .background(palette.cardSurface.copy(alpha = 0.55f))
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Exit presentation",
                    tint = ink,
                    modifier = Modifier.size(VennelaTokens.viewerIcon)
                )
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier
                    .size(VennelaTokens.viewerControl)
                    .clip(CircleShape)
                    .background(palette.cardSurface.copy(alpha = 0.45f))
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous slide",
                    tint = ink,
                    modifier = Modifier.size(VennelaTokens.viewerIcon)
                )
            }
            Box(
                Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(accent.primary.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((number.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(accent.secondary)
                )
            }
            Box(
                Modifier
                    .size(VennelaTokens.viewerControl)
                    .clip(CircleShape)
                    .background(palette.cardSurface.copy(alpha = 0.45f))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next slide",
                    tint = ink,
                    modifier = Modifier.size(VennelaTokens.viewerIcon)
                )
            }
        }
    }
}
