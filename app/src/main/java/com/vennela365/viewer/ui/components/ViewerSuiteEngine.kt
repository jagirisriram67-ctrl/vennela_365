package com.vennela365.viewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens

/**
 * The 5 global flagship viewing styles requested by the user:
 * 1. Google Workspace (Google Docs, Sheets, Slides, Drive PDF)
 * 2. Microsoft 365 (Word, Excel, PowerPoint, OneDrive Ribbon)
 * 3. WPS Office Super-App (Kingsoft WPS Eye-Care Green, Auto-Scroll, Night Parchment)
 * 4. LibreOffice Desktop Pro (Writer, Calc, Impress with Rulers & Status Bar)
 * 5. Oken Scanner / CamScanner HD (Magic Color, Crisp B&W, Smart Frame, Stamp & Watermark)
 */
enum class ViewerSuiteStyle(
    val id: String,
    val shortTitle: String,
    val fullTitle: String,
    val subtitle: String,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val badgeText: String
) {
    GOOGLE_WORKSPACE(
        id = "google_workspace",
        shortTitle = "Google",
        fullTitle = "Google Workspace",
        subtitle = "Google Docs • Sheets • Slides • Drive print & pageless view",
        brandPrimary = Color(0xFF1A73E8),
        brandSecondary = Color(0xFF0F9D58),
        badgeText = "G"
    ),
    MICROSOFT_365(
        id = "microsoft_365",
        shortTitle = "Microsoft 365",
        fullTitle = "Microsoft 365 Office",
        subtitle = "Word • Excel • PowerPoint Ribbon + Immersive Line Focus",
        brandPrimary = Color(0xFF185ABD),
        brandSecondary = Color(0xFFC43E1C),
        badgeText = "M365"
    ),
    WPS_SUPER(
        id = "wps_super",
        shortTitle = "WPS Super",
        fullTitle = "WPS Office Super-App",
        subtitle = "Eye-Care Green Paper • Auto-Scroll Teleprompter • VIP Tools",
        brandPrimary = Color(0xFFD8292F),
        brandSecondary = Color(0xFF1677FF),
        badgeText = "WPS"
    ),
    LIBRE_OFFICE(
        id = "libre_office",
        shortTitle = "LibreOffice",
        fullTitle = "LibreOffice Desktop Pro",
        subtitle = "Writer • Calc • Impress • Margin Crop Marks & Status Bar",
        brandPrimary = Color(0xFF18A303),
        brandSecondary = Color(0xFF0369A3),
        badgeText = "LO"
    ),
    OKEN_SCANNER(
        id = "oken_scanner",
        shortTitle = "Oken Scanner",
        fullTitle = "Oken Scanner HD Studio",
        subtitle = "Magic Color • Crisp B&W • Smart Crop Frame • Security Watermark",
        brandPrimary = Color(0xFF00B894),
        brandSecondary = Color(0xFF0984E3),
        badgeText = "SCAN"
    );

    val badge: String get() = badgeText

    companion object {
        fun fromId(id: String?): ViewerSuiteStyle =
            entries.firstOrNull { it.id == id } ?: GOOGLE_WORKSPACE
    }
}

/** Product kind so each suite colors its header like the real app (Docs=Blue, Sheets=Green, Slides=Amber/Orange, PDF=Red, Scan=Teal). */
enum class ViewerKind(
    val label: String,
    val googleColor: Color,
    val microsoftColor: Color,
    val wpsColor: Color,
    val libreColor: Color,
    val okenColor: Color,
    val icon: ImageVector
) {
    DOCS(
        label = "Docs",
        googleColor = Color(0xFF1A73E8),
        microsoftColor = Color(0xFF185ABD),
        wpsColor = Color(0xFF1677FF),
        libreColor = Color(0xFF0369A3),
        okenColor = Color(0xFF00B894),
        icon = Icons.Default.Description
    ),
    SHEETS(
        label = "Sheets",
        googleColor = Color(0xFF188038),
        microsoftColor = Color(0xFF107C41),
        wpsColor = Color(0xFF00A854),
        libreColor = Color(0xFF18A303),
        okenColor = Color(0xFF00B894),
        icon = Icons.Default.TableChart
    ),
    SLIDES(
        label = "Slides",
        googleColor = Color(0xFFF29900),
        microsoftColor = Color(0xFFC43E1C),
        wpsColor = Color(0xFFFA541C),
        libreColor = Color(0xFFD36118),
        okenColor = Color(0xFF00B894),
        icon = Icons.Default.Slideshow
    ),
    PDF(
        label = "PDF",
        googleColor = Color(0xFFD93025),
        microsoftColor = Color(0xFFB3261E),
        wpsColor = Color(0xFFD8292F),
        libreColor = Color(0xFFC92A2A),
        okenColor = Color(0xFF00B894),
        icon = Icons.Default.PictureAsPdf
    ),
    SCAN_IMAGE(
        label = "Oken Scan",
        googleColor = Color(0xFF1A73E8),
        microsoftColor = Color(0xFF0078D4),
        wpsColor = Color(0xFFD8292F),
        libreColor = Color(0xFF18A303),
        okenColor = Color(0xFF00B894),
        icon = Icons.Default.DocumentScanner
    );

    fun accentFor(suite: ViewerSuiteStyle): Color = when (suite) {
        ViewerSuiteStyle.GOOGLE_WORKSPACE -> googleColor
        ViewerSuiteStyle.MICROSOFT_365 -> microsoftColor
        ViewerSuiteStyle.WPS_SUPER -> wpsColor
        ViewerSuiteStyle.LIBRE_OFFICE -> libreColor
        ViewerSuiteStyle.OKEN_SCANNER -> okenColor
    }

    fun appNameFor(suite: ViewerSuiteStyle): String = when (this) {
        DOCS -> when (suite) {
            ViewerSuiteStyle.GOOGLE_WORKSPACE -> "Google Docs"
            ViewerSuiteStyle.MICROSOFT_365 -> "Microsoft Word"
            ViewerSuiteStyle.WPS_SUPER -> "WPS Writer"
            ViewerSuiteStyle.LIBRE_OFFICE -> "LibreOffice Writer"
            ViewerSuiteStyle.OKEN_SCANNER -> "Oken HD Doc Scan"
        }
        SHEETS -> when (suite) {
            ViewerSuiteStyle.GOOGLE_WORKSPACE -> "Google Sheets"
            ViewerSuiteStyle.MICROSOFT_365 -> "Microsoft Excel"
            ViewerSuiteStyle.WPS_SUPER -> "WPS Spreadsheets"
            ViewerSuiteStyle.LIBRE_OFFICE -> "LibreOffice Calc"
            ViewerSuiteStyle.OKEN_SCANNER -> "Oken HD Table Scan"
        }
        SLIDES -> when (suite) {
            ViewerSuiteStyle.GOOGLE_WORKSPACE -> "Google Slides"
            ViewerSuiteStyle.MICROSOFT_365 -> "Microsoft PowerPoint"
            ViewerSuiteStyle.WPS_SUPER -> "WPS Presentation"
            ViewerSuiteStyle.LIBRE_OFFICE -> "LibreOffice Impress"
            ViewerSuiteStyle.OKEN_SCANNER -> "Oken HD Board Scan"
        }
        PDF -> when (suite) {
            ViewerSuiteStyle.GOOGLE_WORKSPACE -> "Google Drive PDF"
            ViewerSuiteStyle.MICROSOFT_365 -> "Microsoft 365 PDF"
            ViewerSuiteStyle.WPS_SUPER -> "WPS PDF Reader"
            ViewerSuiteStyle.LIBRE_OFFICE -> "LibreOffice Draw PDF"
            ViewerSuiteStyle.OKEN_SCANNER -> "Oken Scanner HD PDF"
        }
        SCAN_IMAGE -> when (suite) {
            ViewerSuiteStyle.GOOGLE_WORKSPACE -> "Google Photos Lens"
            ViewerSuiteStyle.MICROSOFT_365 -> "Microsoft Lens"
            ViewerSuiteStyle.WPS_SUPER -> "WPS Scanner"
            ViewerSuiteStyle.LIBRE_OFFICE -> "LibreOffice Draw"
            ViewerSuiteStyle.OKEN_SCANNER -> "Oken Scanner HD"
        }
    }
}

