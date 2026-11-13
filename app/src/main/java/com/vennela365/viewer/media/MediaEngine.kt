package com.vennela365.viewer.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Live playback state, mirrored into Compose. */
data class MediaUiState(
    val uri: String? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val speed: Float = 1f,
    val error: String? = null,
    val hasVideo: Boolean = false
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** Real, read-only technical metadata read from the container itself. */
data class MediaInfo(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Int = 0,
    val mimeType: String? = null,
    val hasAudio: Boolean = false,
    val hasVideo: Boolean = false,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val frameRate: Float = 0f
) {
    val resolutionLabel: String?
        get() = if (width > 0 && height > 0) "${width}×$height" else null

    val durationLabel: String
        get() = formatDuration(durationMs)

    val bitrateLabel: String?
        get() = if (bitrate > 0) "${bitrate / 1000} kbps" else null

    val channelLabel: String?
        get() = when (channels) {
            1 -> "Mono"
            2 -> "Stereo"
            in 3..16 -> "$channels channels"
            else -> null
        }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Single-instance ExoPlayer wrapper used by the audio and video viewers.
 *
 * Read-only by construction: it only ever opens a content URI for playback and
 * never writes, transcodes or downloads anything.
 */
class MediaEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private var player: ExoPlayer? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val created = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startTicker() else stopTicker()
                    pushProgress()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _state.update {
                        it.copy(
                            isBuffering = playbackState == Player.STATE_BUFFERING,
                            isEnded = playbackState == Player.STATE_ENDED
                        )
                    }
                    pushProgress()
                }

                override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                    _state.update { it.copy(speed = playbackParameters.speed) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            error = error.errorCodeName.ifBlank {
                                error.message ?: "This file could not be decoded on this device"
                            }
                        )
                    }
                }
            })
        }
        player = created
        return created
    }

    /** Loads a real file. Playback only starts when the user taps play. */
    fun open(uri: Uri, title: String, autoPlay: Boolean = false) {
        val exo = ensurePlayer()
        _state.update {
            MediaUiState(uri = uri.toString(), title = title, hasVideo = it.hasVideo)
        }
        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.prepare()
        exo.playWhenReady = autoPlay
    }

    fun setHasVideo(hasVideo: Boolean) {
        _state.update { it.copy(hasVideo = hasVideo) }
    }

    /** The shared ExoPlayer, created on first use. Used as the video surface. */
    fun obtainPlayer(): ExoPlayer = ensurePlayer()

    fun togglePlayPause() {
        val exo = player ?: return
        if (exo.playbackState == Player.STATE_ENDED) {
            exo.seekTo(0)
            exo.playWhenReady = true
            return
        }
        exo.playWhenReady = !exo.playWhenReady
        _state.update { it.copy(isPlaying = exo.playWhenReady) }
    }

    fun play() {
        player?.playWhenReady = true
        _state.update { it.copy(isPlaying = true) }
    }

    fun pause() {
        player?.playWhenReady = false
        _state.update { it.copy(isPlaying = false) }
    }

    fun seekTo(positionMs: Long) {
        val exo = player ?: return
        val target = positionMs.coerceIn(0L, exo.duration.coerceAtLeast(0L))
        exo.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun seekToFraction(fraction: Float) {
        val exo = player ?: return
        val duration = exo.duration
        if (duration > 0) seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun skipBy(deltaMs: Long) {
        val exo = player ?: return
        seekTo((exo.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        val exo = player ?: return
        exo.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))
        _state.update { it.copy(speed = speed.coerceIn(0.25f, 4f)) }
    }

    fun setRepeatMode(enabled: Boolean) {
        player?.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    /** Stops and clears the current media item without tearing the player down. */
    fun stop() {
        stopTicker()
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
        _state.value = MediaUiState()
    }

    fun release() {
        stopTicker()
        player?.release()
        player = null
        _state.value = MediaUiState()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                pushProgress()
                delay(400)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun pushProgress() {
        val exo = player ?: return
        val duration = exo.duration.takeIf { it > 0 } ?: 0L
        _state.update {
            it.copy(
                positionMs = exo.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                bufferedMs = exo.bufferedPosition.coerceAtLeast(0L)
            )
        }
    }

    companion object {
        /** Embedded cover art, if the file actually carries any. */
        suspend fun embeddedArtwork(context: Context, uri: Uri?): ByteArray? =
            withContext(Dispatchers.IO) {
                if (uri == null) return@withContext null
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.embeddedPicture
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }
            }

        /**
         * Reads container metadata directly from the file. Runs off the main
         * thread; failures return an empty [MediaInfo] rather than invented data.
         */
        suspend fun readInfo(context: Context, uri: Uri?, isAudioHint: Boolean): MediaInfo =
            withContext(Dispatchers.IO) {
                if (uri == null) return@withContext MediaInfo()
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    fun meta(key: Int): String? =
                        retriever.extractMetadata(key)?.takeIf { it.isNotBlank() }

                    val duration = meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val hasVideo = meta(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
                    val hasAudio = meta(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes" || isAudioHint
                    MediaInfo(
                        title = meta(MediaMetadataRetriever.METADATA_KEY_TITLE),
                        artist = meta(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: meta(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                        album = meta(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                        durationMs = duration,
                        width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                        height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                        bitrate = meta(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0,
                        mimeType = meta(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                        hasAudio = hasAudio,
                        hasVideo = hasVideo,
                        sampleRate = meta(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0,
                        channels = meta(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.toIntOrNull() ?: 0,
                        frameRate = meta(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 0f
                    )
                } catch (_: Exception) {
                    MediaInfo()
                } finally {
                    runCatching { retriever.release() }
                }
            }
    }
}
