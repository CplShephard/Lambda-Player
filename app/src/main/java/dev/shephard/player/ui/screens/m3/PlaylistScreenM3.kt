// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 + M3 padding/radius + grid 3-dot
package dev.shephard.player.ui.screens.m3

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.LineageGridMediaItem
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.LineageNoElements
import dev.shephard.player.ui.components.m3.LineageSortingChip
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.navigation.PageTransitions
import dev.shephard.player.ui.navigation.SubmenuNavGuard
import dev.shephard.player.ui.screens.LocalPlaylist
import dev.shephard.player.ui.screens.encodePlaylists
import dev.shephard.player.ui.screens.ensureLikedSongsPlaylist
import dev.shephard.player.ui.screens.parsePlaylists
import dev.shephard.player.ui.screens.resolvePlaylistTracks
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreenM3(
    libraryViewModel: LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
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
                context.grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(strings.createPlaylist) },
            text = {
                Column {
                    Box(
                        modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (newCoverUri != null) {
                            AsyncImage(model = newCoverUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, label = { Text(strings.playlistName) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            val newPl = LocalPlaylist(name = name, trackIds = emptyList(), coverUri = newCoverUri?.toString())
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

    if (editPlaylistIndex != null) {
        val editingPl = playlists.getOrNull(editPlaylistIndex ?: -1)
        if (editingPl != null) {
            AlertDialog(
                onDismissRequest = { editPlaylistIndex = null },
                title = { Text(strings.editPlaylist) },
                text = {
                    OutlinedTextField(value = editPlaylistName, onValueChange = { editPlaylistName = it }, singleLine = true, label = { Text(strings.playlistName) })
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newNameTrim = editPlaylistName.trim()
                            if (newNameTrim.isNotEmpty()) {
                                val all = rawPlaylists.toMutableList()
                                val idx = all.indexOfFirst { it.name == editingPl.name && it.createdAt == editingPl.createdAt }
                                if (idx >= 0) all[idx] = all[idx].copy(name = newNameTrim)
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

    if (showDeletePlaylistConfirm) {
        val pl = playlistToDelete
        AlertDialog(
            onDismissRequest = { showDeletePlaylistConfirm = false; playlistToDelete = null },
            title = { Text(strings.delete) },
            text = { Text(strings.removePlaylistConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                TextButton(onClick = { showDeletePlaylistConfirm = false; playlistToDelete = null }) { Text(strings.cancel) }
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        TopAppBar(
                            title = { Text(strings.playlists) },
                            actions = {
                                IconButton(onClick = { showCreate = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = strings.createPlaylist)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = { showCreate = true },
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text(strings.createPlaylist) },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            expanded = true,
                            modifier = Modifier.padding(bottom = if (hasMiniPlayer) 80.dp else 0.dp)
                        )
                    }
                ) { innerPadding ->
                    if (playlists.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                            LineageNoElements(
                                icon = Icons.Filled.LibraryMusic,
                                message = strings.noPlaylistsYet,
                                actionLabel = strings.createPlaylist,
                                onAction = { showCreate = true }
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .padding(top = innerPadding.calculateTopPadding()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LineageSortingChip(
                                    label = strings.list,
                                    selected = playlistsLayout == LayoutMode.LIST,
                                    onClick = { scope.launch { prefs.setPlaylistsLayout(LayoutMode.LIST) } }
                                )
                                LineageSortingChip(
                                    label = strings.grid,
                                    selected = playlistsLayout == LayoutMode.GRID,
                                    onClick = { scope.launch { prefs.setPlaylistsLayout(LayoutMode.GRID) } }
                                )
                            }

                            val sortedPlaylists = remember(playlists) {
                                val (pinned, unpinned) = playlists.partition { it.pinned }
                                pinned + unpinned
                            }

                            if (playlistsLayout == LayoutMode.GRID) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 8.dp,
                                        end = 8.dp,
                                        bottom = if (hasMiniPlayer) 160.dp else 80.dp
                                    )
                                ) {
                                    gridItems(sortedPlaylists, key = { it.name + "_" + it.createdAt }) { pl ->
                                        val realIdx = playlists.indexOf(pl)
                                        val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                                        LineageGridMediaItem(
                                            headline = pl.name,
                                            subhead = "${plTracks.size} tracks",
                                            thumbnailModel = pl.coverUri,
                                            placeholderIcon = Icons.Filled.QueueMusic,
                                            trailingIcon = Icons.Filled.MoreVert,
                                            onTrailingClick = { playlistMenuIndex = realIdx },
                                            onClick = { playlistDetailGuard.push(openIndex, realIdx) { openIndex = realIdx } }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = if (hasMiniPlayer) 160.dp else 80.dp, top = 4.dp)
                                ) {
                                    items(sortedPlaylists.size, key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }) { i ->
                                        val pl = sortedPlaylists[i]
                                        val realIdx = playlists.indexOf(pl)
                                        val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                        ) {
                                            LineageListItemWithThumbnail(
                                                headline = pl.name,
                                                supporting = "${plTracks.size} ${strings.trackCount}",
                                                thumbnailModel = pl.coverUri,
                                                placeholderIcon = Icons.Filled.QueueMusic,
                                                trailingIcon = Icons.Filled.MoreVert,
                                                onClick = { playlistDetailGuard.push(openIndex, realIdx) { openIndex = realIdx } },
                                                onTrailingClick = { playlistMenuIndex = realIdx }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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

    playlistMenuIndex?.let { menuIdx ->
        val pl = playlists.getOrNull(menuIdx)
        if (pl != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { playlistMenuIndex = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text(pl.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    SegmentedColumn {
                        item {
                            BaseWidget(icon = Icons.Filled.PlayArrow, title = strings.play, onClick = {
                                playlistMenuIndex = null
                                val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                                if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name)
                            })
                        }
                        item {
                            BaseWidget(icon = Icons.Filled.Shuffle, title = strings.remix, onClick = {
                                playlistMenuIndex = null
                                val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                                if (plTracks.isNotEmpty()) {
                                    if (pl.isSystem) onPlaylistRemixClick(plTracks.shuffled(), strings.likedSongs)
                                    else onPlaylistRemixClick(plTracks.shuffled(), pl.name)
                                }
                            })
                        }
                        if (!pl.isSystem) {
                            item {
                                BaseWidget(icon = Icons.Filled.Edit, title = strings.editPlaylist, onClick = {
                                    playlistMenuIndex = null
                                    editPlaylistName = pl.name
                                    editPlaylistIndex = menuIdx
                                })
                            }
                            item {
                                BaseWidget(icon = if (pl.pinned) Icons.Filled.Pin else Icons.Filled.PushPin, title = if (pl.pinned) strings.unpinPlaylist else strings.pinPlaylist, onClick = {
                                    playlistMenuIndex = null
                                    val all = rawPlaylists.toMutableList()
                                    val rawIdx = all.indexOfFirst { it.name == pl.name && it.createdAt == pl.createdAt }
                                    if (rawIdx >= 0) all[rawIdx] = all[rawIdx].copy(pinned = !pl.pinned)
                                    writePlaylists(all)
                                })
                            }
                            item {
                                BaseWidget(icon = Icons.Filled.Delete, title = strings.delete, onClick = {
                                    playlistMenuIndex = null
                                    playlistToDelete = pl
                                    showDeletePlaylistConfirm = true
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
