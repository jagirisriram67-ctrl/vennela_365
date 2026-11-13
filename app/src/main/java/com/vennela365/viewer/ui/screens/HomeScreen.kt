package com.vennela365.viewer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.storage.StorageVolumeInfo
import com.vennela365.viewer.storage.VolumeKind
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.pressScale
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
        CategoryCardItem("Documents", "${count(FormatCategory.DOCUMENTS)} formats", Icons.Default.Description, Color(0xFF00B0FF), Color(0xFF0077D4), FormatCategory.DOCUMENTS),
        CategoryCardItem("Code & Dev", "${count(FormatCategory.CODE)} formats", Icons.Default.Code, Color(0xFF8B5CF6), Color(0xFF5B21B6), FormatCategory.CODE),
        CategoryCardItem("Spreadsheets", "${count(FormatCategory.SPREADSHEETS)} formats", Icons.Default.Description, Color(0xFF00E676), Color(0xFF00964A), FormatCategory.SPREADSHEETS),
        CategoryCardItem("Images & Art", "${count(FormatCategory.IMAGES)} formats", Icons.Default.Image, Color(0xFF00E5FF), Color(0xFF0098A6), FormatCategory.IMAGES),
        CategoryCardItem("Audio & Sound", "${count(FormatCategory.AUDIO)} formats", Icons.Default.MusicNote, Color(0xFFFF4081), Color(0xFFC2185B), FormatCategory.AUDIO),
        CategoryCardItem("Cinema & Video", "${count(FormatCategory.VIDEO)} formats", Icons.Default.PlayArrow, Color(0xFFB388FF), Color(0xFF7C4DFF), FormatCategory.VIDEO),
        CategoryCardItem("Archives & Zip", "${count(FormatCategory.ARCHIVES)} formats", Icons.Default.FolderZip, Color(0xFFFFAB00), Color(0xFFD67E00), FormatCategory.ARCHIVES),
        CategoryCardItem("3D & Spatial", "${count(FormatCategory.CAD_3D)} formats", Icons.Default.ViewInAr, Color(0xFFFF6D00), Color(0xFFE65100), FormatCategory.CAD_3D)
    )
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val volumes by viewModel.volumes.collectAsState()
    val allFilesGranted by viewModel.allFilesGranted.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val categories = remember { categoryItems() }

    LiquidPage(
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
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            // 1. Futuristic Floating Command Island Header
            item(key = "command_island") {
                HomeCommandIsland(viewModel)
            }

            // 2. Liquid Search Hologram Capsule
            item(key = "search_capsule") {
                HomeSearchCapsule(onClick = { viewModel.navigateTo(Screen.UniversalSearch) })
            }

            // 3. Quick Reflex Power Hub Chips
            item(key = "power_hub") {
                QuickActions(
                    onLens = { viewModel.navigateTo(Screen.UniversalLensViewer) },
                    onBrowse = { viewModel.navigateTo(Screen.AllFiles) },
                    onThemes = { viewModel.navigateTo(Screen.ThemeSelection) },
                    onFidelity = { viewModel.navigateTo(Screen.ThemeSelection) }
                )
            }

            // 4. Hero Format Matrix Bento Hub
            item(key = "format_matrix") {
                FormatMatrixBentoCard(
                    categories = categories,
                    onCategoryClick = { cat ->
                        viewModel.setCategory(cat)
                        viewModel.navigateTo(Screen.Categories)
                    },
                    onOpenAllFormats = {
                        viewModel.setCategory(null)
                        viewModel.navigateTo(Screen.Categories)
                    }
                )
            }

            // 5. Horizontal 3D Kinetic Carousel of Recent Files
            item(key = "recent_carousel_header") {
                SectionHeader(
                    title = "Recent Documents",
                    actionLabel = "View Library",
                    onAction = { viewModel.navigateTo(Screen.RecentFiles) }
                )
            }

            item(key = "recent_carousel") {
                RecentFilesKineticCarousel(
                    recentFiles = recentFiles,
                    onFileClick = { viewModel.openFile(it) },
                    onBrowseClick = { viewModel.navigateTo(Screen.AllFiles) }
                )
            }

            // 6. Device Hardware Storage Console
            item(key = "storage_header") {
                SectionHeader(
                    title = "Storage Hardware",
                    actionLabel = "File Explorer",
                    onAction = { viewModel.navigateTo(Screen.AllFiles) }
                )
            }

            item(key = "storage_console") {
                HomeStorageConsole(
                    volumes = volumes,
                    granted = allFilesGranted,
                    onOpenAll = { viewModel.navigateTo(Screen.AllFiles) },
                    onOpenVolume = { path ->
                        viewModel.openDevicePath(path)
                        viewModel.navigateTo(Screen.AllFiles)
                    }
                )
            }

            // 7. System Engine Status Footer
            item(key = "engine_status_footer") {
                EngineStatusFooter(
                    palette = palette,
                    accent = accent,
                    onOpen = { viewModel.navigateTo(Screen.AllFiles) }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* 1. Floating Command Island Header                                         */
/* ------------------------------------------------------------------------- */

@Composable
private fun HomeCommandIsland(viewModel: MainViewModel) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(26.dp)

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow))
    }

    // Continuous breathing quantum node
    val infiniteTransition = rememberInfiniteTransition(label = "quantumGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = LiquidMotion.shimmer(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quantumScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = LiquidMotion.shimmer(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quantumAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VennelaTokens.spaceXs)
            .graphicsLayer {
                val t = entrance.value
                translationY = (1f - t) * 18f
                scaleX = 0.95f + 0.05f * t
                scaleY = 0.95f + 0.05f * t
                alpha = t
            }
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.40f),
                spotColor = accent.secondary.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.92f),
                        palette.cardSurface.copy(alpha = 0.78f),
                        palette.backgroundTop.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.38f),
                        accent.secondary.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                shape
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Futuristic Quantum Brand Monogram
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.60f),
                        spotColor = accent.secondary.copy(alpha = 0.60f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.40f),
                                accent.secondary.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                accent.secondary.copy(alpha = 0.80f),
                                Color.White.copy(alpha = 0.40f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Vennela Core",
                    tint = accent.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VENNELA",
                        color = palette.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(accent.secondary)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${FormatRegistry.totalCount} FORMATS • ZERO-LATENCY NATIVE",
                    color = accent.secondary.copy(alpha = 0.90f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Floating Island Action Buttons
            HeaderReflexButton(
                icon = Icons.Default.Search,
                description = "Universal Search",
                onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            HeaderReflexButton(
                icon = Icons.Default.Settings,
                description = "System Settings",
                onClick = { viewModel.navigateTo(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun HeaderReflexButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.35f),
                spotColor = accent.secondary.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.cardSurface.copy(alpha = 0.80f),
                        accent.primary.copy(alpha = 0.22f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .pressScale(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = palette.textPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* 2. Liquid Search Hologram Capsule                                         */
/* ------------------------------------------------------------------------- */

@Composable
private fun HomeSearchCapsule(onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.primary.copy(alpha = 0.35f),
                spotColor = accent.secondary.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.88f),
                        palette.cardSurface.copy(alpha = 0.72f),
                        palette.cardSurfaceStrong.copy(alpha = 0.88f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.20f),
                        accent.primary.copy(alpha = 0.40f)
                    )
                ),
                shape
            )
            .pressScale(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.secondary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Search 140+ formats, documents, archives…",
            color = palette.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Search Filter",
            tint = palette.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* 3. Quick Reflex Power Hub Chips                                           */
/* ------------------------------------------------------------------------- */

@Composable
private fun QuickActions(
    onLens: () -> Unit,
    onBrowse: () -> Unit,
    onThemes: () -> Unit,
    onFidelity: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        item {
            LiquidChip(
                text = "Universal Lens",
                icon = Icons.Default.AutoAwesome,
                isActive = true,
                modifier = Modifier.staggeredEntrance(0),
                onClick = onLens
            )
        }
        item {
            LiquidChip(
                text = "Storage Root",
                icon = Icons.Default.FolderOpen,
                modifier = Modifier.staggeredEntrance(1),
                onClick = onBrowse
            )
        }
        item {
            LiquidChip(
                text = "Theme Lab",
                icon = Icons.Default.Palette,
                modifier = Modifier.staggeredEntrance(2),
                onClick = onThemes
            )
        }
        item {
            LiquidChip(
                text = "120 FPS Engine",
                icon = Icons.Default.Speed,
                modifier = Modifier.staggeredEntrance(3),
                onClick = onFidelity
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* 4. Format Matrix Bento Hub                                                */
/* ------------------------------------------------------------------------- */

@Composable
private fun FormatMatrixBentoCard(
    categories: List<CategoryCardItem>,
    onCategoryClick: (FormatCategory) -> Unit,
    onOpenAllFormats: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.30f),
                spotColor = accent.primary.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.90f),
                        palette.cardSurface.copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.32f),
                        accent.secondary.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column {
            // Header row of Bento Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent.primary.copy(alpha = 0.25f))
                            .border(1.dp, accent.secondary.copy(alpha = 0.60f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = accent.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FORMAT MATRIX",
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Zero-JVM-Heap Streaming Pipeline",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.secondary.copy(alpha = 0.16f))
                        .border(1.dp, accent.secondary.copy(alpha = 0.50f), RoundedCornerShape(50))
                        .pressScale(pressedScale = 0.92f, onClick = onOpenAllFormats)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "142 Active",
                        color = accent.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Column High-Tech Grid of Categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEachIndexed { colIndex, item ->
                            val index = rowIndex * 2 + colIndex
                            CategoryReflexTile(
                                item = item,
                                modifier = Modifier
                                    .weight(1f)
                                    .staggeredEntrance(index),
                                onClick = { onCategoryClick(item.category) }
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    item: CategoryCardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = CategoryReflexTile(item = item, modifier = modifier, onClick = onClick)

@Composable
private fun CategoryReflexTile(
    item: CategoryCardItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = item.color.copy(alpha = 0.35f),
                spotColor = item.gradientEnd.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        item.color.copy(alpha = 0.16f),
                        palette.cardSurface.copy(alpha = 0.65f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        item.color.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape
            )
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(
                        listOf(item.color, item.gradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.countLabel,
                color = item.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* 5. Horizontal 3D Kinetic Carousel of Recent Files                         */
/* ------------------------------------------------------------------------- */

@Composable
private fun RecentFilesKineticCarousel(
    recentFiles: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onBrowseClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val display = recentFiles.take(8)

    if (display.isEmpty()) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            VennelaEmptyState(
                title = "Zero Cache Latency",
                hint = "Select any document, database, or archive to view offline.",
                icon = Icons.Default.FolderOpen,
                actionLabel = "Explore Device Storage",
                onAction = onBrowseClick
            )
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            itemsIndexed(display) { index, file ->
                RecentFile3DCard(
                    file = file,
                    index = index,
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun RecentFile3DCard(
    file: FileItem,
    index: Int,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .width(152.dp)
            .height(174.dp)
            .staggeredEntrance(index)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.25f),
                spotColor = accent.primary.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.cardSurfaceStrong.copy(alpha = 0.92f),
                        palette.cardSurface.copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        accent.secondary.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                ),
                shape
            )
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Brand badge or image preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (file.uri != null && isImage(file.extension)) {
                    AsyncImage(
                        model = file.uri,
                        contentDescription = file.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(palette.textPrimary.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
                    )
                } else {
                    FileBrandBadge(extension = file.extension, size = 46.dp)
                }

                // Extension tag pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.secondary.copy(alpha = 0.16f))
                        .border(1.dp, accent.secondary.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = file.extension.uppercase().take(4),
                        color = accent.secondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: File Title
            Text(
                text = file.name,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom: Size and date
            Text(
                text = "${file.formattedSize} • ${file.formattedDate}",
                color = palette.textSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* 6. Device Hardware Storage Console                                        */
/* ------------------------------------------------------------------------- */

@Composable
private fun HomeStorageConsole(
    volumes: List<StorageVolumeInfo>,
    granted: Boolean,
    onOpenAll: () -> Unit,
    onOpenVolume: (String) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val shape = RoundedCornerShape(26.dp)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.30f),
                spotColor = accent.primary.copy(alpha = 0.30f)
            ),
        shape = shape,
        elevation = 8.dp,
        alpha = 0.50f
    ) {
        if (volumes.isEmpty()) {
            Text(
                text = "Scanning Hardware Volumes…",
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Direct POSIX memory-mapped access initializing.",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LiquidButton(
                label = "Open File Explorer",
                icon = Icons.Default.FolderOpen,
                onClick = onOpenAll,
                fillWidth = true
            )
            return@GlassCard
        }

        volumes.forEachIndexed { index, volume ->
            Column(modifier = Modifier.staggeredEntrance(index)) {
                VolumeConsoleRow(volume = volume, onOpen = { volume.rootPath?.let(onOpenVolume) })
                if (index < volumes.lastIndex) GlassDivider()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (granted) accent.secondary else Color(0xFFFFAB00))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (granted) "All files access active • Zero sandbox limitations"
                else "Limited access — grant all files access to browse everything",
                color = if (granted) accent.secondary else Color(0xFFFFAB00),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LiquidButton(
            label = "Open Root File Manager",
            icon = Icons.Default.FolderOpen,
            onClick = onOpenAll,
            fillWidth = true
        )
    }
}

@Composable
private fun VolumeConsoleRow(volume: StorageVolumeInfo, onOpen: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val fraction by animateFloatAsState(
        targetValue = volume.usedFraction.coerceIn(0f, 1f),
        animationSpec = LiquidMotion.emphasis(LiquidMotion.durationHero),
        label = "volumeFill"
    )
    val icon = when (volume.kind) {
        VolumeKind.INTERNAL -> Icons.Default.Memory
        VolumeKind.SD_CARD -> Icons.Default.SdCard
        VolumeKind.USB_OTG -> Icons.Default.Usb
        VolumeKind.UNKNOWN -> Icons.Default.Folder
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (volume.isBrowsable) Modifier.pressScale(0.98f, onOpen) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = icon,
            colors = listOf(accent.primary, accent.secondary),
            size = 38.dp,
            radius = 12.dp,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = volume.label,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Illuminated Multi-Segment Liquid Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.cardBorder.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            clip = false,
                            ambientColor = accent.secondary.copy(alpha = 0.65f),
                            spotColor = accent.primary.copy(alpha = 0.65f)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.primary,
                                    accent.secondary,
                                    Color.White.copy(alpha = 0.60f)
                                )
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (volume.totalBytes > 0L) {
                    "${homeBytes(volume.freeBytes)} free of ${homeBytes(volume.totalBytes)}"
                } else {
                    if (volume.isBrowsable) "Mounted & Ready" else "Not mounted"
                },
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun homeBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    if (gb < 1024) return String.format("%.2f GB", gb)
    return String.format("%.2f TB", gb / 1024.0)
}

/* ------------------------------------------------------------------------- */
/* 7. System Engine Status Footer                                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun EngineStatusFooter(
    palette: com.vennela365.viewer.theme.VennelaPalette,
    accent: com.vennela365.viewer.theme.VennelaAccent,
    onOpen: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val pulse by rememberInfiniteTransition(label = "footerPulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = LiquidMotion.shimmer(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "footerPulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.35f),
                spotColor = accent.primary.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.25f),
                        accent.secondary.copy(alpha = 0.12f),
                        palette.cardSurface.copy(alpha = 0.60f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .pressScale(pressedScale = 0.98f, onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = pulse
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Air-Gapped Privacy • 0ms Vello Strips",
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.secondary.copy(alpha = 0.20f))
                .border(1.dp, accent.secondary.copy(alpha = 0.60f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "120 FPS",
                color = accent.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
