// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.PlaylistCoverArt
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

/** Playlist action sheet following Pixel's header + grouped action rows. */
@Composable
internal fun PlaylistDetailMenu(miuix: Boolean, playlist: LocalPlaylist, onDismiss: () -> Unit,
    onPlay: () -> Unit, onRemix: () -> Unit, onEdit: () -> Unit, onAdd: () -> Unit, onDelete: () -> Unit) {
    val s = LocalStrings.current
    val fg = if (miuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val card = if (miuix) dev.shephard.player.ui.components.miuixDrawerCardColor() else MaterialTheme.colorScheme.surfaceContainerHigh
    val body: @Composable () -> Unit = {
        val closeThen = dev.shephard.player.ui.components.rememberCloseDrawerThen()
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LibraryText(s.libraryOptions, miuix, Modifier.padding(start = 8.dp, bottom = 4.dp), color = fg, bold = true)
            LibraryText(playlist.name, miuix, Modifier.padding(start = 8.dp, bottom = 12.dp), color = fg.copy(alpha = .66f))
            @Composable fun Action(icon: ImageVector, text: String, action: () -> Unit) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(card).clickable { closeThen(action) }
                    .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    LibraryIcon(icon, null, miuix, fg, Modifier.size(22.dp))
                    LibraryText(text, miuix, Modifier.padding(start = 16.dp), color = fg)
                }
            }
            Action(Icons.Default.PlayArrow, s.play, onPlay)
            Action(Icons.Default.Shuffle, s.remix, onRemix)
            if (!playlist.isSystem) {
                Action(Icons.Default.Edit, s.editPlaylist, onEdit)
                Action(Icons.Default.Add, s.addTracks, onAdd)
                Action(Icons.Default.Delete, s.delete, onDelete)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (miuix) MiuixDrawer(onDismissRequest = onDismiss, content = body)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = body)
}

/** Editing remains in a bottom sheet in both themes; cover selection still uses existing persistence. */
@Composable
internal fun PlaylistDetailEditor(miuix: Boolean, playlist: LocalPlaylist, tracks: List<AudioTrack>,
    onDismiss: () -> Unit, onSave: (String) -> Unit, onPickCover: () -> Unit, onRemoveCover: () -> Unit) {
    val s = LocalStrings.current
    val fg = if (miuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val bg = if (miuix) MiuixAppTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = if (miuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    var name by remember(playlist.createdAt) { mutableStateOf(playlist.name) }
    var pendingName by remember { mutableStateOf<String?>(null) }
    val body: @Composable () -> Unit = {
        val dismissDrawer = if (miuix) dev.shephard.player.ui.components.rememberDrawerDismiss() else onDismiss
        val closeThen = dev.shephard.player.ui.components.rememberCloseDrawerThen()
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (miuix) dev.shephard.player.ui.components.MiuixDrawerActionHeader(
                title = s.editPlaylist, onCancel = dismissDrawer, confirmEnabled = name.isNotBlank(),
                onConfirm = { pendingName = name.trim(); dismissDrawer() })
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = { closeThen {} }) { androidx.compose.material3.Icon(Icons.Default.Close, s.cancel) }
                LibraryText(s.editPlaylist, false, color = fg, bold = true)
                androidx.compose.material3.IconButton(onClick = { val saved = name.trim(); closeThen { onSave(saved) } }, enabled = name.isNotBlank()) {
                    androidx.compose.material3.Icon(Icons.Default.Check, s.save)
                }
            }
            Box(Modifier.size(168.dp).clip(RoundedCornerShape(22.dp)).background(bg).clickable(onClick = onPickCover), contentAlignment = Alignment.Center) {
                PlaylistCoverArt(playlist.coverUri, tracks, placeholderColor = bg, placeholderIconColor = accent, animateVideo = true,
                    fallback = { LibraryIcon(Icons.Default.MusicNote, null, miuix, accent, Modifier.size(48.dp)) })
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(34.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
                    LibraryIcon(Icons.Default.Edit, s.cover, miuix, fg, Modifier.size(18.dp))
                }
            }
            if (playlist.coverUri != null) LibraryText(s.removeCover, miuix, Modifier.clickable(onClick = onRemoveCover).padding(6.dp), color = accent)
            if (miuix) dev.shephard.player.ui.miuix.OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { dev.shephard.player.ui.miuix.Text(s.playlistName, color = fg) })
            else androidx.compose.material3.OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { androidx.compose.material3.Text(s.playlistName) })

        }
    }
    if (miuix) MiuixDrawer(onDismissRequest = {
        val saved = pendingName
        pendingName = null
        if (saved != null) onSave(saved) else onDismiss()
    }, content = body)
    else M3BottomSheetWrapper(onDismissRequest = onDismiss, content = body)
}
