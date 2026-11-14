// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.components.rememberStaticArtwork
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme

/** Album navigation from the carousel; it queries the library, not only the playback queue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    albumName: String,
    artist: String,
    useMiuix: Boolean,
    player: PlayerViewModel,
    onBack: () -> Unit,
    library: LibraryViewModel = viewModel(),
) {
    val all by library.tracks.collectAsState()
    val loading by library.isLoading.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(library) { library.loadTracks() }
    val songs = remember(all, albumId, albumName, artist) {
        if (albumId > 0L) all.filter { it.albumId == albumId }
        else all.filter { it.album.equals(albumName, ignoreCase = true) && it.artist.equals(artist, ignoreCase = true) }
    }
    val background = if (useMiuix) MiuixAppTheme.colorScheme.background else MaterialTheme.colorScheme.surface
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    Scaffold(containerColor = background, topBar = {
        TopAppBar(title = { Text(albumName.ifBlank { strings.album }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.backContentDescription) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = background, titleContentColor = foreground, navigationIconContentColor = foreground))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(180.dp).clip(RoundedCornerShape(24.dp)).background(foreground.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, null, tint = foreground.copy(alpha = .5f), modifier = Modifier.size(56.dp))
                        AsyncImage(model = rememberStaticArtwork(songs.firstOrNull()?.albumArtUri), contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Text(albumName, color = foreground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    Text(artist, color = foreground.copy(alpha = .65f), modifier = Modifier.padding(bottom = 16.dp))
                }
            }
            if (loading && songs.isEmpty()) item { CircularProgressIndicator(Modifier.padding(24.dp)) }
            if (!loading && songs.isEmpty()) item { Text(strings.noSearchResults, color = foreground, modifier = Modifier.padding(16.dp)) }
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                ListItem(
                    headlineContent = { Text(song.title, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(song.artist, color = foreground.copy(alpha = .65f), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { AsyncImage(rememberStaticArtwork(song.albumArtUri), null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop) },
                    modifier = Modifier.clickable { player.setQueueAndPlay(songs, index, albumName) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}
