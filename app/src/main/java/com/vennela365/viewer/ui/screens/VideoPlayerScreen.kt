package com.vennela365.viewer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vennela365.viewer.R
import com.vennela365.viewer.media.formatDuration
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.storage.ShareUri
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.VennelaEmptyState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private val videoSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Seek step used by the double-tap zones, in milliseconds. */
private const val DOUBLE_TAP_STEP_MS = 10_000L

private val videoExtensions = setOf(
    "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "flv", "wmv",
    "mpeg", "mpg", "ts", "mts", "m2ts", "ogv", "vob"
)

private val aspectOptions = listOf(
    AspectOption(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit"),
    AspectOption(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill"),
    AspectOption(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom"),
    AspectOption(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT, "Fixed height")
)

private data class AspectOption(val resizeMode: Int, val label: String)

/** What the transparent gesture overlay is currently reporting. */
private data class GestureFeedback(val icon: ImageVector, val label: String, val value: Float, val showBar: Boolean)

/** One genuinely reported audio or text track. */
private data class TrackChoice(val type: Int, val group: TrackGroup, val index: Int, val label: String, val selected: Boolean)

private enum class PlayerSheet { None, Tracks, Playlist }

/** Resume positions survive configuration changes, keyed by file id. */
private val videoResumeSaver = Saver<MutableMap<String, Long>, HashMap<String, Long>>(
    save = { HashMap(it) },
    restore = { it }
)

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}.0×" else "${speed}×"

/** Real [Format] facts, joined; never a synthesised label. */
private fun describeFormat(format: Format): String {
    val parts = mutableListOf<String>()
    format.label?.takeIf { it.isNotBlank() }?.let { parts += it }
    format.language?.takeIf { it.isNotBlank() && it != "und" }?.let { parts += it.uppercase() }
    format.codecs?.takeIf { it.isNotBlank() }?.let { parts += it }
    if (format.channelCount > 0) parts += "${format.channelCount}ch"
    if (format.sampleRate > 0) parts += "${format.sampleRate} Hz"
    if (format.bitrate > 0) parts += "${format.bitrate / 1000} kbps"
    if (format.width > 0 && format.height > 0) parts += "${format.width}×${format.height}"
    return parts.joinToString(" • ")
}

@Composable
fun VideoPlayerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val info by viewModel.mediaInfo.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat().coerceAtLeast(1f)

    val file = selectedFile
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var isFavorite by remember(file?.id) { mutableStateOf(file?.isFavorite == true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var brightness by remember { mutableFloatStateOf(0.75f) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var feedback by remember { mutableStateOf<GestureFeedback?>(null) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatOne by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(PlayerSheet.None) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }
    var gestureScrub by remember { mutableStateOf<Float?>(null) }
    var resumeApplied by remember(file?.id) { mutableStateOf(false) }
    val resumePositions = rememberSaveable(saver = videoResumeSaver) { mutableMapOf<String, Long>() }

    // Bind the shared player before the surface is created.
    val exoPlayer = remember(file?.id) { if (file?.uri != null) viewModel.mediaPlayer() else null }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    // Real sibling videos from the folder the user actually opened.
    val siblings = remember(file?.id) {
        viewModel.siblingMediaFiles().filter { it.extension.lowercase() in videoExtensions }
    }
    val siblingIndex = siblings.indexOfFirst { it.id == file?.id }

    val durationMs = if (mediaState.durationMs > 0) mediaState.durationMs else info.durationMs
    val positionMs = mediaState.positionMs
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val buffered = if (durationMs > 0) (mediaState.bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val isPlaying = mediaState.isPlaying
    val speed = mediaState.speed
    val resumeKey = file?.id
    val storedResume = resumeKey?.let { resumePositions[it] } ?: 0L
    val resumeAt = if (storedResume > 3000L && !resumeApplied && positionMs < 5000L &&
        (durationMs <= 0L || storedResume < durationMs - 5000L)
    ) storedResume else 0L

    // Seed volume and brightness from the real device state once per file.
    LaunchedEffect(file?.id) {
        val manager = audioManager
        if (manager != null && manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) > 0) {
            volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        }
        brightness = 0.75f
        sheet = PlayerSheet.None
    }

    // Immersive playback: hide the system bars while this viewer is on screen.
    if (exoPlayer != null) {
        DisposableEffect(Unit) {
            val window = (context as? android.app.Activity)?.window
            val previousBrightness = window?.attributes?.screenBrightness
            setSystemBarsHidden(window, true)
            onDispose {
                setSystemBarsHidden(window, false)
                if (window != null && previousBrightness != null) {
                    window.attributes = window.attributes.apply { screenBrightness = previousBrightness }
                }
            }
        }
    }

    // The screen only stays awake while playback is really running.
    DisposableEffect(isPlaying) {
        val window = (context as? android.app.Activity)?.window
        if (isPlaying) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Apply the window brightness chosen by the vertical swipe.
    LaunchedEffect(brightness) {
        val window = (context as? android.app.Activity)?.window ?: return@LaunchedEffect
        window.attributes = window.attributes.apply { screenBrightness = brightness.coerceIn(0.05f, 1f) }
    }

    LaunchedEffect(file?.id) {
        val target = file
        if (target?.uri != null && mediaState.uri != target.uri.toString()) viewModel.prepareMedia(target)
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopMedia() } }

    // Track lists are whatever the player reports; polled while it is attached.
    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            tracks = player.currentTracks
            delay(1500L)
        }
    }

    // Remember where the user was, per file id, without hammering the map.
    val positionBucket = positionMs / 5000L
    LaunchedEffect(resumeKey, positionBucket) {
        val key = resumeKey ?: return@LaunchedEffect
        when {
            positionMs < 3000L -> Unit
            durationMs > 0L && positionMs >= durationMs - 5000L -> resumePositions.remove(key)
            else -> resumePositions[key] = positionMs
        }
    }

    // Auto-hide the control bar, resetting on every interaction.
    LaunchedEffect(interactionTick, controlsLocked, sheet) {
        if (controlsLocked || sheet != PlayerSheet.None) return@LaunchedEffect
        interactionTick
        delay(3600L)
        controlsVisible = false
    }

    fun report(icon: ImageVector, label: String, value: Float, showBar: Boolean) {
        feedback = GestureFeedback(icon, label, value, showBar)
        interactionTick += 1
    }

    // The gesture readout fades itself out shortly after the finger stops.
    LaunchedEffect(interactionTick) {
        if (feedback == null) return@LaunchedEffect
        delay(900L)
        feedback = null
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2400L)
            message = null
        }
    }

    fun applyVolume(target: Float) {
        val manager = audioManager ?: return
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return
        val level = (target.coerceIn(0f, 1f) * max).toInt()
        runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0) }
        volume = level.toFloat() / max.toFloat()
    }

    fun playSibling(index: Int) {
        val track = siblings.getOrNull(index) ?: return
        viewModel.openFile(track)
        viewModel.prepareMedia(track, autoPlay = true)
    }

    fun nextIndex(): Int = when {
        shuffle && siblings.size > 1 -> Random.nextInt(siblings.size)
        siblingIndex in 0 until siblings.lastIndex -> siblingIndex + 1
        siblings.isNotEmpty() -> 0
        else -> -1
    }

    fun previousIndex(): Int = when {
        shuffle && siblings.size > 1 -> Random.nextInt(siblings.size)
        siblingIndex > 0 -> siblingIndex - 1
        siblings.isNotEmpty() -> siblings.lastIndex
        else -> -1
    }

    fun shareVideo() {
        val target = file ?: return
        val mime = target.formatInfo.mimeType.takeIf { it.isNotBlank() } ?: "video/*"
        val intent = Intent(Intent.ACTION_SEND).setType(mime)
        if (!ShareUri.attachStream(context, intent, target.uri)) {
            message = "This file has no stream another app can read"
            return
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share ${target.name}")) }
            .onFailure { message = "No app on this device can share the file" }
    }

    fun openWith() {
        val target = file ?: return
        val mime = target.formatInfo.mimeType.takeIf { it.isNotBlank() } ?: "video/*"
        val intent = ShareUri.viewIntent(context, target.uri, mime)
        if (intent == null) {
            message = "This file cannot be handed to another app"
            return
        }
        runCatching { context.startActivity(intent) }
            .onFailure { message = "No installed app can play ${target.extension.uppercase()} files" }
    }

    fun copyPath() {
        val value = file?.let { it.path.ifBlank { it.uri?.toString().orEmpty() } }.orEmpty()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (value.isBlank() || clipboard == null) {
            message = "This file has no copyable path on this device"
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Vennela path", value))
        message = "Path copied"
    }

    fun selectTrack(choice: TrackChoice) {
        val player = exoPlayer ?: return
        val override = TrackSelectionOverride(choice.group, choice.index)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(override).build()
        sheet = PlayerSheet.None
        message = "Track selected"
    }

    fun clearTrackOverride(type: Int) {
        val player = exoPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(type).build()
        message = "Back to automatic selection"
    }

    fun tracksOf(type: Int): List<TrackChoice> = tracks?.groups?.flatMap { group ->
        if (group.type != type) emptyList() else {
            (0 until group.length).filter { group.isTrackSupported(it) }.map { index ->
                TrackChoice(
                    type, group.mediaTrackGroup, index,
                    describeFormat(group.getTrackFormat(index)),
                    runCatching { group.isTrackSelected(index) }.getOrDefault(false)
                )
            }
        }
    }.orEmpty()

    val audioTracks = tracksOf(C.TRACK_TYPE_AUDIO)
    val textTracks = tracksOf(C.TRACK_TYPE_TEXT)

    val subtitle = buildString {
        append("Media3")
        info.resolutionLabel?.let { append(" • $it") }
        info.bitrateLabel?.let { append(" • $it") }
        if (file != null && file.sizeBytes > 0) append(" • ${file.formattedSize}")
        if (audioTracks.size > 1) append(" • ${audioTracks.size} audio tracks")
    }

    fun seekTo(ms: Long) {
        if (durationMs > 0) viewModel.seekMedia((ms.toFloat() / durationMs).coerceIn(0f, 1f))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ---- Real video surface on a true-black letterbox ----
        if (exoPlayer != null) {
            AndroidVideoSurface(player = exoPlayer, resizeMode = resizeMode)
        } else {
            Image(
                painter = painterResource(id = R.drawable.bg_lunar_lake),
                contentDescription = "Vennela lunar backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ---- Gesture layer: above the surface, below every control ----
        Box(
            modifier = Modifier.fillMaxSize()
                .pointerInput(controlsLocked, durationMs) {
                    detectTapGestures(
                        onTap = {
                            if (!controlsLocked) {
                                controlsVisible = !controlsVisible
                                if (controlsVisible) interactionTick += 1
                            }
                        },
                        onDoubleTap = { offset ->
                            if (controlsLocked) return@detectTapGestures
                            val third = size.width / 3f
                            when {
                                offset.x < third -> {
                                    viewModel.skipMedia(-DOUBLE_TAP_STEP_MS)
                                    report(Icons.Default.Replay10, "10 seconds back", 0f, false)
                                    controlsVisible = true
                                }
                                offset.x > third * 2 -> {
                                    viewModel.skipMedia(DOUBLE_TAP_STEP_MS)
                                    report(Icons.Default.Forward10, "10 seconds forward", 0f, false)
                                    controlsVisible = true
                                }
                                else -> {
                                    viewModel.toggleMediaPlayback()
                                    controlsVisible = true
                                    interactionTick += 1
                                }
                            }
                        }
                    )
                }
                .pointerInput(controlsLocked, screenWidthDp, durationMs) {
                    var startX = 0f
                    var seeking = false
                    var target = 0f
                    detectDragGestures(
                        onDragStart = { start ->
                            startX = start.x
                            seeking = false
                            target = progress
                            gestureScrub = null
                        },
                        onDragEnd = {
                            if (seeking && durationMs > 0) viewModel.seekMedia(target.coerceIn(0f, 1f))
                            seeking = false
                            gestureScrub = null
                        },
                        onDragCancel = {
                            seeking = false
                            gestureScrub = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                if (!controlsLocked && durationMs > 0) {
                                    seeking = true
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    target = (target + dragAmount.x / width).coerceIn(0f, 1f)
                                    gestureScrub = target
                                    val at = formatDuration((target * durationMs).toLong())
                                    report(Icons.Default.FastForward, "$at / ${formatDuration(durationMs)}", target, true)
                                }
                            } else if (!seeking && !controlsLocked) {
                                val delta = -dragAmount.y / size.height.toFloat().coerceAtLeast(1f)
                                if (startX < size.width / 2f) {
                                    brightness = (brightness + delta).coerceIn(0.05f, 1f)
                                    report(Icons.Default.Brightness6, "Brightness", brightness, true)
                                } else {
                                    applyVolume(volume + delta)
                                    report(Icons.AutoMirrored.Filled.VolumeUp, "Volume", volume, true)
                                }
                                controlsVisible = true
                            }
                        }
                    )
                }
        )

        // Audio-only file opened in the video viewer: be explicit about it.
        if (file != null && info.hasAudio && !info.hasVideo && mediaState.uri != null && !mediaState.isBuffering) {
            Column( modifier = Modifier.align(Alignment.Center).padding(VennelaTokens.spaceHuge), horizontalAlignment = Alignment.CenterHorizontally ) {
                Icon(Icons.Default.GraphicEq, null, tint = accent.secondary, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                Text( text = "This file has no video track — audio is playing", color = palette.textPrimary, fontSize = VennelaTokens.body )
            }
        }

        // Frosted skeleton while the media prepares instead of a blank frame.
        if (file?.uri != null && (mediaState.uri == null || (mediaState.isBuffering && positionMs == 0L))) {
            VideoSkeleton(modifier = Modifier.fillMaxSize())
        }

        mediaState.error?.let { description ->
            Column( modifier = Modifier.align(Alignment.Center).padding(VennelaTokens.spaceHuge), horizontalAlignment = Alignment.CenterHorizontally ) {
                Icon(Icons.Default.ErrorOutline, null, tint = palette.textPrimary, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                Text( text = "This file cannot be decoded on this device", color = palette.textPrimary, fontSize = VennelaTokens.titleSmall, fontWeight = FontWeight.SemiBold )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                Text( text = description, color = palette.textSecondary, fontSize = VennelaTokens.caption, maxLines = 3, overflow = TextOverflow.Ellipsis )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                Box(
                    modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(accent.primary.copy(alpha = 0.9f))
                        .pressScale(pressedScale = 0.95f) { file?.let { viewModel.prepareMedia(it) } }
                        .padding(horizontal = VennelaTokens.spaceXl),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Retry", color = palette.textPrimary, fontSize = VennelaTokens.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (mediaState.isBuffering && positionMs > 0L) {
            CircularProgressIndicator(
                color = accent.secondary,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(44.dp)
            )
        }

        FadeChrome(visible = feedback != null, modifier = Modifier.align(Alignment.Center)) {
            feedback?.let { GlassFeedback(feedback = it) }
        }
        FadeChrome(
            visible = message != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp)
        ) {
            FeedbackPill(text = message.orEmpty())
        }

        // ---- Auto-hiding frosted chrome ----
        AnimatedVisibility(
            visible = controlsVisible || controlsLocked,
            enter = fadeIn(animationSpec = tween(LiquidMotion.durationFast)) +
                slideInVertically(animationSpec = LiquidMotion.glide(LiquidMotion.durationBase)) { -it / 3 },
            exit = fadeOut(animationSpec = tween(LiquidMotion.durationFast)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopChrome(
                title = file?.name ?: "No video selected",
                subtitle = subtitle,
                isFavorite = isFavorite,
                onBack = { viewModel.navigateBack() },
                onToggleFavorite = {
                    file?.let { viewModel.toggleFavorite(it) }
                    isFavorite = !isFavorite
                    interactionTick += 1
                },
                onShare = { shareVideo() },
                onOpenWith = { openWith() },
                onCopyPath = { copyPath() },
                onDetails = { viewModel.navigateTo(Screen.FileMetadata) }
            )
        }

        FadeChrome(visible = controlsVisible && !controlsLocked, modifier = Modifier.align(Alignment.Center)) {
            CenterPlayButton(isPlaying = isPlaying, onClick = { viewModel.toggleMediaPlayback() })
        }

        // Lock toggle — always reachable while the chrome is up.
        FadeChrome(
            visible = controlsVisible || controlsLocked,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = VennelaTokens.spaceLg)
        ) {
            GlassIconCircle(
                icon = if (controlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (controlsLocked) "Unlock controls" else "Lock controls",
                selected = controlsLocked,
                onClick = {
                    controlsLocked = !controlsLocked
                    if (!controlsLocked) interactionTick += 1
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !controlsLocked,
            enter = fadeIn(animationSpec = tween(LiquidMotion.durationFast)) +
                slideInVertically(animationSpec = LiquidMotion.glide(LiquidMotion.durationBase)) { it / 3 },
            exit = fadeOut(animationSpec = tween(LiquidMotion.durationFast)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(
                positionMs = positionMs,
                durationMs = durationMs,
                progress = progress,
                buffered = buffered,
                scrubOverride = gestureScrub,
                isPlaying = isPlaying,
                speed = speed,
                resizeMode = resizeMode,
                shuffle = shuffle,
                repeatOne = repeatOne,
                resumeAt = resumeAt,
                onResume = {
                    seekTo(storedResume)
                    resumeApplied = true
                    interactionTick += 1
                },
                onSeek = { fraction -> viewModel.seekMedia(fraction) },
                onScrubFeedback = { interactionTick += 1 },
                onSkipBack = { viewModel.skipMedia(-DOUBLE_TAP_STEP_MS) },
                onSkipForward = { viewModel.skipMedia(DOUBLE_TAP_STEP_MS) },
                onPlayPause = { viewModel.toggleMediaPlayback() },
                onRestart = { viewModel.seekMedia(0f) },
                onPrevious = { previousIndex().takeIf { it >= 0 }?.let { playSibling(it) } },
                onNext = { nextIndex().takeIf { it >= 0 }?.let { playSibling(it) } },
                onSpeed = { viewModel.setMediaSpeed(it) },
                onAspect = { resizeMode = it },
                onShuffle = { shuffle = !shuffle },
                onRepeat = {
                    repeatOne = !repeatOne
                    viewModel.setMediaRepeat(repeatOne)
                },
                onTracks = {
                    sheet = PlayerSheet.Tracks
                    interactionTick += 1
                },
                onPlaylist = {
                    sheet = PlayerSheet.Playlist
                    interactionTick += 1
                },
                onHideControls = { controlsVisible = false },
                onInteract = { interactionTick += 1 }
            )
        }

        // ---- Tracks / playlist sheet ----
        AnimatedVisibility(
            visible = sheet != PlayerSheet.None,
            enter = slideInVertically(animationSpec = LiquidMotion.liquid()) { it } +
                fadeIn(animationSpec = LiquidMotion.glide(LiquidMotion.durationFast)),
            exit = slideOutVertically(animationSpec = LiquidMotion.exit()) { it } +
                fadeOut(animationSpec = LiquidMotion.exit()),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerSheetPanel(
                sheet = sheet,
                audioTracks = audioTracks,
                textTracks = textTracks,
                playlist = siblings,
                currentFileId = file?.id,
                isPlaying = isPlaying,
                onSelectTrack = { selectTrack(it) },
                onClearOverride = { clearTrackOverride(it) },
                onPickSheet = { sheet = it },
                onPlayIndex = { index ->
                    sheet = PlayerSheet.None
                    playSibling(index)
                },
                onDismiss = { sheet = PlayerSheet.None }
            )
        }

        // Empty state when nothing was handed to the viewer.
        if (file == null || file.uri == null) {
            Box( modifier = Modifier.fillMaxSize().background(palette.backgroundBottom.copy(alpha = 0.62f)), contentAlignment = Alignment.Center ) {
                VennelaEmptyState(
                    title = "No video selected",
                    hint = "Open a video from Files or Search to start playback.",
                    icon = Icons.Default.PlayArrow,
                    actionLabel = "Back",
                    onAction = { viewModel.navigateBack() }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Chrome                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
private fun FadeChrome(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(LiquidMotion.durationFast)),
        exit = fadeOut(animationSpec = tween(LiquidMotion.durationBase)),
        modifier = modifier
    ) { content() }
}

@Composable
private fun AndroidVideoSurface(player: Player, resizeMode: Int) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = resizeMode
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TopChrome(
    title: String,
    subtitle: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onCopyPath: () -> Unit,
    onDetails: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var menuOpen by remember { mutableStateOf(false) }
    val spec = liquidGlassSpec(alpha = 0.40f, frost = VennelaTokens.frostRegular, elevation = VennelaTokens.elevationBar)
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceMd)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusCardLarge))
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconCircle(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = VennelaTokens.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text( text = subtitle, color = palette.textSecondary, fontSize = VennelaTokens.micro, maxLines = 1, overflow = TextOverflow.Ellipsis )
        }
        GlassIconCircle(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            selected = isFavorite,
            onClick = onToggleFavorite
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceXs))
        Box {
            GlassIconCircle(Icons.Default.MoreVert, "More options for this video", { menuOpen = true }, tint = accent.secondary)
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                MenuAction("Share", Icons.Default.Share) { menuOpen = false; onShare() }
                MenuAction("Open with", Icons.AutoMirrored.Filled.OpenInNew) { menuOpen = false; onOpenWith() }
                MenuAction("Copy path", Icons.Default.ContentCopy) { menuOpen = false; onCopyPath() }
                MenuAction("File details", Icons.Default.MoreVert) { menuOpen = false; onDetails() }
            }
        }
    }
}

@Composable
private fun MenuAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    DropdownMenuItem(
        text = { Text(label, color = palette.textPrimary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = palette.textSecondary) },
        onClick = onClick
    )
}

@Composable
private fun CenterPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val pulse by animateFloatAsState(
        targetValue = if (isPlaying) 0.92f else 1f,
        animationSpec = LiquidMotion.liquid(),
        label = "centerPlayPulse"
    )
    Box(
        modifier = Modifier.size(84.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            // Bright hero control: neon halo + gradient disc.
            .shadow(
                elevation = 28.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.80f),
                spotColor = accent.primary.copy(alpha = 0.80f)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.92f),
                        accent.primary.copy(alpha = 0.88f)
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.80f), CircleShape)
            .pressScale(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(42.dp)
        )
    }
}

@Composable
private fun GlassIconCircle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    tint: Color? = null
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val resolved = tint ?: if (selected) Color.White else palette.textPrimary
    Box(
        modifier = Modifier.size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(accent.primary.copy(alpha = 0.90f), accent.secondary.copy(alpha = 0.62f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            palette.backgroundBottom.copy(alpha = 0.38f),
                            palette.backgroundBottom.copy(alpha = 0.24f)
                        )
                    )
                }
            )
            .border(
                1.dp,
                accent.secondary.copy(alpha = if (selected) 0.80f else 0.34f),
                CircleShape
            )
            .pressScale(pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon( imageVector = icon, contentDescription = contentDescription, tint = resolved, modifier = Modifier.size(VennelaTokens.iconMd) )
    }
}

