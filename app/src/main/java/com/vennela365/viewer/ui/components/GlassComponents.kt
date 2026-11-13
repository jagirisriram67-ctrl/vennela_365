package com.vennela365.viewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.theme.*
import com.vennela365.viewer.viewmodel.Screen

/**
 * The app atmosphere plus the backdrop that every liquid-glass surface frosts.
 *
 * The scenery is drawn once into a small cached bitmap, then published through
 * [LocalBackdropImage] so glass samples a blurred copy of exactly what sits
 * behind it. No photograph is shipped, so this costs a few kilobytes and still
 * recolours with the active theme and accent.
 */
@Composable
fun VennelaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val tier by FidelityGovernor.activeTier.collectAsState()
    val quality = when (tier) {
        FidelityTier.TIER_1_ULTRA -> 4
        FidelityTier.TIER_2_BALANCED -> 2
        FidelityTier.TIER_3_PERFORMANCE -> 0
    }

    LiquidBackdrop(
        modifier = modifier,
        render = { size ->
            LiquidAtmosphere.render(
                canvas = nativeCanvas,
                width = size.width,

                height = size.height,
                palette = palette,
                accent = accent,
                quality = quality
            )
        }
    ) {
        // A whisper of grain keeps large flat gradients from banding on 8-bit
        // panels. Aurora Lumen also lifts the top with a soft accent bloom and
        // eases the bottom scrim, so screens read luminous instead of murky.
        if (quality > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to accent.secondary.copy(alpha = if (palette.isDark) 0.10f else 0.06f),
                            0.45f to Color.Transparent,
                            1f to palette.backgroundBottom.copy(alpha = if (palette.isDark) 0.34f else 0.20f)
                        )
                    )
            )
        }
        content()
    }
}

/**
 * A frosted top bar with an optional leading glyph.
 *
 * A thin alias over the app's single [ViewerTopBar] so legacy call sites keep
 * working while presenting exactly the same chrome as every other screen.
 */
@Composable
fun TopGlassBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    iconVector: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    actions: @Composable RowScope.() -> Unit = {}
) {
    ViewerTopBar(
        title = title,
        subtitle = subtitle,
        onBack = onBackClick,
        leadingIcon = iconVector,
        leadingIconTint = iconTint,
        statusInset = true,
        actions = actions
    )
}

@Composable
fun TopGlassBar(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onLeadingClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    TopGlassBar(
        title = title,
        subtitle = subtitle,
        onBackClick = onLeadingClick,
        iconVector = null
    ) {
        if (trailingIcon != null && onTrailingClick != null) {
            IconButton(onClick = onTrailingClick) {
                Icon(trailingIcon, contentDescription = null, tint = LocalVennelaPalette.current.textPrimary)
            }
        }
    }
}

@Composable
fun FloatingDockNav(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = when (currentScreen) {
        Screen.Home -> "home"
        Screen.FileExplorer -> "files"
        Screen.UniversalSearch -> "search"
        Screen.Favorites -> "favorites"
        Screen.Settings -> "settings"
        else -> "home"
    }
    Box(modifier = modifier) {
        FloatingDockNav(
            activeTab = activeTab,
            onTabSelected = { tab ->
                when (tab) {
                    "home" -> onNavigate(Screen.Home)
                    "files" -> onNavigate(Screen.FileExplorer)
                    "search" -> onNavigate(Screen.UniversalSearch)
                    "favorites" -> onNavigate(Screen.Favorites)
                    "settings" -> onNavigate(Screen.Settings)
                }
            }
        )
    }
}

