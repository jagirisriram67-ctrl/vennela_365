package com.vennela365.viewer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.model.SpreadsheetCell
import com.vennela365.viewer.model.SpreadsheetSheet
import com.vennela365.viewer.parser.SpreadsheetParser
import com.vennela365.viewer.rendering.ViewerState
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.GlassListRow
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.ViewerTopBar
import com.vennela365.viewer.ui.components.ReaderBottomPill
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.SectionHeader
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.VennelaSearchField
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/* ------------------------------------------------------------------------- */
/* Grid metrics — every one is a token multiple, never an invented dp.        */
/* ------------------------------------------------------------------------- */

/** Frozen row-number / select-all gutter. */
private val GridGutterWidth = VennelaTokens.badgeLg + VennelaTokens.spaceLg

/** Touch-safe row heights: both densities stay at or above the 48dp minimum. */
private val CompactRowHeight = VennelaTokens.touchTargetMin
private val ComfortableRowHeight = VennelaTokens.touchTargetMin + VennelaTokens.spaceMd

private const val InitialGridRows = 200
private const val GridRowChunk = 200
private const val MaxGridRows = 4000
private const val MaxDataRows = 300
private const val MaxFindMatches = 500
private const val StatsScanCap = 2000
private const val RangeStatsCap = 4000

/**
 * Column widths. Deterministic so the frozen header strip and the scrolling
 * body can never disagree, and so "jump to cell" can compute a real offset.
 */
private fun columnWidth(index: Int): Dp = VennelaTokens.badgeLg + when (index) {
    0 -> VennelaTokens.spaceLg
    1 -> VennelaTokens.spaceHuge
    2 -> VennelaTokens.spaceXxl + VennelaTokens.spaceXs
    3 -> VennelaTokens.spaceXxl
    4 -> VennelaTokens.spaceHuge + VennelaTokens.spaceMd
    else -> VennelaTokens.spaceXxl + VennelaTokens.spaceXs
}

/** The two real densities. Both change row height *and* rendered font size. */
private enum class SheetDensity(
    val label: String,
    val rowHeight: Dp,
    val fontSize: TextUnit,
    val lineHeight: TextUnit
) {
    COMPACT("Compact", CompactRowHeight, VennelaTokens.micro, 14.sp),
    COMFORTABLE("Comfortable", ComfortableRowHeight, VennelaTokens.caption, 17.sp)
}

private enum class SheetMode(val label: String, val icon: ImageVector) {
    SHEET("Grid", Icons.Default.TableChart),
    STATS("Stats", Icons.Default.Calculate),
    DATA("Rows", Icons.Default.FilterList)
}

/* ------------------------------------------------------------------------- */
/* Address + range model (real A1 notation)                                   */
/* ------------------------------------------------------------------------- */

private val CellAddressRegex = Regex("^([A-Z]{1,3})([0-9]{1,7})$")

/** Bijective base-26 column name: 0 -> A, 25 -> Z, 26 -> AA. */
private fun columnName(index: Int): String {
    if (index < 0) return "A"
    var i = index
    val out = StringBuilder()
    while (i >= 0) {
        out.append(('A'.code + (i % 26)).toChar())
        i = i / 26 - 1
    }
    return out.reverse().toString()
}

private fun columnIndexFromName(name: String): Int? {
    if (name.isEmpty() || name.length > 3) return null
    var value = 0
    for (ch in name.uppercase(Locale.US)) {
        if (ch !in 'A'..'Z') return null
        value = value * 26 + (ch - 'A' + 1)
        if (value > 16_384) return null
    }
    return value - 1
}

/** 0-based column, 1-based row. */
private fun parseSingleCell(token: String): Pair<Int, Int>? {
    val match = CellAddressRegex.find(token.trim()) ?: return null
    val column = columnIndexFromName(match.groupValues[1]) ?: return null
    val row = match.groupValues[2].toIntOrNull() ?: return null
    if (row < 1) return null
    return column to row
}

private fun parseCellRange(raw: String): CellRange? {
    val text = raw.trim().uppercase(Locale.US).replace("$", "").replace(" ", "")
    if (text.isEmpty()) return null
    val parts = text.split(':')
    val start = parseSingleCell(parts[0]) ?: return null
    val end = if (parts.size > 1) parseSingleCell(parts[1]) ?: return null else start
    return CellRange(start.first, start.second, end.first, end.second)
}

private data class CellRange(val c1: Int, val r1: Int, val c2: Int, val r2: Int) {
    val minCol: Int get() = minOf(c1, c2)
    val maxCol: Int get() = maxOf(c1, c2)
    val minRow: Int get() = minOf(r1, r2)
    val maxRow: Int get() = maxOf(r1, r2)
    val isSingle: Boolean get() = minRow == maxRow && minCol == maxCol
    val cellCount: Int get() = (maxCol - minCol + 1) * (maxRow - minRow + 1)
    fun contains(col: Int, row: Int): Boolean = col in minCol..maxCol && row in minRow..maxRow
    fun onRow(row: Int): Boolean = row in minRow..maxRow
    fun onColumn(col: Int): Boolean = col in minCol..maxCol
    val address: String
        get() = if (isSingle) "${columnName(minCol)}$minRow"
        else "${columnName(minCol)}$minRow:${columnName(maxCol)}$maxRow"
}

private data class SheetMatch(val row: Int, val col: Int) {
    val address: String get() = "${columnName(col)}$row"
}

