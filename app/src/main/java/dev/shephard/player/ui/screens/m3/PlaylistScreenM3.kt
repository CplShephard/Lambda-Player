// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens.m3

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.navigation.PageTransitions
import dev.shephard.player.ui.navigation.SubmenuNavGuard
import dev.shephard.player.ui.screens.LocalPlaylist
import dev.shephard.player.ui.screens.encodePlaylists
import dev.shephard.player.ui.screens.ensureLikedSongsPlaylist
import dev.shephard.player.ui.screens.parsePlaylists
import dev.shephard.player.ui.screens.resolvePlaylistTracks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Material 3 Playlists page. Content mirrors the Miuix PlaylistScreen with M3
 * components only, including the grid/list switcher from preferences.
 */
@Composable
fun PlaylistScreenM3(
    libraryViewModel: LibraryViewModel = viewModel(),
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val tracks by libraryViewModel.tracks.collectAsState()

    LaunchedEffect(Unit) {
        if (tracks.isEmpty()) libraryViewModel.loadTracks()
    }

    val json by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlistsLayout by prefs.playlistsLayout.collectAsState(initial = LayoutMode.LIST)
    val likedSongIdsJson by prefs.likedSongIds.collectAsState(initial = "[]")
    val likedIds = remember(likedSongIdsJson) {
        try {
            org.json.JSONArray(likedSongIdsJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } }
        } catch (_: Exception) { emptyList() }
    }
    val rawPlaylists = remember(json) { parsePlaylists(json) }
    val playlists = remember(rawPlaylists, strings) { ensureLikedSongsPlaylist(rawPlaylists, strings) }

    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var openIndex by remember { mutableStateOf<Int?>(null) }

    val playlistDetailGuard = remember { SubmenuNavGuard() }
    var playlistMenuIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistName by remember { mutableStateOf("") }
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<LocalPlaylist?>(null) }

    var newCoverUri by remember { mutableStateOf<Uri?>(null) }
    var newCoverCropOutputUri by remember { mutableStateOf<Uri?>(null) }

    fun writePlaylists(all: List<LocalPlaylist>) {
        scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
    }

    fun deletePlaylist(pl: LocalPlaylist) {
        val all = rawPlaylists.filterNot { it.name == pl.name && it.createdAt == pl.createdAt && it.isSystem == pl.isSystem }
        writePlaylists(all)
        if (openIndex == rawPlaylists.indexOf(pl)) openIndex = null
    }

    // ── New playlist cover crop (same flow as Miuix page) ────────────────────
    val newCoverCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = newCoverCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            newCoverUri = output
        }
        newCoverCropOutputUri = null
    }

    fun launchNewCoverCrop(sourceUri: Uri) {
        if (context.contentResolver.getType(sourceUri) == "image/gif") {
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null) newCoverUri = persisted
            }
            return
        }
        val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
        val file = java.io.File(dir, "new_playlist_cover_${System.currentTimeMillis()}.jpg")
        val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        newCoverCropOutputUri = outputUri

        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(sourceUri, "image/*")
            putExtra("crop", "true")
            putExtra("scale", true)
            putExtra("outputX", 512)
            putExtra("outputY", 512)
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            putExtra("return-data", false)
            putExtra("noFaceDetection", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "playlist_cover", sourceUri)
        }
        val resolvedActivities = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolvedActivities) {
            val packageName = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    packageName,
                    outputUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
        }
        if (resolvedActivities.isNotEmpty()) {
            newCoverCropLauncher.launch(cropIntent)
        } else {
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null) newCoverUri = persisted
            }
        }
    }

    val newCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            launchNewCoverCrop(uri)
        }
    }

    // ── Create dialogue ──────────────────────────────────────────────────────
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(strings.createPlaylist) },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { newCoverPicker.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (newCoverUri != null) {
                            AsyncImage(
                                model = newCoverUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text(strings.playlistName) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            val newPl = LocalPlaylist(
                                name = name,
                                trackIds = emptyList(),
                                coverUri = newCoverUri?.toString(),
                            )
                            writePlaylists(rawPlaylists + newPl)
                            showCreate = false
                            newName = ""
                            newCoverUri = null
                        }
                    },
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(strings.cancel) }
            },
        )
    }

    // ── Edit name dialogue ───────────────────────────────────────────────────
    if (editPlaylistIndex != null) {
        val editingPl = playlists.getOrNull(editPlaylistIndex ?: -1)
        if (editingPl != null) {
            AlertDialog(
                onDismissRequest = { editPlaylistIndex = null },
                title = { Text(strings.editPlaylist) },
                text = {
                    OutlinedTextField(
                        value = editPlaylistName,
                        onValueChange = { editPlaylistName = it },
                        singleLine = true,
                        label = { Text(strings.playlistName) },
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newName = editPlaylistName.trim()
                            if (newName.isNotEmpty()) {
                                val all = rawPlaylists.toMutableList()
                                val idx = all.indexOfFirst { it.name == editingPl.name && it.createdAt == editingPl.createdAt }
                                if (idx >= 0) all[idx] = all[idx].copy(name = newName)
                                writePlaylists(all)
                            }
                            editPlaylistIndex = null
                        },
                    ) { Text(strings.save) }
                },
                dismissButton = {
                    TextButton(onClick = { editPlaylistIndex = null }) { Text(strings.cancel) }
                },
            )
        } else {
            editPlaylistIndex = null
        }
    }

    // ── Delete confirmation ──────────────────────────────────────────────────
    if (showDeletePlaylistConfirm) {
        val pl = playlistToDelete
        AlertDialog(
            onDismissRequest = { showDeletePlaylistConfirm = false; playlistToDelete = null },
            title = { Text(strings.delete) },
            text = {
                Text(strings.removePlaylistConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePlaylistConfirm = false
                        if (pl != null) deletePlaylist(pl)
                        playlistToDelete = null
                    },
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlaylistConfirm = false; playlistToDelete = null }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    if (openIndex != null && openIndex !in playlists.indices) openIndex = null
    if (playlistMenuIndex != null && playlistMenuIndex !in playlists.indices) playlistMenuIndex = null

    BackHandler(enabled = openIndex != null) {
        playlistDetailGuard.pop { openIndex = null }
    }

    AnimatedContent(
        targetState = openIndex,
        transitionSpec = {
            if (targetState != null) {
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.m3EnterSubmenu,
                    initialContentExit = PageTransitions.m3ExitSubmenu,
                    targetContentZIndex = 1f
                )
            } else {
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.m3PopEnterSubmenu,
                    initialContentExit = PageTransitions.m3PopExitSubmenu,
                    targetContentZIndex = 0f
                )
            }
        },
        label = "playlistM3Nav",
    ) { idx ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (idx == null) {
                M3PlaylistListView(
                    playlists = playlists,
                    tracks = tracks,
                    strings = strings,
                    likedIds = likedIds,
                    layout = playlistsLayout,
                    hasMiniPlayer = hasMiniPlayer,
                    onOpen = { openIdx -> playlistDetailGuard.push(openIndex, openIdx) { openIndex = openIdx } },
                    onPlay = { pl ->
                        val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                        if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name)
                    },
                    onRemix = { pl ->
                        val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                        if (plTracks.isNotEmpty()) {
                            if (pl.isSystem) {
                                onPlaylistRemixClick(plTracks.shuffled(), strings.likedSongs)
                            } else {
                                onPlaylistRemixClick(plTracks.shuffled(), pl.name)
                            }
                        }
                    },
                    onMenu = { realIdx -> playlistMenuIndex = realIdx },
                    onCreate = { showCreate = true },
                )
            } else {
                val selectedPl = playlists.getOrNull(idx)
                if (selectedPl != null) {
                    val plTracks = remember(selectedPl, tracks, likedIds) { resolvePlaylistTracks(selectedPl, tracks, likedIds) }
                    M3PlaylistDetail(
                        playlist = selectedPl,
                        allTracks = tracks,
                        plTracks = plTracks,
                        strings = strings,
                        onBack = { playlistDetailGuard.pop { openIndex = null } },
                        onTrackClick = { list, i -> onTrackClick(list, i, if (selectedPl.isSystem) strings.likedSongs else selectedPl.name) },
                        onPlayAll = { if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (selectedPl.isSystem) strings.likedSongs else selectedPl.name) },
                        onPlayRemix = {
                            if (plTracks.isNotEmpty()) {
                                if (selectedPl.isSystem) onPlaylistRemixClick(plTracks.shuffled(), strings.likedSongs)
                                else onPlaylistRemixClick(plTracks.shuffled(), selectedPl.name)
                            }
                        },
                        onRemoveTrack = { trackId ->
                            val newTrackIds = selectedPl.trackIds - trackId
                            val all = rawPlaylists.toMutableList()
                            val rawIdx = all.indexOfFirst { it.name == selectedPl.name && it.createdAt == selectedPl.createdAt }
                            if (rawIdx >= 0) {
                                all[rawIdx] = all[rawIdx].copy(trackIds = newTrackIds)
                                writePlaylists(all)
                            }
                        },
                        onAddTracks = {},
                        onPickCover = {},
                        onRename = {},
                        onDelete = {},
                    )
                }
            }
        }
    }

    // ── Playlist menu (rename / pin / delete / cover) ───────────────────────
    playlistMenuIndex?.let { menuIdx ->
        val pl = playlists.getOrNull(menuIdx)
        if (pl != null) {
            AlertDialog(
                onDismissRequest = { playlistMenuIndex = null },
                title = { Text(pl.name) },
                text = {
                    Column {
                        MenuRow(Icons.Filled.PlayArrow, strings.play) {
                            playlistMenuIndex = null
                            val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                            if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name)
                        }
                        MenuRow(Icons.Filled.Shuffle, strings.remix) {
                            playlistMenuIndex = null
                            val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                            if (plTracks.isNotEmpty()) {
                                if (pl.isSystem) onPlaylistRemixClick(plTracks.shuffled(), strings.likedSongs)
                                else onPlaylistRemixClick(plTracks.shuffled(), pl.name)
                            }
                        }
                        if (!pl.isSystem) {
                            MenuRow(Icons.Filled.Edit, strings.editPlaylist) {
                                playlistMenuIndex = null
                                editPlaylistName = pl.name
                                editPlaylistIndex = menuIdx
                            }
                            MenuRow(if (pl.pinned) Icons.Filled.Pin else Icons.Filled.PushPin, if (pl.pinned) strings.unpinPlaylist else strings.pinPlaylist) {
                                playlistMenuIndex = null
                                val all = rawPlaylists.toMutableList()
                                val rawIdx = all.indexOfFirst { it.name == pl.name && it.createdAt == pl.createdAt }
                                if (rawIdx >= 0) all[rawIdx] = all[rawIdx].copy(pinned = !pl.pinned)
                                writePlaylists(all)
                            }
                            MenuRow(Icons.Filled.Delete, strings.delete) {
                                playlistMenuIndex = null
                                playlistToDelete = pl
                                showDeletePlaylistConfirm = true
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { playlistMenuIndex = null }) { Text(strings.cancel) }
                },
            )
        }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun M3PlaylistListView(
    playlists: List<LocalPlaylist>,
    tracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    likedIds: List<Long>,
    layout: Int,
    hasMiniPlayer: Boolean,
    onOpen: (Int) -> Unit,
    onPlay: (LocalPlaylist) -> Unit,
    onRemix: (LocalPlaylist) -> Unit,
    onMenu: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.playlists) },
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(Icons.Filled.Add, contentDescription = strings.createPlaylist)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = strings.createPlaylist)
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (playlists.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = strings.noPlaylistsYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val sortedPlaylists = remember(playlists) {
                    val (pinned, unpinned) = playlists.partition { it.pinned }
                    pinned + unpinned
                }
                val hasPinned = playlists.any { it.pinned }
                if (layout == LayoutMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            end = 12.dp,
                            bottom = if (hasMiniPlayer) 200.dp else 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "pinned_header", span = { GridItemSpan(2) }) {
                            AnimatedVisibility(
                                visible = hasPinned,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = strings.pinnedPlaylists,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        items(
                            count = sortedPlaylists.size,
                            key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }
                        ) { i ->
                            val pl = sortedPlaylists[i]
                            val realIdx = playlists.indexOf(pl)
                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                            M3PlaylistGridCard(
                                playlist = pl,
                                plTracks = plTracks,
                                strings = strings,
                                onClick = { onOpen(realIdx) },
                                onMenu = { onMenu(realIdx) },
                                onPlay = { onPlay(pl) },
                                onRemix = { onRemix(pl) },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            end = 12.dp,
                            bottom = if (hasMiniPlayer) 200.dp else 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "pinned_header") {
                            AnimatedVisibility(
                                visible = hasPinned,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = strings.pinnedPlaylists,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        items(
                            count = sortedPlaylists.size,
                            key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }
                        ) { i ->
                            val pl = sortedPlaylists[i]
                            val realIdx = playlists.indexOf(pl)
                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                            M3PlaylistListCard(
                                playlist = pl,
                                plTracks = plTracks,
                                strings = strings,
                                onClick = { onOpen(realIdx) },
                                onMenu = { onMenu(realIdx) },
                                onPlay = { onPlay(pl) },
                                onRemix = { onRemix(pl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3PlaylistListCard(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onPlay: () -> Unit,
    onRemix: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverUri != null) {
                    AsyncImage(
                        model = playlist.coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${plTracks.size} ${strings.trackCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.play,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onRemix) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = strings.remix,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
        }
    }
}

@Composable
private fun M3PlaylistGridCard(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onPlay: () -> Unit,
    onRemix: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverUri != null) {
                    AsyncImage(
                        model = playlist.coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${plTracks.size} ${strings.trackCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = strings.play,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onRemix, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = strings.remix,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onMenu, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
