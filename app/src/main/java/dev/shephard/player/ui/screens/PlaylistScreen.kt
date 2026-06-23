@file:OptIn(ExperimentalFoundationApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.rememberBounceOverscrollEffect
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class LocalPlaylist(
    val name: String,
    val trackIds: List<Long>,
    val coverUri: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

private fun parsePlaylists(json: String): List<LocalPlaylist> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        val ids = obj.optJSONArray("trackIds")
        val list = if (ids != null) (0 until ids.length()).map { ids.getLong(it) } else emptyList()
        LocalPlaylist(
            name = obj.optString("name"),
            trackIds = list,
            coverUri = obj.optString("coverUri").takeIf { it.isNotEmpty() },
            isPinned = obj.optBoolean("isPinned", false),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
} catch (_: Exception) { emptyList() }

private fun encodePlaylists(items: List<LocalPlaylist>): String {
    val arr = JSONArray()
    items.forEach { pl ->
        val obj = JSONObject()
        obj.put("name", pl.name)
        obj.put("coverUri", pl.coverUri ?: "")
        obj.put("isPinned", pl.isPinned)
        obj.put("createdAt", pl.createdAt)
        val ids = JSONArray()
        pl.trackIds.forEach { ids.put(it) }
        obj.put("trackIds", ids)
        arr.put(obj)
    }
    return arr.toString()
}

@Composable
fun PlaylistScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    onTrackClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    miniPlayerVisible: Boolean = false
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val tracks by libraryViewModel.tracks.collectAsState()

    // Playlist ekranına girilince şarkıları yükle (izin verilmişse)
    LaunchedEffect(Unit) {
        if (tracks.isEmpty()) libraryViewModel.loadTracks()
    }

    val json by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(json) { parsePlaylists(json) }

    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Açık olan playlist (detay sekmesi). null = liste görünümü.
    var openIndex by remember { mutableStateOf<Int?>(null) }

    // Track seçici dialogu için durum
    var trackPickerForIndex by remember { mutableStateOf<Int?>(null) }
    var pickerSelected by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Cover image picker state
    var showCoverPickerForIndex by remember { mutableStateOf<Int?>(null) }

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

    // Liste boşaldıysa veya index taşarsa detay sekmesini güvenle kapat.
    if (openIndex != null && openIndex !in playlists.indices) {
        openIndex = null
    }

    // Detay açıkken geri tuşu önce detayı kapatsın.
    BackHandler(enabled = openIndex != null) { openIndex = null }

    AnimatedContent(
        targetState = openIndex,
        transitionSpec = {
            val forward = targetState != null
            val dir = if (forward)
                AnimatedContentTransitionScope.SlideDirection.Left
            else
                AnimatedContentTransitionScope.SlideDirection.Right
            (slideIntoContainer(dir, spring(dampingRatio = 0.85f, stiffness = 380f)) + fadeIn())
                .togetherWith(
                    slideOutOfContainer(dir, spring(dampingRatio = 0.85f, stiffness = 380f)) + fadeOut()
                )
        },
        label = "playlistNav",
        modifier = Modifier.fillMaxSize()
    ) { idx ->
        if (idx == null) {
            // ----- LİSTE GÖRÜNÜMÜ -----
            PlaylistListView(
                playlists = playlists,
                tracks = tracks,
                strings = strings,
                onOpen = { openIndex = it },
                onPlay = { pl ->
                    val plTracks = pl.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
                    if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0)
                },
                onDelete = { delIdx ->
                    val next = playlists.toMutableList().also { it.removeAt(delIdx) }
                    scope.launch { prefs.setPlaylistsJson(encodePlaylists(next)) }
                },
                onCreate = { showCreate = true; newName = "" }
            )
        } else {
            // ----- DETAY GÖRÜNÜMÜ (AYRI SEKME) -----
            val pl = playlists.getOrNull(idx)
            if (pl == null) {
                openIndex = null
            } else {
                val plTracks = remember(pl, tracks) {
                    pl.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
                }
                PlaylistDetailView(
                    playlist = pl,
                    plTracks = plTracks,
                    strings = strings,
                    onBack = { openIndex = null },
                    onTrackClick = { i -> onTrackClick(plTracks, i) },
                    onPlayAll = { if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0) },
                    onRemoveTrack = { trackId ->
                        val updated = pl.copy(trackIds = pl.trackIds.filterNot { it == trackId })
                        val all = playlists.toMutableList()
                        all[idx] = updated
                        scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                    },
                    onAddTracks = {
                        pickerSelected = pl.trackIds.toSet()
                        trackPickerForIndex = idx
                    },
                    onPickCover = {
                        showCoverPickerForIndex = idx
                        coverPicker.launch(arrayOf("image/*"))
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
                        val next = playlists + LocalPlaylist(name, emptyList())
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

    // Gelişmiş track seçici: tüm şarkılar listesi + checkbox
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
}

@Composable
private fun PlaylistListView(
    playlists: List<LocalPlaylist>,
    tracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onOpen: (Int) -> Unit,
    onPlay: (LocalPlaylist) -> Unit,
    onDelete: (Int) -> Unit,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overscroll(rememberBounceOverscrollEffect()),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists.size) { idx ->
                    val pl = playlists[idx]
                    val plTracks = remember(pl, tracks) {
                        pl.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(idx) }
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
                                val coverUri = pl.coverUri?.let { Uri.parse(it) }
                                val firstArt = coverUri ?: plTracks.firstOrNull()?.albumArtUri
                                var artLoaded by remember(firstArt) { mutableStateOf(false) }
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

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${plTracks.size} ${if (plTracks.size == 1) "track" else "tracks"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BouncyIconButton(
                                onClick = { onPlay(pl) },
                                icon = Icons.Filled.PlayArrow,
                                contentDescription = strings.play,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            // 3-dot menu instead of destructive delete
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                BouncyIconButton(
                                    onClick = { showMenu = true },
                                    icon = Icons.Filled.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Playlist Name") },
                                        onClick = { /* TODO: edit name */ showMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pin to Top") },
                                        onClick = { /* TODO: pin logic (max 3) */ showMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            onDelete(idx)
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dynamically shift FAB upward when mini player is visible
        val fabBottomPadding = if (miniPlayerVisible) 200.dp else 140.dp

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = fabBottomPadding, end = 24.dp)
                .bounceClick(onClick = onCreate),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.createPlaylist)
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: LocalPlaylist,
    plTracks: List<AudioTrack>,
    strings: dev.shephard.player.ui.i18n.Strings,
    onBack: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onAddTracks: () -> Unit,
    onPickCover: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overscroll(rememberBounceOverscrollEffect()),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 160.dp)
        ) {
            // Üst bar: geri + playlist adı
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
                            text = playlist.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${plTracks.size} ${if (plTracks.size == 1) "track" else "tracks"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Kapak + Oynat butonu
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPickCover() },
                    contentAlignment = Alignment.Center
                ) {
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
                    // Edit overlay icon
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

            if (plTracks.isEmpty()) {
                item {
                    Text(
                        text = "No tracks yet. Tap \"${strings.addTracks}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(plTracks.size) { i ->
                    val t = plTracks[i]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTrackClick(i) }
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
                            var loaded by remember(t.id) { mutableStateOf(false) }
                            AsyncImage(
                                model = t.albumArtUri,
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
                                text = t.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = t.artist,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BouncyIconButton(
                            onClick = { onRemoveTrack(t.id) },
                            icon = Icons.Filled.Delete,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
