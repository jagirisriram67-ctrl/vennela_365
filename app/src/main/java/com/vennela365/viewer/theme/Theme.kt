package com.vennela365.viewer.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Nebula Flux Material 3 color schemes
// ---------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = MoonBlue,                          // Electric Cyan
    secondary = NeonViolet,                      // Aurora Violet
    tertiary = CatViolet,
    background = MoonDeepDark,                   // deep graphite #0C0F14
    surface = MoonSurfaceDark,                   // #141820
    surfaceVariant = elevatedNavySurface,         // #1A1F2A
    onPrimary = Color(0xFF001F2A),
    onSecondary = Color(0xFF1A0040),
    onTertiary = Color.White,
    onBackground = TextPrimary,                  // #F0F2F5
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,            // #8B95A5
    outline = MoonGlassBorder,                   // cyan rim 30% alpha
    outlineVariant = Color(0x1E00D4FF),          // subtle cyan veil
    error = errorCoral,
    onError = Color.White,
    errorContainer = errorContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0091C7),
    secondary = Color(0xFF7C3AED),
    tertiary = Color(0xFF8B5CF6),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightGlassBorder,
    outlineVariant = Color(0xFFD0D8E8),
    error = errorCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

// ---------------------------------------------------------------------------
// VennelaTheme — signature preserved exactly
// ---------------------------------------------------------------------------

@Composable
fun VennelaTheme(
    activeTheme: AppTheme = AppTheme.LUNAR_NIGHT,
    themeMode: ThemeMode = ThemeMode.DARK,
    accent: AccentOption? = null,
    fontStyle: AppFontStyle = AppFontStyle.ONEPLUS_SANS,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val resolvedAccent = accent?.toAccent() ?: activeTheme.toAccent()
    val palette = (if (isDark) DarkVennelaPalette else LightVennelaPalette)
        .tinted(resolvedAccent)

    val colorScheme = (if (isDark) DarkColorScheme else LightColorScheme).copy(
        primary = resolvedAccent.primary,
        secondary = resolvedAccent.secondary,
        background = palette.backgroundTop,
        surface = palette.backgroundMid
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            runCatching {
                val window = (view.context as? Activity)?.window ?: return@runCatching
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalVennelaPalette provides palette,
        LocalVennelaAccent provides resolvedAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = vennelaTypographyFor(fontStyle),
            content = content
        )
    }
}

// ---------------------------------------------------------------------------
// glassCard — Nebula Flux: liquid borders (bright→transparent diagonal),
// chromatic depth gradient body, and cyan/violet glow shadow.
// ---------------------------------------------------------------------------

fun Modifier.glassCard(
    cornerRadius: Dp,
    backgroundColor: Color = Color(0x2E1A2030),  // 18% graphite glass
    borderColor: Color = Color(0x4D00D4FF),       // cyan rim
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp
): Modifier = glassCard(
    shape = RoundedCornerShape(cornerRadius),
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    borderWidth = borderWidth,
    elevation = elevation
)

fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(VennelaTokens.radiusCard),
    backgroundColor: Color = Color(0x2E1A2030),
    borderColor: Color = Color(0x4D00D4FF),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp
): Modifier = composed {
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color(0x4000D4FF),     // cyan ambient glow
            spotColor = Color(0x308B5CF6)          // violet spot glow
        )
        .clip(shape)
        .background(
            // Chromatic depth: top surface is lighter/transparent, deeper is
            // more saturated — the hallmark Nebula Flux layering.
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.30f),
                    backgroundColor.copy(alpha = 0.18f),
                    backgroundColor.copy(alpha = 0.10f)
                )
            )
        )
        .border(
            borderWidth,
            // Liquid border: bright top-left flowing to transparent bottom-right
            Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = VennelaTokens.liquidBorderAlphaBright),
                    borderColor.copy(alpha = 0.30f),
                    borderColor.copy(alpha = VennelaTokens.liquidBorderAlphaFade)
                )
            ),
            shape
        )
}

// ---------------------------------------------------------------------------
// glassPill — Nebula Flux: more translucent, liquid gradient border
// ---------------------------------------------------------------------------

fun Modifier.glassPill(
    shape: Shape = RoundedCornerShape(50),
    backgroundColor: Color = Color(0x301A2030),   // 19% graphite glass
    borderColor: Color = Color(0x4D00D4FF)        // cyan rim
): Modifier = composed {
    this
        .clip(shape)
        .background(
            Brush.horizontalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.28f),
                    backgroundColor.copy(alpha = 0.16f)
                )
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = VennelaTokens.liquidBorderAlphaBright),
                    borderColor.copy(alpha = VennelaTokens.liquidBorderAlphaFade)
                )
            ),
            shape
        )
}
