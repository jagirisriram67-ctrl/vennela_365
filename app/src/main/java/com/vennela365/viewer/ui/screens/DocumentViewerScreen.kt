package com.vennela365.viewer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.applyViewerColorMode
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.DocumentHighlight
import com.vennela365.viewer.ui.components.DocumentMarkup
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.HyperGutterRadar
import com.vennela365.viewer.ui.components.LassoToComputeOverlay
import com.vennela365.viewer.ui.components.LightboxComparator
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.PrivacyCurtain
import com.vennela365.viewer.ui.components.PrivacyEngine
import com.vennela365.viewer.ui.components.RadarTick
import com.vennela365.viewer.ui.components.RadarTickType
import com.vennela365.viewer.ui.components.SplitHorizonViewer
import com.vennela365.viewer.ui.components.StudySheetCompilerView
import com.vennela365.viewer.ui.components.ViewerDock
import com.vennela365.viewer.ui.components.ViewerDockAction
import com.vennela365.viewer.ui.components.ViewerIconAction
import com.vennela365.viewer.ui.components.ViewerTopBar
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

/* ------------------------------------------------------------------------- */
/* Constants, models, caches                                                  */
/* ------------------------------------------------------------------------- */

private const val DocMinZoom = 0.5f
private const val DocMaxZoom = 3f
private const val DocMinTypeScale = 0.7f
private const val DocMaxTypeScale = 2.2f
private const val DocMinLineScale = 0.85f
private const val DocMaxLineScale = 2f
private const val DocMaxMatches = 400
private const val DocMaxHeadings = 200
private const val DocSkeletonGraceMs = 3_000L

private val DocBaseFontSize = 15f
private val DocBaseLineHeight = 23f

/** Which pro panel the dock currently shows; one tray host instead of five. */
private enum class DocPanel(val label: String) {
    NONE("none"), TEXT("Type & spacing"), CONTENT("Contents"), ZOOM("Zoom"),
    VIEW("Reading view"), MORE("Tools"), INFO("File information")
}

/** A chip rail entry, so rails stay data-driven instead of hand-unrolled. */
private data class DocChip(
    val label: String,
    val icon: ImageVector? = null,
    val active: Boolean = false,
    val onClick: () -> Unit
)

/** Real, cheap text statistics: one pass, no intermediate collections. */
private data class DocStats(val characters: Int, val words: Int, val lines: Int)

private fun docStats(text: String): DocStats {
    if (text.isEmpty()) return DocStats(0, 0, 0)
    var words = 0
    var lines = 1
    var inWord = false
    for (ch in text) {
        when {
            ch == '\n' -> { lines++; inWord = false }
            ch.isWhitespace() -> inWord = false
            !inWord -> { words++; inWord = true }
        }
    }
    return DocStats(text.length, words, lines)
}

/**
 * Parse cache for the expensive derived structures (headline ticks), plus the
 * per-file reading position. Reopening the same document is therefore instant.
 */
private object DocParseCache {
    private const val MaxFiles = 4
    private val ticks = object : LinkedHashMap<String, List<RadarTick>>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<RadarTick>>) =
            size > MaxFiles
    }

    @Synchronized
    fun headings(fileKey: String, text: String): List<RadarTick> {
        ticks[fileKey]?.let { return it }
        val lines = text.lines()
        val total = maxOf(lines.size, 1)
        val parsed = lines.mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#") || trimmed.startsWith("Slide") ->
                    RadarTick(index.toFloat() / total, RadarTickType.HEADING, trimmed.take(28))
                trimmed.contains("error", ignoreCase = true) ||
                    trimmed.contains("fail", ignoreCase = true) ->
                    RadarTick(index.toFloat() / total, RadarTickType.ERROR_ANOMALY, "Anomaly")
                index % 25 == 0 && index > 0 ->
                    RadarTick(index.toFloat() / total, RadarTickType.BOOKMARK, "Section")
                else -> null
            }
        }
        if (text.isNotBlank()) ticks[fileKey] = parsed
        return parsed
    }
}

/**
 * Reading state remembered per document: scroll offset, zoom and the two text
 * scales. Declared at file level so the composable's `remember`/`LaunchedEffect`
 * blocks can name the type of the lambdas they pass to [DocReadingMemory].
 */
private data class DocSaved(
    val scroll: Int = 0,
    val zoom: Float = 1f,
    val typeScale: Float = 1f,
    val lineScale: Float = 1f
) {
    fun with(
        scroll: Int = this.scroll,
        zoom: Float = this.zoom,
        typeScale: Float = this.typeScale,
        lineScale: Float = this.lineScale
    ) = DocSaved(scroll, zoom, typeScale, lineScale)
}

private object DocReadingMemory {
    private const val MaxFiles = 12
    private val state = object : LinkedHashMap<String, DocSaved>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DocSaved>) = size > MaxFiles
    }

    @Synchronized fun get(fileKey: String): DocSaved = state[fileKey] ?: DocSaved()

    @Synchronized
    fun update(fileKey: String, transform: (DocSaved) -> DocSaved) {
        state[fileKey] = transform(state[fileKey] ?: DocSaved())
    }
}

