package com.drivetunes

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size as AndroidSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale

val Bg = Color(0xFF0B0B14)
val PanelBg = Color(0xFF17172B)
val Violet = Color(0xFF8B5CF6)
val Pink = Color(0xFFEC4899)
val Cyan = Color(0xFF22D3EE)
val Muted = Color(0xFF9CA3C7)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Pink,
            background = Bg,
            surface = PanelBg,
            onSurface = Color.White,
            onBackground = Color.White
        ),
        content = content
    )
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

// ---------- Icons (Material paths, so we don't need the huge icons-extended library) ----------

private fun vec(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()

object Ic {
    val Play = vec("play", "M8 5v14l11-7z")
    val Pause = vec("pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z")
    val Next = vec("next", "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z")
    val Prev = vec("prev", "M6 6h2v12H6zm3.5 6l8.5 6V6z")
    val Shuffle = vec(
        "shuffle",
        "M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"
    )
    val Repeat = vec("repeat", "M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z")
    val RepeatOne = vec(
        "repeat_one",
        "M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4zm-4-2V9h-1l-2 1v1h1.5v4H13z"
    )
    val Sort = vec("sort", "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z")
    val Note = vec("note", "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z")
    val Star = vec(
        "star",
        "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"
    )
    val Block = vec(
        "block",
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM4 12c0-4.42 3.58-8 8-8 1.85 0 3.55.63 4.9 1.69L5.69 16.9C4.63 15.55 4 13.85 4 12zm8 8c-1.85 0-3.55-.63-4.9-1.69L18.31 7.1C19.37 8.45 20 10.15 20 12c0 4.42-3.58 8-8 8z"
    )

    // About screen
    val Chat = vec(
        "chat",
        "M20 2H4C2.9 2 2 2.9 2 4v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"
    )
    val Copy = vec(
        "copy",
        "M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8C6.9 5 6 5.9 6 7v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
    )
    val Check = vec("check", "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    val Heart = vec(
        "heart",
        "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
    )
    val Shield = vec("shield", "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z")
    val Code = vec(
        "code",
        "M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"
    )
    val ExpandMore = vec("expand_more", "M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z")
    val ExpandLess = vec("expand_less", "M12 8l-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z")
    val OpenNew = vec(
        "open_new",
        "M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"
    )
    val Flag = vec("flag", "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z")
    val Person = vec(
        "person",
        "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
    )
    val Bulb = vec(
        "bulb",
        "M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"
    )
    val Mail = vec(
        "mail",
        "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"
    )
}

// ---------- Album art ----------

object ArtCache {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val missing: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun load(ctx: Context, id: Long, px: Int): ImageBitmap? {
        val key = "$id:$px"
        cache.get(key)?.let { return it }
        if (key in missing) return null
        val bmp = try {
            ctx.contentResolver.loadThumbnail(
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                AndroidSize(px, px),
                null
            ).asImageBitmap()
        } catch (e: Exception) {
            null
        }
        if (bmp != null) cache.put(key, bmp) else missing.add(key)
        return bmp
    }
}

@Composable
fun ArtImage(id: Long?, px: Int, shape: Shape, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val art by produceState<ImageBitmap?>(initialValue = null, id, px) {
        value = if (id == null) null else withContext(Dispatchers.IO) { ArtCache.load(ctx, id, px) }
    }
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Violet, Pink)))
    ) {
        val a = art
        if (a != null) {
            Image(
                bitmap = a,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Ic.Note,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.5f)
            )
        }
    }
}

// ---------- The eye-catching progress bar ----------

@Composable
fun FancySeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val fraction = when {
        dragging -> dragFraction
        duration > 0 -> (position.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }
    val transition = rememberInfiniteTransition(label = "glow")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(duration) {
                detectTapGestures(onTap = { o ->
                    if (duration > 0) onSeek((o.x / size.width * duration).toLong().coerceIn(0L, duration))
                })
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        dragFraction = (o.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragging = false
                        if (duration > 0) onSeek((dragFraction * duration).toLong())
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val h = 10.dp.toPx()
        val cy = size.height / 2
        val w = size.width * fraction
        val brush = Brush.horizontalGradient(listOf(Violet, Pink, Cyan), 0f, maxOf(w, 1f))

        // track
        drawRoundRect(
            Color.White.copy(alpha = 0.12f),
            Offset(0f, cy - h / 2),
            Size(size.width, h),
            CornerRadius(h / 2)
        )
        // soft glow under the progress
        drawRoundRect(
            brush,
            Offset(0f, cy - h * 0.9f),
            Size(w, h * 1.8f),
            CornerRadius(h),
            alpha = 0.35f * pulse
        )
        // progress
        drawRoundRect(brush, Offset(0f, cy - h / 2), Size(w, h), CornerRadius(h / 2))
        // thumb
        drawCircle(Color.White.copy(alpha = 0.28f * pulse), 17.dp.toPx(), Offset(w, cy))
        drawCircle(Color.White, 9.dp.toPx(), Offset(w, cy))
        drawCircle(Pink, 4.5.dp.toPx(), Offset(w, cy))
    }
}
