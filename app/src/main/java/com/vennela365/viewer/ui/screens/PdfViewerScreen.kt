package com.vennela365.viewer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.applyViewerColorMode
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.HyperGutterRadar
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.OkenScanFilter
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.SuiteStyleSwitcherStrip
import com.vennela365.viewer.ui.components.ViewerGlassSheet
import com.vennela365.viewer.ui.components.ViewerKind
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.ViewerTopBar
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.WpsPaperTone
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.rememberSuiteSessionState
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PdfMinZoom = 0.5f
private const val PdfMaxZoom = 6f
private const val PdfMaxRenderDimension = 2048
private const val PdfThumbPixelWidth = 132
private const val PdfMaxFindHits = 240
private const val PdfMaxFindRows = 30
private const val PdfMaxOutlineRows = 400
private const val PdfMaxTextProbe = 200_000

private data class RenderedPdf(
    val pageCount: Int = 0,
    val bitmap: Bitmap? = null,
    val pageWidth: Int = 0,
    val pageHeight: Int = 0,
    val error: String? = null
)

/** One resolved quick-find result: either a real page or a text-layer snippet. */
private data class PdfFindHit(val order: Int, val pageIndex: Int?, val title: String, val snippet: String)

/** A chip rail entry, so rails stay data-driven instead of hand-unrolled. */
private data class PdfChip(
    val label: String,
    val icon: ImageVector? = null,
    val active: Boolean = false,
    val onClick: () -> Unit
)

/** Which pro panel the dock currently shows; one tray host instead of five. */
private enum class PdfPanel(val label: String) {
    NONE("none"), JUMP("Jump to page"), ZOOM("Zoom"), VIEW("Tools"), INFO("File information")
}

/**
 * Document-level parse cache. Page counts, continuous-scroll bitmaps, and rendered
 * thumbnails survive the screen leaving composition, so reopening the same PDF paints
 * its real page indicator and thumbnails on the first frame instead of re-rendering.
 */
internal object PdfProCache {
    private const val MaxThumbs = 32
    private const val MaxPages = 10
    private val pageCounts = ConcurrentHashMap<String, Int>()
    private val thumbs = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>) = size > MaxThumbs
    }
    private val pages = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>) = size > MaxPages
    }

    fun pageCount(fileKey: String): Int = pageCounts[fileKey] ?: 0
    fun putPageCount(fileKey: String, count: Int) { if (count > 0) pageCounts[fileKey] = count }
    @Synchronized fun thumb(key: String): Bitmap? = thumbs[key]?.takeIf { !it.isRecycled }
    @Synchronized fun putThumb(key: String, bitmap: Bitmap) { thumbs[key] = bitmap }
    @Synchronized fun page(key: String): Bitmap? = pages[key]?.takeIf { !it.isRecycled }
    @Synchronized fun putPage(key: String, bitmap: Bitmap) { pages[key] = bitmap }
}

/** Per-file reading position, so reopening a PDF lands where you left it. */
private object PdfReadingMemory {
    private const val MaxFiles = 12
    private val pages = object : LinkedHashMap<String, Int>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>) = size > MaxFiles
    }
    private val zooms = object : LinkedHashMap<String, Float>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Float>) = size > MaxFiles
    }

    @Synchronized fun page(fileKey: String): Int = pages[fileKey] ?: 0
    @Synchronized fun putPage(fileKey: String, value: Int) { pages[fileKey] = value }
    @Synchronized fun zoom(fileKey: String): Float = zooms[fileKey] ?: 1f
    @Synchronized fun putZoom(fileKey: String, value: Float) { zooms[fileKey] = value }
}

private fun pdfHostActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Real window-level keep-screen-on flag, always cleared when the screen leaves. */
@Composable
private fun PdfKeepScreenFlag(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        val window = pdfHostActivity(context)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/**
 * Streams a local PDF copy to the print spooler. The print framework needs a
 * real file, so [PdfViewerScreen] materialises one before starting the job.
 */
private class PdfPrintAdapter(private val source: File, private val jobName: String) :
    PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) return callback?.onLayoutCancelled() ?: Unit
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) return callback?.onWriteFailed("No print destination") ?: Unit
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback?.onWriteFailed(error.message ?: "Could not write the PDF to the spooler")
        }
    }
}