private fun docHostActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Real window-level keep-screen-on flag, always cleared when the screen leaves. */
@Composable
private fun DocKeepScreenFlag(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        val window = docHostActivity(context)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/**
 * A plain float holder for values measured during layout.
 *
 * Position callbacks fire on every scroll frame; keeping them out of the
 * snapshot system means no recomposition is scheduled just to record a
 * measurement.
 */
private class LayoutFloat(initial: Float = 0f) {
    var value: Float = initial
}

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * Document viewer — the reading surface.
 *
 * Hierarchy follows `Screens/8.document_viewer(docx).png`: a floating frosted
 * toolbar, the extracted body on a paper page, a hyper-gutter progress rail down
 * the right edge and a floating glass dock.
 *
 * The extraction pipeline itself (`viewModel.documentText`) is untouched. This
 * file adds the pro reading layer: quick find with a real match count, heading
 * jump and outline, zoom presets, reading display modes, type and spacing
 * steppers, keep-screen-on, per-file reading position, real share / open-with /
 * save-a-copy plus a metadata panel that only reports computable values.
 */
@Composable
fun DocumentViewerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val accent = LocalVennelaAccent.current
    val palette = LocalVennelaPalette.current
    val density = LocalDensity.current

    val selectedFile by viewModel.selectedFile.collectAsState()
    val extractedText by viewModel.documentText.collectAsState()
    val truncated by viewModel.documentTruncated.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val isLassoActive by viewModel.isLassoActive.collectAsState()
    val isSplitHorizonActive by viewModel.isSplitHorizonActive.collectAsState()
    val isPrivacyActive by viewModel.isPrivacyActive.collectAsState()
    val enableMicroLouvre by viewModel.enableMicroLouvre.collectAsState()
    val isStudySheetActive by viewModel.isStudySheetActive.collectAsState()
    val isLightboxActive by viewModel.isLightboxActive.collectAsState()

    val scope = rememberCoroutineScope()
    val fileKey = selectedFile?.id ?: "no-doc"

    // Reading position memory — restored per file id.
    val saved = remember(fileKey) { DocReadingMemory.get(fileKey) }
    var textScale by rememberSaveable(fileKey) { mutableStateOf(saved.typeScale) }
    var lineScale by rememberSaveable(fileKey) { mutableStateOf(saved.lineScale) }
    var zoom by rememberSaveable(fileKey) { mutableStateOf(saved.zoom) }
    var panel by remember { mutableStateOf(DocPanel.NONE) }
    var keepScreenOn by rememberSaveable { mutableStateOf(false) }
    var findActive by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findIndex by remember { mutableIntStateOf(0) }

    // Layout-time measurements live outside the snapshot system on purpose:
    // they are rewritten on every scroll frame, and snapshot writes during
    // layout would schedule needless recompositions on the whole page.
    val scrollTopInRoot = remember { LayoutFloat() }
    val bodyTopInContent = remember { LayoutFloat(-1f) }
    var bodyLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val scrollState = rememberScrollState()
    val fileName = selectedFile?.name ?: "Document"
    val fontSize = (DocBaseFontSize * textScale * zoom).sp
    val lineHeight = (DocBaseLineHeight * textScale * lineScale * zoom).sp

    // Restore and then continuously remember the scroll offset for this file.
    var positionRestored by remember(fileKey) { mutableStateOf(false) }
    LaunchedEffect(fileKey, scrollState.maxValue) {
        if (!positionRestored && scrollState.maxValue > 0) {
            if (saved.scroll > 0) scrollState.scrollTo(saved.scroll.coerceIn(0, scrollState.maxValue))
            positionRestored = true
        }
    }
    LaunchedEffect(fileKey, textScale, lineScale, zoom) {
        DocReadingMemory.update(fileKey) {
            it.with(typeScale = textScale, lineScale = lineScale, zoom = zoom)
        }
    }
    LaunchedEffect(fileKey) {
        snapshotFlow { scrollState.value }.collect { offset ->
            DocReadingMemory.update(fileKey) { it.with(scroll = offset) }
        }
    }
    DocKeepScreenFlag(keepScreenOn)

    // Heading / anomaly ticks, cached per file so reopening is instant.
    val radarTicks = remember(fileKey, extractedText) {
        if (extractedText.isBlank()) emptyList() else DocParseCache.headings(fileKey, extractedText)
    }
    val headings = remember(radarTicks) { radarTicks.filter { it.type == RadarTickType.HEADING } }
    val headingLabels = remember(headings) { headings.take(DocMaxHeadings) }

    val activeDisplayText = remember(extractedText, isPrivacyActive) {
        if (isPrivacyActive) PrivacyEngine.maskPii(extractedText) else extractedText
    }

    // Honest loading state: skeleton until the extractor has had a fair chance.
    var parseSettled by remember(fileKey) { mutableStateOf(false) }
    LaunchedEffect(fileKey, extractedText) {
        when {
            extractedText.isNotBlank() -> parseSettled = true
            selectedFile?.uri == null -> parseSettled = true
            else -> {
                delay(DocSkeletonGraceMs)
                parseSettled = true
            }
        }
    }

    val stats = remember(activeDisplayText) { docStats(activeDisplayText) }

    val matches = remember(activeDisplayText, findQuery) {
        val query = findQuery
        if (query.isBlank()) {
            emptyList()
        } else {
            val found = ArrayList<Int>()
            var cursor = activeDisplayText.indexOf(query, 0, ignoreCase = true)
            var guard = 0
            while (cursor >= 0 && guard < DocMaxMatches) {
                found.add(cursor)
                cursor = activeDisplayText.indexOf(
                    query, cursor + query.length.coerceAtLeast(1), ignoreCase = true
                )
                guard++
            }
            found
        }
    }
    val safeFindIndex = if (matches.isEmpty()) 0 else findIndex.coerceIn(0, matches.lastIndex)

    // Rich, Docs-like markup: headings, lists, quotes and tables are recovered
    // from the text layer, while find hits are mapped back to source offsets.
    val annotatedBody = remember(
        activeDisplayText, matches, safeFindIndex, accent, palette, findQuery, fontSize, lineHeight
    ) {
        val highlights = if (matches.isEmpty()) {
            emptyList()
        } else {
            matches.mapIndexed { idx, start ->
                DocumentHighlight(
                    start = start,
                    end = (start + findQuery.length).coerceAtMost(activeDisplayText.length),
                    primary = idx == safeFindIndex
                )
            }
        }
        DocumentMarkup.build(
            source = activeDisplayText,
            typography = DocumentMarkup.Typography(
                bodySize = fontSize,
                ink = palette.textPrimary,
                muted = palette.textSecondary,
                accent = accent.primary,
                tableTint = accent.primary.copy(alpha = 0.10f),
                isDarkPaper = palette.isDark
            ),
            highlights = highlights
        )
    }

    // Precise match scrolling: bounding box of the match inside the laid-out text.
    LaunchedEffect(safeFindIndex, matches, findQuery, activeDisplayText) {
        if (matches.isEmpty()) return@LaunchedEffect
        val start = matches.getOrNull(safeFindIndex) ?: return@LaunchedEffect
        val target = runCatching {
            val layout = bodyLayout
            if (layout != null && bodyTopInContent.value >= 0f && start < layout.layoutInput.text.length) {
                bodyTopInContent.value + layout.getBoundingBox(start).top - with(density) { 150.dp.toPx() }
            } else {
                start.toFloat() / activeDisplayText.length.coerceAtLeast(1).toFloat() * scrollState.maxValue
            }
        }.getOrDefault(0f)
        scrollState.animateScrollTo(target.roundToInt().coerceIn(0, scrollState.maxValue))
    }

    val progress = if (scrollState.maxValue > 0) {
        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    } else {
        0f
    }
    val activeHeadingIndex = remember(headings, progress) {
        headings.indexOfLast { it.normalizedPosition <= progress + 0.001f }
    }
    val activeHeading = headings.getOrNull(activeHeadingIndex)

    fun goToTick(tick: RadarTick) {
        scope.launch {
            scrollState.animateScrollTo(
                (tick.normalizedPosition * scrollState.maxValue).roundToInt()
                    .coerceIn(0, scrollState.maxValue)
            )
        }
    }

    fun stepSection(delta: Int) {
        if (headings.isEmpty()) return
        val index = (activeHeadingIndex + delta).coerceIn(0, headings.lastIndex)
        goToTick(headings[index])
    }

    // ---- Save-as: writes a real copy of the extracted text ---------------
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            selectedFile?.formatInfo?.mimeType?.takeIf { it.isNotBlank() } ?: "text/plain"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val payload = if (activeDisplayText.isNotBlank()) activeDisplayText else extractedText
        scope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    true
                }.getOrDefault(false)
            }
            Toast.makeText(
                context,
                if (written) "Saved a copy of ${selectedFile?.name ?: "the document"}"
                else "Could not write to that location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val mime = selectedFile?.formatInfo?.mimeType?.takeIf { it.isNotBlank() } ?: "text/plain"

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun shareDocument() {
        val uri = selectedFile?.uri ?: return toast("There is no file to share")
        val intent = Intent(Intent.ACTION_SEND).apply { type = mime }
        if (!ShareUri.attachStream(context, intent, uri)) {
            return toast("This file's location cannot be handed to another app")
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share $fileName")) }
            .onFailure { toast("No app accepted this file") }
    }

    fun openWith() {
        val intent = ShareUri.viewIntent(context, selectedFile?.uri, mime)
            ?: return toast("This file's location cannot be handed to another app")
        runCatching { context.startActivity(Intent.createChooser(intent, "Open with")) }
            .onFailure { toast("No app on this device can open this document") }
    }

    val zoomChips = listOf(
        DocChip("Fit width", Icons.Default.AspectRatio, kotlin.math.abs(zoom - 1f) < 0.02f) { zoom = 1f },
        DocChip("50%", active = kotlin.math.abs(zoom - 0.5f) < 0.02f) { zoom = 0.5f },
        DocChip("100%", active = kotlin.math.abs(zoom - 1f) < 0.02f) { zoom = 1f },
        DocChip("150%", active = kotlin.math.abs(zoom - 1.5f) < 0.02f) { zoom = 1.5f },
        DocChip("200%", active = kotlin.math.abs(zoom - 2f) < 0.02f) { zoom = 2f },
        DocChip("Zoom out", Icons.Default.ZoomOut) { zoom = (zoom - 0.25f).coerceAtLeast(DocMinZoom) },
        DocChip("Zoom in", Icons.Default.ZoomIn) { zoom = (zoom + 0.25f).coerceAtMost(DocMaxZoom) }
    )
    val modeChips = listOf(
        DocChip("Normal", Icons.Default.Visibility, viewerColorMode == ViewerColorMode.ORIGINAL) {
            viewModel.setViewerColorMode(ViewerColorMode.ORIGINAL)
        },
        DocChip("Sepia", Icons.AutoMirrored.Filled.MenuBook, viewerColorMode == ViewerColorMode.SEPIA) {
            viewModel.setViewerColorMode(ViewerColorMode.SEPIA)
        },
        DocChip("Night", Icons.Default.DarkMode, viewerColorMode == ViewerColorMode.OKLAB_DARK) {
            viewModel.setViewerColorMode(ViewerColorMode.OKLAB_DARK)
        },
        DocChip(
            "High contrast", Icons.Default.Contrast,
            viewerColorMode == ViewerColorMode.HIGH_CONTRAST_INVERTED
        ) { viewModel.setViewerColorMode(ViewerColorMode.HIGH_CONTRAST_INVERTED) }
    )
    val toolChips = listOf(
        DocChip("Find in document", Icons.Default.Search, findActive) {
            findActive = !findActive
            if (!findActive) findQuery = ""
        },
        DocChip("Keep screen on", Icons.Default.Visibility, keepScreenOn) { keepScreenOn = !keepScreenOn },
        DocChip("Share", Icons.Default.Share) { shareDocument() },
        DocChip("Open with", Icons.AutoMirrored.Filled.OpenInNew) { openWith() },
        DocChip("Save a copy", Icons.Default.SaveAlt) {
            saveLauncher.launch(selectedFile?.name ?: "vennela-document.txt")
        },
        DocChip("Share screen", Icons.Default.Share) { viewModel.navigateTo(Screen.ShareScreen) },
        DocChip("Universal search", Icons.Default.Search) { viewModel.navigateTo(Screen.UniversalSearch) },
        DocChip("Lasso to compute", Icons.Default.Calculate, isLassoActive) { viewModel.toggleLasso() },
        DocChip("Split view", Icons.Default.VerticalSplit, isSplitHorizonActive) { viewModel.toggleSplitHorizon() },
        DocChip("Privacy curtain", Icons.Default.Security, isPrivacyActive) { viewModel.togglePrivacy() },
        DocChip("Study sheet", Icons.Default.AutoAwesome) { viewModel.showStudySheet() },
        DocChip("Compare revisions", Icons.Default.AspectRatio) { viewModel.showLightbox() },
        DocChip("Metadata screen", Icons.Default.Info) { viewModel.navigateTo(Screen.FileMetadata) },
        DocChip("File information", Icons.Default.Info, panel == DocPanel.INFO) { panel = DocPanel.INFO }
    )
    val infoRows = listOf(
        "Name" to fileName,
        "Size" to (selectedFile?.formattedSize ?: "Unknown"),
        "Modified" to (selectedFile?.formattedDate ?: "Unknown"),
        "Format" to (selectedFile?.formatInfo?.name ?: "Unknown"),
        "Extension" to (selectedFile?.extension?.uppercase()?.ifBlank { "none" } ?: "Unknown"),
        "Characters" to if (stats.characters > 0) "${stats.characters}" else "No text extracted",
        "Words" to if (stats.words > 0) "${stats.words}" else "No text extracted",
        "Lines" to "${stats.lines}",
        "Headings detected" to "${headings.size}",
        "Read position" to "${(progress * 100).roundToInt()}% • line ${((progress * stats.lines).toInt() + 1)}",
        "Reading mode" to viewerColorMode.title,
        "Type size / line pitch" to "${(textScale * 100).roundToInt()}% • ${(lineScale * 100).roundToInt()}%"
    )

    LiquidPage(
        modifier = Modifier.fillMaxSize(),
        title = null,
        dock = {
            // Single tray host: every pro panel renders here, no param plumbing.
            DocTray(visible = panel != DocPanel.NONE) {
                when (panel) {
                    DocPanel.TEXT -> {
                        DocTrayHeader(
                            Icons.Default.FormatSize, "Type size & spacing",
                            "${(textScale * 100).roundToInt()}% • line ${(lineScale * 100).roundToInt()}%"
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocStepperRow(
                            label = "Type size",
                            value = "${(textScale * 100).roundToInt()}%",
                            onDecrease = {
                                textScale = (textScale - 0.1f).coerceIn(DocMinTypeScale, DocMaxTypeScale)
                            },
                            onIncrease = {
                                textScale = (textScale + 0.1f).coerceIn(DocMinTypeScale, DocMaxTypeScale)
                            },
                            slider = {
                                Slider(
                                    value = textScale,
                                    onValueChange = { textScale = it },
                                    valueRange = DocMinTypeScale..DocMaxTypeScale,
                                    colors = docSliderColors(accent.secondary, palette.cardBorder),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        )
                        DocStepperRow(
                            label = "Line spacing",
                            value = "${(lineScale * 100).roundToInt()}%",
                            onDecrease = {
                                lineScale = (lineScale - 0.1f).coerceIn(DocMinLineScale, DocMaxLineScale)
                            },
                            onIncrease = {
                                lineScale = (lineScale + 0.1f).coerceIn(DocMinLineScale, DocMaxLineScale)
                            },
                            slider = {
                                Slider(
                                    value = lineScale,
                                    onValueChange = { lineScale = it },
                                    valueRange = DocMinLineScale..DocMaxLineScale,
                                    colors = docSliderColors(accent.primary, palette.cardBorder),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocChipRail(
                            listOf(
                                DocChip("Compact", active = lineScale < 1.05f) { lineScale = 0.95f },
                                DocChip("Cozy", active = lineScale in 1.05f..1.35f) { lineScale = 1.2f },
                                DocChip("Airy", active = lineScale > 1.35f) { lineScale = 1.5f },
                                DocChip("Reset type") { textScale = 1f; lineScale = 1f }
                            )
                        )
                    }

                    DocPanel.CONTENT -> {
                        DocTrayHeader(
                            Icons.AutoMirrored.Filled.FormatListBulleted, "Detected contents",
                            "${headings.size} headings"
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        if (headings.isEmpty()) {
                            Text(
                                "No headings were detected in this document's text layer, so there is no outline " +
                                    "to jump between yet.",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = VennelaTokens.spaceSm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    activeHeading?.label ?: "Before the first heading",
                                    color = palette.textSecondary,
                                    fontSize = VennelaTokens.micro,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                DocRoundAction(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous section"
                                ) { stepSection(-1) }
                                DocRoundAction(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next section"
                                ) { stepSection(1) }
                            }
                            if (headings.size > DocMaxHeadings) {
                                Text(
                                    "Showing the first $DocMaxHeadings of ${headings.size} entries",
                                    color = palette.textTertiary,
                                    fontSize = VennelaTokens.micro
                                )
                            }
                            LazyColumn(Modifier.fillMaxWidth().height(VennelaTokens.spaceHuge * 5)) {
                                items(count = headingLabels.size, key = { index -> "head_$index" }) { index ->
                                    val tick = headingLabels[index]
                                    val active = index == activeHeadingIndex
                                    GlassListRow(
                                        title = tick.label ?: "Section",
                                        subtitle = "${(tick.normalizedPosition * 100).roundToInt()}% through the document",
                                        modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                                        leading = {
                                            Box(
                                                Modifier.size(VennelaTokens.spaceSm)
                                                    .clip(CircleShape)
                                                    .background(docTickColor(tick, accent.secondary, palette.textTertiary))
                                            )
                                        },
                                        onClick = { goToTick(tick) }
                                    )
                                    if (index < headingLabels.lastIndex) GlassDivider()
                                }
                            }
                        }
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocChipRail(
                            listOf(
                                DocChip("Jump to top") {
                                    scope.launch { scrollState.animateScrollTo(0) }
                                },
                                DocChip("Jump to end") {
                                    scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                                },
                                DocChip("Full contents screen", Icons.Default.Info) {
                                    viewModel.navigateTo(Screen.PdfToc)
                                }
                            )
                        )
                    }

                    DocPanel.ZOOM -> {
                        DocTrayHeader(
                            Icons.Default.ZoomIn, "Zoom",
                            "${(zoom * 100).roundToInt()}% • Fit width is 100%"
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocChipRail(zoomChips)
                    }

                    DocPanel.VIEW -> {
                        Text(
                            "Display mode — ${viewerColorMode.title}",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.caption,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocChipRail(modeChips)
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocChipRail(toolChips.take(4))
                    }

                    DocPanel.MORE -> {
                        DocTrayHeader(Icons.Default.MoreHoriz, "Tools", "pdf printing is not offered here")
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        DocChipRail(toolChips.drop(4))
                    }

                    DocPanel.INFO -> {
                        DocTrayHeader(Icons.Default.Info, "File information", "real values only")
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        infoRows.forEachIndexed { index, (label, value) ->
                            GlassListRow(
                                title = label,
                                subtitle = value,
                                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
                            )
                            if (index < infoRows.lastIndex) GlassDivider()
                        }
                    }

                    DocPanel.NONE -> Unit
                }
            }

            // ---- Floating icon dock -------------------------------------
            ViewerDock {
                ViewerDockAction(
                    Icons.AutoMirrored.Filled.FormatListBulleted, "Contents",
                    active = panel == DocPanel.CONTENT
                ) { panel = if (panel == DocPanel.CONTENT) DocPanel.NONE else DocPanel.CONTENT }
                ViewerDockAction(Icons.Default.FormatSize, "Text", active = panel == DocPanel.TEXT) {
                    panel = if (panel == DocPanel.TEXT) DocPanel.NONE else DocPanel.TEXT
                }
                ViewerDockAction(Icons.Default.ZoomIn, "Zoom", active = panel == DocPanel.ZOOM) {
                    panel = if (panel == DocPanel.ZOOM) DocPanel.NONE else DocPanel.ZOOM
                }
                ViewerDockAction(
                    Icons.Default.ColorLens, "Reading theme",
                    active = panel == DocPanel.VIEW || viewerColorMode != ViewerColorMode.ORIGINAL
                ) { panel = if (panel == DocPanel.VIEW) DocPanel.NONE else DocPanel.VIEW }
                ViewerDockAction(Icons.Default.MoreHoriz, "More tools", active = panel == DocPanel.MORE) {
                    panel = if (panel == DocPanel.MORE) DocPanel.NONE else DocPanel.MORE
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val paneModifier = Modifier
                .fillMaxSize()
                .applyViewerColorMode(viewerColorMode)
                .padding(horizontal = VennelaTokens.gutter)
                .verticalScroll(scrollState)
                .onGloballyPositioned { scrollTopInRoot.value = it.positionInRoot().y }

            val bodyPane: @Composable () -> Unit = {
                Column(paneModifier) {
                    Spacer(Modifier.height(VennelaTokens.viewerTopInset))
                    when {
                        activeDisplayText.isNotBlank() -> RealDocumentCard(
                            fileName = fileName,
                            subtitle = selectedFile?.formatInfo?.name ?: "Document",
                            body = annotatedBody,
                            truncated = truncated,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            onBodyLayout = { bodyLayout = it },
                            onBodyPositioned = { yInRoot ->
                                bodyTopInContent.value = yInRoot - scrollTopInRoot.value + scrollState.value
                            }
                        )

                        selectedFile?.uri != null && !parseSettled -> DocGlassSkeleton()

                        selectedFile?.uri != null -> VennelaErrorState(
                            title = "No readable text layer",
                            message = "This document yielded no text — it may store its layout as binary streams " +
                                "only. Retrying runs the extractor again; nothing is modified.",
                            onRetry = { selectedFile?.let { viewModel.openFile(it) } },
                            onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                        )

                        else -> VennelaEmptyState(
                            title = "No document open",
                            hint = "Open a document from Files or Recents to read it here.",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            actionLabel = "Browse files",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceLg))
                }
            }

            if (isSplitHorizonActive) {
                SplitHorizonViewer(
                    modifier = Modifier.fillMaxSize(),
                    referenceTitle = "Reference: ${fileName.take(22)}",
                    onCloseReference = { viewModel.toggleSplitHorizon() },
                    referenceContent = {
                        Box(
                            Modifier.fillMaxSize()
                                .padding(
                                    horizontal = VennelaTokens.gutter,
                                    vertical = VennelaTokens.spaceHuge
                                )
                                .verticalScroll(rememberScrollState())
                        ) {
                            RealDocumentCard(
                                fileName = fileName,
                                subtitle = "Pinned reference",
                                body = buildAnnotatedString {
                                    append(
                                        if (activeDisplayText.isNotBlank()) activeDisplayText.take(900)
                                        else "Pin a section from the document to compare it here."
                                    )
                                },
                                truncated = false,
                                fontSize = (13f * textScale * zoom).sp,
                                lineHeight = (19f * textScale * lineScale * zoom).sp
                            )
                        }
                    },
                    primaryContent = bodyPane
                )
            } else {
                bodyPane()
            }

            // ---- Slim floating toolbar (floats over the page) ------------
            ViewerTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                title = fileName,
                subtitle = buildString {
                    append(selectedFile?.formatInfo?.name ?: "Document")
                    append(" • ${viewerColorMode.title}")
                    append(" • ${(zoom * 100).roundToInt()}%")
                },
                progress = progress,
                onBack = { viewModel.navigateBack() },
                actions = {
                    ViewerIconAction(
                        Icons.Default.Search,
                        contentDescription = "Find in document",
                        active = findActive,
                        onClick = {
                            findActive = !findActive
                            if (!findActive) findQuery = ""
                        }
                    )
                    ViewerIconAction(
                        Icons.Default.ColorLens,
                        contentDescription = "Reading theme • ${viewerColorMode.title}",
                        onClick = { viewModel.cycleViewerColorMode() }
                    )
                    ViewerIconAction(
                        Icons.Default.SaveAlt,
                        contentDescription = "Save a copy",
                        onClick = { saveLauncher.launch(selectedFile?.name ?: "vennela-document.txt") }
                    )
                }
            )

            // ---- Quick find bar with a real match count ------------------
            AnimatedVisibility(
                visible = findActive,
                enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.glide()),
                exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit()),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = VennelaTokens.viewerTopInset + VennelaTokens.spaceXs)
            ) {
                FindInDocumentBar(
                    query = findQuery,
                    onQueryChange = { findQuery = it; findIndex = 0 },
                    matchCount = matches.size,
                    currentMatch = if (matches.isEmpty()) 0 else safeFindIndex + 1,
                    onPrevious = {
                        if (matches.isNotEmpty()) {
                            findIndex = (safeFindIndex - 1 + matches.size) % matches.size
                        }
                    },
                    onNext = {
                        if (matches.isNotEmpty()) findIndex = (safeFindIndex + 1) % matches.size
                    },
                    onClose = { findActive = false; findQuery = "" }
                )
            }

            // ---- Reading position pill -----------------------------------
            ReadingProgressPill(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = VennelaTokens.gutter, bottom = VennelaTokens.spaceXl),
                progress = progress,
                lineNumber = ((progress * stats.lines).toInt() + 1).coerceAtLeast(1),
                totalLines = maxOf(stats.lines, 1),
                readingModeTitle = viewerColorMode.title,
                visible = extractedText.isNotBlank() && !isSplitHorizonActive
            )

            // ---- Hyper-gutter radar rail ---------------------------------
            HyperGutterRadar(
                ticks = radarTicks,
                currentScrollProgress = progress,
                onScrub = { ratio ->
                    scope.launch { scrollState.scrollTo((ratio * scrollState.maxValue).toInt()) }
                },
                formatPageOrLineLabel = { ratio ->
                    val line = ceil(ratio * stats.lines).toInt().coerceAtLeast(1)
                    "Line $line"
                }
            )

            LassoToComputeOverlay(
                isActive = isLassoActive,
                onClose = { viewModel.toggleLasso() },
                sourceTextProvider = { extractedText }
            )

            PrivacyCurtain(
                isActive = isPrivacyActive,
                onClose = { viewModel.togglePrivacy() },
                enableMicroLouvre = enableMicroLouvre,
                onToggleMicroLouvre = { viewModel.setMicroLouvre(it) }
            )

            if (isLightboxActive) {
                LightboxComparator(
                    originalText = extractedText,
                    modifiedText = activeDisplayText,
                    onClose = { viewModel.dismissLightbox() }
                )
            }

            if (isStudySheetActive) {
                StudySheetCompilerView(
                    rawText = if (extractedText.isNotBlank()) extractedText else "Document text extraction ready.",
                    title = fileName,
                    onClose = { viewModel.dismissStudySheet() }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Tray chrome and shared controls                                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun DocTray(visible: Boolean, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.liquid()),
        exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit())
    ) {
        Box(Modifier.fillMaxWidth().padding(bottom = VennelaTokens.spaceSm)) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(VennelaTokens.radiusCard),
                elevation = VennelaTokens.elevationOverlay
            ) {
                Column(Modifier.padding(VennelaTokens.spaceLg), content = content)
            }
        }
    }
}

@Composable
private fun DocTrayHeader(icon: ImageVector, title: String, trailing: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm))
        Spacer(Modifier.width(VennelaTokens.spaceSm))
        Text(
            title, color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(trailing, color = palette.textSecondary, fontSize = VennelaTokens.micro, maxLines = 1)
    }
}

/** Horizontally scrolling chip rail; never a fixed row that can clip. */
@Composable
private fun DocChipRail(chips: List<DocChip>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
    ) {
        items(count = chips.size, key = { index -> "${chips[index].label}_$index" }) { index ->
            val chip = chips[index]
            LiquidChip(
                text = chip.label,
                icon = chip.icon,
                isActive = chip.active,
                onClick = chip.onClick,
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        }
    }
}

/** One real stepper + slider pair: both write the same rendering value. */
@Composable
private fun DocStepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    slider: @Composable RowScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) row@{
        Column(Modifier.width(VennelaTokens.spaceHuge * 2)) {
            Text(label, color = palette.textPrimary, fontSize = VennelaTokens.caption, fontWeight = FontWeight.SemiBold)
            Text(value, color = palette.textSecondary, fontSize = VennelaTokens.micro)
        }
        DocRoundAction(Icons.Default.Remove, "Decrease $label", onClick = onDecrease)
        // `slider` is a RowScope extension, so it must be invoked with the Row
        // as an explicit receiver rather than from inside the Box.
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { this@row.slider() }
        DocRoundAction(Icons.Default.Add, "Increase $label", onClick = onIncrease)
    }
}

@Composable
private fun DocRoundAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        Modifier.size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .background(accent.primary.copy(alpha = 0.20f))
            .border(1.dp, accent.secondary.copy(alpha = 0.35f), CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = palette.textPrimary, modifier = Modifier.size(VennelaTokens.iconSm))
    }
}

@Composable
private fun RowScope.DocDockAction(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .weight(1f)
            .height(VennelaTokens.touchTargetMin + VennelaTokens.spaceSm)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .then(
                if (active) {
                    Modifier
                        .background(accent.primary.copy(alpha = 0.30f))
                        .border(1.dp, accent.secondary.copy(alpha = 0.55f), RoundedCornerShape(VennelaTokens.radiusControl))
                } else {
                    Modifier
                }
            )
            .pressScale(pressedScale = 0.93f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = palette.textPrimary, modifier = Modifier.size(VennelaTokens.iconMd))
        Spacer(Modifier.height(VennelaTokens.spaceXxs))
        Text(
            text = label,
            color = if (active) palette.textPrimary else palette.textSecondary,
            fontSize = VennelaTokens.micro,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun docTickColor(tick: RadarTick, accentColor: Color, mutedColor: Color): Color = when (tick.type) {
    RadarTickType.HEADING -> accentColor
    RadarTickType.SEARCH_MATCH -> accentColor
    RadarTickType.BOOKMARK -> mutedColor
    RadarTickType.ERROR_ANOMALY -> accentColor
}

@Composable
private fun docSliderColors(active: Color, inactive: Color) = SliderDefaults.colors(
    thumbColor = active,
    activeTrackColor = active,
    inactiveTrackColor = inactive
)

/* ------------------------------------------------------------------------- */
/* Collapsible toolbar                                                        */
/* ------------------------------------------------------------------------- */

@Composable
private fun CollapsibleDocToolbar(
    modifier: Modifier = Modifier,
    fileName: String,
    subtitle: String,
    collapse: Float,
    barHeight: androidx.compose.ui.unit.Dp,
    progress: Float,
    onBack: () -> Unit,
    onFind: () -> Unit,
    onColorMode: () -> Unit,
    onSaveAs: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.34f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationBar
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceXs)
            .height(barHeight)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusCardLarge))
    ) {
        Row(
            Modifier.weight(1f).padding(horizontal = VennelaTokens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(VennelaTokens.badgeSm)
                    .clip(CircleShape)
                    .background(accent.primary.copy(alpha = 0.26f))
                    .border(1.dp, accent.secondary.copy(alpha = 0.34f), CircleShape)
                    .pressScale(pressedScale = 0.90f, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(VennelaTokens.iconMd)
                )
            }
            AnimatedVisibility(
                visible = collapse < 0.55f,
                enter = fadeIn(LiquidMotion.glide(LiquidMotion.durationFast)) + expandHorizontally(LiquidMotion.liquid()),
                exit = fadeOut(LiquidMotion.exit()) + shrinkHorizontally(LiquidMotion.exit())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(VennelaTokens.spaceSm))
                    Box(
                        Modifier.size(VennelaTokens.badgeSm)
                            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.primary, accent.secondary.copy(alpha = 0.86f))
                                )
                            )
                            .border(
                                1.dp,
                                palette.textPrimary.copy(alpha = 0.28f),
                                RoundedCornerShape(VennelaTokens.radiusChip)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(VennelaTokens.iconMd)
                        )
                    }
                }
            }
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = palette.textPrimary,
                    fontSize = lerp(17.sp, 14.sp, collapse),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(
                    visible = collapse < 0.5f,
                    enter = fadeIn(LiquidMotion.glide(LiquidMotion.durationFast)),
                    exit = fadeOut(LiquidMotion.exit())
                ) {
                    Text(
                        text = subtitle,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ToolbarAction(Icons.Default.Search, "Find in document", onFind)
            ToolbarAction(Icons.Default.ColorLens, "Reading theme", onColorMode)
            ToolbarAction(Icons.Default.SaveAlt, "Save a copy", onSaveAs)
        }
        Box(
            Modifier.fillMaxWidth()
                .height(2.5.dp)
                .background(palette.cardBorder.copy(alpha = 0.30f))
        ) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
            )
        }
    }
}

