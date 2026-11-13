package com.vennela365.viewer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.CodeSyntaxHighlighter
import com.vennela365.viewer.parser.DocumentTextExtractor
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.ViewerColorMode
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.applyViewerColorMode
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.HyperGutterRadar
import com.vennela365.viewer.ui.components.LassoToComputeOverlay
import com.vennela365.viewer.ui.components.PrivacyCurtain
import com.vennela365.viewer.ui.components.PrivacyEngine
import com.vennela365.viewer.ui.components.RadarTick
import com.vennela365.viewer.ui.components.RadarTickType
import com.vennela365.viewer.ui.components.ReaderBottomPill
import com.vennela365.viewer.ui.components.ReaderChromeVisibility
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderProgressRule
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.StudySheetCompilerView
import com.vennela365.viewer.ui.components.SuiteStyleSwitcherStrip
import com.vennela365.viewer.ui.components.VennelaBackground
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.ViewerSuiteStyle
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/* ------------------------------------------------------------------------- */
/* Tunables                                                                   */
/* ------------------------------------------------------------------------- */

/**
 * Hard ceiling on the rows handed to the list. A 40k-line bundle would
 * otherwise allocate 40k highlighted strings before the first frame; the
 * viewer says so in its footer instead of pretending the rest does not exist.
 */
private const val MaxRenderedLines = 6_000

/** Longest slice of one source line that is tokenised or laid out. */
private const val MaxCharsPerLine = 2_000
private const val MaxCodeMatches = 400
private const val MaxRememberedPositions = 24
private const val CodeFontMin = 8f
private const val CodeFontMax = 22f
private const val CodeFontStep = 1f

/** Width reserved for the line-number gutter. */
private val GutterWidth = VennelaTokens.badgeLg + VennelaTokens.spaceSm

/* ------------------------------------------------------------------------- */
/* Reading position memory                                                    */
/* ------------------------------------------------------------------------- */

/** Where the reader left off, so reopening a file resumes at the same line. */
private data class CodePosition(val index: Int, val offset: Int)

private object CodePositions {
    private val positions = LinkedHashMap<String, CodePosition>()

    fun get(fileId: String?): CodePosition = fileId?.let { positions[it] } ?: CodePosition(0, 0)

    fun put(fileId: String?, position: CodePosition) {
        val id = fileId ?: return
        positions.remove(id)
        positions[id] = position
        while (positions.size > MaxRememberedPositions) {
            val oldest = positions.keys.firstOrNull() ?: break
            positions.remove(oldest)
        }
    }
}

/**
 * Structural ticks for the gutter radar: functions, classes and periodicity.
 * Bounded on purpose — a radar with more ticks than pixels is noise, and the
 * whole point of this screen is that opening a file stays instant.
 */
private fun buildRadarTicks(lines: List<String>): List<RadarTick> {
    if (lines.size < 2) return emptyList()
    val total = lines.size.toFloat()
    val stride = (lines.size / 80).coerceAtLeast(1)
    val ticks = ArrayList<RadarTick>(96)
    var index = 0
    while (index < lines.size && ticks.size < 120) {
        val trimmed = lines[index].trim()
        when {
            trimmed.startsWith("def ") || trimmed.startsWith("fun ") || trimmed.startsWith("fn ") ||
                trimmed.startsWith("function ") || trimmed.startsWith("func ") ->
                ticks += RadarTick(index / total, RadarTickType.HEADING, trimmed.take(24))

            trimmed.startsWith("class ") || trimmed.startsWith("struct ") ||
                trimmed.startsWith("interface ") || trimmed.startsWith("impl ") ->
                ticks += RadarTick(index / total, RadarTickType.SEARCH_MATCH, trimmed.take(24))

            index % stride == 0 && index > 0 ->
                ticks += RadarTick(index / total, RadarTickType.BOOKMARK, "L${index + 1}")

            else -> Unit
        }
        index++
    }
    return ticks
}

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * Code viewer, rebuilt as a content-first reader.
 *
 * The page owns the whole viewport and the reader chrome — a 30dp floating
 * toolbar and a single tool pill — slides away while reading forward, exactly
 * like [MarkdownViewerScreen]. A slim top inset keeps the first line clear of
 * the bar at rest; the bottom breathing room lives in the list's content
 * padding so the page itself never reserves a band. Everything the previous editor frame could do is
 * still here: a real gutter, syntax highlighting, wrap, font stepping, find
 * with match navigation, jump-to-line, copy, share, open-with, language
 * detection and file stats.
 */
