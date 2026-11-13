package com.vennela365.viewer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.model.HexRow
import com.vennela365.viewer.parser.ExifData
import com.vennela365.viewer.parser.ExifParser
import com.vennela365.viewer.parser.FileParserManager
import com.vennela365.viewer.registry.FormatCategory
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.ui.components.AnimatedDisclosure
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.components.GlassDivider
import com.vennela365.viewer.ui.components.IconBadge
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/* ------------------------------------------------------------------------- */
/* Real-data helpers                                                          */
/* ------------------------------------------------------------------------- */

private val exifCapableExtensions = setOf(
    "jpg", "jpeg", "jpe", "jfif", "png", "webp", "heic", "heif", "tif", "tiff",
    "dng", "cr2", "cr3", "nef", "arw", "raf", "orf", "rw2", "raw", "mp4", "mov", "m4v", "3gp"
)

private fun FileItem.canCarryExif(): Boolean =
    formatInfo.category == FormatCategory.IMAGES || extension.lowercase() in exifCapableExtensions

/** Provider-reported facts (SAF / content providers), never invented. */
private data class ProviderMeta(
    val displayName: String? = null,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null,
    val readable: Boolean = false
)

private sealed class HashState {
    object Computing : HashState()
    object Unavailable : HashState()
    data class Ready(val sha256: String, val md5: String) : HashState()
}

/**
 * A content-based identification derived from the file's own leading bytes.
 * `confidence` is a real 0..1 score, not a decoration: it drops when the bytes
 * disagree with what the extension claims.
 */
private data class ContentDetection(
    val container: String,
    val confidence: Float,
    val reason: String
) {
    val label: String
        get() = when {
            confidence >= 0.85f -> "High confidence"
            confidence >= 0.5f -> "Medium confidence"
            else -> "Low confidence"
        }
}

private fun hexRowsToBytes(rows: List<HexRow>, maxBytes: Int = 32): ByteArray {
    val row = rows.firstOrNull { it.offset != "ERROR" } ?: return ByteArray(0)
    val tokens = row.hexBytes.trim().split(" ").filter { it.length == 2 }
    val size = minOf(tokens.size, maxBytes)
    val out = ByteArray(size)
    for (i in 0 until size) {
        out[i] = (tokens[i].toIntOrNull(16) ?: 0).toByte()
    }
    return out
}

/** Accepts both hex signatures ("25 50 44 46") and ASCII ones ("PK"). */
private fun matchesSignature(bytes: ByteArray, signature: String): Boolean {
    val tokens = signature.trim().split(" ").filter { it.isNotBlank() }
    if (tokens.isEmpty()) return false
    val expected = ArrayList<Byte>()
    for (token in tokens) {
        val isHex = token.length == 2 && token.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        if (isHex) {
            expected.add((token.toIntOrNull(16) ?: 0).toByte())
        } else {
            token.forEach { expected.add(it.code.toByte()) }
        }
    }
    if (expected.isEmpty() || bytes.size < expected.size) return false
    return expected.indices.all { bytes[it] == expected[it] }
}

private fun detectContent(file: FileItem, bytes: ByteArray): ContentDetection {
    if (bytes.isEmpty()) {
        return ContentDetection(
            container = "Not enough bytes read",
            confidence = 0f,
            reason = "The provider returned no readable bytes, so no signature could be tested."
        )
    }
    val container = FileParserManager.detectMagicFormat(bytes)
    val declared = file.formatInfo.signatures
    val declaredMatch = declared.any { matchesSignature(bytes, it) }
    return when {
        declaredMatch -> ContentDetection(
            container = container,
            confidence = 0.95f,
            reason = "Leading bytes match the signature registered for .${file.extension}."
        )
        declared.isNotEmpty() -> ContentDetection(
            container = container,
            confidence = 0.25f,
            reason = "Extension .${file.extension} declares a different signature than the bytes carry — " +
                "treat the extension as unreliable."
        )
        container == "Binary Data Stream" -> ContentDetection(
            container = container,
            confidence = 0.3f,
            reason = "No known magic bytes and no registered signature for .${file.extension}."
        )
        else -> ContentDetection(
            container = container,
            confidence = 0.7f,
            reason = "Content signature recognized; .${file.extension} registers no signature to compare against."
        )
    }
}

