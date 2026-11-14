// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 + M3 padding/radius + grid 3-dot
package dev.shephard.player.ui.screens.m3

import dev.shephard.player.ui.components.m3.m3CardPress
import dev.shephard.player.ui.components.m3.m3CardPressLayer
import androidx.compose.foundation.interaction.MutableInteractionSource

import dev.shephard.player.ui.components.LocalBottomOverlayHeight
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import dev.shephard.player.ui.components.PlaylistCoverArt
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.shephard.player.ui.components.m3.rememberExpressiveTopBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.shephard.player.ui.components.m3.M3SmallTopBar
import dev.shephard.player.ui.components.m3.M3CoverEditor
import dev.shephard.player.ui.components.m3.m3ItemCardColors
import dev.shephard.player.ui.components.m3.m3TopBarColors
import dev.shephard.player.ui.components.m3.rememberCoverPicker
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.LineageNoElements
import dev.shephard.player.ui.components.m3.LineageSortingChip
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.i18n.LocalStrings
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
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> },
    onOpenPlaylistDetail: (Int) -> Unit = {},
    embedded: Boolean = false,
    query: String = "",
    sortMode: Int = 0,
    descending: Boolean = false,
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
    var playlistMenuIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistName by remember { mutableStateOf("") }
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<LocalPlaylist?>(null) }

    var newCoverUri by remember { mutableStateOf<Uri?>(null) }
    var newCoverCropOutputUri by remember { mutableStateOf<Uri?>(null) }

    fun playPlaylist(pl: LocalPlaylist, plTracks: List<AudioTrack>) {
        if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name)
    }

    fun writePlaylists(all: List<LocalPlaylist>) {
        scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
    }

    fun setPlaylistCover(pl: LocalPlaylist, cover: Uri?) {
        val all = rawPlaylists.toMutableList()
        val i = all.indexOfFirst { it.name == pl.name && it.createdAt == pl.createdAt && it.isSystem == pl.isSystem }
        if (i >= 0) {
            all[i] = all[i].copy(coverUri = cover?.toString())
            writePlaylists(all)
        }
    }

    // The playlist whose cover is being replaced; remembered across the picker/crop round-trip.
    var coverTarget by remember { mutableStateOf<LocalPlaylist?>(null) }
    val editCoverPicker = rememberCoverPicker { uri ->
        coverTarget?.let { setPlaylistCover(it, uri) }
        coverTarget = null
    }
    val pickEditCover: () -> Unit = {
        coverTarget = playlists.getOrNull(editPlaylistIndex ?: -1)
        editCoverPicker()
    }

    fun deletePlaylist(pl: LocalPlaylist) {
        val all = rawPlaylists.filterNot { it.name == pl.name && it.createdAt == pl.createdAt && it.isSystem == pl.isSystem }
        writePlaylists(all)
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
        if (dev.shephard.player.player.ImagePersistence.isAnimatedCover(context, sourceUri)) {
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    M3CoverEditor(
                        model = newCoverUri,
                        placeholderIcon = Icons.Filled.QueueMusic,
                        hasCustomCover = newCoverUri != null,
                        onPick = { newCoverPicker.launch(arrayOf("image/*", "video/mp4")) },
                        onRemove = { newCoverUri = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
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
            val editTracks = remember(editingPl, tracks, likedIds) { resolvePlaylistTracks(editingPl, tracks, likedIds) }
            dev.shephard.player.ui.screens.PlaylistDetailEditor(false, editingPl, editTracks,
                onDismiss = { editPlaylistIndex = null },
                onSave = { name ->
                    editPlaylistIndex = null
                    scope.launch { prefs.updatePlaylistsJson { json ->
                        val all = parsePlaylists(json).toMutableList()
                        val i = all.indexOfFirst { it.name == editingPl.name && it.createdAt == editingPl.createdAt }
                        if (i >= 0) all[i] = all[i].copy(name = name)
                        encodePlaylists(all)
                    } }
                },
                onPickCover = pickEditCover,
                onRemoveCover = { setPlaylistCover(editingPl, null) })
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

    if (playlistMenuIndex != null && playlistMenuIndex !in playlists.indices) playlistMenuIndex = null

    // Real height of the mini player + dock (measured in NavGraph), so the FAB always clears them.
    val overlayHeight by LocalBottomOverlayHeight.current
    val fabLift by androidx.compose.animation.core.animateDpAsState(
        targetValue = overlayHeight + 8.dp,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "playlistFabLift"
    )

    // Playlist detail is a top-level submenu (PlaylistDetailRoute in NavGraph), exactly like the
    // settings submenus: it covers the pager, dock and mini player by z-order instead of living
    // inside this page. Opening it just pushes the route.
    val m3TopBarState = rememberExpressiveTopBarState()
    Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(m3TopBarState.connection),
                    containerColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        if (!embedded) M3SmallTopBar(
                            state = m3TopBarState,
                            title = { Text(strings.playlists) },
                            colors = m3TopBarColors(),
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
                            // Lives above the whole bottom stack (mini player + dock). Animated so it
                            // glides up/down together with the mini player.
                            modifier = Modifier.padding(bottom = fabLift)
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
                            if (!embedded) Row(
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

                            val sortedPlaylists = remember(playlists, query, sortMode, descending) {
                                val filtered = playlists.filter { it.name.contains(query, ignoreCase = true) }
                                val sorted = when (sortMode) {
                                    3 -> filtered.sortedBy { it.createdAt }
                                    4 -> filtered.sortedBy { it.trackIds.size }
                                    else -> filtered.sortedBy { it.name.lowercase() }
                                }.let { if (descending) it.reversed() else it }
                                val (pinned, unpinned) = sorted.partition { it.pinned }
                                pinned + unpinned
                            }

                            // Grid <-> list: the content slides sideways while it scales and fades, so the
                            // switch reads as one motion instead of a cut.
                            if (query.isNotBlank() && sortedPlaylists.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    dev.shephard.player.ui.screens.LibraryEmptyResults(false, "${strings.noSearchResults} ${strings.playlists}")
                                }
                            } else AnimatedContent(
                                targetState = playlistsLayout,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                transitionSpec = {
                                    val toGrid = targetState == LayoutMode.GRID
                                    (slideInHorizontally(animationSpec = tween(380)) { full -> if (toGrid) full / 5 else -full / 5 } +
                                        fadeIn(animationSpec = tween(260)) +
                                        scaleIn(animationSpec = tween(380), initialScale = 0.94f)) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(380)) { full -> if (toGrid) -full / 5 else full / 5 } +
                                            fadeOut(animationSpec = tween(200)) +
                                            scaleOut(animationSpec = tween(380), targetScale = 0.94f)) using
                                        SizeTransform(clip = false)
                                },
                                label = "playlistLayoutSwitch",
                            ) { layout ->
                                if (layout == LayoutMode.GRID) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            start = 8.dp,
                                            end = 8.dp,
                                            top = 4.dp,
                                            bottom = 220.dp
                                        )
                                    ) {
                                        items(count = sortedPlaylists.size, key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }) { i ->
                                            val pl = sortedPlaylists[i]
                                            val realIdx = playlists.indexOf(pl)
                                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                                            Box(modifier = Modifier.animateItem()) {
                                                PlaylistGridCardM3(
                                                    playlist = pl,
                                                    plTracks = plTracks,
                                                    strings = strings,
                                                    onClick = { onOpenPlaylistDetail(realIdx) },
                                                    onMore = { playlistMenuIndex = realIdx },
                                                    onPlay = { playPlaylist(pl, plTracks) },
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 220.dp, top = 4.dp)
                                    ) {
                                        items(count = sortedPlaylists.size, key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }) { i ->
                                            val pl = sortedPlaylists[i]
                                            val realIdx = playlists.indexOf(pl)
                                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                                            Box(modifier = Modifier.animateItem()) {
                                                PlaylistRowM3(
                                                    playlist = pl,
                                                    plTracks = plTracks,
                                                    strings = strings,
                                                    onClick = { onOpenPlaylistDetail(realIdx) },
                                                    onMore = { playlistMenuIndex = realIdx },
                                                    onPlay = { playPlaylist(pl, plTracks) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
    playlistMenuIndex?.let { menuIdx ->
        val pl = playlists.getOrNull(menuIdx)
        if (pl != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            M3BottomSheetWrapper(
            onDismissRequest = { playlistMenuIndex = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                val closeThen = dev.shephard.player.ui.components.rememberCloseDrawerThen()
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text(pl.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    SegmentedColumn {
                        // Play / Remix live on the row itself (Play button) and the detail page.
                        if (!pl.isSystem) {
                            item {
                                BaseWidget(icon = Icons.Filled.Edit, title = strings.editPlaylist, onClick = {
                                    closeThen {
                                        editPlaylistName = pl.name
                                        editPlaylistIndex = menuIdx
                                    }
                                })
                            }
                            item {
                                BaseWidget(icon = if (pl.pinned) Icons.Filled.Pin else Icons.Filled.PushPin, title = if (pl.pinned) strings.unpinPlaylist else strings.pinPlaylist, onClick = {
                                    closeThen {
                                    val all = rawPlaylists.toMutableList()
                                    val rawIdx = all.indexOfFirst { it.name == pl.name && it.createdAt == pl.createdAt }
                                    if (rawIdx >= 0) all[rawIdx] = all[rawIdx].copy(pinned = !pl.pinned)
                                    writePlaylists(all)
                                    }
                                })
                            }
                            item {
                                BaseWidget(icon = Icons.Filled.Delete, title = strings.delete, onClick = {
                                    closeThen {
                                        playlistToDelete = pl
                                        showDeletePlaylistConfirm = true
                                    }
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

/** Playlist row for the Material 3 list layout: cover (or collage), name, count, Play and more. */
@Composable
private fun PlaylistRowM3(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onClick: () -> Unit,
    onMore: () -> Unit,
    onPlay: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = m3ItemCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .m3CardPress(shape = RoundedCornerShape(20.dp), onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                PlaylistCoverArt(
                    coverUri = playlist.coverUri,
                    tracks = plTracks,
                    placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                    placeholderIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    fallback = {
                        Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                    }
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (playlist.isSystem) strings.likedSongs else playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${plTracks.size} ${strings.trackCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlay, enabled = plTracks.isNotEmpty()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = strings.play, tint = MaterialTheme.colorScheme.primary)
            }
            // The menu only holds edit / pin / delete, none of which apply to system playlists.
            if (!playlist.isSystem) {
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = strings.menu, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Playlist tile for the Material 3 grid layout: square cover (or collage) with Play and more overlays. */
@Composable
private fun PlaylistGridCardM3(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onClick: () -> Unit,
    onMore: () -> Unit,
    onPlay: () -> Unit,
) {
    val cardInteractions = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .m3CardPress(shape = RoundedCornerShape(12.dp), overlayColor = Color.White,
                interactionSource = cardInteractions, showOverlay = false, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .m3CardPressLayer(cardInteractions, RoundedCornerShape(12.dp), Color.White)
        ) {
            PlaylistCoverArt(
                coverUri = playlist.coverUri,
                tracks = plTracks,
                placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                placeholderIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                fallback = {
                    Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(48.dp))
                }
            )
            if (!playlist.isSystem) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(onClick = onMore),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = strings.menu, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = plTracks.isNotEmpty(), onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = strings.play, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (playlist.isSystem) strings.likedSongs else playlist.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${plTracks.size} ${strings.trackCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