/**
 * WPS Office & Universal Reading Paper Tones — includes the iconic Chinese WPS
 * Eye-Care Green (#C7EDCC), Warm Parchment, Pure A4 White, and Obsidian Night.
 */
enum class WpsPaperTone(
    val id: String,
    val label: String,
    val paperColor: Color,
    val inkColor: Color,
    val mutedInkColor: Color,
    val canvasSurroundColor: Color
) {
    PURE_WHITE(
        id = "pure_white",
        label = "A4 White",
        paperColor = Color(0xFFFFFFFF),
        inkColor = Color(0xFF1F2329),
        mutedInkColor = Color(0xFF5F6368),
        canvasSurroundColor = Color(0xFFF1F3F4)
    ),
    EYE_CARE_GREEN(
        id = "eye_care_green",
        label = "WPS Eye-Care",
        paperColor = Color(0xFFC7EDCC),
        inkColor = Color(0xFF102A16),
        mutedInkColor = Color(0xFF35593E),
        canvasSurroundColor = Color(0xFFB5DFB9)
    ),
    WARM_PARCHMENT(
        id = "warm_parchment",
        label = "Parchment",
        paperColor = Color(0xFFF6EFE2),
        inkColor = Color(0xFF2D2216),
        mutedInkColor = Color(0xFF6B5843),
        canvasSurroundColor = Color(0xFFEAE0CE)
    ),
    CALM_BLUE(
        id = "calm_blue",
        label = "Clear Sky",
        paperColor = Color(0xFFEAF2FF),
        inkColor = Color(0xFF0F213E),
        mutedInkColor = Color(0xFF476085),
        canvasSurroundColor = Color(0xFFDCE7FA)
    ),
    NIGHT_OBSIDIAN(
        id = "night_obsidian",
        label = "Night Dark",
        paperColor = Color(0xFF161B24),
        inkColor = Color(0xFFE8ECF4),
        mutedInkColor = Color(0xFF9AA4B5),
        canvasSurroundColor = Color(0xFF0D1017)
    );

    companion object {
        val CRISP_WHITE: WpsPaperTone get() = PURE_WHITE
    }
}

/**
 * Oken Scanner / CamScanner HD document enhancement filters with real GPU
 * ColorMatrix pipelines.
 */