/**
 * Pro PDF Studio & Universal PDF Viewer.
 *
 * Supports 4 real viewing modes (Single Page, Continuous Vertical Scroll, 2-Page Spread,
 * and Apple Books-style Reflow Reader), Live Vector Ink & Highlighter Markup with flattened
 * multi-page PDF export, Offline Neural TTS Read-Aloud, Page Bookmarking, Quick Find,
 * Page Grid, Print Spooling, and a 1-tap built-in 5-Page Showcase PDF generator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val extractedText by viewModel.documentText.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val activeSuite by viewModel.preferredSuiteStyle.collectAsState()
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val suiteSession = rememberSuiteSessionState(activeSuite, palette.isDark)
    val scope = rememberCoroutineScope()
    val pageInsetPx = with(LocalDensity.current) { VennelaTokens.spaceSm.toPx() }
    val fileKey = selectedFile?.id ?: "no-pdf"

    // Reading position memory — restored per file id.
    var page by rememberSaveable(fileKey) { mutableStateOf(PdfReadingMemory.page(fileKey)) }
    var reloadKey by remember(fileKey) { mutableIntStateOf(0) }

    // Pro PDF Studio state: view mode, markup studio, voice reader, bookmarks
    var viewMode by rememberSaveable(fileKey) { mutableStateOf(PdfViewMode.SINGLE_PAGE) }
    var showMarkupBar by rememberSaveable(fileKey) { mutableStateOf(false) }
    var isHighlighter by rememberSaveable { mutableStateOf(false) }
    var activeInkColorIndex by rememberSaveable { mutableIntStateOf(0) }
    var strokeScale by rememberSaveable { mutableFloatStateOf(1.0f) }
    var annotationEpoch by remember { mutableIntStateOf(0) }
    var showTtsBar by rememberSaveable(fileKey) { mutableStateOf(false) }

    val activeInkColor = PdfMarkupColors.getOrElse(activeInkColorIndex) { PdfMarkupColors.first() }
    val currentPageStrokes = remember(fileKey, page, annotationEpoch) {
        PdfAnnotationMemory.strokes(fileKey, page)
    }
    val isCurrentPageBookmarked = remember(fileKey, page, annotationEpoch) {
        PdfBookmarkMemory.isBookmarked(fileKey, page)
    }
    val bookmarkedPages = remember(fileKey, annotationEpoch) {
        PdfBookmarkMemory.bookmarkedPages(fileKey)
    }

    var showThumbRail by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf(PdfPanel.NONE) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showGridSheet by remember { mutableStateOf(false) }
    var rotationDegrees by rememberSaveable(fileKey) { mutableIntStateOf(0) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees.toFloat(),
        animationSpec = LiquidMotion.liquid(),
        label = "pdfRotation"
    )
    var jumpPageInput by remember { mutableStateOf("") }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findIndex by remember { mutableIntStateOf(0) }
    var keepScreenOn by rememberSaveable { mutableStateOf(false) }
    var viewportWidth by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableFloatStateOf(0f) }

    PdfKeepScreenFlag(keepScreenOn)

    val rendered by produceState(
        initialValue = RenderedPdf(pageCount = PdfProCache.pageCount(fileKey)),
        fileKey, selectedFile?.uri, page, reloadKey
    ) {
        val uri = selectedFile?.uri
        if (uri == null) {
            value = RenderedPdf(error = "No file selected")
            return@produceState
        }
        val oldBitmap = value.bitmap
        value = RenderedPdf(pageCount = value.pageCount, bitmap = null)
        oldBitmap?.recycle()
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (renderer.pageCount == 0) {
                            RenderedPdf(error = "This document has no pages")
                        } else {
                            PdfProCache.putPageCount(fileKey, renderer.pageCount)
                            renderer.openPage(page.coerceIn(0, renderer.pageCount - 1)).use { pdfPage ->
                                val factor = minOf(
                                    PdfMaxRenderDimension.toFloat() / pdfPage.width.coerceAtLeast(1),
                                    PdfMaxRenderDimension.toFloat() / pdfPage.height.coerceAtLeast(1),
                                    2f
                                )
                                val width = (pdfPage.width * factor).toInt().coerceAtLeast(1)
                                val height = (pdfPage.height * factor).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                RenderedPdf(renderer.pageCount, bitmap, width, height)
                            }
                        }
                    }
                } ?: RenderedPdf(error = "This file cannot be opened")
            }.getOrElse { error -> RenderedPdf(error = error.message ?: "This PDF could not be rendered") }
        }
    }

    var scale by remember(fileKey) { mutableFloatStateOf(PdfReadingMemory.zoom(fileKey)) }
    var offset by remember(fileKey) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        if (!showMarkupBar) {
            scale = (scale * zoomChange).coerceIn(PdfMinZoom, PdfMaxZoom)
            offset = if (scale <= 1.01f) Offset.Zero else offset + panChange
        }
    }

    val dragX = remember(fileKey) { Animatable(0f) }
    val pagingEnabled = scale <= 1.01f && !showMarkupBar && viewMode == PdfViewMode.SINGLE_PAGE
    val stripState = rememberLazyListState()
    val thumbCache = remember(fileKey) { ConcurrentHashMap<Int, Bitmap>() }

    LaunchedEffect(page, rendered.pageCount) {
        if (rendered.pageCount > 0 && page in 0 until rendered.pageCount) {
            stripState.animateScrollToItem(page.coerceAtLeast(0))
        }
    }
    LaunchedEffect(fileKey, page) { PdfReadingMemory.putPage(fileKey, page) }
    LaunchedEffect(fileKey, scale) { PdfReadingMemory.putZoom(fileKey, scale) }

    val pageAspect = if (rendered.pageHeight > 0) rendered.pageWidth.toFloat() / rendered.pageHeight else 0f
    val innerWidth = (viewportWidth - pageInsetPx * 2f).coerceAtLeast(1f)
    val innerHeight = (viewportHeight - pageInsetPx * 2f).coerceAtLeast(1f)
    val fittedWidth = if (pageAspect > 0f) minOf(innerWidth, innerHeight * pageAspect) else innerWidth
    val fitWidthScale = if (fittedWidth > 1f) (innerWidth / fittedWidth).coerceIn(PdfMinZoom, PdfMaxZoom) else 1f

    fun goToPage(target: Int, resetZoom: Boolean) {
        if (rendered.pageCount <= 0) return
        page = target.coerceIn(0, rendered.pageCount - 1)
        if (resetZoom) {
            scale = 1f
            offset = Offset.Zero
            scope.launch { dragX.snapTo(0f) }
        }
    }

    fun goTo(target: Int) = goToPage(target, resetZoom = true)

    fun animateZoom(target: Float) {
        val from = scale
        if (target <= 1.01f) offset = Offset.Zero
        scope.launch {
            animate(
                initialValue = from,
                targetValue = target.coerceIn(PdfMinZoom, PdfMaxZoom),
                animationSpec = LiquidMotion.liquid()
            ) { value, _ -> scale = value }
        }
    }

    val isShowcasePdf = selectedFile?.name?.contains("Vennela_365_Pro_Studio_Guide", ignoreCase = true) == true
    val pageTextsForReflow: List<String> = remember(isShowcasePdf, extractedText, rendered.pageCount) {
        if (isShowcasePdf) {
            ShowcasePdfPageTexts
        } else if (extractedText.isNotBlank() && rendered.pageCount > 0) {
            val chunkSize = (extractedText.length / rendered.pageCount.coerceAtLeast(1)).coerceAtLeast(200)
            extractedText.chunked(chunkSize)
        } else {
            emptyList()
        }
    }

    val searchableText = remember(extractedText, isShowcasePdf) {
        if (isShowcasePdf) {
            ShowcasePdfPageTexts.joinToString("\n\n")
        } else {
            val candidate = extractedText
            if (candidate.length < 24) {
                ""
            } else {
                val readable = candidate.count { it.isLetterOrDigit() || it.isWhitespace() }
                if (readable.toFloat() / candidate.length >= 0.90f) candidate.take(PdfMaxTextProbe) else ""
            }
        }
    }

    val findHits = remember(findQuery, rendered.pageCount, searchableText) {
        val query = findQuery.trim()
        if (query.isEmpty() || rendered.pageCount <= 0) {
            emptyList()
        } else {
            val hits = ArrayList<PdfFindHit>()
            for (index in 0 until rendered.pageCount) {
                if (hits.size >= PdfMaxFindHits) break
                if ((index + 1).toString().contains(query, ignoreCase = true)) {
                    hits.add(
                        PdfFindHit(
                            hits.size, index, "Page ${index + 1}",
                            "Jump to page ${index + 1} of ${rendered.pageCount}"
                        )
                    )
                }
            }
            if (searchableText.isNotEmpty()) {
                var cursor = searchableText.indexOf(query, 0, ignoreCase = true)
                var guard = 0
                while (cursor >= 0 && guard < 80 && hits.size < PdfMaxFindHits) {
                    val start = (cursor - 34).coerceAtLeast(0)
                    val end = (cursor + query.length + 44).coerceAtMost(searchableText.length)
                    hits.add(
                        PdfFindHit(
                            hits.size, null, "Text layer match",
                            searchableText.substring(start, end).replace('\n', ' ').trim()
                        )
                    )
                    cursor = searchableText.indexOf(query, cursor + query.length.coerceAtLeast(1), true)
                    guard++
                }
            }
            hits
        }
    }
    val safeFindIndex = if (findHits.isEmpty()) 0 else findIndex.coerceIn(0, findHits.lastIndex)
    val activeHit = findHits.getOrNull(safeFindIndex)

    LaunchedEffect(activeHit, showFindBar) {
        val target = activeHit?.pageIndex
        if (showFindBar && target != null && target != page) goToPage(target, resetZoom = false)
    }

    val mime = selectedFile?.formatInfo?.mimeType?.takeIf { it.isNotBlank() } ?: "application/pdf"

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun launchShowcasePdf() {
        scope.launch {
            val generated = withContext(Dispatchers.IO) {
                runCatching { generateShowcasePdf(context) }.getOrNull()
            }
            if (generated != null && generated.exists()) {
                viewModel.openUri(Uri.fromFile(generated), generated.name)
            } else {
                toast("Could not generate showcase PDF")
            }
        }
    }

    fun exportAndShareAnnotatedPdf() {
        val uri = selectedFile?.uri ?: return toast("No PDF open to export")
        scope.launch {
            toast("Flattening annotations into PDF…")
            val exported = withContext(Dispatchers.IO) {
                exportAnnotatedPdf(
                    context = context,
                    sourceUri = uri,
                    fileKey = fileKey,
                    baseFileName = selectedFile?.name ?: "document.pdf"
                )
            }
            if (exported == null || !exported.exists()) {
                return@launch toast("Could not export annotated PDF")
            }
            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf" }
            if (ShareUri.attachStream(context, intent, Uri.fromFile(exported))) {
                runCatching {
                    context.startActivity(Intent.createChooser(intent, "Share ${exported.name}"))
                }.onFailure {
                    viewModel.openUri(Uri.fromFile(exported), exported.name)
                }
            } else {
                viewModel.openUri(Uri.fromFile(exported), exported.name)
            }
        }
    }

    fun shareFile() {
        val uri = selectedFile?.uri ?: return toast("There is no file to share")
        val intent = Intent(Intent.ACTION_SEND).apply { type = mime }
        if (!ShareUri.attachStream(context, intent, uri)) {
            return toast("This file's location cannot be handed to another app")
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share ${selectedFile?.name ?: "PDF"}"))
        }.onFailure { toast("No app accepted this file") }
    }

    fun openWith() {
        val intent = ShareUri.viewIntent(context, selectedFile?.uri, mime)
            ?: return toast("This file's location cannot be handed to another app")
        runCatching { context.startActivity(Intent.createChooser(intent, "Open with")) }
            .onFailure { toast("No app on this device can open application/pdf") }
    }

    fun shareCurrentPage() {
        val bitmap = rendered.bitmap ?: return
        try {
            val file = File(context.cacheDir, "vennela_pdf_page_${page + 1}.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png" }
            if (!ShareUri.attachStream(context, intent, Uri.fromFile(file))) {
                return toast("Could not export this page")
            }
            context.startActivity(Intent.createChooser(intent, "Share page ${page + 1}"))
        } catch (e: Exception) {
            toast("Could not export page: ${e.message}")
        }
    }

    fun printDocument() {
        val uri = selectedFile?.uri ?: return toast("There is no file to print")
        val jobName = selectedFile?.name ?: "Vennela document"
        scope.launch {
            val local = withContext(Dispatchers.IO) {
                runCatching {
                    val safeName = jobName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(48)
                    File(context.cacheDir, "vennela_print_$safeName.pdf").also { target ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }.getOrNull()
            }
            if (local == null || !local.exists() || local.length() == 0L) {
                return@launch toast("Could not prepare this PDF for printing")
            }
            val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return@launch toast("Printing is not available on this device")
            runCatching {
                manager.print(jobName, PdfPrintAdapter(local, jobName), PrintAttributes.Builder().build())
            }.onFailure { toast("Could not start the print job") }
        }
    }

    fun jumpToInput() {
        val target = jumpPageInput.toIntOrNull()
        if (target != null && target in 1..rendered.pageCount) {
            goTo(target - 1)
            jumpPageInput = ""
        } else {
            toast("Enter a page between 1 and ${rendered.pageCount}")
        }
    }

    val zoomChips = listOf(
        PdfChip("Fit width", Icons.Default.AspectRatio, abs(scale - fitWidthScale) < 0.02f) { animateZoom(fitWidthScale) },
        PdfChip("Fit page", Icons.Default.FitScreen, abs(scale - 1f) < 0.02f) { animateZoom(1f) },
        PdfChip("50%", active = abs(scale - 0.5f) < 0.02f) { animateZoom(0.5f) },
        PdfChip("100%", active = abs(scale - 1f) < 0.02f) { animateZoom(1f) },
        PdfChip("200%", active = abs(scale - 2f) < 0.02f) { animateZoom(2f) },
        PdfChip("Zoom out", Icons.Default.ZoomOut) { animateZoom(scale - 0.25f) },
        PdfChip("Zoom in", Icons.Default.ZoomIn) { animateZoom(scale + 0.25f) }
    )
    val toolChips = listOf(
        PdfChip("Markup Studio", Icons.Default.Edit, showMarkupBar) {
            showMarkupBar = !showMarkupBar
            if (showMarkupBar) viewMode = PdfViewMode.SINGLE_PAGE
        },
        PdfChip("Voice Read-Aloud", Icons.AutoMirrored.Filled.VolumeUp, showTtsBar) {
            showTtsBar = !showTtsBar
        },
        PdfChip("Bookmark Page", Icons.Default.Bookmark, isCurrentPageBookmarked) {
            PdfBookmarkMemory.toggle(fileKey, page)
            annotationEpoch++
        },
        PdfChip("Page grid", Icons.Default.GridView) { showGridSheet = true },
        PdfChip("Rotate 90°", Icons.AutoMirrored.Filled.RotateRight, rotationDegrees % 360 != 0) {
            rotationDegrees += 90
        },
        PdfChip("Outline & Bookmarks", Icons.AutoMirrored.Filled.ListAlt) { showTocSheet = true },
        PdfChip("Save Annotated PDF", Icons.Default.Share) { exportAndShareAnnotatedPdf() },
        PdfChip("Full search screen", Icons.Default.Search) { viewModel.navigateTo(Screen.PdfSearch) },
        PdfChip("Thumbnails", Icons.Default.GridView, showThumbRail) { showThumbRail = !showThumbRail },
        PdfChip("Jump to page", Icons.AutoMirrored.Filled.ListAlt, panel == PdfPanel.JUMP) { panel = PdfPanel.JUMP },
        PdfChip("File info", Icons.Default.Info, panel == PdfPanel.INFO) { panel = PdfPanel.INFO },
        PdfChip("Keep screen on", Icons.Default.Visibility, keepScreenOn) { keepScreenOn = !keepScreenOn },
        PdfChip("Share file", Icons.Default.Share) { shareFile() },
        PdfChip("Share this page", Icons.Default.Share) { shareCurrentPage() },
        PdfChip("Open with", Icons.AutoMirrored.Filled.OpenInNew) { openWith() },
        PdfChip("Print", Icons.Default.Print) { printDocument() },
        PdfChip("Load Showcase PDF", Icons.Default.AutoAwesome) { launchShowcasePdf() },
        PdfChip("Metadata screen", Icons.Default.Info) { viewModel.navigateTo(Screen.FileMetadata) }
    )
    val annotatedPageCount = remember(fileKey, rendered.pageCount, annotationEpoch) {
        PdfAnnotationMemory.totalAnnotatedPages(fileKey, rendered.pageCount)
    }
    val infoRows = listOf(
        "Name" to (selectedFile?.name ?: "No file open"),
        "Size" to (selectedFile?.formattedSize ?: "Unknown"),
        "Modified" to (selectedFile?.formattedDate ?: "Unknown"),
        "Format" to (selectedFile?.formatInfo?.name ?: "Unknown"),
        "Extension" to (selectedFile?.extension?.uppercase()?.ifBlank { "none" } ?: "Unknown"),
        "Pages" to if (rendered.pageCount > 0) "${rendered.pageCount} pages" else "Still reading the page tree…",
        "Page box" to if (rendered.pageWidth > 0) "${rendered.pageWidth} × ${rendered.pageHeight} px" else "Not rendered yet",
        "View mode" to "${viewMode.label} • ${viewerColorMode.title}",
        "Bookmarks & Markup" to "${bookmarkedPages.size} bookmarked • $annotatedPageCount annotated",
        "Reading position" to "Page ${page + 1} at ${(scale * 100).roundToInt()}%"
    )

    LiquidPage(
        modifier = Modifier.fillMaxSize(),
        title = null,
        dock = {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
            ) {
                // Live Voice Read-Aloud TTS bar
                AnimatedVisibility(
                    visible = showTtsBar && rendered.pageCount > 0,
                    enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.liquid()),
                    exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit())
                ) {
                    val ttsText = pageTextsForReflow.getOrNull(page)
                        ?.takeIf { it.isNotBlank() }
                        ?: searchableText.ifBlank {
                            "Vennela 365 PDF Studio. Page ${page + 1} of ${rendered.pageCount} in ${selectedFile?.name ?: "document"}."
                        }
                    PdfTtsControllerBar(
                        textToSpeak = ttsText,
                        pageLabel = "Page ${page + 1} of ${rendered.pageCount}",
                        onClose = { showTtsBar = false }
                    )
                }

                // Live iOS 26 Markup Studio bar
                AnimatedVisibility(
                    visible = showMarkupBar && rendered.pageCount > 0,
                    enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.liquid()),
                    exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit())
                ) {
                    PdfMarkupToolbar(
                        isHighlighter = isHighlighter,
                        onSelectTool = { isHighlighter = it },
                        activeColor = activeInkColor,
                        onSelectColor = { chosen ->
                            val idx = PdfMarkupColors.indexOfFirst { it == chosen }
                            if (idx >= 0) activeInkColorIndex = idx
                        },
                        strokeScale = strokeScale,
                        onCycleStrokeScale = {
                            strokeScale = when {
                                strokeScale <= 0.8f -> 1.0f
                                strokeScale <= 1.2f -> 1.6f
                                else -> 0.7f
                            }
                        },
                        canUndo = currentPageStrokes.isNotEmpty(),
                        onUndo = {
                            if (PdfAnnotationMemory.undo(fileKey, page)) annotationEpoch++
                        },
                        canRedo = true,
                        onRedo = {
                            if (PdfAnnotationMemory.redo(fileKey, page)) annotationEpoch++
                        },
                        onClearPage = {
                            PdfAnnotationMemory.clearPage(fileKey, page)
                            annotationEpoch++
                        },
                        onExportAnnotatedPdf = { exportAndShareAnnotatedPdf() },
                        onClose = { showMarkupBar = false }
                    )
                }

                PdfTray(
                    visible = showThumbRail && rendered.pageCount > 0 && !showMarkupBar && viewMode == PdfViewMode.SINGLE_PAGE,
                    innerPadding = PaddingValues(vertical = VennelaTokens.spaceSm)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.spaceMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pages", color = palette.textPrimary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = VennelaTokens.caption, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Page ${page + 1} of ${rendered.pageCount}",
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = VennelaTokens.micro
                        )
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    LazyRow(
                        state = stripState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = VennelaTokens.spaceMd),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                    ) {
                        items(count = rendered.pageCount, key = { index -> "thumb_$index" }) { index ->
                            PdfPageThumbnail(
                                uri = selectedFile?.uri,
                                fileKey = fileKey,
                                pageIndex = index,
                                cache = thumbCache,
                                active = index == page
                            ) { goTo(index) }
                        }
                    }
                }

                // One tray host for every pro panel: jump, zoom, tools, info.
                PdfTray(visible = panel != PdfPanel.NONE) {
                    when (panel) {
                        PdfPanel.JUMP -> {
                            TrayHeader(
                                Icons.AutoMirrored.Filled.ListAlt, "Jump to page",
                                "${page + 1} / ${rendered.pageCount}"
                            )
                            Slider(
                                value = page.toFloat()
                                    .coerceIn(0f, (rendered.pageCount - 1).coerceAtLeast(0).toFloat()),
                                onValueChange = { goToPage(it.roundToInt(), resetZoom = false) },
                                valueRange = 0f..(rendered.pageCount - 1).coerceAtLeast(1).toFloat(),
                                steps = (rendered.pageCount - 2).coerceIn(0, 60),
                                colors = SliderDefaults.colors(
                                    thumbColor = accent.secondary,
                                    activeTrackColor = accent.secondary,
                                    inactiveTrackColor = palette.cardBorder
                                ),
                                modifier = Modifier.fillMaxWidth().heightIn(min = VennelaTokens.touchTargetMin)
                            )
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                    .background(palette.cardSurface.copy(alpha = 0.55f))
                                    .border(
                                        1.dp, accent.secondary.copy(alpha = 0.4f),
                                        RoundedCornerShape(VennelaTokens.radiusPill)
                                    )
                                    .padding(horizontal = VennelaTokens.spaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = jumpPageInput,
                                    onValueChange = { input ->
                                        jumpPageInput = input.filter { it.isDigit() }.take(5)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        color = palette.textPrimary,
                                        fontSize = VennelaTokens.bodySmall
                                    ),
                                    cursorBrush = SolidColor(accent.secondary),
                                    modifier = Modifier.weight(1f).heightIn(min = VennelaTokens.touchTargetMin),
                                    decorationBox = { inner ->
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                            if (jumpPageInput.isEmpty()) {
                                                Text(
                                                    "Page 1–${rendered.pageCount}",
                                                    color = palette.textTertiary,
                                                    fontSize = VennelaTokens.bodySmall
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                                Text(
                                    "Go",
                                    color = accent.secondary,
                                    fontSize = VennelaTokens.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .heightIn(min = VennelaTokens.touchTargetMin)
                                        .pressScale(pressedScale = 0.92f, onClick = { jumpToInput() })
                                        .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd)
                                )
                            }
                        }

                        PdfPanel.ZOOM -> {
                            TrayHeader(
                                Icons.Default.ZoomIn, "Zoom",
                                "${(scale * 100).roundToInt()}% • fit page is 100%"
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            ChipRail(zoomChips)
                        }

                        PdfPanel.VIEW -> {
                            Text(
                                "Layout mode — ${viewMode.label}",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            ChipRail(
                                PdfViewMode.entries.map { mode ->
                                    PdfChip(mode.label, mode.icon, viewMode == mode) {
                                        viewMode = mode
                                    }
                                }
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            Text(
                                "Display filter — ${viewerColorMode.title}",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            ChipRail(
                                PdfDisplayMode.entries.map { mode ->
                                    PdfChip(mode.label, mode.icon, viewerColorMode == mode.mode) {
                                        viewModel.setViewerColorMode(mode.mode)
                                    }
                                }
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            ChipRail(toolChips)
                        }

                        PdfPanel.INFO -> {
                            TrayHeader(Icons.Default.Info, "File information", "real values only")
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

                        PdfPanel.NONE -> Unit
                    }
                }

                val dockSpec = liquidGlassSpec(
                    alpha = 0.32f,
                    frost = VennelaTokens.frostHeavy,
                    elevation = VennelaTokens.elevationDock,
                    strong = true
                )
                Row(
                    Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                        .liquidGlass(dockSpec, RoundedCornerShape(VennelaTokens.radiusDock))
                        .padding(horizontal = VennelaTokens.spaceXs, vertical = VennelaTokens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
                ) {
                    GlassRoundAction(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page",
                        enabled = page > 0,
                        modifier = Modifier.testTag("pdf_prev_button")
                    ) { goTo(page - 1) }

                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PageIndicatorPill(
                            label = "${page + 1} / ${rendered.pageCount.coerceAtLeast(1)}",
                            modifier = Modifier.testTag("pdf_page_pill")
                        ) {
                            panel = if (panel == PdfPanel.JUMP) PdfPanel.NONE else PdfPanel.JUMP
                        }
                    }

                    GlassRoundAction(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page",
                        enabled = page < rendered.pageCount - 1,
                        modifier = Modifier.testTag("pdf_next_button")
                    ) { goTo(page + 1) }

                    GlassRoundAction(
                        Icons.Default.Edit, "Markup studio",
                        active = showMarkupBar,
                        modifier = Modifier.testTag("pdf_markup_button")
                    ) {
                        showMarkupBar = !showMarkupBar
                        if (showMarkupBar) viewMode = PdfViewMode.SINGLE_PAGE
                    }

                    GlassRoundAction(
                        Icons.Default.Search, "Quick find",
                        active = showFindBar,
                        modifier = Modifier.testTag("pdf_find_button")
                    ) {
                        showFindBar = !showFindBar
                        if (!showFindBar) findQuery = ""
                    }
                    GlassRoundAction(
                        Icons.Default.ZoomIn, "Zoom controls",
                        active = panel == PdfPanel.ZOOM,
                        modifier = Modifier.testTag("pdf_zoom_button")
                    ) { panel = if (panel == PdfPanel.ZOOM) PdfPanel.NONE else PdfPanel.ZOOM }
                    GlassRoundAction(
                        Icons.Default.MoreHoriz, "More tools",
                        active = panel == PdfPanel.VIEW || panel == PdfPanel.INFO,
                        modifier = Modifier.testTag("pdf_more_button")
                    ) { panel = if (panel == PdfPanel.VIEW) PdfPanel.NONE else PdfPanel.VIEW }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            ViewerTopBar(
                title = selectedFile?.name ?: "Pro PDF Studio",
                subtitle = buildString {
                    append(viewMode.label)
                    if (rendered.pageCount > 0) append(" • ${rendered.pageCount} pages")
                    if (viewMode == PdfViewMode.SINGLE_PAGE) append(" • ${(scale * 100).roundToInt()}%")
                },
                onBack = { viewModel.navigateBack() },
                modifier = Modifier,
                progress = if (rendered.pageCount > 0) {
                    (page + 1).toFloat() / rendered.pageCount.toFloat()
                } else {
                    null
                }
            ) {
                if (rendered.pageCount > 0) {
                    ReaderIconButton(
                        icon = viewMode.icon,
                        contentDescription = "Cycle layout mode (${viewMode.label})",
                        active = viewMode != PdfViewMode.SINGLE_PAGE,
                        modifier = Modifier.testTag("pdf_view_mode_button"),
                        onClick = {
                            val modes = PdfViewMode.entries
                            viewMode = modes[(modes.indexOf(viewMode) + 1) % modes.size]
                        }
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isCurrentPageBookmarked) "Remove bookmark" else "Bookmark page",
                        active = isCurrentPageBookmarked,
                        modifier = Modifier.testTag("pdf_bookmark_button"),
                        onClick = {
                            PdfBookmarkMemory.toggle(fileKey, page)
                            annotationEpoch++
                        }
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.GridView,
                        contentDescription = "Page grid overview",
                        active = showGridSheet,
                        modifier = Modifier.testTag("pdf_grid_button"),
                        onClick = { showGridSheet = true }
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate page 90 degrees",
                        active = rotationDegrees % 360 != 0,
                        modifier = Modifier.testTag("pdf_rotate_button"),
                        onClick = { rotationDegrees += 90 }
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceXs))
                }
                ReaderIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = if (showFindBar) "Close quick find" else "Quick find in PDF",
                    active = showFindBar,
                    onClick = {
                        showFindBar = !showFindBar
                        if (!showFindBar) findQuery = ""
                    }
                )
                Spacer(Modifier.width(VennelaTokens.spaceXs))
                ReaderIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = "File details",
                    onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                )
            }

            val pdfColorFilter: ColorFilter? = remember(activeSuite, suiteSession.scanFilter, suiteSession.paperTone) {
                when {
                    activeSuite == ViewerSuiteStyle.OKEN_SCANNER -> suiteSession.scanFilter.toColorFilter()
                    activeSuite == ViewerSuiteStyle.WPS_SUPER && suiteSession.paperTone == WpsPaperTone.EYE_CARE_GREEN -> {
                        // Gentle WPS Eye-Care Green paper matrix tint
                        ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    0.78f, 0f, 0f, 0f, 0f,
                                    0f, 0.93f, 0f, 0f, 0f,
                                    0f, 0f, 0.80f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                    }
                    else -> null
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(top = VennelaTokens.viewerTopInset)
            ) {
                // Multi-Suite Switcher Strip + Active Suite Quick Controls for PDF
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VennelaTokens.spaceSm, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuiteStyleSwitcherStrip(
                        activeSuite = activeSuite,
                        onSelectSuite = { viewModel.setPreferredSuiteStyle(it) },
                        isDark = palette.isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VennelaTokens.spaceSm, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(OkenScanFilter.entries.size) { idx ->
                            val filter = OkenScanFilter.entries[idx]
                            LiquidChip(
                                text = filter.label,
                                isActive = suiteSession.scanFilter == filter,
                                onClick = { suiteSession.scanFilter = filter }
                            )
                        }
                        item {
                            LiquidChip(
                                text = "Auto-Crop",
                                isActive = suiteSession.showAutoCropHandles,
                                onClick = { suiteSession.showAutoCropHandles = !suiteSession.showAutoCropHandles }
                            )
                        }
                        item {
                            LiquidChip(
                                text = "Watermark",
                                isActive = suiteSession.showSecurityWatermark,
                                onClick = { suiteSession.showSecurityWatermark = !suiteSession.showSecurityWatermark }
                            )
                        }
                        item {
                            LiquidChip(
                                text = "Official Stamp",
                                isActive = suiteSession.showOfficialStamp,
                                onClick = { suiteSession.showOfficialStamp = !suiteSession.showOfficialStamp }
                            )
                        }
                    }
                } else if (activeSuite == ViewerSuiteStyle.WPS_SUPER) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VennelaTokens.spaceSm, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(WpsPaperTone.entries.size) { idx ->
                            val tone = WpsPaperTone.entries[idx]
                            LiquidChip(
                                text = tone.label,
                                isActive = suiteSession.paperTone == tone,
                                onClick = { suiteSession.paperTone = tone }
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = VennelaTokens.spaceXs)
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .applyViewerColorMode(viewerColorMode)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (activeSuite) {
                                    ViewerSuiteStyle.GOOGLE_WORKSPACE -> if (palette.isDark) Color(0xFF202124) else Color(0xFFF1F3F4)
                                    ViewerSuiteStyle.MICROSOFT_365 -> if (palette.isDark) Color(0xFF1B1A19) else Color(0xFFEDEBE9)
                                    ViewerSuiteStyle.WPS_SUPER -> if (suiteSession.paperTone == WpsPaperTone.EYE_CARE_GREEN) Color(0xFFDDF3E0) else palette.backgroundBottom
                                    ViewerSuiteStyle.LIBRE_OFFICE -> if (palette.isDark) Color(0xFF202225) else Color(0xFFDADCE0)
                                    ViewerSuiteStyle.OKEN_SCANNER -> Color(0xFF181C22)
                                }
                            )
                            .border(0.75.dp, ViewerKind.PDF.accentFor(activeSuite).copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                            .onSizeChanged { size ->
                                viewportWidth = size.width.toFloat()
                                viewportHeight = size.height.toFloat()
                            }
                        .pointerInput(page, pagingEnabled) {
                            if (!pagingEnabled) return@pointerInput
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    scope.launch {
                                        dragX.snapTo(
                                            (dragX.value + amount)
                                                .coerceIn(-size.width.toFloat(), size.width.toFloat())
                                        )
                                    }
                                },
                                onDragEnd = {
                                    val threshold = size.width * 0.22f
                                    when {
                                        dragX.value <= -threshold && page < rendered.pageCount - 1 -> goTo(page + 1)
                                        dragX.value >= threshold && page > 0 -> goTo(page - 1)
                                        else -> scope.launch { dragX.animateTo(0f, LiquidMotion.liquid()) }
                                    }
                                },
                                onDragCancel = { scope.launch { dragX.animateTo(0f, LiquidMotion.liquid()) } }
                            )
                        }
                        .pointerInput(showMarkupBar, viewMode) {
                            if (showMarkupBar || viewMode != PdfViewMode.SINGLE_PAGE) return@pointerInput
                            detectTapGestures(onDoubleTap = { animateZoom(if (scale > 1.01f) 1f else 2f) })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = rendered.bitmap
                    when {
                        selectedFile?.uri == null -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
                            modifier = Modifier.padding(VennelaTokens.spaceXl)
                        ) {
                            VennelaEmptyState(
                                title = "No PDF open",
                                hint = "Open a PDF from Files or Recents, or launch the built-in 5-Page Pro Showcase PDF to test Continuous Scroll, Live Vector Ink Markup, Reflow Reader, and Voice Read-Aloud.",
                                icon = Icons.Default.PictureAsPdf,
                                actionLabel = "Browse files",
                                onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                            )
                            LiquidButton(
                                label = "Launch 5-Page Pro Showcase PDF",
                                icon = Icons.Default.AutoAwesome,
                                onClick = { launchShowcasePdf() },
                                modifier = Modifier.testTag("pdf_launch_showcase_button")
                            )
                        }

                        rendered.error != null -> Box(Modifier.padding(VennelaTokens.spaceXl)) {
                            VennelaErrorState(
                                title = "This PDF could not be rendered",
                                message = rendered.error ?: "Unknown rendering error",
                                onRetry = { reloadKey++ },
                                onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                            )
                        }

                        rendered.pageCount > 0 && viewMode == PdfViewMode.CONTINUOUS -> {
                            PdfContinuousScrollView(
                                uri = selectedFile?.uri,
                                fileKey = fileKey,
                                pageCount = rendered.pageCount,
                                currentPage = page,
                                rotationDegrees = rotationDegrees,
                                colorFilter = pdfColorFilter,
                                annotationEpoch = annotationEpoch,
                                onActivePageChanged = { newPage ->
                                    if (newPage != page) page = newPage
                                },
                                onToggleBookmark = { idx ->
                                    PdfBookmarkMemory.toggle(fileKey, idx)
                                    annotationEpoch++
                                }
                            )
                        }

                        rendered.pageCount > 0 && viewMode == PdfViewMode.SPREAD -> {
                            PdfTwoPageSpreadView(
                                uri = selectedFile?.uri,
                                fileKey = fileKey,
                                pageCount = rendered.pageCount,
                                currentPage = page,
                                colorFilter = pdfColorFilter,
                                onSelectPage = { idx -> goTo(idx) }
                            )
                        }

                        rendered.pageCount > 0 && viewMode == PdfViewMode.REFLOW -> {
                            PdfReflowReaderView(
                                fileTitle = selectedFile?.name ?: "PDF Document",
                                pageCount = rendered.pageCount,
                                currentPage = page,
                                pageTexts = pageTextsForReflow,
                                onJumpToPage = { idx -> goTo(idx) }
                            )
                        }

                        bitmap != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(VennelaTokens.spaceSm)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        rotationZ = animatedRotation,
                                        translationX = offset.x + dragX.value,
                                        translationY = offset.y
                                    )
                                    .transformable(transformState)
                                    .drawWithContent {
                                        drawContent()
                                        if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER && suiteSession.showAutoCropHandles) {
                                            val turquoise = Color(0xFF00D2A0)
                                            val stroke = 2.5.dp.toPx()
                                            val pad = 12.dp.toPx()
                                            val corner = 22.dp.toPx()
                                            val w = size.width - pad * 2
                                            val h = size.height - pad * 2
                                            drawRect(
                                                color = turquoise.copy(alpha = 0.78f),
                                                topLeft = Offset(pad, pad),
                                                size = androidx.compose.ui.geometry.Size(w, h),
                                                style = Stroke(width = 1.5.dp.toPx())
                                            )
                                            drawLine(turquoise, Offset(pad, pad), Offset(pad + corner, pad), stroke)
                                            drawLine(turquoise, Offset(pad, pad), Offset(pad, pad + corner), stroke)
                                            drawLine(turquoise, Offset(pad + w, pad), Offset(pad + w - corner, pad), stroke)
                                            drawLine(turquoise, Offset(pad + w, pad), Offset(pad + w, pad + corner), stroke)
                                            drawLine(turquoise, Offset(pad, pad + h), Offset(pad + corner, pad + h), stroke)
                                            drawLine(turquoise, Offset(pad, pad + h), Offset(pad, pad + h - corner), stroke)
                                            drawLine(turquoise, Offset(pad + w, pad + h), Offset(pad + w - corner, pad + h), stroke)
                                            drawLine(turquoise, Offset(pad + w, pad + h), Offset(pad + w, pad + h - corner), stroke)
                                        }
                                        if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER && suiteSession.showOfficialStamp) {
                                            val stampRed = Color(0xFFD93025).copy(alpha = 0.76f)
                                            val center = Offset(size.width - 68.dp.toPx(), size.height - 68.dp.toPx())
                                            rotate(-14f, pivot = center) {
                                                drawCircle(stampRed, radius = 36.dp.toPx(), center = center, style = Stroke(width = 2.5.dp.toPx()))
                                                drawCircle(stampRed, radius = 31.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "PDF page ${page + 1}",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = pdfColorFilter,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Live Vector Ink & Highlighter Overlay aligned to the page
                                PdfAnnotationOverlay(
                                    strokes = currentPageStrokes,
                                    markupEnabled = showMarkupBar,
                                    activeColor = activeInkColor,
                                    isHighlighter = isHighlighter,
                                    strokeScale = strokeScale,
                                    onCommitStroke = { stroke ->
                                        PdfAnnotationMemory.addStroke(fileKey, page, stroke)
                                        annotationEpoch++
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (dragX.value != 0f) {
                                OverlayPill {
                                    Text(
                                        if (dragX.value < 0) "Next page →" else "← Previous page",
                                        color = palette.textPrimary,
                                        fontSize = VennelaTokens.micro,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (rendered.pageCount > 1 && !showMarkupBar) {
                                HyperGutterRadar(
                                    ticks = emptyList(),
                                    currentScrollProgress = page.toFloat() /
                                        (rendered.pageCount - 1).coerceAtLeast(1),
                                    onScrub = { fraction ->
                                        goToPage(
                                            (fraction * (rendered.pageCount - 1)).toInt()
                                                .coerceIn(0, rendered.pageCount - 1),
                                            resetZoom = false
                                        )
                                    },
                                    formatPageOrLineLabel = { fraction ->
                                        "Page " + ((fraction * (rendered.pageCount - 1)).toInt()
                                            .coerceIn(0, rendered.pageCount - 1) + 1)
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxSize()
                                )
                            }

                            if (showFindBar && activeHit != null) {
                                OverlayPill(top = true) {
                                    Text(
                                        "Match ${safeFindIndex + 1} of ${findHits.size} • ${activeHit.title}",
                                        color = palette.textPrimary,
                                        fontSize = VennelaTokens.micro,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        else -> PdfGlassSkeleton(Modifier.fillMaxSize().padding(VennelaTokens.spaceXl))
                    }

                    if (showFindBar) {
                        Column(
                            Modifier.align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = VennelaTokens.spaceSm),
                            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            PdfFindBar(
                                query = findQuery,
                                onQueryChange = { findQuery = it; findIndex = 0 },
                                current = if (findHits.isEmpty()) 0 else safeFindIndex + 1,
                                total = findHits.size,
                                onPrevious = {
                                    if (findHits.isNotEmpty()) {
                                        findIndex = (safeFindIndex - 1 + findHits.size) % findHits.size
                                    }
                                },
                                onNext = {
                                    if (findHits.isNotEmpty()) findIndex = (safeFindIndex + 1) % findHits.size
                                },
                                onClose = { showFindBar = false; findQuery = "" }
                            )
                            AnimatedVisibility(
                                visible = findQuery.isNotBlank(),
                                enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.glide()),
                                exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit())
                            ) {
                                PdfFindResultsPanel(
                                    hits = findHits,
                                    activeIndex = safeFindIndex,
                                    hasTextLayer = searchableText.isNotEmpty(),
                                    pageCount = rendered.pageCount,
                                    onSelect = { index -> findIndex = index },
                                    onDismiss = { showFindBar = false; findQuery = "" }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (showTocSheet) {
        ViewerGlassSheet(
            title = "Outline & Bookmarks",
            subtitle = "${rendered.pageCount} pages • ${bookmarkedPages.size} bookmarked",
            onDismiss = { showTocSheet = false }
        ) {
            ChipRail(
                listOf(
                    PdfChip("Jump to page", Icons.AutoMirrored.Filled.ListAlt) {
                        showTocSheet = false; panel = PdfPanel.JUMP
                    },
                    PdfChip("Quick find", Icons.Default.Search) {
                        showTocSheet = false; showFindBar = true
                    },
                    PdfChip("Bookmark Page ${page + 1}", Icons.Default.Bookmark, isCurrentPageBookmarked) {
                        PdfBookmarkMemory.toggle(fileKey, page)
                        annotationEpoch++
                    },
                    PdfChip("Full outline screen", Icons.Default.Info) {
                        showTocSheet = false; viewModel.navigateTo(Screen.PdfToc)
                    }
                )
            )
            if (bookmarkedPages.isNotEmpty()) {
                Text(
                    "Bookmarked Pages (${bookmarkedPages.size})",
                    color = accent.secondary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.Bold
                )
                ChipRail(
                    bookmarkedPages.map { bPage ->
                        PdfChip("★ Page ${bPage + 1}", Icons.Default.Bookmark, bPage == page) {
                            goTo(bPage)
                            showTocSheet = false
                        }
                    }
                )
            }
            if (rendered.pageCount == 0) {
                Text(
                    "The page tree has not been read yet, so there is nothing to list.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.bodySmall
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth().height(VennelaTokens.spaceHuge * 8)) {
                    items(
                        count = minOf(rendered.pageCount, PdfMaxOutlineRows),
                        key = { index -> "outline_$index" }
                    ) { index ->
                        val active = index == page
                        val bookmarked = index in bookmarkedPages
                        val hasInk = PdfAnnotationMemory.strokes(fileKey, index).isNotEmpty()
                        GlassListRow(
                            title = buildString {
                                append("Page ${index + 1}")
                                if (bookmarked) append(" ★")
                                if (hasInk) append(" • Annotated")
                            },
                            subtitle = if (active) "Currently open" else "Tap to jump to page ${index + 1}",
                            modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                            leading = {
                                Icon(
                                    if (bookmarked) Icons.Default.Bookmark else Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = if (active || bookmarked) accent.secondary else palette.textTertiary,
                                    modifier = Modifier.size(VennelaTokens.iconMd)
                                )
                            },
                            onClick = {
                                goTo(index)
                                showTocSheet = false
                            }
                        )
                        if (index < minOf(rendered.pageCount, PdfMaxOutlineRows) - 1) GlassDivider()
                    }
                }
            }
        }
    }

    if (showGridSheet && rendered.pageCount > 0) {
        ViewerGlassSheet(
            title = "All Pages Grid",
            subtitle = "${rendered.pageCount} pages • ${selectedFile?.name ?: "PDF"}",
            onDismiss = { showGridSheet = false }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = VennelaTokens.spaceHuge * 12),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
                contentPadding = PaddingValues(bottom = VennelaTokens.spaceLg)
            ) {
                items(
                    count = minOf(rendered.pageCount, PdfMaxOutlineRows),
                    key = { index -> "grid_$index" }
                ) { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXxs)
                    ) {
                        PdfPageThumbnail(
                            uri = selectedFile?.uri,
                            fileKey = fileKey,
                            pageIndex = index,
                            cache = thumbCache,
                            active = index == page,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                        ) {
                            goTo(index)
                            showGridSheet = false
                        }
                    }
                }
            }
        }
    }
}

/** The four reading modes this screen exposes, mapped onto the real engine. */
private enum class PdfDisplayMode(val label: String, val mode: ViewerColorMode, val icon: ImageVector) {
    NORMAL("Normal", ViewerColorMode.ORIGINAL, Icons.Default.Visibility),
    SEPIA("Sepia", ViewerColorMode.SEPIA, Icons.AutoMirrored.Filled.MenuBook),
    NIGHT("Night", ViewerColorMode.OKLAB_DARK, Icons.Default.DarkMode),
    HIGH_CONTRAST("High contrast", ViewerColorMode.HIGH_CONTRAST_INVERTED, Icons.Default.Contrast)
}