fun getCategoryColor(category: FormatCategory): Color {
    return when (category) {
        FormatCategory.DOCUMENTS -> CatBlue
        FormatCategory.SPREADSHEETS -> CatEmerald
        FormatCategory.PRESENTATIONS -> CatOrange
        FormatCategory.EBOOKS -> CatCyan
        FormatCategory.IMAGES -> Color(0xFF22D3B8)
        FormatCategory.AUDIO -> CatRose
        FormatCategory.VIDEO -> CatViolet
        FormatCategory.ARCHIVES -> CatAmber
        FormatCategory.CODE -> Color(0xFF5B8DFF)
        FormatCategory.CONFIG -> CatCyan
        FormatCategory.MARKDOWN -> CatViolet
        FormatCategory.DATABASES -> Color(0xFFA87BFF)
        FormatCategory.CAD_3D -> CatEmerald
        FormatCategory.EMAIL_CONTACTS -> Color(0xFF38BDF8)
        FormatCategory.FONTS -> CatAmber
        FormatCategory.LOGS -> CatSlate
        FormatCategory.BINARIES -> Color(0xFF818CF8)
        FormatCategory.OTHERS -> CatSlate
    }
}

fun getCategoryIcon(category: FormatCategory): ImageVector {
    return when (category) {
        FormatCategory.DOCUMENTS -> Icons.Default.Description
        FormatCategory.SPREADSHEETS -> Icons.Default.TableChart
        FormatCategory.PRESENTATIONS -> Icons.Default.Slideshow
        FormatCategory.EBOOKS -> Icons.AutoMirrored.Filled.MenuBook
        FormatCategory.IMAGES -> Icons.Default.Image
        FormatCategory.AUDIO -> Icons.Default.MusicNote
        FormatCategory.VIDEO -> Icons.Default.PlayCircle
        FormatCategory.ARCHIVES -> Icons.Default.Archive
        FormatCategory.CODE -> Icons.Default.Code
        FormatCategory.CONFIG -> Icons.Default.Settings
        FormatCategory.MARKDOWN -> Icons.Default.AutoStories
        FormatCategory.DATABASES -> Icons.Default.Storage
        FormatCategory.CAD_3D -> Icons.Default.ViewInAr
        FormatCategory.EMAIL_CONTACTS -> Icons.Default.Mail
        FormatCategory.FONTS -> Icons.Default.FontDownload
        FormatCategory.LOGS -> Icons.AutoMirrored.Filled.ReceiptLong
        FormatCategory.BINARIES -> Icons.Default.Memory
        FormatCategory.OTHERS -> Icons.Default.Category
    }
}

@Composable
fun FileBrandBadge(
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val ext = extension.lowercase()
    val (bgColor, iconVector, badgeText) = when (ext) {
        "pdf" -> Triple(Color(0xFFFF5470), Icons.Default.PictureAsPdf, "PDF")
        "doc", "docx" -> Triple(Color(0xFF4C8DFF), Icons.Default.Description, "W")
        "xls", "xlsx", "csv" -> Triple(Color(0xFF19D89B), Icons.Default.TableChart, "X")
        "ppt", "pptx" -> Triple(Color(0xFFFF9A3D), Icons.Default.Slideshow, "P")
        "jpg", "jpeg", "png", "webp", "gif" -> Triple(Color(0xFF1FC9B4), Icons.Default.Image, null)
        "mp3", "wav", "flac", "aac" -> Triple(Color(0xFFFF4D9E), Icons.Default.MusicNote, null)
        "mp4", "mkv", "mov", "avi" -> Triple(Color(0xFFA06BFF), Icons.Default.PlayArrow, null)
        "zip", "rar", "7z", "tar" -> Triple(Color(0xFFFFBE3D), Icons.Default.FolderZip, null)
        "py" -> Triple(Color(0xFF52A0FF), Icons.Default.Code, "PY")
        "java", "kt", "js", "ts", "html", "css" -> Triple(Color(0xFF7C86FF), Icons.Default.Code, "</>")
        "md", "markdown" -> Triple(Color(0xFF6E6BFF), Icons.Default.AutoStories, "M↓")
        "mmd", "mermaid" -> Triple(Color(0xFFBE7CFF), Icons.Default.AccountTree, null)
        else -> Triple(Color(0xFF7A8BB5), Icons.AutoMirrored.Filled.InsertDriveFile, ext.take(3).uppercase())
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusBadge),
                clip = false,
                ambientColor = bgColor.copy(alpha = 0.60f),
                spotColor = bgColor.copy(alpha = 0.60f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusBadge))
            .background(
                Brush.linearGradient(
                    listOf(
                        bgColor.copy(alpha = 1f),
                        bgColor.copy(alpha = 0.86f),
                        bgColor.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))
                ),
                RoundedCornerShape(VennelaTokens.radiusBadge)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (badgeText != null && badgeText.length <= 3) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = if (badgeText.length <= 2) 16.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((size.value * 0.55f).dp)
            )
        }
    }
}

