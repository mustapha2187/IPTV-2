package com.musta.iptv.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.musta.iptv.data.Channel
import com.musta.iptv.data.SourceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PlayStatus {
    object Idle : PlayStatus
    object Loading : PlayStatus
    object Playing : PlayStatus
    data class Error(val detail: String) : PlayStatus
}

enum class AspectMode { FIT, ZOOM, STRETCH }

/**
 * One player for the whole app, so the small preview in the lists and the full-screen view show the same stream
 * and playback never restarts when you move between screens.
 */
object Playback {
    lateinit var player: ExoPlayer
        private set

    val current = MutableStateFlow<Channel?>(null)
    val status = MutableStateFlow<PlayStatus>(PlayStatus.Idle)
    val aspect = MutableStateFlow(16f / 9f)
    val aspectMode = MutableStateFlow(AspectMode.FIT)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var queue: List<Channel> = emptyList()
    private var index = 0
    private var previous: Channel? = null
    private var job: Job? = null

    fun init(context: Context) {
        if (::player.isInitialized) return
        player = ExoPlayer.Builder(context.applicationContext).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) status.value = PlayStatus.Playing
                }

                override fun onPlayerError(error: PlaybackException) {
                    status.value = PlayStatus.Error(error.errorCodeName)
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        aspect.value = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    }
                }
            })
        }
    }

    fun release() {
        job?.cancel()
        if (::player.isInitialized) player.release()
    }

    /** Plays queue[index] and remembers the list so Up / Down can zap through it. */
    fun start(channels: List<Channel>, startIndex: Int) {
        queue = channels
        index = startIndex.coerceIn(0, (channels.size - 1).coerceAtLeast(0))
        playCurrent()
    }

    fun zap(delta: Int) {
        if (queue.isEmpty()) return
        index = ((index + delta) % queue.size + queue.size) % queue.size
        playCurrent()
    }

    fun playPrevious() {
        val target = previous ?: return
        val position = queue.indexOfFirst { it.id == target.id }
        if (position >= 0) {
            index = position
        } else {
            queue = listOf(target)
            index = 0
        }
        playCurrent()
    }

    fun restart() = playCurrent()

    fun stop() {
        job?.cancel()
        player.stop()
        current.value = null
        status.value = PlayStatus.Idle
    }

    fun cycleAspect() {
        aspectMode.value = when (aspectMode.value) {
            AspectMode.FIT -> AspectMode.ZOOM
            AspectMode.ZOOM -> AspectMode.STRETCH
            AspectMode.STRETCH -> AspectMode.FIT
        }
    }

    private fun playCurrent() {
        val channel = queue.getOrNull(index) ?: return
        val old = current.value
        if (old != null && old.id != channel.id) previous = old
        current.value = channel
        status.value = PlayStatus.Loading
        job?.cancel()
        job = scope.launch {
            try {
                val url = withContext(Dispatchers.IO) { SourceManager.resolve(channel) }
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.playWhenReady = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status.value = PlayStatus.Error(e.message ?: "")
            }
        }
    }
}
