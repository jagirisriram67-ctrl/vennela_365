package com.vennela365.viewer.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// NEBULA FLUX palette — Vennela 365's original design language.
//
// A deep graphite cosmos with chromatic depth layers, electric cyan and aurora
// violet accents, vivid neon category tones, and liquid-glass surfaces that
// shift from opaque at depth to crystalline at the surface. Every legacy
// token is preserved; new Nebula-specific tokens extend the vocabulary.
// ---------------------------------------------------------------------------

// Primary Nebula Background — deep graphite cosmos
val MoonDeepDark = Color(0xFF0C0F14)            // graphite abyss
val MoonSurfaceDark = Color(0xFF141820)         // depth layer 1
val MoonGlassCard = Color(0x2E1A2030)           // glass body (18% alpha)
val MoonGlassBorder = Color(0x4D00D4FF)         // cyan rim glow
val MoonGlassBorderActive = Color(0xA68B5CF6)   // violet active rim

val MoonBlue = Color(0xFF00D4FF)                // electric cyan (primary accent)
val MoonCyanGlow = Color(0xFF00E5FF)            // brighter cyan highlight
val MoonSky = Color(0xFF38DCFF)                 // sky-cyan wash

// Nebula Flux neon tokens — aurora mesh glow sources
val NeonBlue = Color(0xFF00BFFF)
val NeonCyan = Color(0xFF00FFEA)
val NeonViolet = Color(0xFF8B5CF6)
val NeonMagenta = Color(0xFFE040FB)
val NeonAmber = Color(0xFFFFAB00)
val NeonGreen = Color(0xFF00E676)
val NeonCoral = Color(0xFFFF5252)

// Category Accent Colors — vivid neon-shifted jewels
val CatBlue = Color(0xFF00B0FF)
val CatEmerald = Color(0xFF00E676)
val CatViolet = Color(0xFF8B5CF6)
val CatAmber = Color(0xFFFFAB00)
val CatRose = Color(0xFFFF4081)
val CatCyan = Color(0xFF00E5FF)
val CatOrange = Color(0xFFFF6D00)
val CatSlate = Color(0xFF78909C)

// Glass Highlights — chromatic depth layers
val GlassHighlight = Color(0x2EFFFFFF)          // 18% white
val GlassShadow = Color(0x4D000000)
val TextPrimary = Color(0xFFF0F2F5)
val TextSecondary = Color(0xFF8B95A5)
val TextMuted = Color(0xFF505868)

// Spec §4.1 named tokens — semantic aliases
val midnightBackground = MoonDeepDark
val deepNavySurface = MoonSurfaceDark
val elevatedNavySurface = Color(0xFF1A1F2A)     // depth layer 2
val glassSurface = Color(0x301A2030)            // glass body
val glassBorder = Color(0x4D00D4FF)             // cyan rim
val moonlightSilver = Color(0xFFF0F2F5)
val primaryIceBlue = MoonCyanGlow
val secondaryViolet = Color(0xFF8B5CF6)
val successTeal = Color(0xFF00E676)
val warningAmber = Color(0xFFFFAB00)
val errorCoral = Color(0xFFFF5252)
val textTertiary = TextMuted

// Semantic containers — deeper, more saturated
val successContainer = Color(0xFF0A2E1F)
val warningContainer = Color(0xFF332200)
val errorContainer = Color(0xFF3B0A0A)
val selectionGlow = Color(0xFF00D4FF)
val focusRing = Color(0xFF8B5CF6)
val disabledControl = Color(0xFF2A2F3A)
val unavailableContent = Color(0xFF3D4556)

// Light theme — intentionally designed Nebula daytime, not inverted dark
val LightBackground = Color(0xFFF4F6FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEAEDF5)
val LightGlassCard = Color(0xCCFFFFFF)
val LightGlassBorder = Color(0xFFD0D8E8)
val LightTextPrimary = Color(0xFF0C0F14)
val LightTextSecondary = Color(0xFF505868)
val LightTextTertiary = Color(0xFF8B95A5)

// Theme enum for Theme Selection Screen
enum class AppTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val isDark: Boolean = true
) {
    LUNAR_NIGHT("01", "Lunar Night", "Default theme", Color(0xFF00D4FF), Color(0xFF8B5CF6), true),
    SUNRISE_GLOW("02", "Sunrise Glow", "Warm and peaceful", Color(0xFFFF6D00), Color(0xFFFFAB00), true),
    FOREST_GREEN("03", "Forest Green", "Fresh and natural", Color(0xFF00E676), Color(0xFF69F0AE), true),
    OCEAN_BLUE("04", "Ocean Blue", "Calm and focused", Color(0xFF00B0FF), Color(0xFF00E5FF), true),
    SAKURA_PINK("05", "Sakura Pink", "Soft and elegant", Color(0xFFFF4081), Color(0xFFFF80AB), true),
    AURORA("06", "Aurora", "Vibrant and colorful", Color(0xFF8B5CF6), Color(0xFF00E676), true),
    MINIMAL_LIGHT("07", "Minimal Light", "Clean and modern", Color(0xFF2979FF), Color(0xFF00B0FF), false),
    MINIMAL_DARK("08", "Minimal Dark", "Sleek and premium", Color(0xFF78909C), Color(0xFF8B95A5), true),
    SUNSET("09", "Sunset", "Bold and energetic", Color(0xFFFF6D00), Color(0xFFFF4081), true),
    GALAXY("10", "Galaxy", "Deep and immersive", Color(0xFF8B5CF6), Color(0xFFB388FF), true);
}