// ============ SPEC §4.2 — REMAINING REUSABLE COMPONENTS ============

/** Alias: spec name for the frosted top bar. */
@Composable
fun VennelaAppBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    iconVector: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    actions: @Composable RowScope.() -> Unit = {}
) = TopGlassBar(title, subtitle, onBackClick, iconVector, iconTint, actions)

/**
 * A circular icon button.
 *
 * A thin alias over the app's single [ReaderIconButton], so it no longer
 * reserves a 48dp square that inflated the bars it sat in.
 */
@Composable
fun VennelaIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    ReaderIconButton(
        icon = icon,
        contentDescription = contentDescription.orEmpty(),
        modifier = modifier,
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun VennelaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search files, folders, types...",
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusPill),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.45f),
                spotColor = accent.secondary.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.86f),
                        palette.cardSurface.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.22f),
                        accent.primary.copy(alpha = 0.45f)
                    )
                ),
                RoundedCornerShape(VennelaTokens.radiusPill)
            )
            .padding(horizontal = 14.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconMd))
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = palette.textPrimary, fontSize = VennelaTokens.bodySmall),
            cursorBrush = SolidColor(accent.secondary),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = palette.textTertiary, fontSize = VennelaTokens.bodySmall)
                inner()
            }
        )
        if (value.isNotEmpty()) {
            ReaderIconButton(
                icon = Icons.Default.Clear,
                contentDescription = "Clear search",
                onClick = { onValueChange("") }
            )
        }
    }
}

@Composable
fun VennelaBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) = FloatingDockNav(currentScreen = currentScreen, onNavigate = onNavigate, modifier = modifier)

@Composable
fun VennelaFileRow(
    name: String,
    meta: String,
    extension: String,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileBrandBadge(extension = extension, size = 46.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(meta, color = palette.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isFavorite) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = CatRose, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = palette.textSecondary)
        }
    }
}

@Composable
fun VennelaFileGridCard(
    name: String,
    meta: String,
    extension: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.40f),
                spotColor = accent.secondary.copy(alpha = 0.40f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.82f),
                        palette.cardSurface.copy(alpha = 0.62f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                RoundedCornerShape(VennelaTokens.radiusCardLarge)
            )
            .pressScale(pressedScale = 0.955f, onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileBrandBadge(extension = extension, size = 52.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(name, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(meta, color = palette.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Adaptive file grid: phones 2-col → tablets/foldables more (spec §4.5). */
@Composable
fun VennelaAdaptiveFileGrid(
    files: List<Triple<String, String, String>>,
    onFileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(files.size) { i ->
            val (name, meta, ext) = files[i]
            VennelaFileGridCard(
                name = name,
                meta = meta,
                extension = ext,
                onClick = { onFileClick(i) },
                modifier = Modifier.staggeredEntrance(i)
            )
        }
    }
}

@Composable
fun VennelaFileTypeIcon(
    extension: String,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val accent = LocalVennelaAccent.current
    val resolvedTint = if (tint == Color.Unspecified) accent.secondary else tint
    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusChip),
                clip = false,
                ambientColor = resolvedTint.copy(alpha = 0.55f),
                spotColor = resolvedTint.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .background(
                Brush.linearGradient(
                    listOf(
                        resolvedTint.copy(alpha = 0.34f),
                        resolvedTint.copy(alpha = 0.16f)
                    )
                )
            )
            .border(1.dp, resolvedTint.copy(alpha = 0.62f), RoundedCornerShape(VennelaTokens.radiusChip)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = extension, tint = resolvedTint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun VennelaMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.4f))
    }
}