@Composable
fun CodeViewerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val viewerColorMode by viewModel.viewerColorMode.collectAsState()
    val isLassoActive by viewModel.isLassoActive.collectAsState()
    val isPrivacyActive by viewModel.isPrivacyActive.collectAsState()
    val enableMicroLouvre by viewModel.enableMicroLouvre.collectAsState()
    val isStudySheetActive by viewModel.isStudySheetActive.collectAsState()
    val activeSuite by viewModel.preferredSuiteStyle.collectAsState()

    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val file = selectedFile
    val fileId = file?.id

    var isWrapped by remember { mutableStateOf(false) }
    var fontSizeSp by remember(fileId) { mutableFloatStateOf(12.5f) }
    var showFind by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var activeMatch by remember { mutableIntStateOf(0) }
    var jumpText by remember { mutableStateOf("") }
    var chromeVisible by remember { mutableStateOf(true) }
    var retryToken by remember(fileId) { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    // The floating bars draw *over* the page, so the page only owes them a
    // modest inset: the status bar plus the token's viewer inset.
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chromeTopInset = statusInset + VennelaTokens.viewerTopInset

    /* ------------------------------ content ------------------------------ */

    // The view model inlines small files, but URI-only opens (and large files)
    // can arrive with blank sample content, so the screen reads those itself
    // instead of showing a dead placeholder.
    var streamedCode by remember(fileId) { mutableStateOf<String?>(null) }
    var isReading by remember(fileId) { mutableStateOf(false) }

    LaunchedEffect(fileId, file?.uri, file?.sampleContent, retryToken) {
        val target = file
        if (target == null || target.uri == null || target.sampleContent.isNotBlank()) {
            streamedCode = null
            isReading = false
            return@LaunchedEffect
        }
        isReading = true
        streamedCode = runCatching {
            withContext(Dispatchers.IO) { DocumentTextExtractor.readPlainText(context, target.uri) }
        }.getOrNull()?.takeIf { it.isNotBlank() }
        isReading = false
    }

    val inlineBody = file?.sampleContent.orEmpty()
    val rawCode = when {
        inlineBody.isNotBlank() -> inlineBody
        streamedCode != null -> streamedCode.orEmpty()
        else -> ""
    }
    val hasRealContent = inlineBody.isNotBlank() || streamedCode != null

    val displayCode = remember(rawCode, isPrivacyActive) {
        if (isPrivacyActive) PrivacyEngine.maskPii(rawCode) else rawCode
    }
    val lines = remember(displayCode) {
        if (displayCode.isEmpty()) emptyList() else displayCode.lines()
    }
    val renderedLines = remember(lines) { lines.take(MaxRenderedLines) }
    val rowCount = renderedLines.size
    val radarTicks = remember(renderedLines) { buildRadarTicks(renderedLines) }

    val extension = file?.extension?.lowercase(Locale.US).orEmpty()
    val languageLabel = if (extension.isBlank()) "TEXT" else extension.uppercase(Locale.US)
    val highlightLanguage = remember(extension) { extension.ifBlank { "txt" } }

    // Tokenising the whole file on the main thread was the single biggest cost
    // when a long file was opened, so it runs off the main thread and is keyed
    // to the exact text being displayed (privacy masking changes that text).
    val codeKey = remember(displayCode) { displayCode.hashCode() }
    var highlighted by remember(fileId, highlightLanguage, codeKey) {
        mutableStateOf<List<AnnotatedString>>(emptyList())
    }
    LaunchedEffect(renderedLines, highlightLanguage, codeKey) {
        highlighted = if (renderedLines.isEmpty()) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) {
                renderedLines.map { line ->
                    CodeSyntaxHighlighter.highlight(line.take(MaxCharsPerLine), highlightLanguage)
                }
            }
        }
    }

    /* -------------------------------- find ------------------------------- */

    var matchLines by remember(fileId, codeKey) { mutableStateOf<List<Int>>(emptyList()) }
    val matchSet = remember(matchLines) { matchLines.toSet() }
    LaunchedEffect(renderedLines, findQuery, codeKey) {
        if (findQuery.isBlank()) {
            matchLines = emptyList()
            return@LaunchedEffect
        }
        val query = findQuery
        matchLines = withContext(Dispatchers.Default) {
            val hits = ArrayList<Int>(64)
            for (index in renderedLines.indices) {
                if (renderedLines[index].contains(query, ignoreCase = true)) {
                    hits += index
                    if (hits.size >= MaxCodeMatches) break
                }
            }
            hits
        }
    }
    LaunchedEffect(matchLines) {
        activeMatch = 0
    }
    val activeMatchLine = matchLines.getOrNull(activeMatch) ?: -1

    /* ------------------------------ progress ----------------------------- */

    val firstVisible = listState.firstVisibleItemIndex
    val progress = if (rowCount <= 1) {
        0f
    } else {
        (firstVisible.toFloat() / (rowCount - 1).toFloat()).coerceIn(0f, 1f)
    }

    /* --------------------- chrome hides while reading ------------------- */

    LaunchedEffect(listState) {
        snapshotFlow {
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

    /* --------------------------- position memory ------------------------ */

    var restored by remember(fileId) { mutableStateOf(false) }
    LaunchedEffect(fileId, rowCount) {
        if (restored || rowCount == 0) return@LaunchedEffect
        val saved = CodePositions.get(fileId)
        runCatching { listState.scrollToItem(saved.index.coerceIn(0, rowCount - 1), saved.offset) }
        restored = true
    }
    LaunchedEffect(fileId) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> CodePositions.put(fileId, CodePosition(index, offset)) }
    }
    DisposableEffect(fileId) {
        onDispose {
            CodePositions.put(
                fileId,
                CodePosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            )
        }
    }

    /* ------------------------------ actions ------------------------------ */

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun goToLine(lineNumber: Int) {
        if (rowCount == 0) return
        val index = (lineNumber - 1).coerceIn(0, rowCount - 1)
        scope.launch { listState.animateScrollToItem(index) }
    }

    fun stepMatch(delta: Int) {
        if (matchLines.isEmpty()) return
        val next = ((activeMatch + delta) % matchLines.size + matchLines.size) % matchLines.size
        activeMatch = next
        scope.launch { listState.animateScrollToItem(matchLines[next]) }
    }

    fun toggleFind() {
        showFind = !showFind
        if (!showFind) findQuery = ""
        if (showFind) showJump = false
        chromeVisible = true
    }

    fun toggleJump() {
        showJump = !showJump
        if (showJump) showFind = false
        chromeVisible = true
    }

    fun copyAll() {
        if (rawCode.isBlank()) {
            toast("Nothing to copy from this file")
            return
        }
        clipboardManager.setText(AnnotatedString(rawCode))
        toast("Code copied to clipboard")
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

    /* ------------------------------- frame ------------------------------- */

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            val state = viewerState
            when {
                file == null -> CodeStateFrame(chromeTopInset) {
                    VennelaEmptyState(
                        title = "No code open",
                        hint = "Pick a code, config, log or plain-text file from Your library to read it here.",
                        icon = Icons.Default.Code
                    )
                }

                isReading || state is ViewerState.Loading -> CodeStateFrame(chromeTopInset) {
                    VennelaLoadingState(
                        label = "Reading ${file?.name ?: "file"}…",
                        progress = (state as? ViewerState.Loading)?.progress
                    )
                }

                state is ViewerState.Failed -> CodeStateFrame(chromeTopInset) {
                    VennelaErrorState(
                        title = "Couldn't open this file",
                        message = state.reason,
                        onRetry = { viewModel.openFile(file) },
                        onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                !hasRealContent -> CodeStateFrame(chromeTopInset) {
                    VennelaErrorState(
                        title = "Couldn't read this file as text",
                        message = "Vennela could not decode this file as UTF-8 text. " +
                            "The original file has not been changed.",
                        onRetry = { retryToken++ },
                        onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                rowCount == 0 -> CodeStateFrame(chromeTopInset) {
                    VennelaEmptyState(
                        title = "This file is empty",
                        hint = "${file?.name ?: "This file"} opened cleanly but contains no text to show.",
                        icon = Icons.Default.Code,
                        actionLabel = "File properties",
                        onAction = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                }

                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyViewerColorMode(viewerColorMode)
                ) {
                    CodeReadingSurface(
                        lines = renderedLines,
                        highlighted = highlighted,
                        isWrapped = isWrapped,
                        fontSizeSp = fontSizeSp,
                        topInset = chromeTopInset,
                        listState = listState,
                        matchLines = matchSet,
                        activeMatchLine = activeMatchLine,
                        totalLineCount = lines.size,
                        activeSuite = activeSuite,
                        onSelectSuite = { viewModel.setPreferredSuiteStyle(it) }
                    )
                }
            }

            // Hairline progress, Google Docs style.
            if (rowCount > 0) {
                ReaderProgressRule(
                    progress = progress,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // A 6dp structural radar on the right edge: the whole file at a
            // glance, with its functions and classes. It takes no band at all.
            if (rowCount > 1) {
                HyperGutterRadar(
                    ticks = radarTicks,
                    currentScrollProgress = progress,
                    onScrub = { fraction ->
                        val target = (fraction * (rowCount - 1)).roundToInt()
                            .coerceIn(0, rowCount - 1)
                        scope.launch { listState.scrollToItem(target) }
                    },
                    formatPageOrLineLabel = { fraction ->
                        val line = (fraction * rowCount).roundToInt().coerceIn(1, rowCount)
                        "Line $line"
                    }
                )
            }

            // Floating top bar — auto-hides while scrolling down.
            ReaderChromeVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopToolbar(
                    title = file?.name ?: "Code",
                    subtitle = buildString {
                        append(activeSuite.badge)
                        append("  •  ")
                        append(languageLabel)
                        if (rowCount > 0) {
                            append("  •  ")
                            append(rowCount)
                            append(if (rowCount == 1) " line" else " lines")
                        }
                    },
                    onBack = { viewModel.navigateBack() }
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.ColorLens,
                        contentDescription = "Viewer colour mode • ${viewerColorMode.title}",
                        active = viewerColorMode != ViewerColorMode.ORIGINAL,
                        onClick = {
                            viewModel.cycleViewerColorMode()
                            chromeVisible = true
                        }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share this file",
                        onClick = { shareFile() }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "Reader tools and file stats",
                        active = showTools,
                        onClick = {
                            showTools = !showTools
                            chromeVisible = true
                        }
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
                    status = if (rowCount > 0) "${(progress * 100).roundToInt()}%" else null
                ) {
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.WrapText,
                        contentDescription = if (isWrapped) "Wrapping on" else "Wrapping off",
                        active = isWrapped,
                        modifier = Modifier.testTag("code_wrap_button"),
                        onClick = {
                            isWrapped = !isWrapped
                            chromeVisible = true
                        }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.TextDecrease,
                        contentDescription = "Smaller code",
                        modifier = Modifier.testTag("code_font_dec_button"),
                        onClick = {
                            fontSizeSp = (fontSizeSp - CodeFontStep).coerceAtLeast(CodeFontMin)
                            chromeVisible = true
                        }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.TextIncrease,
                        contentDescription = "Larger code",
                        modifier = Modifier.testTag("code_font_inc_button"),
                        onClick = {
                            fontSizeSp = (fontSizeSp + CodeFontStep).coerceAtMost(CodeFontMax)
                            chromeVisible = true
                        }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Find in file",
                        active = showFind,
                        modifier = Modifier.testTag("code_find_button"),
                        onClick = { toggleFind() }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.LowPriority,
                        contentDescription = "Jump to line",
                        active = showJump,
                        modifier = Modifier.testTag("code_jump_button"),
                        onClick = { toggleJump() }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Copy all code",
                        modifier = Modifier.testTag("code_copy_button"),
                        onClick = { copyAll() }
                    )
                }
            }

            // Find / jump strips float under the bar instead of pushing the page.
            AnimatedVisibility(
                visible = showFind,
                enter = fadeIn(tween(140)) + expandVertically(tween(160)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = chromeTopInset)
                    .padding(horizontal = VennelaTokens.gutter)
            ) {
                CodeFindStrip(
                    query = findQuery,
                    matchCount = matchLines.size,
                    activeIndex = if (matchLines.isEmpty()) 0 else activeMatch + 1,
                    onQueryChange = { findQuery = it },
                    onStep = { stepMatch(it) },
                    onClose = {
                        showFind = false
                        findQuery = ""
                    }
                )
            }

            AnimatedVisibility(
                visible = showJump,
                enter = fadeIn(tween(140)) + expandVertically(tween(160)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = chromeTopInset)
                    .padding(horizontal = VennelaTokens.gutter)
            ) {
                CodeJumpStrip(
                    value = jumpText,
                    maxLine = rowCount,
                    onValueChange = { jumpText = it.filter(Char::isDigit).take(7) },
                    onGo = {
                        jumpText.toIntOrNull()?.let { goToLine(it) }
                        showJump = false
                        jumpText = ""
                    },
                    onClose = {
                        showJump = false
                        jumpText = ""
                    }
                )
            }

            // Reader tools + real file stats, in a compact floating sheet.
            // A dim, tap-anywhere backdrop dismisses it without stealing layout.
            if (showTools) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.backgroundBottom.copy(alpha = 0.30f))
                        .pressScale(pressedScale = 1f) { showTools = false }
                )
            }

            if (showTools) {
                CodeToolsPanel(
                    fileName = file?.name.orEmpty(),
                    formatName = file?.formatInfo?.name.orEmpty(),
                    languageLabel = languageLabel,
                    encodingLabel = "UTF-8",
                    colorModeLabel = viewerColorMode.title,
                    lineCount = lines.size,
                    characterCount = displayCode.length,
                    sizeLabel = file?.formattedSize.orEmpty(),
                    modifiedLabel = file?.formattedDate.orEmpty(),
                    isPrivacyActive = isPrivacyActive,
                    isLassoActive = isLassoActive,
                    isStudySheetActive = isStudySheetActive,
                    onClose = { showTools = false },
                    onSearch = {
                        showTools = false
                        viewModel.navigateTo(Screen.UniversalSearch)
                    },
                    onOpenWith = {
                        showTools = false
                        openWith()
                    },
                    onDetails = {
                        showTools = false
                        viewModel.navigateTo(Screen.FileMetadata)
                    },
                    onPrivacy = { viewModel.togglePrivacy() },
                    onLasso = { viewModel.toggleLasso() },
                    onStudySheet = {
                        showTools = false
                        viewModel.showStudySheet()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = chromeTopInset)
                )
            }

            /* ------------------------------ overlays ----------------------------- */

            LassoToComputeOverlay(
                isActive = isLassoActive,
                onClose = { viewModel.toggleLasso() },
                sourceTextProvider = { rawCode }
            )

            PrivacyCurtain(
                isActive = isPrivacyActive,
                onClose = { viewModel.togglePrivacy() },
                enableMicroLouvre = enableMicroLouvre,
                onToggleMicroLouvre = { viewModel.setMicroLouvre(it) }
            )

            if (isStudySheetActive) {
                StudySheetCompilerView(
                    rawText = rawCode,
                    title = file?.name ?: "Code",
                    onClose = { viewModel.dismissStudySheet() }
                )
            }
        }
    }
}

