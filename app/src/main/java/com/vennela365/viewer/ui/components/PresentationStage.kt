package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Present mode: one slide, edge to edge, no app chrome.
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
    val stageScrim = palette.backgroundBottom.copy(alpha = 0.98f)

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timerText = remember(mins, secs) {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(stageScrim)
            .pointerInput(number, total) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 72.dp.toPx()
                        if (dragAccumulated < -threshold && number < total) {
                            onNext()
                        } else if (dragAccumulated > threshold && number > 1) {
                            onPrevious()
                        }
                        dragAccumulated = 0f
                    }
                )
            }
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = Triple(number, title, body),
                transitionSpec = {
                    val forward = targetState.first >= initialState.first
                    (slideInHorizontally(LiquidMotion.liquid()) { if (forward) it / 4 else -it / 4 } +
                        fadeIn(LiquidMotion.glide()))
                        .togetherWith(
                            slideOutHorizontally(LiquidMotion.glide()) { if (forward) -it / 5 else it / 5 } +
                                fadeOut(LiquidMotion.exit())
                        )
                },
                label = "stageSlideTransition"
            ) { (_, slideTitle, slideBody) ->
                Column(
                    Modifier
                        .widthIn(max = 920.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                        .background(paper)
                        .border(
                            1.dp,
                            accent.secondary.copy(alpha = 0.24f),
                            RoundedCornerShape(VennelaTokens.radiusCardLarge)
                        )
                        .padding(horizontal = 28.dp, vertical = 22.dp)
                ) {
                    Text(
                        text = slideTitle.ifBlank { "Slide $number" },
                        color = ink,
                        fontFamily = VennelaFonts.Serif,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (slideBody.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                .background(accent.primary.copy(alpha = 0.7f))
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    slideBody.take(12).forEach { line ->
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
                                fontFamily = VennelaFonts.Sans,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Top timer pill on left
        Row(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(palette.cardSurface.copy(alpha = 0.55f))
                .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusPill))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = timerText,
                color = ink,
                fontFamily = VennelaFonts.Mono,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Exit affordance and slide counter: quiet, top-right.
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$number / $total",
                color = muted,
                fontFamily = VennelaFonts.Mono,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.Medium
            )
            Box(
                Modifier
                    .size(VennelaTokens.touchTargetMin)
                    .clip(CircleShape)
                    .background(palette.cardSurface.copy(alpha = 0.55f))
                    .testTag("stage_exit_button")
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
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier
                    .size(VennelaTokens.touchTargetMin)
                    .clip(CircleShape)
                    .background(palette.cardSurface.copy(alpha = 0.45f))
                    .testTag("stage_prev_button")
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
                    .height(3.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(accent.primary.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((number.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(accent.secondary)
                )
            }
            Box(
                Modifier
                    .size(VennelaTokens.touchTargetMin)
                    .clip(CircleShape)
                    .background(palette.cardSurface.copy(alpha = 0.45f))
                    .testTag("stage_next_button")
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