/* ------------------------------------------------------------------------- */
/* Gesture feedback + loading skeleton                                        */
/* ------------------------------------------------------------------------- */

@Composable
private fun GlassFeedback(feedback: GestureFeedback) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(
        alpha = 0.46f,
        frost = VennelaTokens.frostRegular,
        elevation = VennelaTokens.elevationOverlay
    )
    Column(
        modifier = Modifier.liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusCard))
            .padding(horizontal = VennelaTokens.spaceXl, vertical = VennelaTokens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(feedback.icon, null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconXl))
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        Text( text = feedback.label, color = palette.textPrimary, fontSize = VennelaTokens.titleSmall, fontWeight = FontWeight.Bold )
        if (feedback.showBar) {
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            Box( modifier = Modifier.width(132.dp).height(6.dp) .clip(RoundedCornerShape(VennelaTokens.radiusPill)) .background(palette.textPrimary.copy(alpha = 0.16f)) ) {
                Box(
                    modifier = Modifier.fillMaxWidth(feedback.value.coerceIn(0f, 1f)).height(6.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                        .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
                )
            }
        }
    }
}

@Composable
private fun FeedbackPill(text: String) {
    val palette = LocalVennelaPalette.current
    val spec = liquidGlassSpec(
        alpha = 0.44f,
        frost = VennelaTokens.frostLight,
        elevation = VennelaTokens.elevationOverlay
    )
    Box( modifier = Modifier.liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill)) .padding(horizontal = VennelaTokens.spaceXl, vertical = VennelaTokens.spaceSm) ) {
        Text( text = text, color = palette.textPrimary, fontSize = VennelaTokens.caption, fontWeight = FontWeight.Medium, maxLines = 2 )
    }
}

