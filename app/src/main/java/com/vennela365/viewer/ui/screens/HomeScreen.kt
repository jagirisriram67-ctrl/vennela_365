package com.vennela365.viewer.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.theme.CatRose
import com.vennela365.viewer.theme.LiquidGlassSpec
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.SuiteStyleSwitcherStrip
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.rememberLazyScrollSignal
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

private val homeImageExtensions = setOf(
    "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "jfif"
)

private fun isImage(extension: String) = extension.lowercase() in homeImageExtensions

data class CategoryCardItem(
    val title: String,
    val countLabel: String,
    val icon: ImageVector,
    val color: Color,
    val gradientEnd: Color,
    val category: FormatCategory
)

fun categoryItems(): List<CategoryCardItem> {
    fun count(category: FormatCategory) = FormatRegistry.getByCategory(category).size
    return listOf(
        CategoryCardItem("Documents", "${count(FormatCategory.DOCUMENTS)} formats", Icons.Default.Description, Color(0xFF2F8BFF), Color(0xFF0A66D6), FormatCategory.DOCUMENTS),
        CategoryCardItem("Images", "${count(FormatCategory.IMAGES)} formats", Icons.Default.Image, Color(0xFF14C9A4), Color(0xFF0A9378), FormatCategory.IMAGES),
        CategoryCardItem("Videos", "${count(FormatCategory.VIDEO)} formats", Icons.Default.PlayArrow, Color(0xFF9E6BFF), Color(0xFF6E35E8), FormatCategory.VIDEO),
        CategoryCardItem("Audio", "${count(FormatCategory.AUDIO)} formats", Icons.Default.MusicNote, Color(0xFF28A9FF), Color(0xFF137CE2), FormatCategory.AUDIO),
        CategoryCardItem("Archives", "${count(FormatCategory.ARCHIVES)} formats", Icons.Default.FolderZip, Color(0xFFF5A623), Color(0xFFD47B00), FormatCategory.ARCHIVES),
        CategoryCardItem("Code", "${count(FormatCategory.CODE)} formats", Icons.Default.Code, Color(0xFF6B7CFF), Color(0xFF4750DB), FormatCategory.CODE),
        CategoryCardItem("Ebooks", "${count(FormatCategory.EBOOKS)} formats", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF00BCD4), Color(0xFF008B9E), FormatCategory.EBOOKS),
        CategoryCardItem("Spreadsheets", "${count(FormatCategory.SPREADSHEETS)} formats", Icons.Default.TableChart, Color(0xFF1ED760), Color(0xFF0F9D43), FormatCategory.SPREADSHEETS)
    )
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val activeSuite by viewModel.preferredSuiteStyle.collectAsState()
    val palette = LocalVennelaPalette.current
    val context = LocalContext.current
    val categories = remember { categoryItems() }
    val listState = rememberLazyListState()

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName: String? = null
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && cursor.moveToFirst()) displayName = cursor.getString(idx)
                }
            }
            viewModel.openUri(uri, displayName ?: uri.lastPathSegment)
        }
    }

    LiquidPage(
        scrollState = rememberLazyScrollSignal(listState),
        dock = {
            FloatingDockNav(
                activeTab = "home",
                onTabSelected = { tab ->
                    when (tab) {
                        "files" -> viewModel.navigateTo(Screen.AllFiles)
                        "search" -> viewModel.navigateTo(Screen.UniversalSearch)
                        "favorites" -> viewModel.navigateTo(Screen.Favorites)
                        "settings" -> viewModel.navigateTo(Screen.Settings)
                    }
                }
            )
        }
    ) { pagePadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            item(key = "home_header") {
                HomeHeader(
                    onSearchClick = { viewModel.navigateTo(Screen.UniversalSearch) },
                    onSettingsClick = { viewModel.navigateTo(Screen.Settings) }
                )
            }

            item(key = "home_search") {
                HomeSearchBar(
                    onSearchClick = { viewModel.navigateTo(Screen.UniversalSearch) },
                    onOpenPickerClick = { openDocumentLauncher.launch(arrayOf("*/*")) }
                )
            }

            item(key = "home_suite_strip") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FILE VIEWING ENGINE • ${activeSuite.badge.uppercase()}",
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.OnePlusDisplay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                        Text(
                            text = activeSuite.subtitle,
                            color = activeSuite.brandPrimary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SuiteStyleSwitcherStrip(
                        activeSuite = activeSuite,
                        onSelectSuite = { viewModel.setPreferredSuiteStyle(it) },
                        isDark = palette.isDark
                    )
                }
            }

            item(key = "home_categories") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.chunked(2).forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEachIndexed { colIndex, item ->
                                val index = rowIndex * 2 + colIndex
                                CategoryCard(
                                    item = item,
                                    modifier = Modifier
                                        .weight(1f)
                                        .staggeredEntrance(index)
                                        .testTag("home_category_${item.category.name.lowercase()}"),
                                    onClick = {
                                        viewModel.setCategory(item.category)
                                        viewModel.navigateTo(Screen.Categories)
                                    }
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "recent_header") {
                SectionHeader(
                    title = "Recent Files",
                    actionLabel = "See all",
                    onAction = { viewModel.navigateTo(Screen.RecentFiles) }
                )
            }

            item(key = "recent_card") {
                RecentFilesGlassCard(
                    recentFiles = recentFiles.take(5),
                    onFileClick = { viewModel.openFile(it) },
                    onMoreClick = {
                        viewModel.selectFile(it)
                        viewModel.navigateTo(Screen.FileMetadata)
                    },
                    onOpenFileClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                    onBrowseClick = { viewModel.navigateTo(Screen.AllFiles) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationBase)) }

    val barShape = RoundedCornerShape(VennelaTokens.radiusPill)
    val barSpec = liquidGlassSpec(
        alpha = if (palette.isDark) 0.72f else 0.86f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationBar,
        sheen = 0.24f,
        highlight = 0.20f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                alpha = entrance.value
                translationY = (1f - entrance.value) * 10f
            }
            .liquidGlass(barSpec, barShape)
            .padding(start = 9.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false,
                    ambientColor = accent.primary.copy(alpha = 0.42f),
                    spotColor = accent.secondary.copy(alpha = 0.42f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.primary,
                            accent.secondary.copy(alpha = 0.88f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.58f),
                            Color.White.copy(alpha = 0.14f)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NightlightRound,
                contentDescription = "Vennela 365",
                tint = Color(0xFFFDE68A),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Vennela 365",
                color = palette.textPrimary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Universal File Studio",
                color = palette.textSecondary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HomeHeaderIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search files",
                modifier = Modifier.testTag("home_search_btn"),
                onClick = onSearchClick
            )
            HomeHeaderIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.testTag("home_settings_btn"),
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun HomeHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val fillBrush = if (palette.isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.92f),
                palette.cardSurface.copy(alpha = 0.74f)
            )
        )
    }
    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (palette.isDark) 0.30f else 0.88f),
            Color.White.copy(alpha = 0.10f)
        )
    )
    Box(
        modifier = modifier
            .size(VennelaTokens.controlSm)
            .clip(CircleShape)
            .background(fillBrush, CircleShape)
            .border(1.dp, rimBrush, CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.textPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HomeSearchBar(
    onSearchClick: () -> Unit,
    onOpenPickerClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = if (palette.isDark) 0.60f else 0.84f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationCard,
        sheen = 0.22f,
        highlight = 0.18f
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .testTag("home_search_bar")
            .pressScale(pressedScale = 0.985f, onClick = onSearchClick)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = accent.secondary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Search files, folders, formats…",
            color = palette.textSecondary,
            fontFamily = VennelaFonts.Sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val openFill = if (palette.isDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    accent.primary.copy(alpha = 0.36f),
                    accent.secondary.copy(alpha = 0.24f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.96f),
                    accent.primary.copy(alpha = 0.18f)
                )
            )
        }
        val openRim = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (palette.isDark) 0.56f else 0.92f),
                accent.secondary.copy(alpha = 0.42f)
            )
        )
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(openFill)
                .border(1.dp, openRim, RoundedCornerShape(VennelaTokens.radiusPill))
                .testTag("home_quick_open_btn")
                .pressScale(pressedScale = 0.94f, onClick = onOpenPickerClick)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Open file from device",
                    tint = if (palette.isDark) Color.White else accent.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Open",
                    color = if (palette.isDark) Color.White else accent.primary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryCard(
    item: CategoryCardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val shape = RoundedCornerShape(VennelaTokens.radiusTile)
    val spec = LiquidGlassSpec(
        tint = if (palette.isDark) {
            Color(0xFF171F35).copy(alpha = 0.68f)
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        tintEnd = item.color.copy(alpha = if (palette.isDark) 0.16f else 0.12f),
        frost = VennelaTokens.frostRegular,
        backdropAlpha = 0.72f,
        rim = if (palette.isDark) {
            item.color.copy(alpha = 0.36f)
        } else {
            Color.White.copy(alpha = 0.90f)
        },
        rimWidth = 1.dp,
        sheen = 0.22f,
        elevation = 8.dp,
        shadowColor = if (palette.isDark) {
            Color.Black.copy(alpha = 0.42f)
        } else {
            item.color.copy(alpha = 0.14f)
        },
        highlight = 0.18f
    )

    Row(
        modifier = modifier
            .liquidGlass(spec, shape)
            .pressScale(pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false,
                    ambientColor = item.color.copy(alpha = 0.40f),
                    spotColor = item.gradientEnd.copy(alpha = 0.40f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(item.color, item.gradientEnd)))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.56f), Color.White.copy(alpha = 0.10f))
                    ),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = palette.textPrimary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.countLabel,
                color = palette.textSecondary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RecentFilesGlassCard(
    recentFiles: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onMoreClick: (FileItem) -> Unit,
    onOpenFileClick: () -> Unit,
    onBrowseClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        alpha = if (palette.isDark) 0.44f else 0.72f
    ) {
        if (recentFiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.primary.copy(alpha = 0.16f))
                        .border(1.dp, accent.secondary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = accent.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No recent files yet",
                    color = palette.textPrimary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Open any document, spreadsheet, PDF, or archive on your device.",
                    color = palette.textSecondary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidButton(
                        label = "Open File",
                        icon = Icons.Default.Description,
                        onClick = onOpenFileClick,
                        fillWidth = false,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                            .background(palette.cardSurfaceStrong.copy(alpha = 0.65f))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusPill))
                            .pressScale(pressedScale = 0.96f, onClick = onBrowseClick)
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Browse Storage",
                            color = palette.textPrimary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(accent.primary.copy(alpha = 0.16f))
                        .border(0.75.dp, accent.secondary.copy(alpha = 0.50f), RoundedCornerShape(VennelaTokens.radiusPill))
                        .pressScale(pressedScale = 0.97f) {
                            val pdfFile = generateShowcasePdf(context)
                            onFileClick(
                                FileItem(
                                    id = Uri.fromFile(pdfFile).toString(),
                                    name = pdfFile.name,
                                    uri = Uri.fromFile(pdfFile),
                                    extension = "pdf",
                                    sizeBytes = pdfFile.length(),
                                    formatInfo = FormatRegistry.lookup("pdf")
                                )
                            )
                        }
                        .padding(vertical = 11.dp, horizontal = 16.dp)
                        .testTag("home_launch_pdf_showcase"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Launch 5-Page Pro PDF Studio Showcase →",
                        color = accent.secondary,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                recentFiles.forEachIndexed { index, file ->
                    RecentFileRow(
                        file = file,
                        onClick = { onFileClick(file) },
                        onMoreClick = { onMoreClick(file) },
                        modifier = Modifier.staggeredEntrance(index)
                    )
                    if (index < recentFiles.lastIndex) {
                        GlassDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentFileRow(
    file: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.985f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.uri != null && isImage(file.extension)) {
            AsyncImage(
                model = file.uri,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.cardSurfaceStrong)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            )
        } else {
            FileBrandBadge(extension = file.extension, size = 44.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = palette.textPrimary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val typeLabel = file.extension.uppercase().ifBlank { "FILE" }
            Text(
                text = "$typeLabel • ${file.formattedSize} • ${file.formattedDate}",
                color = palette.textSecondary,
                fontFamily = VennelaFonts.Sans,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (file.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = CatRose,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "File details",
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