/** Centred load/empty/error frame that still clears the floating bar. */
@Composable
private fun CodeStateFrame(
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
/* Reading surface                                                            */
/* ------------------------------------------------------------------------- */

/**
 * The code page: one virtualised list of source rows.
 *
 * Every row is `[line number][code]`, so the gutter can never drift away from
 * its line — no measurement pass, no shared scroll offset. Rows are only
 * composed while they are near the viewport, which keeps a 6,000-line file at
 * the same frame cost as a 40-line one.
 */
@Composable
private fun CodeReadingSurface(
    lines: List<String>,
    highlighted: List<AnnotatedString>,
    isWrapped: Boolean,
    fontSizeSp: Float,
    topInset: Dp,
    listState: LazyListState,
    matchLines: Set<Int>,
    activeMatchLine: Int,
    totalLineCount: Int,
    activeSuite: ViewerSuiteStyle,
    onSelectSuite: (ViewerSuiteStyle) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    val rowCount = lines.size
    val lineHeight = (fontSizeSp + 5.5f).sp
    val lineHeightDp = (fontSizeSp + 5.5f).dp
    val pageBrush = remember(palette) {
        Brush.verticalGradient(
            listOf(
                palette.cardSurfaceStrong.copy(alpha = 0.95f),
                palette.cardSurface.copy(alpha = 0.91f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBrush),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxSize()
                .padding(horizontal = VennelaTokens.gutter),
            contentPadding = PaddingValues(
                start = VennelaTokens.spaceXs,
                end = VennelaTokens.spaceSm,
                top = topInset + VennelaTokens.spaceXs,
                bottom = VennelaTokens.spaceHuge + VennelaTokens.spaceXl
            )
        ) {
            item(key = "code_suite_strip") {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    SuiteStyleSwitcherStrip(
                        activeSuite = activeSuite,
                        onSelectSuite = onSelectSuite,
                        isDark = palette.isDark
                    )
                }
            }

            items(count = rowCount, key = { index -> index }) { index ->
                val isActive = index == activeMatchLine
                val isHit = matchLines.contains(index)
                val source = lines[index]
                val rowText = if (source.length > MaxCharsPerLine) {
                    source.take(MaxCharsPerLine)
                } else {
                    source
                }
                val rowScroll = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isActive) {
                                Modifier.background(
                                    accent.primary.copy(alpha = 0.20f),
                                    RoundedCornerShape(VennelaTokens.radiusChip)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = VennelaTokens.spaceXxs),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}",
                        color = when {
                            isActive -> accent.secondary
                            isHit -> accent.primary
                            else -> palette.textTertiary
                        },
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.micro,
                        fontWeight = if (isHit) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = lineHeight,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .width(GutterWidth)
                            .heightIn(min = lineHeightDp)
                            .padding(end = VennelaTokens.spaceSm)
                    )

                    Text(
                        text = highlighted.getOrNull(index) ?: AnnotatedString(rowText),
                        color = palette.textPrimary,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeight,
                        softWrap = isWrapped,
                    // A wrapped 2,000-character minified line would otherwise
                    // lay out as dozens of screens and stall the frame.
                    maxLines = if (isWrapped) 120 else 1,
                    modifier = Modifier
                        .then(
                            if (isWrapped) {
                                Modifier.fillMaxWidth()
                            } else {
                                // Each row owns its horizontal offset: a single
                                // shared state would snap back whenever a short
                                // row re-measured the scrollable's maximum.
                                Modifier.horizontalScroll(rowScroll)
                            }
                        )
                )
            }
        }

        if (totalLineCount > rowCount) {
            item(key = "code_truncated") {
                Text(
                    text = "Showing the first $rowCount of $totalLineCount lines for a fast frame rate. " +
                        "Copy or share the file to read the rest.",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro,
                    modifier = Modifier.padding(
                        top = VennelaTokens.spaceMd,
                        start = VennelaTokens.spaceXs,
                        bottom = VennelaTokens.spaceSm
                    )
                )
            }
        }
    }
    }
}