private data class ColumnStats(
    val column: Int,
    val header: String,
    val numericCount: Int,
    val sum: Double,
    val mean: Double,
    val min: Double,
    val max: Double
)

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * Spreadsheet viewer with Google-Sheets-grade reading depth.
 *
 * The grid keeps a genuinely frozen header row (a separate strip driven by the
 * same [ScrollState] as every body row) and a frozen first column (row numbers
 * live outside the horizontally scrolling body). Cell selection carries real A1
 * notation, a formula bar, find-and-jump, real column statistics, real sorting
 * and a density switch that changes row height and type size. Every value shown
 * comes from the parsed workbook — nothing is invented.
 */
@Composable
fun SpreadsheetViewerScreen(viewModel: MainViewModel) {
    val sheets by viewModel.spreadsheetSheets.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val viewerState by viewModel.viewerState.collectAsState()
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(SheetMode.SHEET) }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf<CellRange?>(null) }
    var densityMode by remember { mutableStateOf(SheetDensity.COMFORTABLE) }
    var sortColumn by remember { mutableIntStateOf(-1) }
    var sortAscending by remember { mutableStateOf(true) }
    var filterQuery by remember { mutableStateOf("") }
    var findQuery by remember { mutableStateOf("") }
    var findOpen by remember { mutableStateOf(false) }
    var activeMatch by remember { mutableIntStateOf(0) }
    var addressInput by remember { mutableStateOf("") }
    var rowLimit by remember { mutableIntStateOf(InitialGridRows) }

    // Shared horizontal scroll: the frozen header strip and every body row read
    // the very same state, so they cannot drift apart.
    val gridScroll = rememberScrollState()
    val gridListState = rememberLazyListState()

    val currentSheet = remember(sheets, selectedSheetIndex) {
        if (sheets.isEmpty()) null else sheets[selectedSheetIndex.coerceIn(0, sheets.lastIndex)]
    }

    LaunchedEffect(sheets.size) {
        if (sheets.isNotEmpty() && selectedSheetIndex > sheets.lastIndex) selectedSheetIndex = 0
    }

    // Rows in display order. Sorting is real: it reorders the parsed rows.
    val viewRows = remember(currentSheet, sortColumn, sortAscending) {
        val rows = currentSheet?.rows.orEmpty()
        if (sortColumn < 0) {
            rows
        } else {
            rows.sortedWith(
                if (sortAscending) {
                    compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) { row ->
                        row.getOrNull(sortColumn)?.value
                    }
                } else {
                    compareByDescending(nullsLast(String.CASE_INSENSITIVE_ORDER)) { row ->
                        row.getOrNull(sortColumn)?.value
                    }
                }
            )
        }
    }

    val columnCount = remember(currentSheet) {
        val sheet = currentSheet ?: return@remember 0
        val widest = sheet.rows.take(200).maxOfOrNull { it.size } ?: 0
        maxOf(sheet.headers.size, widest, 6)
    }

    val totalRows = (currentSheet?.rows?.size ?: 0) + 1

    LaunchedEffect(currentSheet, rowLimit) {
        if (rowLimit > totalRows) rowLimit = maxOf(InitialGridRows, totalRows)
    }

    val matches = remember(currentSheet, findQuery) {
        val sheet = currentSheet
        if (sheet == null || findQuery.isBlank()) emptyList() else findMatches(sheet, findQuery)
    }

    LaunchedEffect(matches) { activeMatch = 0 }

    val numericColumns = remember(currentSheet) {
        currentSheet?.let(::computeColumnStats).orEmpty()
    }

    val isLoading = sheets.isEmpty() &&
        (viewerState is ViewerState.Loading || viewerState is ViewerState.Detecting)
    val loadFailure = viewerState as? ViewerState.Failed

    val activeCell: SpreadsheetCell? = selection?.let { range ->
        if (range.minRow <= 1) null
        else currentSheet?.rows?.getOrNull(range.minRow - 2)?.getOrNull(range.minCol)
    }

    fun selectRange(range: CellRange) {
        selection = range
        addressInput = range.address
    }

    fun scrollToCell(range: CellRange) {
        val bodyIndex = (range.minRow - 2).coerceAtLeast(0)
        val offset = (0 until range.minCol).fold(0.dp) { acc, col -> acc + columnWidth(col) }
        scope.launch {
            gridListState.animateScrollToItem(bodyIndex)
            val target = with(density) { offset.toPx() }.toInt()
            gridScroll.animateScrollTo(target.coerceIn(0, gridScroll.maxValue))
        }
    }

    fun goToMatch(index: Int) {
        val match = matches.getOrNull(index) ?: return
        selectRange(CellRange(match.col, match.row, match.col, match.row))
        scrollToCell(selection!!)
    }

    fun jumpToAddress() {
        val range = parseCellRange(addressInput) ?: run {
            Toast.makeText(context, "Enter an address like B4 or A1:C9", Toast.LENGTH_SHORT).show()
            return
        }
        val maxColumn = (columnCount - 1).coerceAtLeast(0)
        val maxRow = totalRows.coerceAtLeast(1)
        if (range.minCol > maxColumn || range.minRow > maxRow) {
            Toast.makeText(
                context,
                "This sheet ends at ${columnName(maxColumn)}$maxRow",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val clamped = CellRange(
            range.c1.coerceIn(0, maxColumn),
            range.r1.coerceIn(1, maxRow),
            range.c2.coerceIn(0, maxColumn),
            range.r2.coerceIn(1, maxRow)
        )
        selectRange(clamped)
        scrollToCell(clamped)
    }

    fun onHeaderTap(column: Int) {
        when {
            sortColumn != column -> {
                sortColumn = column
                sortAscending = true
            }
            sortAscending -> sortAscending = false
            else -> sortColumn = -1
        }
    }

    fun shareSheet() {
        val file = selectedFile ?: return
        val intent = Intent(Intent.ACTION_SEND).setType(file.formatInfo.mimeType)
            .putExtra(Intent.EXTRA_SUBJECT, file.name)
        if (ShareUri.attachStream(context, intent, file.uri)) {
            runCatching { context.startActivity(Intent.createChooser(intent, "Share ${file.name}")) }
        } else {
            Toast.makeText(context, "This file cannot be shared from its location", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWith() {
        val file = selectedFile ?: return
        val intent = ShareUri.viewIntent(context, file.uri, file.formatInfo.mimeType)
        if (intent != null) {
            runCatching { context.startActivity(Intent.createChooser(intent, "Open ${file.name} with")) }
                .onFailure {
                    Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "This file cannot be handed to another app", Toast.LENGTH_SHORT).show()
        }
    }

    LiquidPage(
        title = null,
        dock = {
            SheetToolDock(
                mode = selectedMode,
                findOpen = findOpen,
                onShare = { shareSheet() },
                onOpenWith = { openWith() },
                onFind = {
                    findOpen = !findOpen
                    if (findOpen) selectedMode = SheetMode.SHEET
                },
                onModeChange = { selectedMode = it },
                onMore = { viewModel.navigateTo(Screen.FileMetadata) }
            )
        }
    ) { pagePadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Floating, slim sheet toolbar — the grid keeps the whole screen.
            ViewerTopBar(
                title = selectedFile?.name ?: "Spreadsheet",
                subtitle = "${selectedFile?.formatInfo?.name ?: "Spreadsheet"} • " +
                    "${selectedFile?.formattedSize ?: "No file selected"}",
                onBack = { viewModel.navigateBack() },
                modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
            ) {
                ReaderIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search files",
                    onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
                )
                Spacer(Modifier.width(VennelaTokens.spaceXs))
                ReaderIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = "File details",
                    onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                )
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = pagePadding.calculateTopPadding() + VennelaTokens.viewerTopInset,
                    bottom = pagePadding.calculateBottomPadding()
                )
                .padding(horizontal = VennelaTokens.gutter)
        ) {
            SheetControlRail(
                selectedMode = selectedMode,
                density = densityMode,
                sortColumn = sortColumn,
                sortAscending = sortAscending,
                hasSheet = currentSheet != null,
                onModeChange = {
                    selectedMode = it
                    if (it != SheetMode.SHEET) findOpen = false
                },
                onDensityChange = { densityMode = it },
                onToggleSort = {
                    if (sortColumn >= 0) sortAscending = !sortAscending
                    else if (columnCount > 0) {
                        sortColumn = 0
                        sortAscending = true
                    }
                },
                onInfo = { viewModel.navigateTo(Screen.FileMetadata) }
            )

            Spacer(Modifier.height(VennelaTokens.spaceSm))

            when {
                loadFailure != null -> GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.38f
                ) {
                    VennelaErrorState(
                        title = "This sheet could not be opened",
                        message = loadFailure.reason,
                        onRetry = { selectedFile?.let { viewModel.openFile(it) } }
                    )
                }

                isLoading -> GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.38f
                ) {
                    VennelaLoadingState(label = "Parsing workbook on device...")
                }

                sheets.isEmpty() || currentSheet == null -> GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.38f
                ) {
                    VennelaEmptyState(
                        title = "No sheet loaded",
                        hint = "Open a .xlsx, .ods, .json or .csv workbook and its real cells appear here — " +
                            "rows, columns and values are parsed on device.",
                        icon = Icons.Default.TableChart,
                        actionLabel = "Browse storage",
                        onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                    )
                }

                else -> {
                    SheetFormulaBar(
                        address = addressInput,
                        rawValue = activeCell?.value.orEmpty(),
                        selectionLabel = selection?.let { range ->
                            if (range.isSingle) null
                            else "${range.cellCount} cells • ${range.address}"
                        },
                        hasSelection = selection != null,
                        onAddressChange = { addressInput = it },
                        onAddressSubmit = { jumpToAddress() },
                        onClearSelection = {
                            selection = null
                            addressInput = ""
                        }
                    )

                    AnimatedVisibility(
                        visible = findOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            SheetFindBar(
                                query = findQuery,
                                matchCount = matches.size,
                                activeIndex = if (matches.isEmpty()) 0 else activeMatch + 1,
                                onQueryChange = { findQuery = it },
                                onStep = { delta ->
                                    if (matches.isNotEmpty()) {
                                        activeMatch = ((activeMatch + delta) % matches.size + matches.size) % matches.size
                                        goToMatch(activeMatch)
                                    }
                                },
                                onClose = {
                                    findOpen = false
                                    findQuery = ""
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(VennelaTokens.spaceSm))

                    when (selectedMode) {
                        SheetMode.SHEET -> {
                            SheetGrid(
                                sheet = currentSheet,
                                rows = viewRows.take(rowLimit),
                                columnCount = columnCount,
                                rowHeight = densityMode.rowHeight,
                                fontSize = densityMode.fontSize,
                                lineHeight = densityMode.lineHeight,
                                selection = selection,
                                sortColumn = sortColumn,
                                sortAscending = sortAscending,
                                gridScroll = gridScroll,
                                listState = gridListState,
                                onSelectRange = { selectRange(it) },
                                onHeaderTap = { onHeaderTap(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            Spacer(Modifier.height(VennelaTokens.spaceSm))

                            SheetRowFooter(
                                shown = viewRows.take(rowLimit).size,
                                total = viewRows.size,
                                cap = MaxGridRows,
                                onLoadMore = {
                                    rowLimit = (rowLimit + GridRowChunk).coerceAtMost(MaxGridRows)
                                },
                                onShowAll = {
                                    rowLimit = viewRows.size.coerceAtMost(MaxGridRows)
                                }
                            )

                            Spacer(Modifier.height(VennelaTokens.spaceSm))

                            SheetTabRail(
                                sheets = sheets,
                                selectedIndex = selectedSheetIndex,
                                onSelect = { index ->
                                    selectedSheetIndex = index
                                    selection = null
                                    addressInput = ""
                                    sortColumn = -1
                                    rowLimit = InitialGridRows
                                }
                            )
                        }

                        SheetMode.STATS -> SheetStatsSection(
                            sheetName = currentSheet.name,
                            scannedRows = minOf(currentSheet.rows.size, StatsScanCap),
                            totalRows = currentSheet.rows.size,
                            columns = numericColumns,
                            selectionStats = if (selection == null) null else computeRangeStats(
                                currentSheet,
                                selection!!,
                                RangeStatsCap
                            ),
                            selectionAddress = selection?.address,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        SheetMode.DATA -> SheetDataSection(
                            sheet = currentSheet,
                            query = filterQuery,
                            onQueryChange = { filterQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }

                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                }
            }
        }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Find + statistics maths (real cells only)                                  */
/* ------------------------------------------------------------------------- */

private fun findMatches(sheet: SpreadsheetSheet, query: String): List<SheetMatch> {
    val out = ArrayList<SheetMatch>(64)
    sheet.headers.forEachIndexed { col, header ->
        if (out.size < MaxFindMatches && header.contains(query, ignoreCase = true)) {
            out += SheetMatch(row = 1, col = col)
        }
    }
    for (rowIndex in sheet.rows.indices) {
        val cells = sheet.rows[rowIndex]
        for (col in cells.indices) {
            if (out.size >= MaxFindMatches) return out
            if (cells[col].value.contains(query, ignoreCase = true)) {
                out += SheetMatch(row = rowIndex + 2, col = col)
            }
        }
    }
    return out
}

private fun computeColumnStats(sheet: SpreadsheetSheet): List<ColumnStats> {
    val columns = sheet.headers.size
    if (columns == 0 || sheet.rows.isEmpty()) return emptyList()
    val counts = IntArray(columns)
    val sums = DoubleArray(columns)
    val mins = DoubleArray(columns) { Double.MAX_VALUE }
    val maxes = DoubleArray(columns) { -Double.MAX_VALUE }

    for (index in 0 until minOf(sheet.rows.size, StatsScanCap)) {
        val row = sheet.rows[index]
        for (col in 0 until columns) {
            val raw = row.getOrNull(col)?.value ?: continue
            val number = SpreadsheetParser.cleanNumeric(raw) ?: continue
            counts[col]++
            sums[col] += number
            if (number < mins[col]) mins[col] = number
            if (number > maxes[col]) maxes[col] = number
        }
    }

    return (0 until columns)
        .filter { counts[it] > 0 }
        .map { col ->
            ColumnStats(
                column = col,
                header = sheet.headers.getOrNull(col).orEmpty().ifBlank { columnName(col) },
                numericCount = counts[col],
                sum = sums[col],
                mean = sums[col] / counts[col],
                min = mins[col],
                max = maxes[col]
            )
        }
}

private fun computeRangeStats(sheet: SpreadsheetSheet, range: CellRange, cap: Int): ColumnStats? {
    val minRow = maxOf(range.minRow, 2)
    val firstIndex = minRow - 2
    val lastIndex = minOf(range.maxRow - 2, sheet.rows.lastIndex)
    if (firstIndex > lastIndex) return null
    val lastCol = minOf(range.maxCol, maxOf(sheet.headers.size, 1) + 8)
    var count = 0
    var sum = 0.0
    var min = Double.MAX_VALUE
    var max = -Double.MAX_VALUE
    var scanned = 0
    for (index in firstIndex..lastIndex) {
        if (scanned++ >= cap) break
        val row = sheet.rows[index]
        for (col in range.minCol..lastCol) {
            val raw = row.getOrNull(col)?.value ?: continue
            val number = SpreadsheetParser.cleanNumeric(raw) ?: continue
            count++
            sum += number
            if (number < min) min = number
            if (number > max) max = number
        }
    }
    if (count == 0) return null
    return ColumnStats(
        column = range.minCol,
        header = range.address,
        numericCount = count,
        sum = sum,
        mean = sum / count,
        min = min,
        max = max
    )
}

private fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = Math.round(value * 1000.0) / 1000.0
    return if (abs(rounded - rounded.toLong()) < 0.0001 && abs(rounded) < 1e15) {
        String.format(Locale.US, "%,d", rounded.toLong())
    } else {
        String.format(Locale.US, "%,.3f", rounded)
    }
}

/* ------------------------------------------------------------------------- */
/* Chrome                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun SheetTopAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = palette.textPrimary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

/** Mode / density / sort rail. A LazyRow so pills never clip on narrow devices. */
@Composable
private fun SheetControlRail(
    selectedMode: SheetMode,
    density: SheetDensity,
    sortColumn: Int,
    sortAscending: Boolean,
    hasSheet: Boolean,
    onModeChange: (SheetMode) -> Unit,
    onDensityChange: (SheetDensity) -> Unit,
    onToggleSort: () -> Unit,
    onInfo: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
        contentPadding = PaddingValues(vertical = VennelaTokens.spaceXs)
    ) {
        items(SheetMode.entries.size, key = { "mode_${SheetMode.entries[it].name}" }) { index ->
            val mode = SheetMode.entries[index]
            LiquidChip(
                text = mode.label,
                icon = mode.icon,
                isActive = selectedMode == mode,
                onClick = { onModeChange(mode) },
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        }
        items(SheetDensity.entries.size, key = { "density_${SheetDensity.entries[it].name}" }) { index ->
            val option = SheetDensity.entries[index]
            LiquidChip(
                text = option.label,
                isActive = density == option,
                onClick = { onDensityChange(option) },
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        }
        item(key = "sort") {
            LiquidChip(
                text = if (sortColumn < 0) "Sort" else "Sort ${columnName(sortColumn)}",
                icon = when {
                    sortColumn < 0 -> Icons.AutoMirrored.Filled.Sort
                    sortAscending -> Icons.Default.ArrowUpward
                    else -> Icons.Default.ArrowDownward
                },
                isActive = sortColumn >= 0,
                onClick = onToggleSort,
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        }
        item(key = "info") {
            LiquidChip(
                text = "Info",
                icon = Icons.Default.Info,
                isActive = false,
                onClick = onInfo,
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        }
        if (hasSheet) {
            item(key = "grid_hint") {
                LiquidChip(
                    text = "Tap a column letter to sort",
                    icon = Icons.Default.GridOn,
                    isActive = false,
                    onClick = {}
                )
            }
        }
    }
}

/** Frosted address box + formula bar for the active cell. */
@Composable
private fun SheetFormulaBar(
    address: String,
    rawValue: String,
    selectionLabel: String?,
    hasSelection: Boolean,
    onAddressChange: (String) -> Unit,
    onAddressSubmit: () -> Unit,
    onClearSelection: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VennelaTokens.radiusControl),
        alpha = 0.40f,
        elevation = VennelaTokens.elevationCard
    ) {
        Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceSm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = VennelaTokens.spaceMd, end = VennelaTokens.spaceXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(VennelaTokens.badgeLg + VennelaTokens.spaceXl)
                        .heightIn(min = VennelaTokens.touchTargetMin)
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(accent.secondary.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            accent.secondary.copy(alpha = 0.40f),
                            RoundedCornerShape(VennelaTokens.radiusChip)
                        )
                        .padding(horizontal = VennelaTokens.spaceSm),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (address.isEmpty()) {
                        Text(
                            text = "A1",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.micro,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    BasicTextField(
                        value = address,
                        onValueChange = { onAddressChange(it.uppercase(Locale.US).take(12)) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = accent.secondary,
                            fontSize = VennelaTokens.micro,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(accent.secondary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { onAddressSubmit() }),
                        modifier = Modifier.semantics {
                            contentDescription = "Cell address. Type a cell or range and press go to jump"
                        }
                    )
                }

                Spacer(Modifier.width(VennelaTokens.spaceSm))

                Text(
                    text = "fx",
                    color = accent.secondary,
                    fontSize = VennelaTokens.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(VennelaTokens.spaceSm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rawValue.ifBlank {
                            if (hasSelection) "This cell is empty" else "Tap any cell to inspect its value"
                        },
                        color = if (rawValue.isNotBlank()) palette.textPrimary else palette.textTertiary,
                        fontSize = VennelaTokens.caption,
                        fontWeight = if (rawValue.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectionLabel != null) {
                        Text(
                            text = selectionLabel,
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.nano,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(VennelaTokens.touchTargetMin)
                        .clip(CircleShape)
                        .pressScale(
                            pressedScale = 0.88f,
                            onClick = if (hasSelection) onClearSelection else null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear cell selection",
                        tint = if (hasSelection) palette.textSecondary else Color.Transparent,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetFindBar(
    query: String,
    matchCount: Int,
    activeIndex: Int,
    onQueryChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.44f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationRaised
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusControl))
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm)
    ) {
        VennelaSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Find across every cell..."
        )
        Spacer(Modifier.height(VennelaTokens.spaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    query.isBlank() -> "Type to search the whole sheet"
                    matchCount == 0 -> "No matches"
                    matchCount >= MaxFindMatches -> "$activeIndex of $matchCount+ matches"
                    else -> "$activeIndex of $matchCount matches"
                },
                color = if (matchCount == 0 && query.isNotBlank()) palette.textTertiary else accent.secondary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            SheetIconAction(Icons.Default.KeyboardArrowUp, "Previous match") { onStep(-1) }
            SheetIconAction(Icons.Default.KeyboardArrowDown, "Next match") { onStep(1) }
            SheetIconAction(Icons.Default.Close, "Close find") { onClose() }
        }
    }
}

@Composable
private fun SheetIconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = palette.textSecondary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Grid — frozen header row + frozen first column                             */
/* ------------------------------------------------------------------------- */

@Composable
private fun SheetGrid(
    sheet: SpreadsheetSheet,
    rows: List<List<SpreadsheetCell>>,
    columnCount: Int,
    rowHeight: Dp,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    selection: CellRange?,
    sortColumn: Int,
    sortAscending: Boolean,
    gridScroll: ScrollState,
    listState: LazyListState,
    onSelectRange: (CellRange) -> Unit,
    onHeaderTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.44f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationRaised
    )
    Column(modifier = modifier.liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusTile))) {
        // Frozen header row. Row 1 of the sheet never scrolls vertically; its
        // column letters share the body's horizontal scroll state.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(GridGutterWidth)
                    .height(rowHeight)
                    .clip(RoundedCornerShape(topStart = VennelaTokens.radiusTile))
                    .background(accent.primary.copy(alpha = 0.12f))
                    .pressScale(pressedScale = 0.96f) {
                        onSelectRange(CellRange(0, 1, (columnCount - 1).coerceAtLeast(0), 1))
                    }
                    .padding(start = VennelaTokens.spaceLg),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Select the header row",
                    tint = if (selection?.onRow(1) == true) accent.secondary else palette.textTertiary,
                    modifier = Modifier.size(VennelaTokens.iconSm)
                )
            }
            Row(modifier = Modifier.horizontalScroll(gridScroll)) {
                for (col in 0 until columnCount) {
                    ColumnHeaderCell(
                        column = col,
                        label = sheet.headers.getOrNull(col).orEmpty().ifBlank { columnName(col) },
                        width = columnWidth(col),
                        height = rowHeight,
                        highlighted = selection?.onColumn(col) == true,
                        sorted = sortColumn == col,
                        ascending = sortAscending,
                        fontSize = fontSize,
                        onClick = { onHeaderTap(col) }
                    )
                }
            }
        }

        GlassDivider()

        // Body. Only a bounded window is composed, and it is the only vertically
        // scrolling element on the screen, so nothing is nested.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(rows.size, key = { index -> "row_$index" }) { index ->
                val cells = rows[index]
                val sheetRow = index + 2
                val rowHighlighted = selection?.onRow(sheetRow) == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .background(
                            when {
                                rowHighlighted -> accent.primary.copy(alpha = 0.10f)
                                index % 2 == 0 -> palette.textPrimary.copy(alpha = 0.03f)
                                else -> Color.Transparent
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Frozen first column: inside the Row, outside the scroll.
                    Box(
                        modifier = Modifier
                            .width(GridGutterWidth)
                            .height(rowHeight)
                            .background(
                                if (rowHighlighted) accent.primary.copy(alpha = 0.22f)
                                else Color.Transparent
                            )
                            .pressScale(pressedScale = 0.96f) {
                                onSelectRange(CellRange(0, sheetRow, (columnCount - 1).coerceAtLeast(0), sheetRow))
                            }
                            .padding(start = VennelaTokens.spaceLg),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "$sheetRow",
                            color = if (rowHighlighted) accent.secondary else palette.textTertiary,
                            fontSize = VennelaTokens.micro,
                            fontWeight = if (rowHighlighted) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }

                    Row(modifier = Modifier.horizontalScroll(gridScroll)) {
                        for (col in 0 until columnCount) {
                            val cell = cells.getOrNull(col)
                            SheetDataCell(
                                cell = cell,
                                width = columnWidth(col),
                                height = rowHeight,
                                coordinate = "${columnName(col)}$sheetRow",
                                inRange = selection?.contains(col, sheetRow) == true,
                                isAnchor = selection?.minCol == col && selection?.minRow == sheetRow,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                onClick = {
                                    onSelectRange(CellRange(col, sheetRow, col, sheetRow))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnHeaderCell(
    column: Int,
    label: String,
    width: Dp,
    height: Dp,
    highlighted: Boolean,
    sorted: Boolean,
    ascending: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .background(
                when {
                    highlighted -> accent.primary.copy(alpha = 0.26f)
                    sorted -> accent.primary.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .semantics {
                contentDescription = "Column ${columnName(column)}, $label" +
                    if (sorted) ", sorted ${if (ascending) "ascending" else "descending"}" else ""
            }
            .pressScale(pressedScale = 0.96f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = columnName(column),
            color = if (highlighted || sorted) palette.textPrimary else accent.secondary,
            fontSize = VennelaTokens.micro,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(start = VennelaTokens.spaceSm)
        )
        Spacer(Modifier.width(VennelaTokens.spaceXs))
        Text(
            text = label,
            color = if (highlighted || sorted) palette.textPrimary else palette.textSecondary,
            fontSize = fontSize,
            fontWeight = if (highlighted || sorted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (sorted) {
            Icon(
                imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier
                    .size(VennelaTokens.iconSm)
                    .padding(end = VennelaTokens.spaceXs)
            )
        } else {
            Spacer(Modifier.width(VennelaTokens.spaceSm))
        }
    }
}

@Composable
private fun SheetDataCell(
    cell: SpreadsheetCell?,
    width: Dp,
    height: Dp,
    coordinate: String,
    inRange: Boolean,
    isAnchor: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val value = cell?.value.orEmpty()
    val tagColor = when (cell?.tagColor) {
        "green" -> accent.secondary
        "amber" -> accent.primary
        "red" -> palette.textSecondary
        null -> null
        else -> palette.textSecondary
    }
    val negative = value.startsWith("-")

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .padding(horizontal = VennelaTokens.spaceXxs)
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .background(
                when {
                    isAnchor -> accent.primary.copy(alpha = 0.34f)
                    inRange -> accent.primary.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isAnchor) 1.dp else 0.dp,
                color = if (isAnchor) accent.secondary else Color.Transparent,
                shape = RoundedCornerShape(VennelaTokens.radiusChip)
            )
            .semantics {
                contentDescription = if (value.isBlank()) {
                    "Cell $coordinate, empty"
                } else {
                    "Cell $coordinate, $value"
                }
            }
            .pressScale(pressedScale = 0.96f, onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceSm),
        contentAlignment = Alignment.CenterStart
    ) {
        if (tagColor != null && value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                    .background(tagColor.copy(alpha = 0.20f))
                    .padding(
                        horizontal = VennelaTokens.spaceSm,
                        vertical = VennelaTokens.spaceXxs
                    )
            ) {
                Text(
                    text = value,
                    color = tagColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = value,
                color = if (negative) palette.textSecondary else palette.textPrimary,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = if (negative) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Real footer with a real control instead of silent truncation. */
@Composable
private fun SheetRowFooter(
    shown: Int,
    total: Int,
    cap: Int,
    onLoadMore: () -> Unit,
    onShowAll: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.touchTargetMin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildString {
                append("Showing $shown of $total data rows")
                if (total > cap) append(" • $cap row render cap")
            },
            color = palette.textTertiary,
            fontSize = VennelaTokens.micro,
            modifier = Modifier.weight(1f)
        )
        if (shown < total) {
            LiquidChip(
                text = "Load more",
                icon = Icons.Default.KeyboardArrowDown,
                isActive = false,
                onClick = onLoadMore,
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            LiquidChip(
                text = if (total > cap) "Show first $cap" else "Show all",
                icon = Icons.Default.ArrowUpward,
                isActive = true,
                onClick = onShowAll,
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        } else if (total > 0) {
            Text(
                text = "All rows loaded",
                color = accent.secondary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Real sheet tabs. Scrolls rather than clipping a busy workbook. */
@Composable
private fun SheetTabRail(
    sheets: List<SpreadsheetSheet>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val palette = LocalVennelaPalette.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(vertical = VennelaTokens.spaceXs)
    ) {
        item(key = "layers") {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Workbook sheets",
                tint = palette.textSecondary,
                modifier = Modifier.size(VennelaTokens.iconLg)
            )
        }
        items(sheets.size, key = { index -> "sheet_$index" }) { index ->
            LiquidChip(
                text = sheets[index].name,
                isActive = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Statistics mode                                                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun SheetStatsSection(
    sheetName: String,
    scannedRows: Int,
    totalRows: Int,
    columns: List<ColumnStats>,
    selectionStats: ColumnStats?,
    selectionAddress: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
        contentPadding = PaddingValues(bottom = VennelaTokens.spaceLg)
    ) {
        if (selectionStats != null) {
            item(key = "selection_stats") {
                StatsCard(
                    title = "Selection ${selectionAddress.orEmpty()}",
                    subtitle = "${selectionStats.numericCount} numeric cells",
                    accentValue = selectionStats.sum,
                    rows = listOf(
                        "Count" to formatNumber(selectionStats.numericCount.toDouble()),
                        "Sum" to formatNumber(selectionStats.sum),
                        "Mean" to formatNumber(selectionStats.mean),
                        "Min" to formatNumber(selectionStats.min),
                        "Max" to formatNumber(selectionStats.max)
                    )
                )
            }
        } else {
            item(key = "selection_hint") {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusControl),
                    alpha = 0.34f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(VennelaTokens.spaceLg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = accent.secondary,
                            modifier = Modifier.size(VennelaTokens.iconMd)
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Text(
                            text = "Select a cell or drag a range in the Grid mode to see its statistics here.",
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.bodySmall
                        )
                    }
                }
            }
        }

        item(key = "stats_header") {
            SectionHeader(
                title = "Numeric columns",
                trailing = {
                    Text(
                        text = "${columns.size} found",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro
                    )
                }
            )
        }

        if (columns.isEmpty()) {
            item(key = "stats_empty") {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.38f
                ) {
                    VennelaEmptyState(
                        title = "No numeric column in “$sheetName”",
                        hint = "Statistics are only shown for columns that genuinely contain numbers. " +
                            "This sheet holds ${totalRows} rows of non-numeric data.",
                        icon = Icons.Default.Calculate
                    )
                }
            }
        } else {
            items(columns.size, key = { index -> "stats_${columns[index].column}" }) { index ->
                val stats = columns[index]
                StatsCard(
                    title = stats.header,
                    subtitle = "Column ${columnName(stats.column)} • ${stats.numericCount} numbers",
                    accentValue = stats.sum,
                    rows = listOf(
                        "Count" to formatNumber(stats.numericCount.toDouble()),
                        "Sum" to formatNumber(stats.sum),
                        "Mean" to formatNumber(stats.mean),
                        "Min" to formatNumber(stats.min),
                        "Max" to formatNumber(stats.max)
                    )
                )
            }
            if (totalRows > scannedRows) {
                item(key = "stats_note") {
                    Text(
                        text = "Statistics computed from the first $scannedRows of $totalRows rows.",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    subtitle: String,
    accentValue: Double,
    rows: List<Pair<String, String>>
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VennelaTokens.radiusCard),
        alpha = 0.42f,
        elevation = VennelaTokens.elevationRaised
    ) {
        Column(modifier = Modifier.padding(VennelaTokens.spaceLg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatNumber(accentValue),
                    color = accent.secondary,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs)
            ) {
                rows.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                            .background(palette.textPrimary.copy(alpha = 0.05f))
                            .padding(vertical = VennelaTokens.spaceSm, horizontal = VennelaTokens.spaceXs),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.nano,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceXxs))
                        Text(
                            text = value,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.micro,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Row browser mode                                                           */
/* ------------------------------------------------------------------------- */

@Composable
private fun SheetDataSection(
    sheet: SpreadsheetSheet,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val rows = sheet.rows
    val filteredRows = remember(sheet, query) {
        if (query.isBlank()) {
            rows.withIndex().take(MaxDataRows).map { it.index to it.value }
        } else {
            rows.withIndex()
                .filter { (_, row) -> row.any { it.value.contains(query, ignoreCase = true) } }
                .take(MaxDataRows)
                .map { it.index to it.value }
        }
    }
    val matchCount = remember(sheet, query) {
        if (query.isBlank()) rows.size
        else rows.count { row -> row.any { it.value.contains(query, ignoreCase = true) } }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd),
        contentPadding = PaddingValues(bottom = VennelaTokens.spaceLg)
    ) {
        item(key = "data_search") {
            VennelaSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Filter sheet rows..."
            )
        }

        item(key = "data_stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                StatTile("Rows", "${rows.size}", Modifier.weight(1f))
                StatTile("Columns", "${sheet.headers.size}", Modifier.weight(1f))
                StatTile("Matching", "$matchCount", Modifier.weight(1f), accent.secondary)
            }
        }

        if (filteredRows.isEmpty()) {
            item(key = "data_empty") {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.38f
                ) {
                    VennelaEmptyState(
                        title = if (rows.isEmpty()) "This sheet has no rows" else "No rows match “$query”",
                        hint = if (rows.isEmpty()) {
                            "The workbook parsed cleanly but this sheet came back empty."
                        } else {
                            "Filtering runs across every cell of ${sheet.name}. Clear it to see all " +
                                "${rows.size} rows again."
                        },
                        icon = Icons.Default.FilterList,
                        actionLabel = if (query.isBlank()) null else "Clear filter",
                        onAction = if (query.isBlank()) null else ({ onQueryChange("") })
                    )
                }
            }
        } else {
            item(key = "data_list") {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCard),
                    alpha = 0.40f,
                    elevation = VennelaTokens.elevationRaised
                ) {
                    Column(modifier = Modifier.padding(vertical = VennelaTokens.spaceXs)) {
                        filteredRows.forEachIndexed { index, (rowNumber, row) ->
                            val cells = row.filter { it.value.isNotBlank() }
                            GlassListRow(
                                title = if (cells.isEmpty()) "Empty row"
                                else cells.joinToString("  •  ") { it.value },
                                subtitle = "Row ${rowNumber + 2} • ${row.size} cells"
                            )
                            if (index < filteredRows.lastIndex) GlassDivider()
                        }
                    }
                }
            }
            item(key = "data_footer") {
                Text(
                    text = if (matchCount > filteredRows.size) {
                        "Showing ${filteredRows.size} of $matchCount matching rows"
                    } else {
                        "Showing all ${filteredRows.size} matching rows"
                    },
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.micro
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassPanel(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(VennelaTokens.radiusCard),
            clip = false,
            ambientColor = accent.primary.copy(alpha = 0.40f),
            spotColor = accent.secondary.copy(alpha = 0.40f)
        ),
        alpha = 0.46f,
        elevation = VennelaTokens.elevationRaised
    ) {
        Column(modifier = Modifier.padding(VennelaTokens.spaceMd)) {
            Text(
                text = label,
                color = accent.secondary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(VennelaTokens.spaceXxs))
            Text(
                text = value,
                color = valueColor ?: palette.textPrimary,
                fontSize = VennelaTokens.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Bottom tool dock                                                           */
/* ------------------------------------------------------------------------- */

@Composable
private fun SheetToolDock(
    mode: SheetMode,
    findOpen: Boolean,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onFind: () -> Unit,
    onModeChange: (SheetMode) -> Unit,
    onMore: () -> Unit
) {
    // A compact floating pill: six icon actions, no labels, so the grid keeps
    // almost the whole screen.
    ReaderBottomPill {
        SheetDockAction(Icons.Default.Share, "Share sheet", false, onShare)
        SheetDockAction(Icons.AutoMirrored.Filled.OpenInNew, "Open with another app", false, onOpenWith)
        SheetDockAction(Icons.AutoMirrored.Filled.ManageSearch, "Find in sheet", findOpen, onFind)
        SheetDockAction(
            Icons.Default.Calculate,
            "Statistics",
            mode == SheetMode.STATS
        ) { onModeChange(SheetMode.STATS) }
        SheetDockAction(
            Icons.Default.TableChart,
            "Grid",
            mode == SheetMode.SHEET
        ) { onModeChange(SheetMode.SHEET) }
        SheetDockAction(Icons.Default.Info, "File details", false, onMore)
    }
}

@Composable
private fun RowScope.SheetDockAction(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .pressScale(pressedScale = 0.94f, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = VennelaTokens.spaceXs, vertical = VennelaTokens.spaceXxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) accent.secondary else palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
    }
}

/* ------------------------------------------------------------------------- */