private fun formatExactDate(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(epochMs))

private suspend fun readProviderMeta(context: Context, uri: Uri?): ProviderMeta = withContext(Dispatchers.IO) {
    if (uri == null) return@withContext ProviderMeta()
    var name: String? = null
    var size: Long? = null
    val readable = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        true
    }.getOrDefault(false)

    val document = runCatching { DocumentFile.fromSingleUri(context, uri) }.getOrNull()
    val documentSize = runCatching { document?.length() }.getOrNull()?.takeIf { it > 0L }
    val modified = runCatching { document?.lastModified() }.getOrNull()?.takeIf { it > 0L }

    ProviderMeta(
        displayName = name ?: runCatching { document?.name }.getOrNull(),
        sizeBytes = size ?: documentSize,
        lastModified = modified,
        readable = readable
    )
}

private fun readOnlyGrantHeld(context: Context, uri: Uri?): Boolean {
    if (uri == null) return false
    return runCatching {
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }.getOrDefault(false)
}

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
fun FileMetadataScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val hexRows by viewModel.hexRows.collectAsState()
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    var rawExpanded by remember { mutableStateOf(false) }
    val file = selectedFile

    LiquidPage(
        title = file?.name ?: "File metadata",
        subtitle = "File Metadata",
        onBack = { viewModel.navigateBack() },
        dock = {
            MetadataActionDock(
                onInfo = { viewModel.navigateBack() },
                onShare = { viewModel.navigateTo(Screen.ShareScreen) },
                onCopyPath = {
                    copyToClipboard(context, file?.path?.ifBlank { file.name } ?: "", "Path copied")
                },
                onMore = { rawExpanded = !rawExpanded }
            )
        },
        topBarActions = {
            TopBarAction(Icons.Default.Search, "Search files") { viewModel.navigateTo(Screen.UniversalSearch) }
            TopBarAction(Icons.Default.Code, "Raw technical data") { rawExpanded = !rawExpanded }
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
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
            ) {
                item {
                    GlassCard {
                        VennelaEmptyState(
                            title = "No file selected",
                            hint = "Open a file from the explorer, search or recents to inspect its metadata.",
                            icon = Icons.Default.Description,
                            actionLabel = "Browse files",
                            onAction = { viewModel.navigateTo(Screen.FileExplorer) }
                        )
                    }
                }
            }
            return@LiquidPage
        }

        val providerMeta by produceState(ProviderMeta(), file.uri) {
            value = readProviderMeta(context, file.uri)
        }
        val exif by produceState(ExifData(), file.uri, file.extension) {
            value = if (file.canCarryExif()) ExifParser.extractExif(context, file.uri) else ExifData()
        }
        val hashes by produceState<HashState>(HashState.Computing, file.uri) {
            val uri = file.uri
            value = if (uri == null) {
                HashState.Unavailable
            } else {
                val sha = FileParserManager.computeChecksum(context, uri, "SHA-256")
                val md5 = FileParserManager.computeChecksum(context, uri, "MD5")
                if (sha == "N/A" && md5 == "N/A") HashState.Unavailable else HashState.Ready(sha, md5)
            }
        }

        val bytes = remember(hexRows) { hexRowsToBytes(hexRows) }
        val detection = remember(bytes, file.id) { detectContent(file, bytes) }
        val grantHeld = remember(file.uri) { readOnlyGrantHeld(context, file.uri) }
        val info = file.formatInfo

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = pagePadding.calculateTopPadding() + VennelaTokens.spaceMd,
                bottom = pagePadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceMd)
        ) {
            /* ---------------- Hero preview ---------------- */
            item(key = "hero") {
                HeroPreviewCard(
                    file = file,
                    palette = palette,
                    accent = accent,
                    onOpenViewer = {
                        when (info.category) {
                            FormatCategory.IMAGES -> viewModel.navigateTo(Screen.ImageViewer)
                            FormatCategory.AUDIO -> viewModel.navigateTo(Screen.AudioPlayer)
                            FormatCategory.VIDEO -> viewModel.navigateTo(Screen.VideoPlayer)
                            FormatCategory.DOCUMENTS, FormatCategory.EBOOKS -> viewModel.navigateTo(Screen.DocumentViewer)
                            FormatCategory.SPREADSHEETS -> viewModel.navigateTo(Screen.SpreadsheetViewer)
                            FormatCategory.ARCHIVES -> viewModel.navigateTo(Screen.ArchiveBrowser)
                            FormatCategory.CODE, FormatCategory.CONFIG -> viewModel.navigateTo(Screen.CodeViewer)
                            FormatCategory.MARKDOWN -> viewModel.navigateTo(Screen.MarkdownViewer)
                            else -> viewModel.openFile(file)
                        }
                    }
                )
            }

            /* ---------------- Identity ---------------- */
            item(key = "identity") {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FileBrandBadge(extension = file.extension, size = VennelaTokens.badgeLg)
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${info.name} • ${file.formattedSize}",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(VennelaTokens.touchTargetMin)
                                .clip(CircleShape)
                                .pressScale(0.9f) { viewModel.toggleFavorite(file) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (file.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (file.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (file.isFavorite) accent.secondary else palette.textSecondary,
                                modifier = Modifier.size(VennelaTokens.iconLg)
                            )
                        }
                    }
                }
            }

            /* ---------------- Detection ---------------- */
            item(key = "detection") {
                MetaSection(title = "Content detection", icon = Icons.Default.Fingerprint) {
                    MetaRow(
                        label = "Detected container",
                        value = detection.container,
                        icon = Icons.Default.DataObject,
                        copyable = detection.confidence > 0f
                    )
                    ConfidenceMeter(
                        confidence = detection.confidence,
                        label = detection.label,
                        reason = detection.reason
                    )
                    if (bytes.isNotEmpty()) {
                        MetaRow(
                            label = "Leading bytes",
                            value = bytes.joinToString(" ") { String.format(Locale.US, "%02X", it) },
                            icon = Icons.Default.Code
                        )
                    }
                }
            }

            /* ---------------- File ---------------- */
            item(key = "file") {
                MetaSection(title = "File", icon = Icons.Default.Description) {
                    MetaRow("File name", file.name, Icons.Default.Description)
                    MetaRow("File type", "${info.name} (L${info.levelNumber} • ${info.supportLevel.label})", Icons.Default.Category)
                    MetaRow("MIME type", info.mimeType, Icons.Default.Tag)
                    MetaRow(
                        label = "File size",
                        value = "${file.formattedSize} (${String.format(Locale.US, "%,d", file.sizeBytes)} bytes)",
                        icon = Icons.Default.Storage
                    )
                    val location = providerMeta.displayName?.let { file.path.ifBlank { it } } ?: file.path
                    MetaRow("Location", location.ifBlank { "Not exposed by this file source" }, Icons.Default.Folder)
                    MetaRow("Category", info.category.displayName, Icons.Default.Category)
                }
            }

            /* ---------------- Dates ---------------- */
            item(key = "dates") {
                MetaSection(title = "Dates", icon = Icons.Default.DateRange) {
                    val modified = providerMeta.lastModified ?: file.lastModified
                    MetaRow("Modified", formatExactDate(modified), Icons.Default.AccessTime)
                    MetaRow("Modified (epoch ms)", modified.toString(), Icons.Default.AccessTime)
                    if (exif.dateTime != null) {
                        MetaRow("Captured (EXIF)", exif.dateTime!!, Icons.Default.DateRange)
                    } else {
                        MetaRow(
                            label = "Captured (EXIF)",
                            value = "No original date tag in this file",
                            icon = Icons.Default.DateRange,
                            copyable = false
                        )
                    }
                    MetaRow(
                        label = "Created",
                        value = "Android providers expose modification time only",
                        icon = Icons.Default.Info,
                        copyable = false
                    )
                }
            }

            /* ---------------- Media / EXIF ---------------- */
            item(key = "media") {
                MetaSection(title = "Media & EXIF", icon = Icons.Default.CameraAlt) {
                    if (exif.hasExif) {
                        val camera = listOfNotNull(exif.cameraMake, exif.cameraModel)
                            .joinToString(" ").ifBlank { null }
                        camera?.let { MetaRow("Camera", it, Icons.Default.CameraAlt) }
                        exif.lensModel?.let { MetaRow("Lens", it, Icons.Default.CameraAlt) }
                        exif.dimensions?.let { MetaRow("Dimensions", it, Icons.Default.GridOn) }
                        exif.exposureTime?.let { MetaRow("Exposure", it, Icons.Default.Exposure) }
                        exif.fNumber?.let { MetaRow("Aperture", it, Icons.Default.Exposure) }
                        exif.iso?.let { MetaRow("ISO", it, Icons.Default.Exposure) }
                        exif.focalLength?.let { MetaRow("Focal length", it, Icons.Default.Exposure) }
                        exif.flash?.let { MetaRow("Flash", it, Icons.Default.WbSunny) }
                        exif.whiteBalance?.let { MetaRow("White balance", it, Icons.Default.WbSunny) }
                        exif.software?.let { MetaRow("Software", it, Icons.Default.Palette) }
                    } else {
                        Text(
                            text = if (file.canCarryExif()) {
                                exif.error
                                    ?: "No EXIF block found in this file's header — nothing is shown rather than guessed."
                            } else {
                                "${info.category.displayName} files carry no EXIF block."
                            },
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.bodySmall,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            /* ---------------- Location ---------------- */
            if (exif.latitude != null && exif.longitude != null) {
                item(key = "location") {
                    val lat = exif.latitude!!
                    val lon = exif.longitude!!
                    var revealed by remember { mutableStateOf(false) }
                    MetaSection(title = "Location tag", icon = Icons.Default.LocationOn) {
                        if (revealed) {
                            CoordinatePlot(latitude = lat, longitude = lon)
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            MetaRow(
                                label = "GPS",
                                value = String.format(Locale.US, "%.6f, %.6f", lat, lon),
                                icon = Icons.Default.LocationOn
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = VennelaTokens.spaceSm),
                                horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
                            ) {
                                DockTextAction(
                                    label = "Open in Maps",
                                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))
                                        )
                                    }
                                }
                                DockTextAction(
                                    label = "Hide",
                                    icon = Icons.Default.Lock,
                                    modifier = Modifier.weight(1f)
                                ) { revealed = false }
                            }
                        } else {
                            Text(
                                text = "This file embeds GPS coordinates. They stay on-device until you reveal them.",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            DockTextAction(label = "Reveal coordinates", icon = Icons.Default.LocationOn) {
                                revealed = true
                            }
                        }
                    }
                }
            }

            /* ---------------- Permissions ---------------- */
            item(key = "permissions") {
                MetaSection(title = "Permissions & safety", icon = Icons.Default.Security) {
                    MetaRow(
                        label = "Access mode",
                        value = "Read-only — Vennela never edits, renames or uploads a file",
                        icon = Icons.Default.Lock,
                        copyable = false
                    )
                    MetaRow(
                        label = "Persisted read grant",
                        value = if (grantHeld) "Held for this URI" else "Session grant only (not persisted)",
                        icon = Icons.Default.Security,
                        copyable = false
                    )
                    MetaRow("Renderer", info.defaultRenderer, Icons.Default.Speed)
                    MetaRow("Fallback handler", info.fallbackHandler, Icons.Default.Speed)
                    info.maxSafeBytes?.let {
                        MetaRow(
                            label = "Safe inspection cap",
                            value = "${String.format(Locale.US, "%,d", it)} bytes",
                            icon = Icons.Default.Storage
                        )
                    }
                    if (info.securityNote.isNotBlank()) {
                        MetaRow("Security note", info.securityNote, Icons.Default.Security, copyable = false)
                    }
                    if (info.limitations.isNotBlank()) {
                        MetaRow("Known limits", info.limitations, Icons.Default.Info, copyable = false)
                    }
                    if (info.description.isNotBlank()) {
                        MetaRow("Format notes", info.description, Icons.Default.Info, copyable = false)
                    }
                }
            }

            /* ---------------- Hashes ---------------- */
            item(key = "hashes") {
                MetaSection(title = "Integrity", icon = Icons.Default.Fingerprint) {
                    when (val state = hashes) {
                        is HashState.Computing -> {
                            Text(
                                text = "Computing checksums from the file stream…",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall
                            )
                        }
                        is HashState.Unavailable -> {
                            Text(
                                text = "This file source does not expose a readable stream, so no checksum can be computed.",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.bodySmall,
                                lineHeight = 19.sp
                            )
                        }
                        is HashState.Ready -> {
                            MetaRow("SHA-256", state.sha256, Icons.Default.Fingerprint)
                            MetaRow("MD5", state.md5, Icons.Default.Fingerprint)
                        }
                    }
                }
            }

            /* ---------------- Raw technical block ---------------- */
            item(key = "raw") {
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(0.99f) { rawExpanded = !rawExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(
                            icon = Icons.Default.Code,
                            colors = listOf(accent.primary, accent.secondary),
                            size = VennelaTokens.badgeSm,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(VennelaTokens.spaceMd))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Raw technical block",
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${hexRows.size} hex rows • signatures • handler chain",
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.caption
                            )
                        }
                        AnimatedDisclosure(expanded = rawExpanded)
                    }

                    AnimatedVisibility(visible = rawExpanded) {
                        Column {
                            Spacer(Modifier.height(VennelaTokens.spaceMd))
                            GlassDivider()
                            Spacer(Modifier.height(VennelaTokens.spaceMd))
                            TechnicalLine("URI", file.uri?.toString() ?: "no content URI")
                            TechnicalLine("Extension", ".${file.extension}")
                            TechnicalLine("Canonical id", info.canonicalId)
                            TechnicalLine("Registry signatures", info.signatures.ifEmpty { listOf("none declared") }.joinToString(", "))
                            TechnicalLine("Detected container", detection.container)
                            TechnicalLine("Detection score", String.format(Locale.US, "%.2f", detection.confidence))
                            TechnicalLine("Compound suffix", if (info.isCompoundSuffix) "yes" else "no")
                            TechnicalLine("Password required", if (info.passwordRequired) "yes" else "no")
                            TechnicalLine("Aliases", info.aliases.ifEmpty { listOf("none") }.joinToString(", "))
                            TechnicalLine("Sample fixture", info.testFixture)
                            TechnicalLine("Provider readable", if (providerMeta.readable) "yes" else "no")
                            TechnicalLine("Byte length (provider)", providerMeta.sizeBytes?.toString() ?: "unreported")
                            Spacer(Modifier.height(VennelaTokens.spaceSm))
                            HexPreview(rows = hexRows)
                        }
                    }
                }
            }

            item(key = "footer") { Spacer(Modifier.height(VennelaTokens.spaceSm)) }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Building blocks                                                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun TopBarAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(0.9f, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = palette.textPrimary, modifier = Modifier.size(VennelaTokens.iconMd))
    }
}

