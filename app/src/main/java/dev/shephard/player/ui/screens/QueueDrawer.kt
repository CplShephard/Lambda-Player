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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.QueueRemoveButton
import dev.shephard.player.ui.components.rememberStaticArtwork
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf

private data class QueueEntry(val key: Int, val track: AudioTrack)

@Composable
fun QueueDrawer(playerViewModel: PlayerViewModel, useMiuix: Boolean, onDismiss: () -> Unit) {
    val state by playerViewModel.uiState.collectAsState()
    val isRemixed by playerViewModel.isRemixed.collectAsState()
    val strings = LocalStrings.current
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val text = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val card = if (useMiuix) dev.shephard.player.ui.components.miuixDrawerCardColor() else MaterialTheme.colorScheme.surfaceContainerHigh
    val dock = if (useMiuix) MiuixAppTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHighest
    val current = state.currentQueueIndex.coerceIn(0, (state.queue.size - 1).coerceAtLeast(0))
    val entries = remember { mutableStateListOf<QueueEntry>() }
    var drag by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(state.queue, current) {
        if (drag == null) {
            entries.clear()
            entries.addAll(state.queue.drop(current).mapIndexed { i, track -> QueueEntry(current + i, track) })
        }
    }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        if (from.index in entries.indices && to.index in entries.indices) {
            val f = from.index + current
            val t = to.index + current
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
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                LibraryText(strings.queue, useMiuix, color = text, bold = true)
                LibraryText("${strings.queueSource}: ${state.currentPlaylistName ?: strings.queue}", useMiuix, color = text.copy(alpha = .65f))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                        ReorderableItem(reorder, key = entry.key) { dragging ->
                            val actualIndex = current + index
                            val active = actualIndex == current
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(card)
                                .clickable { playerViewModel.playQueueItemAbsolute(actualIndex) }
                                .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = rememberStaticArtwork(entry.track.albumArtUri), contentDescription = null,
                                    modifier = Modifier.size(46.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    LibraryText(entry.track.title, useMiuix, color = if (active) accent else text, bold = true)
                                    LibraryText(entry.track.artist, useMiuix, color = text.copy(alpha = .65f))
                                }
                                if (!active) {
                                    QueueRemoveButton(trackId = entry.key.toLong(), contentDescription = strings.removeFromQueue,
                                        confirmLabel = strings.queueRemoveConfirm, color = Color(0xFFE04F5F),
                                        labelStyle = if (useMiuix) MiuixAppTheme.typography.labelMedium else MaterialTheme.typography.labelMedium,
                                        onRemove = { playerViewModel.removeQueueItemAbsolute(actualIndex) })
                                    LibraryIcon(Icons.Filled.DragHandle, strings.reorder, useMiuix, text.copy(alpha = if (dragging) 1f else .65f), Modifier.size(28.dp).draggableHandle())
                                }
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).clip(RoundedCornerShape(28.dp))
                    .background(dock).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    QueueDockButton(strings.remix, Icons.Filled.AutoAwesome, isRemixed, accent, text, useMiuix,
                        enabled = state.queue.size > 1 || isRemixed, onClick = playerViewModel::remixQueue)
                    QueueDockButton(strings.repeat, if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        state.repeatMode != RepeatMode.OFF, accent, text, useMiuix, onClick = playerViewModel::cycleRepeatMode)
                }
            }
        }
    }
    if (useMiuix) MiuixDrawer(onDismissRequest = onDismiss, content = content)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = content)
}

@Composable
private fun QueueDockButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, accent: Color, foreground: Color, useMiuix: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(22.dp)).background(if (selected) accent.copy(alpha = .18f) else Color.Transparent)
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val ink = if (!enabled) foreground.copy(alpha = .35f) else if (selected) accent else foreground
        LibraryIcon(icon, label, useMiuix, ink)
        LibraryText(label, useMiuix, color = ink)
    }
}