enum class OkenScanFilter(
    val id: String,
    val label: String,
    val subtitle: String
) {
    MAGIC_COLOR("magic_color", "Magic Color", "Sharpens ink & whitens paper background"),
    CRISP_BW("crisp_bw", "B&W Crisp", "High-contrast binary document thresholding"),
    LIGHTEN("lighten", "Lighten", "Lifts shadows from uneven phone lighting"),
    GRAYSCALE("grayscale", "Eco Gray", "Clean monochrome toner-saving scan"),
    ORIGINAL("original", "Original", "Unmodified source pixels");

    fun toColorMatrix(): ColorMatrix? = when (this) {
        ORIGINAL -> null
        MAGIC_COLOR -> ColorMatrix(
            floatArrayOf(
                1.48f, -0.12f, -0.12f, 0f, -18f,
                -0.12f, 1.48f, -0.12f, 0f, -18f,
                -0.12f, -0.12f, 1.52f, 0f, -14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        CRISP_BW -> ColorMatrix(
            floatArrayOf(
                0.85f, 1.65f, 0.32f, 0f, -155f,
                0.85f, 1.65f, 0.32f, 0f, -155f,
                0.85f, 1.65f, 0.32f, 0f, -155f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        LIGHTEN -> ColorMatrix(
            floatArrayOf(
                1.18f, 0f, 0f, 0f, 22f,
                0f, 1.18f, 0f, 0f, 22f,
                0f, 0f, 1.20f, 0f, 26f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
    }

    fun toColorFilter(): ColorFilter? = toColorMatrix()?.let { ColorFilter.colorMatrix(it) }

    companion object {
        val BW_CRISP: OkenScanFilter get() = CRISP_BW
    }
}

/** Microsoft 365 Office Ribbon tabs. */
enum class OfficeRibbonTab(val label: String) {
    HOME("Home"),
    LAYOUT("Layout"),
    REVIEW("Review"),
    VIEW("View")
}

/**
 * Interactive state for the Universal Suite Header & Paper Engine.
 */
class SuiteSessionState(
    initialStyle: ViewerSuiteStyle,
    isDarkTheme: Boolean
) {
    var pageless by mutableStateOf(false)
    var showRuler by mutableStateOf(true)
    var paperTone by mutableStateOf(
        when {
            initialStyle == ViewerSuiteStyle.WPS_SUPER && !isDarkTheme -> WpsPaperTone.EYE_CARE_GREEN
            isDarkTheme -> WpsPaperTone.NIGHT_OBSIDIAN
            else -> WpsPaperTone.PURE_WHITE
        }
    )
    var ribbonTab by mutableStateOf(OfficeRibbonTab.HOME)
    var immersiveLineFocus by mutableStateOf(false)
    var autoScrollActive by mutableStateOf(false)
    var autoScrollSpeed by mutableStateOf(1.0f)
    var libreMarginGuides by mutableStateOf(true)
    var libreLineNumbers by mutableStateOf(false)
    var okenFilter by mutableStateOf(OkenScanFilter.MAGIC_COLOR)
    var okenCropFrame by mutableStateOf(true)
    var okenWatermark by mutableStateOf(true)
    var okenOfficialStamp by mutableStateOf(false)

    var pagelessMode: Boolean
        get() = pageless
        set(value) { pageless = value }

    var scanFilter: OkenScanFilter
        get() = okenFilter
        set(value) { okenFilter = value }

    var showAutoCropHandles: Boolean
        get() = okenCropFrame
        set(value) { okenCropFrame = value }

    var showSecurityWatermark: Boolean
        get() = okenWatermark
        set(value) { okenWatermark = value }

    var showOfficialStamp: Boolean
        get() = okenOfficialStamp
        set(value) { okenOfficialStamp = value }
}

@Composable
fun rememberSuiteSessionState(
    activeSuite: ViewerSuiteStyle,
    isDarkTheme: Boolean
): SuiteSessionState = remember(activeSuite, isDarkTheme) {
    SuiteSessionState(activeSuite, isDarkTheme)
}

/* ------------------------------------------------------------------------- */
/* 1. Top Suite Switcher Pill Bar + Suite Command Header                      */
/* ------------------------------------------------------------------------- */

/**
 * The complete top header for any file viewer, combining:
 * - File title, format badge, back button, and quick actions
 * - 1-Tap **Suite Style Switcher** (`Google` • `Microsoft 365` • `WPS Super` • `LibreOffice` • `Oken Scanner`)
 * - Authentic suite-specific Ribbon / Ruler / Eye-Care / Scanner controls!
 */
@Composable
fun SuiteCommandHeader(
    title: String,
    subtitle: String,
    kind: ViewerKind,
    activeSuite: ViewerSuiteStyle,
    onSuiteChange: (ViewerSuiteStyle) -> Unit,
    session: SuiteSessionState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onFindToggle: (() -> Unit)? = null,
    onOutlineToggle: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onFontCycle: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null,
    extraActions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalVennelaPalette.current
    val brandColor by animateColorAsState(
        targetValue = kind.accentFor(activeSuite),
        animationSpec = tween(LiquidMotion.durationFast),
        label = "suiteBrandColor"
    )
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val headerSurface = when (activeSuite) {
        ViewerSuiteStyle.GOOGLE_WORKSPACE ->
            if (palette.isDark) Color(0xFF1F1F1F) else Color(0xFFF8F9FA)
        ViewerSuiteStyle.MICROSOFT_365 ->
            brandColor
        ViewerSuiteStyle.WPS_SUPER ->
            if (palette.isDark) Color(0xFF181C26) else Color(0xFFFFFFFF)
        ViewerSuiteStyle.LIBRE_OFFICE ->
            if (palette.isDark) Color(0xFF20242B) else Color(0xFFECEFF4)
        ViewerSuiteStyle.OKEN_SCANNER ->
            Color(0xFF0E171D)
    }

    val isSolidBrandBar = activeSuite == ViewerSuiteStyle.MICROSOFT_365 ||
        activeSuite == ViewerSuiteStyle.OKEN_SCANNER
    val headerTextPrimary = if (isSolidBrandBar) Color.White else palette.textPrimary
    val headerTextSecondary = if (isSolidBrandBar) Color.White.copy(alpha = 0.80f) else palette.textSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp)
            .background(headerSurface)
            .padding(top = statusInset)
    ) {
        // ---- Row 1: App Bar with Back, Suite Icon Badge, Title, and Primary Actions ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .testTag("viewer_back_button")
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSolidBrandBar) Color.White.copy(alpha = 0.16f)
                        else brandColor.copy(alpha = 0.12f)
                    )
                    .pressScale(0.90f, onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isSolidBrandBar) Color.White else brandColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Suite Product Icon Tile (Google Docs blue icon, Word W badge, WPS badge, etc.)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSolidBrandBar) Color.White.copy(alpha = 0.20f)
                        else brandColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = kind.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = headerTextPrimary,
                        fontFamily = VennelaFonts.OnePlusDisplay,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSolidBrandBar) Color.White.copy(alpha = 0.22f)
                                else brandColor.copy(alpha = 0.14f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${activeSuite.badgeText} • ${kind.label}",
                            color = if (isSolidBrandBar) Color.White else brandColor,
                            fontFamily = VennelaFonts.OnePlusDisplay,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = subtitle,
                    color = headerTextSecondary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onFindToggle != null) {
                SuiteHeaderIconBtn(
                    icon = Icons.Default.Search,
                    contentDescription = "Find",
                    tint = headerTextPrimary,
                    onClick = onFindToggle
                )
            }
            if (onExportPdf != null) {
                SuiteHeaderIconBtn(
                    icon = Icons.Default.PictureAsPdf,
                    contentDescription = "Export PDF",
                    tint = headerTextPrimary,
                    onClick = onExportPdf
                )
            }
            if (onShare != null) {
                SuiteHeaderIconBtn(
                    icon = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = headerTextPrimary,
                    onClick = onShare
                )
            }
            extraActions()
        }

        // ---- Row 2: 1-Tap Flagship Suite Style Switcher Strip ----
        SuiteStyleSwitcherStrip(
            activeSuite = activeSuite,
            onSuiteChange = onSuiteChange,
            isSolidBrandBar = isSolidBrandBar,
            brandColor = brandColor
        )

        // ---- Row 3: Suite-Specific Authentic Toolbar / Ribbon / Ruler ----
        when (activeSuite) {
            ViewerSuiteStyle.GOOGLE_WORKSPACE -> GoogleWorkspaceSubBar(
                kind = kind,
                brandColor = brandColor,
                session = session,
                onOutlineToggle = onOutlineToggle,
                onPrint = onPrint,
                onFontCycle = onFontCycle
            )
            ViewerSuiteStyle.MICROSOFT_365 -> Microsoft365RibbonBar(
                brandColor = brandColor,
                session = session,
                onFontCycle = onFontCycle,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onOutlineToggle = onOutlineToggle,
                onPrint = onPrint
            )
            ViewerSuiteStyle.WPS_SUPER -> WpsSuperAppSubBar(
                brandColor = brandColor,
                session = session,
                onExportPdf = onExportPdf,
                onOutlineToggle = onOutlineToggle
            )
            ViewerSuiteStyle.LIBRE_OFFICE -> LibreOfficeSubBar(
                brandColor = brandColor,
                session = session,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onPrint = onPrint
            )
            ViewerSuiteStyle.OKEN_SCANNER -> OkenScannerSubBar(
                session = session,
                onExportPdf = onExportPdf
            )
        }

        // ---- Progress hairline ----
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(brandColor.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .height(2.dp)
                        .background(if (isSolidBrandBar) Color.White else brandColor)
                )
            }
        }
    }
}

