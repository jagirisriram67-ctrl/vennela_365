package com.vennela365.viewer.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vennela365.viewer.parser.CodeSyntaxHighlighter
import com.vennela365.viewer.parser.GeometryAndGeoParser
import com.vennela365.viewer.parser.StructuredDataParser
import com.vennela365.viewer.theme.*
import com.vennela365.viewer.ui.components.*
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/* ========================================================================= */
/* 1. Sandboxed SVG / SVGZ Vector Studio                                     */
/* ========================================================================= */

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxedSvgViewerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val docText by viewModel.documentText.collectAsState()

    val rawSource = docText.ifBlank { file?.sampleContent.orEmpty() }
    var darkCanvas by remember { mutableStateOf(true) }
    var showSource by remember { mutableStateOf(false) }
    var zoomPct by remember { mutableIntStateOf(100) }

    val inspection = remember(rawSource, darkCanvas) {
        StructuredDataParser.inspectAndSanitizeSvg(rawSource, darkBackdrop = darkCanvas)
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViewerTopBar(
                title = file?.name ?: "SVG Vector",
                subtitle = "Sandboxed • viewBox ${inspection.viewBox}",
                onBack = { viewModel.navigateBack() },
                leadingIcon = Icons.Default.Polyline,
                actions = {
                    ViewerIconAction(
                        icon = if (darkCanvas) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle SVG backdrop",
                        modifier = Modifier.testTag("svg_backdrop_toggle")
                    ) { darkCanvas = !darkCanvas }
                    ViewerIconAction(
                        icon = Icons.Default.Info,
                        contentDescription = "File metadata",
                        modifier = Modifier.testTag("svg_info_button")
                    ) { viewModel.navigateTo(Screen.FileMetadata) }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 880.dp),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = VennelaTokens.spaceSm,
                    bottom = navInset + VennelaTokens.spaceLg
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                item(key = "tabs") {
                    VennelaSegmentedControl(
                        options = listOf("Vector Canvas", "DOM & Source"),
                        selected = if (showSource) "DOM & Source" else "Vector Canvas",
                        onSelect = { showSource = it == "DOM & Source" }
                    )
                }

                if (rawSource.isBlank()) {
                    item(key = "empty") {
                        GlassCard {
                            VennelaEmptyState(
                                title = "No SVG vector loaded",
                                hint = "Open an .svg or .svgz vector graphic to render it safely offline with DOM & color inspection.",
                                icon = Icons.Default.Polyline,
                                actionLabel = "Browse files",
                                onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                            )
                        }
                    }
                } else if (!showSource) {
                    item(key = "canvas") {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp),
                            shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                            alpha = 0.48f
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.javaScriptEnabled = false
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                        @Suppress("DEPRECATION")
                                        settings.allowFileAccessFromFileURLs = false
                                        @Suppress("DEPRECATION")
                                        settings.allowUniversalAccessFromFileURLs = false
                                        settings.blockNetworkLoads = true
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        settings.useWideViewPort = true
                                        settings.loadWithOverviewMode = true
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean = true
                                        }
                                    }
                                },
                                update = { webView ->
                                    webView.setInitialScale(zoomPct)
                                    webView.loadDataWithBaseURL(
                                        null,
                                        inspection.sanitizedHtmlDocument,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            )
                        }
                    }

                    item(key = "zoom_presets") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            listOf(75, 100, 150, 225).forEach { pct ->
                                LiquidChip(
                                    text = "$pct%",
                                    isActive = zoomPct == pct,
                                    modifier = Modifier.weight(1f).testTag("svg_zoom_$pct"),
                                    onClick = { zoomPct = pct }
                                )
                            }
                        }
                    }
                } else {
                    item(key = "source_code") {
                        val highlighted = remember(inspection.rawSvg) {
                            CodeSyntaxHighlighter.highlight(inspection.rawSvg.take(25_000), "xml")
                        }
                        GlassCard(alpha = 0.56f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                                    .background(Color(0xFF0A0F1A))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = highlighted,
                                    fontFamily = VennelaFonts.Mono,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                if (rawSource.isNotBlank()) {
                    item(key = "telemetry") {
                        GlassCard(alpha = 0.50f) {
                            Text(
                                text = "Vector Telemetry (Scripts & External Links Stripped)",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Size: ${inspection.widthLabel} × ${inspection.heightLabel} • Paths: ${inspection.pathCount} • Shapes: ${inspection.shapeCount} • Groups: ${inspection.groupCount}",
                                color = palette.textSecondary,
                                fontFamily = VennelaFonts.Mono,
                                fontSize = VennelaTokens.caption
                            )
                            if (inspection.paletteHexes.isNotEmpty()) {
                                Spacer(Modifier.height(VennelaTokens.spaceSm))
                                Text(
                                    text = "Extracted Palette (Tap to copy hex)",
                                    color = palette.textTertiary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.caption
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    inspection.paletteHexes.forEach { hex ->
                                        val swatchColor = runCatching {
                                            val norm = if (hex.length == 4) {
                                                "#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}"
                                            } else hex
                                            Color(android.graphics.Color.parseColor(norm))
                                        }.getOrDefault(accent.primary)
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                                .background(palette.cardSurfaceStrong)
                                                .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusPill))
                                                .clickable {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cm.setPrimaryClip(ClipData.newPlainText("Color", hex))
                                                    Toast.makeText(context, "Copied $hex", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            )
                                            Text(
                                                text = hex,
                                                color = palette.textPrimary,
                                                fontFamily = VennelaFonts.Mono,
                                                fontSize = VennelaTokens.nano
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
    }
}

/* ========================================================================= */
/* 2. Real Animated GIF / WebP / APNG / AVIF Player                          */
/* ========================================================================= */

@Composable
fun LiveAnimatedImageScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()

    var playing by remember { mutableStateOf(true) }
    var darkCanvas by remember { mutableStateOf(true) }
    var drawable by remember { mutableStateOf<Drawable?>(null) }
    var dimensions by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file?.uri) {
        val uri = file?.uri
        if (uri == null) {
            drawable = null
            dimensions = ""
            errorMsg = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 28) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    val d = ImageDecoder.decodeDrawable(source)
                    drawable = d
                    dimensions = "${d.intrinsicWidth} × ${d.intrinsicHeight} px"
                    errorMsg = null
                } else {
                    val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    if (bmp != null) {
                        dimensions = "${bmp.width} × ${bmp.height} px"
                        drawable = android.graphics.drawable.BitmapDrawable(context.resources, bmp)
                        errorMsg = null
                    } else {
                        errorMsg = "Decoder returned no frames for this file."
                    }
                }
            }.onFailure { err ->
                errorMsg = err.message ?: "Could not decode animated image."
            }
        }
    }

    LaunchedEffect(drawable, playing) {
        if (Build.VERSION.SDK_INT >= 28) {
            val anim = drawable as? AnimatedImageDrawable
            if (anim != null) {
                if (playing) anim.start() else anim.stop()
            }
        }
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViewerTopBar(
                title = file?.name ?: "Animated Image",
                subtitle = dimensions.ifBlank { file?.formatInfo?.name ?: "GIF / WebP / APNG" },
                onBack = { viewModel.navigateBack() },
                leadingIcon = Icons.Default.Animation,
                actions = {
                    ViewerIconAction(
                        icon = if (darkCanvas) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle stage background",
                        modifier = Modifier.testTag("anim_backdrop_toggle")
                    ) { darkCanvas = !darkCanvas }
                    ViewerIconAction(
                        icon = Icons.Default.Info,
                        contentDescription = "Image details",
                        modifier = Modifier.testTag("anim_info_button")
                    ) { viewModel.navigateTo(Screen.FileMetadata) }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .padding(
                        start = VennelaTokens.gutter,
                        end = VennelaTokens.gutter,
                        top = VennelaTokens.spaceSm,
                        bottom = navInset + VennelaTokens.spaceLg
                    ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                if (file == null) {
                    GlassCard {
                        VennelaEmptyState(
                            title = "No animated image open",
                            hint = "Open a GIF, animated WebP, APNG, or AVIF image to inspect live playback.",
                            icon = Icons.Default.Animation,
                            actionLabel = "Browse files",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                } else {
                    GlassPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                            .background(if (darkCanvas) Color(0xFF0A0F1A) else Color(0xFFF8FAFC)),
                        alpha = 0.30f
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when {
                                errorMsg != null -> Text(
                                    text = errorMsg.orEmpty(),
                                    color = NeonCoral,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.bodySmall,
                                    modifier = Modifier.padding(16.dp)
                                )
                                drawable != null -> AndroidView(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.FIT_CENTER
                                        }
                                    },
                                    update = { img ->
                                        img.setImageDrawable(drawable)
                                    }
                                )
                                else -> Text(
                                    text = "Decoding frames…",
                                    color = palette.textSecondary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.bodySmall
                                )
                            }
                        }
                    }

                    val isAnimated = Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidButton(
                            label = if (playing) "Pause frames" else "Play frames",
                            icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f).testTag("anim_play_pause"),
                            onClick = { playing = !playing }
                        )
                        if (!isAnimated && drawable != null) {
                            Text(
                                text = "Static single frame",
                                color = palette.textTertiary,
                                fontFamily = VennelaFonts.Mono,
                                fontSize = VennelaTokens.caption
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ========================================================================= */
/* 3. Interactive Subtitle / Lyric / Playlist & Media Stream Timeline         */
/* ========================================================================= */

@Composable
fun TimedTrackReaderScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val info by viewModel.mediaInfo.collectAsState()
    val docText by viewModel.documentText.collectAsState()

    val source = docText.ifBlank { file?.sampleContent.orEmpty() }
    val ext = file?.extension?.lowercase().orEmpty()
    val cues = remember(source, ext) { StructuredDataParser.parseTimedTracks(source, ext) }
    var query by remember { mutableStateOf("") }

    val filteredCues = remember(cues, query) {
        if (query.isBlank()) cues
        else cues.filter {
            it.text.contains(query, true) || it.speakerOrMeta.contains(query, true) || it.timeRange.contains(query, true)
        }
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViewerTopBar(
                title = file?.name ?: "Tracks & Subtitles",
                subtitle = if (cues.isNotEmpty()) "${ext.uppercase()} • ${cues.size} cues" else "Media Streams",
                onBack = { viewModel.navigateBack() },
                leadingIcon = Icons.Default.Subtitles,
                actions = {
                    if (cues.isNotEmpty()) {
                        ViewerIconAction(
                            icon = Icons.Default.ContentCopy,
                            contentDescription = "Copy clean transcript",
                            modifier = Modifier.testTag("track_copy_transcript")
                        ) {
                            val transcript = cues.joinToString("\n") { "${it.timeRange}  ${it.text}" }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
                            Toast.makeText(context, "Transcript copied (${cues.size} lines)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = VennelaTokens.spaceSm,
                    bottom = navInset + VennelaTokens.spaceLg
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                when {
                    cues.isNotEmpty() -> {
                        item(key = "track_search") {
                            VennelaSearchField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = "Search dialogue, lyrics, or timestamps…",
                                modifier = Modifier.testTag("track_search_input")
                            )
                        }
                        items(filteredCues, key = { it.index }) { cue ->
                            GlassCard(alpha = 0.48f) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                                            .background(accent.primary.copy(alpha = 0.22f))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = cue.timeRange,
                                            color = accent.secondary,
                                            fontFamily = VennelaFonts.Mono,
                                            fontSize = VennelaTokens.nano,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (cue.speakerOrMeta.isNotBlank()) {
                                        Text(
                                            text = cue.speakerOrMeta,
                                            color = palette.textTertiary,
                                            fontFamily = VennelaFonts.Sans,
                                            fontSize = VennelaTokens.nano,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = cue.text,
                                    color = palette.textPrimary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.bodySmall,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    info.hasAudio || info.hasVideo -> {
                        item(key = "media_streams") {
                            GlassCard {
                                Text(
                                    text = file?.name ?: "Container Streams",
                                    color = palette.textPrimary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                if (info.hasVideo) {
                                    VennelaMetadataRow("Video track", info.resolutionLabel ?: "Active")
                                    GlassDivider()
                                }
                                if (info.hasAudio) {
                                    VennelaMetadataRow("Audio track", info.channelLabel ?: "Active")
                                    GlassDivider()
                                }
                                info.bitrateLabel?.let {
                                    VennelaMetadataRow("Bitrate", it)
                                }
                            }
                        }
                    }

                    else -> {
                        item(key = "empty_tracks") {
                            GlassCard {
                                VennelaEmptyState(
                                    title = "No cues or media streams",
                                    hint = "Open an .srt, .vtt, .ass, .lrc subtitle/lyric file or .m3u/.pls playlist to inspect cues.",
                                    icon = Icons.Default.Subtitles,
                                    actionLabel = "Browse files",
                                    onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ========================================================================= */
/* 4. Interactive 3D CAD / Mesh Wireframe Studio (OBJ / ASCII STL)           */
/* ========================================================================= */

@Composable
fun CadWireframeViewerScreen(viewModel: MainViewModel) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val docText by viewModel.documentText.collectAsState()

    val source = docText.ifBlank { file?.sampleContent.orEmpty() }
    val ext = file?.extension?.lowercase().orEmpty()
    val mesh = remember(source, ext) { GeometryAndGeoParser.parseMesh(source, ext) }

    var yaw by remember { mutableFloatStateOf(0.55f) }
    var pitch by remember { mutableFloatStateOf(0.35f) }
    var autoRotate by remember { mutableStateOf(true) }

    LaunchedEffect(autoRotate, mesh.edges.size) {
        if (autoRotate && mesh.edges.isNotEmpty()) {
            while (true) {
                delay(24)
                yaw += 0.015f
            }
        }
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViewerTopBar(
                title = file?.name ?: "3D / CAD Viewer",
                subtitle = "${mesh.formatLabel} • ${mesh.vertices.size} verts • ${mesh.faceCount} faces",
                onBack = { viewModel.navigateBack() },
                leadingIcon = Icons.Default.ViewInAr,
                actions = {
                    ViewerIconAction(
                        icon = Icons.Default.Memory,
                        contentDescription = "Hex inspector",
                        modifier = Modifier.testTag("cad_hex_button")
                    ) { viewModel.navigateTo(Screen.HexViewer) }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .padding(
                        start = VennelaTokens.gutter,
                        end = VennelaTokens.gutter,
                        top = VennelaTokens.spaceSm,
                        bottom = navInset + VennelaTokens.spaceLg
                    ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                if (file == null || mesh.isBinaryOrUnsupported || mesh.edges.isEmpty()) {
                    GlassCard {
                        Text(
                            text = file?.name ?: "3D / CAD Container",
                            color = palette.textPrimary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = VennelaTokens.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = mesh.notice ?: "Open an .obj or ASCII .stl file for interactive 3D wireframe rotation, or inspect headers safely.",
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Sans,
                            fontSize = VennelaTokens.bodySmall,
                            lineHeight = 20.sp
                        )
                    }
                    LiquidButton(
                        label = "Open Hex Inspector",
                        icon = Icons.Default.Memory,
                        modifier = Modifier.testTag("cad_open_hex"),
                        onClick = { viewModel.navigateTo(Screen.HexViewer) }
                    )
                } else {
                    GlassPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                            .background(Color(0xFF070B14))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    autoRotate = false
                                    yaw += dragAmount.x * 0.01f
                                    pitch = (pitch + dragAmount.y * 0.01f).coerceIn(-1.45f, 1.45f)
                                }
                            },
                        alpha = 0.25f
                    ) {
                        val lineColor = accent.secondary
                        val glowColor = accent.primary.copy(alpha = 0.35f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w * 0.5f
                            val cy = h * 0.5f
                            val scale = minOf(w, h) * 0.72f
                            val cosY = cos(yaw); val sinY = sin(yaw)
                            val cosP = cos(pitch); val sinP = sin(pitch)

                            // Subtle radial grid horizon
                            drawCircle(
                                color = Color.White.copy(alpha = 0.06f),
                                radius = scale * 0.65f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1f)
                            )

                            val projected = Array(mesh.vertices.size) { idx ->
                                val v = mesh.vertices[idx]
                                val x1 = v.x * cosY - v.z * sinY
                                val z1 = v.x * sinY + v.z * cosY
                                val y2 = v.y * cosP - z1 * sinP
                                val z2 = v.y * sinP + z1 * cosP
                                val perspective = 2.4f / (2.4f - z2.coerceIn(-1.2f, 1.2f))
                                Offset(cx + x1 * scale * perspective, cy - y2 * scale * perspective)
                            }

                            mesh.edges.forEach { edge ->
                                val p1 = projected[edge.a]
                                val p2 = projected[edge.b]
                                drawLine(glowColor, p1, p2, strokeWidth = 3.2f, cap = StrokeCap.Round)
                                drawLine(lineColor, p1, p2, strokeWidth = 1.4f, cap = StrokeCap.Round)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                    ) {
                        LiquidChip(
                            text = if (autoRotate) "Auto-Orbit ON" else "Auto-Orbit OFF",
                            icon = Icons.Default.ViewInAr,
                            isActive = autoRotate,
                            modifier = Modifier.weight(1f).testTag("cad_autorotate"),
                            onClick = { autoRotate = !autoRotate }
                        )
                        LiquidChip(
                            text = "Reset Angle",
                            icon = Icons.Default.CenterFocusStrong,
                            isActive = false,
                            modifier = Modifier.weight(1f).testTag("cad_reset_angle"),
                            onClick = {
                                yaw = 0.55f
                                pitch = 0.35f
                            }
                        )
                    }

                    GlassCard(alpha = 0.50f) {
                        val dx = mesh.maxBounds.x - mesh.minBounds.x
                        val dy = mesh.maxBounds.y - mesh.minBounds.y
                        val dz = mesh.maxBounds.z - mesh.minBounds.z
                        Text(
                            text = "Bounding Box: %.2f × %.2f × %.2f • Drag viewport to orbit 360°".format(dx, dy, dz),
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = VennelaTokens.caption
                        )
                    }
                }
            }
        }
    }
}

/* ========================================================================= */
/* 5. Interactive Geospatial Vector Plotter (GeoJSON / GPX / KML)            */
/* ========================================================================= */

@Composable
fun GeoVectorViewerScreen(viewModel: MainViewModel) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val docText by viewModel.documentText.collectAsState()

    val source = docText.ifBlank { file?.sampleContent.orEmpty() }
    val ext = file?.extension?.lowercase().orEmpty()
    val geo = remember(source, ext) { GeometryAndGeoParser.parseGeo(source, ext) }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ViewerTopBar(
                title = file?.name ?: "Geospatial Viewer",
                subtitle = "${geo.formatLabel} • ${geo.waypoints.size} pts • ${geo.polylines.size} paths",
                onBack = { viewModel.navigateBack() },
                leadingIcon = Icons.Default.Map,
                actions = {
                    ViewerIconAction(
                        icon = Icons.Default.Memory,
                        contentDescription = "Hex inspector",
                        modifier = Modifier.testTag("gis_hex_button")
                    ) { viewModel.navigateTo(Screen.HexViewer) }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 860.dp),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = VennelaTokens.spaceSm,
                    bottom = navInset + VennelaTokens.spaceLg
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                if (file == null || (geo.waypoints.isEmpty() && geo.polylines.isEmpty())) {
                    item(key = "geo_fallback") {
                        GlassCard {
                            Text(
                                text = geo.notice ?: "Open a GeoJSON, GPX track, or KML file to plot coordinates offline.",
                                color = palette.textSecondary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.bodySmall,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    item(key = "geo_hex_cta") {
                        LiquidButton(
                            label = "Inspect Raw Container",
                            icon = Icons.Default.Memory,
                            modifier = Modifier.testTag("gis_open_hex"),
                            onClick = { viewModel.navigateTo(Screen.HexViewer) }
                        )
                    }
                } else {
                    item(key = "geo_canvas") {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                                .background(Color(0xFF07111C)),
                            alpha = 0.28f
                        ) {
                            val trackColor = accent.secondary
                            val pointColor = NeonAmber
                            Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                val latSpan = (geo.maxLat - geo.minLat).coerceAtLeast(1e-5)
                                val lonSpan = (geo.maxLon - geo.minLon).coerceAtLeast(1e-5)

                                fun project(lat: Double, lon: Double): Offset {
                                    val nx = ((lon - geo.minLon) / lonSpan).toFloat()
                                    val ny = 1f - ((lat - geo.minLat) / latSpan).toFloat()
                                    return Offset(nx * size.width, ny * size.height)
                                }

                                // Coordinate grid lines
                                for (i in 0..4) {
                                    val f = i / 4f
                                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, size.height * f), Offset(size.width, size.height * f), 1f)
                                    drawLine(Color.White.copy(alpha = 0.08f), Offset(size.width * f, 0f), Offset(size.width * f, size.height), 1f)
                                }

                                geo.polylines.forEach { poly ->
                                    if (poly.points.size >= 2) {
                                        val path = Path()
                                        poly.points.forEachIndexed { idx, pt ->
                                            val off = project(pt.lat, pt.lon)
                                            if (idx == 0) path.moveTo(off.x, off.y) else path.lineTo(off.x, off.y)
                                        }
                                        if (poly.closed) path.close()
                                        drawPath(path, trackColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                                    }
                                }

                                geo.waypoints.forEach { pt ->
                                    val off = project(pt.lat, pt.lon)
                                    drawCircle(pointColor.copy(alpha = 0.35f), radius = 14f, center = off)
                                    drawCircle(pointColor, radius = 6.5f, center = off)
                                }
                            }
                        }
                    }

                    item(key = "geo_stats") {
                        GlassCard(alpha = 0.52f) {
                            Text(
                                text = "Spatial Summary (100% Offline Coordinate Plot)",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            if (geo.totalDistanceKm > 0.0) {
                                VennelaMetadataRow("Track length", "%.2f km (%.2f mi)".format(geo.totalDistanceKm, geo.totalDistanceKm * 0.621371))
                                GlassDivider()
                            }
                            if (geo.minEleMeters != null && geo.maxEleMeters != null) {
                                VennelaMetadataRow("Elevation range", "%.0f m – %.0f m".format(geo.minEleMeters, geo.maxEleMeters))
                                GlassDivider()
                            }
                            VennelaMetadataRow("Latitude bounds", "%.4f° .. %.4f°".format(geo.minLat, geo.maxLat))
                            GlassDivider()
                            VennelaMetadataRow("Longitude bounds", "%.4f° .. %.4f°".format(geo.minLon, geo.maxLon))
                        }
                    }

                    if (geo.waypoints.isNotEmpty()) {
                        item(key = "wpt_header") {
                            Text(
                                text = "Waypoints (${geo.waypoints.size})",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        items(geo.waypoints.take(40)) { wpt ->
                            GlassCard(alpha = 0.44f) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = wpt.label.ifBlank { "Waypoint" },
                                        color = palette.textPrimary,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "%.4f, %.4f".format(wpt.lat, wpt.lon),
                                        color = accent.secondary,
                                        fontFamily = VennelaFonts.Mono,
                                        fontSize = VennelaTokens.caption
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
