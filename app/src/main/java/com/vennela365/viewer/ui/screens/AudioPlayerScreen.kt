package com.vennela365.viewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vennela365.viewer.R
import com.vennela365.viewer.media.MediaEngine
import com.vennela365.viewer.media.formatDuration
import com.vennela365.viewer.model.FileItem
import com.vennela365.viewer.theme.LocalVennelaAccent
import com.vennela365.viewer.theme.LocalVennelaPalette
import com.vennela365.viewer.theme.LiquidMotion
import com.vennela365.viewer.theme.VennelaFonts
import com.vennela365.viewer.theme.VennelaTokens
import com.vennela365.viewer.theme.liquidGlassSpec
import com.vennela365.viewer.theme.liquidGlass
import com.vennela365.viewer.ui.components.GlassPanel
import com.vennela365.viewer.ui.components.LiquidButton
import com.vennela365.viewer.ui.components.LiquidChip
import com.vennela365.viewer.ui.components.LiquidPage
import com.vennela365.viewer.ui.components.ReaderIconButton
import com.vennela365.viewer.ui.components.ReaderTopToolbar
import com.vennela365.viewer.ui.components.VennelaEmptyState
import com.vennela365.viewer.ui.components.pressScale
import com.vennela365.viewer.ui.components.staggeredEntrance
import com.vennela365.viewer.viewmodel.MainViewModel
import com.vennela365.viewer.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val audioExtensions = setOf(
    "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "wma",
    "amr", "aiff", "aif", "alac", "ape", "mka", "mid", "midi", "dsf", "dff"
)

private val audioSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Sleep-timer presets, in minutes. */
private val sleepPresets = listOf(5, 10, 15, 30, 60)

