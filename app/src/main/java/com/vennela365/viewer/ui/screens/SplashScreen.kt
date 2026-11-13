package com.vennela365.viewer.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.R
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(viewModel: MainViewModel) {
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current
    val scrim = palette.backgroundBottom
    var entered by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(LiquidMotion.durationHero, easing = LiquidMotion.glideEasing),
        label = "splashAlpha"
    )
    val contentRise by animateFloatAsState(
        targetValue = if (entered) 0f else 32f,
        animationSpec = tween(LiquidMotion.durationHero, easing = LiquidMotion.emphasisEasing),
        label = "splashRise"
    )
    val exitAlpha by animateFloatAsState(
        targetValue = if (leaving) 0f else 1f,
        animationSpec = tween(LiquidMotion.durationBase, easing = LiquidMotion.exitEasing),
        label = "splashExit"
    )

    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val sheenX by shimmer.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen"
    )
    val moonPulse by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moonPulse"
    )
    val markHalo by shimmer.animateFloat(
        initialValue = 0.52f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markHalo"
    )
    val ringProgress by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringProgress"
    )
    val ring2Progress by shimmer.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Progress"
    )
    val sweepX by shimmer.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = LiquidMotion.shimmer(3400),
            repeatMode = RepeatMode.Restart
        ),
        label = "markSweep"
    )

    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim)
            .graphicsLayer { alpha = exitAlpha }
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_lunar_lake),
            contentDescription = "Moonlit lake at dusk",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to scrim.copy(alpha = 0.36f),
                        0.44f to scrim.copy(alpha = 0.08f),
                        0.72f to scrim.copy(alpha = 0.62f),
                        1f to scrim.copy(alpha = 0.92f)
                    )
                )
        )

        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = VennelaTokens.spaceXxl)
                .padding(bottom = navInset + VennelaTokens.spaceXxl)
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentRise
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.weight(0.95f))
            SplashMoonMark(
                pulse = moonPulse,
                halo = markHalo,
                ringProgress = ringProgress,
                ring2Progress = ring2Progress,
                sweepX = sweepX
            )
            Spacer(modifier = Modifier.weight(1.05f))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White,
                                    accent.secondary,
                                    Color.White
                                )
                            )
                        )
                    ) {
                        append("Vennela 365")
                    }
                },
                fontSize = 44.sp,
                fontFamily = VennelaFonts.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = accent.secondary.copy(alpha = 0.55f),
                        blurRadius = 24f
                    )
                )
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, accent.secondary.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Text(
                text = "UNIVERSAL FILE VIEWER",
                color = palette.textSecondary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.2.sp
            )

            Spacer(modifier = Modifier.height(VennelaTokens.spaceHuge))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(60.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(VennelaTokens.radiusPill),
                        ambientColor = accent.primary.copy(alpha = 0.55f),
                        spotColor = accent.primary.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.94f),
                                accent.primary,
                                accent.secondary.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.55f), accent.secondary.copy(alpha = 0.35f))
                        ),
                        RoundedCornerShape(VennelaTokens.radiusPill)
                    )
                    .pressScale(pressedScale = 0.975f) {
                        leaving = true
                        viewModel.navigateTo(Screen.Home)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * sheenX }
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = VennelaTokens.spaceXxl, end = VennelaTokens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        text = "Get Started",
                        color = Color.White,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Get Started",
                            tint = Color.White,
                            modifier = Modifier.size(VennelaTokens.iconMd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))

            Text(
                text = "Offline • Private • Instant Preview",
                color = palette.textTertiary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp
            )
        }

        // Gentle auto-advance so the first run is never a dead end.
        LaunchedEffect(Unit) {
            delay(4200)
            if (!leaving) {
                leaving = true
                delay(LiquidMotion.durationBase.toLong())
                viewModel.navigateTo(Screen.Home)
            }
        }
    }
}

/**
 * The cinematic moon mark: a breathing neon glow, a hairline ring that keeps
 * expanding past the disc, and a slow specular sweep across it. Purely
 * decorative — it reads the values the splash already animates.
 */
@Composable
private fun SplashMoonMark(
    pulse: Float,
    halo: Float,
    ringProgress: Float,
    ring2Progress: Float,
    sweepX: Float
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val disc = VennelaTokens.badgeLg * 2 + VennelaTokens.spaceMd
    val shape = CircleShape

    Box(
        modifier = Modifier.size(disc + VennelaTokens.spaceXxl * 2),
        contentAlignment = Alignment.Center
    ) {
        // Widest soft aura.
        Box(
            modifier = Modifier
                .size(disc * 2f)
                .graphicsLayer { alpha = 0.22f + 0.20f * pulse }
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.secondary.copy(alpha = 0.55f),
                            accent.primary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Two rings travelling outward on the same slow beat.
        listOf(ringProgress, ring2Progress).forEachIndexed { index, progress ->
            val t = progress.coerceIn(0f, 1.5f)
            if (t <= 1f) {
                Box(
                    modifier = Modifier
                        .size(disc)
                        .graphicsLayer {
                            val s = 1f + 0.62f * t
                            scaleX = s
                            scaleY = s
                            alpha = (1f - t) * if (index == 0) 0.60f else 0.42f
                        }
                        .border(1.5.dp, accent.secondary, shape)
                )
            }
        }

        // The mark itself.
        Box(
            modifier = Modifier
                .size(disc)
                .shadow(
                    elevation = 8.dp + 22.dp * pulse,
                    shape = shape,
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.55f),
                    spotColor = accent.primary.copy(alpha = 0.65f)
                )
                .clip(shape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.50f),
                            accent.secondary.copy(alpha = 0.24f),
                            palette.backgroundBottom.copy(alpha = 0.35f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            accent.secondary.copy(alpha = 0.25f)
                        )
                    ),
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = "Vennela moon",
                tint = Color(0xFFFDE68A),
                modifier = Modifier
                    .size(disc * 0.52f)
                    .graphicsLayer {
                        scaleX = 0.96f + 0.06f * pulse
                        scaleY = 0.96f + 0.06f * pulse
                    }
            )
            // Looping shimmer sweep across the disc.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = size.width * sweepX }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = halo),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
