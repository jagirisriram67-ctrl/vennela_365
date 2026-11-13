package com.vennela365.viewer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vennela365.viewer.parser.DocumentTextExtractor
import com.vennela365.viewer.registry.FormatRegistry
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.DocumentMarkup
import com.vennela365.viewer.ui.components.FileBrandBadge
import com.vennela365.viewer.ui.components.ReaderBottomPill
import com.vennela365.viewer.ui.components.ReaderChromeVisibility
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderPaper
import com.vennela365.viewer.ui.components.ReaderProgressRule
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.VennelaBackground
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.VennelaErrorState
import com.vennela365.viewer.ui.components.VennelaLoadingState
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/* ------------------------------------------------------------------------- */
/* Model                                                                      */
/* ------------------------------------------------------------------------- */

/**
 * One attachment found inside the message file. [payload] is the encoded body
 * of its MIME part, kept so share / open-with can decode the real bytes.
 */
private data class MailAttachment(
    val name: String,
    val sizeBytes: Long?,
    val exactSize: Boolean,
    val mime: String?,
    val encoding: String,
    val payload: String
) {
    val extension: String get() = name.substringAfterLast('.', "")
}

private data class MailMessage(
    val subject: String? = null,
    val from: String? = null,
    val to: String? = null,
    val date: String? = null,
    val location: String? = null,
    val body: String = "",
    val attachments: List<MailAttachment> = emptyList()
)

private data class MailContent(
    val fileId: String,
    val raw: String,
    val message: MailMessage?,
    val error: String? = null,
    val bodyMarkup: AnnotatedString? = null,
    val wordCount: Int = 0
)

/* ------------------------------------------------------------------------- */
/* Tunables                                                                   */
/* ------------------------------------------------------------------------- */

private const val MaxBodyChars = 200_000
private const val MaxAttachmentParts = 24
private const val MaxMimeParts = 64
private const val MaxMimeDepth = 2
private const val MaxAttachmentPayloadChars = 400_000
private const val MaxStagedAttachmentBytes = 8 * 1024 * 1024
private const val StagedMailBudget = 8
private const val MailStagedDir = "mail_stage"

/** Matches the reader's own extraction window, used only for the truncation note. */
private const val MailExtractorCharCap = 400_000

/* ------------------------------------------------------------------------- */
/* Screen                                                                     */
/* ------------------------------------------------------------------------- */

/**
 * A real message reader for .eml, .msg, .mbox, .ics and .vcf files.
 *
 * It reads the file's own header block and MIME parts (never the network),
 * shows the subject, sender, recipients and date, lays the body out on the
 * document paper, lists the attachments with their real decoded sizes and can
 * hand an attachment or the whole message to another app through [ShareUri].
 */