@Composable
fun VennelaSegmentedControl(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.35f),
                spotColor = accent.secondary.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.80f),
                        palette.cardSurface.copy(alpha = 0.62f)
                    )
                )
            )
            .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusCardLarge))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val sel = opt == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                    .then(
                        if (sel) {
                            Modifier
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(VennelaTokens.radiusControl),
                                    clip = false,
                                    ambientColor = accent.primary.copy(alpha = 0.65f),
                                    spotColor = accent.secondary.copy(alpha = 0.65f)
                                )
                                .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.30f),
                                    RoundedCornerShape(VennelaTokens.radiusControl)
                                )
                        } else {
                            Modifier
                        }
                    )
                    .pressScale(pressedScale = 0.95f) { onSelect(opt) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opt, color = if (sel) Color.White else palette.textSecondary, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun VennelaBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(topStart = VennelaTokens.radiusSheet, topEnd = VennelaTokens.radiusSheet),
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.50f),
                spotColor = accent.secondary.copy(alpha = 0.50f)
            )
            .clip(RoundedCornerShape(topStart = VennelaTokens.radiusSheet, topEnd = VennelaTokens.radiusSheet))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.94f),
                        palette.cardSurface.copy(alpha = 0.86f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(accent.secondary.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))
                ),
                RoundedCornerShape(topStart = VennelaTokens.radiusSheet, topEnd = VennelaTokens.radiusSheet)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.secondary.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(title, color = palette.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textSecondary)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        content()
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/* ------------------------------------------------------------------------- */
/* State screens                                                              */
/*                                                                            */
/* Empty, error and loading share one construction: a glyph, a headline, a   */
/* line of explanation and optional actions. Keeping them identical is what  */
/* makes the app feel like one product rather than a pile of screens.        */
/* ------------------------------------------------------------------------- */

/** The round glyph that opens every state screen. */
@Composable
private fun StateGlyph(
    icon: ImageVector,
    tint: Color,
    glow: Color,
    size: Dp = 58.dp,
    iconSize: Dp = 26.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = glow.copy(alpha = 0.55f),
                spotColor = glow.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.40f), tint.copy(alpha = 0.18f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.60f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** A pill action used by the state screens. */
@Composable
private fun StateAction(
    label: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .then(
                if (primary) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(50),
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.60f),
                        spotColor = accent.secondary.copy(alpha = 0.60f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(50))
            .then(
                if (primary) {
                    Modifier.background(
                        Brush.horizontalGradient(listOf(accent.primary, accent.secondary))
                    )
                } else {
                    Modifier.background(palette.cardSurface.copy(alpha = 0.50f))
                }
            )
            .border(
                1.dp,
                if (primary) Color.White.copy(alpha = 0.30f) else accent.secondary.copy(alpha = 0.50f),
                RoundedCornerShape(50)
            )
            .pressScale(pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (primary) Color.White else palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun VennelaEmptyState(
    title: String,
    hint: String,
    icon: ImageVector = Icons.Default.FolderOpen,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(VennelaTokens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StateGlyph(icon = icon, tint = accent.primary, glow = accent.secondary)
        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
        Text(
            text = hint,
            color = palette.textSecondary,
            fontSize = VennelaTokens.bodySmall,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            StateAction(label = actionLabel, primary = true, onClick = onAction)
        }
    }
}

@Composable
fun VennelaErrorState(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
    onDetails: (() -> Unit)? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusSheet),
                clip = false,
                ambientColor = errorCoral.copy(alpha = 0.45f),
                spotColor = errorCoral.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusSheet))
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.88f),
                        palette.cardSurface.copy(alpha = 0.70f)
                    )
                )
            )
            .border(1.dp, errorCoral.copy(alpha = 0.65f), RoundedCornerShape(VennelaTokens.radiusSheet))
            .padding(VennelaTokens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StateGlyph(
            icon = Icons.Default.ErrorOutline,
            tint = errorCoral,
            glow = errorCoral
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
        Text(
            text = message,
            color = palette.textSecondary,
            fontSize = VennelaTokens.bodySmall,
            textAlign = TextAlign.Center
        )
        if (onRetry != null || onDetails != null) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                onRetry?.let { StateAction(label = "Retry", primary = true, onClick = it) }
                onDetails?.let { StateAction(label = "Details", primary = false, onClick = it) }
            }
        }
    }
}

