@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package dev.shephard.player.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.rememberBounceOverscrollEffect
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class LocalPlaylist(
    val name: String,
    val trackIds: List<Long>,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortMode: String = "custom",
    val pinned: Boolean = false,
    val isSystem: Boolean = false
)

internal fun parsePlaylists(json: String): List<LocalPlaylist> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        val ids = obj.optJSONArray("trackIds")
        val list = if (ids != null) (0 until ids.length()).map { ids.getLong(it) } else emptyList()
        LocalPlaylist(
            obj.optString("name"),
            list,
            obj.optString("coverUri").takeIf { it.isNotEmpty() },
            obj.optLong("createdAt", System.currentTimeMillis()),
            obj.optString("sortMode", "custom"),
            obj.optBoolean("pinned", false),
            obj.optBoolean("isSystem", false)
        )
    }
} catch (_: Exception) { emptyList() }

internal fun encodePlaylists(items: List<LocalPlaylist>): String {
    val arr = JSONArray()
    items.forEach { pl ->
        val obj = JSONObject()
        obj.put("name", pl.name)
        obj.put("coverUri", pl.coverUri ?: "")
        obj.put("createdAt", pl.createdAt)
        obj.put("sortMode", pl.sortMode)
        obj.put("pinned", pl.pinned)
        obj.put("isSystem", pl.isSystem)
        val ids = JSONArray()
        pl.trackIds.forEach { ids.put(it) }
        obj.put("trackIds", ids)
        arr.put(obj)
    }
    return arr.toString()
}

internal fun ensureLikedSongsPlaylist(playlists: List<LocalPlaylist>, strings: dev.shephard.player.ui.i18n.Strings): List<LocalPlaylist> {
    return if (playlists.none { it.isSystem }) {
        listOf(LocalPlaylist(strings.likedSongs, emptyList(), isSystem = true, createdAt = 0L)) + playlists
    } else {
        playlists.map { if (it.isSystem) it.copy(name = strings.likedSongs) else it }
    }
}

