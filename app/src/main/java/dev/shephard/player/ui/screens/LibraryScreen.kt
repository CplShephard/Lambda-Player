@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.shephard.player.ui.screens

import dev.shephard.player.ui.components.m3.m3CardPress

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
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
import dev.shephard.player.ui.components.miuixHeaderSafeTop
import dev.shephard.player.ui.components.MiuixSwitcherPills
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
    var selectedTab by remember { mutableIntStateOf(runBlocking { prefs.librarySelectedTab.first() }.let { if (it in listOf(0, 1, 3)) it else 0 }) }
    var group by remember(selectedTab) { mutableStateOf<String?>(null) }
    var showSelection by remember { mutableStateOf(false) }
    var songSearch by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showLayout by remember { mutableStateOf(false) }
    val musicLayout by prefs.musicsLayout.collectAsState(initial = dev.shephard.player.player.LayoutMode.LIST)
    val playlistLayout by prefs.playlistsLayout.collectAsState(initial = dev.shephard.player.player.LayoutMode.LIST)
    val albumLayout by prefs.albumsLayout.collectAsState(initial = dev.shephard.player.player.LayoutMode.LIST)
    val refreshing by libraryViewModel.isLoading.collectAsState()
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
    val labels = listOf(0 to strings.librarySongs, 1 to strings.libraryAlbums, 3 to strings.playlists)
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().then(if (useMiuix) Modifier.miuixHeaderSafeTop() else Modifier.statusBarsPadding())) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryText(strings.library, useMiuix, Modifier.weight(1f), color = foreground, fontSize = 30.sp, bold = true)
            LibraryHeaderButton(useMiuix, Icons.Default.Search, strings.search, foreground) {
                showSearch = !showSearch; if (!showSearch) songSearch = ""
            }
            LibraryHeaderButton(useMiuix, Icons.Default.Sort, strings.sortLibrary, foreground) { showSort = true }
            LibraryHeaderButton(useMiuix, Icons.Default.MoreVert, strings.libraryOptions, foreground) { showLayout = true }
        }
        if (showSearch) {
            if (useMiuix) dev.shephard.player.ui.miuix.OutlinedTextField(songSearch, { songSearch = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, placeholder = { dev.shephard.player.ui.miuix.Text(strings.search) })
            else OutlinedTextField(songSearch, { songSearch = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, label = { Text(strings.search) })
        }
        if (useMiuix) {
            MiuixSwitcherPills(
                options = labels,
                selected = selectedTab,
                onSelected = { index ->
                    selectedTab = index
                    scope.launch { prefs.setLibrarySelectedTab(index) }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEach { (index, label) ->
                    FilterChip(selected = selectedTab == index, onClick = { selectedTab = index; scope.launch { prefs.setLibrarySelectedTab(index) } }, label = { Text(label, maxLines = 1) })
                }
            }
        }
        val filteredSongs = remember(tracks, songSearch, songSort, sortDescending, folderFilter) {
            val filtered = tracks.filter { track ->
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
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { if (permission.hasPermission) libraryViewModel.loadTracks() else permission.requestPermission() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            when (selectedTab) {
                0 -> if (useMiuix) MusicScreen(libraryViewModel, playerViewModel, { list, index -> onTrackClick(list, index, null) }, hasMiniPlayer, filteredSongs, embedded = true)
                     else MusicScreenM3(libraryViewModel, playerViewModel, { list, index -> onTrackClick(list, index, null) }, hasMiniPlayer, filteredSongs, embedded = true)
                3 -> if (useMiuix) PlaylistScreen(libraryViewModel, hasMiniPlayer, onTrackClick, onPlaylistRemixClick, onOpenPlaylistDetail, embedded = true, query = songSearch, sortMode = songSort, descending = sortDescending)
                     else PlaylistScreenM3(libraryViewModel, hasMiniPlayer, onTrackClick, onPlaylistRemixClick, onOpenPlaylistDetail, embedded = true, query = songSearch, sortMode = songSort, descending = sortDescending)
                else -> if (!permission.hasPermission) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (useMiuix) dev.shephard.player.ui.miuix.Button(onClick = permission.requestPermission) { dev.shephard.player.ui.miuix.Text(strings.libraryRefresh) }
                        else Button(onClick = permission.requestPermission) { Text(strings.libraryRefresh) }
                    }
                } else {
                    val groups = remember(tracks, strings.libraryUnknown) {
                        tracks.groupBy { "${it.album.takeIf(String::isNotBlank) ?: strings.libraryUnknown} • ${it.artist}" }
                    }
                    val matchingGroups = remember(groups, songSearch, songSort, sortDescending) {
                        val filtered = groups.entries.filter { it.key.contains(songSearch, ignoreCase = true) }
                        val sorted = when (songSort) {
                            1 -> filtered.sortedBy { it.value.firstOrNull()?.artist.orEmpty().lowercase() }
                            3 -> filtered.sortedBy { it.value.maxOfOrNull { track -> track.dateAdded } ?: 0L }
                            4 -> filtered.sortedBy { it.value.sumOf { track -> track.durationMs } }
                            else -> filtered.sortedBy { it.key.lowercase() }
                        }
                        if (sortDescending) sorted.reversed() else sorted
                    }
                    val visibleTracks = group?.let { groups[it] }.orEmpty().filter {
                        songSearch.isBlank() || it.title.contains(songSearch, true) || it.artist.contains(songSearch, true)
                    }.let { list ->
                        val sorted = when (songSort) {
                            1 -> list.sortedBy { it.artist.lowercase() }
                            3 -> list.sortedBy { it.dateAdded }
                            4 -> list.sortedBy { it.durationMs }
                            else -> list.sortedBy { it.title.lowercase() }
                        }
                        if (sortDescending) sorted.reversed() else sorted
                    }
                    var options by remember { mutableStateOf<AudioTrack?>(null) }
                    var edit by remember { mutableStateOf<AudioTrack?>(null) }
                    if (group == null && albumLayout == dev.shephard.player.player.LayoutMode.GRID) {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = if (hasMiniPlayer) 150.dp else 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(matchingGroups.size, key = { matchingGroups[it].key }) { i ->
                                val entry = matchingGroups[i]
                                LibraryAlbumTile(entry.key, entry.value, useMiuix) { group = entry.key; songSearch = "" }
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = if (hasMiniPlayer) 150.dp else 96.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (group == null) {
                                items(matchingGroups, key = { it.key }) { entry ->
                                    LibraryAlbumRow(entry.key, entry.value, useMiuix) { group = entry.key; songSearch = "" }
                                }
                            } else {
                                item { LibraryText("‹  ${group.orEmpty()}", useMiuix, Modifier.clickable { group = null; songSearch = "" }.padding(12.dp), color = accent) }
                                items(visibleTracks, key = { it.id }) { track ->
                                    LibrarySongRow(track, useMiuix, onClick = { onTrackClick(visibleTracks, visibleTracks.indexOf(track), group) }, onMore = { options = track })
                                }
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
    if (showSort) {
        val choices = if (selectedTab == 3) listOf(0 to strings.title, 3 to strings.dateAdded, 4 to strings.trackCount)
            else listOf(0 to strings.title, 1 to strings.artist, 2 to strings.album, 3 to strings.dateAdded, 4 to strings.durationLabel)
        LibraryChoiceDrawer(useMiuix, strings.sortLibrary, choices.map { (index, label) ->
            Triple(label, songSort == index, { showSort = false; scope.launch { prefs.setLibrarySongSort(index) }; Unit })
        } + Triple(if (sortDescending) "Z–A" else "A–Z", false, { showSort = false; scope.launch { prefs.setLibrarySortDescending(!sortDescending) }; Unit })) { showSort = false }
    }
    if (showLayout) {
        val currentLayout = when (selectedTab) { 1 -> albumLayout; 3 -> playlistLayout; else -> musicLayout }
        LibraryChoiceDrawer(useMiuix, strings.layout,
            listOf(dev.shephard.player.player.LayoutMode.LIST to strings.list, dev.shephard.player.player.LayoutMode.GRID to strings.grid).map { (mode, label) ->
                Triple(label, currentLayout == mode, { showLayout = false; scope.launch {
                    when (selectedTab) { 1 -> prefs.setAlbumsLayout(mode); 3 -> prefs.setPlaylistsLayout(mode); else -> prefs.setMusicsLayout(mode) }
                }; Unit })
            }) { showLayout = false }
    }
    if (showSelection) LibrarySelectionSheet(tracks, useMiuix, playerViewModel) { showSelection = false }
    deleteTrack?.let { track ->
        val confirmDelete: () -> Unit = {
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
        }
        if (useMiuix) dev.shephard.player.ui.miuix.MiuixDeleteConfirmationDialog(
            show = true, title = strings.delete, message = strings.deleteTrackConfirm,
            confirmText = strings.delete, cancelText = strings.cancel,
            onDismiss = { deleteTrack = null }, onConfirm = confirmDelete)
        else AlertDialog(onDismissRequest = { deleteTrack = null }, title = { Text(strings.delete) },
            text = { Text(strings.deleteTrackConfirm) },
            dismissButton = { TextButton(onClick = { deleteTrack = null }) { Text(strings.cancel) } },
            confirmButton = { TextButton(onClick = confirmDelete) { Text(strings.delete) } })
    }
}