/* ------------------------------------------------------------------------- */
/* Find + jump strips                                                         */
/* ------------------------------------------------------------------------- */

@Composable
private fun CodeFindStrip(
    query: String,
    matchCount: Int,
    activeIndex: Int,
    onQueryChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.52f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationRaised,
        strong = true
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(
                start = VennelaTokens.spaceMd,
                end = VennelaTokens.spaceXs,
                top = VennelaTokens.spaceXxs,
                bottom = VennelaTokens.spaceXxs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        CodeTextField(
            value = query,
            placeholder = "Find in file…",
            onValueChange = onQueryChange,
            onImeAction = { onStep(1) },
            imeAction = ImeAction.Search,
            modifier = Modifier.weight(1f)
        )
        if (query.isNotBlank()) {
            Text(
                text = if (matchCount == 0) "No matches" else "$activeIndex/$matchCount",
                color = if (matchCount == 0) palette.textTertiary else accent.secondary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = VennelaTokens.spaceXs)
            )
        }
        ReaderIconButton(Icons.Default.KeyboardArrowUp, "Previous match") { onStep(-1) }
        ReaderIconButton(Icons.Default.KeyboardArrowDown, "Next match") { onStep(1) }
        ReaderIconButton(Icons.Default.Close, "Close find") { onClose() }
    }
}

