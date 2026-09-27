package com.drivetunes

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/** Play counters, stored per MediaStore id. Shared between the UI and the playback service. */
object PlayStats {
    private var sp: SharedPreferences? = null
    val counts = MutableStateFlow<Map<Long, Int>>(emptyMap())

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences("stats", Context.MODE_PRIVATE)
        sp = p
        val map = HashMap<Long, Int>()
        for ((k, v) in p.all) {
            val id = k.toLongOrNull() ?: continue
            val n = v as? Int ?: continue
            map[id] = n
        }
        counts.value = map
    }

    fun increment(id: Long) {
        val p = sp ?: return
        val n = (counts.value[id] ?: 0) + 1
        p.edit().putInt(id.toString(), n).apply()
        counts.value = counts.value + (id to n)
    }
}
