// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.i18n.LocalStrings

/** Read-only album route reuses the actual playlist-detail UI without persisting a playlist. */
@Composable
fun AlbumDetailScreen(albumId: Long, albumName: String, artist: String, useMiuix: Boolean,
    player: PlayerViewModel, onBack: () -> Unit, library: LibraryViewModel = viewModel()) {
    val all by library.tracks.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(library) { library.ensureTracksLoaded() }
    val songs = remember(all, albumId, albumName, artist) {
        if (albumId > 0L) all.filter { it.albumId == albumId }
        else all.filter { it.album.equals(albumName, ignoreCase = true) && it.artist.equals(artist, ignoreCase = true) }
    }
    val album = remember(albumName, songs) {
        LocalPlaylist(name = albumName, trackIds = songs.map { it.id },
            coverUri = songs.firstOrNull()?.albumArtUri?.toString(), createdAt = 0L)
    }
    var optionTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var editTrack by remember { mutableStateOf<AudioTrack?>(null) }
    val playAll: () -> Unit = { if (songs.isNotEmpty()) player.setQueueAndPlay(songs, 0, albumName) }
    val remix: () -> Unit = { if (songs.isNotEmpty()) player.setQueueAndPlayRemixed(songs, albumName) }
    val onTrack: (List<AudioTrack>, Int) -> Unit = { tracks, index -> player.setQueueAndPlay(tracks, index, albumName) }
    if (useMiuix) PlaylistDetailView(
        playlist = album, allTracks = all, plTracks = songs, strings = strings, onBack = onBack,
        menuChoices = listOf(Triple(strings.play, false, playAll), Triple(strings.remix, false, remix)),
        onTrackClick = onTrack, onPlayAll = playAll, onPlayRemix = remix,
        onRemoveTrack = {}, onTrackOptions = { optionTrack = it }, onAddTracks = {}, isAlbum = true)
    else dev.shephard.player.ui.screens.m3.M3PlaylistDetail(
        playlist = album, allTracks = all, plTracks = songs, strings = strings, onBack = onBack,
        onTrackClick = onTrack, onPlayAll = playAll, onPlayRemix = remix,
        onRemoveTrack = {}, onTrackOptions = { optionTrack = it }, isAlbum = true)
    optionTrack?.let { track ->
        SongOptionsSheet(track, useMiuix, player, onDismiss = { optionTrack = null },
            onEdit = { optionTrack = null; editTrack = track }, onDelete = null)
    }
    editTrack?.let { track ->
        if (useMiuix) EditMusicDrawer(track, library, player) { editTrack = null }
        else dev.shephard.player.ui.screens.m3.M3EditTrackDialogLineage(track, library, player) { editTrack = null }
    }
}
