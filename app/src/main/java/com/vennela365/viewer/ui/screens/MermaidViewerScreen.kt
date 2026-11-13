package com.vennela365.viewer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vennela365.viewer.parser.DocumentTextExtractor
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaAccent
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.ReaderBottomPill
import com.vennela365.viewer.ui.components.ReaderChromeVisibility
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderProgressRule
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.VennelaBackground
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/* ------------------------------------------------------------------------- */
/* Tunables                                                                   */
/* ------------------------------------------------------------------------- */

/** Longest source the parser will look at; the rest is surfaced honestly. */
private const val MaxMermaidSourceChars = 200_000
private const val MaxMermaidNodes = 160
private const val MaxMermaidEdges = 320
private const val MaxMermaidSourceLines = 1_200
private const val MinZoom = 0.25f
private const val MaxZoom = 3f
private const val ZoomStep = 0.2f

/**
 * Mermaid diagram types this renderer actually understands. Anything else is
 * reported as unsupported instead of being drawn as a nonsense flowchart.
 */
private val MermaidDiagramHeads = listOf(
    "flowchart", "graph", "sequenceDiagram", "classDiagram", "stateDiagram",
    "stateDiagram-v2", "erDiagram", "gantt", "pie", "journey", "mindmap",
    "timeline", "gitGraph", "quadrantChart", "xychart", "C4Context",
    "requirementDiagram", "sankey", "block", "packet", "architecture", "kanban",
    "radar", "treemap", "zenuml"
)

// ---------------------------------------------------------------------------
// Mermaid flowchart subset
// ---------------------------------------------------------------------------

private data class MNode(val id: String, val label: String)
private data class MEdge(val from: String, val to: String, val label: String?)
private data class MGraph(
    val topDown: Boolean,
    val nodes: List<MNode>,
    val edges: List<MEdge>
) {
    val isEmpty: Boolean get() = nodes.isEmpty()
}

private object MermaidParser {

    private val directionRegex = Regex("^\\s*(?:graph|flowchart)\\s+(TD|TB|BT|LR|RL)\\b", RegexOption.IGNORE_CASE)
    private val arrowRegex = Regex("\\s*(-->|---|==>|-\\.->|->|--)\\s*(?:\\|([^|]*)\\|\\s*)?")
    private val nodeRegex = Regex("^(\\w+)\\s*(?:\\[(.*?)\\]|\\{(.*?)\\}|\\((.*?)\\))?$")
    private val skipPrefixes = listOf(
        "subgraph", "end", "style", "classdef", "class ", "click", "linkstyle", "direction", "%%"
    )

    fun parse(source: String): MGraph {
        val nodes = LinkedHashMap<String, MNode>()
        val edges = mutableListOf<MEdge>()
        var topDown = true

        source.replace("\r\n", "\n").replace('\r', '\n').lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            directionRegex.find(line)?.let { match ->
                val dir = match.groupValues[1].uppercase()
                topDown = dir == "TD" || dir == "TB" || dir == "BT"
                return@forEach
            }
            if (skipPrefixes.any { line.startsWith(it, ignoreCase = true) }) return@forEach

            val segments = arrowRegex.split(line)
            if (segments.size == 1) {
                // Standalone node definition
                parseNode(line)?.let { nodes[it.id] = it }
                return@forEach
            }

            var previous: MNode? = null
            var pendingLabel: String? = null
            segments.forEachIndexed { index, segment ->
                when (index % 3) {
                    0 -> parseNode(segment)?.let { node ->
                        nodes[node.id] = node
                        previous?.let { from ->
                            edges += MEdge(from.id, node.id, pendingLabel)
                        }
                        previous = node
                        pendingLabel = null
                    }

                    1 -> Unit // arrow token

                    else -> pendingLabel = segment.trim().takeIf { it.isNotEmpty() }
                }
            }
        }

