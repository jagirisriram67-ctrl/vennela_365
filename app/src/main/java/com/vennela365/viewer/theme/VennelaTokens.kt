package com.vennela365.viewer.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vennela 365 design tokens — Nebula Flux edition.
 *
 * Single source of truth for radii, spacing, type, elevation, and motion.
 * Enlarged corner radii for the liquid-glass language; new animation tokens
 * for magnetic spring physics, entrance choreography, and neon glow pulses.
 */
object VennelaTokens {

    // ---- Radii (liquid-glass curvature, tuned so surfaces read compact) ----
    val radiusChip = 14.dp
    val radiusBadge = 16.dp
    val radiusControl = 18.dp
    val radiusTile = 22.dp
    val radiusCard = 24.dp
    val radiusCardLarge = 28.dp
    val radiusSheet = 30.dp
    val radiusDock = 32.dp
    val radiusPill = 50.dp

    // ---- Spacing scale -------------------------------------------------
    val spaceXxs = 2.dp
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 20.dp
    val spaceXxl = 28.dp
    val spaceHuge = 40.dp

    /** Standard horizontal page gutter. */
    val gutter = 14.dp

    // ---- Layout --------------------------------------------------------
    val touchTargetMin = 48.dp
    val touchTargetCompact = 36.dp
    val iconSm = 15.dp
    val iconMd = 18.dp
    val iconLg = 22.dp
    val iconXl = 26.dp
    val badgeXs = 26.dp
    val badgeSm = 32.dp
    val badgeMd = 38.dp
    val badgeLg = 44.dp

    // ---- Viewer chrome (compact, content-first) -------------------------
    val viewerBar = 40.dp
    val viewerDock = 42.dp
    val viewerControl = 30.dp
    val viewerIcon = 18.dp
    val viewerTopInset = 48.dp
    val viewerSheetPadding = 14.dp

    /**
     * The one size every compact icon button uses.
     *
     * 32dp is the smallest footprint that still reads as a comfortable target
     * on a phone, and it fits inside a 40dp bar without inflating it.
     */
    val controlSm = 32.dp

    // ---- Type scale ----------------------------------------------------
    val titleHero = 30.sp
    val titleDisplay = 26.sp
    val titleLarge = 22.sp
    val titleMedium = 17.sp
    val titleSmall = 15.sp
    val body = 15.sp
    val bodySmall = 13.sp
    val caption = 12.sp
    val micro = 11.sp
    val nano = 10.sp

    // ---- Elevation -----------------------------------------------------
    val elevationFlat = 0.dp
    val elevationRaised = 4.dp
    val elevationCard = 8.dp
    val elevationBar = 10.dp
    val elevationDock = 14.dp
    val elevationOverlay = 20.dp

    /** Frost strength presets (dp of blur). */
    val frostLight = 14.dp
    val frostRegular = 22.dp
    val frostHeavy = 34.dp

    // ---- NEW: Magnetic Spring Physics ----------------------------------
    /** Damping ratio for press feedback (under-damped bounce). */
    val springDampingPress = 0.55f
    /** Stiffness for press animations (lower = softer spring). */
    val springStiffnessPress = 400f
    /** Scale factor on press-down (slight shrink). */
    val pressScaleDown = 0.94f
    /** Rotation degrees added on press for magnetic tilt. */
    val pressRotationDeg = 1.2f

    /** Damping for entrance reveal springs. */
    val springDampingEntrance = 0.72f
    /** Stiffness for entrance reveal springs. */
    val springStiffnessEntrance = 300f

    // ---- NEW: Entrance Choreography ------------------------------------
    /** Base delay between staggered item entrances. */
    val entranceStaggerMs = 40
    /** Duration of fade-in for entrance animations. */
    val entranceFadeDurationMs = 220
    /** Initial Y-offset for entrance slide-up (dp). */
    val entranceSlideOffsetY = 24.dp
    /** Initial scale for entrance zoom-in. */
    val entranceScaleStart = 0.92f

    // ---- NEW: Neon Pulse Glow ------------------------------------------
    /** Glow radius for active / focused neon pulse. */
    val glowRadiusActive = 16.dp
    /** Glow radius for subtle ambient neon halo. */
    val glowRadiusAmbient = 8.dp
    /** Duration of one breathing glow cycle (ms). */
    val glowPulseDurationMs = 2400
    /** Alpha range for glow pulse: low end. */
    val glowPulseAlphaLow = 0.25f
    /** Alpha range for glow pulse: high end. */
    val glowPulseAlphaHigh = 0.65f

    // ---- NEW: Liquid Border Gradient -----------------------------------
    /** Alpha of the bright (top-left) end of a liquid border. */
    val liquidBorderAlphaBright = 0.60f
    /** Alpha of the transparent (bottom-right) end. */
    val liquidBorderAlphaFade = 0.08f

    // ---- NEW: Chromatic Depth Layers -----------------------------------
    /** Saturation boost factor per depth level (0 = surface, deeper = more). */
    val depthSaturationStep = 0.06f
    /** Number of distinct depth levels used for surface layering. */
    val depthLevels = 4
}

/**
 * Typography tuned for legibility on low-end panels: larger minimum sizes and
 * generous line heights than the Material defaults, with no downloaded fonts so
 * the APK stays small and rendering stays instant.
 */
val VennelaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = VennelaTokens.titleDisplay,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = VennelaTokens.titleLarge,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = VennelaTokens.titleMedium,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = VennelaTokens.titleSmall,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = VennelaTokens.body,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = VennelaTokens.bodySmall,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = VennelaTokens.caption,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = VennelaTokens.micro,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)
