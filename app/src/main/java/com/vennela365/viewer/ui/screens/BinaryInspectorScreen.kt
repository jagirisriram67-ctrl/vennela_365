package com.vennela365.viewer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.model.HexRow
import com.vennela365.viewer.parser.FileParserManager
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BinaryInspection(
    val detected: String = "Unknown",
    val checksum: String = "—",
    val hex: List<HexRow> = emptyList()
)

/**
 * A real inspector for formats no layout renderer can honestly decompose:
 * executables, model checkpoints, CAD/3D, GIS and scientific containers.
 *
 * It shows what can be proven from the file itself — the magic-byte container
 * type, a SHA-256 of the bytes and the first pages of a hex dump — instead of a
 * paragraph promising features that do not exist. Nothing is executed, parsed
 * into an object graph or written back.
 */
@Composable
fun BinaryInspectorScreen(
    viewModel: MainViewModel,
    family: String,
    subtitle: String
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val file by viewModel.selectedFile.collectAsState()

    val inspection by produceState<BinaryInspection?>(initialValue = null, file?.uri) {
        value = withContext(Dispatchers.IO) {
            val uri = file?.uri
            val magic = runCatching {
                context.contentResolver.openInputStream(uri!!)?.use { stream ->
                    val head = ByteArray(16)
                    val read = stream.read(head)
                    if (read > 0) FileParserManager.detectMagicFormat(head.copyOf(read)) else "Unknown"
                } ?: "Unknown"
            }.getOrDefault("Unknown")

            val checksum = FileParserManager.computeChecksum(context, uri, "SHA-256")
            val rows = FileParserManager.readHexDump(context, uri, 4096).take(48)

            BinaryInspection(detected = magic, checksum = checksum, hex = rows)
        }
    }

    LiquidPage(
        modifier = Modifier.fillMaxSize(),
        title = family,
        subtitle = subtitle,
        onBack = { viewModel.navigateBack() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = padding.calculateTopPadding() + VennelaTokens.spaceSm,
                bottom = padding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm)
        ) {
            item(key = "file") {
                Column(
                    Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VennelaTokens.radiusCard))
                        .background(palette.cardSurfaceStrong)
                        .border(
                            1.dp,
                            palette.textPrimary.copy(alpha = 0.10f),
                            RoundedCornerShape(VennelaTokens.radiusCard)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = file?.name ?: "No file open",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceXs))
                    InspectRow("Container", inspection?.detected ?: "Reading headers…")
                    InspectRow("Extension", (file?.extension?.uppercase() ?: "—"))
                    InspectRow("Size", file?.formattedSize ?: "—")
                    InspectRow("Modified", file?.formattedDate ?: "—")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            InspectRow("SHA-256", inspection?.checksum ?: "Computing…")
                        }
                        val readyHash = inspection?.checksum?.takeIf { it.isNotBlank() && it != "—" && it != "N/A" }
                        if (readyHash != null) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(readyHash))
                                    Toast.makeText(context, "SHA-256 copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(VennelaTokens.touchTargetMin)
                                    .testTag("binary_copy_hash_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy SHA-256 checksum",
                                    tint = accent.secondary,
                                    modifier = Modifier.size(VennelaTokens.iconSm)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "safety") {
                Row(
                    Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VennelaTokens.radiusCard))
                        .background(accent.primary.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            accent.secondary.copy(alpha = 0.22f),
                            RoundedCornerShape(VennelaTokens.radiusCard)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = accent.secondary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                    Spacer(Modifier.width(VennelaTokens.spaceSm))
                    Text(
                        text = "Opened as bytes only. Nothing here can run, install or modify the file.",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.micro,
                        lineHeight = 16.sp
                    )
                }
            }

            item(key = "hex") {
                Column(
                    Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VennelaTokens.radiusCard))
                        .background(palette.cardSurface.copy(alpha = 0.55f))
                        .border(
                            1.dp,
                            palette.cardBorder.copy(alpha = 0.35f),
                            RoundedCornerShape(VennelaTokens.radiusCard)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Hex preview • first 4 KB",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.nano,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = VennelaFonts.Mono
                    )
                    Spacer(Modifier.height(VennelaTokens.spaceXs))
                    val rows = inspection?.hex.orEmpty()
                    if (rows.isEmpty()) {
                        Text(
                            text = "No bytes could be read from this location.",
                            color = palette.textTertiary,
                            fontSize = VennelaTokens.micro
                        )
                    } else {
                        rows.forEach { row ->
                            Text(
                                text = row.offset + "  " + row.hexBytes,
                                color = palette.textPrimary,
                                fontSize = 9.5.sp,
                                lineHeight = 14.sp,
                                fontFamily = VennelaFonts.Mono,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectRow(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            modifier = Modifier.width(70.dp),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = palette.textSecondary,
            fontSize = VennelaTokens.micro,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = if (label == "SHA-256") VennelaFonts.Mono else VennelaFonts.Sans
        )
    }
}
