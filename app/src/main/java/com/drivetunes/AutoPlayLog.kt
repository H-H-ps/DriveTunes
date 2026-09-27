package com.drivetunes

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Small on-device log of auto-play attempts (Bluetooth connect -> playback start).
 * Kept short and local so the user can open Settings and see why auto-play did or
 * didn't fire the last few times, e.g. on a car unit where it sometimes fails silently.
 */
object AutoPlayLog {
    private const val MAX_ENTRIES = 40
    // Prefix marking a line as an actual failure, so it can be filtered out for the
    // "email errors" button without touching the routine/informational lines.
    private const val ERROR_TAG = "[خطأ] "
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    private fun sp(c: Context) = c.getSharedPreferences("autoplay_log", Context.MODE_PRIVATE)

    fun add(c: Context, message: String) = addLine(c, message)

    /** Same as [add], but marked so it shows up in [errorEntries] and the "email errors" export. */
    fun addError(c: Context, message: String) = addLine(c, "$ERROR_TAG$message")

    private fun addLine(c: Context, message: String) {
        val line = "${fmt.format(System.currentTimeMillis())}  $message"
        val updated = (listOf(line) + entries(c)).take(MAX_ENTRIES)
        sp(c).edit().putString("lines", updated.joinToString("\n")).apply()
    }

    fun entries(c: Context): List<String> {
        val raw = sp(c).getString("lines", null) ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    /** Only the entries marked as real failures (not routine/informational lines). */
    fun errorEntries(c: Context): List<String> = entries(c).filter { ERROR_TAG in it }

    fun clear(c: Context) = sp(c).edit().remove("lines").apply()
}
