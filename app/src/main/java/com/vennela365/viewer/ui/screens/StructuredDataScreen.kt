package com.vennela365.viewer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.CodeSyntaxHighlighter
import com.vennela365.viewer.parser.DataNode
import com.vennela365.viewer.parser.DataNodeKind
import com.vennela365.viewer.parser.StructuredDataParser
import com.vennela365.viewer.theme.*
import com.vennela365.viewer.ui.components.*
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen

private enum class StructuredViewTab(val label: String) {
    TREE("Interactive Tree"),
    PRETTY("Formatted"),
    RAW("Raw")
}

private data class VisibleTreeRow(
    val node: DataNode,
    val isExpanded: Boolean,
    val hasChildren: Boolean
)

@Composable
fun StructuredDataInspectorScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()
    val docText by viewModel.documentText.collectAsState()

    val source = docText.ifBlank { file?.sampleContent.orEmpty() }
    val ext = file?.extension?.lowercase().orEmpty().ifBlank { "json" }
    val parsed = remember(source, ext) { StructuredDataParser.parseStructuredTree(source, ext) }
    val driveShortcut = remember(source, ext) {
        if (ext in setOf("gdoc", "gsheet", "gslides", "gdraw", "gform", "gsite", "gshortcut")) {
            StructuredDataParser.parseGoogleShortcut(source)
        } else null
    }

    var activeTab by remember { mutableStateOf(StructuredViewTab.TREE) }
    var searchQuery by remember { mutableStateOf("") }
    val expandedIds = remember(file?.id, parsed.roots) {
        mutableStateMapOf<String, Boolean>().apply {
            parsed.roots.forEach { put(it.id, true) }
        }
    }

    val flatRows = remember(parsed.roots, expandedIds.toMap(), searchQuery) {
        buildList {
            val q = searchQuery.trim()
            fun matchesAny(n: DataNode): Boolean {
                if (q.isEmpty()) return true
                if (n.key.contains(q, true) || n.previewValue.contains(q, true) || n.path.contains(q, true)) return true
                return n.children.any { matchesAny(it) }
            }

            fun walk(node: DataNode) {
                if (!matchesAny(node)) return
                val hasKids = node.children.isNotEmpty()
                val expanded = if (q.isNotEmpty()) true else (expandedIds[node.id] ?: (node.depth < 1))
                add(VisibleTreeRow(node, expanded, hasKids))
                if (expanded) {
                    node.children.forEach { walk(it) }
                }
            }
            parsed.roots.forEach { walk(it) }
        }
    }

    fun copyText(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
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
                title = file?.name ?: "Structured Data",
                subtitle = "${parsed.formatLabel} • ${parsed.totalNodes} nodes",
                onBack = { viewModel.navigateBack() },
                leadingIcon = Icons.Default.DataObject,
                actions = {
                    if (source.isNotBlank()) {
                        ViewerIconAction(
                            icon = Icons.Default.ContentCopy,
                            contentDescription = "Copy formatted source",
                            modifier = Modifier.testTag("sd_copy_all")
                        ) {
                            copyText(file?.name ?: "source", parsed.prettyFormatted)
                        }
                    }
                    ViewerIconAction(
                        icon = Icons.Default.Info,
                        contentDescription = "File metadata",
                        modifier = Modifier.testTag("sd_info_button")
                    ) {
                        viewModel.navigateTo(Screen.FileMetadata)
                    }
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
                            title = "No structured file open",
                            hint = "Open a JSON, XML, YAML, TOML, INI, ENV, or Google Drive shortcut file to explore its collapsible tree.",
                            icon = Icons.Default.DataObject,
                            actionLabel = "Browse files",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                }
                return@Column
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
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
            ) {
                if (driveShortcut != null) {
                    item(key = "drive_shortcut") {
                        GlassCard(alpha = 0.55f) {
                            Text(
                                text = "Google Workspace Cloud Shortcut",
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            driveShortcut.docId?.let {
                                Text(
                                    text = "Document ID: $it",
                                    color = palette.textSecondary,
                                    fontFamily = VennelaFonts.Mono,
                                    fontSize = VennelaTokens.caption
                                )
                            }
                            driveShortcut.email?.let {
                                Text(
                                    text = "Account: $it",
                                    color = palette.textTertiary,
                                    fontFamily = VennelaFonts.Sans,
                                    fontSize = VennelaTokens.caption
                                )
                            }
                            driveShortcut.url?.let { url ->
                                Spacer(Modifier.height(VennelaTokens.spaceSm))
                                LiquidButton(
                                    label = "Open in Google Drive / Browser",
                                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                                    modifier = Modifier.testTag("sd_open_drive_url"),
                                    onClick = {
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }.onFailure {
                                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item(key = "controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)) {
                        VennelaSegmentedControl(
                            options = StructuredViewTab.entries.map { it.label },
                            selected = activeTab.label,
                            onSelect = { label ->
                                activeTab = StructuredViewTab.entries.first { it.label == label }
                            }
                        )
                        if (activeTab == StructuredViewTab.TREE) {
                            VennelaSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search keys, values, or JSONPath…",
                                modifier = Modifier.testTag("sd_search_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                            ) {
                                LiquidChip(
                                    text = "Expand All",
                                    icon = Icons.Default.UnfoldMore,
                                    isActive = false,
                                    modifier = Modifier.weight(1f).testTag("sd_expand_all"),
                                    onClick = {
                                        fun markAll(list: List<DataNode>) {
                                            list.forEach {
                                                if (it.children.isNotEmpty()) {
                                                    expandedIds[it.id] = true
                                                    markAll(it.children)
                                                }
                                            }
                                        }
                                        markAll(parsed.roots)
                                    }
                                )
                                LiquidChip(
                                    text = "Collapse All",
                                    icon = Icons.Default.UnfoldLess,
                                    isActive = false,
                                    modifier = Modifier.weight(1f).testTag("sd_collapse_all"),
                                    onClick = {
                                        expandedIds.clear()
                                        parsed.roots.forEach { expandedIds[it.id] = false }
                                    }
                                )
                            }
                        }
                    }
                }

                when {
                    source.isBlank() -> item(key = "blank") {
                        GlassCard {
                            Text(
                                text = "This file returned no readable UTF-8 text. Inspect raw bytes in Hex Viewer.",
                                color = palette.textSecondary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.bodySmall
                            )
                        }
                    }

                    activeTab == StructuredViewTab.TREE -> {
                        if (parsed.error != null) {
                            item(key = "parse_err") {
                                GlassCard(alpha = 0.50f) {
                                    Text(
                                        text = "Syntax notice: ${parsed.error}",
                                        color = NeonAmber,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (flatRows.isEmpty()) {
                            item(key = "no_nodes") {
                                GlassCard {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No nodes match \"$searchQuery\"."
                                        else "Switch to Formatted or Raw tab to inspect source.",
                                        color = palette.textSecondary,
                                        fontFamily = VennelaFonts.Sans,
                                        fontSize = VennelaTokens.bodySmall
                                    )
                                }
                            }
                        } else {
                            items(flatRows, key = { it.node.id }) { row ->
                                TreeNodeRow(
                                    row = row,
                                    onToggleExpand = {
                                        expandedIds[row.node.id] = !row.isExpanded
                                    },
                                    onCopyNode = {
                                        val payload = if (row.node.fullValue.isNotBlank()) {
                                            "${row.node.path} = ${row.node.fullValue}"
                                        } else {
                                            row.node.path
                                        }
                                        copyText("path & value", payload)
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        val display = if (activeTab == StructuredViewTab.PRETTY) parsed.prettyFormatted else source
                        val bounded = display.take(40_000)
                        val highlighted = CodeSyntaxHighlighter.highlight(bounded, if (ext in setOf("jsonc", "json5", "ndjson", "geojson")) "json" else ext)
                        item(key = "code_view") {
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
                                if (display.length > bounded.length) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Truncated to first 40,000 characters for smooth 120 FPS rendering.",
                                        color = palette.textTertiary,
                                        fontFamily = VennelaFonts.Sans,
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

@Composable
private fun TreeNodeRow(
    row: VisibleTreeRow,
    onToggleExpand: () -> Unit,
    onCopyNode: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val n = row.node
    val indentDp = (n.depth.coerceAtMost(8) * 14).dp

    val (badgeText, badgeColor) = when (n.kind) {
        DataNodeKind.OBJECT -> "{ }" to accent.primary
        DataNodeKind.ARRAY -> "[ ]" to accent.secondary
        DataNodeKind.ELEMENT -> "<>" to CatCyan
        DataNodeKind.STRING -> "str" to NeonAmber
        DataNodeKind.NUMBER -> "num" to CatViolet
        DataNodeKind.BOOLEAN -> "bool" to NeonGreen
        DataNodeKind.NULL -> "null" to NeonCoral
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(palette.cardSurfaceStrong.copy(alpha = 0.72f))
            .border(1.dp, palette.cardBorder.copy(alpha = 0.35f), RoundedCornerShape(VennelaTokens.radiusControl))
            .heightIn(min = VennelaTokens.touchTargetMin)
            .clickable {
                if (row.hasChildren) onToggleExpand() else onCopyNode()
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (row.hasChildren) {
            Icon(
                imageVector = if (row.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (row.isExpanded) "Collapse" else "Expand",
                tint = accent.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.20f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                color = badgeColor,
                fontFamily = VennelaFonts.Mono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = n.key,
                color = palette.textPrimary,
                fontFamily = VennelaFonts.Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = n.previewValue,
                color = palette.textSecondary,
                fontFamily = VennelaFonts.Mono,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        ReaderIconButton(
            icon = Icons.Default.ContentCopy,
            contentDescription = "Copy ${n.path}",
            onClick = onCopyNode
        )
    }
}
