package com.drivetunes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay

enum class Screen { LIBRARY, PLAYER, SETTINGS, ABOUT }

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AUTO_PLAY = "auto_play"
    }

    private val vm: MainViewModel by viewModels()
    private lateinit var conn: PlayerConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        conn = PlayerConnection(applicationContext)
        conn.connect()
        handleIntent(intent)
        setContent {
            AppTheme {
                Root(vm, conn)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Opened from the "tap to play" notification (fallback when the background start was blocked). */
    private fun handleIntent(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_AUTO_PLAY, false) == true) {
            i.removeExtra(EXTRA_AUTO_PLAY)
            ContextCompat.startForegroundService(
                this,
                Intent(this, PlaybackService::class.java).setAction(PlaybackService.ACTION_AUTO_PLAY)
            )
        }
    }

    override fun onDestroy() {
        conn.release()
        super.onDestroy()
    }
}

private fun hasAudioPermission(ctx: Context): Boolean {
    val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
}

private fun missingPermissions(ctx: Context): Array<String> {
    val wanted = ArrayList<String>()
    if (Build.VERSION.SDK_INT >= 33) {
        wanted.add(Manifest.permission.READ_MEDIA_AUDIO)
        wanted.add(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        wanted.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    if (Build.VERSION.SDK_INT >= 31) wanted.add(Manifest.permission.BLUETOOTH_CONNECT)
    return wanted
        .filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
        .toTypedArray()
}

@Composable
fun Root(vm: MainViewModel, conn: PlayerConnection) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(Screen.LIBRARY) }
    var hasAudio by remember { mutableStateOf(hasAudioPermission(ctx)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasAudio = hasAudioPermission(ctx)
        if (hasAudio) vm.reload()
    }
    LaunchedEffect(Unit) {
        val m = missingPermissions(ctx)
        if (m.isNotEmpty()) launcher.launch(m)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAudio = hasAudioPermission(ctx)
        if (hasAudio) vm.reload()
    }

    // Progress bar ticker
    LaunchedEffect(conn.controller, conn.isPlaying) {
        conn.tick()
        while (conn.isPlaying) {
            delay(250)
            conn.tick()
        }
    }

    BackHandler(enabled = screen != Screen.LIBRARY) {
        screen = if (screen == Screen.ABOUT) Screen.SETTINGS else Screen.LIBRARY
    }

    val counts by PlayStats.counts.collectAsState()
    val favorites by Favorites.favorites.collectAsState()
    val excluded by Favorites.excluded.collectAsState()
    val visible = remember(vm.songs, vm.sort, vm.query, counts, vm.source, favorites) {
        val q = vm.query.trim()
        var base = if (q.isEmpty()) vm.songs
        else vm.songs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
        if (vm.source == LibrarySource.FAVORITES) base = base.filter { it.id in favorites }
        Library.sort(base, vm.sort, counts)
    }

    when (screen) {
        Screen.LIBRARY -> LibraryScreen(
            vm = vm,
            conn = conn,
            songs = visible,
            counts = counts,
            favorites = favorites,
            excluded = excluded,
            hasAudio = hasAudio,
            onRequestPermission = {
                val m = missingPermissions(ctx)
                if (m.isNotEmpty()) launcher.launch(m)
            },
            onOpenPlayer = { screen = Screen.PLAYER },
            onOpenSettings = { screen = Screen.SETTINGS }
        )
        Screen.PLAYER -> PlayerScreen(vm, conn, onBack = { screen = Screen.LIBRARY })
        Screen.SETTINGS -> SettingsScreen(
            vm,
            onBack = { screen = Screen.LIBRARY },
            onOpenAbout = { screen = Screen.ABOUT }
        )
        Screen.ABOUT -> AboutScreen(onBack = { screen = Screen.SETTINGS })
    }
}
