package com.vennela365.viewer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.CodeSyntaxHighlighter
import com.vennela365.viewer.parser.NotebookCell
import com.vennela365.viewer.parser.NotebookOutput
import com.vennela365.viewer.parser.NotebookParser
import com.vennela365.viewer.theme.*
import com.vennela365.viewer.ui.components.*
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

private enum class NotebookFilter(val label: String) {
    ALL("All"),
    CODE("Code"),
    MARKDOWN("Markdown"),
    PLOTS("Plots & Outputs")
}

@Composable
fun NotebookViewerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val docText by viewModel.documentText.collectAsState()

    val source = docText.ifBlank { file?.sampleContent.orEmpty() }
    val ext = file?.extension?.lowercase().orEmpty().ifBlank { "ipynb" }
    val notebook = remember(source, ext) { NotebookParser.parse(source, ext) }

    var filter by remember { mutableStateOf(NotebookFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var zoomedPlot by remember { mutableStateOf<NotebookOutput.InlineImage?>(null) }

    BackHandler(enabled = zoomedPlot != null) {
        zoomedPlot = null
    }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusInset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ViewerTopBar(
                    title = file?.name ?: "Notebook",
                    subtitle = "${notebook.kernelName} • ${notebook.cells.size} cells",
                    onBack = { viewModel.navigateBack() },
                    leadingIcon = Icons.Default.Code,
                    actions = {
                        ViewerIconAction(
                            icon = Icons.Default.Info,
                            contentDescription = "File details",
                            modifier = Modifier.testTag("nb_info_button")
                        ) { viewModel.navigateTo(Screen.FileMetadata) }
                    }
                )

                if (file == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = 820.dp)
                            .padding(VennelaTokens.gutter),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard {
                            VennelaEmptyState(
                                title = "No notebook open",
                                hint = "Open an .ipynb, .qmd, or .rmd file to inspect Markdown cells, syntax-highlighted code, and saved Matplotlib outputs.",
                                icon = Icons.Default.Code,
                                actionLabel = "Browse files",
                                onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                            )
                        }
                    }
                    return@Column
                }

                val filteredCells = remember(notebook.cells, filter, searchQuery) {
                    notebook.cells.filter { cell ->
                        val matchFilter = when (filter) {
                            NotebookFilter.ALL -> true
                            NotebookFilter.CODE -> cell.cellType == "code"
                            NotebookFilter.MARKDOWN -> cell.cellType == "markdown"
                            NotebookFilter.PLOTS -> cell.outputs.isNotEmpty()
                        }
                        val matchSearch = searchQuery.isBlank() ||
                            cell.source.contains(searchQuery, true) ||
                            cell.outputs.any { it is NotebookOutput.TextStream && it.text.contains(searchQuery, true) }
                        matchFilter && matchSearch
                    }
                }

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
                    item(key = "header") {
                        GlassCard(alpha = 0.50f) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${notebook.kernelName} • ${notebook.nbFormat}",
                                        color = palette.textPrimary,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Read-only sandbox • ${notebook.codeCount} code • ${notebook.markdownCount} markdown • ${notebook.outputImageCount} plots",
                                        color = palette.textSecondary,
                                        fontFamily = VennelaFonts.Mono,
                                        fontSize = VennelaTokens.micro
                                    )
                                }
                                IconBadge(
                                    icon = Icons.Default.Shield,
                                    colors = listOf(NeonGreen, accent.secondary),
                                    size = VennelaTokens.badgeSm,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    item(key = "controls") {
                        Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                            VennelaSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Filter notebook cells or outputs…",
                                modifier = Modifier.testTag("nb_search_input")
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                            ) {
                                NotebookFilter.entries.forEach { f ->
                                    LiquidChip(
                                        text = f.label,
                                        isActive = filter == f,
                                        modifier = Modifier.testTag("nb_filter_${f.name.lowercase()}"),
                                        onClick = { filter = f }
                                    )
                                }
                            }
                        }
                    }

                    if (notebook.error != null) {
                        item(key = "error") {
                            GlassCard(alpha = 0.55f) {
                                Text(
                                    text = notebook.error,
                                    color = NeonCoral,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (filteredCells.isEmpty()) {
                        item(key = "empty_filtered") {
                            GlassCard {
                                Text(
                                    text = "No cells match the current filter.",
                                    color = palette.textSecondary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.bodySmall
                                )
                            }
                        }
                    } else {
                        items(filteredCells, key = { it.index }) { cell ->
                            NotebookCellCard(
                                cell = cell,
                                language = notebook.language,
                                onCopyCell = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Cell ${cell.index + 1}", cell.source))
                                    Toast.makeText(context, "Cell copied", Toast.LENGTH_SHORT).show()
                                },
                                onPlotClick = { plot -> zoomedPlot = plot }
                            )
                        }
                    }
                }
            }

            // Plot lightbox overlay
            zoomedPlot?.let { plot ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .clickable { zoomedPlot = null }
                        .padding(VennelaTokens.gutter),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = plot.bitmap.asImageBitmap(),
                        contentDescription = "Expanded notebook plot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(VennelaTokens.radiusCard))
                    )
                    ReaderIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close plot preview",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = statusInset, end = 8.dp)
                            .testTag("nb_close_plot"),
                        onClick = { zoomedPlot = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookCellCard(
    cell: NotebookCell,
    language: String,
    onCopyCell: () -> Unit,
    onPlotClick: (NotebookOutput.InlineImage) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val isCode = cell.cellType == "code"
    val highlighted = remember(cell.source, language, isCode) {
        if (isCode) CodeSyntaxHighlighter.highlight(cell.source, language) else null
    }

    GlassCard(alpha = if (isCode) 0.54f else 0.42f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val badgeLabel = if (isCode) {
                    "In [${cell.executionCount?.toString() ?: " "}]"
                } else {
                    cell.cellType.uppercase()
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(if (isCode) accent.primary.copy(alpha = 0.22f) else accent.secondary.copy(alpha = 0.18f))
                        .border(
                            1.dp,
                            (if (isCode) accent.primary else accent.secondary).copy(alpha = 0.45f),
                            RoundedCornerShape(VennelaTokens.radiusPill)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = if (isCode) accent.secondary else palette.textPrimary,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.nano,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isCode) {
                    Text(
                        text = language,
                        color = palette.textTertiary,
                        fontFamily = VennelaFonts.Mono,
                        fontSize = VennelaTokens.nano
                    )
                }
            }

            ReaderIconButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "Copy cell ${cell.index + 1}",
                modifier = Modifier.testTag("nb_copy_cell_${cell.index}"),
                onClick = onCopyCell
            )
        }

        Spacer(Modifier.height(VennelaTokens.spaceSm))

        if (isCode && highlighted != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                    .background(Color(0xFF0A0F1A))
                    .border(1.dp, palette.cardBorder.copy(alpha = 0.45f), RoundedCornerShape(VennelaTokens.radiusControl))
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
        } else {
            val mdLines = remember(cell.source) { cell.source.lines() }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mdLines.forEach { rawLine ->
                    val trimmed = rawLine.trimStart()
                    val headingLevel = when {
                        trimmed.startsWith("### ") -> 3
                        trimmed.startsWith("## ") -> 2
                        trimmed.startsWith("# ") -> 1
                        else -> 0
                    }
                    val textBody = trimmed.removePrefix("### ").removePrefix("## ").removePrefix("# ")
                    Text(
                        text = textBody,
                        color = palette.textPrimary,
                        fontFamily = if (headingLevel > 0) VennelaFonts.Serif else VennelaFonts.Sans,
                        fontSize = when (headingLevel) {
                            1 -> VennelaTokens.titleLarge
                            2 -> VennelaTokens.titleMedium
                            3 -> VennelaTokens.titleSmall
                            else -> VennelaTokens.bodySmall
                        },
                        fontWeight = if (headingLevel > 0) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (cell.outputs.isNotEmpty()) {
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.cardBorder.copy(alpha = 0.35f))
            )
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                cell.outputs.forEach { out ->
                    when (out) {
                        is NotebookOutput.TextStream -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                    .background(if (out.isError) NeonCoral.copy(alpha = 0.14f) else palette.cardSurfaceStrong)
                                    .border(
                                        1.dp,
                                        if (out.isError) NeonCoral.copy(alpha = 0.55f) else palette.cardBorder.copy(alpha = 0.35f),
                                        RoundedCornerShape(VennelaTokens.radiusChip)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = out.name.uppercase(),
                                    color = if (out.isError) NeonCoral else palette.textTertiary,
                                    fontFamily = VennelaFonts.Mono,
                                    fontSize = VennelaTokens.nano,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = out.text.take(2500),
                                    color = if (out.isError) NeonCoral else palette.textPrimary,
                                    fontFamily = VennelaFonts.Mono,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        is NotebookOutput.InlineImage -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                                    .background(Color.White)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusControl))
                                    .clickable { onPlotClick(out) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = out.bitmap.asImageBitmap(),
                                    contentDescription = "Notebook inline output plot",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 320.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