@Composable
fun SuiteCommandHeader(
    kind: ViewerKind,
    fileName: String,
    subtitle: String,
    activeSuite: ViewerSuiteStyle,
    onSelectSuite: (ViewerSuiteStyle) -> Unit,
    session: SuiteSessionState,
    progress: Float,
    zoomPercent: Int,
    onBack: () -> Unit,
    onSearchToggle: () -> Unit,
    onOutlineToggle: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onTextScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    SuiteCommandHeader(
        title = fileName,
        subtitle = "$subtitle • $zoomPercent%",
        kind = kind,
        activeSuite = activeSuite,
        onSuiteChange = onSelectSuite,
        session = session,
        onBack = onBack,
        modifier = modifier,
        progress = progress,
        onFindToggle = onSearchToggle,
        onOutlineToggle = onOutlineToggle,
        onExportPdf = onExportPdf,
        onPrint = onPrint,
        onShare = onShare,
        onFontCycle = { onTextScaleChange(0.1f) },
        onZoomIn = { onZoomChange(0.15f) },
        onZoomOut = { onZoomChange(-0.15f) }
    )
}

@Composable
private fun SuiteHeaderIconBtn(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .pressScale(0.90f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 1-tap horizontal strip allowing instant switching between:
 * Google • Microsoft 365 • WPS Super • LibreOffice • Oken Scanner
 */
@Composable
fun SuiteStyleSwitcherStrip(
    activeSuite: ViewerSuiteStyle,
    onSuiteChange: (ViewerSuiteStyle) -> Unit,
    isSolidBrandBar: Boolean = false,
    brandColor: Color = Color(0xFF1A73E8)
) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ViewerSuiteStyle.entries.forEach { suite ->
            val selected = suite == activeSuite
            val pillBg = when {
                selected && isSolidBrandBar -> Color.White
                selected -> suite.brandPrimary
                isSolidBrandBar -> Color.White.copy(alpha = 0.14f)
                palette.isDark -> Color.White.copy(alpha = 0.08f)
                else -> Color.Black.copy(alpha = 0.06f)
            }
            val pillFg = when {
                selected && isSolidBrandBar -> brandColor
                selected -> Color.White
                isSolidBrandBar -> Color.White.copy(alpha = 0.90f)
                else -> palette.textPrimary
            }
            Row(
                modifier = Modifier
                    .testTag("suite_style_${suite.id}")
                    .clip(RoundedCornerShape(50))
                    .background(pillBg)
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .pressScale(0.94f) { onSuiteChange(suite) }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected && !isSolidBrandBar) Color.White else suite.brandPrimary)
                )
                Text(
                    text = suite.shortTitle,
                    color = pillFg,
                    fontFamily = VennelaFonts.OnePlusDisplay,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SuiteStyleSwitcherStrip(
    activeSuite: ViewerSuiteStyle,
    onSelectSuite: (ViewerSuiteStyle) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        SuiteStyleSwitcherStrip(
            activeSuite = activeSuite,
            onSuiteChange = onSelectSuite,
            isSolidBrandBar = false,
            brandColor = activeSuite.brandPrimary
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Suite-specific authentic sub-bars                                          */
/* ------------------------------------------------------------------------- */

@Composable
private fun GoogleWorkspaceSubBar(
    kind: ViewerKind,
    brandColor: Color,
    session: SuiteSessionState,
    onOutlineToggle: (() -> Unit)?,
    onPrint: (() -> Unit)?,
    onFontCycle: (() -> Unit)?
) {
    val palette = LocalVennelaPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuiteMiniChip(
                label = if (session.pageless) "Pageless" else "Print Layout",
                active = !session.pageless,
                accentColor = brandColor,
                onClick = { session.pageless = !session.pageless }
            )
            SuiteMiniChip(
                label = "Ruler",
                active = session.showRuler,
                accentColor = brandColor,
                onClick = { session.showRuler = !session.showRuler }
            )
            WpsPaperTone.entries.take(3).forEach { tone ->
                SuiteMiniChip(
                    label = tone.label,
                    active = session.paperTone == tone,
                    accentColor = brandColor,
                    onClick = { session.paperTone = tone }
                )
            }
            if (onOutlineToggle != null) {
                SuiteMiniChip(
                    label = "Document Outline",
                    active = false,
                    accentColor = brandColor,
                    onClick = onOutlineToggle
                )
            }
            if (onFontCycle != null) {
                SuiteMiniChip(
                    label = "Font (1+ Sans)",
                    active = false,
                    accentColor = brandColor,
                    onClick = onFontCycle
                )
            }
            if (onPrint != null) {
                SuiteMiniChip(
                    label = "Print",
                    active = false,
                    accentColor = brandColor,
                    onClick = onPrint
                )
            }
        }
        if (session.showRuler && kind != ViewerKind.SHEETS) {
            GoogleDocsRulerBar(brandColor = brandColor, isDark = palette.isDark)
        }
    }
}

@Composable
fun GoogleDocsRulerBar(brandColor: Color, isDark: Boolean) {
    val rulerBg = if (isDark) Color(0xFF28292C) else Color(0xFFEDF2FA)
    val tickColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(rulerBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = size.width / 8f
            // Left and right blue margin indent triangles like Google Docs
            drawCircle(
                color = brandColor,
                radius = 4.dp.toPx(),
                center = Offset(step * 0.7f, size.height * 0.5f)
            )
            drawCircle(
                color = brandColor,
                radius = 4.dp.toPx(),
                center = Offset(size.width - step * 0.7f, size.height * 0.5f)
            )
            for (i in 1..7) {
                val x = i * step
                drawLine(
                    color = tickColor,
                    start = Offset(x, size.height * 0.55f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.2.dp.toPx()
                )
                val halfX = x - step * 0.5f
                drawLine(
                    color = tickColor.copy(alpha = 0.5f),
                    start = Offset(halfX, size.height * 0.72f),
                    end = Offset(halfX, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            (1..7).forEach { inch ->
                Text(
                    text = inch.toString(),
                    color = tickColor,
                    fontFamily = VennelaFonts.Mono,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Microsoft365RibbonBar(
    brandColor: Color,
    session: SuiteSessionState,
    onFontCycle: (() -> Unit)?,
    onZoomIn: (() -> Unit)?,
    onZoomOut: (() -> Unit)?,
    onOutlineToggle: (() -> Unit)?,
    onPrint: (() -> Unit)?
) {
    val palette = LocalVennelaPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        // Ribbon Tab Strip (Home | Layout | Review | View)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OfficeRibbonTab.entries.forEach { tab ->
                val active = session.ribbonTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { session.ribbonTab = tab }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.72f),
                        fontFamily = VennelaFonts.OnePlusDisplay,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) Color.White else Color.Transparent)
                    )
                }
            }
        }

        // Ribbon Command Shelf
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (palette.isDark) Color(0xFF1E222B) else Color(0xFFF3F2F1))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (session.ribbonTab) {
                OfficeRibbonTab.HOME -> {
                    if (onFontCycle != null) {
                        SuiteMiniChip("OnePlus Sans / Calibri", active = true, accentColor = brandColor, onClick = onFontCycle)
                    }
                    if (onZoomIn != null) {
                        SuiteMiniChip("A+ Grow Font", active = false, accentColor = brandColor, onClick = onZoomIn)
                    }
                    if (onZoomOut != null) {
                        SuiteMiniChip("A- Shrink Font", active = false, accentColor = brandColor, onClick = onZoomOut)
                    }
                    SuiteMiniChip(
                        label = "Highlight Mode",
                        active = session.immersiveLineFocus,
                        accentColor = brandColor,
                        onClick = { session.immersiveLineFocus = !session.immersiveLineFocus }
                    )
                }
                OfficeRibbonTab.LAYOUT -> {
                    SuiteMiniChip(
                        label = if (session.pageless) "Mobile Reflow" else "Print Layout",
                        active = !session.pageless,
                        accentColor = brandColor,
                        onClick = { session.pageless = !session.pageless }
                    )
                    SuiteMiniChip(
                        label = "Ruler Guides",
                        active = session.showRuler,
                        accentColor = brandColor,
                        onClick = { session.showRuler = !session.showRuler }
                    )
                    WpsPaperTone.entries.forEach { tone ->
                        SuiteMiniChip(
                            label = tone.label,
                            active = session.paperTone == tone,
                            accentColor = brandColor,
                            onClick = { session.paperTone = tone }
                        )
                    }
                }
                OfficeRibbonTab.REVIEW -> {
                    if (onOutlineToggle != null) {
                        SuiteMiniChip("Navigation Pane", active = false, accentColor = brandColor, onClick = onOutlineToggle)
                    }
                    SuiteMiniChip(
                        label = "Immersive Reader",
                        active = session.immersiveLineFocus,
                        accentColor = brandColor,
                        onClick = { session.immersiveLineFocus = !session.immersiveLineFocus }
                    )
                    if (onPrint != null) {
                        SuiteMiniChip("Print / PDF", active = false, accentColor = brandColor, onClick = onPrint)
                    }
                }
                OfficeRibbonTab.VIEW -> {
                    SuiteMiniChip(
                        label = "Line Focus Spotlight",
                        active = session.immersiveLineFocus,
                        accentColor = brandColor,
                        onClick = { session.immersiveLineFocus = !session.immersiveLineFocus }
                    )
                    SuiteMiniChip(
                        label = "Auto-Scroll",
                        active = session.autoScrollActive,
                        accentColor = brandColor,
                        onClick = { session.autoScrollActive = !session.autoScrollActive }
                    )
                    if (onZoomOut != null) {
                        SuiteMiniChip("Zoom -", active = false, accentColor = brandColor, onClick = onZoomOut)
                    }
                    if (onZoomIn != null) {
                        SuiteMiniChip("Zoom +", active = false, accentColor = brandColor, onClick = onZoomIn)
                    }
                }
            }
        }
    }
}

