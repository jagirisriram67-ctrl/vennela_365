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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.HyperGutterRadar
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.ViewerTopBar
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
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
 * Document-level parse cache. Page counts and rendered thumbnails survive the
 * screen leaving composition, so reopening the same PDF paints its real page
 * indicator and thumbnails on the first frame instead of re-rendering.
 */
private object PdfProCache {
    private const val MaxThumbs = 32
    private val pageCounts = ConcurrentHashMap<String, Int>()
    private val thumbs = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>) = size > MaxThumbs
    }

    fun pageCount(fileKey: String): Int = pageCounts[fileKey] ?: 0
    fun putPageCount(fileKey: String, count: Int) { if (count > 0) pageCounts[fileKey] = count }
    @Synchronized fun thumb(key: String): Bitmap? = thumbs[key]?.takeIf { !it.isRecycled }
    @Synchronized fun putThumb(key: String, bitmap: Bitmap) { thumbs[key] = bitmap }
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
 * PDF viewer.
 *
 * Hierarchy follows `Screens/9.pdf_viewer.png`: frosted toolbar with a real page
 * indicator, the rendered page on a paper bed, a page-thumbnail rail and a
 * frosted bottom tool bar — extended with a pro layer: quick find, page jump
 * with a real scrubber, outline drawer, zoom presets, reading display modes,
 * keep-screen-on, per-file reading position, share / open-with / print and a
 * metadata panel that reports only real values.
 *
 * The `PdfRenderer` pipeline (page clamping, bitmap sizing, recycling, export)
 * is preserved exactly as it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val extractedText by viewModel.documentText.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val scope = rememberCoroutineScope()
    val pageInsetPx = with(LocalDensity.current) { VennelaTokens.spaceSm.toPx() }
    val fileKey = selectedFile?.id ?: "no-pdf"

    // Reading position memory — restored per file id.
    var page by rememberSaveable(fileKey) { mutableStateOf(PdfReadingMemory.page(fileKey)) }
    var reloadKey by remember(fileKey) { mutableIntStateOf(0) }

    var showThumbRail by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf(PdfPanel.NONE) }
    var showTocSheet by remember { mutableStateOf(false) }
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
        scale = (scale * zoomChange).coerceIn(PdfMinZoom, PdfMaxZoom)
        offset = if (scale <= 1.01f) Offset.Zero else offset + panChange
    }

    val dragX = remember(fileKey) { Animatable(0f) }
    val pagingEnabled = scale <= 1.01f
    val stripState = rememberLazyListState()
    val thumbCache = remember(fileKey) { ConcurrentHashMap<Int, Bitmap>() }

    LaunchedEffect(page, rendered.pageCount) {
        if (rendered.pageCount > 0 && page in 0 until rendered.pageCount) {
            stripState.animateScrollToItem(page.coerceAtLeast(0))
        }
    }
    LaunchedEffect(fileKey, page) { PdfReadingMemory.putPage(fileKey, page) }
    LaunchedEffect(fileKey, scale) { PdfReadingMemory.putZoom(fileKey, scale) }

    // Real "fit width" needs the laid-out viewport and the true page aspect.
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

    // Quick find corpus. PdfRenderer exposes no text API, so the only honest
    // text corpus is the extractor's output — and only while it reads as text
    // rather than as a compressed binary stream.
    val searchableText = remember(extractedText) {
        val candidate = extractedText
        if (candidate.length < 24) {
            ""
        } else {
            val readable = candidate.count { it.isLetterOrDigit() || it.isWhitespace() }
            if (readable.toFloat() / candidate.length >= 0.90f) candidate.take(PdfMaxTextProbe) else ""
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

    // Selecting a page hit really turns the page; the rail highlight follows it.
    LaunchedEffect(activeHit, showFindBar) {
        val target = activeHit?.pageIndex
        if (showFindBar && target != null && target != page) goToPage(target, resetZoom = false)
    }

    val mime = selectedFile?.formatInfo?.mimeType?.takeIf { it.isNotBlank() } ?: "application/pdf"

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

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
        PdfChip("Outline", Icons.AutoMirrored.Filled.ListAlt) { showTocSheet = true },
        PdfChip("Full search screen", Icons.Default.Search) { viewModel.navigateTo(Screen.PdfSearch) },
        PdfChip("Thumbnails", Icons.Default.GridView, showThumbRail) { showThumbRail = !showThumbRail },
        PdfChip("Jump to page", Icons.AutoMirrored.Filled.ListAlt, panel == PdfPanel.JUMP) { panel = PdfPanel.JUMP },
        PdfChip("File info", Icons.Default.Info, panel == PdfPanel.INFO) { panel = PdfPanel.INFO },
        PdfChip("Keep screen on", Icons.Default.Visibility, keepScreenOn) { keepScreenOn = !keepScreenOn },
        PdfChip("Share file", Icons.Default.Share) { shareFile() },
        PdfChip("Share this page", Icons.Default.Share) { shareCurrentPage() },
        PdfChip("Open with", Icons.AutoMirrored.Filled.OpenInNew) { openWith() },
        PdfChip("Print", Icons.Default.Print) { printDocument() },
        PdfChip("Metadata screen", Icons.Default.Info) { viewModel.navigateTo(Screen.FileMetadata) }
    )
    val infoRows = listOf(
        "Name" to (selectedFile?.name ?: "No file open"),
        "Size" to (selectedFile?.formattedSize ?: "Unknown"),
        "Modified" to (selectedFile?.formattedDate ?: "Unknown"),
        "Format" to (selectedFile?.formatInfo?.name ?: "Unknown"),
        "Extension" to (selectedFile?.extension?.uppercase()?.ifBlank { "none" } ?: "Unknown"),
        "Pages" to if (rendered.pageCount > 0) "${rendered.pageCount} pages" else "Still reading the page tree…",
        "Page box" to if (rendered.pageWidth > 0) "${rendered.pageWidth} × ${rendered.pageHeight} px" else "Not rendered yet",
        "Reading position" to "Page ${page + 1} at ${(scale * 100).roundToInt()}%"
    )

    LiquidPage(
        modifier = Modifier.fillMaxSize(),
        title = null,
        dock = {
            Column(Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.gutter)) {
                PdfTray(
                    visible = showThumbRail && rendered.pageCount > 0,
                    innerPadding = PaddingValues(vertical = VennelaTokens.spaceSm)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.spaceMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pages", color = palette.textPrimary,
                            fontSize = VennelaTokens.caption, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Page ${page + 1} of ${rendered.pageCount}",
                            color = palette.textSecondary, fontSize = VennelaTokens.micro
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
                                "Display mode — ${viewerColorMode.title}",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.caption,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
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
                    elevation = VennelaTokens.elevationDock
                )
                Row(
                    Modifier.fillMaxWidth()
                        .liquidGlass(dockSpec, RoundedCornerShape(VennelaTokens.radiusDock))
                        .padding(horizontal = VennelaTokens.spaceXs, vertical = VennelaTokens.spaceXxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
                ) {
                    GlassRoundAction(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page",
                        enabled = page > 0
                    ) { goTo(page - 1) }

                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PageIndicatorPill("${page + 1} / ${rendered.pageCount.coerceAtLeast(1)}") {
                            panel = if (panel == PdfPanel.JUMP) PdfPanel.NONE else PdfPanel.JUMP
                        }
                    }

                    GlassRoundAction(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page",
                        enabled = page < rendered.pageCount - 1
                    ) { goTo(page + 1) }

                    GlassRoundAction(Icons.Default.Search, "Quick find", active = showFindBar) {
                        showFindBar = !showFindBar
                        if (!showFindBar) findQuery = ""
                    }
                    GlassRoundAction(
                        Icons.Default.ZoomIn, "Zoom controls",
                        active = panel == PdfPanel.ZOOM
                    ) { panel = if (panel == PdfPanel.ZOOM) PdfPanel.NONE else PdfPanel.ZOOM }
                    GlassRoundAction(
                        Icons.Default.MoreHoriz, "More tools",
                        active = panel == PdfPanel.VIEW || panel == PdfPanel.INFO
                    ) { panel = if (panel == PdfPanel.VIEW) PdfPanel.NONE else PdfPanel.VIEW }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            // Floating, slim toolbar — the page gets the whole screen.
            ViewerTopBar(
                title = selectedFile?.name ?: "PDF viewer",
                subtitle = buildString {
                    append("PDF")
                    if (rendered.pageCount > 0) append(" • ${rendered.pageCount} pages")
                    append(" • ${(scale * 100).roundToInt()}%")
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
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = "Jump to page",
                        active = panel == PdfPanel.JUMP,
                        onClick = { panel = if (panel == PdfPanel.JUMP) PdfPanel.NONE else PdfPanel.JUMP }
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

            Box(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = VennelaTokens.spaceXs)
                    .padding(top = VennelaTokens.viewerTopInset)
            ) {
            Box(
                Modifier.fillMaxSize()
                    .applyViewerColorMode(viewerColorMode)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.backgroundBottom)
                    .border(1.dp, accent.secondary.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
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
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { animateZoom(if (scale > 1.01f) 1f else 2f) })
                    },
                contentAlignment = Alignment.Center
            ) {
                val bitmap = rendered.bitmap
                when {
                    selectedFile?.uri == null -> VennelaEmptyState(
                        title = "No PDF open",
                        hint = "Open a PDF from Files or Recents to read it here.",
                        icon = Icons.Default.PictureAsPdf,
                        actionLabel = "Browse files",
                        onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                    )

                    bitmap != null -> {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF page ${page + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                                .padding(VennelaTokens.spaceSm)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x + dragX.value,
                                    translationY = offset.y
                                )
                                .transformable(transformState)
                        )

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

                        if (rendered.pageCount > 1) {
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

                        // The active quick-find result, highlighted over the page.
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

                    rendered.error != null -> Box(Modifier.padding(VennelaTokens.spaceXl)) {
                        VennelaErrorState(
                            title = "This PDF could not be rendered",
                            message = rendered.error ?: "Unknown rendering error",
                            onRetry = { reloadKey++ },
                            onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                        )
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

    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            containerColor = palette.cardSurfaceStrong,
            contentColor = palette.textPrimary
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceXl)
                    .padding(bottom = VennelaTokens.spaceXxl)
            ) {
                TrayHeader(
                    Icons.AutoMirrored.Filled.ListAlt, "Outline",
                    "${rendered.pageCount} pages • ${selectedFile?.name ?: "PDF"}"
                )
                Spacer(Modifier.height(VennelaTokens.spaceMd))
                ChipRail(
                    listOf(
                        PdfChip("Jump to page", Icons.AutoMirrored.Filled.ListAlt) {
                            showTocSheet = false; panel = PdfPanel.JUMP
                        },
                        PdfChip("Quick find", Icons.Default.Search) {
                            showTocSheet = false; showFindBar = true
                        },
                        PdfChip("Full outline screen", Icons.Default.Info) {
                            showTocSheet = false; viewModel.navigateTo(Screen.PdfToc)
                        }
                    )
                )
                Spacer(Modifier.height(VennelaTokens.spaceMd))
                if (rendered.pageCount == 0) {
                    Text(
                        "The page tree has not been read yet, so there is nothing to list.",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.bodySmall
                    )
                } else {
                    if (rendered.pageCount > PdfMaxOutlineRows) {
                        Text(
                            "Showing the first $PdfMaxOutlineRows of ${rendered.pageCount} pages",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.micro
                        )
                    }
                    LazyColumn(Modifier.fillMaxWidth().height(VennelaTokens.spaceHuge * 9)) {
                        items(
                            count = minOf(rendered.pageCount, PdfMaxOutlineRows),
                            key = { index -> "outline_$index" }
                        ) { index ->
                            val active = index == page
                            GlassListRow(
                                title = "Page ${index + 1}",
                                subtitle = if (active) "Currently open" else "Tap to open",
                                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
                                leading = {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = if (active) accent.secondary else palette.textTertiary,
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
            modifier = Modifier.fillMaxWidth().padding(bottom = VennelaTokens.spaceSm),
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
private fun PageIndicatorPill(label: String, onClick: () -> Unit) {
    val accent = LocalVennelaAccent.current
    Row(
        Modifier.clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(accent.primary.copy(alpha = 0.26f))
            .border(1.dp, accent.secondary.copy(alpha = 0.45f), RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXxs),
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
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        Modifier.size(VennelaTokens.viewerControl)
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
        Modifier.size(
            width = VennelaTokens.badgeLg + VennelaTokens.spaceMd,
            height = VennelaTokens.badgeLg + VennelaTokens.radiusCardLarge
        )
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
