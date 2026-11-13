package com.vennela365.viewer.ui.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.OkenScanFilter
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.SuiteStyleSwitcherStrip
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.rememberSuiteSessionState
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How the image is scaled into the viewport. */
private enum class ImageFit(val label: String) {
    Fit("Fit"),
    Fill("Fill"),
    Actual("100%"),
    Free("Free")
}

/** Non-destructive GPU color grading presets. */
private enum class ImageColorPreset(val label: String) {
    Original("Original"),
    Vivid("Vivid"),
    Warm("Warm"),
    Cool("Cool"),
    Mono("Mono")
}

private fun ImageColorPreset.toColorFilter(): ColorFilter? = when (this) {
    ImageColorPreset.Original -> null
    ImageColorPreset.Mono -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    ImageColorPreset.Vivid -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.42f) })
    ImageColorPreset.Warm -> ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                1.08f, 0f, 0f, 0f, 8f,
                0f, 1.02f, 0f, 0f, 4f,
                0f, 0f, 0.88f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    ImageColorPreset.Cool -> ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.92f, 0f, 0f, 0f, -4f,
                0f, 1.0f, 0f, 0f, 2f,
                0f, 0f, 1.12f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}

/** Load state of the hero image, used to drive the frosted skeleton. */
private enum class ImageLoad { Loading, Ready, Failed }

/**
 * Only fields the file actually carries. Anything null is simply not shown —
 * a camera model or GPS position is never invented.
 */
private data class ImageExif(
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val orientation: String? = null,
    val make: String? = null,
    val model: String? = null,
    val dateTaken: String? = null,
    val exposure: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val gps: String? = null
) {
    val dimensions: String?
        get() = if (widthPx > 0 && heightPx > 0) "$widthPx × $heightPx px" else null

    val megapixels: String?
        get() = if (widthPx > 0 && heightPx > 0) {
            String.format(Locale.US, "%.1f MP", widthPx.toLong() * heightPx / 1_000_000.0)
        } else {
            null
        }

    val hasExifFields: Boolean
        get() = listOf(make, model, dateTaken, exposure, aperture, iso, focalLength, flash, gps)
            .any { it != null }
}

/** Small bounded cache so reopening the same image is instant. */
private val imageExifCache = LinkedHashMap<String, ImageExif>()

private fun cacheExif(id: String, value: ImageExif): ImageExif {
    if (imageExifCache.size > 24) imageExifCache.clear()
    imageExifCache[id] = value
    return value
}

/**
 * Full-bleed image viewer on the liquid glass frame.
 *
 * Everything shown is read from the file itself: sibling images form the
 * filmstrip, dimensions and EXIF come from the decoder and the container's own
 * metadata block, and the zoom readout is the live transform state. Nothing is
 * mocked, and an unreadable field is simply absent.
 */
