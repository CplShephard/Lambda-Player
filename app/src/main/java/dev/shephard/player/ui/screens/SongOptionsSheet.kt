@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.shephard.player.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import kotlinx.coroutines.launch

/** Shared content and actions, hosted in each theme's native sheet. */
@Composable
internal fun SongOptionsSheet(
    track: AudioTrack,
    useMiuix: Boolean,
    player: PlayerViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val prefs = remember(context) { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val state by player.uiState.collectAsState()
    val playlistJson by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(playlistJson) { parsePlaylists(playlistJson).filterNot { it.isSystem } }
    var page by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val card = if (useMiuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().heightIn(max = 660.dp).padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                AsyncImage(dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri), null, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    LibraryText(track.title, useMiuix, color = foreground, bold = true)
                    LibraryText("${track.artist} • ${track.album}", useMiuix, color = foreground.copy(alpha = .7f), maxLines = 2)
                }
            }
            when (page) {
                0 -> Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    @Composable fun Action(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(RoundedCornerShape(15.dp)).background(card)
                            .clickable(onClick = action).padding(vertical = 13.dp, horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            LibraryIcon(icon, null, useMiuix, foreground); Spacer(Modifier.width(16.dp)); LibraryText(label, useMiuix, color = foreground)
                        }
                    }
                    Action(strings.play, Icons.Default.PlayArrow) { player.setQueueAndPlay(listOf(track), 0); onDismiss() }
                    Action(strings.likedSongs, if (track.id in state.likedSongIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder) { player.toggleLike(track.id) }
                    Action(strings.playNext, Icons.Default.QueueMusic) { player.addTrackToPlayNext(track); onDismiss() }
                    Action(strings.addToQueue, Icons.Default.PlaylistAdd) { player.addTrackToQueue(track); onDismiss() }
                    Action(strings.addToPlaylist, Icons.Default.LibraryMusic) { page = 1 }
                    Action(strings.shareTrack, Icons.Default.Share) {
                        val intent = Intent(Intent.ACTION_SEND).setType("audio/*").putExtra(Intent.EXTRA_STREAM, track.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { context.startActivity(Intent.createChooser(intent, strings.shareTrack)) }
                        onDismiss()
                    }
                    Action(strings.musicBrainzSearch, Icons.Default.Search) {
                        val search = android.net.Uri.parse("https://musicbrainz.org/search").buildUpon()
                            .appendQueryParameter("query", "${track.artist} ${track.title}")
                            .appendQueryParameter("type", "recording").appendQueryParameter("method", "indexed").build()
                        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, search)) }.isFailure)
                            android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                    Action(strings.setRingtone, Icons.Default.Notifications) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.System.canWrite(context)) {
                            android.widget.Toast.makeText(context, strings.ringtonePermission, android.widget.Toast.LENGTH_LONG).show()
                            runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) }
                        } else {
                            val success = runCatching { android.media.RingtoneManager.setActualDefaultRingtoneUri(context, android.media.RingtoneManager.TYPE_RINGTONE, track.uri) }.isSuccess
                            android.widget.Toast.makeText(context, if (success) strings.ringtoneSet else strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                    Action(strings.libraryDetails, Icons.Default.Info) { page = 2 }
                    Action(strings.editMusic, Icons.Default.Edit, onEdit)
                    if (onRemoveFromPlaylist != null) Action(strings.removeFromPlaylist, Icons.Default.RemoveCircleOutline, onRemoveFromPlaylist)
                    if (onDelete != null) Action(strings.delete, Icons.Default.Delete, onDelete)
                }
                1 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LibraryText("‹", useMiuix, Modifier.clickable { page = 0 }.padding(12.dp), color = foreground)
                        LibraryText(strings.addToPlaylist, useMiuix, color = foreground, bold = true)
                    }
                    if (useMiuix) dev.shephard.player.ui.miuix.OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { dev.shephard.player.ui.miuix.Text(strings.search) })
                    else OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(strings.search) })
                    LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 280.dp)) {
                        items(playlists.filter { it.name.contains(query, ignoreCase = true) }) { playlist ->
                            val checked = track.id in playlist.trackIds
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(card).clickable {
                                scope.launch {
                                    prefs.updatePlaylistsJson { json ->
                                        updatePlaylistMembership(json, playlist.name, playlist.createdAt, listOf(track.id), "toggle")
                                    }
                                }
                            }.padding(vertical = 6.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (useMiuix) dev.shephard.player.ui.miuix.Checkbox(checked = checked, onCheckedChange = null)
                                else Checkbox(checked = checked, onCheckedChange = null)
                                LibraryText(playlist.name, useMiuix, modifier = Modifier.padding(start = 8.dp), color = foreground)
                            }
                        }
                    }
                    if (useMiuix) dev.shephard.player.ui.miuix.OutlinedTextField(newName, { newName = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { dev.shephard.player.ui.miuix.Text(strings.playlistName) })
                    else OutlinedTextField(newName, { newName = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(strings.playlistName) })
                    LibraryText(strings.createPlaylist, useMiuix, Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(card)
                        .clickable(enabled = newName.isNotBlank()) {
                            val name = newName.trim(); newName = ""
                            scope.launch { prefs.updatePlaylistsJson { json -> appendPlaylist(json, name, listOf(track.id)) } }
                        }.padding(14.dp), color = foreground)
                }
                else -> {
                    LibraryText("‹  ${strings.libraryDetails}", useMiuix, Modifier.clickable { page = 0 }.padding(12.dp), color = foreground, bold = true)
                    listOf(track.title, track.artist, track.album, track.genre ?: strings.libraryUnknown,
                        track.formattedDuration(), track.folder ?: track.uri.toString()).forEach {
                        LibraryText(it, useMiuix, Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(card).padding(12.dp), color = foreground, maxLines = 3)
                    }
                }
            }
        }
    }
    if (useMiuix) MiuixDrawer(onDismissRequest = onDismiss, content = content)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = content)
}