@Composable
fun AudioPlayerScreen(viewModel: MainViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val mediaState by viewModel.mediaState.collectAsState()
    val info by viewModel.mediaInfo.collectAsState()
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val context = LocalContext.current

    val file = selectedFile
    val fileUri = file?.uri?.toString()

    // Real cover art from the container's embedded picture, when present.
    val artwork by produceState<ByteArray?>(null, fileUri) {
        value = MediaEngine.embeddedArtwork(context, file?.uri)
    }

    // Real sibling tracks from the folder the user actually opened.
    val queue = remember(file?.id) {
        viewModel.siblingMediaFiles().filter { it.extension.lowercase() in audioExtensions }
    }
    val currentIndex = queue.indexOfFirst { it.id == file?.id }

    // Cover art for the neighbouring tracks, used by the medallion's shoulders.
    val previousArtwork by produceState<ByteArray?>(null, queue.getOrNull(currentIndex - 1)?.id) {
        value = MediaEngine.embeddedArtwork(context, queue.getOrNull(currentIndex - 1)?.uri)
    }
    val nextArtwork by produceState<ByteArray?>(null, queue.getOrNull(currentIndex + 1)?.id) {
        value = MediaEngine.embeddedArtwork(context, queue.getOrNull(currentIndex + 1)?.uri)
    }

    var isFavorite by remember(file?.id) { mutableStateOf(file?.isFavorite == true) }
    var repeatOne by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var sleepRemainingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(file?.id) {
        val target = file
        if (target?.uri != null && mediaState.uri != target.uri.toString()) {
            viewModel.prepareMedia(target)
        }
    }

    // A viewer stops its own audio when you leave it.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopMedia() }
    }

    // Sleep timer: counts down real seconds and pauses media when it lands.
    LaunchedEffect(sleepRemainingSeconds) {
        if (sleepRemainingSeconds > 0) {
            delay(1000L)
            sleepRemainingSeconds -= 1
            if (sleepRemainingSeconds == 0) viewModel.toggleMediaPlayback()
        }
    }

    val isPlaying = mediaState.isPlaying
    val positionMs = mediaState.positionMs
    val durationMs = if (mediaState.durationMs > 0) mediaState.durationMs else info.durationMs
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val title = info.title?.takeIf { it.isNotBlank() } ?: file?.name ?: "No audio selected"
    val artist = info.artist?.takeIf { it.isNotBlank() } ?: "Unknown artist"

    fun playAt(index: Int) {
        val track = queue.getOrNull(index) ?: return
        viewModel.openFile(track)
        viewModel.prepareMedia(track, autoPlay = true)
    }

    fun nextIndex(): Int = when {
        shuffle && queue.size > 1 -> Random.nextInt(queue.size)
        currentIndex in 0 until queue.lastIndex -> currentIndex + 1
        queue.isNotEmpty() -> 0
        else -> -1
    }

    fun previousIndex(): Int = when {
        shuffle && queue.size > 1 -> Random.nextInt(queue.size)
        currentIndex > 0 -> currentIndex - 1
        queue.isNotEmpty() -> queue.lastIndex
        else -> -1
    }

    LiquidPage(
        title = null
    ) { insets ->
        Box(modifier = Modifier.fillMaxSize()) {
        // Floating, slim transport bar — the artwork owns the screen.
        ReaderTopToolbar(
            title = file?.name ?: "Now Playing",
            subtitle = if (queue.isEmpty()) "Single track" else "Track ${currentIndex + 1} of ${queue.size}",
            onBack = { viewModel.navigateBack() },
            modifier = Modifier.padding(horizontal = VennelaTokens.gutter)
        ) {
            ReaderIconButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                active = isFavorite,
                onClick = {
                    file?.let { viewModel.toggleFavorite(it) }
                    isFavorite = !isFavorite
                }
            )
            Spacer(Modifier.width(VennelaTokens.spaceXs))
            ReaderIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "File details and options",
                onClick = { viewModel.navigateTo(Screen.FileMetadata) }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VennelaTokens.gutter,
                end = VennelaTokens.gutter,
                top = 68.dp,
                bottom = insets.calculateBottomPadding()
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (file == null || file.uri == null) {
                item {
                    VennelaEmptyState(
                        title = "No audio selected",
                        hint = "Open a track from Files or Search to start listening.",
                        icon = Icons.Default.MusicNote
                    )
                }
                return@LazyColumn
            }

            // ---- Frosted album-art medallion with cover-art shoulders ----
            item {
                AlbumMedallion(
                    artwork = artwork,
                    previousArtwork = previousArtwork,
                    nextArtwork = nextArtwork,
                    isPlaying = isPlaying,
                    onSwipePrevious = {
                        val previous = previousIndex()
                        if (previous >= 0) playAt(previous)
                    },
                    onSwipeNext = {
                        val next = nextIndex()
                        if (next >= 0) playAt(next)
                    }
                )
            }

            // ---- Title / artist ----
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = palette.textPrimary,
                            fontSize = VennelaTokens.titleDisplay,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(VennelaTokens.spaceXxs))
                        Text(
                            text = listOfNotNull(artist, info.album?.takeIf { it.isNotBlank() })
                                .joinToString(" • "),
                            color = palette.textSecondary,
                            fontSize = VennelaTokens.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (mediaState.isBuffering) {
                        CircularProgressIndicator(
                            color = accent.secondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            mediaState.error?.let { message ->
                item {
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceMd))
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VennelaTokens.radiusControl)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = VennelaTokens.spaceLg,
                                vertical = VennelaTokens.spaceMd
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = accent.secondary,
                                modifier = Modifier.size(VennelaTokens.iconMd)
                            )
                            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))
                            Text(
                                text = message,
                                color = palette.textPrimary,
                                fontSize = VennelaTokens.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ---- Scrubbable waveform ----
            item {
                val scrubFraction = remember { mutableStateOf(-1f) }
                val shownProgress = if (scrubFraction.value >= 0f) scrubFraction.value else progress

                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceLg)
                ) {
                    WaveformSeekBar(
                        progress = shownProgress,
                        playedColor = accent.secondary,
                        idleColor = palette.textTertiary.copy(alpha = 0.42f),
                        modifier = Modifier.scrubbable(
                            onScrub = { fraction -> scrubFraction.value = fraction },
                            onCommit = { fraction ->
                                scrubFraction.value = -1f
                                viewModel.seekMedia(fraction)
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(VennelaTokens.spaceXs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val shownPosition = if (scrubFraction.value >= 0f && durationMs > 0) {
                            (scrubFraction.value * durationMs).toLong()
                        } else {
                            positionMs
                        }
                        Text(
                            text = formatDuration(shownPosition),
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = VennelaTokens.caption,
                            fontWeight = if (scrubFraction.value >= 0f) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = formatDuration(durationMs),
                            color = palette.textSecondary,
                            fontFamily = VennelaFonts.Mono,
                            fontSize = VennelaTokens.caption
                        )
                    }
                }
            }

            // ---- Transport on a glass lozenge ----
            item {
                TransportLozenge(
                    isPlaying = isPlaying,
                    shuffle = shuffle,
                    repeatOne = repeatOne,
                    onShuffle = { shuffle = !shuffle },
                    onPrevious = { previousIndex().takeIf { it >= 0 }?.let { playAt(it) } },
                    onPlayPause = { viewModel.toggleMediaPlayback() },
                    onNext = { nextIndex().takeIf { it >= 0 }?.let { playAt(it) } },
                    onRepeat = {
                        repeatOne = !repeatOne
                        viewModel.setMediaRepeat(repeatOne)
                    },
                    onOpenQueue = { showQueue = true }
                )
            }

            // ---- Playback speed ----
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                    Text(
                        text = "Speed",
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LazyRow(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceSm),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                    contentPadding = PaddingValues(end = VennelaTokens.spaceSm)
                ) {
                    items(audioSpeeds, key = { it.toString() }) { candidate ->
                        val label = if (candidate == candidate.toInt().toFloat()) {
                            "${candidate.toInt()}×"
                        } else {
                            "${candidate}×"
                        }
                        LiquidChip(
                            text = label,
                            isActive = mediaState.speed == candidate,
                            onClick = { viewModel.setMediaSpeed(candidate) }
                        )
                    }
                }
            }

            // ---- Sleep timer ----
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(VennelaTokens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
                    Text(
                        text = if (sleepRemainingSeconds > 0) {
                            "Sleep in ${formatDuration(sleepRemainingSeconds * 1000L)}"
                        } else {
                            "Sleep timer"
                        },
                        color = if (sleepRemainingSeconds > 0) accent.secondary else palette.textSecondary,
                        fontFamily = if (sleepRemainingSeconds > 0) VennelaFonts.Mono else VennelaFonts.Sans,
                        fontSize = VennelaTokens.micro,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LazyRow(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = VennelaTokens.spaceSm),
                    horizontalArrangement = Arrangement.spacedBy(VennelaTokens.spaceSm),
                    contentPadding = PaddingValues(end = VennelaTokens.spaceSm)
                ) {
                    items(sleepPresets, key = { it.toString() }) { minutes ->
                        LiquidChip(
                            text = "$minutes min",
                            icon = Icons.Default.NightlightRound,
                            isActive = sleepMinutes == minutes && sleepRemainingSeconds > 0,
                            onClick = {
                                if (sleepMinutes == minutes && sleepRemainingSeconds > 0) {
                                    sleepMinutes = 0
                                    sleepRemainingSeconds = 0
                                } else {
                                    sleepMinutes = minutes
                                    sleepRemainingSeconds = minutes * 60
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(VennelaTokens.spaceLg))
                LiquidButton(
                    label = if (queue.isEmpty()) "No queue in this folder" else "Queue • ${queue.size} tracks",
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    enabled = queue.isNotEmpty(),
                    modifier = Modifier.widthIn(max = 640.dp).testTag("audio_queue_cta_button"),
                    onClick = { showQueue = true }
                )
                Spacer(modifier = Modifier.height(VennelaTokens.spaceSm))
            }
        }
        // ---- Queue sheet ----
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(animationSpec = LiquidMotion.liquid()) { it } +
                fadeIn(animationSpec = LiquidMotion.glide(LiquidMotion.durationFast)),
            exit = slideOutVertically(animationSpec = LiquidMotion.exit()) { it } +
                fadeOut(animationSpec = LiquidMotion.exit()),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            QueueSheet(
                queue = queue,
                currentFileId = file?.id,
                currentIndex = currentIndex,
                isPlaying = isPlaying,
                bottomInset = insets.calculateBottomPadding(),
                onTrackClick = { index -> playAt(index) },
                onDismiss = { showQueue = false }
            )
        }
        }
        }
}

