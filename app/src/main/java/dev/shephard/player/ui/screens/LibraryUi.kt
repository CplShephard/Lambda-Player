// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.m3.m3CardPress
import dev.shephard.player.ui.components.rememberStaticArtwork
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme

@Composable
internal fun LibraryText(text: String, miuix: Boolean, modifier: Modifier = Modifier,
    color: Color, bold: Boolean = false, maxLines: Int = 1,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified) {
    if (miuix) dev.shephard.player.ui.miuix.Text(text, modifier, color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = fontSize, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    else androidx.compose.material3.Text(text, modifier, color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = fontSize, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun LibraryIcon(icon: ImageVector, description: String?, miuix: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (miuix) dev.shephard.player.ui.miuix.Icon(icon, description, modifier, tint = color)
    else androidx.compose.material3.Icon(icon, description, modifier, tint = color)
}

@Composable
internal fun LibraryHeaderButton(miuix: Boolean, icon: ImageVector, description: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        LibraryIcon(icon, description, miuix, color, Modifier.size(24.dp))
    }
}

@Composable
internal fun LibraryChoiceDrawer(miuix: Boolean, title: String, choices: List<Triple<String, Boolean, () -> Unit>>, onDismiss: () -> Unit) {
    val ink = if (miuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val accent = if (miuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val card = if (miuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            LibraryText(title, miuix, Modifier.padding(8.dp), color = ink, bold = true)
            choices.forEach { (label, selected, action) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(card).clickable(onClick = action)
                    .padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    LibraryText(label, miuix, Modifier.weight(1f), color = if (selected) accent else ink)
                    if (selected) LibraryIcon(Icons.Default.Check, null, miuix, accent)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (miuix) MiuixDrawer(onDismissRequest = onDismiss, content = body)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = body)
}

@Composable
internal fun LibraryArtwork(track: AudioTrack?, miuix: Boolean, modifier: Modifier = Modifier) {
    val bg = if (miuix) MiuixAppTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (miuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
        LibraryIcon(Icons.Default.MusicNote, null, miuix, fg.copy(alpha = .5f), Modifier.size(30.dp))
        AsyncImage(rememberStaticArtwork(track?.albumArtUri), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun LibraryAlbumRow(name: String, tracks: List<AudioTrack>, miuix: Boolean, onClick: () -> Unit) {
    val bg = if (miuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (miuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(bg)
        .then(if (miuix) Modifier.clickable(onClick = onClick) else Modifier.m3CardPress(shape, onClick = onClick))
        .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LibraryArtwork(tracks.firstOrNull(), miuix, Modifier.size(58.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            LibraryText(name, miuix, color = fg, bold = true)
            LibraryText("${tracks.size} ${LocalStrings.current.librarySongs}", miuix, color = fg.copy(alpha = .66f))
        }
    }
}

@Composable
internal fun LibraryAlbumTile(name: String, tracks: List<AudioTrack>, miuix: Boolean, onClick: () -> Unit) {
    val bg = if (miuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (miuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(bg)
        .then(if (miuix) Modifier.clickable(onClick = onClick) else Modifier.m3CardPress(shape, onClick = onClick)).padding(10.dp)) {
        LibraryArtwork(tracks.firstOrNull(), miuix, Modifier.fillMaxWidth().aspectRatio(1f))
        LibraryText(name, miuix, Modifier.padding(top = 10.dp), color = fg, bold = true, maxLines = 2)
        LibraryText("${tracks.size} ${LocalStrings.current.librarySongs}", miuix, Modifier.padding(top = 3.dp, bottom = 4.dp), color = fg.copy(alpha = .65f))
    }
}

@Composable
internal fun LibrarySongRow(track: AudioTrack, miuix: Boolean, onClick: () -> Unit, onMore: () -> Unit) {
    val bg = if (miuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (miuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(bg)
        .then(if (miuix) Modifier.clickable(onClick = onClick) else Modifier.m3CardPress(shape, onClick = onClick))
        .padding(start = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        LibraryArtwork(track, miuix, Modifier.size(52.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            LibraryText(track.title, miuix, color = fg, bold = true)
            LibraryText(track.artist, miuix, color = fg.copy(alpha = .65f))
        }
        LibraryHeaderButton(miuix, Icons.Default.MoreVert, LocalStrings.current.libraryOptions, fg, onMore)
    }
}

@Composable
internal fun LibraryEmptyResults(miuix: Boolean, message: String) {
    val fg = if (miuix) MiuixAppTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        LibraryIcon(Icons.Default.FolderOff, null, miuix, fg, Modifier.size(56.dp))
        LibraryText(message, miuix, Modifier.padding(top = 16.dp), color = fg, bold = true, maxLines = 2)
    }
}