@Composable
private fun HeroPreviewCard(
    file: FileItem,
    palette: com.vennela365.viewer.theme.VennelaPalette,
    accent: com.vennela365.viewer.theme.VennelaAccent,
    onOpenViewer: () -> Unit
) {
    val isImage = file.formatInfo.category == FormatCategory.IMAGES && file.uri != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.cardSurfaceStrong,
                        palette.cardSurface
                    )
                )
            )
            .border(1.dp, accent.secondary.copy(alpha = 0.35f), RoundedCornerShape(VennelaTokens.radiusCardLarge))
    ) {
        if (isImage) {
            AsyncImage(
                model = file.uri,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                palette.backgroundMid,
                                palette.backgroundBottom
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FileBrandBadge(extension = file.extension, size = 76.dp)
                    Spacer(Modifier.height(VennelaTokens.spaceMd))
                    Text(
                        text = file.formatInfo.name,
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.bodySmall
                    )
                    Text(
                        text = file.formattedSize,
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.caption
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(VennelaTokens.spaceMd)
                .size(VennelaTokens.touchTargetMin)
                .testTag("metadata_open_viewer_button")
                .clip(CircleShape)
                .background(palette.cardSurfaceStrong.copy(alpha = 0.82f))
                .border(1.dp, accent.secondary.copy(alpha = 0.4f), CircleShape)
                .pressScale(0.9f, onOpenViewer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Open in viewer",
                tint = palette.textPrimary,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
        }
    }
}

@Composable
private fun MetaSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    GlassCard(modifier = Modifier.widthIn(max = 840.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = icon,
                colors = listOf(accent.primary, accent.secondary),
                size = VennelaTokens.badgeSm,
                contentDescription = null
            )
            Spacer(Modifier.width(VennelaTokens.spaceMd))
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(VennelaTokens.spaceSm))
        GlassDivider()
        Spacer(Modifier.height(VennelaTokens.spaceSm))
        content()
    }
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    copyable: Boolean = true
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VennelaTokens.spaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(Modifier.width(VennelaTokens.spaceSm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        if (copyable && value.isNotBlank()) {
            CopyButton(value = value, label = label)
        }
    }
}

@Composable
private fun CopyButton(value: String, label: String) {
    val palette = LocalVennelaPalette.current
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .pressScale(0.88f) { copyToClipboard(context, value, "$label copied") },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy $label",
            tint = palette.textTertiary,
            modifier = Modifier.size(VennelaTokens.iconSm)
        )
    }
}