/* ------------------------------------------------------------------------- */
/* Album medallion                                                            */
/* ------------------------------------------------------------------------- */

/**
 * The frosted album-art medallion: a large square of real cover art floating on
 * glass, with the previous and next covers peeking in behind its shoulders.
 * Dragging horizontally moves through the queue.
 */
@Composable
private fun AlbumMedallion(
    artwork: ByteArray?,
    previousArtwork: ByteArray?,
    nextArtwork: ByteArray?,
    isPlaying: Boolean,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current

    // A slow breathing glow while audio is actually playing.
    val transition = rememberInfiniteTransition(label = "albumPulse")
    val glow by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "albumGlow"
    )
    val effectiveGlow = if (isPlaying) glow else 0.28f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(268.dp)
            .pointerInput(Unit) {
                var accumulated = 0f
                detectDragGestures(
                    onDragEnd = {
                        when {
                            accumulated <= -70f -> onSwipeNext()
                            accumulated >= 70f -> onSwipePrevious()
                        }
                        accumulated = 0f
                    },
                    onDragCancel = { accumulated = 0f }
                ) { _, dragAmount ->
                    accumulated += dragAmount.x
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Shoulders: previous (left) and next (right) covers, dimmed behind glass.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ArtworkTile(
                artwork = previousArtwork,
                size = 118.dp,
                corner = VennelaTokens.radiusCardLarge,
                alpha = 0.5f,
                contentDescription = "Previous track artwork"
            )
            Spacer(modifier = Modifier.width(VennelaTokens.spaceSm))
            ArtworkTile(
                artwork = nextArtwork,
                size = 118.dp,
                corner = VennelaTokens.radiusCardLarge,
                alpha = 0.5f,
                contentDescription = "Next track artwork"
            )
        }

        // Hero cover, front and centre.
        Box(
            modifier = Modifier
                .size(236.dp)
                .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.secondary.copy(alpha = effectiveGlow),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    1.5.dp,
                    accent.secondary.copy(alpha = 0.55f),
                    RoundedCornerShape(VennelaTokens.radiusCardLarge)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = artwork,
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.bg_lunar_lake),
                    contentDescription = "Vennela lunar artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                )
                // Frosted veil so the placeholder reads as glass, not a photo.
                GlassPanel(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(VennelaTokens.radiusCardLarge),
                    alpha = 0.30f
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = palette.textPrimary.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                    )
                }
            }

            // Specular sheen across the top of the artwork.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(VennelaTokens.radiusCardLarge))
                    .background(
                        Brush.verticalGradient(
                            0f to palette.textPrimary.copy(alpha = 0.16f),
                            0.35f to Color.Transparent,
                            1f to palette.backgroundBottom.copy(alpha = 0.18f)
                        )
                    )
            )
        }
    }
}