        return MGraph(topDown, nodes.values.toList(), edges)
    }

    private fun parseNode(raw: String): MNode? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val match = nodeRegex.find(text) ?: return null
        val id = match.groupValues[1]
        if (id.isEmpty()) return null
        val label = match.groupValues.drop(2)
            .firstOrNull { it.isNotEmpty() }
            ?.replace("<br/>", " ")
            ?.replace("<br>", " ")
            ?.trim()
            ?.trim('"')
            ?: id
        return MNode(id, label)
    }
}

/** Layer assignment via longest-path from the roots. Cycles are tolerated. */
private fun layoutDepths(graph: MGraph): Map<String, Int> {
    val depth = mutableMapOf<String, Int>()
    val incoming = graph.edges.groupBy { it.to }
    val outgoing = graph.edges.groupBy { it.from }
    val roots = graph.nodes.filter { incoming[it.id].isNullOrEmpty() }.map { it.id }
    val start = roots.ifEmpty { graph.nodes.take(1).map { it.id } }

    val queue = ArrayDeque<String>()
    start.forEach { depth[it] = 0; queue.addLast(it) }
    var guard = 0
    while (queue.isNotEmpty() && guard++ < 5_000) {
        val id = queue.removeFirst()
        val current = depth[id] ?: 0
        outgoing[id].orEmpty().forEach { edge ->
            val candidate = current + 1
            val existing = depth[edge.to]
            if (existing == null || candidate > existing) {
                depth[edge.to] = candidate
                queue.addLast(edge.to)
            }
        }
    }
    graph.nodes.forEach { depth.putIfAbsent(it.id, 0) }
    return depth
}

/* ------------------------------------------------------------------------- */
/* Parsed content, built off the main thread                                  */
/* ------------------------------------------------------------------------- */

private data class MermaidContent(
    val source: String,
    val graph: MGraph,
    val layers: Map<Int, List<MNode>>,
    val kind: String,
    val truncated: Boolean,
    /** Set when the file is a diagram type this renderer does not implement. */
    val unsupported: String? = null,
    /** Set when a supported diagram could not be read at all. */
    val failure: String? = null
)

/** The first meaningful declaration in the file, e.g. `flowchart` or `pie`. */
private fun detectDiagramKind(source: String): String? {
    for (raw in source.lineSequence()) {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("%%") || line.all { it == '-' }) continue
        val head = line.substringBefore(' ').substringBefore('\t').trim()
        if (head.isEmpty()) continue
        return MermaidDiagramHeads.firstOrNull { it.equals(head, ignoreCase = true) }
    }
    return null
}

private fun buildMermaidContent(raw: String): MermaidContent {
    val source = if (raw.length > MaxMermaidSourceChars) raw.take(MaxMermaidSourceChars) else raw
    val kind = detectDiagramKind(source)

    if (kind != null && !kind.equals("graph", ignoreCase = true) &&
        !kind.equals("flowchart", ignoreCase = true)
    ) {
        return MermaidContent(
            source = source,
            graph = MGraph(true, emptyList(), emptyList()),
            layers = emptyMap(),
            kind = kind,
            truncated = false,
            unsupported = "This file is a $kind diagram. Vennela renders flowchart and graph " +
                "definitions; tap the source button below to read the definition, or open the " +
                "file with another app."
        )
    }

    val parsed = MermaidParser.parse(source)
    val truncated = parsed.nodes.size > MaxMermaidNodes || parsed.edges.size > MaxMermaidEdges
    val graph = if (!truncated) {
        parsed
    } else {
        val nodes = parsed.nodes.take(MaxMermaidNodes)
        val ids = nodes.mapTo(HashSet()) { it.id }
        parsed.copy(
            nodes = nodes,
            edges = parsed.edges
                .filter { it.from in ids && it.to in ids }
                .take(MaxMermaidEdges)
        )
    }
    val depths = layoutDepths(graph)
    val layers = graph.nodes.groupBy { depths[it.id] ?: 0 }.toSortedMap()
    val failure = if (graph.nodes.isEmpty()) {
        "No nodes could be read from this ${kind ?: "diagram"}. Vennela reads " +
            "“graph TD” / “flowchart LR” definitions built from node ids joined by arrows. " +
            "Tap the source button below to read what the file actually contains."
    } else {
        null
    }

    return MermaidContent(
        source = source,
        graph = graph,
        layers = layers,
        kind = kind ?: "diagram",
        truncated = truncated,
        failure = failure
    )
}

