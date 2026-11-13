package com.vennela365.viewer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.PublicationDocument
import com.vennela365.viewer.parser.PublicationParser
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.*
import com.vennela365.viewer.ui.components.*
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.launch

private enum class BookPaperTheme(
    val label: String,
    val surfaceColor: Color,
    val inkColor: Color,
    val subtleColor: Color
) {
    LUNAR("Lunar", Color(0xFF0E1422), Color(0xFFF1F5F9), Color(0xFF94A3B8)),
    PAPER("Paper", Color(0xFFFAF8F4), Color(0xFF1E293B), Color(0xFF64748B)),
    SEPIA("Sepia", Color(0xFFF3E9D2), Color(0xFF3E2F20), Color(0xFF7C6650)),
    AMOLED("AMOLED", Color(0xFF000000), Color(0xFFE2E8F0), Color(0xFF71717A))
}

@Composable
fun PublicationViewerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val scope = rememberCoroutineScope()

    var doc by remember { mutableStateOf<PublicationDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var activeChapterIdx by remember { mutableIntStateOf(0) }
    var activeComicPage by remember { mutableIntStateOf(0) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var showTypographySheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var paperTheme by remember { mutableStateOf(if (palette.isDark) BookPaperTheme.LUNAR else BookPaperTheme.PAPER) }
    var useSerif by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableIntStateOf(16) }
    var lineHeightMult by remember { mutableFloatStateOf(1.58f) }

    LaunchedEffect(file?.id, file?.uri) {
        val current = file
        if (current == null) {
            doc = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        activeChapterIdx = 0
        activeComicPage = 0
        doc = PublicationParser.parsePublication(context, current.uri, current.extension, current.name)
        isLoading = false
    }

    BackHandler(enabled = showChapterSheet || showTypographySheet || showSearchBar) {
        showChapterSheet = false
        showTypographySheet = false
        showSearchBar = false
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusInset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val parsed = doc
                val progress = when {
                    parsed == null -> null
                    parsed.comicPages.isNotEmpty() -> (activeComicPage + 1f) / parsed.comicPages.size.coerceAtLeast(1)
                    parsed.chapters.isNotEmpty() -> (activeChapterIdx + 1f) / parsed.chapters.size.coerceAtLeast(1)
                    else -> null
                }
                val subtitle = when {
                    parsed == null -> file?.formatInfo?.name
                    parsed.comicPages.isNotEmpty() -> "CBZ • Page ${activeComicPage + 1} of ${parsed.comicPages.size}"
                    parsed.chapters.isNotEmpty() -> "${parsed.formatLabel} • Ch ${activeChapterIdx + 1}/${parsed.chapters.size}"
                    else -> parsed.formatLabel
                }

                ViewerTopBar(
                    title = parsed?.title ?: file?.name ?: "Publication",
                    subtitle = subtitle,
                    onBack = { viewModel.navigateBack() },
                    leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
                    progress = progress,
                    actions = {
                        if (parsed != null && parsed.chapters.isNotEmpty()) {
                            ViewerIconAction(
                                icon = Icons.Default.Search,
                                contentDescription = "Search in book",
                                active = showSearchBar,
                                modifier = Modifier.testTag("pub_search_button")
                            ) {
                                showSearchBar = !showSearchBar
                            }
                            ViewerIconAction(
                                icon = Icons.Default.FormatSize,
                                contentDescription = "Reading typography",
                                active = showTypographySheet,
                                modifier = Modifier.testTag("pub_typography_button")
                            ) {
                                showTypographySheet = !showTypographySheet
                                showChapterSheet = false
                            }
                        }
                        ViewerIconAction(
                            icon = Icons.Default.Info,
                            contentDescription = "File metadata",
                            modifier = Modifier.testTag("pub_info_button")
                        ) {
                            viewModel.navigateTo(Screen.FileMetadata)
                        }
                    }
                )

                if (showSearchBar && parsed != null && parsed.chapters.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 860.dp)
                            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceXs)
                    ) {
                        VennelaSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search chapters or text…",
                            modifier = Modifier.testTag("pub_search_input")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 880.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when {
                        file == null -> Box(
                            modifier = Modifier.fillMaxSize().padding(VennelaTokens.gutter),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard {
                                VennelaEmptyState(
                                    title = "No publication open",
                                    hint = "Open an EPUB, FB2, or CBZ comic archive to read offline with full chapter & page controls.",
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    actionLabel = "Browse files",
                                    onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                                )
                            }
                        }

                        isLoading -> Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = accent.primary)
                            Spacer(Modifier.height(VennelaTokens.spaceMd))
                            Text(
                                "Indexing publication chapters…",
                                color = palette.textSecondary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.bodySmall
                            )
                        }

                        parsed == null || parsed.isDrmOrUnsupportedBinary || (parsed.chapters.isEmpty() && parsed.comicPages.isEmpty()) -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(
                                        start = VennelaTokens.gutter,
                                        end = VennelaTokens.gutter,
                                        top = VennelaTokens.spaceMd,
                                        bottom = navInset + 84.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
                            ) {
                                GlassCard {
                                    Text(
                                        text = parsed?.title ?: file?.name ?: "Publication",
                                        color = palette.textPrimary,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                                    Text(
                                        text = parsed?.notice ?: "This publication has no extractable text or image streams.",
                                        color = palette.textSecondary,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.bodySmall,
                                        lineHeight = 20.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                                    LiquidButton(
                                        label = "Hex inspector",
                                        icon = Icons.Default.Memory,
                                        modifier = Modifier.weight(1f).testTag("pub_hex_button"),
                                        onClick = { viewModel.navigateTo(Screen.HexViewer) }
                                    )
                                    file?.uri?.let { u ->
                                        LiquidButton(
                                            label = "External app",
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            modifier = Modifier.weight(1f).testTag("pub_external_button"),
                                            onClick = { ShareUri.viewIntent(context, u, file?.formatInfo?.mimeType ?: "*/*") }
                                        )
                                    }
                                }
                            }
                        }

                        parsed.comicPages.isNotEmpty() -> {
                            CbzComicReaderBody(
                                uri = file?.uri,
                                pages = parsed.comicPages,
                                activePage = activeComicPage,
                                onSelectPage = { activeComicPage = it },
                                bottomClearance = navInset + 76.dp
                            )
                        }

                        else -> {
                            EpubChapterReaderBody(
                                doc = parsed,
                                activeChapterIdx = activeChapterIdx,
                                paperTheme = paperTheme,
                                useSerif = useSerif,
                                fontSizeSp = fontSizeSp,
                                lineHeightMult = lineHeightMult,
                                searchQuery = searchQuery,
                                bottomClearance = navInset + 78.dp
                            )
                        }
                    }
                }
            }

            // Bottom floating reader dock + expandable sheets
            val parsed = doc
            if (parsed != null && (parsed.chapters.isNotEmpty() || parsed.comicPages.isNotEmpty())) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 600.dp)
                        .padding(bottom = navInset),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ViewerSheet(
                        visible = showTypographySheet && parsed.chapters.isNotEmpty(),
                        icon = Icons.Default.FormatSize,
                        title = "Reading appearance",
                        trailing = "${fontSizeSp}sp • ${if (useSerif) "Serif" else "Sans"}"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            BookPaperTheme.entries.forEach { t ->
                                val selected = paperTheme == t
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp)
                                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                        .background(t.surfaceColor)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) accent.secondary else palette.cardBorder,
                                            shape = RoundedCornerShape(VennelaTokens.radiusChip)
                                        )
                                        .testTag("pub_theme_${t.name.lowercase()}")
                                        .clickable { paperTheme = t },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = t.label,
                                        color = t.inkColor,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.caption,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LiquidChip(
                                text = "Serif",
                                isActive = useSerif,
                                modifier = Modifier.weight(1f).testTag("pub_font_serif"),
                                onClick = { useSerif = true }
                            )
                            LiquidChip(
                                text = "Sans",
                                isActive = !useSerif,
                                modifier = Modifier.weight(1f).testTag("pub_font_sans"),
                                onClick = { useSerif = false }
                            )
                            ReaderIconButton(
                                icon = Icons.Default.Remove,
                                contentDescription = "Decrease font size",
                                modifier = Modifier.testTag("pub_font_dec"),
                                onClick = { fontSizeSp = (fontSizeSp - 1).coerceAtLeast(12) }
                            )
                            Text(
                                "${fontSizeSp}",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Mono,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                            ReaderIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = "Increase font size",
                                modifier = Modifier.testTag("pub_font_inc"),
                                onClick = { fontSizeSp = (fontSizeSp + 1).coerceAtMost(28) }
                            )
                        }
                    }

                    ViewerSheet(
                        visible = showChapterSheet && parsed.chapters.isNotEmpty(),
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        title = "Chapters (${parsed.chapters.size})",
                        trailing = "${parsed.totalWords} words"
                    ) {
                        val filteredChapters = remember(parsed.chapters, searchQuery) {
                            if (searchQuery.isBlank()) parsed.chapters
                            else parsed.chapters.filter {
                                it.title.contains(searchQuery, true) || it.body.contains(searchQuery, true)
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(filteredChapters, key = { _, ch -> ch.index }) { _, ch ->
                                val isCurrent = ch.index == activeChapterIdx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                        .background(if (isCurrent) accent.primary.copy(alpha = 0.22f) else Color.Transparent)
                                        .testTag("pub_chapter_${ch.index}")
                                        .clickable {
                                            activeChapterIdx = ch.index
                                            showChapterSheet = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ch.index + 1}.",
                                        color = accent.secondary,
                                        fontFamily = VennelaFonts.Mono,
                                        fontSize = VennelaTokens.caption,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(34.dp)
                                    )
                                    Text(
                                        text = ch.title,
                                        color = palette.textPrimary,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.bodySmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${ch.wordCount}w",
                                        color = palette.textTertiary,
                                        fontFamily = VennelaFonts.Mono,
                                        fontSize = VennelaTokens.nano
                                    )
                                }
                            }
                        }
                    }

                    ViewerDock {
                        if (parsed.comicPages.isNotEmpty()) {
                            ViewerDockAction(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous page",
                                enabled = activeComicPage > 0,
                                modifier = Modifier.testTag("cbz_prev_page")
                            ) {
                                activeComicPage = (activeComicPage - 1).coerceAtLeast(0)
                            }
                            Text(
                                text = "${activeComicPage + 1} / ${parsed.comicPages.size}",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Mono,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            ViewerDockAction(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next page",
                                enabled = activeComicPage < parsed.comicPages.lastIndex,
                                modifier = Modifier.testTag("cbz_next_page")
                            ) {
                                activeComicPage = (activeComicPage + 1).coerceAtMost(parsed.comicPages.lastIndex)
                            }
                        } else {
                            ViewerDockAction(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous chapter",
                                enabled = activeChapterIdx > 0,
                                modifier = Modifier.testTag("pub_prev_chapter")
                            ) {
                                activeChapterIdx = (activeChapterIdx - 1).coerceAtLeast(0)
                            }
                            ViewerDockAction(
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Chapters",
                                active = showChapterSheet,
                                modifier = Modifier.testTag("pub_chapters_toggle")
                            ) {
                                showChapterSheet = !showChapterSheet
                                showTypographySheet = false
                            }
                            ViewerDockAction(
                                icon = Icons.Default.ContentCopy,
                                contentDescription = "Copy chapter text",
                                modifier = Modifier.testTag("pub_copy_chapter")
                            ) {
                                val ch = parsed.chapters.getOrNull(activeChapterIdx)
                                if (ch != null) {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText(ch.title, "${ch.title}\n\n${ch.body}"))
                                    Toast.makeText(context, "Chapter copied", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ViewerDockAction(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next chapter",
                                enabled = activeChapterIdx < parsed.chapters.lastIndex,
                                modifier = Modifier.testTag("pub_next_chapter")
                            ) {
                                activeChapterIdx = (activeChapterIdx + 1).coerceAtMost(parsed.chapters.lastIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubChapterReaderBody(
    doc: PublicationDocument,
    activeChapterIdx: Int,
    paperTheme: BookPaperTheme,
    useSerif: Boolean,
    fontSizeSp: Int,
    lineHeightMult: Float,
    searchQuery: String,
    bottomClearance: androidx.compose.ui.unit.Dp
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val chapter = doc.chapters.getOrNull(activeChapterIdx) ?: return
    val scroll = rememberScrollState()

    LaunchedEffect(activeChapterIdx) {
        scroll.scrollTo(0)
    }

    val coverBitmap = remember(doc.coverBytes) {
        doc.coverBytes?.let { bytes -> runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = bottomClearance
            ),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
    ) {
        if (activeChapterIdx == 0) {
            GlassCard(alpha = 0.48f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap.asImageBitmap(),
                            contentDescription = "Book cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 64.dp, height = 92.dp)
                                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusChip))
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            color = palette.textPrimary,
                            fontFamily = VennelaFonts.Serif,
                            fontSize = VennelaTokens.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (doc.author.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = doc.author,
                                color = accent.secondary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        val estMinutes = (doc.totalWords / 220).coerceAtLeast(1)
                        val metaLine = listOfNotNull(
                            "${doc.chapters.size} chapters",
                            "${doc.totalWords} words",
                            "~$estMinutes min read",
                            doc.language.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        Text(
                            text = metaLine,
                            color = palette.textTertiary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = VennelaTokens.micro
                        )
                    }
                }
            }
        }

        // Paper-themed reading page card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .background(paperTheme.surfaceColor)
                .border(1.dp, palette.cardBorder.copy(alpha = 0.45f), RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = "CHAPTER ${chapter.index + 1}",
                color = accent.primary,
                fontFamily = VennelaFonts.Mono,
                fontSize = VennelaTokens.nano,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = chapter.title,
                color = paperTheme.inkColor,
                fontFamily = VennelaFonts.Serif,
                fontSize = (fontSizeSp + 6).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (fontSizeSp * 1.4f + 6).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${chapter.wordCount} words • ~${(chapter.wordCount / 220).coerceAtLeast(1)} min",
                color = paperTheme.subtleColor,
                fontFamily = VennelaFonts.Mono,
                fontSize = VennelaTokens.nano
            )
            if (searchQuery.isNotBlank()) {
                val hits = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE).findAll(chapter.body).count()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$hits match${if (hits == 1) "" else "es"} for \"$searchQuery\" in this chapter",
                    color = accent.secondary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(VennelaTokens.spaceMd))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(paperTheme.subtleColor.copy(alpha = 0.25f))
            )
            Spacer(Modifier.height(VennelaTokens.spaceLg))

            val paragraphs = remember(chapter.body) {
                chapter.body.split("\n\n").filter { it.isNotBlank() }
            }
            paragraphs.forEach { para ->
                val isSubHeading = para.startsWith("## ")
                val clean = para.removePrefix("## ").trim()
                val matchesSearch = searchQuery.isNotBlank() && clean.contains(searchQuery, true)
                Text(
                    text = clean,
                    color = if (matchesSearch) accent.secondary else paperTheme.inkColor,
                    fontFamily = if (isSubHeading) VennelaFonts.Serif else if (useSerif) VennelaFonts.Serif else VennelaFonts.Sans,
                    fontSize = if (isSubHeading) (fontSizeSp + 2).sp else fontSizeSp.sp,
                    fontWeight = if (isSubHeading) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = ((if (isSubHeading) fontSizeSp + 2 else fontSizeSp) * lineHeightMult).sp,
                    modifier = Modifier.padding(bottom = if (isSubHeading) 10.dp else 14.dp)
                )
            }
        }
    }
}

@Composable
private fun CbzComicReaderBody(
    uri: android.net.Uri?,
    pages: List<com.vennela365.viewer.parser.CbzComicPage>,
    activePage: Int,
    onSelectPage: (Int) -> Unit,
    bottomClearance: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val page = pages.getOrNull(activePage)
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadingPage by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(uri, page?.entryPath) {
        if (uri == null || page == null) {
            bitmap = null
            loadingPage = false
            return@LaunchedEffect
        }
        loadingPage = true
        scale = 1f
        bitmap = PublicationParser.loadCbzPageBitmap(context, uri, page.entryPath)
        loadingPage = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = VennelaTokens.spaceSm,
                bottom = bottomClearance
            ),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
    ) {
        GlassPanel(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .pointerInput(activePage) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 3.5f)
                    }
                },
            alpha = 0.35f
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    loadingPage -> CircularProgressIndicator(color = accent.primary)
                    bitmap == null -> Text(
                        "Unable to decode comic frame.",
                        color = palette.textSecondary,
                        fontFamily = VennelaFonts.Sans,
                        fontSize = VennelaTokens.bodySmall
                    )
                    else -> Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = page?.fileName ?: "Comic page ${activePage + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        }

        // Compact page scrubber strip
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(pages, key = { _, p -> p.index }) { idx, p ->
                val selected = idx == activePage
                Box(
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(
                            if (selected) Brush.horizontalGradient(listOf(accent.primary, accent.secondary))
                            else Brush.horizontalGradient(listOf(palette.cardSurfaceStrong, palette.cardSurfaceStrong))
                        )
                        .border(
                            1.dp,
                            if (selected) Color.White.copy(alpha = 0.35f) else palette.cardBorder,
                            RoundedCornerShape(VennelaTokens.radiusChip)
                        )
                        .testTag("cbz_thumb_$idx")
                        .clickable { onSelectPage(idx) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P${idx + 1}",
                        color = if (selected) Color.White else palette.textSecondary,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.caption,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
