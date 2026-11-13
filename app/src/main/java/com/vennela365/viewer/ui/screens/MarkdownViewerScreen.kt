package com.vennela365.viewer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.DocumentTextExtractor
import com.vennela365.viewer.parser.MarkdownParser
import com.vennela365.viewer.parser.MdBlock
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaAccent
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.applyViewerColorMode
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.MarkdownBlockView
import com.vennela365.viewer.ui.components.ReaderBottomPill
import com.vennela365.viewer.ui.components.ReaderChromeVisibility
import com.vennela365.viewer.ui.components.ReaderColorOption
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderPaper
import com.vennela365.viewer.ui.components.ReaderProgressRule
import com.vennela365.viewer.ui.components.ReaderSettingsSheet
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.VennelaBackground
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/* ------------------------------------------------------------------------- */
/* Model                                                                      */
/* ------------------------------------------------------------------------- */

private data class MdStats(val lines: Int, val words: Int, val characters: Int)

private data class MdHeading(val blockIndex: Int, val lineIndex: Int, val level: Int, val title: String)

private data class MdContent(
    val fileId: String?,
    val source: String,
    val blocks: List<MdBlock>,
    val outline: List<MdHeading>,
    val stats: MdStats,
    val error: String? = null
) {
    val hasBody: Boolean get() = error == null && source.isNotBlank()
}

private data class MdOccurrence(val blockIndex: Int, val start: Int, val end: Int)

/** A real hit inside the raw source, used by the source view's inline highlight. */
private data class SourceMatch(val lineIndex: Int, val start: Int, val end: Int)

private object MarkdownCache {
    private const val MaxCachedSourceChars = 400_000
    private val content = LruCache<String, MdContent>(4)

    fun get(fileId: String?): MdContent? = fileId?.let { content.get(it) }

    fun put(value: MdContent) {
        val id = value.fileId ?: return
        if (value.error != null) return
        if (value.source.length > MaxCachedSourceChars) return
        content.put(id, value)
    }
}

/** Where the reader left off, so reopening the document resumes exactly there. */
private data class ReadingPosition(val index: Int, val offset: Int)

private object ReadingPositions {
    private const val MaxEntries = 24
    private val positions = LinkedHashMap<String, ReadingPosition>()

    fun get(fileId: String?): ReadingPosition =
        fileId?.let { positions[it] } ?: ReadingPosition(0, 0)