/** Frosted placeholder blocks so a preparing video never shows a blank frame. */
@Composable
private fun VideoSkeleton(modifier: Modifier = Modifier) {
    val palette = LocalVennelaPalette.current
    val alpha by rememberInfiniteTransition(label = "videoSkeleton").animateFloat(
        initialValue = 0.10f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoSkeletonAlpha"
    )
    Box(modifier = modifier.padding(VennelaTokens.spaceXl), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .background(palette.backgroundBottom.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(VennelaTokens.spaceLg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.62f).height(14.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(palette.cardSurface.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                Box(
                    modifier = Modifier.fillMaxWidth(0.86f).height(10.dp)
                        .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                        .background(palette.cardSurface.copy(alpha = alpha))
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Bottom controls                                                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun BottomControls(
    positionMs: Long,
    durationMs: Long,
    progress: Float,
    buffered: Float,
    scrubOverride: Float?,
    isPlaying: Boolean,
    speed: Float,
    resizeMode: Int,
    shuffle: Boolean,
    repeatOne: Boolean,
    resumeAt: Long,
    onResume: () -> Unit,
    onSeek: (Float) -> Unit,
    onScrubFeedback: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeed: (Float) -> Unit,
    onAspect: (Int) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onTracks: () -> Unit,
    onPlaylist: () -> Unit,
    onHideControls: () -> Unit,
    onInteract: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var speedMenuOpen by remember { mutableStateOf(false) }
    var aspectMenuOpen by remember { mutableStateOf(false) }
    val spec = liquidGlassSpec(
        alpha = 0.46f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationDock,
        sheen = 0.16f
    )
    val shownPosition = scrubOverride?.let { (it * durationMs).toLong() } ?: positionMs

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = VennelaTokens.gutter, vertical = VennelaTokens.spaceMd)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusSheet))
            .padding(horizontal = VennelaTokens.spaceLg, vertical = VennelaTokens.spaceMd)
    ) {
        if (resumeAt > 0L) {
            Row(
                modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(accent.primary.copy(alpha = 0.26f))
                    .border(1.dp, accent.secondary.copy(alpha = 0.5f), RoundedCornerShape(VennelaTokens.radiusPill))
                    .pressScale(pressedScale = 0.96f, onClick = onResume)
                    .padding(horizontal = VennelaTokens.spaceLg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm))
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Text(
                    text = "Resume from ${formatDuration(resumeAt)}",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
        }

        Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically ) {
            Text(
                text = "${formatDuration(shownPosition)} / ${formatDuration(durationMs)}",
                color = palette.textPrimary,
                fontSize = VennelaTokens.caption,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text( text = if (isPlaying) "Playing • screen awake" else "Paused", color = palette.textSecondary, fontSize = VennelaTokens.nano )
                Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                Box( modifier = Modifier.size(VennelaTokens.touchTargetMin).clip(CircleShape).clickable(onClick = onHideControls), contentAlignment = Alignment.Center ) {
                    Icon( Icons.Default.FullscreenExit, contentDescription = "Hide controls", tint = palette.textPrimary, modifier = Modifier.size(VennelaTokens.iconMd) )
                }
            }
        }

        VideoSeekBar(
            progress = scrubOverride ?: progress,
            buffered = buffered,
            onSeek = onSeek,
            onScrubFeedback = onScrubFeedback
        )
        Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))

        Row( modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically ) {
            CircleAction(Icons.Default.SkipPrevious, "Previous video in this folder", onPrevious)
            CircleAction(Icons.Default.RestartAlt, "Restart from beginning", onRestart)
            CircleAction(Icons.Default.Replay10, "Back 10 seconds", onSkipBack)
            Box(
                modifier = Modifier.size(62.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.secondary, accent.primary)))
                    .border(1.5.dp, palette.textPrimary.copy(alpha = 0.85f), CircleShape)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            CircleAction(Icons.Default.Forward10, "Forward 10 seconds", onSkipForward)
            CircleAction(Icons.Default.SkipNext, "Next video in this folder", onNext)
            CircleAction(Icons.Default.Fullscreen, "Choose how the video fills the screen") {
                aspectMenuOpen = true
                onInteract()
            }
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = VennelaTokens.spaceSm)
        ) {
            item {
                Box {
                    PlayerChip(formatSpeed(speed), Icons.Default.Speed, speed != 1f) {
                        speedMenuOpen = true
                        onInteract()
                    }
                    OptionMenu(
                        expanded = speedMenuOpen,
                        labels = videoSpeeds.map { formatSpeed(it) },
                        selections = videoSpeeds.map { it == speed },
                        onDismiss = { speedMenuOpen = false },
                        onPick = { index ->
                            onSpeed(videoSpeeds[index])
                            speedMenuOpen = false
                        }
                    )
                }
            }
            item {
                Box {
                    val current = aspectOptions.firstOrNull { it.resizeMode == resizeMode }
                    PlayerChip(
                        text = current?.label ?: "Fit",
                        icon = Icons.Default.AspectRatio,
                        isActive = resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ) {
                        aspectMenuOpen = true
                        onInteract()
                    }
                    OptionMenu(
                        expanded = aspectMenuOpen,
                        labels = aspectOptions.map { it.label },
                        selections = aspectOptions.map { it.resizeMode == resizeMode },
                        onDismiss = { aspectMenuOpen = false },
                        onPick = { index ->
                            onAspect(aspectOptions[index].resizeMode)
                            aspectMenuOpen = false
                        }
                    )
                }
            }
            item { PlayerChip("Shuffle", Icons.Default.Shuffle, shuffle, onShuffle) }
            item {
                PlayerChip(
                    text = if (repeatOne) "Repeat one" else "Repeat",
                    icon = if (repeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    isActive = repeatOne,
                    onClick = onRepeat
                )
            }
            item { PlayerChip("Tracks", Icons.Default.Audiotrack, onClick = onTracks) }
            item { PlayerChip("Playlist", Icons.AutoMirrored.Filled.PlaylistPlay, onClick = onPlaylist) }
        }
    }
}