/* ------------------------------------------------------------------------- */
/* Diagram geometry                                                           */
/* ------------------------------------------------------------------------- */

private const val NODE_WIDTH_DP = 132f
private const val NODE_HEIGHT_DP = 52f
private const val GAP_MAIN_DP = 54f
private const val GAP_CROSS_DP = 18f

private fun diagramWidthDp(graph: MGraph, layers: Map<Int, List<MNode>>): Float {
    val layerCount = layers.keys.maxOrNull()?.plus(1) ?: 1
    val widestLayer = layers.values.maxOfOrNull { it.size } ?: 1
    return if (graph.topDown) {
        widestLayer * NODE_WIDTH_DP + (widestLayer - 1).coerceAtLeast(0) * GAP_CROSS_DP + 32f
    } else {
        layerCount * NODE_WIDTH_DP + (layerCount - 1).coerceAtLeast(0) * GAP_MAIN_DP + 32f
    }
}

private fun diagramHeightDp(graph: MGraph, layers: Map<Int, List<MNode>>): Float {
    val layerCount = layers.keys.maxOrNull()?.plus(1) ?: 1
    val widestLayer = layers.values.maxOfOrNull { it.size } ?: 1
    return if (graph.topDown) {
        layerCount * NODE_HEIGHT_DP + (layerCount - 1).coerceAtLeast(0) * GAP_MAIN_DP + 32f
    } else {
        widestLayer * NODE_HEIGHT_DP + (widestLayer - 1).coerceAtLeast(0) * GAP_CROSS_DP + 32f
    }
}

private fun widestLayerWidth(layers: Map<Int, List<MNode>>): Float =
    (layers.values.maxOfOrNull { it.size } ?: 1).let {
        it * NODE_WIDTH_DP + (it - 1).coerceAtLeast(0) * GAP_CROSS_DP
    }

private fun widestLayerHeight(layers: Map<Int, List<MNode>>): Float =
    (layers.values.maxOfOrNull { it.size } ?: 1).let {
        it * NODE_HEIGHT_DP + (it - 1).coerceAtLeast(0) * GAP_CROSS_DP
    }

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * Mermaid viewer, rebuilt on the content-first reader frame.
 *
 * The parser, depth layout and canvas renderer are the same real engine as
 * before. What changed is the frame: the diagram owns the whole viewport, and
 * a 30dp floating toolbar plus one tool pill carry every control — source
 * toggle, zoom, fit-to-width, fit-to-height, reset, share, open-with. Parsing,
 * layout and bounding now run off the main thread, and a diagram type the
 * renderer cannot draw gets an honest explanation instead of a blank canvas.
 */
