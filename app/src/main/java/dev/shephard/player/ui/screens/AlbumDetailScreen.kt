// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme

/** Album navigation from the carousel queries the complete local library. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(albumId: Long, albumName: String, artist: String, useMiuix: Boolean,
    player: PlayerViewModel, onBack: () -> Unit, library: LibraryViewModel = viewModel()) {
    val all by library.tracks.collectAsState()
    val loading by library.isLoading.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(library) { library.loadTracks() }
    val songs = remember(all, albumId, albumName, artist) {
        if (albumId > 0L) all.filter { it.albumId == albumId }
        else all.filter { it.album.equals(albumName, ignoreCase = true) && it.artist.equals(artist, ignoreCase = true) }
    }
    val bg = if (useMiuix) MiuixAppTheme.colorScheme.background else MaterialTheme.colorScheme.surface
    val fg = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val barState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()
    var songOptions by remember { mutableStateOf<dev.shephard.player.data.AudioTrack?>(null) }
    var editSong by remember { mutableStateOf<dev.shephard.player.data.AudioTrack?>(null) }
    Scaffold(containerColor = bg, contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        if (useMiuix) dev.shephard.player.ui.components.SubmenuTopBar(
            title = albumName.ifBlank { strings.album }, onBack = onBack, state = barState, collapseFraction = 0f)
        else TopAppBar(title = { Text(albumName.ifBlank { strings.album }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.backContentDescription) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = bg))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LibraryArtwork(songs.firstOrNull(), useMiuix, Modifier.size(180.dp))
                    LibraryText(albumName, useMiuix, Modifier.padding(top = 16.dp), color = fg, bold = true, maxLines = 2)
                    LibraryText(artist, useMiuix, Modifier.padding(top = 4.dp), color = fg.copy(alpha = .65f))
                }
            }
            if (loading && songs.isEmpty()) item {
                if (useMiuix) dev.shephard.player.ui.miuix.CircularProgressIndicator(Modifier.padding(24.dp))
                else CircularProgressIndicator(Modifier.padding(24.dp))
            }
            if (!loading && songs.isEmpty()) item { LibraryText(strings.noSearchResults, useMiuix, Modifier.padding(16.dp), color = fg) }
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                LibrarySongRow(song, useMiuix, onClick = { player.setQueueAndPlay(songs, index, albumName) }, onMore = { songOptions = song })
            }
        }
    }
    songOptions?.let { song ->
        SongOptionsSheet(song, useMiuix, player, onDismiss = { songOptions = null },
            onEdit = { songOptions = null; editSong = song }, onDelete = null)
    }
    editSong?.let { song ->
        if (useMiuix) EditMusicDrawer(song, library, player) { editSong = null }
        else dev.shephard.player.ui.screens.m3.M3EditTrackDialogLineage(song, library, player) { editSong = null }
    }
}