/** Dropdown of labelled options with a real check mark on the active one. */
@Composable
private fun OptionMenu(
    expanded: Boolean,
    labels: List<String>,
    selections: List<Boolean>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        labels.forEachIndexed { index, label ->
            val selected = selections.getOrElse(index) { false }
            DropdownMenuItem(
                text = {
                    Text( text = label, color = palette.textPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal )
                },
                trailingIcon = {
                    if (selected) {
                        Icon( Icons.Default.Check, contentDescription = null, tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm) )
                    }
                },
                onClick = { onPick(index) }
            )
        }
    }
}

/** [LiquidChip] at a full 48dp touch target, the player's chip everywhere. */
@Composable
private fun PlayerChip(
    text: String,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    LiquidChip(
        text = text,
        modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin),
        icon = icon,
        isActive = isActive,
        onClick = onClick
    )
}

@Composable
private fun VideoSeekBar(
    progress: Float,
    buffered: Float,
    onSeek: (Float) -> Unit,
    onScrubFeedback: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    var scrubFraction by remember { mutableFloatStateOf(-1f) }
    val shown = if (scrubFraction >= 0f) scrubFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxWidth().height(VennelaTokens.touchTargetMin)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    scrubFraction = fraction
                    onSeek(fraction)
                    onScrubFeedback()
                    scrubFraction = -1f
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        scrubFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        onScrubFeedback()
                    },
                    onDragEnd = {
                        onSeek(scrubFraction.coerceIn(0f, 1f))
                        scrubFraction = -1f
                    },
                    onDragCancel = {
                        onSeek(scrubFraction.coerceIn(0f, 1f))
                        scrubFraction = -1f
                    }
                ) { change, _ ->
                    change.consume()
                    scrubFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    onScrubFeedback()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box( modifier = Modifier.fillMaxWidth().height(5.dp) .clip(RoundedCornerShape(VennelaTokens.radiusPill)) .background(palette.textPrimary.copy(alpha = 0.22f)) )
        Box(
            modifier = Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(5.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(palette.textPrimary.copy(alpha = 0.34f))
        )
        Box(
            modifier = Modifier.fillMaxWidth(shown).height(5.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary)))
        )
        Box(modifier = Modifier.fillMaxWidth(shown), contentAlignment = Alignment.CenterEnd) {
            Box( modifier = Modifier.size(14.dp) .clip(CircleShape) .background(palette.textPrimary) .border(2.dp, accent.secondary, CircleShape) )
        }
    }
}

