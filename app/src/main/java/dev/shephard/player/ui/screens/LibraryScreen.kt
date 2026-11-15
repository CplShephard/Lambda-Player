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
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import top.yukonga.miuix.kmp.blur.layerBackdrop
import dev.shephard.player.ui.components.miuixHeaderSafeTop
import dev.shephard.player.ui.components.MiuixSwitcherPills
import dev.shephard.player.ui.components.LibraryTabsFixedShade
import dev.shephard.player.ui.components.rememberLibraryTabsBackdrop
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.screens.m3.MusicScreenM3
import dev.shephard.player.ui.screens.m3.PlaylistScreenM3
import dev.shephard.player.ui.screens.m3.M3EditTrackDialogLineage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding

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
    onOpenAlbumDetail: (Long, String, String) -> Unit,
) {
    val strings = LocalStrings.current
    val isLandscape = dev.shephard.player.ui.util.isLandscapeWindow()
    val listBottomPadding = if (isLandscape) {
        if (hasMiniPlayer) 190.dp else 110.dp
    } else {
        if (hasMiniPlayer) 150.dp else 96.dp
    }
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    // Do not block the UI thread while DataStore opens during a page transition.
    val initialTab by prefs.librarySelectedTab.collectAsState(initial = -1)
    if (initialTab < 0) { Box(Modifier.fillMaxSize()); return }
    var selectedTab by remember { mutableIntStateOf(initialTab.let { if (it in listOf(0, 1, 3)) it else 0 }) }
    var group by remember(selectedTab) { mutableStateOf<String?>(null) }
    var showSelection by remember { mutableStateOf(false) }
    var songSearch by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val musicLayout by prefs.musicsLayout.collectAsState(initial = dev.shephard.player.player.LayoutMode.LIST)
    val playlistLayout by prefs.playlistsLayout.collectAsState(initial = dev.shephard.player.player.LayoutMode.LIST)
    val albumLayout by prefs.albumsLayout.collectAsState(initial = dev.shephard.player.player.LayoutMode.LIST)
    val refreshing by libraryViewModel.isLoading.collectAsState()
    var manualRefreshIndicator by remember { mutableStateOf(false) }
    val songSort by prefs.librarySongSort.collectAsState(initial = 0)
    val sortDescending by prefs.librarySortDescending.collectAsState(initial = false)
    val folderFilter by prefs.libraryFolderFilter.collectAsState(initial = "")
    val tracks by libraryViewModel.tracks.collectAsState()
    val state by playerViewModel.uiState.collectAsState()
    // Also reconcile a queue restored after the first library emission.
    val queueIdentity = state.queue.map { it.id to it.uri }
    LaunchedEffect(tracks, queueIdentity) { playerViewModel.syncLibraryTracks(tracks) }
    val permission = rememberAudioPermissionState(onGranted = { libraryViewModel.loadTracks() })
    var pendingDelete by remember { mutableStateOf<android.net.Uri?>(null) }
    var deleteTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var deleteProgress by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var showDeleteProgress by remember { mutableStateOf(false) }
    var lastCleanupReport by remember { mutableStateOf<dev.shephard.player.player.PlayerViewModel.OrphanCleanupReport?>(null) }
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
    LaunchedEffect(Unit) { if (permission.hasPermission) libraryViewModel.ensureTracksLoaded() }
    BackHandler(enabled = group != null) { group = null }
    val labels = listOf(0 to strings.librarySongs, 3 to strings.playlists, 1 to strings.libraryAlbums)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = labels.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
        pageCount = { labels.size }
    )
    var tabNavigationToken by remember { mutableIntStateOf(0) }
    var tabNavigationInProgress by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!tabNavigationInProgress) labels.getOrNull(page)?.first?.let { selectedTab = it }
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            labels.getOrNull(page)?.first?.let { prefs.setLibrarySelectedTab(it) }
        }
    }
    val selectTab: (Int) -> Unit = { tab ->
        val target = labels.indexOfFirst { it.first == tab }
        if (target >= 0) {
            selectedTab = tab // Highlight the tapped tab immediately, before pager animation.
            tabNavigationInProgress = true
            val token = ++tabNavigationToken
            scope.launch {
                try { pagerState.animateScrollToPage(target) }
                finally {
                    if (token == tabNavigationToken) {
                        tabNavigationInProgress = false
                        labels.getOrNull(pagerState.currentPage)?.first?.let { selectedTab = it }
                    }
                }
            }
        }
    }
    val foreground = if (useMiuix) MiuixAppTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
    val accent = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(if (useMiuix) MiuixAppTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
        .then(if (useMiuix) Modifier.miuixHeaderSafeTop() else Modifier.statusBarsPadding())) {
        Row(Modifier.fillMaxWidth().padding(start = if (isLandscape) 14.dp else 20.dp, end = 8.dp, top = if (isLandscape) 4.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryText(strings.library, useMiuix, Modifier.weight(1f), color = foreground, fontSize = if (isLandscape) 24.sp else 30.sp, bold = true)
            LibraryHeaderButton(useMiuix, Icons.Default.Search, strings.search, foreground) {
                showSearch = !showSearch; if (!showSearch) songSearch = ""
            }
            val sortChoices = if (selectedTab == 3) listOf(0 to strings.title, 3 to strings.dateAdded, 4 to strings.trackCount)
                else listOf(0 to strings.title, 1 to strings.artist, 2 to strings.album, 3 to strings.dateAdded, 4 to strings.durationLabel)
            LibraryPopupButton(useMiuix, Icons.Default.Sort, strings.sortLibrary, foreground,
                sortChoices.map { (index, label) -> Triple(label, songSort == index, { scope.launch { prefs.setLibrarySongSort(index) }; Unit }) } +
                    Triple(if (sortDescending) "Z–A" else "A–Z", false, { scope.launch { prefs.setLibrarySortDescending(!sortDescending) }; Unit }))
            val currentLayout = when (selectedTab) { 1 -> albumLayout; 3 -> playlistLayout; else -> musicLayout }
            LibraryPopupButton(useMiuix, Icons.Default.MoreVert, strings.libraryOptions, foreground,
                listOf(dev.shephard.player.player.LayoutMode.LIST to strings.list, dev.shephard.player.player.LayoutMode.GRID to strings.grid).map { (mode, label) ->
                    Triple(label, currentLayout == mode, { scope.launch {
                        when (selectedTab) { 1 -> prefs.setAlbumsLayout(mode); 3 -> prefs.setPlaylistsLayout(mode); else -> prefs.setMusicsLayout(mode) }
                    }; Unit })
                })
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showSearch,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            if (useMiuix) dev.shephard.player.ui.miuix.OutlinedTextField(songSearch, { songSearch = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (isLandscape) 2.dp else 6.dp), singleLine = true, placeholder = { dev.shephard.player.ui.miuix.Text(strings.search) })
            else OutlinedTextField(songSearch, { songSearch = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (isLandscape) 2.dp else 6.dp), singleLine = true, label = { Text(strings.search) })
        }
        // Fixed shade under Library tabs reusing miuix topBar dim (Library has no collapsing bar)
        val libraryTabsBackdrop = if (useMiuix) rememberLibraryTabsBackdrop() else null
        if (useMiuix) {
            LibraryTabsFixedShade(
                backdrop = libraryTabsBackdrop,
                modifier = Modifier.fillMaxWidth()
            ) {
                MiuixSwitcherPills(
                    options = labels,
                    selected = selectedTab,
                    onSelected = { index ->
                        selectTab(index)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isLandscape) 3.dp else 6.dp),
                    animateSelection = true,
                )
            }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = if (isLandscape) 3.dp else 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEach { (index, label) ->
                    val selected = selectedTab == index
                    val chipBackground by androidx.compose.animation.animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        animationSpec = androidx.compose.animation.core.tween(220), label = "libraryChipBackground",
                    )
                    val chipText by androidx.compose.animation.animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = androidx.compose.animation.core.tween(220), label = "libraryChipText",
                    )
                    val chipBorder by androidx.compose.animation.animateColorAsState(
                        if (selected) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = androidx.compose.animation.core.tween(220), label = "libraryChipBorder",
                    )
                    FilterChip(
                        selected = selected, onClick = { selectTab(index) }, label = { Text(label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = chipBackground, selectedContainerColor = chipBackground,
                            labelColor = chipText, selectedLabelColor = chipText,
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                    )
                }
            }
        }
        val filteredSongs by produceState(tracks, tracks, songSearch, songSort, sortDescending, folderFilter) {
            value = withContext(Dispatchers.Default) {
                val filtered = tracks.filter { track ->
                    songSearch.isBlank() || track.title.contains(songSearch, true) || track.artist.contains(songSearch, true) || track.album.contains(songSearch, true)
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
        }
        val pagerContent: @Composable () -> Unit = {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState, modifier = Modifier.fillMaxSize().then(if (libraryTabsBackdrop != null) Modifier.layerBackdrop(libraryTabsBackdrop) else Modifier), beyondViewportPageCount = 1
            ) { page ->
            when (labels[page].first) {
                0 -> if (useMiuix) MusicScreen(libraryViewModel, playerViewModel, { list, index -> onTrackClick(list, index, strings.music) }, hasMiniPlayer, filteredSongs, embedded = true)
                     else MusicScreenM3(libraryViewModel, playerViewModel, { list, index -> onTrackClick(list, index, strings.music) }, hasMiniPlayer, filteredSongs, embedded = true)
                3 -> if (useMiuix) PlaylistScreen(libraryViewModel, hasMiniPlayer, onTrackClick, onPlaylistRemixClick, onOpenPlaylistDetail, embedded = true, query = songSearch, sortMode = songSort, descending = sortDescending)
                     else PlaylistScreenM3(libraryViewModel, hasMiniPlayer, onTrackClick, onPlaylistRemixClick, onOpenPlaylistDetail, embedded = true, query = songSearch, sortMode = songSort, descending = sortDescending)
                else -> if (!permission.hasPermission) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (useMiuix) dev.shephard.player.ui.miuix.Button(onClick = permission.requestPermission) { dev.shephard.player.ui.miuix.Text(strings.libraryRefresh) }
                        else Button(onClick = permission.requestPermission) { Text(strings.libraryRefresh) }
                    }
                } else {
                    val groups by produceState(emptyMap<String, List<AudioTrack>>(), tracks, strings.libraryUnknown) {
                        value = withContext(Dispatchers.Default) {
                            tracks.groupBy { "${it.album.takeIf(String::isNotBlank) ?: strings.libraryUnknown} • ${it.artist}" }
                        }
                    }
                    val matchingGroups by produceState(emptyList<Map.Entry<String, List<AudioTrack>>>(), groups, songSearch, songSort, sortDescending) {
                        value = withContext(Dispatchers.Default) {
                            val filtered = groups.entries.filter { it.key.contains(songSearch, ignoreCase = true) }
                            val sorted = when (songSort) {
                                1 -> filtered.sortedBy { it.value.firstOrNull()?.artist.orEmpty().lowercase() }
                                3 -> filtered.sortedBy { it.value.maxOfOrNull { track -> track.dateAdded } ?: 0L }
                                4 -> filtered.sortedBy { it.value.sumOf { track -> track.durationMs } }
                                else -> filtered.sortedBy { it.key.lowercase() }
                            }
                            if (sortDescending) sorted.reversed() else sorted
                        }
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
                    if (group == null && matchingGroups.isEmpty()) {
                        LibraryEmptyResults(useMiuix, "${strings.noSearchResults} ${strings.libraryAlbums}")
                    } else dev.shephard.player.ui.components.LayoutSwitchAnimation(albumLayout, Modifier.fillMaxSize()) { displayLayout ->
if (group == null && displayLayout == dev.shephard.player.player.LayoutMode.GRID) {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (isLandscape) 8 else 2),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = listBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(matchingGroups.size, key = { matchingGroups[it].key }) { i ->
                                val entry = matchingGroups[i]
                                Box(Modifier.animateItem()) { LibraryAlbumTile(entry.key, entry.value, useMiuix) { entry.value.firstOrNull()?.let { track -> onOpenAlbumDetail(track.albumId ?: 0L, track.album, track.artist) } } }
                            }
                        }
                    } else if (isLandscape) {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = listBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (group == null) {
                                gridItems(matchingGroups, key = { it.key }) { entry ->
                                    Box(Modifier.animateItem()) { LibraryAlbumRow(entry.key, entry.value, useMiuix) { entry.value.firstOrNull()?.let { track -> onOpenAlbumDetail(track.albumId ?: 0L, track.album, track.artist) } } }
                                }
                            } else {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    LibraryText("‹  ${group.orEmpty()}", useMiuix, Modifier.clickable { group = null; songSearch = "" }.padding(12.dp), color = accent)
                                }
                                gridItems(visibleTracks, key = { it.id }) { track ->
                                    Box(Modifier.animateItem()) { LibrarySongRow(track, useMiuix, onClick = { onTrackClick(visibleTracks, visibleTracks.indexOf(track), group) }, onMore = { options = track }) }
                                }
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = listBottomPadding),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (group == null) {
                                items(matchingGroups, key = { it.key }) { entry ->
                                    Box(Modifier.animateItem()) { LibraryAlbumRow(entry.key, entry.value, useMiuix) { entry.value.firstOrNull()?.let { track -> onOpenAlbumDetail(track.albumId ?: 0L, track.album, track.artist) } } }
                                }
                            } else {
                                item { LibraryText("‹  ${group.orEmpty()}", useMiuix, Modifier.clickable { group = null; songSearch = "" }.padding(12.dp), color = accent) }
                                items(visibleTracks, key = { it.id }) { track ->
                                    Box(Modifier.animateItem()) { LibrarySongRow(track, useMiuix, onClick = { onTrackClick(visibleTracks, visibleTracks.indexOf(track), group) }, onMore = { options = track }) }
                                }
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
        val refreshLibrary: () -> Unit = {
            if (!permission.hasPermission) permission.requestPermission()
            else if (!manualRefreshIndicator && !refreshing) {
                manualRefreshIndicator = true
                libraryViewModel.refreshTracks() // Publish scan results immediately; delay only the indicator.
                scope.launch {
                    kotlinx.coroutines.delay(1400L)
                    manualRefreshIndicator = false
                }
            }
        }
        if (useMiuix) {
            top.yukonga.miuix.kmp.basic.PullToRefresh(
                isRefreshing = refreshing || manualRefreshIndicator, onRefresh = refreshLibrary,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                refreshTexts = listOf(strings.pullToRefresh, strings.releaseToRefresh, strings.refreshing, strings.refreshComplete),
                content = pagerContent,
            )
        } else {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = refreshing || manualRefreshIndicator, onRefresh = refreshLibrary,
                modifier = Modifier.weight(1f).fillMaxWidth(), content = { pagerContent() },
            )
        }
    }
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
        // Wrapped confirm that first scans DataStore with progress bar to 100% then deletes only GEREKSIZ orphan data
        val confirmWithScan: () -> Unit = {
            val trackToDelete = deleteTrack
            showDeleteProgress = true; deleteProgress = 0
            scope.launch(Dispatchers.IO) {
                // progress bar scans DataStore directory while deleting file
                val ids = tracks.map { it.id.toString() }.toSet().let { if (trackToDelete != null) it - trackToDelete.id.toString() else it }
                val stableKeys = tracks.filter { it.id != trackToDelete?.id }.map { dev.shephard.player.data.TrackFingerprint.stableKey(it) }.toSet()
                // show scanningprogress to 100% before actual orphan deletion (as requested: popup with progress bar)
                for (p in 0..100 step 12) { withContext(Dispatchers.Main) { deleteProgress = p }; kotlinx.coroutines.delay(45) }
                val report = playerViewModel.scanAndCleanOrphanedData(ids, stableKeys) { p -> scope.launch(Dispatchers.Main) { deleteProgress = p } }
                withContext(Dispatchers.Main) { lastCleanupReport = report; deleteProgress = 100; showDeleteProgress = false }
                // now actually delete the music file
                withContext(Dispatchers.Main) { confirmDelete() }
            }
        }
        if (showDeleteProgress) {
            if (useMiuix) dev.shephard.player.ui.miuix.MiuixDialog(
                onDismissRequest = { showDeleteProgress = false },
                title = "DataStore orphan scan… ${deleteProgress}%",
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(modifier = Modifier.fillMaxWidth(fraction = deleteProgress/100f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary))
                        }
                        Text(text = "${deleteProgress}% — scanning for GEREKSIZ leftover metadata (listening stats, lyrics…)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        if (lastCleanupReport != null) Text(text = lastCleanupReport!!.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                        Text(text = "Only orphaned DataStore entries will be removed, music file deletion follows.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f), modifier = Modifier.padding(top=6.dp))
                    }
                },
                buttons = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { top.yukonga.miuix.kmp.basic.TextButton(onClick = { showDeleteProgress = false }, text = strings.cancel) } }
            ) else AlertDialog(
                onDismissRequest = { showDeleteProgress = false },
                title = { Text("Scanning… ${deleteProgress}%") },
                text = {
                    Column {
                        LinearProgressIndicator(progress = { deleteProgress/100f }, modifier = Modifier.fillMaxWidth())
                        Text("${deleteProgress}% — scanning DataStore for orphaned leftover metadata")
                    }
                },
                confirmButton = {}, dismissButton = { TextButton(onClick = { showDeleteProgress = false }) { Text(strings.cancel) } }
            )
        }
        if (useMiuix) dev.shephard.player.ui.miuix.MiuixDeleteConfirmationDialog(
            show = true, title = strings.delete, message = strings.deleteTrackConfirm,
            confirmText = strings.delete, cancelText = strings.cancel,
            onDismiss = { deleteTrack = null }, onConfirm = confirmWithScan)
        else AlertDialog(onDismissRequest = { deleteTrack = null }, title = { Text(strings.delete) },
            text = { Text(strings.deleteTrackConfirm) },
            dismissButton = { TextButton(onClick = { deleteTrack = null }) { Text(strings.cancel) } },
            confirmButton = { TextButton(onClick = confirmWithScan) { Text(strings.delete) } })
    }
}