@Composable
private fun ToolbarAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    Box(
        Modifier.size(VennelaTokens.viewerControl)
            .clip(CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = palette.textPrimary,
            modifier = Modifier.size(VennelaTokens.viewerIcon)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Find bar and reading pill                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun FindInDocumentBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    currentMatch: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.42f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationOverlay
    )
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(
                start = VennelaTokens.spaceMd,
                end = VennelaTokens.spaceXs,
                top = VennelaTokens.spaceXxs,
                bottom = VennelaTokens.spaceXxs
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.textPrimary, fontSize = VennelaTokens.bodySmall),
                cursorBrush = SolidColor(accent.secondary),
                modifier = Modifier.weight(1f).padding(vertical = VennelaTokens.spaceSm),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Find in document",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.bodySmall
                        )
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Text(
                    text = if (matchCount == 0) "No matches" else "$currentMatch of $matchCount",
                    color = if (matchCount == 0) accent.primary else palette.textSecondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            ToolbarAction(Icons.Default.ArrowUpward, "Previous match", onPrevious)
            ToolbarAction(Icons.Default.ArrowDownward, "Next match", onNext)
            ToolbarAction(Icons.Default.Close, "Close find bar", onClose)
        }
    }
}

@Composable
private fun ReadingProgressPill(
    modifier: Modifier = Modifier,
    progress: Float,
    lineNumber: Int,
    totalLines: Int,
    readingModeTitle: String,
    visible: Boolean
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(LiquidMotion.glide()),
        exit = fadeOut(LiquidMotion.exit()),
        modifier = modifier
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(VennelaTokens.radiusPill),
                    clip = false,
                    ambientColor = accent.primary.copy(alpha = 0.55f),
                    spotColor = accent.secondary.copy(alpha = 0.55f)
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            palette.cardSurfaceStrong.copy(alpha = 0.88f),
                            palette.cardSurface.copy(alpha = 0.74f)
                        )
                    )
                )
                .border(1.dp, accent.secondary.copy(alpha = 0.45f), RoundedCornerShape(VennelaTokens.radiusPill))
                .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
        ) {
            Box(Modifier.size(VennelaTokens.spaceSm).clip(CircleShape).background(accent.secondary))
            Text(
                "Line $lineNumber/$totalLines",
                color = palette.textPrimary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${(progress * 100).roundToInt()}% • $readingModeTitle",
                color = palette.textSecondary,
                fontSize = VennelaTokens.micro
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Frosted skeleton                                                           */
/* ------------------------------------------------------------------------- */

/** Shimmering frosted placeholder shown while the text layer is extracted. */
@Composable
private fun DocGlassSkeleton() {
    val palette = LocalVennelaPalette.current
    val transition = rememberInfiniteTransition(label = "docSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(LiquidMotion.durationSlow, easing = LiquidMotion.glideEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "docSkeletonAlpha"
    )
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusCard))
            .background(palette.cardSurface.copy(alpha = 0.35f))
            .padding(VennelaTokens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
    ) {
        repeat(7) { index ->
            Box(
                Modifier.fillMaxWidth(if (index % 3 == 2) 0.62f else 1f)
                    .height(VennelaTokens.spaceLg)
                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                    .background(palette.cardSurface.copy(alpha = alpha))
            )
        }
        Text(
            "Extracting the text layer…",
            color = palette.textSecondary,
            fontSize = VennelaTokens.caption
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Paper page                                                                 */
/* ------------------------------------------------------------------------- */

/**
 * The paper page the extracted text is rendered on.
 *
 * Paper and ink come from the active palette tokens, so light and dark themes
 * both read correctly and the colour-mode matrix still transforms the result.
 */
@Composable
fun RealDocumentCard(
    fileName: String,
    subtitle: String,
    body: AnnotatedString,
    truncated: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 23.sp,
    onBodyLayout: ((TextLayoutResult) -> Unit)? = null,
    onBodyPositioned: ((Float) -> Unit)? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val paper = palette.cardSurfaceStrong
    val ink = palette.textPrimary
    val inkMuted = palette.textSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusCard))
            .background(paper)
            .border(1.dp, ink.copy(alpha = 0.10f), RoundedCornerShape(VennelaTokens.radiusCard))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = fileName,
            color = ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = inkMuted.copy(alpha = 0.85f),
            fontSize = VennelaTokens.nano,
            modifier = Modifier.padding(top = VennelaTokens.spaceXxs)
        )
        Spacer(Modifier.height(VennelaTokens.spaceMd))
        Box(
            Modifier.fillMaxWidth()
                .height(1.dp)
                .background(ink.copy(alpha = 0.08f))
        )
        Spacer(Modifier.height(VennelaTokens.spaceMd))
        Text(
            text = body,
            color = ink,
            fontSize = fontSize,
            lineHeight = lineHeight,
            onTextLayout = { layout -> onBodyLayout?.invoke(layout) },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords -> onBodyPositioned?.invoke(coords.positionInRoot().y) }
        )
        if (truncated) {
            Spacer(Modifier.height(VennelaTokens.spaceLg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(VennelaTokens.iconSm)
                )
                Spacer(Modifier.width(VennelaTokens.spaceSm))
                Text(
                    text = "Preview truncated for memory safety — the original file is unchanged.",
                    color = inkMuted,
                    fontSize = VennelaTokens.micro
                )
            }
        }
    }
}