    fun put(fileId: String?, position: ReadingPosition) {
        val id = fileId ?: return
        positions.remove(id)
        positions[id] = position
        while (positions.size > MaxEntries) {
            val oldest = positions.keys.firstOrNull() ?: break
            positions.remove(oldest)
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Tunables                                                                   */
/* ------------------------------------------------------------------------- */

private const val MaxSourceLines = 1200

/** Longest slice of a single source line the code view will lay out. */
private const val MaxCharsPerSourceLine = 2_000
private const val MaxMatches = 400
private const val SourceOverscan = 24
private const val FontScaleMin = 0.80f
private const val FontScaleMax = 1.80f
private const val SpacingMin = 0.80f
private const val SpacingMax = 2.20f
private const val FontScaleStep = 0.10f
private const val SpacingStep = 0.10f

private val GutterWidth = VennelaTokens.badgeLg + VennelaTokens.spaceSm
private val SourceLineStep = VennelaTokens.spaceXl

private val MdThemes = listOf(
    ReaderColorOption("Normal", ViewerColorMode.ORIGINAL, Icons.Default.Visibility),
    ReaderColorOption("Sepia", ViewerColorMode.SEPIA, Icons.AutoMirrored.Filled.MenuBook),
    ReaderColorOption("Night", ViewerColorMode.OLED_PITCH, Icons.Default.NightlightRound),
    ReaderColorOption("High contrast", ViewerColorMode.SAUVOLA_BW, Icons.Default.WarningAmber)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/* ------------------------------------------------------------------------- */
/* Content building (off the main thread)                                     */
/* ------------------------------------------------------------------------- */

private fun buildContent(fileId: String?, source: String): MdContent {
    val blocks = MarkdownParser.parse(source)
    val headingBlocks = blocks.mapIndexedNotNull { index, block ->
        (block as? MdBlock.Heading)?.let { Triple(index, it.level, it.text) }
    }
    val lineIndices = headingLineIndices(source, headingBlocks.map { it.third })
    val outline = headingBlocks.mapIndexed { order, (blockIndex, level, title) ->
        MdHeading(
            blockIndex = blockIndex,
            lineIndex = lineIndices.getOrElse(order) { 0 },
            level = level,
            title = title
        )
    }
    val words = source.split(Regex("\\s+")).count { it.isNotBlank() }
    return MdContent(
        fileId = fileId,
        source = source,
        blocks = blocks,
        outline = outline,
        stats = MdStats(
            lines = if (source.isEmpty()) 0 else source.lines().size,
            words = words,
            characters = source.length
        )
    )
}

/**
 * Pairs parsed heading blocks with their source line, monotonically. Real
 * line numbers only — the cursor never moves backwards, so an ambiguous
 * heading title still resolves to a plausible, ordered line.
 */
private fun headingLineIndices(source: String, titles: List<String>): IntArray {
    if (titles.isEmpty()) return IntArray(0)
    val lines = source.lines()
    var cursor = 0
    return IntArray(titles.size) { order ->
        val target = titles[order].trim().trimEnd('#').trim()
        var found = -1
        var index = cursor
        while (index < lines.size) {
            val candidate = lines[index].trim().removePrefix("#").trim().trimEnd('#').trim()
            if (candidate == target) {
                found = index
                break
            }
            index++
        }
        if (found >= 0) cursor = found + 1
        if (found < 0) cursor else found
    }
}

/** Real block-level occurrences of [query], in reading order. */
private fun findOccurrences(blocks: List<MdBlock>, query: String): List<MdOccurrence> {
    if (query.isBlank()) return emptyList()
    val out = ArrayList<MdOccurrence>(32)
    blocks.forEachIndexed { blockIndex, block ->
        val text = blockPlainText(block)
        if (text.isEmpty()) return@forEachIndexed
        var start = text.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            if (out.size >= MaxMatches) return out
            out += MdOccurrence(blockIndex, start, start + query.length)
            start = text.indexOf(query, start + query.length, ignoreCase = true)
        }
    }
    return out
}

private fun blockPlainText(block: MdBlock): String = when (block) {
    is MdBlock.Heading -> block.text
    is MdBlock.Paragraph -> block.spans.joinToString(" ") { it.text }
    is MdBlock.Bullet -> block.spans.joinToString(" ") { it.text }
    is MdBlock.Ordered -> "${block.number}. ${block.spans.joinToString(" ") { it.text }}"
    is MdBlock.Code -> block.code
    is MdBlock.Quote -> block.spans.joinToString(" ") { it.text }
    is MdBlock.Table -> (block.header + block.rows.flatten()).joinToString(" ")
    MdBlock.Rule -> ""
}

/** Real line-level hits in the raw source, in document order. */
private fun findLineMatches(source: String, query: String): List<SourceMatch> {
    if (query.isBlank()) return emptyList()
    val out = ArrayList<SourceMatch>(32)
    source.lines().forEachIndexed { lineIndex, line ->
        var start = line.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            if (out.size >= MaxMatches) return out
            out += SourceMatch(lineIndex, start, start + query.length)
            start = line.indexOf(query, start + query.length, ignoreCase = true)
        }
    }
    return out
}

/** Inline highlight of the real hits in one source line. */
private fun annotateSourceLine(
    line: String,
    lineIndex: Int,
    matches: List<SourceMatch>,
    activeMatch: Int,
    accent: VennelaAccent
): AnnotatedString {
    if (matches.isEmpty()) return AnnotatedString(line)
    val hits = matches.withIndex().filter { it.value.lineIndex == lineIndex }
    if (hits.isEmpty()) return AnnotatedString(line)
    return buildAnnotatedString {
        var cursor = 0
        for ((ordinal, hit) in hits) {
            if (hit.start < cursor || hit.end > line.length) continue
            append(line.substring(cursor, hit.start))
            val isActive = ordinal == activeMatch
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = if (isActive) accent.secondary else accent.primary.copy(alpha = 0.55f),
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            ) {
                append(line.substring(hit.start, hit.end))
            }
            cursor = hit.end
        }
        if (cursor < line.length) append(line.substring(cursor))
    }
}

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
fun MarkdownViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val fileId = selectedFile?.id
    var retryToken by remember(fileId) { mutableStateOf(0) }

