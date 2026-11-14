// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.shephard.player.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme

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
    val state by player.uiState.collectAsState()
    var showDetails by remember(track.id) { mutableStateOf(false) }
    var showSongMenu by remember(track.id) { mutableStateOf(true) }
    var showPlaylistPicker by remember(track.id) { mutableStateOf(false) }
    var openPickerAfterClose by remember(track.id) { mutableStateOf(false) }
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val card = if (useMiuix) dev.shephard.player.ui.components.miuixDrawerCardColor() else MaterialTheme.colorScheme.surfaceContainerHigh

    // Keep this host alive while switching windows. The parent removes it only after the
    // playlist picker is closed, so its state is not destroyed during the first exit animation.
    val onSongMenuClosed: () -> Unit = {
        if (openPickerAfterClose) {
            openPickerAfterClose = false
            showSongMenu = false
            showPlaylistPicker = true
        } else onDismiss()
    }
    val content: @Composable () -> Unit = {
        val closeThen = dev.shephard.player.ui.components.rememberCloseDrawerThen()
        val backState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(
            state = backState,
            isBackEnabled = showDetails,
            onBackCompleted = { showDetails = false },
        )
        Column(Modifier.fillMaxWidth().heightIn(max = 660.dp).padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                AsyncImage(dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri), null,
                    Modifier.size(64.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    LibraryText(track.title, useMiuix, color = foreground, bold = true)
                    LibraryText("${track.artist} • ${track.album}", useMiuix, color = foreground.copy(alpha = .7f), maxLines = 2)
                }
                if (!showDetails) {
                    Box(
                        Modifier.align(Alignment.Top).size(40.dp).clip(CircleShape).clickable { showDetails = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        LibraryIcon(Icons.Default.Info, strings.libraryDetails, useMiuix, foreground, Modifier.size(22.dp))
                    }
                }
            }
            AnimatedContent(
                targetState = showDetails,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(tween(280)) { it } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(160)))
                    } else {
                        (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(160)))
                    }
                },
                label = "songOptionsDetailsSlide",
            ) { details ->
                if (!details) {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val actions = mutableListOf<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>>()
                        actions += Triple(strings.play, Icons.Default.PlayArrow) { closeThen { player.setQueueAndPlay(listOf(track), 0) } }
                        actions += Triple(strings.likedSongs, if (track.id in state.likedSongIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder) { player.toggleLike(track.id) }
                        actions += Triple(strings.playNext, Icons.Default.QueueMusic) { closeThen { player.addTrackToPlayNext(track) } }
                        actions += Triple(strings.addToQueue, Icons.Default.PlaylistAdd) { closeThen { player.addTrackToQueue(track) } }
                        actions += Triple(strings.addToPlaylist, Icons.Default.LibraryMusic) {
                            openPickerAfterClose = true
                            closeThen { }
                        }
                        actions += Triple(strings.shareTrack, Icons.Default.Share) {
                            closeThen {
                                val intent = Intent(Intent.ACTION_SEND).setType("audio/*").putExtra(Intent.EXTRA_STREAM, track.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                runCatching { context.startActivity(Intent.createChooser(intent, strings.shareTrack)) }
                            }
                        }
                        actions += Triple(strings.editMusic, Icons.Default.Edit) { closeThen(onEdit) }
                        if (onRemoveFromPlaylist != null) actions += Triple(strings.removeFromPlaylist, Icons.Default.RemoveCircleOutline) { closeThen(onRemoveFromPlaylist) }
                        if (onDelete != null) actions += Triple(strings.delete, Icons.Default.Delete) { closeThen(onDelete) }
                        actions.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (label, icon, action) ->
                                    Row(Modifier.weight(1f).heightIn(min = 58.dp).clip(RoundedCornerShape(15.dp)).background(card)
                                        .clickable(onClick = action).padding(vertical = 12.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        LibraryIcon(icon, null, useMiuix, foreground, Modifier.size(21.dp))
                                        LibraryText(label, useMiuix, Modifier.padding(start = 10.dp), color = foreground, maxLines = 2)
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable { showDetails = false }, contentAlignment = Alignment.Center) {
                            LibraryIcon(Icons.AutoMirrored.Filled.ArrowBack, strings.backContentDescription, useMiuix, foreground, Modifier.size(22.dp))
                        }
                        listOf(track.title, track.artist, track.album, track.genre ?: strings.libraryUnknown,
                            track.formattedDuration(), track.folder ?: track.uri.toString()).forEach {
                            LibraryText(it, useMiuix, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(card).padding(12.dp), color = foreground, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
    if (showSongMenu) {
        if (useMiuix) MiuixDrawer(onDismissRequest = onSongMenuClosed, content = content)
        else M3BottomSheetWrapper(onDismissRequest = onSongMenuClosed, content = content)
    }
    if (showPlaylistPicker) {
        if (useMiuix) AddToPlaylistDrawer(track.id, track, player, onDismiss, strings)
        else dev.shephard.player.ui.screens.m3.M3AddToPlaylistDrawer(track.id, track, player, onDismiss, strings)
    }
}
