package com.drivetunes

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun sortLabel(m: SortMode): String = stringResource(
    when (m) {
        SortMode.MOST_PLAYED -> R.string.sort_most_played
        SortMode.TITLE_AZ -> R.string.sort_title_az
        SortMode.TITLE_ZA -> R.string.sort_title_za
        SortMode.ARTIST -> R.string.sort_artist
        SortMode.NEWEST -> R.string.sort_newest
        SortMode.OLDEST -> R.string.sort_oldest
        SortMode.DURATION -> R.string.sort_duration
    }
)

@Composable
fun LibraryScreen(
    vm: MainViewModel,
    conn: PlayerConnection,
    songs: List<Song>,
    counts: Map<Long, Int>,
    favorites: Set<Long>,
    excluded: Set<Long>,
    hasAudio: Boolean,
    onRequestPermission: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var sortMenu by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val shown = remember(songs, favoritesOnly, favorites) {
        if (favoritesOnly) songs.filter { it.id in favorites } else songs
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1035), Bg)))
            .systemBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.app_name),
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Violet, Pink, Cyan)),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            }
        }

        TextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint), color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
            trailingIcon = {
                if (vm.query.isNotEmpty()) {
                    IconButton(onClick = { vm.query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Muted)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PanelBg,
                unfocusedContainerColor = PanelBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Pink,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PanelBg)
                        .clickable { sortMenu = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Ic.Sort, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sortLabel(vm.sort), color = Color.White, fontSize = 13.sp)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortMode.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(m)) },
                            onClick = {
                                vm.updateSort(m)
                                sortMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.songs_count, shown.size), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Violet, Pink)))
                    .clickable { conn.playShuffled(shown) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Ic.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (favoritesOnly) Brush.linearGradient(listOf(Violet, Pink)) else Brush.linearGradient(listOf(PanelBg, PanelBg)))
                    .clickable { favoritesOnly = !favoritesOnly }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Ic.Star,
                    contentDescription = null,
                    tint = if (favoritesOnly) Color.White else Muted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.favorites_only),
                    color = if (favoritesOnly) Color.White else Muted,
                    fontSize = 13.sp
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !hasAudio -> PermissionPrompt(onRequestPermission)
                shown.isEmpty() -> Text(
                    stringResource(if (favoritesOnly) R.string.no_favorites else R.string.no_songs),
                    color = Muted,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(shown, key = { _, s -> s.id }) { index, song ->
                        SongRow(
                            song = song,
                            plays = counts[song.id] ?: 0,
                            current = song.id.toString() == conn.mediaId,
                            isFavorite = song.id in favorites,
                            isExcluded = song.id in excluded,
                            onToggleFavorite = { Favorites.toggleFavorite(song.id) },
                            onToggleExcluded = { Favorites.toggleExcluded(song.id) },
                            onClick = {
                                if (song.id.toString() == conn.mediaId) {
                                    onOpenPlayer()
                                } else {
                                    conn.playQueue(shown, index)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (conn.mediaId != null) MiniPlayer(conn, onOpenPlayer)
    }
}

@Composable
private fun SongRow(
    song: Song,
    plays: Int,
    current: Boolean,
    isFavorite: Boolean,
    isExcluded: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleExcluded: () -> Unit,
    onClick: () -> Unit
) {
    val unknown = stringResource(R.string.unknown_artist)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtImage(id = song.id, px = 128, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (current) Pink else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artist = song.artist.ifEmpty { unknown }
            Text(
                if (plays > 0) "$artist  ·  ${stringResource(R.string.plays, plays)}" else artist,
                color = Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleExcluded),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Ic.Block,
                contentDescription = null,
                tint = if (isExcluded) Pink else Muted.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Ic.Star,
                contentDescription = null,
                tint = if (isFavorite) Color(0xFFFFC107) else Muted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MiniPlayer(conn: PlayerConnection, onOpen: () -> Unit) {
    val fraction = if (conn.duration > 0) (conn.position.toFloat() / conn.duration).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .background(PanelBg)
            .clickable(onClick = onOpen)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Violet, Pink, Cyan)))
            )
        }
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtImage(
                id = conn.mediaId?.toLongOrNull(),
                px = 128,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    conn.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(conn.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { conn.toggle() }) {
                Icon(if (conn.isPlaying) Ic.Pause else Ic.Play, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = { conn.next() }) {
                Icon(Ic.Next, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.perm_text), color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.perm_button)) }
        TextButton(onClick = {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
            )
        }) { Text(stringResource(R.string.perm_settings)) }
    }
}
