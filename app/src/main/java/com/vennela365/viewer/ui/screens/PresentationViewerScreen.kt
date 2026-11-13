package com.vennela365.viewer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.parser.Deck
import com.vennela365.viewer.parser.PresentationParser
import com.vennela365.viewer.parser.Slide
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.AnimatedDisclosure
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.PresentationStage
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.SuiteCommandHeader
import com.vennela365.viewer.ui.components.SuiteFooterStatusBar
import com.vennela365.viewer.ui.components.ViewerDock
import com.vennela365.viewer.ui.components.ViewerDockAction
import com.vennela365.viewer.ui.components.ViewerKind
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.ViewerTopBar
import com.vennela365.viewer.ui.components.WpsPaperTone
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.rememberSuiteSessionState
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

private enum class DeckPane { NONE, OUTLINE, INFO }

/**
 * Real slide reader for pptx / pptm / ppsx / potx / odp / otp / fodp.
 *
 * It renders each slide's genuine title and body text in a 16:9 canvas, with a
 * real thumbnail rail, an outline, speaker notes when the file has them, and
 * in-deck search. Formats that are binary containers rather than decomposed XML
 * are reported honestly instead of being faked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationViewerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val viewerState by viewModel.viewerState.collectAsStateWithLifecycle()
    val activeSuite by viewModel.preferredSuiteStyle.collectAsStateWithLifecycle()
    val suiteSession = rememberSuiteSessionState(activeSuite, palette.isDark)
    val file = selectedFile

    val deckState = produceState<DeckLoad>(initialValue = DeckLoad.Loading, file?.id) {
        value = if (file == null) {
            DeckLoad.Failed("No presentation is open")
        } else {
            val parsed = PresentationParser.parse(context, file.uri, file.extension)
            if (parsed == null || parsed.slides.isEmpty()) {
                DeckLoad.Unsupported(file?.extension.orEmpty())
            } else {
                DeckLoad.Ready(parsed)
            }
        }
    }

    var index by remember(file?.id) { mutableIntStateOf(0) }
    var pane by remember { mutableStateOf(DeckPane.NONE) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var textScale by remember { mutableStateOf(1f) }
    var presenting by remember { mutableStateOf(false) }
    var showGridSheet by remember { mutableStateOf(false) }

    val deck = (deckState.value as? DeckLoad.Ready)?.deck
    val slideCount = deck?.slides?.size ?: 0
    val safeIndex = if (slideCount == 0) 0 else index.coerceIn(0, slideCount - 1)
    val currentSlide = deck?.slides?.getOrNull(safeIndex)

    val matches = remember(deck, findQuery) {
        if (deck == null || findQuery.isBlank()) emptyList()
        else deck.slides.filter { slide ->
            slide.title.contains(findQuery, ignoreCase = true) ||
                slide.body.any { it.contains(findQuery, ignoreCase = true) } ||
                slide.notes?.contains(findQuery, ignoreCase = true) == true
        }
    }

    KeepScreenOn(keepScreenOn)

    LiquidPage(
        title = null,
        dock = {
            ViewerDock {
                ViewerDockAction(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous slide",
                    enabled = safeIndex > 0,
                    modifier = Modifier.testTag("deck_prev_button")
                ) { index = (safeIndex - 1).coerceAtLeast(0) }
                Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (slideCount == 0) "0 / 0" else "${safeIndex + 1} / $slideCount",
                        color = palette.textSecondary,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                ViewerDockAction(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next slide",
                    enabled = slideCount > 0 && safeIndex < slideCount - 1,
                    modifier = Modifier.testTag("deck_next_button")
                ) { index = (safeIndex + 1).coerceAtMost(slideCount - 1) }
                ViewerDockAction(
                    Icons.Default.Slideshow,
                    contentDescription = "Present slide show",
                    active = presenting,
                    enabled = slideCount > 0,
                    modifier = Modifier.testTag("deck_present_button")
                ) { presenting = true }
                ViewerDockAction(
                    Icons.Default.GridView,
                    contentDescription = "Slide sorter grid",
                    active = showGridSheet,
                    enabled = slideCount > 0,
                    modifier = Modifier.testTag("deck_grid_button")
                ) { showGridSheet = true }
                ViewerDockAction(
                    Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Slide outline",
                    active = pane == DeckPane.OUTLINE,
                    enabled = slideCount > 0,
                    modifier = Modifier.testTag("deck_outline_button")
                ) { pane = if (pane == DeckPane.OUTLINE) DeckPane.NONE else DeckPane.OUTLINE }
            }
        }
    ) { pagePadding ->
        Box(modifier = Modifier.fillMaxSize().padding(pagePadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                SuiteCommandHeader(
                    kind = ViewerKind.SLIDES,
                    fileName = file?.name ?: "Presentation",
                    subtitle = when (val state = deckState.value) {
                        is DeckLoad.Loading -> "Reading slides…"
                        is DeckLoad.Ready -> "${state.deck.slides.size} slides • ${activeSuite.badge}"
                        is DeckLoad.Unsupported -> "Text outline unavailable"
                        is DeckLoad.Failed -> "Could not open"
                    },
                    activeSuite = activeSuite,
                    onSelectSuite = { viewModel.setPreferredSuiteStyle(it) },
                    session = suiteSession,
                    progress = if (slideCount > 0) (safeIndex + 1).toFloat() / slideCount else 0f,
                    zoomPercent = (textScale * 100).toInt(),
                    onBack = { viewModel.navigateBack() },
                    onSearchToggle = { findQuery = if (findQuery.isBlank()) " " else "" },
                    onOutlineToggle = { pane = if (pane == DeckPane.OUTLINE) DeckPane.NONE else DeckPane.OUTLINE },
                    onShare = { viewModel.navigateTo(Screen.ShareScreen) },
                    onExportPdf = { presenting = true },
                    onPrint = { showGridSheet = true },
                    onZoomChange = { delta -> textScale = (textScale + delta).coerceIn(0.75f, 1.6f) },
                    onTextScaleChange = { delta -> textScale = (textScale + delta).coerceIn(0.75f, 1.6f) }
                )

                val stageBackdrop = remember(activeSuite, suiteSession.paperTone, palette.isDark) {
                    when (activeSuite) {
                        ViewerSuiteStyle.GOOGLE_WORKSPACE -> if (palette.isDark) Color(0xFF1F2125) else Color(0xFFF1F3F4)
                        ViewerSuiteStyle.MICROSOFT_365 -> if (palette.isDark) Color(0xFF1A1B1E) else Color(0xFFEDEBE9)
                        ViewerSuiteStyle.WPS_SUPER -> if (suiteSession.paperTone == WpsPaperTone.EYE_CARE_GREEN) Color(0xFFDDF3E0) else Color(0xFFF2F4F7)
                        ViewerSuiteStyle.LIBRE_OFFICE -> if (palette.isDark) Color(0xFF202225) else Color(0xFFDADCE0)
                        ViewerSuiteStyle.OKEN_SCANNER -> Color(0xFF1E2229)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(stageBackdrop),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = VennelaTokens.gutter,
                            end = VennelaTokens.gutter,
                            top = VennelaTokens.spaceMd,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
                    ) {
                        when (val state = deckState.value) {
                        is DeckLoad.Loading -> item(key = "loading") { SlideSkeleton() }

                        is DeckLoad.Failed -> item(key = "failed") {
                            MessageCard(
                                title = "Could not open this presentation",
                                body = state.message,
                                actionLabel = "Try again",
                                onAction = { file?.let { viewModel.openFile(it) } }
                            )
                        }

                        is DeckLoad.Unsupported -> item(key = "unsupported") {
                            MessageCard(
                                title = "Layout preview unavailable",
                                body = buildString {
                                    append("This .")
                                    append(state.extension.ifBlank { "unknown" })
                                    append(" file is a binary container, so its slides cannot be decomposed here. ")
                                    append("Vennela does not fake slide content. ")
                                    append("Legacy .ppt and Apple Keynote decks need PowerPoint or Keynote to render their layout.")
                                },
                                actionLabel = "Inspect as archive",
                                onAction = { viewModel.navigateTo(Screen.ArchiveBrowser) }
                            )
                        }

                        is DeckLoad.Ready -> {
                            val current = state.deck.slides.getOrNull(safeIndex)
                            if (current != null) {
                                item(key = "slide") {
                                    var swipeDelta by remember { mutableFloatStateOf(0f) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(safeIndex, state.deck.slides.size) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { swipeDelta = 0f },
                                                    onHorizontalDrag = { change, amount ->
                                                        change.consume()
                                                        swipeDelta += amount
                                                    },
                                                    onDragEnd = {
                                                        val threshold = 64.dp.toPx()
                                                        if (swipeDelta < -threshold && safeIndex < state.deck.slides.lastIndex) {
                                                            index = safeIndex + 1
                                                        } else if (swipeDelta > threshold && safeIndex > 0) {
                                                            index = safeIndex - 1
                                                        }
                                                        swipeDelta = 0f
                                                    }
                                                )
                                            }
                                    ) {
                                        AnimatedContent(
                                            targetState = current,
                                            transitionSpec = {
                                                val forward = targetState.number >= initialState.number
                                                (slideInHorizontally(LiquidMotion.liquid()) { if (forward) it / 4 else -it / 4 } +
                                                    fadeIn(LiquidMotion.glide()))
                                                    .togetherWith(
                                                        slideOutHorizontally(LiquidMotion.glide()) { if (forward) -it / 5 else it / 5 } +
                                                            fadeOut(LiquidMotion.exit())
                                                    )
                                            },
                                            label = "deckSlideCanvasTransition"
                                        ) { animatedSlide ->
                                            SlideCanvas(
                                                slide = animatedSlide,
                                                total = state.deck.slides.size,
                                                textScale = textScale,
                                                highlight = findQuery.takeIf { it.isNotBlank() },
                                                suite = activeSuite,
                                                paperTone = suiteSession.paperTone
                                            )
                                        }
                                    }
                                }

                            item(key = "rail") {
                                SlideRail(
                                    slides = state.deck.slides,
                                    currentIndex = safeIndex,
                                    onSelect = { index = it }
                                )
                            }

                            item(key = "notes") {
                                SlideNotes(slide = current, textScale = textScale)
                            }

                            item(key = "find") {
                                DeckSearch(
                                    query = findQuery,
                                    matches = matches,
                                    onQueryChange = { findQuery = it },
                                    onJump = { slide ->
                                        index = (slide.number - 1).coerceIn(0, state.deck.slides.size - 1)
                                    }
                                )
                            }

                            if (state.deck.truncated) {
                                item(key = "truncated") {
                                    Text(
                                        text = "Showing the first ${state.deck.slides.size} slides. " +
                                            "The deck is larger than the preview limit.",
                                        color = palette.textTertiary,
                                        fontSize = VennelaTokens.micro
                                    )
                                }
                            }
                        }
                    }
                }

                if (pane == DeckPane.OUTLINE && deck != null) {
                    item(key = "outline") {
                        OutlinePane(
                            slides = deck.slides,
                            currentIndex = safeIndex,
                            onSelect = { index = it; pane = DeckPane.NONE }
                        )
                    }
                }

                if (pane == DeckPane.INFO) {
                    item(key = "info") {
                        DeckInfo(file = file, deck = deck, viewerState = viewerState)
                    }
                }

                item(key = "actions") {
                    DeckActions(
                        file = file,
                        textScale = textScale,
                        onTextScale = { textScale = it },
                        keepScreenOn = keepScreenOn,
                        onKeepScreenOn = { keepScreenOn = it }
                    )
                }
            }
            }

            SuiteFooterStatusBar(
                suite = activeSuite,
                pageOrLineLabel = if (slideCount == 0) "Slide 0 of 0" else "Slide ${safeIndex + 1} of $slideCount",
                wordCount = currentSlide?.body?.sumOf { it.split(Regex("\\s+")).count { w -> w.isNotBlank() } } ?: 0,
                charCount = currentSlide?.body?.sumOf { it.length } ?: 0,
                zoomPercent = (textScale * 100).toInt(),
                extraBadge = when (activeSuite) {
                    ViewerSuiteStyle.GOOGLE_WORKSPACE -> "Google Slides 16:9"
                    ViewerSuiteStyle.MICROSOFT_365 -> "PowerPoint Normal View"
                    ViewerSuiteStyle.WPS_SUPER -> "WPS Presentation"
                    ViewerSuiteStyle.LIBRE_OFFICE -> "Impress 16:9 Stage"
                    ViewerSuiteStyle.OKEN_SCANNER -> "Oken HD Board"
                }
            )
            }

            if (presenting && currentSlide != null) {
                PresentationStage(
                    title = currentSlide.title,
                    body = currentSlide.body,
                    number = safeIndex + 1,
                    total = slideCount,
                    onPrevious = { index = (safeIndex - 1).coerceAtLeast(0) },
                    onNext = {
                        index = (safeIndex + 1).coerceAtMost((slideCount - 1).coerceAtLeast(0))
                    },
                    onExit = { presenting = false }
                )
            }
        }
    }

    if (showGridSheet && deck != null && deck.slides.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showGridSheet = false },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceXl)
                    .padding(bottom = VennelaTokens.spaceXxl)
            ) {
                Text(
                    text = "Slide sorter • ${deck.slides.size} slides",
                    color = palette.textPrimary,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = VennelaTokens.spaceHuge * 12),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                    verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    itemsIndexed(deck.slides, key = { _, slide -> "grid_slide_${slide.number}" }) { pos, slide ->
                        val active = pos == safeIndex
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) accent.secondary else palette.cardBorder,
                                    shape = RoundedCornerShape(VennelaTokens.radiusCard)
                                )
                                .pressScale(0.97f) {
                                    index = pos
                                    showGridSheet = false
                                },
                            shape = RoundedCornerShape(VennelaTokens.radiusCard),
                            alpha = if (active) 0.56f else 0.34f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(VennelaTokens.spaceMd)
                            ) {
                                Text(
                                    text = "Slide ${slide.number}",
                                    color = if (active) accent.secondary else palette.textTertiary,
                                    fontFamily = VennelaFonts.Mono,
                                    fontSize = VennelaTokens.nano,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                Text(
                                    text = slide.title,
                                    color = palette.textPrimary,
                                    fontFamily = VennelaFonts.Serif,
                                    fontSize = VennelaTokens.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (slide.body.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                    Text(
                                        text = slide.body.first(),
                                        color = palette.textSecondary,
                                        fontSize = VennelaTokens.micro,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Slide canvas                                                               */
