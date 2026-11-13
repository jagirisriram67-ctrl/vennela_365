package com.vennela365.viewer.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Dark / Light / follow-system preference (persisted in DataStore). */
enum class ThemeMode(val id: String, val label: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    SYSTEM("system", "System");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: DARK
    }
}

/** Six hand-tuned accent swatches — Nebula Flux neon palette. */
enum class AccentOption(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color
) {
    ICE_BLUE("ice_blue", "Ice Blue", Color(0xFF00D4FF), Color(0xFF00E5FF)),
    MOON_VIOLET("moon_violet", "Violet", Color(0xFF8B5CF6), Color(0xFFB388FF)),
    SAKURA("sakura", "Magenta", Color(0xFFFF4081), Color(0xFFFF80AB)),
    SUNSET("sunset", "Orange", Color(0xFFFF6D00), Color(0xFFFFAB00)),
    EMERALD("emerald", "Emerald", Color(0xFF00E676), Color(0xFF69F0AE)),
    AURORA("aurora", "Aurora", Color(0xFF00BCD4), Color(0xFF00FFEA));

    companion object {
        fun fromId(id: String?): AccentOption = entries.firstOrNull { it.id == id } ?: ICE_BLUE
    }
}

/**
 * A resolved accent pair. Settings swatches and Theme Gallery entries both
 * collapse into this, so every screen can read one type regardless of which
 * of the two the user picked.
 */
data class VennelaAccent(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color
)

fun AccentOption.toAccent(): VennelaAccent = VennelaAccent(id, label, primary, secondary)

fun AppTheme.toAccent(): VennelaAccent = VennelaAccent(id, title, primaryAccent, secondaryAccent)

/**
 * Theme-aware tokens — Nebula Flux chromatic depth layers. Screens read these
 * instead of hardcoding any hex values.
 */
data class VennelaPalette(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cardSurface: Color,
    val cardSurfaceStrong: Color,
    val cardBorder: Color,
    val dockSurfaceTop: Color,
    val dockSurfaceBottom: Color,
    val dockText: Color,
    val dockTextMuted: Color
)

val DarkVennelaPalette = VennelaPalette(
    isDark = true,
    backgroundTop = Color(0xFF0C0F14),           // graphite abyss
    backgroundMid = Color(0xFF141820),           // depth layer 1
    backgroundBottom = Color(0xFF0A0D12),        // deepest void
    textPrimary = Color(0xFFF0F2F5),
    textSecondary = Color(0xFF8B95A5),
    textTertiary = Color(0xFF505868),
    cardSurface = Color(0xD91A1F2A),             // 85% layer-2 graphite
    cardSurfaceStrong = Color(0xF2222834),        // 95% layer-3
    cardBorder = Color(0x4D00D4FF),              // cyan rim 30%
    dockSurfaceTop = Color(0xF2222834),          // layer-3
    dockSurfaceBottom = Color(0xE6141820),        // layer-1
    dockText = Color(0xFFF0F2F5),
    dockTextMuted = Color(0xFF8B95A5)
)

val LightVennelaPalette = VennelaPalette(
    isDark = false,
    backgroundTop = Color(0xFFF4F6FA),
    backgroundMid = Color(0xFFEAEDF5),
    backgroundBottom = Color(0xFFF9FAFE),
    textPrimary = Color(0xFF0C0F14),
    textSecondary = Color(0xFF505868),
    textTertiary = Color(0xFF8B95A5),
    cardSurface = Color(0xE6FFFFFF),
    cardSurfaceStrong = Color(0xF8FFFFFF),
    cardBorder = Color(0x4D0091C7),              // darker cyan for light
    dockSurfaceTop = Color(0xFAFFFFFF),
    dockSurfaceBottom = Color(0xF2EAF0FA),
    dockText = Color(0xFF0C0F14),
    dockTextMuted = Color(0xFF505868)
)

/**
 * Nebula Flux reactive tinting — pulls the entire atmosphere toward the active
 * accent. Background, cards, and dock shift chromatically so that switching
 * theme or swatch recolours the whole cosmos, not just the highlights.
 */
fun VennelaPalette.tinted(accent: VennelaAccent): VennelaPalette {
    val bgTint = if (isDark) 0.18f else 0.10f
    return copy(
        backgroundTop = lerp(backgroundTop, accent.primary, bgTint),
        backgroundMid = lerp(backgroundMid, accent.primary, bgTint * 0.65f),
        backgroundBottom = lerp(backgroundBottom, accent.secondary, bgTint * 0.35f),
        cardSurface = lerp(cardSurface, accent.primary, bgTint * 0.28f),
        cardSurfaceStrong = lerp(cardSurfaceStrong, accent.primary, bgTint * 0.22f),
        cardBorder = accent.secondary.copy(alpha = if (isDark) 0.35f else 0.28f),
        dockSurfaceTop = lerp(dockSurfaceTop, accent.primary, bgTint * 0.38f),
        dockSurfaceBottom = lerp(dockSurfaceBottom, accent.secondary, bgTint * 0.20f)
    )
}

val LocalVennelaPalette = staticCompositionLocalOf { DarkVennelaPalette }
val LocalVennelaAccent = staticCompositionLocalOf { AccentOption.ICE_BLUE.toAccent() }