@Composable
private fun CodeJumpStrip(
    value: String,
    maxLine: Int,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.52f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationRaised,
        strong = true
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(
                start = VennelaTokens.spaceMd,
                end = VennelaTokens.spaceXs,
                top = VennelaTokens.spaceXxs,
                bottom = VennelaTokens.spaceXxs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LowPriority,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = "Line",
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        CodeTextField(
            value = value,
            placeholder = "1 – ${maxOf(maxLine, 1)}",
            onValueChange = onValueChange,
            onImeAction = onGo,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Go,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(accent.primary.copy(alpha = 0.30f))
                .border(
                    1.dp,
                    accent.secondary.copy(alpha = 0.45f),
                    RoundedCornerShape(VennelaTokens.radiusPill)
                )
                .pressScale(pressedScale = 0.94f, onClick = onGo)
                .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceXs)
        ) {
            Text(
                text = "Go",
                color = palette.textPrimary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold
            )
        }
        ReaderIconButton(Icons.Default.Close, "Close jump to line") { onClose() }
    }
}

@Composable
private fun CodeTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done
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
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onSearch = { onImeAction() },
                onGo = { onImeAction() },
                onDone = { onImeAction() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VennelaTokens.touchTargetCompact)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Reader tools sheet                                                         */
/* ------------------------------------------------------------------------- */