private fun copyToClipboard(context: Context, value: String, message: String) {
    if (value.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Vennela 365", value))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
private fun ConfidenceMeter(confidence: Float, label: String, reason: String) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(top = VennelaTokens.spaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = accent.secondary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(confidence * 100).toInt()}%",
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
                    .fillMaxWidth(confidence.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
            )
        }
        Spacer(Modifier.height(VennelaTokens.spaceXs))
        Text(
            text = reason,
            color = palette.textSecondary,
            fontSize = VennelaTokens.caption,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun CoordinatePlot(latitude: Double, longitude: Double) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(VennelaTokens.radiusTile))
            .background(Brush.verticalGradient(listOf(palette.backgroundMid, palette.backgroundBottom)))
            .border(1.dp, palette.cardBorder.copy(alpha = 0.85f), RoundedCornerShape(VennelaTokens.radiusTile))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = size.width / 6f
            var x = step
            while (x < size.width) {
                drawLine(
                    color = accent.secondary.copy(alpha = 0.16f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(
                    color = accent.secondary.copy(alpha = 0.16f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(VennelaTokens.spaceMd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent.secondary.copy(alpha = 0.25f))
                    .border(2.dp, accent.secondary, CircleShape)
            )
            Spacer(Modifier.height(VennelaTokens.spaceSm))
            Text(
                text = String.format(Locale.US, "%.4f°, %.4f°", latitude, longitude),
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Offline coordinate plot — open Maps for the real map",
                color = palette.textTertiary,
                fontSize = VennelaTokens.nano
            )
        }
    }
}

@Composable
private fun DockTextAction(
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
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(accent.primary.copy(alpha = 0.22f))
            .border(1.dp, accent.secondary.copy(alpha = 0.4f), RoundedCornerShape(VennelaTokens.radiusControl))
            .pressScale(0.96f, onClick)
            .padding(horizontal = VennelaTokens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
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
private fun TechnicalLine(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Text(
            text = label,
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            fontFamily = VennelaFonts.Mono,
            modifier = Modifier.width(128.dp)
        )
        Text(
            text = value,
            color = palette.textSecondary,
            fontSize = VennelaTokens.nano,
            fontFamily = VennelaFonts.Mono,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HexPreview(rows: List<HexRow>) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val scroll = rememberScrollState()
    val errorRow = rows.firstOrNull { it.offset == "ERROR" }
    when {
        errorRow != null -> {
            Text(
                text = "Byte stream unavailable: ${errorRow.ascii}",
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption
            )
        }
        rows.isEmpty() -> {
            Text(
                text = "No bytes were read for this file source yet.",
                color = palette.textSecondary,
                fontSize = VennelaTokens.caption
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                    .background(palette.backgroundBottom.copy(alpha = 0.6f))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(VennelaTokens.radiusChip))
                    .padding(VennelaTokens.spaceSm)
            ) {
                Column(modifier = Modifier.horizontalScroll(scroll)) {
                    rows.take(24).forEach { row ->
                        Row {
                            Text(
                                text = row.offset,
                                color = palette.textTertiary,
                                fontSize = VennelaTokens.nano,
                                fontFamily = VennelaFonts.Mono
                            )
                            Spacer(Modifier.width(VennelaTokens.spaceSm))
                            Text(
                                text = row.hexBytes,
                                color = palette.textSecondary,
                                fontSize = VennelaTokens.nano,
                                fontFamily = VennelaFonts.Mono
                            )
                            Spacer(Modifier.width(VennelaTokens.spaceSm))
                            Text(
                                text = row.ascii,
                                color = accent.secondary,
                                fontSize = VennelaTokens.nano,
                                fontFamily = VennelaFonts.Mono
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataActionDock(
    onInfo: () -> Unit,
    onShare: () -> Unit,
    onCopyPath: () -> Unit,
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
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceSm)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusDock))
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockAction("Info", Icons.Default.Info, active = true, onClick = onInfo, modifier = Modifier.weight(1f).testTag("meta_dock_info"), palette = palette, accent = accent)
        DockAction("Share", Icons.Default.Share, active = false, onClick = onShare, modifier = Modifier.weight(1f).testTag("meta_dock_share"), palette = palette, accent = accent)
        DockAction("Copy Path", Icons.Default.ContentCopy, active = false, onClick = onCopyPath, modifier = Modifier.weight(1f).testTag("meta_dock_copy"), palette = palette, accent = accent)
        DockAction("More", Icons.Default.MoreHoriz, active = false, onClick = onMore, modifier = Modifier.weight(1f).testTag("meta_dock_more"), palette = palette, accent = accent)
    }
}

@Composable
private fun DockAction(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: com.vennela365.viewer.theme.VennelaPalette,
    accent: com.vennela365.viewer.theme.VennelaAccent
) {
    Column(
        modifier = modifier
            .height(VennelaTokens.touchTargetMin + 8.dp)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .then(
                if (active) {
                    Modifier
                        .background(accent.primary.copy(alpha = 0.9f))
                        .border(1.dp, LocalVennelaPalette.current.textPrimary.copy(alpha = 0.22f), RoundedCornerShape(VennelaTokens.radiusControl))
                } else {
                    Modifier
                }
            )
            .pressScale(0.94f, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) LocalVennelaPalette.current.textPrimary else palette.dockTextMuted,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (active) LocalVennelaPalette.current.textPrimary else palette.dockTextMuted,
            fontSize = VennelaTokens.nano,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