/** Frosted, animated tray shared by every pro-viewer panel on this screen. */
@Composable
private fun PdfTray(
    visible: Boolean,
    innerPadding: PaddingValues = PaddingValues(VennelaTokens.spaceMd),
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(LiquidMotion.glide()) + expandVertically(LiquidMotion.liquid()),
        exit = fadeOut(LiquidMotion.exit()) + shrinkVertically(LiquidMotion.exit())
    ) {
        GlassPanel(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(bottom = VennelaTokens.spaceSm),
            shape = RoundedCornerShape(VennelaTokens.radiusCard),
            elevation = VennelaTokens.elevationOverlay
        ) {
            Column(Modifier.padding(innerPadding), content = content)
        }
    }
}

/** Icon + title + trailing meta line used at the top of a tray. */
@Composable
private fun TrayHeader(icon: ImageVector, title: String, trailing: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm))
        Spacer(Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = title, color = palette.textPrimary,
            fontSize = VennelaTokens.titleSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(trailing, color = palette.textSecondary, fontSize = VennelaTokens.micro)
    }
}

/** Horizontally scrolling chip rail; never a fixed row that can clip. */
@Composable
private fun ChipRail(chips: List<PdfChip>) {
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

/** Small frosted pill that floats over the page bed. */
@Composable
private fun BoxScope.OverlayPill(top: Boolean = false, content: @Composable () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(if (top) VennelaTokens.spaceMd else VennelaTokens.spaceLg)
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(palette.cardSurfaceStrong.copy(alpha = 0.86f))
            .border(1.dp, accent.secondary.copy(alpha = 0.5f), RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceXs),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PdfFindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    current: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.48f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationOverlay
    )
    Row(
        Modifier.fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(
                start = VennelaTokens.spaceLg,
                end = VennelaTokens.spaceXs,
                top = VennelaTokens.spaceXxs,
                bottom = VennelaTokens.spaceXxs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            modifier = Modifier.weight(1f).heightIn(min = VennelaTokens.touchTargetMin),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Find page or text",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.bodySmall
                        )
                    }
                    inner()
                }
            }
        )
        if (query.isNotBlank()) {
            Text(
                text = if (total == 0) "No matches" else "$current of $total",
                color = if (total == 0) accent.primary else palette.textSecondary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        GlassRoundAction(Icons.Default.KeyboardArrowUp, "Previous match", total > 0, onClick = onPrevious)
        GlassRoundAction(Icons.Default.KeyboardArrowDown, "Next match", total > 0, onClick = onNext)
        GlassRoundAction(Icons.Default.Close, "Close quick find", onClick = onClose)
    }
}

