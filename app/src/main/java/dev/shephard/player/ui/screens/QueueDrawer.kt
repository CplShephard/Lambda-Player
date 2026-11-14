// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.rememberStaticArtwork
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class QueueEntry(val key: Int, val track: AudioTrack)

@Composable
fun QueueDrawer(playerViewModel: PlayerViewModel, useMiuix: Boolean, onDismiss: () -> Unit) {
    val state by playerViewModel.uiState.collectAsState()
    val isRemixed by playerViewModel.isRemixed.collectAsState()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val text = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val current = state.currentQueueIndex.coerceIn(0, (state.queue.size - 1).coerceAtLeast(0))
    var query by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val entries = remember { mutableStateListOf<QueueEntry>() }
    var drag by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(state.queue, current, history) {
        if (drag == null) {
            entries.clear()
            val start = if (history) 0 else current
            entries.addAll(state.queue.drop(start).mapIndexed { i, track -> QueueEntry(start + i, track) })
        }
    }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        // Search results are filtered and cannot safely be reordered by their visible positions.
        if (query.isBlank() && from.index in entries.indices && to.index in entries.indices) {
            val start = if (history) 0 else current
            val f = from.index + start; val t = to.index + start
            if (f > current && t > current) {
                drag = (drag?.first ?: f) to t
                entries.add(to.index, entries.removeAt(from.index))
            }
        }
    }
    LaunchedEffect(reorder.isAnyItemDragging) {
        if (!reorder.isAnyItemDragging) {
            drag?.let { (from, to) -> drag = null; if (from != to) playerViewModel.moveQueueItem(from, to) }
        }
    }
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.84f).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(strings.queue, style = MaterialTheme.typography.headlineSmall, color = text, fontWeight = FontWeight.Bold)
                    Text("${strings.queueSource}: ${state.currentPlaylistName ?: strings.queue} · ${state.queue.size}", style = MaterialTheme.typography.bodySmall, color = text.copy(alpha = .65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, strings.cancel, tint = text) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = playerViewModel::skipToPrevious) { Icon(Icons.Filled.SkipPrevious, strings.previous, tint = text) }
                FilledIconButton(onClick = playerViewModel::togglePlayPause) { Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (state.isPlaying) strings.pause else strings.play) }
                IconButton(onClick = playerViewModel::skipToNext) { Icon(Icons.Filled.SkipNext, strings.next, tint = text) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = playerViewModel::toggleShuffle) { Icon(Icons.Filled.Shuffle, strings.shuffle, tint = if (state.shuffleEnabled) accent else text) }
                IconButton(onClick = playerViewModel::cycleRepeatMode) { Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, strings.repeat, tint = if (state.repeatMode != RepeatMode.OFF) accent else text) }
                IconButton(onClick = { history = !history }) { Icon(Icons.Filled.History, strings.queueHistory, tint = if (history) accent else text) }
                IconButton(onClick = { saving = true }, enabled = state.queue.isNotEmpty()) { Icon(Icons.Filled.PlaylistAdd, strings.queueSave, tint = text) }
                IconButton(onClick = { clearing = true }, enabled = current < state.queue.lastIndex) { Icon(Icons.Filled.DeleteSweep, strings.queueClear, tint = text) }
            }
            TextButton(onClick = playerViewModel::remixQueue, enabled = state.queue.size > 1) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = if (isRemixed) accent else text)
                Spacer(Modifier.width(8.dp))
                Text(strings.remix, color = if (isRemixed) accent else text)
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text(strings.queueSearch) }, leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            val visible = if (query.isBlank()) entries.toList() else entries.filter { it.track.title.contains(query, true) || it.track.artist.contains(query, true) }
            if (visible.isEmpty()) Text(strings.queueEmpty, color = text, modifier = Modifier.padding(16.dp))
            LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(visible, key = { _, e -> e.key }) { index, entry ->
                    ReorderableItem(reorder, key = entry.key) { isDragging ->
                        val actualIndex = if (query.isBlank()) (if (history) 0 else current) + index else entry.key
                        val active = actualIndex == current
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (active) accent.copy(alpha = .17f) else text.copy(alpha = if (isDragging) .14f else .06f)).clickable { playerViewModel.playQueueItemAbsolute(actualIndex) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = rememberStaticArtwork(entry.track.albumArtUri), contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)))
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(entry.track.title, color = if (active) accent else text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Text(entry.track.artist, color = text.copy(alpha = .65f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!active) {
                                IconButton(onClick = { playerViewModel.playQueueItemNext(actualIndex) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.PlaylistPlay, strings.playNext, tint = text) }
                                IconButton(onClick = { playerViewModel.removeQueueItemAbsolute(actualIndex) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Close, strings.removeFromQueue, tint = text) }
                                if (query.isBlank() && actualIndex > current) Icon(Icons.Filled.DragHandle, strings.reorder, tint = text, modifier = Modifier.size(28.dp).draggableHandle())
                            }
                        }
                    }
                }
            }
        }
    }
    if (useMiuix) MiuixDrawer(onDismissRequest = onDismiss, content = content)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = content)
    if (saving) AlertDialog(onDismissRequest = { saving = false }, title = { Text(strings.queueSave) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(strings.playlistName) }) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            val playlistName = name.trim(); val ids = state.queue.map { it.id }; saving = false
            scope.launch {
                val ok = runCatching { prefs.updatePlaylistsJson { appendPlaylist(it, playlistName, ids) } }.isSuccess
                android.widget.Toast.makeText(context, if (ok) strings.queueSaved else strings.queueSaveFailed, android.widget.Toast.LENGTH_SHORT).show()
            }
        }) { Text(strings.save) } }, dismissButton = { TextButton(onClick = { saving = false }) { Text(strings.cancel) } })
    if (clearing) AlertDialog(onDismissRequest = { clearing = false }, title = { Text(strings.queueClear) },
        confirmButton = { TextButton(onClick = { playerViewModel.clearUpcomingQueue(); clearing = false }) { Text(strings.queueRemoveConfirm) } },
        dismissButton = { TextButton(onClick = { clearing = false }) { Text(strings.cancel) } })
}