    var isRawMode by remember { mutableStateOf(false) }
    var isTocOpen by remember { mutableStateOf(false) }
    var isInfoOpen by remember { mutableStateOf(false) }
    var isFindOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var activeMatch by remember { mutableStateOf(0) }
    var fontScale by remember(fileId) { mutableStateOf(1f) }
    var spacing by remember(fileId) { mutableStateOf(1f) }
    var wrapSource by remember(fileId) { mutableStateOf(false) }
    var keepAwake by remember(fileId) { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val sourceScroll = rememberScrollState()
    var chromeVisible by remember { mutableStateOf(true) }

    /* ------------------------------ content ------------------------------ */

    val content by produceState<MdContent?>(
        initialValue = MarkdownCache.get(fileId),
        fileId,
        selectedFile?.sampleContent,
        selectedFile?.uri,
        retryToken
    ) {
        val file = selectedFile
        if (file == null) {
            value = null
            return@produceState
        }
        val cached = MarkdownCache.get(fileId)
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = try {
            val text = withContext(Dispatchers.IO) {
                if (file.sampleContent.isNotBlank()) file.sampleContent
                else DocumentTextExtractor.readPlainText(context, file.uri)
            }
            buildContent(fileId, text).also(MarkdownCache::put)
        } catch (error: Throwable) {
            MdContent(
                fileId = fileId,
                source = "",
                blocks = emptyList(),
                outline = emptyList(),
                stats = MdStats(0, 0, 0),
                error = error.message ?: "This document could not be read as text."
            )
        }
    }

    val ready = content?.takeIf { it.fileId == fileId }
    val source = ready?.source.orEmpty()
    val blocks = ready?.blocks.orEmpty()
    val outline = ready?.outline.orEmpty()
    val stats = ready?.stats ?: MdStats(0, 0, 0)
    val isParsing = ready == null && selectedFile != null

    /* ------------------------------ find --------------------------------- */

    val occurrences = remember(blocks, findQuery) {
        if (findQuery.isBlank()) emptyList() else findOccurrences(blocks, findQuery)
    }
    val lineMatches = remember(source, findQuery) {
        if (findQuery.isBlank()) emptyList() else findLineMatches(source, findQuery)
    }
    val matchCount = if (isRawMode) lineMatches.size else occurrences.size
    LaunchedEffect(occurrences, lineMatches, isRawMode) { activeMatch = 0 }

    /* --------------------------- progress ------------------------------- */

    val totalItems = remember(blocks.size) { blocks.size.coerceAtLeast(1) }
    val firstVisible = listState.firstVisibleItemIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
    val progress = if (isRawMode) {
        if (sourceScroll.maxValue > 0) {
            sourceScroll.value.toFloat() / sourceScroll.maxValue.toFloat()
        } else {
            0f
        }
    } else {
        (firstVisible + 1f) / totalItems.toFloat()
    }

    val activeHeadingIndex = remember(outline, firstVisible) {
        if (outline.isEmpty()) -1
        else outline.indexOfLast { it.blockIndex <= firstVisible }.coerceAtLeast(0)
    }

    /* --------------------- chrome hides while reading ------------------- */

    LaunchedEffect(listState, isRawMode) {
        if (isRawMode) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.lastScrolledBackward,
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            )
        }.collect { (scrolling, backward, atTop) ->
            if (atTop) chromeVisible = true
            else if (scrolling) chromeVisible = backward
        }
    }
    LaunchedEffect(sourceScroll, isRawMode) {
        if (!isRawMode) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow {
            sourceScroll.isScrollInProgress to sourceScroll.lastScrolledBackward
        }.collect { (scrolling, backward) ->
            if (scrolling) chromeVisible = backward
        }
    }

    /* --------------------------- position memory -------------------------- */

    var restored by remember(fileId) { mutableStateOf(false) }
    LaunchedEffect(fileId, ready?.fileId) {
        if (restored || ready?.hasBody != true) return@LaunchedEffect
        val saved = ReadingPositions.get(fileId)
        val target = saved.index.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
        runCatching { listState.scrollToItem(target, saved.offset) }
        restored = true
    }

    LaunchedEffect(fileId, isRawMode) {
        if (isRawMode) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                ReadingPositions.put(fileId, ReadingPosition(index, offset))
            }
    }

    DisposableEffect(fileId) {
        onDispose {
            ReadingPositions.put(
                fileId,
                ReadingPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            )
        }
    }

    /* ------------------------------ actions ------------------------------ */

    fun stepMatch(delta: Int) {
        if (matchCount == 0) return
        activeMatch = ((activeMatch + delta) % matchCount + matchCount) % matchCount
        if (isRawMode) {
            val hit = lineMatches.getOrNull(activeMatch) ?: return
            val step = with(density) { (SourceLineStep.value * spacing * fontScale).sp.toPx() }
            scope.launch {
                sourceScroll.animateScrollTo((hit.lineIndex * step).roundToInt())
            }
        } else {
            val hit = occurrences.getOrNull(activeMatch) ?: return
            scope.launch {
                listState.animateScrollToItem(hit.blockIndex, scrollOffset = -24)
            }
        }
    }

    fun jumpToOutline(entry: MdHeading) {
        isTocOpen = false
        if (isRawMode) {
            val step = with(density) { (SourceLineStep.value * spacing * fontScale).sp.toPx() }
            scope.launch { sourceScroll.animateScrollTo((entry.lineIndex * step).roundToInt()) }
        } else {
            scope.launch { listState.animateScrollToItem(entry.blockIndex, scrollOffset = -24) }
        }
    }

    fun copyText() {
        val occurrence = occurrences.getOrNull(activeMatch)
        val payload = if (isFindOpen && occurrence != null) {
            blockPlainText(blocks.getOrNull(occurrence.blockIndex) ?: MdBlock.Rule)
        } else {
            source
        }
        if (payload.isBlank()) {
            Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }
        clipboardManager.setText(AnnotatedString(payload))
        Toast.makeText(context, "Markdown source copied", Toast.LENGTH_SHORT).show()
    }

    fun shareFile() {
        val file = selectedFile ?: return
        val intent = Intent(Intent.ACTION_SEND)
            .setType(file.formatInfo.mimeType)
            .putExtra(Intent.EXTRA_SUBJECT, file.name)
        if (ShareUri.attachStream(context, intent, file.uri)) {
            runCatching { context.startActivity(Intent.createChooser(intent, "Share ${file.name}")) }
        } else {
            Toast.makeText(context, "This file cannot be shared from its location", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(keepAwake) {
        val window = context.findActivity()?.window
        if (keepAwake && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    /* ------------------------------- frame ------------------------------- */

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // The page owns the whole screen; chrome floats above it.
            when {
                selectedFile == null -> VennelaEmptyState(
                    title = "No Markdown open",
                    hint = "Open a .md, .markdown or .mdx file from Files to render it here.",
                    icon = Icons.Default.Description
                )

                ready?.error != null -> VennelaErrorState(
                    title = "This document could not be read",
                    message = ready.error.orEmpty(),
                    onRetry = { retryToken++ }
                )

                isParsing -> MarkdownSkeleton()

                ready?.hasBody != true -> VennelaEmptyState(
                    title = "This document is empty",
                    hint = "The file opened cleanly but contains no text to render.",
                    icon = Icons.Default.Description
                )

                else -> {
                    val readingDensity = androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                            density.density,
                            density.fontScale * fontScale
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .applyViewerColorMode(viewerColorMode)
                        ) {
                            if (isRawMode) {
                                SourceListing(
                                    source = source,
                                    scrollState = sourceScroll,
                                    wrap = wrapSource,
                                    spacing = spacing,
                                    matches = lineMatches,
                                    activeMatch = activeMatch
                                )
                            } else {
                                RenderedReadingSurface(
                                    blocks = blocks,
                                    spacing = spacing,
                                    activeBlock = if (isFindOpen) activeBlockOf(occurrences, activeMatch) else -1,
                                    listState = listState
                                )
                            }
                        }
                    }
                }
            }

            // Hairline progress, Google Docs style.
            ReaderProgressRule(
                progress = progress,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Floating top toolbar — auto-hides while scrolling down.
            ReaderChromeVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopToolbar(
                    title = selectedFile?.name ?: "Markdown",
                    subtitle = selectedFile?.formatInfo?.name,
                    onBack = { viewModel.navigateBack() }
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search library",
                        onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "File details",
                        onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }
            }

            // Floating action pill — also auto-hides.
            ReaderChromeVisibility(
                visible = chromeVisible,
                fromTop = false,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomPill(
                    status = "${(progress * 100).roundToInt()}%  •  ${stats.words} words"
                ) {
                    ReaderIconButton(
                        icon = if (isRawMode) Icons.Default.Visibility else Icons.Default.Code,
                        contentDescription = if (isRawMode) "Rendered view" else "Source view",
                        active = isRawMode,
                        onClick = {
                            isRawMode = !isRawMode
                            chromeVisible = true
                        }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Find in document",
                        active = isFindOpen,
                        onClick = {
                            isFindOpen = !isFindOpen
                            if (!isFindOpen) findQuery = ""
                        }
                    )
                    if (outline.isNotEmpty()) {
                        ReaderIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Contents",
                            active = isTocOpen,
                            onClick = {
                                isTocOpen = !isTocOpen
                                isInfoOpen = false
                            }
                        )
                    }
                    ReaderIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "Reading settings",
                        onClick = { isSettingsOpen = true }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Copy document text",
                        onClick = { copyText() }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share document",
                        onClick = { shareFile() }
                    )
                }
            }

            // Find bar floats under the toolbar instead of pushing the page down.
            AnimatedVisibility(
                visible = isFindOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 68.dp)
                    .padding(horizontal = VennelaTokens.gutter)
            ) {
                MarkdownFindBar(
                    query = findQuery,
                    matchCount = matchCount,
                    activeIndex = if (matchCount == 0) 0 else activeMatch + 1,
                    isSourceMode = isRawMode,
                    activeLine = if (isRawMode) lineMatches.getOrNull(activeMatch)?.lineIndex else null,
                    onQueryChange = { findQuery = it },
                    onStep = { stepMatch(it) },
                    onClear = { findQuery = "" }
                )
            }

            /* --------------------------- overlays --------------------------- */

            if (isTocOpen || isInfoOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.backgroundBottom.copy(alpha = 0.35f))
                        .clickable {
                            isTocOpen = false
                            isInfoOpen = false
                        }
                )
            }

            if (isTocOpen) {
                MarkdownOutlineDrawer(
                    outline = outline,
                    activeIndex = activeHeadingIndex,
                    onClose = { isTocOpen = false },
                    onJump = { jumpToOutline(it) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 72.dp)
                )
            }

            if (isInfoOpen) {
                MarkdownInfoPanel(
                    name = selectedFile?.name.orEmpty(),
                    size = selectedFile?.formattedSize.orEmpty(),
                    modified = selectedFile?.formattedDate.orEmpty(),
                    owner = selectedFile?.formatInfo?.name.orEmpty(),
                    extension = selectedFile?.extension?.uppercase(Locale.US).orEmpty(),
                    lines = stats.lines,
                    words = stats.words,
                    characters = stats.characters,
                    sections = outline.size,
                    codeBlocks = blocks.count { it is MdBlock.Code },
                    tables = blocks.count { it is MdBlock.Table },
                    onClose = { isInfoOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 72.dp)
                )
            }
        }
    }

    if (isSettingsOpen) {
        ReaderSettingsSheet(
            onDismiss = { isSettingsOpen = false },
            fontScale = fontScale,
            onFontScale = { fontScale = it },
            spacing = spacing,
            onSpacing = { spacing = it },
            colorOptions = MdThemes,
            activeColorMode = viewerColorMode,
            onColorMode = { viewModel.setViewerColorMode(it) },
            wrap = wrapSource,
            onWrap = { wrapSource = it },
            keepAwake = keepAwake,
            onKeepAwake = { keepAwake = it }
        )
    }
}

