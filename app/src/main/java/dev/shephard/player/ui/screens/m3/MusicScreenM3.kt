// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 + M3 Switchers + Miuix padding/radius
package dev.shephard.player.ui.screens.m3

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.shephard.player.ui.components.m3.M3CoverEditor
import dev.shephard.player.ui.components.m3.m3ItemCardColors
import dev.shephard.player.ui.components.m3.m3TopBarColors
import dev.shephard.player.ui.components.m3.rememberCoverPicker
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.shephard.player.ui.components.M3BottomSheetWrapper
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.rememberAudioPermissionState
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.LineageGridMediaItem
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.LineageNoElements
import dev.shephard.player.ui.components.m3.LineageSortingChip
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreenM3(
    libraryViewModel: LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    playerViewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onTrackClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    hasMiniPlayer: Boolean = false
) {
    val tracks by libraryViewModel.tracks.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()
    val hasScanned by libraryViewModel.hasScanned.collectAsState()

    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val musicsLayout by prefs.musicsLayout.collectAsState(initial = LayoutMode.LIST)
    val strings = LocalStrings.current

    val permissionState = rememberAudioPermissionState(
        onGranted = { libraryViewModel.loadTracks() }
    )

    LaunchedEffect(Unit) {
        if (permissionState.hasPermission) {
            libraryViewModel.loadTracks()
        }
    }

    var selectedTrackForMenu by remember { mutableStateOf<AudioTrack?>(null) }
    var trackToEdit by remember { mutableStateOf<AudioTrack?>(null) }
    var trackToDelete by remember { mutableStateOf<AudioTrack?>(null) }
    val scope = rememberCoroutineScope()

    var pendingDeleteUri by remember { mutableStateOf<Uri?>(null) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uri = pendingDeleteUri
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch(Dispatchers.IO) {
                uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                withContext(Dispatchers.Main) { libraryViewModel.loadTracks() }
            }
        }
        pendingDeleteUri = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(strings.music) },
                colors = m3TopBarColors(),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !permissionState.hasPermission -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                        Text(strings.accessYourMusic, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                        Text(strings.permissionDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = permissionState.requestPermission, modifier = Modifier.padding(top = 24.dp)) {
                            Text(strings.grantAccess)
                        }
                    }
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                hasScanned && tracks.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        LineageNoElements(icon = Icons.Filled.FolderOff, message = strings.noSongsToPlay)
                    }
                }
                else -> {
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
                                selected = musicsLayout == LayoutMode.LIST,
                                onClick = { scope.launch { prefs.setMusicsLayout(LayoutMode.LIST) } }
                            )
                            LineageSortingChip(
                                label = strings.grid,
                                selected = musicsLayout == LayoutMode.GRID,
                                onClick = { scope.launch { prefs.setMusicsLayout(LayoutMode.GRID) } }
                            )
                        }

                        if (musicsLayout == LayoutMode.GRID) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    bottom = if (hasMiniPlayer) 160.dp else 80.dp
                                ),
                            ) {
                                gridItems(tracks, key = { it.id }) { track ->
                                    LineageGridMediaItem(
                                        headline = track.title,
                                        subhead = track.artist,
                                        thumbnailModel = track.albumArtUri,
                                        placeholderIcon = Icons.Filled.MusicNote,
                                        trailingIcon = Icons.Filled.MoreVert,
                                        onTrailingClick = { selectedTrackForMenu = track },
                                        onClick = {
                                            val idx = tracks.indexOf(track)
                                            if (idx >= 0) onTrackClick(tracks, idx)
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    bottom = if (hasMiniPlayer) 160.dp else 80.dp,
                                    top = 4.dp
                                ),
                            ) {
                                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = m3ItemCardColors()
                                    ) {
                                        LineageListItemWithThumbnail(
                                            headline = track.title,
                                            supporting = "${track.artist} • ${track.album}",
                                            thumbnailModel = track.albumArtUri,
                                            placeholderIcon = Icons.Filled.MusicNote,
                                            trailingText = track.formattedDuration(),
                                            trailingIcon = Icons.Filled.MoreVert,
                                            onClick = { onTrackClick(tracks, index) },
                                            onTrailingClick = { selectedTrackForMenu = track }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet with M3 switcher style #7 instead of popup
    selectedTrackForMenu?.let { track ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        M3BottomSheetWrapper(
            onDismissRequest = { selectedTrackForMenu = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = m3ItemCardColors()
                ) {
                    LineageListItemWithThumbnail(
                        headline = track.title,
                        supporting = track.artist,
                        thumbnailModel = track.albumArtUri,
                        placeholderIcon = Icons.Filled.MusicNote,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                SegmentedColumn {
                    item {
                        BaseWidget(
                            icon = Icons.Filled.Edit,
                            title = strings.editMusic,
                            onClick = { trackToEdit = track; selectedTrackForMenu = null }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Filled.Delete,
                            title = strings.delete,
                            onClick = { trackToDelete = track; selectedTrackForMenu = null }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    trackToDelete?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text(strings.delete) },
            text = { Text(strings.deleteTrackConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = track
                        trackToDelete = null
                        scope.launch(Dispatchers.IO) {
                            val resolver = context.contentResolver
                            val deletedDirectly = try {
                                resolver.delete(toDelete.uri, null, null)
                                true
                            } catch (e: Exception) {
                                val intentSender = when {
                                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ->
                                        runCatching {
                                            android.provider.MediaStore.createDeleteRequest(resolver, listOf(toDelete.uri)).intentSender
                                        }.getOrNull()
                                    android.os.Build.VERSION.SDK_INT >= 29 ->
                                        (e as? android.app.RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                                    else -> null
                                }
                                if (intentSender != null) {
                                    val request = androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                    withContext(Dispatchers.Main) {
                                        pendingDeleteUri = toDelete.uri
                                        deleteConsentLauncher.launch(request)
                                    }
                                }
                                false
                            }
                            if (deletedDirectly) {
                                withContext(Dispatchers.Main) { libraryViewModel.loadTracks() }
                            }
                        }
                    },
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) { Text(strings.cancel) }
            },
        )
    }

    trackToEdit?.let { track ->
        M3EditTrackDialogLineage(
            track = track,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            onDismiss = { trackToEdit = null }
        )
    }
}

@Composable
private fun M3EditTrackDialogLineage(
    track: AudioTrack,
    libraryViewModel: dev.shephard.player.player.LibraryViewModel,
    playerViewModel: dev.shephard.player.player.PlayerViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val existing = remember(track.id) { libraryViewModel.getOverride(track.id) }

    var titleText by remember { mutableStateOf(existing?.title ?: track.title) }
    var artistText by remember { mutableStateOf(existing?.artist ?: track.artist) }
    var albumText by remember { mutableStateOf(existing?.album ?: track.album) }
    var coverUri by remember { mutableStateOf<Uri?>(existing?.coverUri?.let { Uri.parse(it) }) }
    val pickCover = rememberCoverPicker { coverUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editMusic) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                M3CoverEditor(
                    model = coverUri ?: track.albumArtUri,
                    placeholderIcon = Icons.Filled.MusicNote,
                    hasCustomCover = coverUri != null,
                    onPick = pickCover,
                    onRemove = { coverUri = null },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = titleText, onValueChange = { titleText = it }, label = { Text(strings.title) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artistText, onValueChange = { artistText = it }, label = { Text(strings.artist) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = albumText, onValueChange = { albumText = it }, label = { Text(strings.album) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    libraryViewModel.saveTrackOverride(trackId = track.id, title = titleText, artist = artistText, album = albumText, coverUri = coverUri?.toString())
                    val updatedTrack = track.copy(title = titleText.ifBlank { track.title }, artist = artistText.ifBlank { track.artist }, album = albumText.ifBlank { track.album }, albumArtUri = coverUri ?: track.albumArtUri)
                    playerViewModel.notifyTrackUpdated(updatedTrack)
                    onDismiss()
                },
            ) { Text(strings.done) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
