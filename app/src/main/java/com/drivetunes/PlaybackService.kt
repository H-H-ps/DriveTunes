package com.drivetunes

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_AUTO_PLAY = "com.drivetunes.AUTO_PLAY"
        const val ACTION_TOGGLE = "com.drivetunes.TOGGLE"
        const val ACTION_NEXT = "com.drivetunes.NEXT"
        const val ACTION_PREV = "com.drivetunes.PREV"
        /** Optional Long extra on ACTION_AUTO_PLAY: how long to wait, once already in the
         * foreground, before actually starting playback (lets the car finish switching audio
         * to Bluetooth). Applied here rather than in BtReceiver so the wait happens while the
         * service is protected by startForeground(), not in a killable background receiver. */
        const val EXTRA_DELAY_MS = "delay_ms"
    }

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private var counted: String? = null
    private var tickCount = 0

    private val ticker = object : Runnable {
        override fun run() {
            countIfNeeded()
            saveLastPlaybackIfNeeded()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ALL
        // Starting state for the queue-order setting in Settings; playQueue()/autoPlay() may
        // override this per-queue, but this covers the session's very first playback too.
        player.shuffleModeEnabled = Prefs.defaultOrder(this) == DefaultOrder.RANDOM
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                counted = null
                pushNowPlaying()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) saveLastPlaybackNow()
                pushNowPlaying()
            }
        })

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setCallback(Callback())
            .setSessionActivity(openApp)
            .build()
        handler.postDelayed(ticker, 1000)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val r = super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_AUTO_PLAY -> {
                // Call startForeground() right away so the OS treats this process as a
                // foreground service immediately, then wait the configured delay before
                // actually loading/playing the queue.
                ensureForeground()
                val delayMs = intent.getLongExtra(EXTRA_DELAY_MS, 0L)
                if (delayMs > 0L) {
                    handler.postDelayed({ autoPlay() }, delayMs)
                } else {
                    autoPlay()
                }
            }
            ACTION_TOGGLE -> {
                ensureForeground()
                if (player.mediaItemCount > 0) {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
            ACTION_NEXT -> {
                ensureForeground()
                player.seekToNextMediaItem()
            }
            ACTION_PREV -> {
                ensureForeground()
                player.seekToPreviousMediaItem()
            }
        }
        return r
    }

    private fun autoPlay() {
        ensureForeground()
        if (player.isPlaying) {
            AutoPlayLog.add(this, "أمر تشغيل تلقائي وصل لكن في أغنية شغالة أصلاً، تم تجاهله")
            return
        }
        if (player.mediaItemCount > 0) {
            // The service/session is still alive with a loaded queue: this is a quick
            // Bluetooth disconnect -> reconnect (app wasn't fully closed), so just resume the
            // same song where it stopped instead of rebuilding a fresh/random queue.
            AutoPlayLog.add(this, "استئناف نفس الجلسة (كانت متوقفة، الخدمة لسا شغالة) بدون اختيار أغنية جديدة")
            player.play()
            return
        }
        val src = Prefs.librarySource(this)
        var songs = Library.load(this, if (src == LibrarySource.FOLDER) Prefs.folders(this) else emptySet())
        if (src == LibrarySource.FAVORITES) {
            val favs = Favorites.favorites.value
            songs = songs.filter { it.id in favs }
        }
        if (songs.isEmpty()) {
            AutoPlayLog.addError(this, "ما في أغاني بمصدر المكتبة المختار، تم إلغاء التشغيل التلقائي")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        player.shuffleModeEnabled = false
        player.repeatMode = Player.REPEAT_MODE_ALL

        val mode = Prefs.autoPlayMode(this)
        when (mode) {
            AutoPlayMode.RANDOM -> {
                // Songs marked "exclude from random" are skipped here (but still playable manually).
                val pool = songs.filterNot { it.id in Favorites.excluded.value }.ifEmpty { songs }
                player.setMediaItems(pool.shuffled().map { it.toMediaItem() }, 0, 0L)
            }
            AutoPlayMode.HOME_ORDER -> {
                // Same order the home screen currently shows (whatever sort mode is selected there).
                val ordered = Library.sort(songs, Prefs.sort(this), PlayStats.counts.value)
                player.setMediaItems(ordered.map { it.toMediaItem() }, 0, 0L)
            }
            AutoPlayMode.RESUME_LAST -> {
                val lastId = Prefs.lastSongId(this)
                val idx = lastId?.let { id -> songs.indexOfFirst { it.id == id } } ?: -1
                if (idx == -1) {
                    // No last song saved yet (first run) -> fall back to random.
                    player.setMediaItems(songs.shuffled().map { it.toMediaItem() }, 0, 0L)
                } else {
                    player.setMediaItems(songs.map { it.toMediaItem() }, idx, Prefs.lastPosition(this))
                }
            }
            AutoPlayMode.LAST_FROM_START -> {
                val lastId = Prefs.lastSongId(this)
                val idx = lastId?.let { id -> songs.indexOfFirst { it.id == id } } ?: -1
                if (idx == -1) {
                    // No last song saved yet (first run) -> fall back to random.
                    player.setMediaItems(songs.shuffled().map { it.toMediaItem() }, 0, 0L)
                } else {
                    // Same song as RESUME_LAST, but always restarted from position 0.
                    player.setMediaItems(songs.map { it.toMediaItem() }, idx, 0L)
                }
            }
        }
        AutoPlayLog.add(this, "بدء التشغيل التلقائي (${mode.name}) - ${songs.size} أغنية بمصدر المكتبة")
        player.prepare()
        player.play()
    }

    /** Persists the currently playing song + position every few seconds so RESUME_LAST has something fresh to resume from. */
    private fun saveLastPlaybackIfNeeded() {
        if (!player.isPlaying) return
        tickCount++
        if (tickCount % 5 != 0) return
        saveLastPlaybackNow()
    }

    private fun saveLastPlaybackNow() {
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        Prefs.setLastPlayback(this, id, player.currentPosition)
    }

    /** Pushes current title/artist/playing-state to Prefs and refreshes the home-screen widget. */
    private fun pushNowPlaying() {
        val item = player.currentMediaItem
        Prefs.setNowPlaying(
            this,
            title = item?.mediaMetadata?.title?.toString() ?: "",
            artist = item?.mediaMetadata?.artist?.toString() ?: "",
            playing = player.isPlaying
        )
        PlayerWidget.updateAll(this)
    }

    /** Guarantees we call startForeground() quickly after startForegroundService(). */
    private fun ensureForeground() {
        Notifier.ensureChannels(this)
        val n = NotificationCompat.Builder(this, Notifier.CH_PLAYBACK)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(getString(R.string.app_name))
            .setOngoing(true)
            .build()
        try {
            ServiceCompat.startForeground(
                this,
                DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID,
                n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (e: Exception) {
        }
    }

    /** A song counts as "played" after 30s (or half of it if it is shorter). */
    private fun countIfNeeded() {
        val item = player.currentMediaItem ?: return
        if (!player.isPlaying || counted == item.mediaId) return
        val dur = player.duration
        val threshold = if (dur != C.TIME_UNSET && dur > 0) minOf(30_000L, dur / 2) else 30_000L
        if (player.currentPosition >= threshold) {
            item.mediaId.toLongOrNull()?.let { PlayStats.increment(it) }
            counted = item.mediaId
        }
    }

    override fun onDestroy() {
        saveLastPlaybackNow()
        Prefs.setNowPlaying(this, Prefs.nowTitle(this), Prefs.nowArtist(this), playing = false)
        PlayerWidget.updateAll(this)
        handler.removeCallbacksAndMessages(null)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private inner class Callback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val id = item.mediaId.toLongOrNull()
                if (id != null) {
                    item.buildUpon()
                        .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
                        .build()
                } else item
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }
}
