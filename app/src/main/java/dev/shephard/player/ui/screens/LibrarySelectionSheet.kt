@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.shephard.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf

/** Bulk chooser is independent of the list/grid layout and persists into existing playlists. */
@Composable
internal fun LibrarySelectionSheet(tracks: List<AudioTrack>, useMiuix: Boolean, player: PlayerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val prefs = remember(context) { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val stored by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(stored) { parsePlaylists(stored).filterNot { it.isSystem } }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var query by remember { mutableStateOf("") }
    var playlistMode by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val selectedTracks = tracks.filter { it.id in selected }
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    fun onError() = android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
    val content: @Composable () -> Unit = {
        val closeThen = dev.shephard.player.ui.components.rememberCloseDrawerThen()
        val actionCard = if (useMiuix) dev.shephard.player.ui.components.miuixDrawerCardColor() else MaterialTheme.colorScheme.surfaceContainerHigh
        Column(Modifier.fillMaxWidth().heightIn(max = 700.dp).padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (playlistMode) strings.addToPlaylist else "${strings.selectSongs} (${selectedTracks.size})", color = foreground,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { if (playlistMode) playlistMode = false else closeThen {} }) { Text(if (playlistMode) "‹" else "✕") }
            }
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(strings.search) })
            if (!playlistMode) {
                val visible = tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
                Row {
                    TextButton(onClick = { selected = selected + visible.map { it.id } }) { Text(strings.selectAllSongs) }
                    TextButton(onClick = { selected = emptySet() }) { Text(strings.clearSelection) }
                }
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 360.dp)) {
                    items(visible, key = { it.id }) { track ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(actionCard).clickable {
                            selected = if (track.id in selected) selected - track.id else selected + track.id
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = track.id in selected, onCheckedChange = null)
                            AsyncImage(dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri), null, Modifier.size(40.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(track.title, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = foreground.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(actionCard), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(enabled = selectedTracks.isNotEmpty(), onClick = { closeThen { player.setQueueAndPlay(selectedTracks, 0) } }) { Text(strings.play) }
                    TextButton(enabled = selectedTracks.isNotEmpty(), onClick = { closeThen { player.appendTracksToQueue(selectedTracks) } }) { Text(strings.addToQueue) }
                    TextButton(enabled = selectedTracks.isNotEmpty(), onClick = {
                        scope.launch { runCatching { prefs.addLikedTrackIds(selectedTracks.map { it.id }) }.onFailure { onError() } }
                    }) { Text("♥") }
                }
                TextButton(enabled = selectedTracks.isNotEmpty(), onClick = { query = ""; playlistMode = true }) { Text(strings.addToPlaylist) }
            } else {
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 300.dp)) {
                    items(playlists.filter { it.name.contains(query, true) }) { playlist ->
                        val included = selectedTracks.all { it.id in playlist.trackIds }
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(actionCard).clickable {
                            scope.launch {
                                runCatching {
                                    prefs.updatePlaylistsJson { json ->
                                        updatePlaylistMembership(json, playlist.name, playlist.createdAt, selectedTracks.map { it.id }, "add")
                                    }
                                }.onFailure { onError() }
                            }
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(playlist.name, color = foreground, modifier = Modifier.weight(1f))
                            if (included) Text("✓", color = foreground)
                        }
                    }
                }
                OutlinedTextField(newName, { newName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(strings.playlistName) })
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    val name = newName.trim(); newName = ""
                    scope.launch {
                        runCatching {
                            prefs.updatePlaylistsJson { json -> appendPlaylist(json, name, selectedTracks.map { it.id }) }
                        }.onFailure { onError() }
                    }
                }) { Text(strings.createPlaylist) }
            }
        }
    }
    if (useMiuix) MiuixDrawer(onDismissRequest = onDismiss, content = content)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = content)
}
