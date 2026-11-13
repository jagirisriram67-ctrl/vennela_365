package com.vennela365.viewer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.model.HexRow
import com.vennela365.viewer.parser.FileParserManager
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.FloatingDockNav
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.ui.components.VennelaSearchField
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import java.util.Locale

/* ------------------------------------------------------------------------- */
/* Shared, real-data helpers for the two fallback screens                     */
/* ------------------------------------------------------------------------- */

private sealed class GuessConfidence {
    object Unavailable : GuessConfidence()
    data class Value(val container: String, val score: Float, val reason: String) : GuessConfidence()
}

private fun sniffBytes(rows: List<HexRow>, maxBytes: Int = 32): ByteArray {
    val row = rows.firstOrNull { it.offset != "ERROR" } ?: return ByteArray(0)
    val tokens = row.hexBytes.trim().split(" ").filter { it.length == 2 }
    val size = minOf(tokens.size, maxBytes)
    return ByteArray(size) { i -> (tokens[i].toIntOrNull(16) ?: 0).toByte() }
}

private fun hexMatchesSignature(bytes: ByteArray, signature: String): Boolean {
    val tokens = signature.trim().split(" ").filter { it.isNotBlank() }
    if (tokens.isEmpty()) return false
    val expected = ArrayList<Byte>()
    for (token in tokens) {
        val isHex = token.length == 2 && token.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        if (isHex) expected.add((token.toIntOrNull(16) ?: 0).toByte())
        else token.forEach { expected.add(it.code.toByte()) }
    }
    if (expected.isEmpty() || bytes.size < expected.size) return false
    return expected.indices.all { bytes[it] == expected[it] }
}

private fun guessContent(bytes: ByteArray, file: FileItem): GuessConfidence {
    if (bytes.isEmpty()) return GuessConfidence.Unavailable
    val container = FileParserManager.detectMagicFormat(bytes)
    val declared = file.formatInfo.signatures
    val declaredMatch = declared.any { hexMatchesSignature(bytes, it) }
    return when {
        declaredMatch -> GuessConfidence.Value(
            container = container,
            score = 0.95f,
            reason = "Bytes match the signature registered for .${file.extension}."
        )
        declared.isNotEmpty() -> GuessConfidence.Value(
            container = container,
            score = 0.22f,
            reason = "Extension .${file.extension} declares a different signature — the extension is unreliable."
        )
        container == "Binary Data Stream" -> GuessConfidence.Value(
            container = container,
            score = 0.28f,
            reason = "No known magic bytes and no registered signature for .${file.extension}."
        )
        else -> GuessConfidence.Value(
            container = container,
            score = 0.7f,
            reason = "Container signature recognized; .${file.extension} registers nothing to compare."
        )
    }
}

private fun confidenceWord(score: Float): String = when {
    score >= 0.85f -> "High confidence"
    score >= 0.5f -> "Medium confidence"
    score > 0f -> "Low confidence"
    else -> "No signal"
}

private fun hexTextOf(rows: List<HexRow>, maxRows: Int = 256): String =
    rows.take(maxRows).joinToString("\n") { "${it.offset}  ${it.hexBytes}  ${it.ascii}" }

private fun copyRawToClipboard(context: Context, value: String, message: String) {
    if (value.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Vennela 365", value))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun openWithAnotherApp(context: Context, file: FileItem) {
    val uri = file.uri
    val intent = if (uri != null) {
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.formatInfo.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_VIEW).apply {
            type = "*/*"
        }
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open ${file.name} with"))
    }.onFailure {
        Toast.makeText(context, "No installed app can open this file type.", Toast.LENGTH_LONG).show()
    }
}

/* ------------------------------------------------------------------------- */
/* Unknown file viewer                                                        */
/* ------------------------------------------------------------------------- */

