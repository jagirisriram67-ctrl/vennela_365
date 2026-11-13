package com.vennela365.viewer.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.R

/**
 * App-wide typography presets, defaulting to OnePlus Sans (OxygenOS 15 geometric
 * humanist display + razor-sharp Inter body legibility).
 */
enum class AppFontStyle(
    val id: String,
    val label: String,
    val badge: String,
    val subtitle: String
) {
    ONEPLUS_SANS(
        id = "oneplus_sans",
        label = "OnePlus Sans",
        badge = "1+ Sans",
        subtitle = "OxygenOS 15 geometric display + ultra-clean humanist text"
    ),
    GOOGLE_WORKSPACE(
        id = "google_sans",
        label = "Google Workspace Sans",
        badge = "G Sans",
        subtitle = "Rounded geometric workspace typography"
    ),
    EDITORIAL_SERIF(
        id = "editorial_serif",
        label = "Editorial Book Serif",
        badge = "Serif",
        subtitle = "High-contrast book and magazine reading typography"
    ),
    DEVELOPER_MONO(
        id = "developer_mono",
        label = "JetBrains Tabular Mono",
        badge = "Mono",
        subtitle = "Precision monospaced technical typography"
    );

    val title: String get() = label

    companion object {
        fun fromId(id: String?): AppFontStyle =
            entries.firstOrNull { it.id == id } ?: ONEPLUS_SANS
    }
}

/**
 * Curated variable fonts bundled directly in res/font:
 * - OnePlusSans / Sans: Outfit geometric display weights + Inter humanist body weights
 *   modeled after OnePlus Slate / OnePlus Sans (OxygenOS 15).
 * - PlusJakarta: Plus Jakarta Sans for Google Workspace / rounded UI.
 * - Serif: Newsreader for editorial document headlines & long-form prose.
 * - Mono: JetBrains Mono for code blocks, formula bars, and binary/hex inspectors.
 */
object VennelaFonts {
    val OnePlusDisplay: FontFamily = FontFamily(
        Font(R.font.outfit, FontWeight.Normal),
        Font(R.font.outfit, FontWeight.Medium),
        Font(R.font.outfit, FontWeight.SemiBold),
        Font(R.font.outfit, FontWeight.Bold),
        Font(R.font.outfit, FontWeight.ExtraBold)
    )

    val OnePlusBody: FontFamily = FontFamily(
        Font(R.font.inter, FontWeight.Normal),
        Font(R.font.inter, FontWeight.Medium),
        Font(R.font.outfit, FontWeight.SemiBold),
        Font(R.font.outfit, FontWeight.Bold),
        Font(R.font.outfit, FontWeight.ExtraBold)
    )

    val PlusJakarta: FontFamily = FontFamily(
        Font(R.font.plus_jakarta_sans, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans, FontWeight.Bold),
        Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
    )

    /** Default app-wide font family: OnePlus Sans (Outfit + Inter). */
    val Sans: FontFamily = OnePlusBody

    val Serif: FontFamily = FontFamily(
        Font(R.font.newsreader, FontWeight.Normal),
        Font(R.font.newsreader, FontWeight.Medium),
        Font(R.font.newsreader, FontWeight.SemiBold),
        Font(R.font.newsreader, FontWeight.Bold)
    )

    val Mono: FontFamily = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.Medium),
        Font(R.font.jetbrains_mono, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono, FontWeight.Bold)
    )

    fun familyFor(style: AppFontStyle): FontFamily = when (style) {
        AppFontStyle.ONEPLUS_SANS -> OnePlusBody
        AppFontStyle.GOOGLE_WORKSPACE -> PlusJakarta
        AppFontStyle.EDITORIAL_SERIF -> Serif
        AppFontStyle.DEVELOPER_MONO -> Mono
    }

    fun displayFamilyFor(style: AppFontStyle): FontFamily = when (style) {
        AppFontStyle.ONEPLUS_SANS -> OnePlusDisplay
        AppFontStyle.GOOGLE_WORKSPACE -> PlusJakarta
        AppFontStyle.EDITORIAL_SERIF -> Serif
        AppFontStyle.DEVELOPER_MONO -> Mono
    }
}

/**
 * Vennela 365 design tokens.
 *
 * Single source of truth for radii, spacing, type, elevation, and motion.
 */
object VennelaTokens {

    // ---- Radii (liquid-glass curvature) ---------------------------------
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
    val gutter = 16.dp

    // ---- Layout --------------------------------------------------------
    val touchTargetMin = 48.dp
    val touchTargetCompact = 36.dp
    val iconSm = 16.dp
    val iconMd = 19.dp
    val iconLg = 22.dp
    val iconXl = 26.dp
    val badgeXs = 26.dp
    val badgeSm = 34.dp
    val badgeMd = 40.dp
    val badgeLg = 46.dp

    // ---- Viewer chrome (content-first, effortless touch ergonomics) -----
    val viewerBar = 52.dp
    val viewerDock = 54.dp
    val viewerControl = 36.dp
    val viewerIcon = 19.dp
    val viewerTopInset = 54.dp
    val viewerSheetPadding = 16.dp

    /** Compact icon button diameter matching inside viewerBar cleanly. */
    val controlSm = 36.dp

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

    // ---- Magnetic Spring Physics ---------------------------------------
    val springDampingPress = 0.62f
    val springStiffnessPress = 520f
    val pressScaleDown = 0.955f
    val pressRotationDeg = 0.8f

    val springDampingEntrance = 0.78f
    val springStiffnessEntrance = 340f

    // ---- Entrance Choreography -----------------------------------------
    val entranceStaggerMs = 32
    val entranceFadeDurationMs = 200
    val entranceSlideOffsetY = 20.dp
    val entranceScaleStart = 0.94f

    // ---- Neon Pulse Glow -----------------------------------------------
    val glowRadiusActive = 16.dp
    val glowRadiusAmbient = 8.dp
    val glowPulseDurationMs = 2400
    val glowPulseAlphaLow = 0.25f
    val glowPulseAlphaHigh = 0.65f

    // ---- Liquid Border Gradient ----------------------------------------
    val liquidBorderAlphaBright = 0.56f
    val liquidBorderAlphaFade = 0.08f

    // ---- Chromatic Depth Layers ----------------------------------------
    val depthSaturationStep = 0.06f
    val depthLevels = 4
}

/**
 * Builds the Material 3 Typography for the active [AppFontStyle], defaulting
 * to OnePlus Sans (Outfit geometric display + Inter humanist body).
 */
fun vennelaTypographyFor(style: AppFontStyle = AppFontStyle.ONEPLUS_SANS): Typography {
    val display = VennelaFonts.displayFamilyFor(style)
    val body = VennelaFonts.familyFor(style)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = VennelaTokens.titleDisplay,
            lineHeight = 32.sp,
            letterSpacing = (-0.35).sp
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = VennelaTokens.titleLarge,
            lineHeight = 28.sp,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = VennelaTokens.titleMedium,
            lineHeight = 23.sp,
            letterSpacing = (-0.1).sp
        ),
        titleSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = VennelaTokens.titleSmall,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = VennelaTokens.body,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = VennelaTokens.bodySmall,
            lineHeight = 19.sp
        ),
        labelLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = VennelaTokens.caption,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = VennelaTokens.micro,
            lineHeight = 14.sp,
            letterSpacing = 0.2.sp
        )
    )
}

val VennelaTypography = vennelaTypographyFor(AppFontStyle.ONEPLUS_SANS)