@Composable
private fun ArtworkTile(
    artwork: ByteArray?,
    size: androidx.compose.ui.unit.Dp,
    corner: androidx.compose.ui.unit.Dp,
    alpha: Float,
    contentDescription: String
) {
    val accent = LocalVennelaAccent.current
    Box(
        modifier = Modifier
            .size(size)
            // Neon halo so the artwork reads as the hero object on the screen.
            .shadow(
                elevation = 26.dp,
                shape = RoundedCornerShape(corner),
                clip = false,
                ambientColor = accent.secondary.copy(alpha = 0.55f),
                spotColor = accent.primary.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(corner))
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        accent.secondary.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.30f),
                        accent.primary.copy(alpha = 0.70f)
                    )
                ),
                RoundedCornerShape(corner)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                alpha = alpha,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.34f),
                                accent.secondary.copy(alpha = 0.20f)
                            )
                        )
                    )
            )
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/* Transport                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun TransportLozenge(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatOne: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val spec = liquidGlassSpec(alpha = 0.44f, frost = VennelaTokens.frostRegular, elevation = VennelaTokens.elevationCard)
    val activeTint by animateColorAsState(
        targetValue = accent.secondary,
        animationSpec = tween(LiquidMotion.durationFast),
        label = "transportActive"
    )

    Row(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .padding(top = VennelaTokens.spaceLg)
            .liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusPill))
            .padding(horizontal = VennelaTokens.spaceSm, vertical = VennelaTokens.spaceSm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LozengeIcon(
            icon = Icons.Default.Shuffle,
            contentDescription = if (shuffle) "Shuffle on" else "Shuffle off",
            tint = if (shuffle) activeTint else palette.textTertiary,
            modifier = Modifier.testTag("audio_shuffle_button"),
            onClick = onShuffle
        )
        LozengeIcon(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous track",
            tint = palette.textPrimary,
            iconSize = 30.dp,
            modifier = Modifier.testTag("audio_prev_button"),
            onClick = onPrevious
        )

        // Primary play / pause, the brightest object on the screen.
        val playScale by animateFloatAsState(
            targetValue = if (isPlaying) 1.06f else 1f,
            animationSpec = LiquidMotion.hero(),
            label = "playScale"
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .testTag("audio_play_pause_button")
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = accent.secondary.copy(alpha = 0.85f),
                    spotColor = accent.primary.copy(alpha = 0.85f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accent.secondary, accent.primary)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .pressScale(pressedScale = 0.92f, onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        LozengeIcon(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next track",
            tint = palette.textPrimary,
            iconSize = 30.dp,
            modifier = Modifier.testTag("audio_next_button"),
            onClick = onNext
        )
        LozengeIcon(
            icon = if (repeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = if (repeatOne) "Repeat one on" else "Repeat off",
            tint = if (repeatOne) activeTint else palette.textTertiary,
            modifier = Modifier.testTag("audio_repeat_button"),
            onClick = onRepeat
        )
        LozengeIcon(
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            contentDescription = "Open queue",
            tint = palette.textSecondary,
            modifier = Modifier.testTag("audio_queue_button"),
            onClick = onOpenQueue
        )
    }
}

@Composable
private fun LozengeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp = VennelaTokens.iconLg,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(VennelaTokens.touchTargetMin)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ------------------------------------------------------------------------- */
/* Queue sheet                                                                */
/* ------------------------------------------------------------------------- */

