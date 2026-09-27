package com.drivetunes

import android.content.Context

/**
 * How auto-play picks what to play when a chosen Bluetooth device connects.
 * Ordinals of the original three values are kept stable so existing saved prefs don't shift;
 * HOME_ORDER reuses FROM_START's old slot (its meaning changed from A-Z to the home screen's
 * current sort), and LAST_FROM_START is a new mode appended at the end.
 */
enum class AutoPlayMode { RANDOM, RESUME_LAST, HOME_ORDER, LAST_FROM_START }

/** Order used to (re)build the queue for normal, manual playback (i.e. not the Bluetooth auto-play above). */
enum class DefaultOrder { SEQUENTIAL, RANDOM }

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun autoPlay(c: Context): Boolean = sp(c).getBoolean("auto", true)
    fun setAutoPlay(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto", v).apply()

    fun autoPlayMode(c: Context): AutoPlayMode =
        AutoPlayMode.entries.getOrNull(sp(c).getInt("auto_mode", 0)) ?: AutoPlayMode.RANDOM
    fun setAutoPlayMode(c: Context, v: AutoPlayMode) = sp(c).edit().putInt("auto_mode", v.ordinal).apply()

    /** Last song played and its position, kept up to date while the service is alive so RESUME_LAST can pick up where it left off. */
    fun lastSongId(c: Context): Long? = sp(c).getLong("last_song_id", -1L).takeIf { it >= 0 }
    fun lastPosition(c: Context): Long = sp(c).getLong("last_position", 0L)
    fun setLastPlayback(c: Context, songId: Long, positionMs: Long) =
        sp(c).edit().putLong("last_song_id", songId).putLong("last_position", positionMs).apply()

    fun devices(c: Context): Set<String> = sp(c).getStringSet("devices", emptySet()) ?: emptySet()
    fun setDevices(c: Context, v: Set<String>) = sp(c).edit().putStringSet("devices", HashSet(v)).apply()

    fun delay(c: Context): Int = sp(c).getInt("delay", 2)
    fun setDelay(c: Context, v: Int) = sp(c).edit().putInt("delay", v).apply()

    /** Chosen folders (MediaStore RELATIVE_PATH values); empty set means no folder filter (all folders). */
    fun folders(c: Context): Set<String> {
        sp(c).getStringSet("folders", null)?.let { return it }
        // Migrate the old single-folder pref the first time this is read after an update.
        val legacy = sp(c).getString("folder", null)
        return if (legacy != null) setOf(legacy) else emptySet()
    }
    fun setFolders(c: Context, v: Set<String>) =
        sp(c).edit().putStringSet("folders", HashSet(v)).remove("folder").apply()

    fun librarySource(c: Context): LibrarySource =
        LibrarySource.entries.getOrNull(sp(c).getInt("lib_source", 0)) ?: LibrarySource.ALL
    fun setLibrarySource(c: Context, v: LibrarySource) = sp(c).edit().putInt("lib_source", v.ordinal).apply()

    /** Cached "now playing" snapshot so the home-screen widget has something to show without binding to the service. */
    fun nowTitle(c: Context): String = sp(c).getString("now_title", "") ?: ""
    fun nowArtist(c: Context): String = sp(c).getString("now_artist", "") ?: ""
    fun nowPlaying(c: Context): Boolean = sp(c).getBoolean("now_playing", false)
    fun setNowPlaying(c: Context, title: String, artist: String, playing: Boolean) = sp(c).edit()
        .putString("now_title", title)
        .putString("now_artist", artist)
        .putBoolean("now_playing", playing)
        .apply()

    fun swipeRandom(c: Context): Boolean = sp(c).getBoolean("swipe_random", false)
    fun setSwipeRandom(c: Context, v: Boolean) = sp(c).edit().putBoolean("swipe_random", v).apply()

    fun sort(c: Context): SortMode =
        SortMode.entries.getOrNull(sp(c).getInt("sort", 0)) ?: SortMode.MOST_PLAYED

    fun setSort(c: Context, v: SortMode) = sp(c).edit().putInt("sort", v.ordinal).apply()

    /** Order for a freshly started manual queue (tapping a song) when no Bluetooth auto-play is involved. */
    fun defaultOrder(c: Context): DefaultOrder =
        DefaultOrder.entries.getOrNull(sp(c).getInt("default_order", 0)) ?: DefaultOrder.SEQUENTIAL
    fun setDefaultOrder(c: Context, v: DefaultOrder) = sp(c).edit().putInt("default_order", v.ordinal).apply()
}