@Composable
private fun WpsSuperAppSubBar(
    brandColor: Color,
    session: SuiteSessionState,
    onExportPdf: (() -> Unit)?,
    onOutlineToggle: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Signature Chinese WPS Eye-Protection Green Mode
        SuiteMiniChip(
            label = "Eye-Care Green",
            active = session.paperTone == WpsPaperTone.EYE_CARE_GREEN,
            accentColor = Color(0xFF188038),
            onClick = {
                session.paperTone = if (session.paperTone == WpsPaperTone.EYE_CARE_GREEN) {
                    WpsPaperTone.PURE_WHITE
                } else {
                    WpsPaperTone.EYE_CARE_GREEN
                }
            }
        )
        SuiteMiniChip(
            label = "Parchment",
            active = session.paperTone == WpsPaperTone.WARM_PARCHMENT,
            accentColor = Color(0xFFB76E00),
            onClick = { session.paperTone = WpsPaperTone.WARM_PARCHMENT }
        )
        SuiteMiniChip(
            label = "Night Mode",
            active = session.paperTone == WpsPaperTone.NIGHT_OBSIDIAN,
            accentColor = brandColor,
            onClick = { session.paperTone = WpsPaperTone.NIGHT_OBSIDIAN }
        )
        SuiteMiniChip(
            label = if (session.autoScrollActive) "Auto-Scroll (${session.autoScrollSpeed}x)" else "Auto-Scroll",
            active = session.autoScrollActive,
            accentColor = brandColor,
            onClick = { session.autoScrollActive = !session.autoScrollActive }
        )
        if (session.autoScrollActive) {
            SuiteMiniChip(
                label = "Speed +",
                active = false,
                accentColor = brandColor,
                onClick = {
                    session.autoScrollSpeed = if (session.autoScrollSpeed >= 2.5f) 0.5f else session.autoScrollSpeed + 0.5f
                }
            )
        }
        if (onOutlineToggle != null) {
            SuiteMiniChip(
                label = "TOC Outline",
                active = false,
                accentColor = brandColor,
                onClick = onOutlineToggle
            )
        }
        if (onExportPdf != null) {
            SuiteMiniChip(
                label = "WPS Export PDF",
                active = false,
                accentColor = brandColor,
                onClick = onExportPdf
            )
        }
    }
}