@Composable
fun VennelaLoadingState(
    label: String = "Opening…",
    progress: Float? = null,
    onCancel: (() -> Unit)? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(VennelaTokens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                color = accent.secondary,
                trackColor = palette.textPrimary.copy(alpha = 0.14f)
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Text(
                text = "${(progress * 100).toInt()}% • $label",
                color = palette.textSecondary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Medium
            )
        } else {
            CircularProgressIndicator(
                color = accent.secondary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Text(
                text = label,
                color = palette.textSecondary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (onCancel != null) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            StateAction(label = "Cancel", primary = false, onClick = onCancel)
        }
    }
}

@Composable
fun VennelaViewerToolbar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onShare: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false
) {
    TopGlassBar(title = title, subtitle = subtitle, onBackClick = onBack) {
        val palette = LocalVennelaPalette.current
        onFavorite?.let {
            IconButton(onClick = it) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) CatRose else palette.textPrimary)
            }
        }
        onShare?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = palette.textPrimary)
            }
        }
        onInfo?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Info, contentDescription = "Details", tint = palette.textPrimary)
            }
        }
    }
}

@Composable
fun VennelaZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.cardSurfaceStrong)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onZoomOut, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = palette.textPrimary)
        }
        IconButton(onClick = onFit, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.FitScreen, contentDescription = "Fit to screen", tint = accent.secondary)
        }
        IconButton(onClick = onZoomIn, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = palette.textPrimary)
        }
    }
}

@Composable
fun VennelaBreadcrumbs(
    segments: List<String>,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { i, seg ->
            if (i > 0) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = palette.textTertiary, modifier = Modifier.size(16.dp))
            }
            Text(
                seg,
                color = if (i == segments.lastIndex) palette.textPrimary else accent.secondary,
                fontSize = 13.sp,
                fontWeight = if (i == segments.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onSegmentClick(i) }.padding(horizontal = 2.dp, vertical = 6.dp)
            )
        }
    }
}


/**
 * Natural iOS-inspired physics spring touch feedback modifier.
 * Gently scales on press with zero latency and returns with subtle elasticity.
 */
fun Modifier.bouncyTouch(
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BouncyTouchScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Reusable frosted glass interactive action chip.
 */
@Composable
fun GlassChip(
    text: String,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .then(
                if (isActive) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(VennelaTokens.radiusControl),
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.65f),
                        spotColor = accent.secondary.copy(alpha = 0.65f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(
                if (isActive) {
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.92f),
                            accent.secondary.copy(alpha = 0.62f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            palette.cardSurface.copy(alpha = 0.72f),
                            palette.cardSurface.copy(alpha = 0.52f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isActive) Color.White.copy(alpha = 0.32f) else accent.secondary.copy(alpha = 0.30f),
                shape = RoundedCornerShape(VennelaTokens.radiusControl)
            )
            .bouncyTouch(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else accent.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = if (isActive) Color.White else palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