@Composable
fun UnknownFileViewerScreen(viewModel: MainViewModel) {
    val hexRows by viewModel.hexRows.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var activeTab by remember { mutableStateOf("Hex") }
    var rawOnly by remember { mutableStateOf(false) }
    var hexStart by remember { mutableStateOf(0) }
    var jumpTarget by remember { mutableStateOf("") }
    var regexQuery by remember { mutableStateOf("") }

    val file = selectedFile
    val name = file?.name ?: "Raw inspector"

    LiquidPage(
        title = name,
        subtitle = if (file == null) {
            "No file selected"
        } else {
            "${file.formattedSize} • ${file.extension.uppercase().ifBlank { "Unknown" }} type"
        },
        onBack = { viewModel.navigateBack() },
        dock = {
            UnknownToolsDock(
                onSearch = { viewModel.navigateTo(Screen.UniversalSearch) },
                onEncoding = { viewModel.navigateTo(Screen.EncodingSelection) },
                onGoTo = {
                    activeTab = "Hex"
                    jumpTarget = jumpTarget.ifBlank { "00000000" }
                },
                onMore = { viewModel.navigateTo(Screen.FileMetadata) }
            )
        },
        topBarActions = {
            RoundBarAction(
                icon = Icons.Default.Search,
                description = "Search files",
                modifier = Modifier.testTag("unknown_search_button")
            ) { viewModel.navigateTo(Screen.UniversalSearch) }
            RoundBarAction(
                icon = Icons.Default.Info,
                description = "File metadata",
                modifier = Modifier.testTag("unknown_info_button")
            ) { viewModel.navigateTo(Screen.FileMetadata) }
        }
    ) { pagePadding ->
        if (file == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = pagePadding.calculateTopPadding() + VennelaTokens.spaceMd,
                    bottom = pagePadding.calculateBottomPadding()
                )
            ) {
                item {
                    GlassCard {
                        VennelaEmptyState(
                            title = "Nothing to inspect",
                            hint = "Open a file from the explorer or share one into Vennela 365 to inspect its raw bytes.",
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            actionLabel = "Browse files",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                }
            }
            return@LiquidPage
        }

        val bytes = remember(hexRows) { sniffBytes(hexRows) }
        val guess = remember(bytes, file.id) { guessContent(bytes, file) }
        val textPreview by produceState<String?>(null, file.uri, file.id) {
            value = when {
                !file.sampleContent.isBlank() -> file.sampleContent
                file.uri != null -> FileParserManager.readTextContent(context, file.uri, maxLines = 400)
                else -> ""
            }
        }
        val readError = hexRows.firstOrNull { it.offset == "ERROR" }

        val visibleRows = remember(hexRows, hexStart) { hexRows.drop(hexStart).take(512) }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = pagePadding.calculateTopPadding() + VennelaTokens.spaceMd,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            /* -------- Identity -------- */
            item(key = "identity") {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeLg)
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                color = palette.textPrimary,
                                fontFamily = VennelaFonts.Sans,
                                fontSize = VennelaTokens.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${file.formattedSize} • ${String.format(Locale.US, "%,d", file.sizeBytes)} bytes",
                                color = palette.textSecondary,
                                fontFamily = VennelaFonts.Mono,
                                fontSize = VennelaTokens.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Text(
                        text = "Vennela 365 has no tested renderer registered for .${file.extension}, so the file is " +
                            "shown as raw content instead of a guessed picture.",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption,
                        lineHeight = 17.sp
                    )
                }
            }

            /* -------- Detection guess -------- */
            item(key = "guess") {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Fingerprint,
                            colors = listOf(accent.primary, accent.secondary),
                            size = VennelaTokens.badgeSm,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Text(
                            text = "Detected content",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    when (val result = guess) {
                        is GuessConfidence.Unavailable -> {
                            Text(
                                text = "No bytes could be read from this source, so no content guess can be made. " +
                                    "The extension .${file.extension} is all that is known.",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall,
                                lineHeight = 19.sp
                            )
                        }
                        is GuessConfidence.Value -> {
                            Text(
                                text = result.container,
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            ConfidenceBar(score = result.score, label = confidenceWord(result.score))
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            Text(
                                text = result.reason,
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.caption,
                                lineHeight = 17.sp
                            )
                            if (bytes.isNotEmpty()) {
                                Spacer(Modifier.height(VennelaTokens.spaceXs))
                                Text(
                                    text = "Magic bytes: " + bytes.take(16).joinToString(" ") {
                                        String.format(Locale.US, "%02X", it)
                                    },
                                    color = palette.textTertiary,
                                    fontSize = VennelaTokens.nano,
                                    fontFamily = VennelaFonts.Mono
                                )
                            }
                        }
                    }
                }
            }

            /* -------- Tabs -------- */
            item(key = "tabs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                ) {
                    listOf("Text", "Hex", "Metadata").forEach { tab ->
                        LiquidChip(
                            text = tab,
                            isActive = activeTab == tab,
                            onClick = {
                                when (tab) {
                                    "Metadata" -> viewModel.navigateTo(Screen.FileMetadata)
                                    else -> activeTab = tab
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = VennelaTokens.touchTargetMin)
                                .testTag("unknown_tab_${tab.lowercase()}")
                        )
                    }
                }
            }

            /* -------- Read error -------- */
            if (readError != null) {
                item(key = "readerror") {
                    VennelaErrorState(
                        title = "Byte stream unavailable",
                        message = readError.ascii.ifBlank { "The provider refused to open this file." },
                        onRetry = { viewModel.openFile(file) }
                    )
                }
            }

            /* -------- Text tab -------- */
            if (activeTab == "Text") {
                item(key = "regex") {
                    VennelaSearchField(
                        value = regexQuery,
                        onValueChange = { regexQuery = it },
                        placeholder = "Regex or plain text to find in this file…"
                    )
                }
                item(key = "text") {
                    val text = textPreview
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Decoded text preview",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (text == null) "reading…" else "${text.lineSequence().count()} lines",
                                color = palette.textTertiary,
                                fontSize = VennelaTokens.caption
                            )
                        }
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        GlassDivider()
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        when {
                            text == null -> VennelaLoadingState(label = "Decoding bytes as UTF-8…")
                            text.isBlank() -> Text(
                                text = "This file produced no printable text — it is binary. Use the Hex tab.",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall
                            )
                            else -> {
                                RegexMatches(
                                    text = text,
                                    pattern = regexQuery,
                                    accent = accent.secondary,
                                    palette = palette
                                )
                            }
                        }
                    }
                }
            }

            /* -------- Hex tab -------- */
            if (activeTab == "Hex") {
                item(key = "hexheader") {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Size: ${String.format(Locale.US, "%,d", file.sizeBytes)} bytes",
                                    color = palette.textPrimary,
                                    fontSize = VennelaTokens.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${hexRows.size} rows read • 16 bytes/line",
                                    color = palette.textSecondary,
                                    fontSize = VennelaTokens.caption
                                )
                            }
                            LiquidChip(
                                text = if (rawOnly) "ASCII only" else "Hex + ASCII",
                                isActive = rawOnly,
                                modifier = Modifier
                                    .heightIn(min = VennelaTokens.touchTargetMin)
                                    .testTag("unknown_hex_mode_chip"),
                                onClick = { rawOnly = !rawOnly }
                            )
                        }
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InlineOffsetField(
                                value = jumpTarget,
                                onValueChange = { jumpTarget = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unknown_offset_field")
                            )
                            SmallAction(
                                label = "Go to",
                                icon = Icons.Default.MyLocation,
                                modifier = Modifier.testTag("unknown_goto_button"),
                                onClick = {
                                    val wanted = jumpTarget.trim().removePrefix("0x").uppercase()
                                    val parsed = wanted.toLongOrNull(16)
                                    val index = when {
                                        parsed == null -> -1
                                        else -> hexRows.indexOfFirst {
                                            it.offset.toLongOrNull(16) == parsed
                                        }
                                    }
                                    if (index >= 0) {
                                        hexStart = index
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Enter a hex offset between 00000000 and ${hexRows.lastOrNull()?.offset ?: "0"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }

                itemsIndexed(visibleRows, key = { _, row -> row.offset }) { index, row ->
                    HexLine(
                        row = row,
                        rawOnly = rawOnly,
                        index = hexStart + index,
                        onCopy = {
                            copyRawToClipboard(
                                context,
                                "${row.offset}  ${row.hexBytes}  ${row.ascii}",
                                "Row ${row.offset} copied"
                            )
                        }
                    )
                }

                if (hexRows.size > hexStart + 512) {
                    item(key = "hexmore") {
                        Text(
                            text = "${hexRows.size - hexStart - 512} more rows are read but not listed here. " +
                                "Open the dedicated hex viewer to page through all of them.",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.caption,
                            modifier = Modifier.padding(horizontal = VennelaTokens.spaceXs)
                        )
                    }
                }
            }

            /* -------- Actions -------- */
            item(key = "actions") {
                GlassCard {
                    Text(
                        text = "What you can do",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    FallbackActionRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "Open with another app",
                        subtitle = "Hand the original file to any installed viewer",
                        onClick = { openWithAnotherApp(context, file) }
                    )
                    FallbackActionRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy raw bytes",
                        subtitle = "Copies the ${minOf(hexRows.size, 256)} preview rows as hex + ASCII text",
                        onClick = {
                            copyRawToClipboard(
                                context,
                                hexTextOf(hexRows),
                                "Raw bytes copied as text"
                            )
                        }
                    )
                    FallbackActionRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy decoded text",
                        subtitle = "Copies the UTF-8 text preview",
                        onClick = {
                            copyRawToClipboard(
                                context,
                                textPreview.orEmpty().take(64 * 1024),
                                "Text preview copied"
                            )
                        }
                    )
                    FallbackActionRow(
                        icon = Icons.Default.Description,
                        title = "Open in code viewer",
                        subtitle = "Syntax view for readable text found in the file",
                        onClick = { viewModel.navigateTo(Screen.CodeViewer) }
                    )
                    FallbackActionRow(
                        icon = Icons.Default.Info,
                        title = "Inspect metadata",
                        subtitle = "Size, dates, hashes, detection and permissions",
                        onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                    FallbackActionRow(
                        icon = Icons.Default.Translate,
                        title = "Change text encoding",
                        subtitle = "Re-decode the bytes with another charset",
                        onClick = { viewModel.navigateTo(Screen.EncodingSelection) }
                    )
                    FallbackActionRow(
                        icon = Icons.Default.Share,
                        title = "Share",
                        subtitle = "Send or save a copy of the original file",
                        onClick = { viewModel.navigateTo(Screen.ShareScreen) }
                    )
                }
            }

            item(key = "safety") {
                SafetyNote("Nothing here is executed or uploaded. Bytes are read-only and stay on this device.")
            }

            item(key = "footer") { Spacer(Modifier.height(VennelaTokens.spaceSm)) }
        }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Unsupported file screen                                                    */
/* ------------------------------------------------------------------------- */

@Composable
fun UnsupportedFileScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsState()
    val hexRows by viewModel.hexRows.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file = selectedFile

    LiquidPage(
        title = file?.name ?: "Unsupported file",
        subtitle = file?.uri?.toString() ?: "Choose a file to inspect",
        onBack = { viewModel.navigateBack() },
        dock = {
            FloatingDockNav(
                activeTab = "files",
                onTabSelected = { tab ->
                    when (tab) {
                        "home" -> viewModel.navigateTo(Screen.Home)
                        "files" -> viewModel.navigateTo(Screen.AllFiles)
                        "search" -> viewModel.navigateTo(Screen.UniversalSearch)
                        "favorites" -> viewModel.navigateTo(Screen.Favorites)
                        "settings" -> viewModel.navigateTo(Screen.Settings)
                    }
                }
            )
        },
        topBarActions = {
            RoundBarAction(
                icon = Icons.Default.Search,
                description = "Search files",
                modifier = Modifier.testTag("unsupported_search_button")
            ) { viewModel.navigateTo(Screen.UniversalSearch) }
            RoundBarAction(
                icon = Icons.Default.Info,
                description = "File metadata",
                modifier = Modifier.testTag("unsupported_info_button")
            ) { viewModel.navigateTo(Screen.FileMetadata) }
        }
    ) { pagePadding ->
        if (file == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VennelaTokens.gutter,
                    end = VennelaTokens.gutter,
                    top = pagePadding.calculateTopPadding() + VennelaTokens.spaceMd,
                    bottom = pagePadding.calculateBottomPadding()
                )
            ) {
                item {
                    GlassCard {
                        VennelaEmptyState(
                            title = "No file selected",
                            hint = "Open a file and Vennela will explain exactly what it can and cannot do with it.",
                            icon = Icons.Default.QuestionMark,
                            actionLabel = "Browse files",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                }
            }
            return@LiquidPage
        }

        val info = file.formatInfo
        val bytes = remember(hexRows) { sniffBytes(hexRows) }
        val guess = remember(bytes, file.id) { guessContent(bytes, file) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = pagePadding.calculateTopPadding() + VennelaTokens.spaceMd,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            /* -------- Hero -------- */
            item(key = "hero") {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconBadge(
                            icon = Icons.Default.QuestionMark,
                            colors = listOf(accent.primary, accent.secondary),
                            size = 64.dp,
                            radius = VennelaTokens.radiusControl,
                            contentDescription = null
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceMd))
                        Text(
                            text = "Preview not available",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceXs))
                        Text(
                            text = "Vennela 365 has no verified native renderer for this format yet.",
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.bodySmall,
                            lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(VennelaTokens.spaceMd))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(VennelaTokens.radiusTile))
                                .background(palette.cardSurface.copy(alpha = 0.5f))
                                .border(1.dp, palette.cardBorder.copy(alpha = 0.85f), RoundedCornerShape(VennelaTokens.radiusTile))
                                .padding(VennelaTokens.spaceMd),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgeFact(label = "File type", value = "${file.extension.uppercase().ifBlank { "Unknown" }} file")
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(palette.cardBorder)
                            )
                            BadgeFact(label = "Size", value = file.formattedSize)
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(palette.cardBorder)
                            )
                            BadgeFact(label = "Level", value = "L${info.levelNumber}")
                        }
                        Spacer(Modifier.height(VennelaTokens.spaceMd))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                        ) {
                            LiquidButton(
                                label = "View as Text",
                                onClick = { viewModel.navigateTo(Screen.CodeViewer) },
                                icon = Icons.Default.Description,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unsupported_view_text_button")
                            )
                            OutlinedGlassAction(
                                label = "Open with App",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unsupported_open_with_button"),
                                onClick = { openWithAnotherApp(context, file) }
                            )
                        }
                    }
                }
            }

            /* -------- Why unsupported -------- */
            item(key = "why") {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Shield,
                            colors = listOf(accent.primary, accent.secondary),
                            size = VennelaTokens.badgeSm,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Text(
                            text = "Why .${file.extension} is not rendered",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    GlassDivider()
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    ReasonLine("Support level", "L${info.levelNumber} • ${info.supportLevel.label}", palette)
                    Text(
                        text = info.supportLevel.description,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.caption,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    ReasonLine("Registered renderer", info.defaultRenderer, palette)
                    ReasonLine("Fallback handler", info.fallbackHandler, palette)
                    if (info.maxSafeBytes != null) {
                        ReasonLine(
                            label = "Safe inspection cap",
                            value = "${String.format(Locale.US, "%,d", info.maxSafeBytes)} bytes",
                            palette = palette
                        )
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    Text(
                        text = buildString {
                            append("A renderer is only used after it has been tested against this format, so the ")
                            append("registry stops at “${info.supportLevel.label}” for .${file.extension}. ")
                            if (info.levelNumber >= 3) {
                                append("The registered renderer targets ${info.category.displayName.lowercase()} ")
                                append("preview, which is unavailable on this build.")
                            } else {
                                append("Nothing is guessed from the extension alone — no renderer is attempted ")
                                append("until one is verified.")
                            }
                        },
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.bodySmall,
                        lineHeight = 19.sp
                    )
                    if (info.limitations.isNotBlank()) {
                        Spacer(Modifier.height(VennelaTokens.spaceSm))
                        ReasonLine("Documented limits", info.limitations, palette)
                    }
                    if (info.securityNote.isNotBlank()) {
                        ReasonLine("Security note", info.securityNote, palette)
                    }
                    if (info.description.isNotBlank()) {
                        ReasonLine("Registry description", info.description, palette)
                    }
                }
            }

            /* -------- Detection -------- */
            item(key = "guess") {
                GlassCard {
                    Text(
                        text = "Detected content",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    when (val result = guess) {
                        is GuessConfidence.Unavailable -> Text(
                            text = "No readable bytes were returned by this source, so content could not be verified.",
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.bodySmall,
                            lineHeight = 19.sp
                        )
                        is GuessConfidence.Value -> {
                            Text(
                                text = result.container,
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            ConfidenceBar(score = result.score, label = confidenceWord(result.score))
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            Text(
                                text = result.reason,
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.caption,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            /* -------- Hex fallback -------- */
            item(key = "hex") {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hex fallback",
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        SmallAction(
                            label = "Copy",
                            icon = Icons.Default.ContentCopy,
                            modifier = Modifier.testTag("unsupported_copy_hex_button"),
                            onClick = {
                                copyRawToClipboard(context, hexTextOf(hexRows, 64), "Hex preview copied")
                            }
                        )
                    }
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    if (hexRows.isEmpty()) {
                        Text(
                            text = "No bytes have been read yet for this file source. Open the hex viewer to retry the read.",
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.bodySmall,
                            lineHeight = 19.sp
                        )
                    } else {
                        hexRows.take(8).forEach { row ->
                            HexLine(row = row, rawOnly = false, index = 0, onCopy = null)
                        }
                        if (hexRows.size > 8) {
                            Spacer(Modifier.height(VennelaTokens.spaceXs))
                            Text(
                                text = "${hexRows.size - 8} further rows available in the hex viewer.",
                                color = palette.textTertiary,
                                fontSize = VennelaTokens.caption
                            )
                        }
                    }
                }
            }

            /* -------- What you can do -------- */
            item(key = "options") {
                GlassCard {
                    Text(
                        text = "What you can do",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceSm))
                    UnsupportedOptionRow(
                        icon = Icons.Default.Info,
                        title = "Inspect metadata",
                        subtitle = "Size, dates, hashes, permissions and the raw technical block",
                        onClick = { viewModel.navigateTo(Screen.FileMetadata) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.Default.Code,
                        title = "Open hex viewer",
                        subtitle = "Page through raw bytes with offsets and ASCII",
                        onClick = { viewModel.navigateTo(Screen.UnknownFile) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.Default.Description,
                        title = "View as text",
                        subtitle = "Read whatever printable text the file contains",
                        onClick = { viewModel.navigateTo(Screen.CodeViewer) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.Default.Translate,
                        title = "Change text encoding",
                        subtitle = "Re-decode bytes as UTF-16, Latin-1, Shift-JIS and more",
                        onClick = { viewModel.navigateTo(Screen.EncodingSelection) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.Default.Extension,
                        title = "Check optional format modules",
                        subtitle = "See whether a verified viewer could be added",
                        onClick = { viewModel.navigateTo(Screen.Settings) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = "Open with another app",
                        subtitle = "Send the untouched file to an installed viewer",
                        onClick = { openWithAnotherApp(context, file) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.Default.Share,
                        title = "Share or save a copy",
                        subtitle = "Share sheet, copy path or export a duplicate",
                        onClick = { viewModel.navigateTo(Screen.ShareScreen) }
                    )
                    UnsupportedOptionRow(
                        icon = Icons.Default.Search,
                        title = "Find another file",
                        subtitle = "Search ${info.category.displayName.lowercase()} and other formats",
                        onClick = { viewModel.navigateTo(Screen.UniversalSearch) }
                    )
                }
            }

            item(key = "safety") {
                SafetyNote("Your files stay on your device. Vennela 365 does not modify, edit or upload any file.")
            }

            item(key = "footer") { Spacer(Modifier.height(VennelaTokens.spaceSm)) }
        }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Reusable pieces                                                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun RoundBarAction(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(0.9f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = palette.textPrimary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
    }
}

@Composable
private fun ConfidenceBar(score: Float, label: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = accent.secondary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(score.coerceIn(0f, 1f) * 100).toInt()}%",
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption
            )
        }
        Spacer(Modifier.height(VennelaTokens.spaceXs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(palette.cardSurfaceStrong)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
            )
        }
    }
}

/** Two-line monospace byte row: offset + ASCII on top, hex below. Fits phones. */
@Composable
private fun HexLine(
    row: HexRow,
    rawOnly: Boolean,
    index: Int,
    onCopy: (() -> Unit)?
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val isError = row.offset == "ERROR"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusChip))
            .then(if (onCopy != null) Modifier.pressScale(0.99f, onCopy) else Modifier)
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.offset,
                color = if (isError) accent.secondary else palette.textTertiary,
                fontSize = VennelaTokens.nano,
                fontFamily = VennelaFonts.Mono
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = row.ascii,
                color = accent.secondary,
                fontSize = VennelaTokens.nano,
                fontFamily = VennelaFonts.Mono,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
        }
        if (!rawOnly && !isError) {
            Text(
                text = row.hexBytes,
                color = palette.textSecondary,
                fontSize = VennelaTokens.nano,
                fontFamily = VennelaFonts.Mono,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun InlineOffsetField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .height(VennelaTokens.touchTargetMin)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(palette.cardSurface.copy(alpha = 0.55f))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusControl))
            .padding(horizontal = VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
        Spacer(Modifier.width(VennelaTokens.spaceSm))
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { ch -> ch.isLetterOrDigit() }.take(8)) },
            singleLine = true,
            textStyle = TextStyle(
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontFamily = VennelaFonts.Mono
            ),
            cursorBrush = SolidColor(accent.secondary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Hex offset, e.g. 00000100",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.caption,
                        fontFamily = VennelaFonts.Mono
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun SmallAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .height(VennelaTokens.touchTargetMin)
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(accent.primary.copy(alpha = 0.22f))
            .border(1.dp, accent.secondary.copy(alpha = 0.4f), RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(0.95f, onClick)
            .padding(horizontal = VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm))
        Spacer(Modifier.width(VennelaTokens.spaceXs))
        Text(
            text = label,
            color = palette.textPrimary,
            fontFamily = VennelaFonts.Sans,
            fontSize = VennelaTokens.caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun BadgeFact(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = VennelaTokens.nano
        )
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ReasonLine(
    label: String,
    value: String,
    palette: com.vennela365.viewer.theme.VennelaPalette
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = VennelaTokens.spaceXs)) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = VennelaTokens.caption
        )
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FallbackActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VennelaTokens.touchTargetMin)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .pressScale(0.98f, onClick)
            .padding(vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(VennelaTokens.badgeSm)
                .clip(CircleShape)
                .background(accent.primary.copy(alpha = 0.2f))
                .border(1.dp, accent.secondary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
        }
        Spacer(Modifier.width(VennelaTokens.spaceMd))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption,
                lineHeight = 16.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
    }
}

@Composable
private fun OutlinedGlassAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = modifier
            .height(VennelaTokens.touchTargetMin)
            .clip(RoundedCornerShape(VennelaTokens.radiusPill))
            .background(palette.cardSurface.copy(alpha = 0.5f))
            .border(1.dp, accent.secondary.copy(alpha = 0.5f), RoundedCornerShape(VennelaTokens.radiusPill))
            .pressScale(0.96f, onClick)
            .padding(horizontal = VennelaTokens.spaceMd),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm))
        Spacer(Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = VennelaTokens.caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SafetyNote(text: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusTile))
            .background(accent.primary.copy(alpha = 0.14f))
            .border(1.dp, accent.secondary.copy(alpha = 0.32f), RoundedCornerShape(VennelaTokens.radiusTile))
            .padding(VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = accent.secondary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
        Spacer(Modifier.width(VennelaTokens.spaceSm))
        Text(
            text = text,
            color = palette.textPrimary,
            fontSize = VennelaTokens.caption,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Regex / plain-text finder over the decoded preview. */
@Composable
private fun RegexMatches(
    text: String,
    pattern: String,
    accent: Color,
    palette: com.vennela365.viewer.theme.VennelaPalette
) {
    val lines = remember(text) { text.lineSequence().toList() }
    val outcome = remember(pattern, lines) {
        if (pattern.isBlank()) {
            MatchOutcome.NoQuery(lines.size)
        } else {
            runCatching {
                val regex = Regex(pattern)
                val hits = lines.mapIndexedNotNull { index, line ->
                    if (regex.containsMatchIn(line)) index to line else null
                }
                MatchOutcome.Hits(regex.findAll(text).count(), hits)
            }.getOrElse { error ->
                MatchOutcome.Invalid(error.message ?: "Invalid regular expression")
            }
        }
    }

    when (outcome) {
        is MatchOutcome.NoQuery -> {
            Text(
                text = "${outcome.lineCount} decoded lines shown below. " +
                    "Type a pattern above to locate matches.",
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption
            )
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            lines.take(40).forEachIndexed { index, line ->
                Text(
                    text = "${index + 1}  $line",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.nano,
                    fontFamily = VennelaFonts.Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (lines.size > 40) {
                Spacer(Modifier.height(VennelaTokens.spaceXs))
                Text(
                    text = "${lines.size - 40} further lines — open the code viewer for the full text.",
                    color = palette.textTertiary,
                    fontSize = VennelaTokens.caption
                )
            }
        }
        is MatchOutcome.Invalid -> {
            Text(
                text = "That pattern is not valid: ${outcome.message}",
                color = palette.textSecondary,
                fontSize = VennelaTokens.bodySmall
            )
        }
        is MatchOutcome.Hits -> {
            Text(
                text = "${outcome.total} match${if (outcome.total == 1) "" else "es"} on ${outcome.lines.size} line(s)",
                color = accent,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            if (outcome.lines.isEmpty()) {
                Text(
                    text = "No line in the decoded preview matches this pattern.",
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.bodySmall
                )
            } else {
                outcome.lines.take(40).forEach { (index, line) ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "${index + 1}",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.nano,
                            fontFamily = VennelaFonts.Mono,
                            modifier = Modifier.width(34.dp)
                        )
                        Text(
                            text = line.trim().take(200),
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.nano,
                            fontFamily = VennelaFonts.Mono,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (outcome.lines.size > 40) {
                    Spacer(Modifier.height(VennelaTokens.spaceXs))
                    Text(
                        text = "First 40 matching lines shown.",
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.caption
                    )
                }
            }
        }
    }
}

private sealed class MatchOutcome {
    data class NoQuery(val lineCount: Int) : MatchOutcome()
    data class Hits(val total: Int, val lines: List<Pair<Int, String>>) : MatchOutcome()
    data class Invalid(val message: String) : MatchOutcome()
}

@Composable
private fun UnknownToolsDock(
    onSearch: () -> Unit,
    onEncoding: () -> Unit,
    onGoTo: () -> Unit,
    onMore: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.4f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationDock,
        sheen = 0.16f
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceSm)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusDock))
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            Triple("Search", Icons.Default.Search, onSearch),
            Triple("Encoding", Icons.Default.Translate, onEncoding),
            Triple("Go to", Icons.Default.MyLocation, onGoTo),
            Triple("More", Icons.Default.Tune, onMore)
        )
        val tags = listOf("unknown_dock_search", "unknown_dock_encoding", "unknown_dock_goto", "unknown_dock_more")
        items.forEachIndexed { index, (label, icon, action) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(VennelaTokens.touchTargetMin + 8.dp)
                    .testTag(tags[index])
                    .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                    .pressScale(0.94f, action),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = palette.dockTextMuted,
                    modifier = Modifier.size(VennelaTokens.iconMd)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    color = palette.dockTextMuted,
                    fontSize = VennelaTokens.nano,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Public row used by the unsupported screen                                  */
/* ------------------------------------------------------------------------- */

@Composable
fun UnsupportedOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    FallbackActionRow(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
}