@Composable
private fun PdfFindResultsPanel(
    hits: List<PdfFindHit>,
    activeIndex: Int,
    hasTextLayer: Boolean,
    pageCount: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val visible = hits.take(PdfMaxFindRows)

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        elevation = VennelaTokens.elevationOverlay
    ) {
        Column(Modifier.padding(vertical = VennelaTokens.spaceSm)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.spaceLg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Find results", color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasTextLayer) {
                            "${hits.size} matches in $pageCount page labels and the text layer"
                        } else {
                            "${hits.size} matches — no text layer in this PDF, page numbers only"
                        },
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.micro
                    )
                }
                Box(
                    Modifier.size(VennelaTokens.touchTargetMin)
                        .clip(CircleShape)
                        .pressScale(pressedScale = 0.88f, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close find results",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
            }
            if (visible.isEmpty()) {
                Text(
                    "Nothing in this document matches that query.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.bodySmall,
                    modifier = Modifier.padding(
                        horizontal = VennelaTokens.spaceLg,
                        vertical = VennelaTokens.spaceMd
                    )
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth().height(VennelaTokens.spaceHuge * 4)) {
                    items(count = visible.size, key = { index -> "hit_${visible[index].order}" }) { index ->
                        val hit = visible[index]
                        val active = hit.order == activeIndex
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = VennelaTokens.touchTargetMin)
                                .background(
                                    if (active) accent.primary.copy(alpha = 0.22f) else Color.Transparent
                                )
                                .pressScale(pressedScale = 0.98f) { onSelect(hit.order) }
                                .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hit.pageIndex != null) {
                                    Icons.Default.PictureAsPdf
                                } else {
                                    Icons.Default.Search
                                },
                                contentDescription = null,
                                tint = if (active) accent.secondary else palette.textTertiary,
                                modifier = Modifier.size(VennelaTokens.iconSm)
                            )
                            Spacer(Modifier.width(VennelaTokens.spaceMd))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    hit.title, color = palette.textPrimary,
                                    fontSize = VennelaTokens.bodySmall, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    hit.snippet, color = palette.textSecondary,
                                    fontSize = VennelaTokens.micro,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (hits.size > visible.size) {
                    Text(
                        "Showing ${visible.size} of ${hits.size} matches",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro,
                        modifier = Modifier.padding(start = VennelaTokens.spaceLg, top = VennelaTokens.spaceXs)
                    )
                }
            }
        }
    }
}

