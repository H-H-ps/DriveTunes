package com.drivetunes

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.text.Collator
import java.util.TreeMap

enum class SortMode { MOST_PLAYED, TITLE_AZ, TITLE_ZA, ARTIST, NEWEST, OLDEST, DURATION }

/** Where the library (and auto-play) pulls songs from. */
enum class LibrarySource { ALL, FOLDER, FAVORITES }

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val dateAdded: Long,
    val albumId: Long
) {
    val uri: Uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist.ifEmpty { null })
                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
                .build()
        )
        .build()
}

object Library {
    private val collator: Collator = Collator.getInstance().apply { strength = Collator.SECONDARY }

    /** Loads local songs. When [folders] is non-empty (MediaStore RELATIVE_PATH values like "Music/Arabic/"), only songs under any of those folders (and their sub-folders) are used. */
    fun load(ctx: Context, folders: Set<String>): List<Song> {
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val sel = StringBuilder("${MediaStore.Audio.Media.DURATION} >= 10000")
        val args = ArrayList<String>()
        if (folders.isEmpty()) {
            sel.append(" AND ${MediaStore.Audio.Media.IS_MUSIC} != 0")
        } else {
            sel.append(" AND (")
            folders.forEachIndexed { i, _ ->
                if (i > 0) sel.append(" OR ")
                sel.append("${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?")
            }
            sel.append(")")
            folders.forEach { args.add("$it%") }
        }
        val out = ArrayList<Song>()
        try {
            ctx.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj,
                sel.toString(),
                if (args.isEmpty()) null else args.toTypedArray(),
                null
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (c.moveToNext()) {
                    val rawTitle = c.getString(iTitle)
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle
                    else (c.getString(iName) ?: "").substringBeforeLast('.')
                    val artist = c.getString(iArtist) ?: ""
                    out.add(
                        Song(
                            id = c.getLong(iId),
                            title = title,
                            artist = if (artist == "<unknown>") "" else artist,
                            duration = c.getLong(iDur),
                            dateAdded = c.getLong(iDate),
                            albumId = c.getLong(iAlbum)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // no permission / provider error: return what we have
        }
        return out
    }

    /** All folders that contain audio (with parent folders included), with song counts. */
    fun folders(ctx: Context): List<Pair<String, Int>> {
        val map = TreeMap<String, Int>()
        try {
            ctx.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.RELATIVE_PATH),
                "${MediaStore.Audio.Media.DURATION} >= 10000",
                null,
                null
            )?.use { c ->
                while (c.moveToNext()) {
                    val path = c.getString(0) ?: continue
                    var acc = ""
                    for (part in path.split("/")) {
                        if (part.isEmpty()) continue
                        acc += "$part/"
                        map[acc] = (map[acc] ?: 0) + 1
                    }
                }
            }
        } catch (e: Exception) {
        }
        return map.map { it.key to it.value }
    }

    fun sort(list: List<Song>, mode: SortMode, counts: Map<Long, Int>): List<Song> {
        val byTitle = Comparator<Song> { a, b -> collator.compare(a.title, b.title) }
        return when (mode) {
            SortMode.MOST_PLAYED -> list.sortedWith(Comparator { a, b ->
                val d = (counts[b.id] ?: 0) - (counts[a.id] ?: 0)
                if (d != 0) d else collator.compare(a.title, b.title)
            })
            SortMode.TITLE_AZ -> list.sortedWith(byTitle)
            SortMode.TITLE_ZA -> list.sortedWith(byTitle.reversed())
            SortMode.ARTIST -> list.sortedWith(Comparator { a, b ->
                val d = collator.compare(a.artist, b.artist)
                if (d != 0) d else collator.compare(a.title, b.title)
            })
            SortMode.NEWEST -> list.sortedByDescending { it.dateAdded }
            SortMode.OLDEST -> list.sortedBy { it.dateAdded }
            SortMode.DURATION -> list.sortedByDescending { it.duration }
        }
    }
}
