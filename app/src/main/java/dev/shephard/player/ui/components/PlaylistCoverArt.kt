package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack

/**
 * Playlist cover, shared by the Miuix and Material 3 screens and the home screen.
 *
 * An explicit cover always wins. Without one, the album art of the first tracks is used:
 *  - 4 or more tracks: 2x2 collage of the first four,
 *  - 3 tracks: one large tile on the left, two stacked on the right,
 *  - 2 tracks: two tiles side by side,
 *  - 1 track: that track's art on its own (same as before),
 *  - no tracks: [fallback].
 */
@Composable
fun PlaylistCoverArt(
    coverUri: String?,
    tracks: List<AudioTrack>,
    modifier: Modifier = Modifier.fillMaxSize(),
    placeholderColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    placeholderIconColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    fallback: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val arts = tracks.take(4).map { it.albumArtUri }
            when (arts.size) {
                0 -> fallback()
                1 -> ArtTile(
                    model = arts[0],
                    modifier = Modifier.fillMaxSize(),
                    placeholderColor = placeholderColor,
                    placeholderIconColor = placeholderIconColor
                )
                2 -> Row(modifier = Modifier.fillMaxSize()) {
                    ArtTile(arts[0], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                    ArtTile(arts[1], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                }
                3 -> Row(modifier = Modifier.fillMaxSize()) {
                    ArtTile(arts[0], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ArtTile(arts[1], Modifier.weight(1f).fillMaxWidth(), placeholderColor, placeholderIconColor)
                        ArtTile(arts[2], Modifier.weight(1f).fillMaxWidth(), placeholderColor, placeholderIconColor)
                    }
                }
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ArtTile(arts[0], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                        ArtTile(arts[1], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ArtTile(arts[2], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                        ArtTile(arts[3], Modifier.weight(1f).fillMaxHeight(), placeholderColor, placeholderIconColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtTile(
    model: Any?,
    modifier: Modifier,
    placeholderColor: Color,
    placeholderIconColor: Color
) {
    Box(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = placeholderIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