/** The block that owns the currently selected find hit. */
private fun activeBlockOf(occurrences: List<MdOccurrence>, activeMatch: Int): Int =
    occurrences.getOrNull(activeMatch.coerceIn(0, (occurrences.size - 1).coerceAtLeast(0)))?.blockIndex ?: -1

/* ------------------------------------------------------------------------- */
/* Reading surfaces                                                           */
/* ------------------------------------------------------------------------- */

/**
 * The rendered page.
 *
 * Virtualised on purpose: a plain Column would compose every paragraph in the
 * document at once, which is both slow and the reason scrolling used to stall.
 */
@Composable
private fun RenderedReadingSurface(
    blocks: List<MdBlock>,
    spacing: Float,
    activeBlock: Int,
    listState: LazyListState
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val pageShape = RoundedCornerShape(VennelaTokens.radiusCard)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VennelaTokens.gutter)
            // The floating chrome draws over the page, so the page must not
            // reserve a band for it: only a slim top inset keeps the first line
            // clear of the bar at rest, and the bottom breathing room lives in
            // the scroll content so the last line can clear the tool pill.
            .padding(top = VennelaTokens.viewerTopInset, bottom = VennelaTokens.spaceXs)
            .shadow(
                elevation = VennelaTokens.elevationOverlay,
                shape = pageShape,
                clip = false,
                ambientColor = if (palette.isDark) accent.primary.copy(alpha = 0.40f) else Color(0x1F0B142B),
                spotColor = if (palette.isDark) accent.secondary.copy(alpha = 0.40f) else Color(0x1F0B142B)
            )
            .clip(pageShape)
            .background(
                if (palette.isDark) {
                    Brush.verticalGradient(
                        listOf(
                            palette.cardSurfaceStrong.copy(alpha = 0.95f),
                            palette.cardSurface.copy(alpha = 0.92f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF3F8FF)))
                }
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                pageShape
            ),
        contentPadding = PaddingValues(
            start = VennelaTokens.spaceLg,
            end = VennelaTokens.spaceLg,
            top = VennelaTokens.spaceMd,
            // Keeps the final line readable above the floating tool pill.
            bottom = VennelaTokens.spaceHuge + VennelaTokens.spaceXl
        ),
        verticalArrangement = Arrangement.spacedBy((VennelaTokens.spaceSm.value * spacing).dp)
    ) {
        item(key = "md_page") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(accent.primary.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            accent.primary.copy(alpha = 0.30f),
                            RoundedCornerShape(VennelaTokens.radiusChip)
                        )
                        .padding(
                            horizontal = VennelaTokens.spaceSm,
                            vertical = VennelaTokens.spaceXxs
                        )
                ) {
                    Text(
                        text = "RENDERED",
                        color = accent.primary,
                        fontSize = VennelaTokens.nano,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${blocks.size} blocks",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.nano,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        itemsIndexed(
            items = blocks,
            key = { index, _ -> "md_block_$index" }
        ) { index, block ->
            val isActive = index == activeBlock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isActive) {
                            Modifier
                                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                .background(accent.primary.copy(alpha = 0.14f))
                                .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXs)
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (block is MdBlock.Code) {
                    GlassCodeBlock(block = block, baseFontSize = 12.sp)
                } else {
                    MarkdownBlockView(
                        block = block,
                        textColor = palette.textPrimary,
                        mutedColor = palette.textSecondary,
                        headingColor = palette.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Source view with real line numbers and a bounded render window. Line offsets
 * are exact arithmetic (fixed line step), so find, outline jumps and the
 * reading position all land on the true line.
 */
@Composable
private fun SourceListing(
    source: String,
    scrollState: androidx.compose.foundation.ScrollState,
    wrap: Boolean,
    spacing: Float,
    matches: List<SourceMatch>,
    activeMatch: Int
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val lines = remember(source) { source.lines() }
    val rendered = remember(lines) { lines.take(MaxSourceLines) }
    val stepSp = (SourceLineStep.value * spacing).sp
    val stepDp = with(androidx.compose.ui.platform.LocalDensity.current) { stepSp.toDp() }

    val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { stepSp.toPx() }
    var viewportPx by remember { mutableStateOf(1f) }
    val firstVisible = if (stepPx > 0f) (scrollState.value / stepPx).toInt() else 0
    val visibleCount = if (stepPx > 0f) (viewportPx / stepPx).toInt() + 1 else rendered.size
    val windowStart = (firstVisible - SourceOverscan).coerceAtLeast(0)
    val windowEnd = (windowStart + visibleCount + SourceOverscan * 2).coerceAtMost(rendered.size)
    val activeLine = matches.getOrNull(activeMatch)?.lineIndex ?: -1

    // Only the visible window is annotated. Annotating all 1200 windowed lines
    // on every composition was pure main-thread work for ~50 visible rows, and
    // was the single biggest cost when a long file was opened in source mode.
    val windowAnnotated = remember(rendered, matches, activeMatch, accent, windowStart, windowEnd) {
        (windowStart until windowEnd).map { index ->
            annotateSourceLine(rendered[index], index, matches, activeMatch, accent)
        }
    }
    // One shared horizontal scroll so every unwrapped line stays aligned and no
    // per-row scroll state is allocated inside the loop.
    val horizontalScroll = rememberScrollState()

    ReaderPaper(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VennelaTokens.gutter)
            .padding(top = 70.dp, bottom = 104.dp),
        maxWidth = null,
        cornerRadius = VennelaTokens.radiusCard,
        contentPadding = PaddingValues(
            horizontal = VennelaTokens.spaceSm,
            vertical = VennelaTokens.spaceSm
        )
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportPx = it.height.toFloat().coerceAtLeast(1f) }
            .verticalScroll(scrollState)
    ) {
        if (windowStart > 0) Spacer(Modifier.height(stepDp * windowStart))

        for (index in windowStart until windowEnd) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index == activeLine) Modifier.background(accent.primary.copy(alpha = 0.12f))
                        else Modifier
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}",
                    color = if (index == activeLine) accent.secondary else palette.textTertiary,
                    fontSize = VennelaTokens.micro,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = stepSp,
                    maxLines = 1,
                    modifier = Modifier
                        .width(GutterWidth)
                        .padding(end = VennelaTokens.spaceSm)
                )
                Text(
                    text = windowAnnotated.getOrNull(index - windowStart)
                        ?: AnnotatedString(rendered[index].take(MaxCharsPerSourceLine)),
                    color = palette.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = VennelaTokens.caption,
                    lineHeight = stepSp,
                    softWrap = wrap,
                    // A wrapped 400KB single line would otherwise lay out as
                    // hundreds of screenfuls and stall the frame.
                    maxLines = if (wrap) 120 else 1,
                    modifier = Modifier
                        .then(
                            if (wrap) Modifier.fillMaxWidth()
                            else Modifier.horizontalScroll(horizontalScroll)
                        )
                        .padding(horizontal = VennelaTokens.spaceXs)
                )
            }
        }

        if (windowEnd < rendered.size) Spacer(Modifier.height(stepDp * (rendered.size - windowEnd)))

        Spacer(Modifier.height(VennelaTokens.spaceLg))
        Text(
            text = if (lines.size > rendered.size) {
                "Showing ${rendered.size} of ${lines.size} source lines. " +
                    "Rendered mode reads the whole document."
            } else {
                "${lines.size} source lines • ${matches.size} matches"
            },
            color = palette.textTertiary,
            fontSize = VennelaTokens.micro
        )
    }
    }
}

