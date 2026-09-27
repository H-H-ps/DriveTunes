package com.drivetunes

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Per-song "favorite" and "excluded from random" flags, stored as MediaStore-id sets.
 * Shared between the UI and the playback service (same process), like [PlayStats].
 */
object Favorites {
    private var sp: SharedPreferences? = null
    val favorites = MutableStateFlow<Set<Long>>(emptySet())
    val excluded = MutableStateFlow<Set<Long>>(emptySet())

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences("song_flags", Context.MODE_PRIVATE)
        sp = p
        favorites.value = readIds(p, "favorites")
        excluded.value = readIds(p, "excluded")
    }

    private fun readIds(p: SharedPreferences, key: String): Set<Long> =
        (p.getStringSet(key, emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()

    fun toggleFavorite(id: Long) {
        val n = if (id in favorites.value) favorites.value - id else favorites.value + id
        favorites.value = n
        sp?.edit()?.putStringSet("favorites", n.map { it.toString() }.toSet())?.apply()
    }

    fun toggleExcluded(id: Long) {
        val n = if (id in excluded.value) excluded.value - id else excluded.value + id
        excluded.value = n
        sp?.edit()?.putStringSet("excluded", n.map { it.toString() }.toSet())?.apply()
    }
}