/** Shimmering frosted placeholder shown while the page tree is being read. */
@Composable
private fun PdfGlassSkeleton(modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    val transition = rememberInfiniteTransition(label = "pdfSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(LiquidMotion.durationSlow, easing = LiquidMotion.glideEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pdfSkeletonAlpha"
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)) {
        repeat(5) { index ->
            Box(
                Modifier.fillMaxWidth(if (index % 2 == 0) 0.72f else 1f)
                    .height(if (index % 2 == 0) VennelaTokens.spaceXxl else VennelaTokens.spaceHuge)
                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                    .background(palette.cardSurface.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun PageIndicatorPill(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = LocalVennelaAccent.current
    Row(
        modifier
            .heightIn(min = VennelaTokens.touchTargetCompact)
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(accent.primary.copy(alpha = 0.26f))
            .border(1.dp, accent.secondary.copy(alpha = 0.45f), RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceXxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = accent.secondary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(Modifier.width(VennelaTokens.spaceXs))
        Text(
            label, color = accent.secondary,
            fontFamily = VennelaFonts.Mono,
            fontSize = VennelaTokens.micro, fontWeight = FontWeight.Bold, maxLines = 1
        )
    }
}

@Composable
private fun GlassRoundAction(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .then(
                if (active) {
                    Modifier
                        .background(accent.primary.copy(alpha = 0.30f))
                        .border(1.dp, accent.secondary.copy(alpha = 0.45f), CircleShape)
                } else {
                    Modifier
                }
            )
            .pressScale(pressedScale = 0.90f, onClick = if (enabled) onClick else null),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) palette.textPrimary else palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.viewerIcon)
        )
    }
}

@Composable
private fun PdfPageThumbnail(
    uri: Uri?,
    fileKey: String,
    pageIndex: Int,
    cache: ConcurrentHashMap<Int, Bitmap>,
    active: Boolean,
    modifier: Modifier = Modifier.size(
        width = VennelaTokens.badgeLg + VennelaTokens.spaceMd,
        height = VennelaTokens.badgeLg + VennelaTokens.radiusCardLarge
    ),
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val cacheKey = "$fileKey#$pageIndex"

    val thumbnail by produceState<Bitmap?>(initialValue = cache[pageIndex], uri, pageIndex) {
        val inMemory = cache[pageIndex]
        if (inMemory != null && !inMemory.isRecycled) {
            value = inMemory
            return@produceState
        }
        PdfProCache.thumb(cacheKey)?.let {
            cache[pageIndex] = it
            value = it
            return@produceState
        }
        if (uri == null) return@produceState
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (pageIndex in 0 until renderer.pageCount) {
                            renderer.openPage(pageIndex).use { pdfPage ->
                                val ratio = PdfThumbPixelWidth.toFloat() / pdfPage.width.coerceAtLeast(1)
                                val height = (pdfPage.height * ratio).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(
                                    PdfThumbPixelWidth, height, Bitmap.Config.ARGB_8888
                                )
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap
                            }
                        } else {
                            null
                        }
                    }
                }
            }.getOrNull()
        }
        if (decoded != null) {
            cache[pageIndex] = decoded
            PdfProCache.putThumb(cacheKey, decoded)
            value = decoded
        }
    }

    Box(
        modifier
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .background(palette.cardSurface.copy(alpha = 0.6f))
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) accent.secondary else palette.cardBorder,
                shape = RoundedCornerShape(VennelaTokens.radiusChip)
            )
            .pressScale(pressedScale = 0.94f, onClick = onClick)
    ) {
        val bmp = thumbnail
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1} thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "${pageIndex + 1}", color = palette.textSecondary,
                    fontSize = VennelaTokens.bodySmall, fontWeight = FontWeight.SemiBold
                )
            }
        }
        Box(
            Modifier.align(Alignment.BottomStart)
                .padding(VennelaTokens.spaceXs)
                .clip(RoundedCornerShape(VennelaTokens.spaceXs))
                .background(if (active) accent.primary else palette.backgroundBottom.copy(alpha = 0.55f))
                .padding(horizontal = VennelaTokens.spaceXs, vertical = 1.dp)
        ) {
            Text(
                (pageIndex + 1).toString().padStart(2, '0'),
                color = palette.textPrimary,
                fontSize = VennelaTokens.nano,
                fontWeight = FontWeight.Bold
            )
        }
        if (active) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, accent.primary.copy(alpha = 0.22f)))
                )
            )
        }
    }
}