@Composable
private fun LibreOfficeSubBar(
    brandColor: Color,
    session: SuiteSessionState,
    onZoomIn: (() -> Unit)?,
    onZoomOut: (() -> Unit)?,
    onPrint: (() -> Unit)?
) {
    val palette = LocalVennelaPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuiteMiniChip(
                label = "Crop Marks (┌┐)",
                active = session.libreMarginGuides,
                accentColor = brandColor,
                onClick = { session.libreMarginGuides = !session.libreMarginGuides }
            )
            SuiteMiniChip(
                label = "Line Numbers",
                active = session.libreLineNumbers,
                accentColor = brandColor,
                onClick = { session.libreLineNumbers = !session.libreLineNumbers }
            )
            SuiteMiniChip(
                label = "Ruler",
                active = session.showRuler,
                accentColor = brandColor,
                onClick = { session.showRuler = !session.showRuler }
            )
            if (onZoomOut != null) {
                SuiteMiniChip("Zoom -", active = false, accentColor = brandColor, onClick = onZoomOut)
            }
            if (onZoomIn != null) {
                SuiteMiniChip("Zoom +", active = false, accentColor = brandColor, onClick = onZoomIn)
            }
            if (onPrint != null) {
                SuiteMiniChip("Direct Print", active = false, accentColor = brandColor, onClick = onPrint)
            }
        }
        if (session.showRuler) {
            GoogleDocsRulerBar(brandColor = brandColor, isDark = palette.isDark)
        }
    }
}

