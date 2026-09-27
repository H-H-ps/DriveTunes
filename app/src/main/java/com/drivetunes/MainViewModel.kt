package com.drivetunes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var query by mutableStateOf("")
    var sort by mutableStateOf(Prefs.sort(app))
        private set
    var folders by mutableStateOf(Prefs.folders(app))
        private set
    var source by mutableStateOf(Prefs.librarySource(app))
        private set
    var autoPlay by mutableStateOf(Prefs.autoPlay(app))
        private set
    var autoPlayMode by mutableStateOf(Prefs.autoPlayMode(app))
        private set
    var delaySec by mutableIntStateOf(Prefs.delay(app))
        private set
    var swipeRandom by mutableStateOf(Prefs.swipeRandom(app))
        private set
    var devices by mutableStateOf(Prefs.devices(app))
        private set

    private val ctx: Application get() = getApplication<Application>()

    fun reload() {
        val f = if (source == LibrarySource.FOLDER) folders else emptySet()
        viewModelScope.launch {
            songs = withContext(Dispatchers.IO) { Library.load(ctx, f) }
        }
    }

    fun updateSort(m: SortMode) {
        sort = m
        Prefs.setSort(ctx, m)
    }

    /** Toggles one folder in/out of the selection (multi-select). */
    fun toggleFolder(path: String) {
        folders = if (path in folders) folders - path else folders + path
        Prefs.setFolders(ctx, folders)
        reload()
    }

    /** Clears the folder selection (falls back to "all folders"). */
    fun clearFolders() {
        folders = emptySet()
        Prefs.setFolders(ctx, folders)
        reload()
    }

    fun updateSource(v: LibrarySource) {
        source = v
        Prefs.setLibrarySource(ctx, v)
        reload()
    }

    fun updateAutoPlay(v: Boolean) {
        autoPlay = v
        Prefs.setAutoPlay(ctx, v)
    }

    fun updateAutoPlayMode(v: AutoPlayMode) {
        autoPlayMode = v
        Prefs.setAutoPlayMode(ctx, v)
    }

    fun updateDelay(v: Int) {
        delaySec = v
        Prefs.setDelay(ctx, v)
    }

    fun updateSwipeRandom(v: Boolean) {
        swipeRandom = v
        Prefs.setSwipeRandom(ctx, v)
    }

    fun toggleDevice(address: String) {
        val n = if (address in devices) devices - address else devices + address
        devices = n
        Prefs.setDevices(ctx, n)
    }
}