@Composable
private fun CircleAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    Box( modifier = Modifier.size(VennelaTokens.touchTargetMin).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center ) {
        Icon( imageVector = icon, contentDescription = contentDescription, tint = palette.textPrimary, modifier = Modifier.size(VennelaTokens.iconLg) )
    }
}

/* ------------------------------------------------------------------------- */
/* Tracks + playlist sheet                                                    */
/* ------------------------------------------------------------------------- */

@Composable
private fun PlayerSheetPanel(
    sheet: PlayerSheet,
    audioTracks: List<TrackChoice>,
    textTracks: List<TrackChoice>,
    playlist: List<FileItem>,
    currentFileId: String?,
    isPlaying: Boolean,
    onSelectTrack: (TrackChoice) -> Unit,
    onClearOverride: (Int) -> Unit,
    onPickSheet: (PlayerSheet) -> Unit,
    onPlayIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val spec = liquidGlassSpec(
        alpha = 0.52f,
        frost = VennelaTokens.frostHeavy,
        elevation = VennelaTokens.elevationOverlay
    )
    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(max = 440.dp)
            .padding(horizontal = VennelaTokens.spaceSm)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusSheet))
            .padding(bottom = VennelaTokens.spaceMd)
    ) {
        Box(
            modifier = Modifier.padding(top = VennelaTokens.spaceMd)
                .align(Alignment.CenterHorizontally)
                .width(44.dp).height(4.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .background(palette.textTertiary.copy(alpha = 0.45f))
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = VennelaTokens.spaceLg,
                end = VennelaTokens.spaceSm,
                top = VennelaTokens.spaceMd,
                bottom = VennelaTokens.spaceSm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (sheet == PlayerSheet.Tracks) "Tracks" else "Playlist",
                    color = palette.textPrimary,
                    fontSize = VennelaTokens.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (sheet == PlayerSheet.Tracks) {
                        "Reported by the player for this file"
                    } else {
                        "${playlist.size} videos in this folder"
                    },
                    color = palette.textSecondary,
                    fontSize = VennelaTokens.micro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box( modifier = Modifier.size(VennelaTokens.touchTargetMin).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center ) {
                Icon( Icons.Default.FullscreenExit, contentDescription = "Close panel", tint = palette.textSecondary, modifier = Modifier.size(VennelaTokens.iconMd) )
            }
        }
        Row( modifier = Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.spaceMd), horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm) ) {
            PlayerChip("Tracks", Icons.Default.Audiotrack, sheet == PlayerSheet.Tracks) {
                onPickSheet(PlayerSheet.Tracks)
            }
            PlayerChip("Playlist", Icons.AutoMirrored.Filled.PlaylistPlay, sheet == PlayerSheet.Playlist) {
                onPickSheet(PlayerSheet.Playlist)
            }
        }
        Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))

        if (sheet == PlayerSheet.Tracks) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(
                    start = VennelaTokens.spaceSm,
                    end = VennelaTokens.spaceSm,
                    bottom = VennelaTokens.spaceLg
                )
            ) {
                if (audioTracks.isEmpty() && textTracks.isEmpty()) {
                    item {
                        VennelaEmptyState(
                            title = "No separate tracks reported",
                            hint = "This file plays a single combined stream, so there is nothing to switch between.",
                            icon = Icons.Default.Audiotrack
                        )
                    }
                }
                if (audioTracks.isNotEmpty()) {
                    item { TrackGroupHeader("Audio") { onClearOverride(C.TRACK_TYPE_AUDIO) } }
                    itemsIndexed(audioTracks, key = { i, t -> "a$i-${t.label}" }) { index, track ->
                        TrackRow(index, track.label, track.selected) { onSelectTrack(track) }
                    }
                }
                if (textTracks.isNotEmpty()) {
                    item { TrackGroupHeader("Subtitles") { onClearOverride(C.TRACK_TYPE_TEXT) } }
                    itemsIndexed(textTracks, key = { i, t -> "s$i-${t.label}" }) { index, track ->
                        TrackRow(index, track.label, track.selected) { onSelectTrack(track) }
                    }
                }
            }
        } else if (playlist.isEmpty()) {
            VennelaEmptyState(
                title = "No other videos here",
                hint = "Open this video from a folder so its neighbours can be queued.",
                icon = Icons.AutoMirrored.Filled.PlaylistPlay
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(
                    start = VennelaTokens.spaceSm,
                    end = VennelaTokens.spaceSm,
                    bottom = VennelaTokens.spaceLg
                ),
                verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXxs)
            ) {
                itemsIndexed(playlist, key = { _, item -> item.id }) { index, item ->
                    PlaylistRow(index, item, item.id == currentFileId, isPlaying) { onPlayIndex(index) }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(index: Int, item: FileItem, active: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier.fillMaxWidth().staggeredEntrance(index)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(if (active) accent.primary.copy(alpha = 0.24f) else palette.cardSurface.copy(alpha = 0f))
            .clickable(onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(VennelaTokens.badgeSm)
                .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                .background(accent.primary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    active && isPlaying -> Icons.Default.GraphicEq
                    active -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = if (active) accent.secondary else palette.textSecondary,
                modifier = Modifier.size(VennelaTokens.iconMd)
            )
        }
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = palette.textPrimary,
                fontSize = VennelaTokens.bodySmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(item.extension.uppercase())
                    if (item.sizeBytes > 0) append(" • ${item.formattedSize}")
                    if (active) append(" • now playing")
                },
                color = if (active) accent.secondary else palette.textSecondary,
                fontSize = VennelaTokens.micro,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TrackGroupHeader(text: String, onAuto: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, color = palette.textSecondary, fontSize = VennelaTokens.micro, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.heightIn(min = VennelaTokens.touchTargetMin)
                .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                .pressScale(pressedScale = 0.94f, onClick = onAuto)
                .padding(horizontal = VennelaTokens.spaceMd),
            contentAlignment = Alignment.Center
        ) {
            Text( text = "Automatic", color = accent.secondary, fontSize = VennelaTokens.micro, fontWeight = FontWeight.SemiBold )
        }
    }
}