/* ------------------------------------------------------------------------- */

@Composable
private fun SlideCanvas(
    slide: Slide,
    total: Int,
    textScale: Float,
    highlight: String?,
    suite: ViewerSuiteStyle = ViewerSuiteStyle.GOOGLE_WORKSPACE,
    paperTone: WpsPaperTone = WpsPaperTone.CRISP_WHITE
) {
    val brandAccent = ViewerKind.SLIDES.accentFor(suite)
    val effectivePaper = when (suite) {
        ViewerSuiteStyle.WPS_SUPER -> paperTone
        else -> WpsPaperTone.CRISP_WHITE
    }
    val slidePaperColor = effectivePaper.paperColor
    val slideInk = effectivePaper.inkColor
    val slideMuted = effectivePaper.mutedInkColor
    val cornerRadius = when (suite) {
        ViewerSuiteStyle.GOOGLE_WORKSPACE -> 8.dp
        ViewerSuiteStyle.MICROSOFT_365 -> 4.dp
        ViewerSuiteStyle.WPS_SUPER -> 12.dp
        ViewerSuiteStyle.LIBRE_OFFICE -> 2.dp
        ViewerSuiteStyle.OKEN_SCANNER -> 6.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(slidePaperColor)
            .border(1.dp, brandAccent.copy(alpha = 0.35f), RoundedCornerShape(cornerRadius))
    ) {
        // Authentic top/left slide master accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(brandAccent, brandAccent.copy(alpha = 0.65f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(brandAccent.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${ViewerKind.SLIDES.appNameFor(suite)} • Slide ${slide.number} / $total",
                        color = brandAccent,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.nano,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Text(
                text = slide.title,
                color = slideInk,
                fontFamily = VennelaFonts.OnePlusDisplay,
                fontSize = VennelaTokens.titleDisplay * textScale,
                fontWeight = FontWeight.Bold,
                lineHeight = VennelaTokens.titleDisplay * textScale * 1.18f,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            if (slide.title.isNotBlank() && slide.body.isNotEmpty()) {
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(brandAccent)
                )
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
            slide.body.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (highlight != null && line.contains(highlight, ignoreCase = true)) {
                                    brandAccent
                                } else {
                                    brandAccent.copy(alpha = 0.72f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                    Text(
                        text = line,
                        color = slideInk.copy(alpha = 0.88f),
                        fontFamily = VennelaFonts.Sans,
                        fontSize = VennelaTokens.body * textScale,
                        lineHeight = VennelaTokens.body * textScale * 1.42f
                    )
                }
            }
            if (slide.body.isEmpty()) {
                Text(
                    text = "This slide has a title but no body text in the file.",
                    color = slideMuted,
                    fontFamily = VennelaFonts.Sans,
                    fontSize = VennelaTokens.bodySmall
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Transport                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun SlideTransport(
    index: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJump: (Int) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var target by remember { mutableStateOf("") }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                colors = if (index > 0) listOf(accent.primary, accent.secondary)
                else listOf(palette.textTertiary, palette.textTertiary),
                contentDescription = "Previous slide",
                onClick = onPrevious
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${index + 1}",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "of $total",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro
                )
            }
            IconBadge(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                colors = if (index < total - 1) listOf(accent.secondary, accent.primary)
                else listOf(palette.textTertiary, palette.textTertiary),
                contentDescription = "Next slide",
                onClick = onNext
            )
        }

        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(palette.cardBorder.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (total <= 1) 1f else index.toFloat() / (total - 1).toFloat())
                    .height(4.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(accent.primary)
            )
        }

        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = target,
                onValueChange = { raw -> target = raw.filter { it.isDigit() }.take(4) },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Jump to slide number",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.bodySmall
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = accent.primary
                )
            )
            IconBadge(
                icon = Icons.Default.Slideshow,
                colors = listOf(accent.primary, accent.secondary),
                contentDescription = "Go to slide",
                onClick = {
                    target.toIntOrNull()?.let(onJump)
                    target = ""
                }
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Thumbnail rail                                                             */
/* ------------------------------------------------------------------------- */

@Composable
private fun SlideRail(slides: List<Slide>, currentIndex: Int, onSelect: (Int) -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val listState = rememberLazyListState()

    // Keep the active thumbnail on screen as the reader advances.
    LaunchedEffect(currentIndex) {
        if (currentIndex in slides.indices) {
            runCatching { listState.animateScrollToItem(currentIndex) }
        }
    }

    Column {
        SectionHeader(title = "Slides")
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
        ) {
            items(slides, key = { it.number }) { slide ->
                val active = slide.number - 1 == currentIndex
                GlassPanel(
                    modifier = Modifier
                        .width(148.dp)
                        .height(96.dp)
                        .then(Modifier.pressScale(0.96f) { onSelect(slide.number - 1) }),
                    shape = RoundedCornerShape(VennelaTokens.radiusTile),
                    alpha = if (active) 0.58f else 0.36f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(VennelaTokens.spaceSm)
                    ) {
                        Text(
                            text = "${slide.number}",
                            color = if (active) accent.secondary else palette.textTertiary,
                            fontSize = VennelaTokens.nano,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                        Text(
                            text = slide.title,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.micro,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (slide.body.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                            Text(
                                text = slide.body.first(),
                                color = palette.textTertiary,
                                fontSize = VennelaTokens.nano,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Notes                                                                      */
/* ------------------------------------------------------------------------- */

@Composable
private fun SlideNotes(slide: Slide, textScale: Float) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    // Only offered when the file genuinely carries notes for this slide.
    val notes = slide.notes ?: return
    var expanded by remember(slide.number) { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier.pressScale(0.98f) { expanded = !expanded }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Default.Lightbulb,
                colors = listOf(accent.secondary, accent.primary),
                size = VennelaTokens.badgeSm,
                radius = VennelaTokens.radiusChip,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
            Text(
                text = "Speaker notes",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        AnimatedDisclosure(expanded = expanded)
        if (expanded) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Text(
                text = notes,
                color = palette.textSecondary,
                fontSize = VennelaTokens.bodySmall * textScale,
                lineHeight = VennelaTokens.bodySmall * textScale * 1.4f
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Find in deck                                                               */
/* ------------------------------------------------------------------------- */

@Composable
private fun DeckSearch(
    query: String,
    matches: List<Slide>,
    onQueryChange: (String) -> Unit,
    onJump: (Slide) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Find in deck",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotBlank()) {
                Text(
                    text = "${matches.size} ${if (matches.size == 1) "slide" else "slides"}",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search slide titles, body and notes",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.bodySmall
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                cursorColor = accent.primary
            )
        )
        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            if (matches.isEmpty()) {
                Text(
                    text = "No slide contains \u201C$query\u201D.",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.bodySmall
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                    items(matches, key = { it.number }) { slide ->
                        LiquidChip(
                            text = "${slide.number}. ${slide.title.take(24)}",
                            isActive = false,
                            onClick = { onJump(slide) }
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Outline and info                                                           */
/* ------------------------------------------------------------------------- */

@Composable
private fun OutlinePane(slides: List<Slide>, currentIndex: Int, onSelect: (Int) -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceSm)) {
            Text(
                text = "Outline",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = VennelaTokens.spaceLg)
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            slides.forEachIndexed { position, slide ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .then(
                            Modifier.pressScale(0.985f) {
                                onSelect(slide.number - 1)
                            }
                        )
                        .padding(horizontal = VennelaTokens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${slide.number}",
                        color = if (position == currentIndex) accent.secondary else palette.textTertiary,
                        fontSize = VennelaTokens.caption,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(VennelaTokens.spaceXl)
                    )
                    Text(
                        text = slide.title,
                        color = if (position == currentIndex) palette.textPrimary else palette.textSecondary,
                        fontSize = VennelaTokens.bodySmall,
                        fontWeight = if (position == currentIndex) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (position < slides.lastIndex) GlassDivider()
            }
        }
    }
}

@Composable
private fun DeckInfo(file: FileItem?, deck: Deck?, viewerState: ViewerState) {
    val palette = LocalVennelaPalette.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Details",
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        InfoLine("Name", file?.name ?: "—")
        InfoLine("Format", file?.extension?.uppercase()?.ifBlank { "—" } ?: "—")
        InfoLine("Size", file?.formattedSize ?: "—")
        InfoLine("Modified", file?.formattedDate ?: "—")
        InfoLine("Slides", deck?.slides?.size?.toString() ?: "—")
        InfoLine("Deck title", deck?.title ?: "Not recorded in file")
        InfoLine("Author", deck?.author ?: "Not recorded in file")
        InfoLine("State", viewerState::class.simpleName ?: "—")
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VennelaTokens.spaceXxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textTertiary,
            fontSize = VennelaTokens.caption,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Actions                                                                    */
/* ------------------------------------------------------------------------- */

@Composable
private fun DeckActions(
    file: FileItem?,
    textScale: Float,
    onTextScale: (Float) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOn: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Reading",
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
            items(listOf(0.85f, 1f, 1.15f, 1.3f), key = { it }) { scale ->
                LiquidChip(
                    text = "${(scale * 100).toInt()}%",
                    icon = Icons.Default.TextFields,
                    isActive = kotlin.math.abs(scale - textScale) < 0.01f,
                    onClick = { onTextScale(scale) }
                )
            }
            item(key = "keep") {
                LiquidChip(
                    text = "Keep screen on",
                    isActive = keepScreenOn,
                    onClick = { onKeepScreenOn(!keepScreenOn) }
                )
            }
        }

        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
            IconBadge(
                icon = Icons.Default.Share,
                colors = listOf(accent.primary, accent.secondary),
                contentDescription = "Share this presentation",
                onClick = { shareDeck(context, file) }
            )
            IconBadge(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                colors = listOf(accent.secondary, accent.primary),
                contentDescription = "Open in another app",
                onClick = { openDeckExternally(context, file) }
            )
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        IconBadge(
            icon = Icons.Default.ErrorOutline,
            colors = listOf(accent.secondary, accent.primary),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
        Text(
            text = body,
            color = palette.textSecondary,
            fontSize = VennelaTokens.bodySmall,
            lineHeight = VennelaTokens.bodySmall * 1.45f
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VennelaTokens.touchTargetMin)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(accent.primary.copy(alpha = 0.20f))
                .pressScale(0.98f, onAction),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = actionLabel,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Honest skeleton so the canvas never flashes empty while parsing. */
@Composable
private fun SlideSkeleton() {
    val palette = LocalVennelaPalette.current
    val fill by animateFloatAsState(
        targetValue = 0.22f,
        animationSpec = tween(durationMillis = 700),
        label = "slideSkeleton"
    )
    Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
            alpha = fill
        ) {
            Column(modifier = Modifier.padding(VennelaTokens.spaceLg)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(palette.cardBorder.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (it % 2 == 0) 0.8f else 0.62f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                            .background(palette.cardBorder.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                }
            }
        }
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(VennelaTokens.badgeLg),
            shape = RoundedCornerShape(VennelaTokens.radiusTile),
            alpha = fill
        ) {}
    }
}

/* ------------------------------------------------------------------------- */
/* Side effects and intent helpers                                            */
/* ------------------------------------------------------------------------- */

/** Real window flag, always cleared when the reader leaves. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        val window = hostActivity(context)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (enabled) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun hostActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun shareDeck(context: Context, file: FileItem?) {
    if (file == null) return
    val intent = Intent(Intent.ACTION_SEND).apply { type = file.formatInfo.mimeType }
    if (!ShareUri.attachStream(context, intent, file.uri)) return
    runCatching { context.startActivity(Intent.createChooser(intent, "Share ${file.name}")) }
}

private fun openDeckExternally(context: Context, file: FileItem?) {
    if (file == null) return
    val intent = ShareUri.viewIntent(context, file.uri, file.formatInfo.mimeType) ?: return
    runCatching { context.startActivity(intent) }
}

/* ------------------------------------------------------------------------- */
/* Load state                                                                 */
/* ------------------------------------------------------------------------- */

private sealed interface DeckLoad {
    data object Loading : DeckLoad
    data class Ready(val deck: Deck) : DeckLoad
    data class Unsupported(val extension: String) : DeckLoad
    data class Failed(val message: String) : DeckLoad
}