@Composable
fun ImageViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val activeSuite by viewModel.preferredSuiteStyle.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val suiteSession = rememberSuiteSessionState(activeSuite, palette.isDark)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = selectedFile

    // Real sibling images from the folder the user actually opened.
    val strip = remember(file?.id) {
        val siblings = viewModel.siblingImageFiles()
        if (siblings.isEmpty()) listOfNotNull(file) else siblings
    }
    val currentIndex = strip.indexOfFirst { it.id == file?.id }.coerceAtLeast(0)
    val stripState = rememberLazyListState()

    var scale by remember(file?.id) { mutableFloatStateOf(1f) }
    var rotation by remember(file?.id) { mutableFloatStateOf(0f) }
    var flipped by remember(file?.id) { mutableStateOf(false) }
    var offsetX by remember(file?.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(file?.id) { mutableFloatStateOf(0f) }
    var fit by remember(file?.id) { mutableStateOf(ImageFit.Fit) }
    var colorPreset by remember(file?.id) { mutableStateOf(ImageColorPreset.Original) }
    var showGrid by remember(file?.id) { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var load by remember(file?.id) { mutableStateOf(ImageLoad.Loading) }
    var reloadToken by remember(file?.id) { mutableIntStateOf(0) }

    // ---- Real dimensions + EXIF, cached per file id ----
    val exif by produceState(initialValue = imageExifCache[file?.id], key1 = file?.id) {
        val target = file
        if (target?.uri == null) {
            value = ImageExif()
            return@produceState
        }
        val cached = imageExifCache[target.id]
        if (cached != null) {
            value = cached
            return@produceState
        }
        val summary = withContext(Dispatchers.IO) { readImageExif(context, target.uri) }
        value = cacheExif(target.id, summary)
    }
    val exifValue = exif

    val viewportW = viewport.width.toFloat()
    val viewportH = viewport.height.toFloat()
    val imageW = exifValue?.widthPx ?: 0
    val imageH = exifValue?.heightPx ?: 0
    val fitFactor = if (imageW > 0 && imageH > 0 && viewportW > 0f && viewportH > 0f) {
        min(viewportW / imageW, viewportH / imageH)
    } else {
        0f
    }
    // Honest pixel ratio: 100% means one image pixel per screen pixel.
    val nativeRatio = if (fitFactor > 0f) scale * fitFactor else 0f
    val zoomPercent = (scale * 100f).roundToInt()

    val failure = viewerState as? ViewerState.Failed
    val busy = viewerState is ViewerState.Loading || viewerState is ViewerState.Detecting

    fun clampOffsets() {
        if (imageW <= 0 || imageH <= 0 || fitFactor <= 0f || viewportW <= 0f || viewportH <= 0f) {
            offsetX = 0f
            offsetY = 0f
            return
        }
        val quarter = (((rotation.roundToInt() % 360) + 360) % 360)
        val swapped = quarter == 90 || quarter == 270
        val drawnW = (if (swapped) imageH else imageW) * fitFactor * scale
        val drawnH = (if (swapped) imageW else imageH) * fitFactor * scale
        val maxX = ((drawnW - viewportW) / 2f).coerceAtLeast(0f)
        val maxY = ((drawnH - viewportH) / 2f).coerceAtLeast(0f)
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }

    fun applyZoom(target: Float) {
        scale = target.coerceIn(0.25f, 16f)
        clampOffsets()
    }

    fun selectFit(mode: ImageFit) {
        fit = mode
        offsetX = 0f
        offsetY = 0f
        scale = when (mode) {
            ImageFit.Fit -> 1f
            ImageFit.Fill -> if (fitFactor > 0f) {
                max(viewportW / imageW, viewportH / imageH) / fitFactor
            } else {
                1f
            }
            ImageFit.Actual -> if (fitFactor > 0f) 1f / fitFactor else 1f
            ImageFit.Free -> scale
        }.coerceIn(0.25f, 16f)
    }

    /** Double-tap toggles between fit and 200% of the fitted size. */
    fun toggleDoubleTap() {
        if (abs(scale - 1f) < 0.01f) {
            fit = ImageFit.Free
            applyZoom(2f)
        } else {
            selectFit(ImageFit.Fit)
        }
    }

    fun notify(text: String) {
        message = text
    }

    // ---- Keep screen on: real window flag, always cleared on the way out ----
    val activity = remember(context) { context as? android.app.Activity }
    DisposableEffect(keepScreenOn, activity) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // ---- The transient confirmation pill ----
    LaunchedEffect(message) {
        if (message != null) {
            delay(2200L)
            message = null
        }
    }

    // ---- Filmstrip follows the current image, and neighbours are pre-warmed ----
    LaunchedEffect(currentIndex, strip.size) {
        if (strip.isNotEmpty()) runCatching { stripState.animateScrollToItem(currentIndex) }
    }
    LaunchedEffect(currentIndex, strip.size) {
        val loader = context.imageLoader
        listOf(currentIndex - 1, currentIndex + 1).forEach { index ->
            val item = strip.getOrNull(index) ?: return@forEach
            val uri = item.uri ?: return@forEach
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .size(Size(512, 512))
                    .memoryCacheKey("thumb-${item.id}")
                    .build()
            )
        }
    }

    val decodeMax = (max(viewport.width, viewport.height) * 2).coerceIn(1024, 2048)
    val request = remember(file?.id, decodeMax, reloadToken) {
        ImageRequest.Builder(context)
            .data(file?.uri)
            .size(Size(decodeMax, decodeMax))
            .memoryCacheKey("full-${file?.id}")
            .build()
    }

    fun shareImage() {
        val target = file ?: return
        val uri = target.uri ?: return
        val intent = Intent(Intent.ACTION_SEND).setType(imageMime(target))
        if (!ShareUri.attachStream(context, intent, uri)) {
            notify("This image has no stream another app can read")
            return
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share ${target.name}")) }
            .onFailure { notify("No app on this device can share the image") }
    }

    fun openWith() {
        val target = file ?: return
        val intent = ShareUri.viewIntent(context, target.uri, imageMime(target))
        if (intent == null) {
            notify("This image cannot be handed to another app")
            return
        }
        runCatching { context.startActivity(intent) }
            .onFailure { notify("No installed app can open ${target.extension.uppercase()} images") }
    }

    fun copyPath() {
        val target = file ?: return
        val value = target.path.ifBlank { target.uri?.toString().orEmpty() }
        if (value.isBlank()) {
            notify("This file has no readable path on this device")
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            notify("Clipboard unavailable on this device")
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Vennela path", value))
        notify("Path copied")
    }

    /**
     * Setting a wallpaper goes through the system crop picker, which needs no
     * permission; a direct write is only attempted when the platform actually
     * grants SET_WALLPAPER. Failures are reported, never faked.
     */
    fun applyWallpaper() {
        val target = file ?: return
        val shareable = ShareUri.toShareable(context, target.uri)
        if (shareable == null) {
            notify("This image can't be handed to the wallpaper picker")
            return
        }
        val manager = WallpaperManager.getInstance(context)
        val supported = runCatching { manager.isWallpaperSupported }.getOrDefault(false)
        if (!supported) {
            notify("This device does not support changing the wallpaper here")
            return
        }
        val cropIntent = runCatching { manager.getCropAndSetWallpaperIntent(shareable) }.getOrNull()
        if (cropIntent != null) {
            val opened = runCatching {
                context.startActivity(cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }.isSuccess
            notify(if (opened) "Choose where to set this wallpaper" else "No wallpaper app accepted this image")
            return
        }
        val granted = context.checkSelfPermission(Manifest.permission.SET_WALLPAPER) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notify("Wallpaper needs the SET_WALLPAPER permission, which this build does not request")
            return
        }
        scope.launch {
            val applied = withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(shareable)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    val bitmap = context.contentResolver.openInputStream(shareable)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    } ?: return@runCatching false
                    manager.setBitmap(bitmap)
                    bitmap.recycle()
                    true
                }.getOrDefault(false)
            }
            notify(if (applied) "Wallpaper set" else "The system refused to set this wallpaper")
        }
    }

    LiquidPage(
        title = null
    ) { pagePadding ->
        Box( modifier = Modifier .fillMaxSize() .background(palette.backgroundBottom) ) {
            // Full-bleed photo with a floating control strip, Google Photos style.
            ReaderTopToolbar(
                title = file?.name ?: "Image viewer",
                subtitle = file?.let { "${it.extension.uppercase()} • ${it.formattedSize}" }
                    ?: "No image selected",
                onBack = { viewModel.navigateBack() },
                modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
            ) {
                val isFavorite = file?.isFavorite == true
                ReaderIconButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) {
                        "Remove ${file?.name ?: "image"} from favorites"
                    } else {
                        "Add ${file?.name ?: "image"} to favorites"
                    },
                    active = isFavorite,
                    onClick = { file?.let { viewModel.toggleFavorite(it) } }
                )
                Spacer(Modifier.width(VennelaTokens.spaceXs))
                ReaderIconButton(
                    icon = Icons.Default.Lightbulb,
                    contentDescription = if (keepScreenOn) "Allow the screen to sleep" else "Keep the screen on",
                    active = keepScreenOn,
                    onClick = { keepScreenOn = !keepScreenOn }
                )
                Spacer(Modifier.width(VennelaTokens.spaceXs))
                ReaderIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = if (showInfo) "Hide image details" else "Show image details",
                    active = showInfo,
                    onClick = { showInfo = !showInfo }
                )
            }
            when {
                file == null -> ViewerNotice {
                    VennelaEmptyState(
                        title = "No image open",
                        hint = "Pick a photo, screenshot or scan from your device and pinch, pan and rotate it here — fully offline.",
                        icon = Icons.Default.Image,
                        actionLabel = "Browse storage",
                        onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                    )
                }

                failure != null -> ViewerNotice {
                    VennelaErrorState(
                        title = "Couldn't open this image",
                        message = failure.reason,
                        onRetry = if (failure.canRetry) {
                            { viewModel.openFile(file) }
                        } else {
                            null
                        },
                        onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                file.uri == null -> ViewerNotice {
                    VennelaEmptyState(
                        title = "Nothing to render",
                        hint = "${file.name} has no readable image stream on this device. The original file is untouched.",
                        icon = Icons.Default.Image,
                        actionLabel = "File properties",
                        onAction = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewport = it }
                            .pointerInput(file.id, scale) {
                                detectTapGestures(
                                    onTap = { chromeVisible = !chromeVisible },
                                    onDoubleTap = { toggleDoubleTap() }
                                )
                            }
                            .pointerInput(file.id, scale > 1.05f) {
                                viewportGestures(
                                    zoomed = scale > 1.05f,
                                    onTransform = { zoom, pan, turn ->
                                        fit = ImageFit.Free
                                        scale = (scale * zoom).coerceIn(0.25f, 16f)
                                        rotation += turn
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        clampOffsets()
                                    },
                                    onSwipe = { travelled ->
                                        val next = if (travelled < 0f) currentIndex + 1 else currentIndex - 1
                                        strip.getOrNull(next)?.let { viewModel.openFile(it) }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val effectiveColorFilter = remember(activeSuite, suiteSession.scanFilter, colorPreset) {
                            if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER && suiteSession.scanFilter != OkenScanFilter.ORIGINAL) {
                                suiteSession.scanFilter.toColorFilter()
                            } else {
                                colorPreset.toColorFilter()
                            }
                        }

                        AsyncImage(
                            model = request,
                            contentDescription = "Image ${file.name}, ${zoomPercent}% zoom",
                            contentScale = ContentScale.Fit,
                            colorFilter = effectiveColorFilter,
                            onLoading = { load = ImageLoad.Loading },
                            onSuccess = { load = ImageLoad.Ready },
                            onError = { load = ImageLoad.Failed },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale * (if (flipped) -1f else 1f),
                                    scaleY = scale,
                                    rotationZ = rotation,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )

                        if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER && load == ImageLoad.Ready) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (suiteSession.showAutoCropHandles) {
                                    val turquoise = Color(0xFF00D2A0)
                                    val stroke = 2.8.dp.toPx()
                                    val pad = 28.dp.toPx()
                                    val corner = 26.dp.toPx()
                                    val w = size.width - pad * 2
                                    val h = size.height - pad * 2
                                    drawRect(
                                        color = turquoise.copy(alpha = 0.72f),
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
                                if (suiteSession.showOfficialStamp) {
                                    val stampRed = Color(0xFFD93025).copy(alpha = 0.80f)
                                    val center = Offset(size.width - 78.dp.toPx(), size.height - 140.dp.toPx())
                                    rotate(-14f, pivot = center) {
                                        drawCircle(stampRed, radius = 38.dp.toPx(), center = center, style = Stroke(width = 2.5.dp.toPx()))
                                        drawCircle(stampRed, radius = 32.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
                                    }
                                }
                            }
                        }

                        if (showGrid && load == ImageLoad.Ready) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val lineColor = Color.White.copy(alpha = 0.28f)
                                val thirdW = size.width / 3f
                                val thirdH = size.height / 3f
                                drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth = 1.dp.toPx())
                                drawLine(lineColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), strokeWidth = 1.dp.toPx())
                                drawLine(lineColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth = 1.dp.toPx())
                                drawLine(lineColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), strokeWidth = 1.dp.toPx())
                            }
                        }

                        if (load == ImageLoad.Loading) {
                            ImageSkeleton(modifier = Modifier.fillMaxSize())
                        }

                        if (load == ImageLoad.Failed) {
                            Column( modifier = Modifier .align(Alignment.Center) .padding(VennelaTokens.spaceHuge), horizontalAlignment = Alignment.CenterHorizontally ) {
                                Text(
                                    text = "This image could not be decoded",
                                    color = palette.textPrimary,
                                    fontSize = VennelaTokens.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                                Text(
                                    text = "${file.name} may be damaged or use a codec this device lacks.",
                                    color = palette.textSecondary,
                                    fontSize = VennelaTokens.caption
                                )
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = VennelaTokens.touchTargetMin)
                                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                        .background(accent.primary.copy(alpha = 0.9f))
                                        .pressScale(pressedScale = 0.95f) {
                                            load = ImageLoad.Loading
                                            reloadToken += 1
                                        }
                                        .padding(horizontal = VennelaTokens.spaceXl),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(VennelaTokens.iconMd)
                                    )
                                    Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                                    Text( text = "Retry", color = palette.textPrimary, fontSize = VennelaTokens.bodySmall, fontWeight = FontWeight.SemiBold )
                                }
                            }
                        }

                        if (busy && load != ImageLoad.Failed) {
                            GlassPanel(
                                modifier = Modifier.align(Alignment.Center),
                                shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                                alpha = 0.38f,
                                elevation = VennelaTokens.elevationOverlay
                            ) {
                                VennelaLoadingState(label = "Rendering ${file.extension.uppercase()}...")
                            }
                        }

                        // Live readouts: page position, transform state.
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(VennelaTokens.spaceSm),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            ViewerStatPill(text = "${currentIndex + 1} of ${strip.size}")
                            ViewerStatPill(text = "$zoomPercent% • ${fit.label}")
                            ViewerStatPill(
                                text = "${
                                    (((rotation.roundToInt() % 360) + 360) % 360)
                                }°${if (flipped) " • mirrored" else ""}"
                            )
                        }

                        AnimatedVisibility(
                            visible = message != null,
                            enter = fadeIn(animationSpec = tween(LiquidMotion.durationFast)),
                            exit = fadeOut(animationSpec = tween(LiquidMotion.durationBase)),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Box(modifier = Modifier.padding(top = VennelaTokens.spaceHuge)) {
                                ViewerStatPill(text = message.orEmpty())
                            }
                        }
                    }

                    if (showInfo) {
                        ImageInfoPanel(
                            file = file,
                            exif = exifValue,
                            zoomPercent = zoomPercent,
                            nativeRatio = nativeRatio,
                            rotationDegrees = (((rotation.roundToInt() % 360) + 360) % 360),
                            mirrored = flipped,
                            keepScreenOn = keepScreenOn,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxHeight(0.62f),
                            onClose = { showInfo = false },
                            onToggleKeepScreenOn = { keepScreenOn = !keepScreenOn },
                            onShare = { shareImage() },
                            onOpenWith = { openWith() },
                            onWallpaper = { applyWallpaper() },
                            onCopyPath = { copyPath() },
                            onProperties = { viewModel.navigateTo(Screen.FileMetadata) },
                            onExifScreen = { viewModel.navigateTo(Screen.ImageExif) }
                        )
                    }

                    AnimatedVisibility(
                        visible = chromeVisible,
                        enter = fadeIn(animationSpec = tween(LiquidMotion.durationFast)),
                        exit = fadeOut(animationSpec = tween(LiquidMotion.durationFast)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 760.dp)
                                .fillMaxWidth()
                                .padding(bottom = pagePadding.calculateBottomPadding()),
                            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            // Suite Switcher + Oken Scanner HD Filter Rail
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = VennelaTokens.gutter),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SuiteStyleSwitcherStrip(
                                    activeSuite = activeSuite,
                                    onSelectSuite = { viewModel.setPreferredSuiteStyle(it) },
                                    isDark = true
                                )
                                if (activeSuite == ViewerSuiteStyle.OKEN_SCANNER) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(OkenScanFilter.entries) { filter ->
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
                                                text = "Official Stamp",
                                                isActive = suiteSession.showOfficialStamp,
                                                onClick = { suiteSession.showOfficialStamp = !suiteSession.showOfficialStamp }
                                            )
                                        }
                                    }
                                }
                            }

                            Filmstrip(
                                strip = strip,
                                currentIndex = currentIndex,
                                state = stripState,
                                onSelect = { item -> viewModel.openFile(item) }
                            )

                            ImageToolDock(
                                fit = fit,
                                colorPreset = colorPreset,
                                showGrid = showGrid,
                                zoomPercent = zoomPercent,
                                canZoomOut = scale > 0.26f,
                                showInfo = showInfo,
                                keepScreenOn = keepScreenOn,
                                onFit = { selectFit(it) },
                                onColorPreset = { colorPreset = it },
                                onToggleGrid = { showGrid = !showGrid },
                                onZoom = { applyZoom(if (scale > 1.5f) 1f else 2f) },
                                onZoomOut = { applyZoom(scale / 1.5f) },
                                onRotateLeft = {
                                    rotation -= 90f
                                    clampOffsets()
                                },
                                onRotateRight = {
                                    rotation += 90f
                                    clampOffsets()
                                },
                                onFlip = { flipped = !flipped },
                                onReset = {
                                    rotation = 0f
                                    flipped = false
                                    colorPreset = ImageColorPreset.Original
                                    showGrid = false
                                    selectFit(ImageFit.Fit)
                                },
                                onToggleInfo = { showInfo = !showInfo },
                                onShare = { shareImage() },
                                onOpenWith = { openWith() },
                                onWallpaper = { applyWallpaper() },
                                onCopyPath = { copyPath() },
                                onKeepScreenOn = { keepScreenOn = !keepScreenOn }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun imageMime(file: FileItem): String =
    file.formatInfo.mimeType.takeIf { it.isNotBlank() } ?: "image/*"

/* ------------------------------------------------------------------------- */
/* Gestures                                                                   */
/* ------------------------------------------------------------------------- */

/**
 * One finger pans a zoomed image and swipes to the next/previous sibling when
 * the image is fitted; two fingers pinch, rotate and pan. A dedicated tap
 * detector handles double-tap, so this never fights it.
 */
private suspend fun PointerInputScope.viewportGestures(
    zoomed: Boolean,
    onTransform: (zoom: Float, pan: Offset, turn: Float) -> Unit,
    onSwipe: (Float) -> Unit
) {
    val swipeThreshold = 64.dp.toPx()
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var travelled = 0f
        var multiTouch = false
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed > 1) multiTouch = true
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            val turn = event.calculateRotation()
            if (pressed > 0) {
                if (multiTouch || zoomed) {
                    if (zoom != 1f || turn != 0f || pan != Offset.Zero) {
                        onTransform(zoom, pan, turn)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } else if (pan != Offset.Zero) {
                    travelled += pan.x
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
            if (event.changes.all { !it.pressed }) break
        }
        if (!multiTouch && !zoomed && abs(travelled) > swipeThreshold) onSwipe(travelled)
    }
}

/* ------------------------------------------------------------------------- */
/* Real metadata                                                              */
/* ------------------------------------------------------------------------- */

private fun readImageExif(context: Context, uri: Uri): ImageExif {
    var width = 0
    var height = 0
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, bounds)
            width = bounds.outWidth.coerceAtLeast(0)
            height = bounds.outHeight.coerceAtLeast(0)
        }
    }

    val exifFile = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream -> ExifInterface(stream) }
    }.getOrNull() ?: return ImageExif(width, height)

    fun tag(key: String): String? =
        runCatching { exifFile.getAttribute(key)?.takeIf { it.isNotBlank() } }.getOrNull()

    val exifWidth = tag(ExifInterface.TAG_IMAGE_WIDTH)?.toIntOrNull() ?: 0
    val exifHeight = tag(ExifInterface.TAG_IMAGE_LENGTH)?.toIntOrNull() ?: 0
    val orientation = when (tag(ExifInterface.TAG_ORIENTATION)) {
        "1" -> "Normal (0°)"
        "2" -> "Mirrored horizontally"
        "3" -> "Rotated 180°"
        "4" -> "Mirrored vertically"
        "5" -> "Mirrored, then rotated 90° clockwise"
        "6" -> "Rotated 90° clockwise"
        "7" -> "Mirrored, then rotated 90° counter-clockwise"
        "8" -> "Rotated 90° counter-clockwise"
        else -> null
    }
    val make = tag(ExifInterface.TAG_MAKE)
    val model = tag(ExifInterface.TAG_MODEL)
    val exposure = tag(ExifInterface.TAG_EXPOSURE_TIME)?.let { "$it s" }
    val aperture = tag(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
    @Suppress("DEPRECATION")
    val iso = tag(ExifInterface.TAG_ISO_SPEED_RATINGS)
    val focal = tag(ExifInterface.TAG_FOCAL_LENGTH)?.let { "$it mm" }
    val flash = tag(ExifInterface.TAG_FLASH)?.toIntOrNull()?.let {
        if (it and 0x1 == 1) "Fired" else "Did not fire"
    }
    val latitude = tag(ExifInterface.TAG_GPS_LATITUDE)?.toDoubleOrNull()
    val longitude = tag(ExifInterface.TAG_GPS_LONGITUDE)?.toDoubleOrNull()
    val gps = if (latitude != null && longitude != null) {
        val latRef = tag(ExifInterface.TAG_GPS_LATITUDE_REF) ?: ""
        val lonRef = tag(ExifInterface.TAG_GPS_LONGITUDE_REF) ?: ""
        String.format(Locale.US, "%.5f° %s, %.5f° %s", latitude, latRef, longitude, lonRef)
    } else {
        null
    }

    return ImageExif(
        widthPx = if (exifWidth > 0) exifWidth else width,
        heightPx = if (exifHeight > 0) exifHeight else height,
        orientation = orientation,
        make = make,
        model = model,
        dateTaken = tag(ExifInterface.TAG_DATETIME_ORIGINAL),
        exposure = exposure,
        aperture = aperture,
        iso = iso,
        focalLength = focal,
        flash = flash,
        gps = gps
    )
}