@Composable
private fun TrackRow(index: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    Row(
        modifier = Modifier.fillMaxWidth().staggeredEntrance(index)
            .clip(RoundedCornerShape(VennelaTokens.radiusControl))
            .background(if (selected) accent.primary.copy(alpha = 0.24f) else palette.cardSurface.copy(alpha = 0f))
            .clickable(onClick = onClick)
            .padding(horizontal = VennelaTokens.spaceMd, vertical = VennelaTokens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Subtitles,
            contentDescription = null,
            tint = if (selected) accent.secondary else palette.textSecondary,
            modifier = Modifier.size(VennelaTokens.iconMd)
        )
        Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
        Text(
            text = label.ifBlank { "Track ${index + 1}" },
            color = palette.textPrimary,
            fontSize = VennelaTokens.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon( Icons.Default.Check, contentDescription = "Currently selected", tint = accent.secondary, modifier = Modifier.size(VennelaTokens.iconSm) )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Small platform helpers                                                     */
/* ------------------------------------------------------------------------- */

private fun setSystemBarsHidden(window: android.view.Window?, hidden: Boolean) {
    if (window == null) return
    val view = window.decorView
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        val controller = window.insetsController ?: return
        if (hidden) controller.hide(android.view.WindowInsets.Type.systemBars())
        else controller.show(android.view.WindowInsets.Type.systemBars())
    } else {
        @Suppress("DEPRECATION")
        view.systemUiVisibility = if (hidden) {
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            android.view.View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