@Composable
private fun OkenScannerSubBar(
    session: SuiteSessionState,
    onExportPdf: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14212B))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OkenScanFilter.entries.forEach { filter ->
            val selected = session.okenFilter == filter
            Box(
                modifier = Modifier
                    .testTag("oken_filter_${filter.id}")
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Color(0xFF00B894) else Color.White.copy(alpha = 0.10f))
                    .pressScale(0.94f) { session.okenFilter = filter }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = filter.label,
                    color = Color.White,
                    fontFamily = VennelaFonts.OnePlusDisplay,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (session.okenCropFrame) Color(0xFF0984E3) else Color.White.copy(alpha = 0.10f))
                .pressScale(0.94f) { session.okenCropFrame = !session.okenCropFrame }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Auto-Crop Frame",
                color = Color.White,
                fontFamily = VennelaFonts.OnePlusDisplay,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (session.okenWatermark) Color(0xFF00B894) else Color.White.copy(alpha = 0.10f))
                .pressScale(0.94f) { session.okenWatermark = !session.okenWatermark }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Watermark",
                color = Color.White,
                fontFamily = VennelaFonts.OnePlusDisplay,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (session.okenOfficialStamp) Color(0xFFD63031) else Color.White.copy(alpha = 0.10f))
                .pressScale(0.94f) { session.okenOfficialStamp = !session.okenOfficialStamp }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Official Seal",
                color = Color.White,
                fontFamily = VennelaFonts.OnePlusDisplay,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (onExportPdf != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00B894))
                    .pressScale(0.94f, onExportPdf)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "HD Scan PDF",
                    color = Color.White,
                    fontFamily = VennelaFonts.OnePlusDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SuiteMiniChip(
    label: String,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val bg = when {
        active -> accentColor.copy(alpha = 0.16f)
        palette.isDark -> Color.White.copy(alpha = 0.06f)
        else -> Color.Black.copy(alpha = 0.05f)
    }
    val fg = when {
        active -> accentColor
        else -> palette.textSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (active) accentColor.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .pressScale(0.95f, onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontFamily = VennelaFonts.OnePlusDisplay,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/* ------------------------------------------------------------------------- */
/* 2. Suite Paper Canvas & Oken Scanner Overlay                               */
/* ------------------------------------------------------------------------- */

/**
 * Renders the document/page sheet with the exact visual identity of the active
 * [ViewerSuiteStyle]:
 * - Google Docs A4 Print sheet or Pageless flow
 * - Microsoft 365 Word page with Immersive Reader Line Focus
 * - WPS Office Eye-Care Green (#C7EDCC) or Parchment page
 * - LibreOffice page with corner crop marks (`┌ ┐ └ ┘`) & line number gutter
 * - Oken Scanner HD scan bed with Magic Color / B&W contrast, turquoise auto-crop
 *   corners, security watermark, and official red seal stamp
 */
@Composable
fun SuitePaperContainer(
    activeSuite: ViewerSuiteStyle,
    session: SuiteSessionState,
    modifier: Modifier = Modifier,
    pageLabel: String = "Page 1",
    wordCount: Int = 0,
    charCount: Int = 0,
    zoomPercent: Int = 100,
    content: @Composable ColumnScope.(paperColor: Color, inkColor: Color, mutedInk: Color) -> Unit
) {
    val resolvedTone = when {
        activeSuite == ViewerSuiteStyle.OKEN_SCANNER -> when (session.okenFilter) {
            OkenScanFilter.CRISP_BW -> WpsPaperTone.PURE_WHITE
            OkenScanFilter.MAGIC_COLOR -> WpsPaperTone.PURE_WHITE
            OkenScanFilter.LIGHTEN -> WpsPaperTone.PURE_WHITE
            OkenScanFilter.GRAYSCALE -> WpsPaperTone.CALM_BLUE
            OkenScanFilter.ORIGINAL -> session.paperTone
        }
        else -> session.paperTone
    }

    val surroundColor = when (activeSuite) {
        ViewerSuiteStyle.OKEN_SCANNER -> Color(0xFF0E171D)
        else -> resolvedTone.canvasSurroundColor
    }

    val cornerRadius = if (session.pageless) 0.dp else 4.dp
    val horizontalPad = if (session.pageless) 0.dp else 10.dp
    val verticalPad = if (session.pageless) 0.dp else 12.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surroundColor)
            .padding(horizontal = horizontalPad, vertical = verticalPad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .shadow(
                    elevation = if (session.pageless) 0.dp else 8.dp,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(resolvedTone.paperColor)
                .border(
                    width = if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER && session.okenCropFrame) 2.dp else 1.dp,
                    color = when {
                        activeSuite == ViewerSuiteStyle.OKEN_SCANNER && session.okenCropFrame -> Color(0xFF00CEC9)
                        else -> resolvedTone.inkColor.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(cornerRadius)
                )
                .drawWithContent {
                    drawContent()

                    // LibreOffice corner crop marks (┌ ┐ └ ┘)
                    if (activeSuite == ViewerSuiteStyle.LIBRE_OFFICE && session.libreMarginGuides) {
                        val markColor = resolvedTone.mutedInkColor.copy(alpha = 0.45f)
                        val m = 18.dp.toPx()
                        val len = 12.dp.toPx()
                        val stroke = 1.2.dp.toPx()
                        // Top-left
                        drawLine(markColor, Offset(m, m), Offset(m + len, m), stroke)
                        drawLine(markColor, Offset(m, m), Offset(m, m + len), stroke)
                        // Top-right
                        drawLine(markColor, Offset(size.width - m, m), Offset(size.width - m - len, m), stroke)
                        drawLine(markColor, Offset(size.width - m, m), Offset(size.width - m, m + len), stroke)
                        // Bottom-left
                        drawLine(markColor, Offset(m, size.height - m), Offset(m + len, size.height - m), stroke)
                        drawLine(markColor, Offset(m, size.height - m), Offset(m, size.height - m - len), stroke)
                        // Bottom-right
                        drawLine(markColor, Offset(size.width - m, size.height - m), Offset(size.width - m - len, size.height - m), stroke)
                        drawLine(markColor, Offset(size.width - m, size.height - m), Offset(size.width - m, size.height - m - len), stroke)
                    }

                    // Microsoft 365 Immersive Reader Line Focus Spotlight
                    if (session.immersiveLineFocus) {
                        val bandTop = size.height * 0.28f
                        val bandBottom = size.height * 0.56f
                        drawRect(
                            color = Color.Black.copy(alpha = 0.42f),
                            topLeft = Offset.Zero,
                            size = Size(size.width, bandTop)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.42f),
                            topLeft = Offset(0f, bandBottom),
                            size = Size(size.width, (size.height - bandBottom).coerceAtLeast(0f))
                        )
                        drawLine(
                            color = Color(0xFF185ABD),
                            start = Offset(0f, bandTop),
                            end = Offset(size.width, bandTop),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF185ABD),
                            start = Offset(0f, bandBottom),
                            end = Offset(size.width, bandBottom),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Oken Scanner / CamScanner Smart Crop Corner Handles & Security Watermark
                    if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER) {
                        if (session.okenCropFrame) {
                            val handleColor = Color(0xFF00CEC9)
                            val r = 7.dp.toPx()
                            drawCircle(handleColor, r, Offset(8.dp.toPx(), 8.dp.toPx()))
                            drawCircle(handleColor, r, Offset(size.width - 8.dp.toPx(), 8.dp.toPx()))
                            drawCircle(handleColor, r, Offset(8.dp.toPx(), size.height - 8.dp.toPx()))
                            drawCircle(handleColor, r, Offset(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()))
                        }
                        if (session.okenWatermark) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.argb(26, 0, 150, 136)
                                    textSize = 18.dp.toPx()
                                    isFakeBoldText = true
                                }
                                val stepY = 180.dp.toPx()
                                var y = 120.dp.toPx()
                                while (y < size.height) {
                                    canvas.nativeCanvas.save()
                                    canvas.nativeCanvas.rotate(-24f, size.width * 0.5f, y)
                                    canvas.nativeCanvas.drawText(
                                        "OKEN HD SCAN • VENNELA 365 VERIFIED",
                                        size.width * 0.12f,
                                        y,
                                        paint
                                    )
                                    canvas.nativeCanvas.restore()
                                    y += stepY
                                }
                            }
                        }
                        if (session.okenOfficialStamp) {
                            val stampCenter = Offset(size.width - 78.dp.toPx(), 78.dp.toPx())
                            val stampColor = Color(0xFFD63031).copy(alpha = 0.78f)
                            drawCircle(
                                color = stampColor,
                                radius = 44.dp.toPx(),
                                center = stampCenter,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = stampColor,
                                radius = 38.dp.toPx(),
                                center = stampCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.argb(200, 214, 48, 49)
                                    textSize = 11.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.rotate(-14f, stampCenter.x, stampCenter.y)
                                canvas.nativeCanvas.drawText("VERIFIED SCAN", stampCenter.x, stampCenter.y - 4.dp.toPx(), paint)
                                canvas.nativeCanvas.drawText("VENNELA 365", stampCenter.x, stampCenter.y + 11.dp.toPx(), paint)
                                canvas.nativeCanvas.restore()
                            }
                        }
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Optional LibreOffice left-margin line numbering gutter
                if (activeSuite == ViewerSuiteStyle.LIBRE_OFFICE && session.libreLineNumbers) {
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .background(resolvedTone.inkColor.copy(alpha = 0.04f))
                            .padding(top = 26.dp, bottom = 22.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (line in 1..45) {
                            Text(
                                text = if (line == 1 || line % 5 == 0) "$line" else "·",
                                color = resolvedTone.mutedInkColor.copy(alpha = 0.55f),
                                fontFamily = VennelaFonts.Mono,
                                fontSize = 9.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    content(resolvedTone.paperColor, resolvedTone.inkColor, resolvedTone.mutedInkColor)
                }
            }
        }

        // ---- Desktop / Suite Status Footer Bar ----
        Spacer(modifier = Modifier.height(6.dp))
        SuiteFooterStatusBar(
            activeSuite = activeSuite,
            pageLabel = pageLabel,
            wordCount = wordCount,
            charCount = charCount,
            zoomPercent = zoomPercent,
            session = session
        )
    }
}

@Composable
fun SuiteFooterStatusBar(
    activeSuite: ViewerSuiteStyle,
    pageLabel: String,
    wordCount: Int,
    charCount: Int,
    zoomPercent: Int,
    session: SuiteSessionState
) {
    val palette = LocalVennelaPalette.current
    val bg = when (activeSuite) {
        ViewerSuiteStyle.LIBRE_OFFICE -> if (palette.isDark) Color(0xFF20242B) else Color(0xFFE5E9F0)
        ViewerSuiteStyle.MICROSOFT_365 -> Color(0xFF185ABD)
        ViewerSuiteStyle.OKEN_SCANNER -> Color(0xFF14212B)
        else -> palette.cardSurfaceStrong
    }
    val fg = when (activeSuite) {
        ViewerSuiteStyle.MICROSOFT_365, ViewerSuiteStyle.OKEN_SCANNER -> Color.White
        else -> palette.textSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 820.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$pageLabel  •  ${activeSuite.fullTitle}",
            color = fg,
            fontFamily = VennelaFonts.OnePlusDisplay,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = buildString {
                if (wordCount > 0) append("$wordCount words  |  $charCount chars  |  ")
                append(
                    when (activeSuite) {
                        ViewerSuiteStyle.OKEN_SCANNER -> session.okenFilter.label
                        ViewerSuiteStyle.WPS_SUPER -> session.paperTone.label
                        else -> "$zoomPercent%"
                    }
                )
            },
            color = fg.copy(alpha = 0.88f),
            fontFamily = VennelaFonts.Mono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SuitePaperContainer(
    suite: ViewerSuiteStyle,
    session: SuiteSessionState,
    lineCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (paperTone: WpsPaperTone, brandAccent: Color) -> Unit
) {
    val resolvedTone = when {
        suite == ViewerSuiteStyle.OKEN_SCANNER -> when (session.okenFilter) {
            OkenScanFilter.CRISP_BW, OkenScanFilter.MAGIC_COLOR, OkenScanFilter.LIGHTEN -> WpsPaperTone.PURE_WHITE
            OkenScanFilter.GRAYSCALE -> WpsPaperTone.CALM_BLUE
            OkenScanFilter.ORIGINAL -> session.paperTone
        }
        else -> session.paperTone
    }
    SuitePaperContainer(
        activeSuite = suite,
        session = session,
        modifier = modifier,
        pageLabel = "Lines: $lineCount",
        content = { _, _, _ ->
            content(resolvedTone, ViewerKind.DOCS.accentFor(suite))
        }
    )
}

@Composable
fun SuiteFooterStatusBar(
    suite: ViewerSuiteStyle,
    pageOrLineLabel: String,
    wordCount: Int,
    charCount: Int,
    zoomPercent: Int,
    extraBadge: String
) {
    val palette = LocalVennelaPalette.current
    val bg = when (suite) {
        ViewerSuiteStyle.LIBRE_OFFICE -> if (palette.isDark) Color(0xFF20242B) else Color(0xFFE5E9F0)
        ViewerSuiteStyle.MICROSOFT_365 -> Color(0xFF185ABD)
        ViewerSuiteStyle.OKEN_SCANNER -> Color(0xFF14212B)
        else -> palette.cardSurfaceStrong
    }
    val fg = when (suite) {
        ViewerSuiteStyle.MICROSOFT_365, ViewerSuiteStyle.OKEN_SCANNER -> Color.White
        else -> palette.textSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$pageOrLineLabel  •  ${suite.fullTitle}",
            color = fg,
            fontFamily = VennelaFonts.OnePlusDisplay,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildString {
                if (wordCount > 0) append("$wordCount words  •  ")
                append("$extraBadge  •  $zoomPercent%")
            },
            color = fg.copy(alpha = 0.90f),
            fontFamily = VennelaFonts.Mono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
