package com.vennela365.viewer.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Google Android 16 Material 3 Expressive + Glassmorphic Shape Scale
// ---------------------------------------------------------------------------

val VennelaExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ---------------------------------------------------------------------------
// Google Android 16 Material 3 Expressive Base Color Schemes
// ---------------------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = MoonBlue,
    secondary = NeonViolet,
    tertiary = CatViolet,
    background = MoonDeepDark,
    surface = MoonSurfaceDark,
    surfaceVariant = elevatedNavySurface,
    onPrimary = Color(0xFF001F2A),
    onSecondary = Color(0xFF1A0040),
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = MoonGlassBorder,
    outlineVariant = Color(0x2800D4FF),
    error = errorCoral,
    onError = Color.White,
    errorContainer = errorContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006782),
    secondary = Color(0xFF5B43D6),
    tertiary = Color(0xFF7C3AED),
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
// VennelaTheme — Android 16 Material 3 Expressive + Frosted Glassmorphism
// ---------------------------------------------------------------------------

@Composable
fun VennelaTheme(
    activeTheme: AppTheme = AppTheme.LUNAR_NIGHT,
    themeMode: ThemeMode = ThemeMode.DARK,
    accent: AccentOption? = null,
    fontStyle: AppFontStyle = AppFontStyle.ONEPLUS_SANS,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val resolvedAccent = accent?.toAccent() ?: activeTheme.toAccent()
    val palette = (if (isDark) DarkVennelaPalette else LightVennelaPalette)
        .tinted(resolvedAccent)

    // Harmonize Android 12+ Dynamic Color Scheme with our Material 3 Expressive Tonal Palette
    val dynamicBase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrNull()
    } else null

    val baseScheme = dynamicBase ?: if (isDark) DarkColorScheme else LightColorScheme
    val primaryContainer = if (isDark) {
        lerp(palette.cardSurfaceStrong, resolvedAccent.primary, 0.32f)
    } else {
        lerp(Color.White, resolvedAccent.primary, 0.16f)
    }
    val secondaryContainer = if (isDark) {
        lerp(palette.cardSurfaceStrong, resolvedAccent.secondary, 0.28f)
    } else {
        lerp(Color.White, resolvedAccent.secondary, 0.14f)
    }

    val colorScheme = baseScheme.copy(
        primary = resolvedAccent.primary,
        onPrimary = if (isDark) Color(0xFF04131D) else Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = if (isDark) Color.White else palette.textPrimary,
        secondary = resolvedAccent.secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = if (isDark) Color.White else palette.textPrimary,
        tertiary = lerp(resolvedAccent.primary, resolvedAccent.secondary, 0.5f),
        background = palette.backgroundTop,
        onBackground = palette.textPrimary,
        surface = palette.backgroundMid,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.cardSurfaceStrong,
        onSurfaceVariant = palette.textSecondary,
        surfaceContainerLowest = palette.backgroundBottom,
        surfaceContainerLow = palette.backgroundMid,
        surfaceContainer = palette.cardSurface,
        surfaceContainerHigh = palette.cardSurfaceStrong,
        surfaceContainerHighest = lerp(palette.cardSurfaceStrong, resolvedAccent.primary, 0.14f),
        outline = palette.cardBorder,
        outlineVariant = palette.cardBorder.copy(alpha = 0.45f)
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
            shapes = VennelaExpressiveShapes,
            typography = vennelaTypographyFor(fontStyle),
            content = content
        )
    }
}

// ---------------------------------------------------------------------------
// glassCard — Android 16 Material 3 Expressive Frosted Glass Card
// ---------------------------------------------------------------------------

fun Modifier.glassCard(
    cornerRadius: Dp,
    backgroundColor: Color = Color(0x2E1A2030),
    borderColor: Color = Color(0x4D00D4FF),
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
            ambientColor = Color(0x4000D4FF),
            spotColor = Color(0x308B5CF6)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.34f),
                    backgroundColor.copy(alpha = 0.22f),
                    backgroundColor.copy(alpha = 0.14f)
                )
            )
        )
        .border(
            borderWidth,
            Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = VennelaTokens.liquidBorderAlphaBright),
                    borderColor.copy(alpha = 0.32f),
                    borderColor.copy(alpha = VennelaTokens.liquidBorderAlphaFade)
                )
            ),
            shape
        )
}

fun Modifier.glassPill(
    shape: Shape = RoundedCornerShape(50),
    backgroundColor: Color = Color(0x301A2030),
    borderColor: Color = Color(0x4D00D4FF)
): Modifier = composed {
    this
        .clip(shape)
        .background(
            Brush.horizontalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.32f),
                    backgroundColor.copy(alpha = 0.18f)
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