@Composable
fun MermaidViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current
    val density = LocalDensity.current

    val file = selectedFile
    val fileId = file?.id

    var showCode by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var chromeVisible by remember { mutableStateOf(true) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var retryToken by remember(fileId) { mutableIntStateOf(0) }

    val diagramScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val sourceListState = rememberLazyListState()

    // The floating bars draw over the content, so the content only owes them a
    // modest inset rather than a reserved band.
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chromeTopInset = statusInset + VennelaTokens.viewerTopInset
    val pillClearance = VennelaTokens.spaceHuge + VennelaTokens.spaceXl

    /* ------------------------------ content ------------------------------ */

    // The view model inlines small files, but URI-only opens arrive with blank
    // sample content, so the screen reads those itself instead of lying about
    // the file being empty.
    var streamedSource by remember(fileId) { mutableStateOf<String?>(null) }
    var isReading by remember(fileId) { mutableStateOf(false) }

    LaunchedEffect(fileId, file?.uri, file?.sampleContent, retryToken) {
        val target = file
        if (target == null || target.uri == null || target.sampleContent.isNotBlank()) {
            streamedSource = null
            isReading = false
            return@LaunchedEffect
        }
        isReading = true
        streamedSource = runCatching {
            withContext(Dispatchers.IO) { DocumentTextExtractor.readPlainText(context, target.uri) }
        }.getOrNull()?.takeIf { it.isNotBlank() }
        isReading = false
    }

    val source = when {
        file == null -> ""
        file.sampleContent.isNotBlank() -> file.sampleContent
        streamedSource != null -> streamedSource.orEmpty()
        else -> ""
    }
    val hasSource = source.isNotBlank()

    var content by remember(source) { mutableStateOf<MermaidContent?>(null) }
    LaunchedEffect(source) {
        content = if (source.isBlank()) {
            null
        } else {
            withContext(Dispatchers.Default) { buildMermaidContent(source) }
        }
    }
    val model = content

    val allSourceLines = remember(source) { if (source.isEmpty()) emptyList() else source.lines() }
    val sourceLines = remember(allSourceLines) { allSourceLines.take(MaxMermaidSourceLines) }
    val sourceLineCount = allSourceLines.size

    val contentWidthDp = remember(model) { model?.let { diagramWidthDp(it.graph, it.layers) } ?: 0f }
    val contentHeightDp = remember(model) { model?.let { diagramHeightDp(it.graph, it.layers) } ?: 0f }

    /* ----------------------------- fit + zoom ---------------------------- */

    fun fitScale(useWidth: Boolean): Float {
        val viewportPx = if (useWidth) viewport.width else viewport.height
        val contentDp = if (useWidth) contentWidthDp else contentHeightDp
        if (viewportPx <= 0 || contentDp <= 0f) return 1f
        val contentPx = with(density) { contentDp.dp.toPx() }
        if (contentPx <= 0f) return 1f
        return (viewportPx / contentPx).coerceIn(MinZoom, MaxZoom)
    }

    fun zoomBy(delta: Float) {
        zoom = (zoom + delta).coerceIn(MinZoom, MaxZoom)
        chromeVisible = true
    }

    /* --------------------------- progress rule --------------------------- */

    val sourceProgress = if (sourceLines.size <= 1) {
        0f
    } else {
        // The list reserves item 0 for its header, so the first source row is 1.
        val row = (sourceListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
        (row.toFloat() / (sourceLines.size - 1).toFloat()).coerceIn(0f, 1f)
    }
    val diagramProgress = if (diagramScroll.maxValue > 0) {
        (diagramScroll.value.toFloat() / diagramScroll.maxValue.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val showProgress = if (showCode) sourceLines.size > 1 else diagramScroll.maxValue > 0

    /* --------------------- chrome hides while reading ------------------- */

    LaunchedEffect(sourceListState, showCode) {
        if (!showCode) return@LaunchedEffect
        snapshotFlow {
            Triple(
                sourceListState.isScrollInProgress,
                sourceListState.lastScrolledBackward,
                sourceListState.firstVisibleItemIndex == 0 &&
                    sourceListState.firstVisibleItemScrollOffset == 0
            )
        }.collect { (scrolling, backward, atTop) ->
            if (atTop) chromeVisible = true
            else if (scrolling) chromeVisible = backward
        }
    }

    LaunchedEffect(diagramScroll, showCode) {
        if (showCode) return@LaunchedEffect
        snapshotFlow { diagramScroll.isScrollInProgress to diagramScroll.lastScrolledBackward }
            .collect { (scrolling, backward) ->
                if (scrolling) chromeVisible = backward
                else if (diagramScroll.value == 0) chromeVisible = true
            }
    }

    /* ------------------------------ actions ----------------------------- */

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun shareFile() {
        val target = file ?: return toast("There is no file to share")
        val intent = Intent(Intent.ACTION_SEND)
            .setType(target.formatInfo.mimeType)
            .putExtra(Intent.EXTRA_SUBJECT, target.name)
        if (!ShareUri.attachStream(context, intent, target.uri)) {
            toast("This file's location cannot be handed to another app")
            return
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share ${target.name}")) }
            .onFailure { toast("No app accepted this file") }
    }

    fun openWith() {
        val target = file ?: return toast("There is no file to open")
        val intent = ShareUri.viewIntent(context, target.uri, target.formatInfo.mimeType)
        if (intent == null) {
            toast("This file's location cannot be handed to another app")
            return
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open with")) }
            .onFailure { toast("No app on this device can open this file") }
    }

    val subtitle = when {
        showCode -> "Source  •  $sourceLineCount lines"
        model != null && model.unsupported == null && model.failure == null -> buildString {
            append(model.kind)
            append("  •  ")
            append(model.graph.nodes.size)
            append(" nodes, ")
            append(model.graph.edges.size)
            append(" links")
            // Direction is what the layout actually did, so it cannot lie.
            append(if (model.graph.topDown) "  •  ↕" else "  •  ↔")
            if (model.truncated) append("  •  trimmed")
        }

        else -> "Mermaid diagram"
    }

    /* ------------------------------- frame ------------------------------- */

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            val state = viewerState
            when {
                file == null -> MermaidStateFrame(chromeTopInset) {
                    VennelaEmptyState(
                        title = "No diagram open",
                        hint = "Open a .mmd or .mermaid file, or a Markdown file containing a " +
                            "mermaid code block.",
                        icon = Icons.Default.AccountTree
                    )
                }

                isReading || state is ViewerState.Loading || state is ViewerState.Detecting ->
                    MermaidStateFrame(chromeTopInset) {
                        VennelaLoadingState(
                            label = "Reading ${file?.name ?: "diagram"}…",
                            progress = (state as? ViewerState.Loading)?.progress
                        )
                    }

                state is ViewerState.Failed -> MermaidStateFrame(chromeTopInset) {
                    VennelaErrorState(
                        title = "Couldn't read this diagram",
                        message = state.reason,
                        onRetry = { viewModel.openFile(file) },
                        onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                !hasSource -> MermaidStateFrame(chromeTopInset) {
                    VennelaEmptyState(
                        title = "This file is empty",
                        hint = "${file?.name ?: "The file"} has no readable mermaid source on " +
                            "this device. The original file is untouched.",
                        icon = Icons.Default.Code,
                        actionLabel = "File properties",
                        onAction = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                showCode -> MermaidSourceSurface(
                    lines = sourceLines,
                    listState = sourceListState,
                    topInset = chromeTopInset,
                    bottomInset = pillClearance,
                    truncatedAt = if (sourceLineCount > sourceLines.size) sourceLines.size else null,
                    totalLines = sourceLineCount
                )

                model == null -> MermaidStateFrame(chromeTopInset) {
                    VennelaLoadingState(label = "Laying out the diagram…")
                }

                model.unsupported != null -> MermaidStateFrame(chromeTopInset) {
                    VennelaEmptyState(
                        title = "This diagram type isn't rendered here",
                        hint = model.unsupported.orEmpty(),
                        icon = Icons.Default.AccountTree,
                        actionLabel = "Read source",
                        onAction = { showCode = true }
                    )
                }

                model.failure != null -> MermaidStateFrame(chromeTopInset) {
                    VennelaErrorState(
                        title = "Couldn't render this diagram",
                        message = model.failure.orEmpty(),
                        onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                else -> MermaidDiagramSurface(
                    graph = model.graph,
                    layers = model.layers,
                    scale = zoom,
                    topInset = chromeTopInset,
                    bottomInset = pillClearance,
                    verticalScroll = diagramScroll,
                    horizontalScroll = horizontalScroll,
                    viewportSize = viewport,
                    onViewport = { viewport = it }
                )
            }

            // Hairline progress, Google Docs style — only when there is more
            // content than the viewport, so it never sits at a fake 0%.
            if (showProgress) {
                ReaderProgressRule(
                    progress = if (showCode) sourceProgress else diagramProgress,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // Floating top bar — auto-hides while scrolling down.
            ReaderChromeVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopToolbar(
                    title = file?.name ?: "Diagram",
                    subtitle = subtitle,
                    onBack = { viewModel.navigateBack() }
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share this file",
                        onClick = { shareFile() }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open with another app",
                        onClick = { openWith() }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "File properties",
                        onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }
            }

            // Floating tool pill — also auto-hides.
            ReaderChromeVisibility(
                visible = chromeVisible,
                fromTop = false,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomPill(
                    status = if (showCode) null else "${(zoom * 100).roundToInt()}%"
                ) {
                    ReaderIconButton(
                        icon = if (showCode) Icons.Default.AccountTree else Icons.Default.Code,
                        contentDescription = if (showCode) "Show the diagram" else "Show the source",
                        modifier = Modifier.testTag("mermaid_source_button"),
                        active = showCode,
                        onClick = {
                            showCode = !showCode
                            chromeVisible = true
                        }
                    )
                    // Zoom belongs to the canvas; in source mode the pill stays
                    // as small as possible and just carries the way back.
                    if (!showCode) {
                        ReaderIconButton(
                            icon = Icons.Default.ZoomOut,
                            contentDescription = "Zoom out",
                            modifier = Modifier.testTag("mermaid_zoom_out_button"),
                            onClick = { zoomBy(-ZoomStep) }
                        )
                        ReaderIconButton(
                            icon = Icons.Default.ZoomIn,
                            contentDescription = "Zoom in",
                            modifier = Modifier.testTag("mermaid_zoom_in_button"),
                            onClick = { zoomBy(ZoomStep) }
                        )
                        ReaderIconButton(
                            icon = Icons.Default.AspectRatio,
                            contentDescription = "Fit diagram to width",
                            modifier = Modifier.testTag("mermaid_fit_width_button"),
                            active = zoom <= fitScale(useWidth = true) + 0.001f,
                            onClick = {
                                zoom = fitScale(useWidth = true)
                                chromeVisible = true
                            }
                        )
                        ReaderIconButton(
                            icon = Icons.Default.SwapVert,
                            contentDescription = "Fit diagram to height",
                            modifier = Modifier.testTag("mermaid_fit_height_button"),
                            onClick = {
                                zoom = fitScale(useWidth = false)
                                chromeVisible = true
                            }
                        )
                        ReaderIconButton(
                            icon = Icons.Default.FitScreen,
                            contentDescription = "Reset zoom to 100%",
                            modifier = Modifier.testTag("mermaid_reset_zoom_button"),
                            onClick = {
                                zoom = 1f
                                chromeVisible = true
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Centred state frame that still clears the floating bar. */
@Composable
private fun MermaidStateFrame(
    topInset: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VennelaTokens.gutter)
            .padding(top = topInset, bottom = VennelaTokens.spaceHuge),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/* ------------------------------------------------------------------------- */
/* Source view                                                                */
/* ------------------------------------------------------------------------- */

/**
 * The raw mermaid definition, virtualised: only the visible rows are laid out,
 * and both the row count and the line length are bounded so a huge file cannot
 * stall the frame.
 */
@Composable
private fun MermaidSourceSurface(
    lines: List<String>,
    listState: LazyListState,
    topInset: Dp,
    bottomInset: Dp,
    truncatedAt: Int?,
    totalLines: Int
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val lineHeight = VennelaTokens.caption * 1.55f

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            start = VennelaTokens.gutter,
            end = VennelaTokens.gutter,
            top = topInset + VennelaTokens.spaceXs,
            bottom = bottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXxs)
    ) {
        item(key = "mermaid_source_head") {
            Row(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth()
                    .padding(bottom = VennelaTokens.spaceXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(accent.primary.copy(alpha = 0.16f))
                        .padding(
                            horizontal = VennelaTokens.spaceSm,
                            vertical = VennelaTokens.spaceXxs
                        )
                ) {
                    Text(
                        text = "MERMAID SOURCE",
                        color = accent.primary,
                        fontSize = VennelaTokens.nano,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Text(
                    text = "$totalLines lines",
                    color = palette.textTertiary,
                    fontFamily = VennelaFonts.Mono,
                    fontSize = VennelaTokens.nano,
                    maxLines = 1
                )
            }
        }

        items(count = lines.size, key = { index -> index }) { index ->
            Row(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro,
                    fontFamily = VennelaFonts.Mono,
                    lineHeight = lineHeight,
                    maxLines = 1,
                    modifier = Modifier
                        .width(VennelaTokens.badgeSm)
                        .padding(end = VennelaTokens.spaceSm)
                )
                Text(
                    text = lines[index],
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.caption,
                    fontFamily = VennelaFonts.Mono,
                    lineHeight = lineHeight,
                    softWrap = true,
                    modifier = Modifier.widthIn(max = 760.dp)
                )
            }
        }

        if (truncatedAt != null) {
            item(key = "mermaid_source_truncated") {
                Text(
                    text = "Showing the first $truncatedAt of $totalLines source lines. " +
                        "Share the file to read the rest.",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro,
                    modifier = Modifier.padding(top = VennelaTokens.spaceSm)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Diagram surface                                                            */
/* ------------------------------------------------------------------------- */

/**
 * Pan-and-zoom viewport. The diagram is laid out once at its natural size and
 * the same scroll states drive both the pan and the chrome auto-hide, so
 * dragging the canvas forward slides the bars away exactly like a document.
 */
@Composable
private fun MermaidDiagramSurface(
    graph: MGraph,
    layers: Map<Int, List<MNode>>,
    scale: Float,
    topInset: Dp,
    bottomInset: Dp,
    verticalScroll: ScrollState,
    horizontalScroll: ScrollState,
    viewportSize: IntSize,
    onViewport: (IntSize) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val density = LocalDensity.current

    // The scroll container is always at least a viewport big, so a diagram that
    // is smaller than the screen sits centred instead of pinned to the corner.
    val canvasWidth = maxOf(
        (diagramWidthDp(graph, layers) * scale).dp,
        with(density) { viewportSize.width.toDp() }
    )
    val canvasHeight = maxOf(
        (diagramHeightDp(graph, layers) * scale).dp,
        with(density) { viewportSize.height.toDp() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topInset, bottom = bottomInset)
            .padding(horizontal = VennelaTokens.gutter)
            .onSizeChanged(onViewport)
            .horizontalScroll(horizontalScroll)
            .verticalScroll(verticalScroll)
    ) {
        Box(
            modifier = Modifier.size(canvasWidth, canvasHeight),
            contentAlignment = Alignment.Center
        ) {
            MermaidDiagram(
                graph = graph,
                layers = layers,
                scale = scale,
                palette = palette,
                accent = accent
            )
        }
    }
}

@Composable
private fun MermaidDiagram(
    graph: MGraph,
    layers: Map<Int, List<MNode>>,
    scale: Float,
    palette: VennelaPalette,
    accent: VennelaAccent
) {
    val contentWidth = diagramWidthDp(graph, layers)
    val contentHeight = diagramHeightDp(graph, layers)

    // Glass specs are composable values, so they are hoisted once per diagram
    // instead of being rebuilt for every node and edge label.
    val nodeSpec = liquidGlassSpec(
        alpha = 0.34f,
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationRaised,
        sheen = 0.12f
    )
    val labelSpec = liquidGlassSpec(
        alpha = 0.50f,
        frost = VennelaTokens.elevationFlat,
        elevation = VennelaTokens.elevationFlat,
        sheen = 0.10f
    )

    // id -> centre point, in unscaled dp coordinates
    val positions = remember(graph, layers) {
        val map = mutableMapOf<String, Offset>()
        layers.forEach { (layerIndex, nodesInLayer) ->
            nodesInLayer.forEachIndexed { indexInLayer, node ->
                val x: Float
                val y: Float
                if (graph.topDown) {
                    val layerWidth = nodesInLayer.size * NODE_WIDTH_DP +
                        (nodesInLayer.size - 1).coerceAtLeast(0) * GAP_CROSS_DP
                    val startX = 16f + (widestLayerWidth(layers) - layerWidth) / 2f
                    x = startX + indexInLayer * (NODE_WIDTH_DP + GAP_CROSS_DP) + NODE_WIDTH_DP / 2f
                    y = 16f + layerIndex * (NODE_HEIGHT_DP + GAP_MAIN_DP) + NODE_HEIGHT_DP / 2f
                } else {
                    val layerHeight = nodesInLayer.size * NODE_HEIGHT_DP +
                        (nodesInLayer.size - 1).coerceAtLeast(0) * GAP_CROSS_DP
                    val startY = 16f + (widestLayerHeight(layers) - layerHeight) / 2f
                    x = 16f + layerIndex * (NODE_WIDTH_DP + GAP_MAIN_DP) + NODE_WIDTH_DP / 2f
                    y = startY + indexInLayer * (NODE_HEIGHT_DP + GAP_CROSS_DP) + NODE_HEIGHT_DP / 2f
                }
                map[node.id] = Offset(x, y)
            }
        }
        map
    }

    Box(
        modifier = Modifier.size((contentWidth * scale).dp, (contentHeight * scale).dp)
    ) {
        Box(
            modifier = Modifier
                .size(contentWidth.dp, contentHeight.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            // Edges
            Canvas(modifier = Modifier.matchParentSize()) {
                val px = density
                graph.edges.forEach { edge ->
                    val from = positions[edge.from] ?: return@forEach
                    val to = positions[edge.to] ?: return@forEach
                    val start: Offset
                    val end: Offset
                    if (graph.topDown) {
                        start = Offset(from.x * px, (from.y + NODE_HEIGHT_DP / 2f) * px)
                        end = Offset(to.x * px, (to.y - NODE_HEIGHT_DP / 2f) * px)
                    } else {
                        start = Offset((from.x + NODE_WIDTH_DP / 2f) * px, from.y * px)
                        end = Offset((to.x - NODE_WIDTH_DP / 2f) * px, to.y * px)
                    }

                    drawLine(
                        color = accent.secondary.copy(alpha = 0.55f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx()
                    )

                    // Arrow head
                    val direction = Offset(end.x - start.x, end.y - start.y)
                    val length = kotlin.math.hypot(direction.x, direction.y).coerceAtLeast(0.001f)
                    val unit = Offset(direction.x / length, direction.y / length)
                    val normal = Offset(-unit.y, unit.x)
                    val headLength = 12.dp.toPx()
                    val headWidth = 6.dp.toPx()
                    val tip = end
                    val base = Offset(tip.x - unit.x * headLength, tip.y - unit.y * headLength)
                    val path = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(base.x + normal.x * headWidth, base.y + normal.y * headWidth)
                        lineTo(base.x - normal.x * headWidth, base.y - normal.y * headWidth)
                        close()
                    }
                    drawPath(path, color = accent.secondary.copy(alpha = 0.8f))
                    drawPath(path, color = accent.secondary.copy(alpha = 0.35f), style = Stroke(width = 1.dp.toPx()))
                }
            }

            // Edge labels
            graph.edges.forEach { edge ->
                val label = edge.label ?: return@forEach
                val from = positions[edge.from] ?: return@forEach
                val to = positions[edge.to] ?: return@forEach
                val midX = (from.x + to.x) / 2f
                val midY = (from.y + to.y) / 2f
                Box(
                    modifier = Modifier
                        .offset(x = (midX - 30f).dp, y = (midY - 10f).dp)
                        .widthIn(max = 96.dp)
                        .liquidGlass(labelSpec, RoundedCornerShape(VennelaTokens.radiusChip))
                        .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXxs)
                ) {
                    Text(
                        text = label,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.nano,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Nodes
            graph.nodes.forEach { node ->
                val position = positions[node.id] ?: return@forEach
                Box(
                    modifier = Modifier
                        .offset(
                            x = (position.x - NODE_WIDTH_DP / 2f).dp,
                            y = (position.y - NODE_HEIGHT_DP / 2f).dp
                        )
                        .width(NODE_WIDTH_DP.dp)
                        .height(NODE_HEIGHT_DP.dp)
                        .liquidGlass(nodeSpec, RoundedCornerShape(VennelaTokens.radiusControl))
                        .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.label,
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.caption,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
