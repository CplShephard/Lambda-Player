@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.shephard.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.rememberAudioPermissionState
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.m3.MusicScreenM3
import dev.shephard.player.ui.screens.m3.PlaylistScreenM3
import dev.shephard.player.ui.screens.m3.M3EditTrackDialogLineage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

/** A single dock destination. Existing editors and playlists remain the source of truth. */
@Composable
fun LibraryScreen(
    useMiuix: Boolean,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    hasMiniPlayer: Boolean,
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit,
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit,
    onOpenPlaylistDetail: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(runBlocking { prefs.librarySelectedTab.first() }) }
    var group by remember(selectedTab) { mutableStateOf<String?>(null) }
    var showSelection by remember { mutableStateOf(false) }
    var songSearch by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    val songSort by prefs.librarySongSort.collectAsState(initial = 0)
    val sortDescending by prefs.librarySortDescending.collectAsState(initial = false)
    val folderFilter by prefs.libraryFolderFilter.collectAsState(initial = "")
    val tracks by libraryViewModel.tracks.collectAsState()
    val state by playerViewModel.uiState.collectAsState()
    val permission = rememberAudioPermissionState(onGranted = { libraryViewModel.loadTracks() })
    var pendingDelete by remember { mutableStateOf<android.net.Uri?>(null) }
    var deleteTrack by remember { mutableStateOf<AudioTrack?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val uri = pendingDelete
        pendingDelete = null
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch(Dispatchers.IO) {
                // Q grants permission but does not perform the deletion; R+ performs it itself.
                if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.Q && uri != null)
                    runCatching { context.contentResolver.delete(uri, null, null) }
                withContext(Dispatchers.Main) { libraryViewModel.loadTracks() }
            }
        }
    }
    LaunchedEffect(Unit) { if (permission.hasPermission) libraryViewModel.loadTracks() }
    BackHandler(enabled = group != null) { group = null }
    val labels = listOf(strings.librarySongs, strings.libraryAlbums, strings.libraryArtists,
        strings.playlists, strings.likedSongs, strings.libraryFolders, strings.libraryGenres)
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.library, color = foreground, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { showSelection = true }, enabled = tracks.isNotEmpty()) {
                Icon(Icons.Default.PlaylistAddCheck, contentDescription = strings.selectSongs, tint = foreground)
            }
            IconButton(onClick = { if (permission.hasPermission) libraryViewModel.loadTracks() else permission.requestPermission() }) {
                Icon(Icons.Default.Refresh, contentDescription = strings.libraryRefresh, tint = foreground)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                FilterChip(selected = selectedTab == index, onClick = {
                    selectedTab = index
                    scope.launch { prefs.setLibrarySelectedTab(index) }
                }, label = { Text(label, maxLines = 1) })
            }
        }
        val filteredSongs = remember(tracks, songSearch, songSort, sortDescending, folderFilter) {
            val filtered = tracks.filter { track ->
                (folderFilter.isBlank() || track.folder == folderFilter) &&
                    (songSearch.isBlank() || track.title.contains(songSearch, true) || track.artist.contains(songSearch, true) || track.album.contains(songSearch, true))
            }
            val sorted = when (songSort) {
                1 -> filtered.sortedBy { it.artist.lowercase() }
                2 -> filtered.sortedBy { it.album.lowercase() }
                3 -> filtered.sortedBy { it.dateAdded }
                4 -> filtered.sortedBy { it.durationMs }
                else -> filtered.sortedBy { it.title.lowercase() }
            }
            if (sortDescending) sorted.reversed() else sorted
        }
        if (selectedTab == 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(songSearch, { songSearch = it }, Modifier.weight(1f), singleLine = true, label = { Text(strings.search) })
                Box {
                    IconButton(onClick = { showSort = true }) { Icon(Icons.Default.Sort, strings.sortLibrary, tint = foreground) }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        listOf(strings.title, strings.artist, strings.album, strings.dateAdded, strings.durationLabel).forEachIndexed { index, label ->
                            DropdownMenuItem(text = { Text(if (songSort == index) "✓  $label" else label) }, onClick = {
                                showSort = false; scope.launch { prefs.setLibrarySongSort(index) }
                            })
                        }
                    }
                }
                TextButton(onClick = { scope.launch { prefs.setLibrarySortDescending(!sortDescending) } }) { Text(if (sortDescending) "↓" else "↑", color = accent) }
                Box {
                    IconButton(onClick = { showFolders = true }) { Icon(Icons.Default.Folder, strings.libraryFolders, tint = foreground) }
                    DropdownMenu(expanded = showFolders, onDismissRequest = { showFolders = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                        DropdownMenuItem(text = { Text(strings.allFolders) }, onClick = { showFolders = false; scope.launch { prefs.setLibraryFolderFilter("") } })
                        tracks.mapNotNull { it.folder }.distinct().sorted().forEach { folder ->
                            DropdownMenuItem(text = { Text(if (folder == folderFilter) "✓  $folder" else folder, maxLines = 1) }, onClick = {
                                showFolders = false; scope.launch { prefs.setLibraryFolderFilter(folder) }
                            })
                        }
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> if (useMiuix) MusicScreen(libraryViewModel, playerViewModel, { list, index -> onTrackClick(list, index, null) }, hasMiniPlayer, filteredSongs, embedded = true)
                     else MusicScreenM3(libraryViewModel, playerViewModel, { list, index -> onTrackClick(list, index, null) }, hasMiniPlayer, filteredSongs, embedded = true)
                3 -> if (useMiuix) PlaylistScreen(libraryViewModel, hasMiniPlayer, onTrackClick, onPlaylistRemixClick, onOpenPlaylistDetail, embedded = true)
                     else PlaylistScreenM3(libraryViewModel, hasMiniPlayer, onTrackClick, onPlaylistRemixClick, onOpenPlaylistDetail, embedded = true)
                else -> if (!permission.hasPermission) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = permission.requestPermission) { Text(strings.libraryRefresh) }
                    }
                } else {
                    var search by remember(selectedTab) { mutableStateOf("") }
                    var ascending by remember(selectedTab) { mutableStateOf(true) }
                    val groups = remember(tracks, selectedTab, state.likedSongIds, strings.libraryUnknown) {
                        when (selectedTab) {
                            1 -> tracks.groupBy { "${it.album.takeIf(String::isNotBlank) ?: strings.libraryUnknown} • ${it.artist}" }
                            2 -> tracks.groupBy { it.artist.takeIf(String::isNotBlank) ?: strings.libraryUnknown }
                            4 -> mapOf(strings.likedSongs to tracks.filter { it.id in state.likedSongIds })
                            5 -> tracks.groupBy { it.folder ?: strings.libraryUnknown }
                            else -> tracks.groupBy { it.genre ?: strings.libraryUnknown }
                        }
                    }
                    val selectedTracks = if (selectedTab == 4) groups[strings.likedSongs] else group?.let { groups[it] }
                    val visibleTracks = selectedTracks?.filter {
                        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.artist.contains(search, ignoreCase = true)
                    }?.sortedBy { it.title.lowercase() }?.let { if (ascending) it else it.reversed() }
                    var options by remember { mutableStateOf<AudioTrack?>(null) }
                    var edit by remember { mutableStateOf<AudioTrack?>(null) }
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = if (hasMiniPlayer) 150.dp else 96.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(search, { search = it }, Modifier.weight(1f), singleLine = true, label = { Text(strings.search) })
                                TextButton(onClick = { ascending = !ascending }) { Text(if (ascending) "A–Z" else "Z–A", color = accent) }
                            }
                        }
                        if (visibleTracks == null) {
                            val matchingGroups = groups.entries.filter { it.key.contains(search, ignoreCase = true) }.sortedBy { it.key.lowercase() }.let { if (ascending) it else it.reversed() }
                            items(matchingGroups, key = { it.key }) { (name, list) ->
                                ListItem(headlineContent = { Text(name, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text("${list.size} ${strings.librarySongs}") },
                                    leadingContent = { AsyncImage(dev.shephard.player.ui.components.rememberStaticArtwork(list.firstOrNull()?.albumArtUri), null, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) },
                                    modifier = Modifier.clickable { group = name; search = "" }, colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
                            }
                        } else {
                            if (selectedTab != 4) item { TextButton(onClick = { group = null; search = "" }) { Text("‹  ${group.orEmpty()}", color = accent) } }
                            items(visibleTracks, key = { it.id }) { track ->
                                ListItem(headlineContent = { Text(track.title, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingContent = { AsyncImage(dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri), null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) },
                                    trailingContent = { IconButton(onClick = { options = track }) { Icon(Icons.Default.MoreVert, strings.libraryOptions, tint = foreground) } },
                                    modifier = Modifier.clickable { onTrackClick(visibleTracks, visibleTracks.indexOf(track), group) },
                                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
                            }
                        }
                    }
                    options?.let { track ->
                        SongOptionsSheet(track, useMiuix, playerViewModel,
                            onDismiss = { options = null }, onEdit = { options = null; edit = track },
                            onDelete = { options = null; deleteTrack = track })
                    }
                    edit?.let { track ->
                        if (useMiuix) EditMusicDrawer(track, libraryViewModel, playerViewModel) { edit = null }
                        else M3EditTrackDialogLineage(track, libraryViewModel, playerViewModel) { edit = null }
                    }
                }
            }
        }
    }
    if (showSelection) LibrarySelectionSheet(tracks, useMiuix, playerViewModel) { showSelection = false }
    deleteTrack?.let { track ->
        AlertDialog(onDismissRequest = { deleteTrack = null }, title = { Text(strings.delete) },
            text = { Text(strings.deleteTrackConfirm) },
            dismissButton = { TextButton(onClick = { deleteTrack = null }) { Text(strings.cancel) } },
            confirmButton = { TextButton(onClick = {
                deleteTrack = null
                scope.launch(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    try {
                        val deleted = resolver.delete(track.uri, null, null) > 0
                        withContext(Dispatchers.Main) {
                            if (deleted) libraryViewModel.loadTracks()
                            else android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        val sender = when {
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ->
                                runCatching { android.provider.MediaStore.createDeleteRequest(resolver, listOf(track.uri)).intentSender }.getOrNull()
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ->
                                (e as? android.app.RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                            else -> null
                        }
                        withContext(Dispatchers.Main) {
                            if (sender != null) {
                                pendingDelete = track.uri
                                runCatching { deleteLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
                                    .onFailure { pendingDelete = null; android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show() }
                            } else android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) { Text(strings.delete) } })
    }
}