/** Rendered fenced code on its own frosted slab. */
@Composable
private fun GlassCodeBlock(block: MdBlock.Code, baseFontSize: androidx.compose.ui.unit.TextUnit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VennelaTokens.spaceXs)
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.34f,
                    frost = VennelaTokens.frostRegular,
                    elevation = VennelaTokens.elevationRaised
                ),
                RoundedCornerShape(VennelaTokens.radiusControl)
            )
            .padding(VennelaTokens.spaceMd)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = (block.language ?: "code").uppercase(Locale.US),
                color = accent.secondary,
                fontSize = VennelaTokens.nano,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ReaderIconButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "Copy this code block",
                onClick = {
                    clipboardManager.setText(AnnotatedString(block.code))
                    Toast.makeText(context, "Snippet copied", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Spacer(Modifier.height(VennelaTokens.spaceSm))
        Text(
            text = block.code,
            color = palette.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = baseFontSize,
            lineHeight = baseFontSize * 1.5,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Quick find                                                                 */
/* ------------------------------------------------------------------------- */

@Composable
private fun MarkdownFindBar(
    query: String,
    matchCount: Int,
    activeIndex: Int,
    isSourceMode: Boolean,
    activeLine: Int?,
    onQueryChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    onClear: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                liquidGlassSpec(alpha = 0.52f, elevation = VennelaTokens.elevationRaised),
                RoundedCornerShape(VennelaTokens.radiusControl)
            )
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            MarkdownTextField(
                value = query,
                placeholder = "Find in document...",
                onValueChange = onQueryChange,
                onGo = { onStep(1) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(VennelaTokens.spaceXs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    query.isBlank() -> "Type to search the whole document"
                    matchCount == 0 -> "No matches"
                    matchCount >= MaxMatches -> "$activeIndex of $matchCount+ matches"
                    else -> "$activeIndex of $matchCount matches"
                },
                color = if (matchCount == 0 && query.isNotBlank()) palette.textTertiary else accent.secondary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (activeLine != null && matchCount > 0) {
                Text(
                    text = "Line ${activeLine + 1}",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.nano,
                    maxLines = 1
                )
                Spacer(Modifier.width(VennelaTokens.spaceSm))
            }
            ReaderIconButton(Icons.Default.KeyboardArrowUp, "Previous match") { onStep(-1) }
            ReaderIconButton(Icons.Default.KeyboardArrowDown, "Next match") { onStep(1) }
            ReaderIconButton(Icons.Default.Close, "Clear find") { onClear() }
        }
    }
}

@Composable
private fun MarkdownTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = palette.textTertiary,
                fontSize = VennelaTokens.bodySmall,
                maxLines = 1
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(LocalVennelaAccent.current.secondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onGo() }),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VennelaTokens.touchTargetCompact)
                .semantics { contentDescription = "Find in document" }
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Outline drawer + info panel                                                */
/* ------------------------------------------------------------------------- */