@Composable
private fun QueueSheet(
    queue: List<FileItem>,
    currentFileId: String?,
    currentIndex: Int,
    isPlaying: Boolean,
    bottomInset: androidx.compose.ui.unit.Dp,
    onTrackClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalVennelaPalette.current
    val accent = LocalVennelaAccent.current
    val (totalBytes, totalDurationKnown) = remember(queue) {
        queue.fold(0L to false) { acc, item ->
            (acc.first + item.sizeBytes) to (acc.second || item.sizeBytes > 0)
        }
    }

    GlassPanel(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .padding(horizontal = VennelaTokens.spaceSm),
        shape = RoundedCornerShape(VennelaTokens.radiusSheet),
        frost = VennelaTokens.frostHeavy,
        alpha = 0.52f
    ) {
        Column(modifier = Modifier.padding(bottom = bottomInset + VennelaTokens.spaceSm)) {
            // Grab handle
            Box(
                modifier = Modifier
                    .padding(top = VennelaTokens.spaceMd)
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(VennelaTokens.radiusPill))
                    .background(palette.textTertiary.copy(alpha = 0.45f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = VennelaTokens.spaceLg,
                        end = VennelaTokens.spaceSm,
                        top = VennelaTokens.spaceMd,
                        bottom = VennelaTokens.spaceSm
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Queue",
                        color = palette.textPrimary,
                        fontSize = VennelaTokens.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append("${queue.size} tracks")
                            if (totalDurationKnown && totalBytes > 0) {
                                append(" • ${formatBytes(totalBytes)} on disk")
                            }
                            if (currentIndex >= 0) append(" • playing ${currentIndex + 1}")
                        },
                        color = palette.textSecondary,
                        fontSize = VennelaTokens.micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(VennelaTokens.badgeSm)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close queue",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(VennelaTokens.iconMd)
                    )
                }
            }

            if (queue.isEmpty()) {
                VennelaEmptyState(
                    title = "Queue is empty",
                    hint = "Open this track from a folder so its neighbours can be queued.",
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(
                        start = VennelaTokens.spaceSm,
                        end = VennelaTokens.spaceSm,
                        bottom = VennelaTokens.spaceLg
                    ),
                    verticalArrangement = Arrangement.spacedBy(VennelaTokens.spaceXxs)
                ) {
                    itemsIndexed(queue, key = { _, item -> item.id }) { index, track ->
                        val active = track.id == currentFileId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(index)
                                .clip(RoundedCornerShape(VennelaTokens.radiusControl))
                                .background(
                                    if (active) accent.primary.copy(alpha = 0.24f) else Color.Transparent
                                )
                                .clickable { onTrackClick(index) }
                                .padding(
                                    horizontal = VennelaTokens.spaceMd,
                                    vertical = VennelaTokens.spaceSm
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(VennelaTokens.badgeSm)
                                    .clip(RoundedCornerShape(VennelaTokens.radiusChip))
                                    .background(accent.primary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        active && isPlaying -> Icons.Default.GraphicEq
                                        active -> Icons.Default.Pause
                                        else -> Icons.Default.MusicNote
                                    },
                                    contentDescription = null,
                                    tint = if (active) accent.secondary else palette.textSecondary,
                                    modifier = Modifier.size(VennelaTokens.iconMd)
                                )
                            }

                            Spacer(modifier = Modifier.width(VennelaTokens.spaceMd))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    color = if (active) palette.textPrimary else palette.textPrimary,
                                    fontSize = VennelaTokens.bodySmall,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append(track.extension.uppercase())
                                        if (track.sizeBytes > 0) append(" • ${track.formattedSize}")
                                        if (active) append(" • now playing")
                                    },
                                    color = if (active) accent.secondary else palette.textSecondary,
                                    fontSize = VennelaTokens.micro,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}

/* ------------------------------------------------------------------------- */
/* Waveform seek bar                                                          */
/* ------------------------------------------------------------------------- */

/**
 * A scrubbable waveform. Bar heights are a fixed decorative envelope — only the
 * played/idle split comes from real playback position, so nothing here pretends
 * to be sampled audio data. Dragging scrubs; releasing commits the seek.
 */
/**
 * Drag-to-scrub behaviour for a full-width horizontal control. The live fraction
 * is kept in a snapshot state so the pointer coroutine always reads the newest
 * value when the finger lifts, and never a stale capture.
 */
private fun Modifier.scrubbable(
    onScrub: (Float) -> Unit,
    onCommit: (Float) -> Unit
): Modifier = composed {
    var widthPx by remember { mutableFloatStateOf(1f) }
    val latest = remember { mutableFloatStateOf(0f) }

    this
        .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    latest.value = fraction
                    onScrub(fraction)
                },
                onDragEnd = { onCommit(latest.value) },
                onDragCancel = { onCommit(latest.value) }
            ) { change, _ ->
                change.consume()
                val fraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                latest.value = fraction
                onScrub(fraction)
            }
        }
}

@Composable
private fun WaveformSeekBar(
    progress: Float,
    playedColor: Color,
    idleColor: Color,
    modifier: Modifier = Modifier
) {
    val barCount = 38
    val heights = remember(barCount) {
        List(barCount) { i ->
            val t = i / (barCount - 1f)
            val envelope = 0.42f + 0.58f * sin(t * PI).toFloat()
            val wobble = 0.5f + 0.5f * sin(i * 1.63f).toFloat()
            val spike = if (i % 9 == 0) 1.18f else 1f
            (8f + 32f * envelope * wobble * spike).toInt().coerceIn(5, 44)
        }
    }
    val clamped = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            heights.forEachIndexed { index, heightDp ->
                val played = (index + 0.5f) / barCount <= clamped
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(heightDp.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (played) playedColor else idleColor)
                )
            }
        }

        // Playhead thumb rides on the waveform so the scrub target is obvious.
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(48.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(LocalVennelaPalette.current.textPrimary)
                    .border(2.dp, playedColor, CircleShape)
            )
        }
    }
}