@Composable
fun PlaylistScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    hasMiniPlayer: Boolean = false,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> }
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
            JSONArray(likedSongIdsJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } }
        } catch (_: Exception) { emptyList() }
    }
    val rawPlaylists = remember(json) { parsePlaylists(json) }
    val playlists = remember(rawPlaylists, strings) { ensureLikedSongsPlaylist(rawPlaylists, strings) }

    LaunchedEffect(playlists) {
        if (rawPlaylists.isNotEmpty()) {
            val encoded = encodePlaylists(playlists)
            val currentEncoded = encodePlaylists(rawPlaylists)
            if (encoded != currentEncoded) {
                prefs.setPlaylistsJson(encoded)
            }
        }
    }

    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var openIndex by remember { mutableStateOf<Int?>(null) }
    var trackPickerForIndex by remember { mutableStateOf<Int?>(null) }
    var pickerSelected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showCoverPickerForIndex by remember { mutableStateOf<Int?>(null) }
    var playlistMenuIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistName by remember { mutableStateOf("") }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            val idx = showCoverPickerForIndex
            if (idx != null && idx in playlists.indices) {
                val pl = playlists[idx]
                val updated = pl.copy(coverUri = uri.toString())
                val all = playlists.toMutableList()
                all[idx] = updated
                scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
            }
            showCoverPickerForIndex = null
        }
    }

    if (openIndex != null && openIndex !in playlists.indices) {
        openIndex = null
    }

    if (playlistMenuIndex != null && playlistMenuIndex !in playlists.indices) {
        playlistMenuIndex = null
    }

    LaunchedEffect(playlists) {
        if (editPlaylistIndex != null && editPlaylistIndex !in playlists.indices) {
            editPlaylistIndex = null
        }
    }

    androidx.activity.compose.BackHandler(enabled = openIndex != null) { openIndex = null }

    androidx.compose.animation.AnimatedContent(
        targetState = openIndex,
        transitionSpec = {
            if (targetState != null) {
                // opening detail: slide in from right
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn(),
                    initialContentExit = androidx.compose.animation.slideOutHorizontally { -it / 3 } + androidx.compose.animation.fadeOut()
                )
            } else {
                // going back: slide in from left
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = androidx.compose.animation.slideInHorizontally { -it / 3 } + androidx.compose.animation.fadeIn(),
                    initialContentExit = androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut()
                )
            }
        },
        label = "playlistNav"
    ) { idx ->
    if (idx == null) {
        PlaylistListView(
            playlists = playlists,
            tracks = tracks,
            strings = strings,
            layout = playlistsLayout,
            likedIds = likedIds,
            onOpen = { openIndex = it },
            onPlay = { pl ->
                val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name)
            },
            onMenu = { playlistMenuIndex = it },
            onCreate = { showCreate = true; newName = "" }
        )
    } else {
        val pl = playlists.getOrNull(idx)
        if (pl == null) {
            openIndex = null
        } else {
            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
            PlaylistDetailView(
                playlist = pl,
                allTracks = tracks,
                plTracks = plTracks,
                strings = strings,
                onBack = { openIndex = null },
                onTrackClick = { list, i -> onTrackClick(list, i, if (pl.isSystem) strings.likedSongs else pl.name) },
                onPlayAll = { if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name) },
                onRemoveTrack = { trackId ->
                    if (pl.isSystem) {
                        val newLiked = likedIds - trackId
                        val json = JSONArray().apply { newLiked.forEach { put(it) } }.toString()
                        scope.launch { prefs.setLikedSongIds(json) }
                    } else {
                        val updated = pl.copy(trackIds = pl.trackIds.filterNot { it == trackId })
                        val all = playlists.toMutableList()
                        all[idx] = updated
                        scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                    }
                },
                onAddTracks = {
                    pickerSelected = pl.trackIds.toSet()
                    trackPickerForIndex = idx
                },
                onPickCover = {
                    showCoverPickerForIndex = idx
                    coverPicker.launch(arrayOf("image/*"))
                },
                onReorder = { newOrder ->
                    val updated = pl.copy(trackIds = newOrder.map { it.id }, sortMode = "custom")
                    val all = playlists.toMutableList()
                    all[idx] = updated
                    scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                },
                onChangeSort = { mode ->
                    val updated = pl.copy(sortMode = mode)
                    val all = playlists.toMutableList()
                    all[idx] = updated
                    scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                }
            )
        }
    }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(strings.createPlaylist) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        val next = playlists + LocalPlaylist(name, emptyList(), createdAt = System.currentTimeMillis())
                        scope.launch { prefs.setPlaylistsJson(encodePlaylists(next)) }
                    }
                    showCreate = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(strings.cancel) }
            }
        )
    }

    val pickerIdx = trackPickerForIndex
    if (pickerIdx != null) {
        AlertDialog(
            onDismissRequest = { trackPickerForIndex = null },
            title = { Text(strings.addTracks) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .height(400.dp)
                        .overscroll(rememberBounceOverscrollEffect())
                ) {
                    items(tracks) { t ->
                        val checked = t.id in pickerSelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickerSelected = if (checked)
                                        pickerSelected - t.id
                                    else
                                        pickerSelected + t.id
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    pickerSelected = if (on)
                                        pickerSelected + t.id
                                    else
                                        pickerSelected - t.id
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t.title,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = t.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pl = playlists[pickerIdx]
                    val existingIds = pl.trackIds.toMutableList()
                    val toAdd = pickerSelected - pl.trackIds.toSet()
                    val toRemove = pl.trackIds.toSet() - pickerSelected
                    val newIds = existingIds.filterNot { it in toRemove } + toAdd
                    val updated = pl.copy(trackIds = newIds)
                    val all = playlists.toMutableList()
                    all[pickerIdx] = updated
                    scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                    trackPickerForIndex = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { trackPickerForIndex = null }) { Text(strings.cancel) }
            }
        )
    }

    // Playlist menu drawer
    val menuIdx = playlistMenuIndex
    if (menuIdx != null) {
        val pl = playlists[menuIdx]
        val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { playlistMenuIndex = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (pl.isSystem) strings.likedSongs else pl.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val dateStr = remember(pl.createdAt) {
                            if (pl.createdAt > 0) SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(pl.createdAt)) else ""
                        }
                        if (dateStr.isNotEmpty()) {
                            Text(
                                text = "${strings.createdAt}: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${plTracks.size} ${strings.trackCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .bounceClick {
                            editPlaylistIndex = menuIdx
                            editPlaylistName = pl.name
                            playlistMenuIndex = null
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.editPlaylist, color = MaterialTheme.colorScheme.onBackground)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .bounceClick {
                            val pinnedCount = playlists.count { it.pinned }
                            val all = playlists.toMutableList()
                            if (pl.pinned) {
                                all[menuIdx] = pl.copy(pinned = false)
                            } else if (pinnedCount < 3) {
                                all[menuIdx] = pl.copy(pinned = true)
                            }
                            scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                            playlistMenuIndex = null
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (pl.pinned) Icons.Filled.PinDrop else Icons.Filled.PushPin, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(if (pl.pinned) strings.unpinPlaylist else strings.pinPlaylist, color = MaterialTheme.colorScheme.onBackground)
                }
                if (!pl.isSystem) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClick {
                                val all = playlists.toMutableList().also { it.removeAt(menuIdx) }
                                scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                                playlistMenuIndex = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.delete, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Edit playlist name
    val editIdx = editPlaylistIndex
    if (editIdx != null) {
        AlertDialog(
            onDismissRequest = { editPlaylistIndex = null },
            title = { Text(strings.editPlaylist) },
            text = {
                OutlinedTextField(
                    value = editPlaylistName,
                    onValueChange = { editPlaylistName = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = editPlaylistName.trim()
                    if (name.isNotEmpty() && editIdx in playlists.indices) {
                        val pl = playlists[editIdx]
                        val all = playlists.toMutableList()
                        all[editIdx] = pl.copy(name = name)
                        scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                    }
                    editPlaylistIndex = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editPlaylistIndex = null }) { Text(strings.cancel) }
            }
        )
    }
}

private fun resolvePlaylistTracks(pl: LocalPlaylist, tracks: List<AudioTrack>, likedIds: List<Long> = emptyList()): List<AudioTrack> {
    if (pl.isSystem) {
        val trackMap = tracks.associateBy { it.id }
        return likedIds.mapNotNull { trackMap[it] }
    }
    val mapped = pl.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
    return when (pl.sortMode) {
        "alphabetical" -> mapped.sortedBy { it.title.lowercase() }
        "artist" -> mapped.sortedBy { it.artist.lowercase() }
        "timeAdded" -> mapped
        else -> mapped
    }
}

@Composable
private fun PlaylistListView(
    playlists: List<LocalPlaylist>,
    tracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    layout: Int,
    likedIds: List<Long>,
    onOpen: (Int) -> Unit,
    onPlay: (LocalPlaylist) -> Unit,
    onMenu: (Int) -> Unit,
    onCreate: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
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
            if (layout == LayoutMode.GRID) {
                val pinnedPlaylists = playlists.filter { it.pinned }
                val unpinnedPlaylists = playlists.filter { !it.pinned }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .overscroll(rememberBounceOverscrollEffect()),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pinnedPlaylists.size >= 1) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = strings.pinnedPlaylists,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(pinnedPlaylists.size) { i ->
                            val pl = pinnedPlaylists[i]
                            val realIdx = playlists.indexOf(pl)
                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                            PlaylistGridCard(playlist = pl, plTracks = plTracks, strings = strings,
                                onClick = { onOpen(realIdx) }, onMenu = { onMenu(realIdx) }, onPlay = { onPlay(pl) })
                        }
                        item(span = { GridItemSpan(2) }) {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    items(unpinnedPlaylists.size) { i ->
                        val pl = unpinnedPlaylists[i]
                        val realIdx = playlists.indexOf(pl)
                        val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                        PlaylistGridCard(playlist = pl, plTracks = plTracks, strings = strings,
                            onClick = { onOpen(realIdx) }, onMenu = { onMenu(realIdx) }, onPlay = { onPlay(pl) })
                    }
                }
            } else {
                val pinnedPlaylists = playlists.filter { it.pinned }
                val unpinnedPlaylists = playlists.filter { !it.pinned }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .overscroll(rememberBounceOverscrollEffect()),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pinnedPlaylists.size >= 1) {
                        item {
                            Text(
                                text = strings.pinnedPlaylists,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(pinnedPlaylists.size) { i ->
                            val pl = pinnedPlaylists[i]
                            val realIdx = playlists.indexOf(pl)
                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                            PlaylistListCard(playlist = pl, plTracks = plTracks, strings = strings,
                                onClick = { onOpen(realIdx) }, onMenu = { onMenu(realIdx) }, onPlay = { onPlay(pl) })
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                    items(unpinnedPlaylists.size) { i ->
                        val pl = unpinnedPlaylists[i]
                        val realIdx = playlists.indexOf(pl)
                        val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                        PlaylistListCard(playlist = pl, plTracks = plTracks, strings = strings,
                            onClick = { onOpen(realIdx) }, onMenu = { onMenu(realIdx) }, onPlay = { onPlay(pl) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 24.dp)
                .bounceClick(onClick = onCreate),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.createPlaylist)
        }
    }
}

@Composable
private fun PlaylistListCard(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                )
            )
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                val coverUri = playlist.coverUri?.let { Uri.parse(it) }
                val firstArt = coverUri ?: plTracks.firstOrNull()?.albumArtUri
                var artLoaded by remember(firstArt) { mutableStateOf(false) }
                if (playlist.isSystem) {
                    // Fixed Liked Songs cover
                    Box(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                )
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                } else {
                if (firstArt != null) {
                    AsyncImage(
                        model = firstArt,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { artLoaded = it is AsyncImagePainter.State.Success }
                    )
                }
                if (!artLoaded) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (playlist.isSystem) strings.likedSongs else playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${plTracks.size} ${strings.trackCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BouncyIconButton(
                onClick = onPlay,
                icon = Icons.Filled.PlayArrow,
                contentDescription = strings.play,
                tint = MaterialTheme.colorScheme.primary
            )
            BouncyIconButton(
                onClick = onMenu,
                icon = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistGridCard(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                )
            )
            .bounceClick { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val coverUri = playlist.coverUri?.let { Uri.parse(it) }
            val firstArt = coverUri ?: plTracks.firstOrNull()?.albumArtUri
            var artLoaded by remember(firstArt) { mutableStateOf(false) }
            if (playlist.isSystem) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                            )
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            } else {
            if (firstArt != null) {
                AsyncImage(
                    model = firstArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onState = { artLoaded = it is AsyncImagePainter.State.Success }
                )
            }
            if (!artLoaded) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            }
            BouncyIconButton(
                onClick = onMenu,
                icon = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .bounceClick { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.play,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (playlist.isSystem) strings.likedSongs else playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${plTracks.size} ${strings.trackCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: LocalPlaylist,
    allTracks: List<AudioTrack>,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onBack: () -> Unit,
    onTrackClick: (List<AudioTrack>, Int) -> Unit,
    onPlayAll: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onAddTracks: () -> Unit,
    onPickCover: () -> Unit = {},
    onReorder: (List<AudioTrack>) -> Unit = {},
    onChangeSort: (String) -> Unit = {}
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightDp = 64.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // Mutable working list used for live (drag-to-reorder) updates in custom mode.
    val reorderItems = remember { mutableStateListOf<AudioTrack>() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Keep the working list in sync with the source whenever the playlist tracks change.
    LaunchedEffect(plTracks) {
        if (draggedIndex < 0) {
            reorderItems.clear()
            reorderItems.addAll(plTracks)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overscroll(rememberBounceOverscrollEffect()),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BouncyIconButton(
                        onClick = onBack,
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = strings.cancel,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (playlist.isSystem) strings.likedSongs else playlist.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${plTracks.size} ${strings.trackCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(if (!playlist.isSystem) Modifier.clickable { onPickCover() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (playlist.isSystem) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                    )
                                )),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(80.dp))
                        }
                    } else {
                    val coverUri = playlist.coverUri?.let { Uri.parse(it) }
                    val displayArt = coverUri ?: plTracks.firstOrNull()?.albumArtUri
                    if (displayArt != null) {
                        AsyncImage(
                            model = displayArt,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    if (!playlist.isSystem) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    }
                    } // end else (!isSystem)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = plTracks.isNotEmpty()) { onPlayAll() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = strings.play,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.play,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!playlist.isSystem) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onAddTracks() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = strings.addTracks,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.addTracks,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                // Sort selector
                if (!playlist.isSystem) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "custom" to strings.customOrder,
                            "alphabetical" to strings.alphabetical,
                            "artist" to strings.sortByArtist,
                            "timeAdded" to strings.timeAdded
                        ).forEach { (mode, label) ->
                            val selected = playlist.sortMode == mode
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onChangeSort(mode) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (plTracks.isEmpty()) {
                item {
                    Text(
                        text = if (playlist.isSystem) "No liked songs yet" else "No tracks yet. Tap \"${strings.addTracks}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                if (playlist.sortMode == "custom" && !playlist.isSystem) {
                    itemsIndexed(reorderItems, key = { _, t -> t.id }) { i, t ->
                        DraggablePlaylistTrackRow(
                            track = t,
                            isDragged = i == draggedIndex,
                            dragOffsetY = dragOffsetY,
                            onTrackClick = { onTrackClick(reorderItems.toList(), i) },
                            onRemove = { onRemoveTrack(t.id) },
                            onDragStart = { draggedIndex = i; dragOffsetY = 0f },
                            onDrag = { amount ->
                                dragOffsetY += amount
                                val cur = draggedIndex
                                if (cur >= 0) {
                                    if (dragOffsetY > itemHeightPx / 2 && cur < reorderItems.size - 1) {
                                        reorderItems.add(cur + 1, reorderItems.removeAt(cur))
                                        draggedIndex = cur + 1
                                        dragOffsetY -= itemHeightPx
                                    } else if (dragOffsetY < -itemHeightPx / 2 && cur > 0) {
                                        reorderItems.add(cur - 1, reorderItems.removeAt(cur))
                                        draggedIndex = cur - 1
                                        dragOffsetY += itemHeightPx
                                    }
                                }
                            },
                            onDragEnd = {
                                if (reorderItems.toList() != plTracks) {
                                    onReorder(reorderItems.toList())
                                }
                                draggedIndex = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = { draggedIndex = -1; dragOffsetY = 0f }
                        )
                    }
                } else {
                    itemsIndexed(plTracks) { i, t ->
                        PlaylistTrackRow(
                            track = t,
                            onClick = { onTrackClick(plTracks, i) },
                            onRemove = { onRemoveTrack(t.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: AudioTrack,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                )
            )
            .bounceClick { onClick() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            var loaded by remember(track.id) { mutableStateOf(false) }
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                onState = { loaded = it is AsyncImagePainter.State.Success }
            )
            if (!loaded) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(track.formattedDuration(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun DraggablePlaylistTrackRow(
    track: AudioTrack,
    isDragged: Boolean,
    dragOffsetY: Float,
    onTrackClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                if (isDragged) {
                    translationY = dragOffsetY
                    shadowElevation = 12f
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .zIndex(if (isDragged) 1f else 0f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDragged) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .bounceClick { onTrackClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            var loaded by remember(track.id) { mutableStateOf(false) }
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                onState = { loaded = it is AsyncImagePainter.State.Success }
            )
            if (!loaded) {
                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(track.formattedDuration(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(28.dp)
                .pointerInput(track.id) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() }
                    ) { change, dragAmount: androidx.compose.ui.geometry.Offset ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                }
        )
    }
}
