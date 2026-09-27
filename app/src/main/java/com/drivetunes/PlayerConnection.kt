package com.drivetunes

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlin.random.Random

/** UI-side handle to the PlaybackService, exposing Compose-observable state. */
class PlayerConnection(private val context: Context) {

    var controller by mutableStateOf<MediaController?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var mediaId by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var artist by mutableStateOf("")
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_ALL)
        private set

    private var future: ListenableFuture<MediaController>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
        }
    }

    fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = try {
                f.get()
            } catch (e: Exception) {
                null
            }
            if (c != null) {
                controller = c
                c.addListener(listener)
                sync(c)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        future = null
    }

    private fun sync(p: Player) {
        isPlaying = p.isPlaying
        val item = p.currentMediaItem
        mediaId = item?.mediaId
        title = item?.mediaMetadata?.title?.toString() ?: ""
        artist = item?.mediaMetadata?.artist?.toString() ?: ""
        shuffle = p.shuffleModeEnabled
        repeat = p.repeatMode
        tick()
    }

    fun tick() {
        val c = controller ?: return
        position = c.currentPosition
        val d = c.duration
        duration = if (d == C.TIME_UNSET) 0L else d
    }

    fun playQueue(songs: List<Song>, index: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        // Applies the user's chosen default order (Settings) to a freshly started queue.
        // Bluetooth auto-play builds and orders its own queue separately in PlaybackService.
        c.shuffleModeEnabled = Prefs.defaultOrder(context) == DefaultOrder.RANDOM
        c.setMediaItems(songs.map { it.toMediaItem() }, index.coerceIn(0, songs.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun playShuffled(songs: List<Song>) {
        val c = controller ?: return
        val pool = songs.filterNot { it.id in Favorites.excluded.value }.ifEmpty { songs }
        if (pool.isEmpty()) return
        c.shuffleModeEnabled = true
        playQueue(pool, Random.nextInt(pool.size))
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun randomJump() {
        val c = controller ?: return
        val n = c.mediaItemCount
        if (n < 2) return
        val excludedIds = Favorites.excluded.value
        val candidates = (0 until n).filter { i ->
            i != c.currentMediaItemIndex &&
                c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { it !in excludedIds } != false
        }
        val i = if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            var j: Int
            do {
                j = Random.nextInt(n)
            } while (j == c.currentMediaItemIndex)
            j
        }
        c.seekTo(i, 0L)
        c.play()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        position = ms
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
}