@Composable
private fun MarkdownOutlineDrawer(
    outline: List<MdHeading>,
    activeIndex: Int,
    onClose: () -> Unit,
    onJump: (MdHeading) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    Column(
        modifier = modifier
            .padding(horizontal = VennelaTokens.gutter)
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.52f,
                    frost = VennelaTokens.frostHeavy,
                    elevation = VennelaTokens.elevationOverlay,
                    strong = true
                ),
                RoundedCornerShape(VennelaTokens.radiusSheet)
            )
            .padding(VennelaTokens.spaceMd)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = "Contents",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${outline.size} headings",
                color = palette.textTertiary,
                fontSize = VennelaTokens.nano,
                maxLines = 1
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            ReaderIconButton(Icons.Default.Close, "Hide contents", onClick = onClose)
        }
        Spacer(Modifier.height(VennelaTokens.spaceSm))

        if (outline.isEmpty()) {
            Text(
                text = "This document has no headings, so there is nothing to outline. " +
                    "Add a # heading to build one.",
                color = palette.textSecondary,
                fontSize = VennelaTokens.bodySmall
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = VennelaTokens.spaceHuge * 8)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXxs)
            ) {
                outline.take(60).forEachIndexed { index, entry ->
                    val isActive = index == activeIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = VennelaTokens.touchTargetCompact)
                            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                            .background(
                                if (isActive) accent.primary.copy(alpha = 0.22f) else Color.Transparent
                            )
                            .pressScale(pressedScale = 0.97f) { onJump(entry) }
                            .padding(
                                start = (VennelaTokens.spaceXs.value +
                                    (entry.level - 1).coerceIn(0, 4) * VennelaTokens.spaceSm.value).dp,
                                end = VennelaTokens.spaceSm
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.title.ifBlank { "Untitled section" },
                            color = if (isActive) accent.secondary else palette.textSecondary,
                            fontSize = VennelaTokens.micro,
                            fontWeight = if (entry.level <= 2) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (outline.size > 60) {
                    Text(
                        text = "Showing 60 of ${outline.size} headings",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.nano,
                        modifier = Modifier.padding(top = VennelaTokens.spaceSm)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownInfoPanel(
    name: String,
    size: String,
    modified: String,
    owner: String,
    extension: String,
    lines: Int,
    words: Int,
    characters: Int,
    sections: Int,
    codeBlocks: Int,
    tables: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    Column(
        modifier = modifier
            .padding(horizontal = VennelaTokens.gutter)
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .liquidGlass(
                liquidGlassSpec(
                    alpha = 0.52f,
                    frost = VennelaTokens.frostHeavy,
                    elevation = VennelaTokens.elevationOverlay,
                    strong = true
                ),
                RoundedCornerShape(VennelaTokens.radiusSheet)
            )
            .padding(VennelaTokens.spaceLg)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = name.ifBlank { "Document" },
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ReaderIconButton(Icons.Default.Close, "Hide document info", onClick = onClose)
        }
        Spacer(Modifier.height(VennelaTokens.spaceSm))
        InfoRow("Size", size.ifBlank { "Unknown" })
        InfoRow("Modified", modified.ifBlank { "Unknown" })
        InfoRow("Kind", owner.ifBlank { "Text" })
        InfoRow("Extension", extension.ifBlank { "—" })
        InfoRow("Lines", if (lines > 0) "$lines" else "—")
        InfoRow("Words", "$words")
        InfoRow("Characters", "$characters")
        InfoRow("Headings", "$sections")
        InfoRow("Code blocks", "$codeBlocks")
        InfoRow("Tables", "$tables")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.spaceXxl)
            .padding(vertical = VennelaTokens.spaceXxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textTertiary,
            fontSize = VennelaTokens.micro,
            modifier = Modifier.width(VennelaTokens.badgeLg + VennelaTokens.spaceHuge)
        )
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Skeleton                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
private fun MarkdownSkeleton() {
    val palette = LocalVennelaPalette.current
    val transition = rememberInfiniteTransition(label = "mdSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "mdSkeletonAlpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter)
            .padding(top = 72.dp)
            .liquidGlass(
                liquidGlassSpec(alpha = 0.38f, elevation = VennelaTokens.elevationCard),
                RoundedCornerShape(VennelaTokens.radiusCardLarge)
            )
            .padding(VennelaTokens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(VennelaTokens.spaceXl)
                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                .background(palette.cardSurface.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(VennelaTokens.spaceMd)
                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                .background(palette.cardSurface.copy(alpha = alpha * 0.8f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .height(VennelaTokens.spaceMd)
                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                .background(palette.cardSurface.copy(alpha = alpha * 0.7f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(VennelaTokens.spaceXl)
                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                .background(palette.cardSurface.copy(alpha = alpha * 0.85f))
        )
    }
}