@Composable
fun EmailReaderScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val file by viewModel.selectedFile.collectAsState()

    var retryToken by remember(file?.id) { mutableStateOf(0) }
    val content by produceState<MailContent?>(
        initialValue = null,
        file?.id,
        file?.uri,
        file?.sampleContent,
        palette,
        accent,
        retryToken
    ) {
        val current = file
        if (current == null) {
            value = null
            return@produceState
        }
        value = try {
            // A message opened from inside an archive has no URI of its own: the
            // ViewModel already extracted its bytes into sampleContent.
            val raw = if (current.sampleContent.isNotBlank()) {
                current.sampleContent
            } else {
                DocumentTextExtractor.readPlainText(context, current.uri)
            }
            // Header parsing and the paper-markup build both stay off the main
            // thread: a 200k-character body must never be laid out inline.
            withContext(Dispatchers.Default) {
                val parsed = parseMessage(raw, current.extension)
                MailContent(
                    fileId = current.id,
                    raw = raw,
                    message = parsed,
                    bodyMarkup = if (parsed.body.isBlank()) {
                        null
                    } else {
                        DocumentMarkup.build(
                            source = parsed.body,
                            typography = DocumentMarkup.Typography(
                                bodySize = 14.sp,
                                ink = palette.textPrimary,
                                muted = palette.textSecondary,
                                accent = accent.primary,
                                tableTint = accent.primary.copy(alpha = 0.10f),
                                isDarkPaper = palette.isDark
                            )
                        )
                    },
                    wordCount = countMailWords(parsed.body)
                )
            }
        } catch (error: Throwable) {
            MailContent(
                fileId = current.id,
                raw = "",
                message = null,
                error = error.message ?: "This message could not be read."
            )
        }
    }

    val scrollState = rememberScrollState()
    var chromeVisible by remember { mutableStateOf(true) }
    var isDetailsOpen by remember { mutableStateOf(false) }
    var stagingIndex by remember { mutableStateOf<Int?>(null) }

    val ready = content?.takeIf { it.fileId == file?.id }
    val message = ready?.message
    val error = ready?.error
    val raw = ready?.raw.orEmpty()
    val isLoading = ready == null && file != null

    val body = message?.body.orEmpty()
    val attachments = message?.attachments.orEmpty()
    val bodyMarkup = ready?.bodyMarkup
    val wordCount = ready?.wordCount ?: 0
    val truncated = raw.length >= MailExtractorCharCap
    val progress = if (scrollState.maxValue > 0) {
        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    } else {
        0f
    }

    /* --------------------- chrome hides while reading ------------------- */

    LaunchedEffect(scrollState) {
        snapshotFlow {
            Triple(
                scrollState.isScrollInProgress,
                scrollState.lastScrolledBackward,
                scrollState.value == 0
            )
        }.collect { (scrolling, backward, atTop) ->
            if (atTop) chromeVisible = true
            else if (scrolling) chromeVisible = backward
        }
    }

    /* ------------------------------ actions ---------------------------- */

    fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun mailMime(mime: String?, extension: String): String {
        if (!mime.isNullOrBlank()) return mime
        val resolved = FormatRegistry.lookup(extension.lowercase(Locale.US)).mimeType
        return resolved.ifBlank { "application/octet-stream" }
    }

    fun shareMessage() {
        val current = file ?: return toast("There is no message to share")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mailMime(current.formatInfo.mimeType, current.extension)
            putExtra(Intent.EXTRA_SUBJECT, message?.subject ?: current.name)
        }
        if (!ShareUri.attachStream(context, intent, current.uri)) {
            return toast("This message's location cannot be handed to another app")
        }
        launchMailChooser(context, intent, "Share ${current.name}", "No app accepted this message")
    }

    fun openWithMessage() {
        val current = file ?: return toast("There is no message to open")
        val intent = ShareUri.viewIntent(
            context,
            current.uri,
            mailMime(current.formatInfo.mimeType, current.extension)
        ) ?: return toast("This message's location cannot be handed to another app")
        launchMailChooser(context, intent, "Open ${current.name} with", "No app accepted this message")
    }

    fun copyBody() {
        if (body.isBlank()) {
            toast("This message has no body text to copy")
            return
        }
        clipboard.setText(AnnotatedString(body))
        toast("Message body copied")
    }

    fun deliverAttachment(index: Int, openWith: Boolean) {
        val attachment = attachments.getOrNull(index) ?: return
        if (stagingIndex != null) return
        stagingIndex = index
        scope.launch {
            val uri = stageMailAttachment(context, attachment)
            stagingIndex = null
            if (uri == null) {
                toast("Could not read \"${attachment.name}\" out of this message")
                return@launch
            }
            val mime = mailMime(attachment.mime, attachment.extension)
            if (openWith) {
                val intent = ShareUri.viewIntent(context, uri, mime)
                if (intent == null) {
                    toast("Nothing on this device can open \"${attachment.name}\"")
                    return@launch
                }
                launchMailChooser(
                    context,
                    intent,
                    "Open \"${attachment.name}\" with",
                    "No app accepted \"${attachment.name}\""
                )
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_SUBJECT, attachment.name)
                }
                if (!ShareUri.attachStream(context, intent, uri)) {
                    toast("This attachment cannot be shared from its location")
                    return@launch
                }
                launchMailChooser(
                    context,
                    intent,
                    "Share \"${attachment.name}\"",
                    "No app accepted \"${attachment.name}\""
                )
            }
        }
    }

    /* ------------------------------- frame ----------------------------- */

    VennelaBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = VennelaTokens.gutter)
                    // Chrome floats over the page: only a slim top inset, with the
                    // bottom breathing room living inside the scroll content.
                    .padding(top = VennelaTokens.viewerTopInset)
            ) {
                when {
                    file == null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = VennelaTokens.spaceXl)
                    ) {
                        VennelaEmptyState(
                            title = "No message open",
                            hint = "Open an .eml, .msg, .mbox, .ics or .vcf file from Files to read it here.",
                            icon = Icons.Default.MailOutline
                        )
                    }

                    error != null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = VennelaTokens.spaceSm)
                    ) {
                        VennelaErrorState(
                            title = "This message could not be read",
                            message = error,
                            onRetry = { retryToken++ },
                            onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
                        )
                    }

                    isLoading || message == null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = VennelaTokens.spaceSm)
                    ) {
                        VennelaLoadingState(label = "Reading the message headers…")
                    }

                    else -> {
                        MailHeaderCard(
                            subject = message.subject ?: file?.name ?: "Message",
                            from = message.from,
                            to = message.to,
                            date = message.date?.let { normalizeMailDate(it) },
                            location = message.location,
                            kindLabel = file?.formatInfo?.name ?: "Message",
                            sizeLabel = file?.formattedSize ?: "unknown size",
                            attachmentCount = attachments.size
                        )

                        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))

                        ReaderPaper(
                            modifier = Modifier.fillMaxWidth(),
                            maxWidth = 720.dp,
                            cornerRadius = VennelaTokens.radiusCard,
                            contentPadding = PaddingValues(
                                horizontal = VennelaTokens.spaceLg,
                                vertical = VennelaTokens.spaceLg
                            )
                        ) {
                            if (bodyMarkup == null) {
                                Text(
                                    text = if (raw.isBlank()) {
                                        "Nothing readable was found in this file. It may store its body in " +
                                            "an unsupported transfer encoding; the original file is untouched."
                                    } else {
                                        "This file has no plain-text body section."
                                    },
                                    color = palette.textSecondary,
                                    fontSize = VennelaTokens.bodySmall,
                                    lineHeight = 20.sp
                                )
                            } else {
                                Text(
                                    text = bodyMarkup,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        if (attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                            MailAttachmentSection(
                                attachments = attachments,
                                stagingIndex = stagingIndex,
                                onShare = { index -> deliverAttachment(index, openWith = false) },
                                onOpenWith = { index -> deliverAttachment(index, openWith = true) }
                            )
                        }

                        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
                        Column(modifier = Modifier.padding(horizontal = VennelaTokens.spaceXs)) {
                            Text(
                                text = "Read locally from the file's own headers. " +
                                    "No message is downloaded or sent.",
                                color = palette.textTertiary,
                                fontSize = VennelaTokens.nano
                            )
                            if (truncated) {
                                Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                                Text(
                                    text = "Showing the first 400,000 characters of this file; " +
                                        "the original is untouched.",
                                    color = palette.textTertiary,
                                    fontSize = VennelaTokens.nano
                                )
                            }
                        }
                    }
                }

                // Clears the floating action pill without reserving a band.
                Spacer(modifier = Modifier.height(VennelaTokens.spaceHuge + VennelaTokens.spaceXl))
            }

            // Hairline progress, Google Docs style.
            ReaderProgressRule(
                progress = progress,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Floating toolbar — hides while scrolling forward.
            ReaderChromeVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopToolbar(
                    title = file?.name ?: "Message",
                    subtitle = "${file?.extension?.uppercase(Locale.US) ?: "MESSAGE"} • " +
                        "${file?.formattedSize ?: "unknown size"}",
                    onBack = { viewModel.navigateBack() }
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share this message",
                        onClick = { shareMessage() }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open this message in another app",
                        onClick = { openWithMessage() }
                    )
                }
            }

            // Floating action pill — also hides while reading.
            ReaderChromeVisibility(
                visible = chromeVisible,
                fromTop = false,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomPill(
                    status = if (attachments.isNotEmpty()) {
                        "${attachments.size} attachments"
                    } else {
                        "$wordCount words"
                    }
                ) {
                    ReaderIconButton(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Copy message body",
                        onClick = { copyBody() }
                    )
                    ReaderIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "Message details",
                        active = isDetailsOpen,
                        onClick = {
                            isDetailsOpen = !isDetailsOpen
                            chromeVisible = true
                        }
                    )
                }
            }

            /* --------------------------- overlays --------------------------- */

            if (isDetailsOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.backgroundBottom.copy(alpha = 0.35f))
                        .clickable { isDetailsOpen = false }
                )
                MailDetailsPanel(
                    fileName = file?.name ?: "Message",
                    size = file?.formattedSize ?: "unknown size",
                    modified = file?.formattedDate ?: "unknown",
                    kind = file?.formatInfo?.name ?: "Message",
                    extension = file?.extension?.uppercase(Locale.US) ?: "—",
                    bodyCharacters = body.length,
                    wordCount = wordCount,
                    attachmentCount = attachments.size,
                    truncated = truncated,
                    onClose = { isDetailsOpen = false },
                    onOpenProperties = {
                        isDetailsOpen = false
                        viewModel.navigateTo(Screen.FileMetadata)
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = VennelaTokens.viewerTopInset + VennelaTokens.spaceXl)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Header card                                                                */
/* ------------------------------------------------------------------------- */

@Composable
private fun MailHeaderCard(
    subject: String,
    from: String?,
    to: String?,
    date: String?,
    location: String?,
    kindLabel: String,
    sizeLabel: String,
    attachmentCount: Int
) {
    val palette = LocalVennelaPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusCard))
            .background(palette.cardSurface)
            .border(1.dp, palette.cardBorder.copy(alpha = 0.85f), RoundedCornerShape(VennelaTokens.radiusCard))
            .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd)
    ) {
        Text(
            text = subject,
            color = palette.textPrimary,
            fontSize = VennelaTokens.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
        Text(
            text = buildString {
                append(kindLabel)
                append(" • ").append(sizeLabel)
                if (attachmentCount > 0) {
                    append(" • ").append(attachmentCount).append(" attachments")
                }
            },
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.textPrimary.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        from?.let { MailMetaRow("From", it) }
        to?.let { MailMetaRow("To", it) }
        date?.let { MailMetaRow("Date", it) }
        location?.let { MailMetaRow("Where", it) }
    }
}

@Composable
private fun MailMetaRow(label: String, value: String) {
    val palette = LocalVennelaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VennelaTokens.spaceXxs),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = palette.textTertiary,
            fontSize = VennelaTokens.nano,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(VennelaTokens.badgeLg + VennelaTokens.spaceSm)
        )
        Text(
            text = value,
            color = palette.textSecondary,
            fontSize = VennelaTokens.micro,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Attachments                                                                */
/* ------------------------------------------------------------------------- */

@Composable
private fun MailAttachmentSection(
    attachments: List<MailAttachment>,
    stagingIndex: Int?,
    onShare: (Int) -> Unit,
    onOpenWith: (Int) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VennelaTokens.radiusCard))
            .background(palette.cardSurface)
            .border(1.dp, palette.cardBorder.copy(alpha = 0.85f), RoundedCornerShape(VennelaTokens.radiusCard))
            .padding(vertical = VennelaTokens.spaceSm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VennelaTokens.spaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
            Text(
                text = "${attachments.size} " +
                    if (attachments.size == 1) "attachment" else "attachments",
                color = palette.textPrimary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "read-only",
                color = palette.textTertiary,
                fontSize = VennelaTokens.nano,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
        attachments.forEachIndexed { index, attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceXxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileBrandBadge(
                    extension = attachment.extension.ifBlank { "bin" },
                    size = VennelaTokens.badgeSm
                )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.name,
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mailAttachmentDetail(attachment),
                        color = palette.textTertiary,
                        fontSize = VennelaTokens.nano,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (stagingIndex == index) {
                    CircularProgressIndicator(
                        color = accent.secondary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(horizontal = VennelaTokens.spaceXs)
                            .size(VennelaTokens.iconMd)
                    )
                } else {
                    ReaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share ${attachment.name}",
                        onClick = { onShare(index) }
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceXxs))
                    ReaderIconButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open ${attachment.name} in another app",
                        onClick = { onOpenWith(index) }
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Details panel                                                              */
/* ------------------------------------------------------------------------- */

@Composable
private fun MailDetailsPanel(
    fileName: String,
    size: String,
    modified: String,
    kind: String,
    extension: String,
    bodyCharacters: Int,
    wordCount: Int,
    attachmentCount: Int,
    truncated: Boolean,
    onClose: () -> Unit,
    onOpenProperties: () -> Unit,
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
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusSheet))
            .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = "Message details",
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ReaderIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Hide message details",
                onClick = onClose
            )
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        MailInfoRow("File", fileName)
        MailInfoRow("Size", size)
        MailInfoRow("Modified", modified)
        MailInfoRow("Kind", kind)
        MailInfoRow("Extension", extension)
        MailInfoRow("Characters", if (bodyCharacters > 0) "$bodyCharacters" else "—")
        MailInfoRow("Words", "$wordCount")
        MailInfoRow("Attachments", "$attachmentCount")
        MailInfoRow("Body window", if (truncated) "first 400,000 chars" else "complete")
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                .background(accent.primary.copy(alpha = 0.18f))
                .clickable(onClick = onOpenProperties)
                .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = accent.secondary,
                modifier = Modifier.size(VennelaTokens.iconSm)
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            Text(
                text = "Open full file properties",
                color = palette.textPrimary,
                fontSize = VennelaTokens.micro,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MailInfoRow(label: String, value: String) {
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
            fontSize = VennelaTokens.nano,
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

/* ------------------------------------------------------------------------- */
/* Attachment staging (off the main thread)                                   */
/* ------------------------------------------------------------------------- */

private fun launchMailChooser(
    context: Context,
    target: Intent,
    title: String,
    failure: String
) {
    runCatching {
        context.startActivity(
            Intent.createChooser(target, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { Toast.makeText(context, failure, Toast.LENGTH_SHORT).show() }
}

private fun mailAttachmentDetail(attachment: MailAttachment): String {
    val sizeText = when {
        attachment.sizeBytes == null -> "size unavailable"
        attachment.exactSize -> mailFormatBytes(attachment.sizeBytes)
        else -> "≈ ${mailFormatBytes(attachment.sizeBytes)}"
    }
    val mime = attachment.mime?.takeIf { it.isNotBlank() }
    return if (mime != null) "$sizeText • $mime" else sizeText
}

private fun mailFormatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

private fun sanitizeMailStageName(name: String): String {
    val cleaned = name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_')
    return cleaned.ifBlank { "attachment" }.take(80)
}

private fun pruneStagedMailFiles(dir: File) {
    runCatching {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(StagedMailBudget - 1).forEach { it.delete() }
    }
}

/**
 * Decodes one attachment's encoded payload and writes it into the app cache,
 * returning a FileProvider URI so nothing hands a raw `file://` path out.
 */
private suspend fun stageMailAttachment(
    context: Context,
    attachment: MailAttachment
): Uri? = withContext(Dispatchers.IO) {
    val bytes = decodeMailAttachment(attachment) ?: return@withContext null
    val dir = File(context.cacheDir, MailStagedDir)
    if (!dir.exists() && !dir.mkdirs()) return@withContext null
    pruneStagedMailFiles(dir)
    val target = File(dir, sanitizeMailStageName(attachment.name))
    val written = runCatching {
        FileOutputStream(target).use { it.write(bytes) }
    }.isSuccess
    if (!written) return@withContext null
    ShareUri.toShareable(context, Uri.fromFile(target))
}

private fun decodeMailAttachment(attachment: MailAttachment): ByteArray? {
    val payload = attachment.payload
    if (payload.isBlank()) return null
    val bytes = runCatching {
        when {
            attachment.encoding.contains("base64", ignoreCase = true) ->
                Base64.decode(payload.filterNot { it.isWhitespace() }, Base64.DEFAULT)

            attachment.encoding.contains("quoted-printable", ignoreCase = true) ->
                decodeQuotedPrintable(payload)

            else -> payload.toByteArray(Charsets.ISO_8859_1)
        }
    }.getOrNull() ?: return null
    if (bytes.isEmpty() || bytes.size > MaxStagedAttachmentBytes) return null
    return bytes
}

private fun decodeQuotedPrintable(input: String): ByteArray {
    val out = ByteArrayOutputStream(input.length)
    var index = 0
    while (index < input.length) {
        val char = input[index]
        when {
            char == '=' && index + 1 < input.length && input[index + 1] == '\n' -> index += 2
            char == '=' && index + 2 < input.length -> {
                val value = input.substring(index + 1, index + 3).toIntOrNull(16)
                if (value != null) {
                    out.write(value)
                    index += 3
                } else {
                    out.write(char.code and 0xFF)
                    index++
                }
            }

            char == '\n' -> index++
            else -> {
                out.write(char.code and 0xFF)
                index++
            }
        }
    }
    return out.toByteArray()
}

/* ------------------------------------------------------------------------- */
/* Parsing                                                                    */
/* ------------------------------------------------------------------------- */

private fun parseMessage(raw: String, extension: String): MailMessage = when (extension.lowercase(Locale.US)) {
    "ics", "ical", "ifb" -> parseCalendar(raw)
    "vcf", "vcard", "contact" -> parseContact(raw)
    else -> parseEmail(raw)
}

private fun parseEmail(raw: String): MailMessage {
    if (raw.isBlank()) return MailMessage()
    val normalized = raw.replace("\r\n", "\n")
    val headerEnd = normalized.indexOf("\n\n")
    val headerBlock = if (headerEnd >= 0) normalized.substring(0, headerEnd) else normalized
    val rawBody = if (headerEnd >= 0) normalized.substring(headerEnd + 2) else ""

    fun header(name: String): String? = Regex(
        "(?im)^${Regex.escape(name)}:\\s*(.+)$"
    ).find(headerBlock)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    // A multipart message carries the readable text inside its own parts, so
    // prefer the real text/plain (or text/html) part over the MIME framing.
    val boundary = Regex("""(?im)^content-type:[^\n]*boundary\s*=\s*"?([^";\r\n]+)"?""")
        .find(headerBlock)?.groupValues?.get(1)?.trim()
    val preferredBody = if (boundary.isNullOrBlank()) null else mimePreferredBody(normalized, boundary, 0)

    return MailMessage(
        subject = header("Subject"),
        from = header("From"),
        to = header("To"),
        date = header("Date"),
        body = (preferredBody ?: stripHtmlIfNeeded(rawBody)).take(MaxBodyChars),
        attachments = parseAttachments(normalized, headerBlock)
    )
}

/**
 * Finds the message's own readable body part. Returns null when the framing is
 * not a multipart container, so the caller can fall back to the raw body.
 */
private fun mimePreferredBody(source: String, boundary: String, depth: Int): String? {
    if (depth > MaxMimeDepth) return null
    var plain: String? = null
    var html: String? = null
    for (part in splitMimeParts(source, boundary)) {
        val split = part.indexOf("\n\n")
        val headers = if (split >= 0) part.substring(0, split) else part
        val payload = if (split >= 0) part.substring(split + 2) else ""
        val contentType = mailHeaderValue(headers, "Content-Type").orEmpty()
        val disposition = mailHeaderValue(headers, "Content-Disposition").orEmpty()
        if (disposition.contains("attachment", ignoreCase = true)) continue
        val encoding = mailHeaderValue(headers, "Content-Transfer-Encoding").orEmpty()
        val nestedBoundary = Regex("""boundary\s*=\s*"?([^";\r\n]+)"?""", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1)?.trim()
        if (contentType.contains("multipart/", ignoreCase = true) && !nestedBoundary.isNullOrBlank()) {
            val child = mimePreferredBody(payload, nestedBoundary, depth + 1)
            if (child != null && plain == null) plain = child
            continue
        }
        val decoded = decodeMailBodyPart(payload, encoding)
        when {
            contentType.startsWith("text/plain", ignoreCase = true) && plain == null -> plain = decoded
            contentType.startsWith("text/html", ignoreCase = true) && html == null -> html = decoded
        }
    }
    return plain ?: html?.let { stripHtmlIfNeeded(it) }
}

private fun decodeMailBodyPart(payload: String, encoding: String): String {
    if (payload.isBlank()) return ""
    return runCatching {
        when {
            encoding.contains("base64", ignoreCase = true) -> {
                val bytes = Base64.decode(payload.filterNot { it.isWhitespace() }, Base64.DEFAULT)
                String(bytes, Charsets.UTF_8)
            }

            encoding.contains("quoted-printable", ignoreCase = true) ->
                String(decodeQuotedPrintable(payload), Charsets.UTF_8)

            else -> payload
        }
    }.getOrDefault("")
}

private fun parseCalendar(raw: String): MailMessage {
    val normalized = raw.replace("\r\n", "\n")
    fun field(name: String): String? = Regex(
        "(?im)^${Regex.escape(name)}[^:\\n]*:\\s*(.+)$"
    ).find(normalized)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    val body = buildString {
        field("DESCRIPTION")?.let { append(it).append("\n\n") }
        field("LOCATION")?.let { append("Location: ").append(it).append("\n") }
        field("ORGANIZER")?.let { append("Organizer: ").append(it.replace("mailto:", "")).append("\n") }
    }.trim().take(MaxBodyChars)

    return MailMessage(
        subject = field("SUMMARY"),
        from = field("ORGANIZER")?.replace("mailto:", ""),
        to = field("ATTENDEE")?.replace("mailto:", ""),
        date = field("DTSTART"),
        location = field("LOCATION"),
        body = body
    )
}

private fun parseContact(raw: String): MailMessage {
    val normalized = raw.replace("\r\n", "\n")
    fun field(name: String): String? = Regex(
        "(?im)^${Regex.escape(name)}[^:\\n]*:\\s*(.+)$"
    ).find(normalized)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    val phone = field("TEL")
    val email = field("EMAIL")
    val org = field("ORG")?.replace(';', ' ')
    val address = field("ADR")?.replace(';', ' ')?.trim()

    val body = buildString {
        phone?.let { append("Phone: ").append(it).append("\n") }
        email?.let { append("Email: ").append(it).append("\n") }
        org?.let { append("Organization: ").append(it).append("\n") }
        address?.takeIf { it.isNotBlank() }?.let { append("Address: ").append(it).append("\n") }
        field("NOTE")?.let { append("\n").append(it) }
    }.trim().take(MaxBodyChars)

    return MailMessage(
        subject = field("FN") ?: field("N"),
        from = org,
        to = email,
        date = null,
        body = body
    )
}

/* --------------------------- MIME attachments ------------------------------ */

/**
 * Walks the message's own MIME tree and returns the attachments it really
 * declares. Sizes come from `Content-Length` when present, otherwise from the
 * encoded payload length, which is labelled as an estimate in the UI.
 */
private fun parseAttachments(raw: String, headerBlock: String): List<MailAttachment> {
    val boundary = Regex("""(?im)^content-type:[^\n]*boundary\s*=\s*"?([^";\r\n]+)"?""")
        .find(headerBlock)
        ?.groupValues?.get(1)?.trim()
    val parts = if (boundary.isNullOrBlank()) listOf(raw) else splitMimeParts(raw, boundary)
    val collected = ArrayList<MailAttachment>(8)
    parts.forEach { part -> collectAttachmentPart(part, 0, collected) }
    if (collected.isEmpty()) {
        // Last resort for containers without parsable MIME framing: real names
        // only, with no size claim at all.
        Regex("""filename\*?=(?:"([^"]+)"|([^;\r\n]+))""", RegexOption.IGNORE_CASE)
            .findAll(raw)
            .mapNotNull { match ->
                match.groupValues[1].ifBlank { match.groupValues[2] }.trim().takeIf { it.isNotBlank() }
            }
            .distinct()
            .take(MaxAttachmentParts)
            .forEach { name ->
                collected += MailAttachment(
                    name = name,
                    sizeBytes = null,
                    exactSize = false,
                    mime = null,
                    encoding = "",
                    payload = ""
                )
            }
    }
    return collected
}

private fun collectAttachmentPart(
    part: String,
    depth: Int,
    out: MutableList<MailAttachment>
) {
    if (out.size >= MaxAttachmentParts || depth > MaxMimeDepth) return
    val split = part.indexOf("\n\n")
    val headers = if (split >= 0) part.substring(0, split) else part
    val body = if (split >= 0) part.substring(split + 2) else ""
    val contentType = mailHeaderValue(headers, "Content-Type").orEmpty()
    val disposition = mailHeaderValue(headers, "Content-Disposition").orEmpty()
    val encoding = mailHeaderValue(headers, "Content-Transfer-Encoding").orEmpty()

    val nestedBoundary = Regex("""boundary\s*=\s*"?([^";\r\n]+)"?""", RegexOption.IGNORE_CASE)
        .find(contentType)?.groupValues?.get(1)?.trim()
    if (contentType.contains("multipart/", ignoreCase = true) && !nestedBoundary.isNullOrBlank()) {
        splitMimeParts(body, nestedBoundary).forEach { child ->
            collectAttachmentPart(child, depth + 1, out)
        }
        return
    }

    val name = mailFileName(disposition) ?: mailFileName(contentType) ?: return
    val declared = mailHeaderValue(headers, "Content-Length")?.trim()?.toLongOrNull()
    val size = declared ?: estimateMailPayloadBytes(body, encoding)
    val mime = contentType.substringBefore(';').trim().takeIf { it.contains('/') }
    out += MailAttachment(
        name = name,
        sizeBytes = size,
        exactSize = declared != null,
        mime = mime,
        encoding = encoding,
        payload = body.take(MaxAttachmentPayloadChars)
    )
}

private fun splitMimeParts(source: String, boundary: String): List<String> {
    val delimiter = "--$boundary"
    val parts = ArrayList<String>(8)
    var cursor = source.indexOf(delimiter)
    while (cursor >= 0 && parts.size < MaxMimeParts) {
        val start = cursor + delimiter.length
        if (source.startsWith("--", start)) break
        val next = source.indexOf(delimiter, start)
        val end = if (next >= 0) next else source.length
        if (end > start) {
            parts += source.substring(start, end).trimStart('\n')
        }
        cursor = next
    }
    return parts
}

private fun mailHeaderValue(headers: String, name: String): String? = Regex(
    "(?im)^${Regex.escape(name)}\\s*:\\s*([^\\n]*(?:\\n[ \\t][^\\n]*)*)"
).find(headers)
    ?.groupValues?.get(1)
    ?.replace("\n", " ")
    ?.trim()
    ?.takeIf { it.isNotBlank() }

private fun mailFileName(header: String): String? {
    val raw = Regex("""filename\*?=(?:"([^"]+)"|([^;\r\n]+))""", RegexOption.IGNORE_CASE)
        .find(header)
        ?.let { match -> match.groupValues[1].ifBlank { match.groupValues[2] }.trim() }
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val decoded = if (raw.contains("''")) Uri.decode(raw.substringAfter("''")) else raw
    return decoded.replace('/', '_').replace('\\', '_').trim().takeIf { it.isNotBlank() }
}

private fun estimateMailPayloadBytes(payload: String, encoding: String): Long? {
    if (payload.isBlank()) return 0L
    if (encoding.contains("base64", ignoreCase = true)) {
        val chars = payload.count { !it.isWhitespace() }
        val padding = payload.takeLast(2).count { it == '=' }
        return ((chars * 3) / 4 - padding).coerceAtLeast(0).toLong()
    }
    return payload.toByteArray(Charsets.ISO_8859_1).size.toLong()
}

/* ------------------------------ text helpers ------------------------------- */

private fun countMailWords(body: String): Int =
    if (body.isBlank()) 0 else body.split(Regex("\\s+")).count { it.isNotBlank() }

private fun stripHtmlIfNeeded(body: String): String {
    val looksHtml = Regex("<(?:html|body|p|div|br)\\b", RegexOption.IGNORE_CASE).containsMatchIn(body)
    if (!looksHtml) return body
    return body
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}

private fun normalizeMailDate(value: String): String {
    val raw = value.trim()
    val parsers = listOf(
        "EEE, d MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss Z",
        "yyyyMMdd'T'HHmmss'Z'",
        "yyyyMMdd'T'HHmmss",
        "yyyyMMdd"
    )
    for (pattern in parsers) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(raw)
        }.getOrNull()
        if (parsed != null) {
            return SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(parsed)
        }
    }
    return raw
}