/**
 * Compact reader sheet: real file stats plus the tools the old thumb dock
 * carried, at a fraction of its footprint. It floats over the page and is
 * dismissed by tapping anywhere outside it.
 */
@Composable
private fun CodeToolsPanel(
    fileName: String,
    formatName: String,
    languageLabel: String,
    encodingLabel: String,
    colorModeLabel: String,
    lineCount: Int,
    characterCount: Int,
    sizeLabel: String,
    modifiedLabel: String,
    isPrivacyActive: Boolean,
    isLassoActive: Boolean,
    isStudySheetActive: Boolean,
    onClose: () -> Unit,
    onSearch: () -> Unit,
    onOpenWith: () -> Unit,
    onDetails: () -> Unit,
    onPrivacy: () -> Unit,
    onLasso: () -> Unit,
    onStudySheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.52f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationOverlay,
        strong = true
    )

    Column(
        modifier = modifier
            .padding(horizontal = VennelaTokens.gutter)
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusSheet))
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = fileName.ifBlank { "Code" },
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ReaderIconButton(Icons.Default.Close, "Close reader tools", onClick = onClose)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = VennelaTokens.spaceHuge * 10)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
            CodeStatRow("Format", formatName.ifBlank { languageLabel })
            CodeStatRow("Language", languageLabel)
            CodeStatRow("Encoding", encodingLabel)
            CodeStatRow("Colour mode", colorModeLabel)
            CodeStatRow("Lines", if (lineCount > 0) "$lineCount" else "—")
            CodeStatRow("Characters", "$characterCount")
            CodeStatRow("Size", sizeLabel.ifBlank { "Unknown" })
            CodeStatRow("Modified", modifiedLabel.ifBlank { "Unknown" })

            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.cardBorder.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))

            CodePanelAction(Icons.Default.Search, "Search library", false, onSearch)
            CodePanelAction(Icons.AutoMirrored.Filled.OpenInNew, "Open with another app", false, onOpenWith)
            CodePanelAction(Icons.Default.Info, "File details", false, onDetails)
            CodePanelAction(Icons.Default.Security, "Privacy curtain", isPrivacyActive, onPrivacy)
            CodePanelAction(Icons.Default.Calculate, "Lasso to compute", isLassoActive, onLasso)
            CodePanelAction(Icons.Default.AutoAwesome, "Study sheet", isStudySheetActive, onStudySheet)
        }
    }
}

@Composable
private fun CodeStatRow(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.spaceXl)
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
            fontSize = VennelaTokens.micro,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CodePanelAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.touchTargetCompact)
            .then(
                if (active) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(VennelaTokens.radiusChip),
                        clip = false,
                        ambientColor = accent.primary.copy(alpha = 0.60f),
                        spotColor = accent.secondary.copy(alpha = 0.60f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .background(
                if (active) {
                    Brush.linearGradient(
                        listOf(
                            accent.primary.copy(alpha = 0.90f),
                            accent.secondary.copy(alpha = 0.60f)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .pressScale(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else palette.textSecondary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = label,
            color = if (active) Color.White else palette.textPrimary,
            fontSize = VennelaTokens.micro,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
