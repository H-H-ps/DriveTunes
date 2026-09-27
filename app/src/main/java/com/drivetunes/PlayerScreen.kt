package com.drivetunes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(vm: MainViewModel, conn: PlayerConnection, onBack: () -> Unit) {
    val id = conn.mediaId?.toLongOrNull()
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val coverScale by animateFloatAsState(
        targetValue = if (conn.isPlaying) 1f else 0.88f,
        animationSpec = tween(450),
        label = "cover"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3B1D7A), Color(0xFF15102E), Bg)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    stringResource(R.string.now_playing),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(48.dp))
            }

            // Swipe area: cover + title. Left = next, right = previous (or random if enabled in settings)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(vm.swipeRandom) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val x = offsetX.value
                                val threshold = 110.dp.toPx()
                                scope.launch {
                                    if (abs(x) > threshold) {
                                        if (vm.swipeRandom) conn.randomJump()
                                        else if (x < 0) conn.next()
                                        else conn.previous()
                                    }
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                            onHorizontalDrag = { change, dx ->
                                change.consume()
                                scope.launch { offsetX.snapTo(offsetX.value + dx) }
                            }
                        )
                    }
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .graphicsLayer {
                        rotationZ = offsetX.value / 60f
                        alpha = 1f - (abs(offsetX.value) / 1200f).coerceAtMost(0.5f)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtImage(
                    id = id,
                    px = 600,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .scale(coverScale)
                        .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Violet, spotColor = Violet)
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = conn.title.ifEmpty { stringResource(R.string.nothing_playing) },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = conn.artist.ifEmpty { if (id != null) stringResource(R.string.unknown_artist) else "" },
                    color = Muted,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(if (vm.swipeRandom) R.string.hint_swipe_random else R.string.hint_swipe),
                    color = Muted.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            FancySeekBar(
                position = conn.position,
                duration = conn.duration,
                onSeek = { conn.seekTo(it) }
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(fmt(conn.position), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(fmt(conn.duration), color = Muted, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { conn.toggleShuffle() }) {
                    Icon(Ic.Shuffle, null, tint = if (conn.shuffle) Pink else Muted)
                }
                IconButton(onClick = { conn.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Ic.Prev, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    Modifier
                        .size(76.dp)
                        .shadow(16.dp, CircleShape, ambientColor = Pink, spotColor = Pink)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Violet, Pink)))
                        .clickable { conn.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (conn.isPlaying) Ic.Pause else Ic.Play,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { conn.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Ic.Next, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { conn.cycleRepeat() }) {
                    Icon(
                        if (conn.repeat == Player.REPEAT_MODE_ONE) Ic.RepeatOne else Ic.Repeat,
                        null,
                        tint = if (conn.repeat == Player.REPEAT_MODE_OFF) Muted else Cyan
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