/* ------------------------------------------------------------------------- */
/* State frames                                                               */
/* ------------------------------------------------------------------------- */

/** Centres a state surface over the dark viewport without stretching it. */
@Composable
private fun ViewerNotice(content: @Composable () -> Unit) {
    Box( modifier = Modifier .fillMaxSize() .padding(horizontal = VennelaTokens.gutter), contentAlignment = Alignment.Center ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
            alpha = 0.38f
        ) {
            content()
        }
    }
}

/** Frosted placeholder blocks while the decode is in flight. */
@Composable
private fun ImageSkeleton(modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    val transition = rememberInfiniteTransition(label = "imageSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "imageSkeletonAlpha"
    )
    Column(
        modifier = modifier.padding(VennelaTokens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box( modifier = Modifier .fillMaxWidth() .height(180.dp) .clip(RoundedCornerShape(VennelaTokens.radiusCard)) .background(palette.cardSurface.copy(alpha = alpha)) )
        Box( modifier = Modifier .fillMaxWidth(0.7f) .height(14.dp) .clip(RoundedCornerShape(VennelaTokens.radiusChip)) .background(palette.cardSurface.copy(alpha = alpha)) )
        Box( modifier = Modifier .fillMaxWidth(0.45f) .height(12.dp) .clip(RoundedCornerShape(VennelaTokens.radiusChip)) .background(palette.cardSurface.copy(alpha = alpha)) )
    }
}

@Composable
private fun ViewerStatPill(text: String) {
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(VennelaTokens.radiusPill),
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.55f),
                spotColor = accent.primary.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.primary.copy(alpha = 0.88f),
                        accent.secondary.copy(alpha = 0.62f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(VennelaTokens.radiusPill))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = VennelaFonts.Mono,
            fontSize = VennelaTokens.micro,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(
                horizontal = VennelaTokens.spaceMd,
                vertical = VennelaTokens.spaceXs
            )
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Filmstrip                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun Filmstrip(
    strip: List<FileItem>,
    currentIndex: Int,
    state: androidx.compose.foundation.lazy.LazyListState,
    onSelect: (FileItem) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter),
        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
        alpha = 0.34f,
        elevation = VennelaTokens.elevationBar
    ) {
        if (strip.isEmpty()) {
            Text( text = "No other images in this folder", color = palette.textSecondary, fontSize = VennelaTokens.micro, modifier = Modifier.padding(VennelaTokens.spaceLg) )
        } else {
            LazyRow(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = VennelaTokens.spaceSm),
                contentPadding = PaddingValues(horizontal = VennelaTokens.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                itemsIndexed(items = strip, key = { _, item -> item.id }) { index, item ->
                    val isSelected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = VennelaTokens.touchTargetMin)
                            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                            .background(palette.cardSurfaceStrong)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent.secondary else palette.cardBorder,
                                shape = RoundedCornerShape(VennelaTokens.radiusChip)
                            )
                            .staggeredEntrance(index)
                            .pressScale(pressedScale = 0.94f) {
                                if (!isSelected) onSelect(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.uri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.uri)
                                    .size(Size(160, 160))
                                    .memoryCacheKey("thumb-${item.id}")
                                    .build(),
                                contentDescription = if (isSelected) {
                                    "${item.name}, currently shown"
                                } else {
                                    "Show ${item.name}"
                                },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Show ${item.name}",
                                tint = palette.textTertiary,
                                modifier = Modifier.size(VennelaTokens.iconLg)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Tool dock                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun ImageToolDock(
    fit: ImageFit,
    colorPreset: ImageColorPreset,
    showGrid: Boolean,
    zoomPercent: Int,
    canZoomOut: Boolean,
    showInfo: Boolean,
    keepScreenOn: Boolean,
    onFit: (ImageFit) -> Unit,
    onColorPreset: (ImageColorPreset) -> Unit,
    onToggleGrid: () -> Unit,
    onZoom: () -> Unit,
    onZoomOut: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlip: () -> Unit,
    onReset: () -> Unit,
    onToggleInfo: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onWallpaper: () -> Unit,
    onCopyPath: () -> Unit,
    onKeepScreenOn: () -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter),
        shape = RoundedCornerShape(VennelaTokens.radiusDock),
        alpha = 0.36f,
        elevation = VennelaTokens.elevationDock
    ) {
        Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceSm)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = VennelaTokens.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                items(ImageFit.entries.toList(), key = { "fit_${it.name}" }) { mode ->
                    Box( modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin), contentAlignment = Alignment.Center ) {
                        LiquidChip(
                            text = mode.label,
                            icon = if (mode == ImageFit.Actual) Icons.Default.AspectRatio else null,
                            isActive = fit == mode,
                            onClick = { onFit(mode) }
                        )
                    }
                }
                items(ImageColorPreset.entries.toList(), key = { "preset_${it.name}" }) { preset ->
                    Box( modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin), contentAlignment = Alignment.Center ) {
                        LiquidChip(
                            text = preset.label,
                            icon = if (preset == ImageColorPreset.Original) Icons.Default.AutoFixHigh else null,
                            isActive = colorPreset == preset,
                            onClick = { onColorPreset(preset) }
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VennelaTokens.spaceXs),
                contentPadding = PaddingValues(horizontal = VennelaTokens.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                item {
                    ViewerDockAction(
                        icon = Icons.Default.ZoomOut,
                        label = "Out",
                        description = "Zoom out",
                        enabled = canZoomOut,
                        modifier = Modifier.testTag("image_zoom_out_button"),
                        onClick = onZoomOut
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.ZoomIn,
                        label = "$zoomPercent%",
                        description = "Zoom in",
                        active = zoomPercent > 100,
                        modifier = Modifier.testTag("image_zoom_in_button"),
                        onClick = onZoom
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.AutoMirrored.Filled.RotateLeft,
                        label = "Left",
                        description = "Rotate 90 degrees counter-clockwise",
                        onClick = onRotateLeft
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        label = "Right",
                        description = "Rotate 90 degrees clockwise",
                        modifier = Modifier.testTag("image_rotate_right_button"),
                        onClick = onRotateRight
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.Flip,
                        label = "Mirror",
                        description = "Mirror the image horizontally",
                        modifier = Modifier.testTag("image_flip_button"),
                        onClick = onFlip
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.GridOn,
                        label = "Grid",
                        description = if (showGrid) "Hide composition grid" else "Show rule-of-thirds grid",
                        active = showGrid,
                        modifier = Modifier.testTag("image_grid_button"),
                        onClick = onToggleGrid
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.FitScreen,
                        label = "Reset",
                        description = "Reset zoom, rotation, filters and mirroring",
                        onClick = onReset
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.Info,
                        label = "Details",
                        description = if (showInfo) "Hide image details" else "Show image details",
                        active = showInfo,
                        modifier = Modifier.testTag("image_info_button"),
                        onClick = onToggleInfo
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.Lightbulb,
                        label = "Awake",
                        description = if (keepScreenOn) "Allow the screen to sleep" else "Keep the screen on",
                        active = keepScreenOn,
                        onClick = onKeepScreenOn
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.Share,
                        label = "Share",
                        description = "Share this image",
                        onClick = onShare
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Open",
                        description = "Open this image in another app",
                        onClick = onOpenWith
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.Wallpaper,
                        label = "Wallpaper",
                        description = "Set this image as the wallpaper",
                        onClick = onWallpaper
                    )
                }
                item {
                    ViewerDockAction(
                        icon = Icons.Default.ContentCopy,
                        label = "Path",
                        description = "Copy this file's location",
                        onClick = onCopyPath
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerDockAction(
    icon: ImageVector,
    label: String,
    description: String,
    active: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = modifier
            .heightIn(min = VennelaTokens.touchTargetMin)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .pressScale(pressedScale = 0.92f, onClick = if (enabled) onClick else null)
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = when {
                !enabled -> palette.textTertiary
                active -> accent.secondary
                else -> palette.dockText
            },
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
        Text(
            text = label,
            color = when {
                !enabled -> palette.textTertiary
                active -> accent.secondary
                else -> palette.dockTextMuted
            },
            fontSize = VennelaTokens.nano,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Details panel: real file facts, real EXIF                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun ImageInfoPanel(
    file: FileItem,
    exif: ImageExif?,
    zoomPercent: Int,
    nativeRatio: Float,
    rotationDegrees: Int,
    mirrored: Boolean,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onWallpaper: () -> Unit,
    onCopyPath: () -> Unit,
    onProperties: () -> Unit,
    onExifScreen: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceSm),
        shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
        alpha = 0.46f,
        elevation = VennelaTokens.elevationOverlay
    ) {
        Column( modifier = Modifier .fillMaxSize() .verticalScroll(rememberScrollState()) .padding(VennelaTokens.spaceLg) ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Default.Image,
                    colors = listOf(accent.primary, accent.secondary),
                    size = VennelaTokens.badgeSm,
                    radius = VennelaTokens.radiusChip,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text( text = "Read from this file on this device", color = palette.textSecondary, fontSize = VennelaTokens.caption )
                }
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.touchTargetMin)
                        .clip(CircleShape)
                        .pressScale(pressedScale = 0.90f, onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hide image details",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))

            DetailRow(label = "Type", value = "${file.formatInfo.name} • ${file.extension.uppercase()}")
            DetailRow(label = "Size", value = file.formattedSize)
            DetailRow(label = "Modified", value = file.formattedDate)
            DetailRow(
                label = "Location",
                value = file.path.ifBlank { file.uri?.toString() ?: "Not exposed by this provider" }
            )

            DetailRow(label = "Zoom", value = "$zoomPercent% of fit")
            if (nativeRatio > 0f) {
                DetailRow(
                    label = "Pixels",
                    value = "${(nativeRatio * 100f).roundToInt()}% of native resolution"
                )
            }
            DetailRow(
                label = "View",
                value = "$rotationDegrees° rotation${if (mirrored) " • mirrored" else ""}"
            )
            DetailRow(
                label = "Screen",
                value = if (keepScreenOn) "Kept awake" else "Sleeps with the device"
            )

            when {
                exif == null -> {
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                    ImageExifSkeleton()
                }

                exif.dimensions == null && !exif.hasExifFields -> {
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                    Text(
                        text = "This file carries no readable EXIF block, so no camera, lens or GPS data can be shown.",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                    exif.dimensions?.let { DetailRow(label = "Dimensions", value = it) }
                    exif.megapixels?.let { DetailRow(label = "Resolution", value = it) }
                    exif.orientation?.let { DetailRow(label = "Orientation", value = it) }
                    exif.make?.let { DetailRow(label = "Camera make", value = it) }
                    exif.model?.let { DetailRow(label = "Camera model", value = it) }
                    exif.dateTaken?.let { DetailRow(label = "Captured", value = it) }
                    exif.exposure?.let { DetailRow(label = "Exposure", value = it) }
                    exif.aperture?.let { DetailRow(label = "Aperture", value = it) }
                    exif.iso?.let { DetailRow(label = "ISO", value = it) }
                    exif.focalLength?.let { DetailRow(label = "Focal length", value = it) }
                    exif.flash?.let { DetailRow(label = "Flash", value = it) }
                    exif.gps?.let { DetailRow(label = "GPS", value = it) }
                }
            }

            Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))

            Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                LiquidChip(text = "Share", icon = Icons.Default.Share, onClick = onShare)
                LiquidChip(text = "Open with", icon = Icons.AutoMirrored.Filled.OpenInNew, onClick = onOpenWith)
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                LiquidChip(text = "Wallpaper", icon = Icons.Default.Wallpaper, onClick = onWallpaper)
                LiquidChip(text = "Copy path", icon = Icons.Default.ContentCopy, onClick = onCopyPath)
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Row(horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                LiquidChip(
                    text = if (keepScreenOn) "Screen on" else "Keep awake",
                    icon = Icons.Default.Lightbulb,
                    isActive = keepScreenOn,
                    onClick = onToggleKeepScreenOn
                )
                LiquidChip(text = "Properties", icon = Icons.Default.Info, onClick = onProperties)
                LiquidChip(text = "EXIF view", icon = Icons.Default.AspectRatio, onClick = onExifScreen)
            }
        }
    }
}

@Composable
private fun ImageExifSkeleton() {
    val palette = LocalVennelaPalette.current
    val transition = rememberInfiniteTransition(label = "exifSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exifSkeletonAlpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
        listOf(0.82f, 0.64f, 0.72f, 0.5f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                    .background(palette.cardSurface.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueWidth: Dp = 78.dp) {
    val palette = LocalVennelaPalette.current
    Row( modifier = Modifier .fillMaxWidth() .padding(vertical = VennelaTokens.spaceXs), verticalAlignment = Alignment.CenterVertically ) {
        Text( text = label, color = palette.textSecondary, fontSize = VennelaTokens.caption, modifier = Modifier.width(valueWidth) )
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
