// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 + M3 padding/radius + bottom sheet menu
// Behaviour mirrors the MIUIX PlaylistDetailView: visible Play / Remix / Add tracks buttons,
// sort chips (custom, alphabetical, artist, time added), drag-to-reorder for custom order,
// shuffled order and no add button when opened from Home, and 220dp bottom space for the mini player.
package dev.shephard.player.ui.screens.m3

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import dev.shephard.player.ui.components.LocalDetailMiniPlayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.PlaylistCoverArt
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.M3SmallTopBar
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.components.m3.m3ItemCardColors
import dev.shephard.player.ui.components.m3.m3TopBarColors
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import dev.shephard.player.ui.i18n.Strings
import dev.shephard.player.ui.screens.LocalPlaylist
import dev.shephard.player.ui.screens.encodePlaylists
import dev.shephard.player.ui.screens.parsePlaylists
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun M3PlaylistDetail(
    playlist: LocalPlaylist,
    allTracks: List<AudioTrack>,
    plTracks: List<AudioTrack>,
    strings: Strings,
    onBack: () -> Unit,
    onTrackClick: (List<AudioTrack>, Int) -> Unit,
    onPlayAll: () -> Unit,
    onPlayRemix: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onReorder: (List<AudioTrack>) -> Unit = {},
    onChangeSort: (String) -> Unit = {},
    isHomeSimplified: Boolean = false,
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    // Same rules as the MIUIX detail: system (liked songs) and Home playlists are read-only.
    val canEdit = !playlist.isSystem && !isHomeSimplified

    var showMenu by remember { mutableStateOf(false) }
    var showAddTracks by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var trackMenuTrack by remember { mutableStateOf<AudioTrack?>(null) }

    fun updateRaw(transform: (MutableList<LocalPlaylist>) -> Unit) {
        scope.launch {
            val currentJson = prefs.playlistsJson.first()
            val all = parsePlaylists(currentJson).toMutableList()
            transform(all)
            prefs.setPlaylistsJson(encodePlaylists(all))
        }
    }

    var pendingCoverCrop by remember { mutableStateOf<Uri?>(null) }
    val coverCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = pendingCoverCrop
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            updateRaw { all ->
                val idx = all.indexOfFirst { it.name == playlist.name && it.createdAt == playlist.createdAt }
                if (idx >= 0) all[idx] = all[idx].copy(coverUri = output.toString())
            }
        }
        pendingCoverCrop = null
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            if (context.contentResolver.getType(uri) == "image/gif") {
                scope.launch {
                    val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, uri)
                    if (persisted != null) {
                        updateRaw { all ->
                            val idx = all.indexOfFirst { it.name == playlist.name && it.createdAt == playlist.createdAt }
                            if (idx >= 0) all[idx] = all[idx].copy(coverUri = persisted.toString())
                        }
                    }
                }
            } else {
                val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
                val file = java.io.File(dir, "playlist_cover_${System.currentTimeMillis()}.jpg")
                val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingCoverCrop = outputUri
                val cropIntent = Intent("com.android.camera.action.CROP").apply {
                    setDataAndType(uri, "image/*")
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
                    clipData = android.content.ClipData.newUri(context.contentResolver, "playlist_cover", uri)
                }
                val resolved = context.packageManager.queryIntentActivities(cropIntent, 0)
                for (info in resolved) {
                    val pkg = info.activityInfo?.packageName ?: continue
                    try {
                        context.grantUriPermission(pkg, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    } catch (_: SecurityException) { }
                }
                if (resolved.isNotEmpty()) {
                    coverCropLauncher.launch(cropIntent)
                } else {
                    scope.launch {
                        val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, uri)
                        if (persisted != null) {
                            updateRaw { all ->
                                val idx = all.indexOfFirst { it.name == playlist.name && it.createdAt == playlist.createdAt }
                                if (idx >= 0) all[idx] = all[idx].copy(coverUri = persisted.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    // Home shows the tracks in a random order (as on MIUIX); the list itself is never reordered there.
    val shuffledTracks = remember(plTracks) { plTracks.shuffled() }
    val displayTracks = if (isHomeSimplified) shuffledTracks else plTracks

    // Drag-to-reorder state, kept in sync with the playlist order (same approach as the MIUIX detail).
    val reorderItems = remember { mutableStateListOf<AudioTrack>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isReordering by remember { mutableStateOf(false) }
    var pendingCommittedOrder by remember { mutableStateOf<List<Long>?>(null) }

    LaunchedEffect(displayTracks, isReordering) {
        if (isReordering) return@LaunchedEffect
        val pending = pendingCommittedOrder
        if (pending != null) {
            if (displayTracks.map { it.id } == pending) pendingCommittedOrder = null
            return@LaunchedEffect
        }
        if (reorderItems.map { it.id } != displayTracks.map { it.id }) {
            reorderItems.clear()
            reorderItems.addAll(displayTracks)
        }
    }

    val listState = rememberLazyListState()
    // The header (cover, title, buttons, sort chips) is the only item before the tracks.
    val reorderHeaderItemCount = 1
    val reorderableState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        val fromIdx = from.index - reorderHeaderItemCount
        val toIdx = to.index - reorderHeaderItemCount
        if (fromIdx in reorderItems.indices && toIdx in reorderItems.indices) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) fromIdx to toIdx else currentDragInfo.first to toIdx
            reorderItems.add(toIdx, reorderItems.removeAt(fromIdx))
        }
    }
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        isReordering = reorderableState.isAnyItemDragging
        if (reorderableState.isAnyItemDragging) pendingCommittedOrder = null
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                dragInfo = null
                if (from != to && reorderItems.map { it.id } != displayTracks.map { it.id }) {
                    pendingCommittedOrder = reorderItems.map { it.id }
                    onReorder(reorderItems.toList())
                }
            }
        }
    }
    val customOrder = canEdit && playlist.sortMode == "custom"

    if (showAddTracks) {
        var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
        val existingIds = playlist.trackIds
        val candidates = allTracks.filter { it.id !in existingIds }
        AlertDialog(
            onDismissRequest = { showAddTracks = false },
            title = { Text(strings.addTracks) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    items(candidates, key = { it.id }) { track ->
                        val checked = track.id in selected
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { selected = if (checked) selected - track.id else selected + track.id })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddTracks = false
                    if (selected.isNotEmpty()) {
                        updateRaw { all ->
                            val idx = all.indexOfFirst { it.name == playlist.name && it.createdAt == playlist.createdAt }
                            if (idx >= 0) all[idx] = all[idx].copy(trackIds = (all[idx].trackIds + selected).distinct())
                        }
                    }
                }) { Text(strings.done) }
            },
            dismissButton = { TextButton(onClick = { showAddTracks = false }) { Text(strings.cancel) } },
        )
    }

    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text(strings.editPlaylist) },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, label = { Text(strings.playlistName) }) },
            confirmButton = {
                TextButton(onClick = {
                    renameDialog = false
                    val newName = renameText.trim()
                    if (newName.isNotEmpty() && newName != playlist.name) {
                        updateRaw { all ->
                            val idx = all.indexOfFirst { it.name == playlist.name && it.createdAt == playlist.createdAt }
                            if (idx >= 0) all[idx] = all[idx].copy(name = newName)
                        }
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text(strings.cancel) } },
        )
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(strings.delete) },
            text = { Text(strings.removePlaylistConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirm = false
                    updateRaw { all -> all.removeAll { it.name == playlist.name && it.createdAt == playlist.createdAt } }
                    onBack()
                }) { Text(strings.delete) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text(strings.cancel) } },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    // Detail mini player sits above the content (zIndex) and is only composed while the detail is open.
    LocalDetailMiniPlayer.current(Modifier.align(Alignment.BottomCenter).zIndex(1f))
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            M3SmallTopBar(
                title = { Text(text = playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                actions = {
                    if (canEdit) {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text(strings.editPlaylist) }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = { showMenu = false; renameText = playlist.name; renameDialog = true })
                                DropdownMenuItem(text = { Text(strings.cover) }, leadingIcon = { Icon(Icons.Filled.QueueMusic, null) }, onClick = { showMenu = false; coverPicker.launch(arrayOf("image/*")) })
                                DropdownMenuItem(text = { Text(strings.delete) }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = { showMenu = false; deleteConfirm = true })
                            }
                        }
                    }
                },
                colors = m3TopBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 220.dp,
            ),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .then(if (canEdit) Modifier.clickable { coverPicker.launch(arrayOf("image/*")) } else Modifier),
                    ) {
                        PlaylistCoverArt(
                            coverUri = playlist.coverUri,
                            tracks = plTracks,
                            placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                            placeholderIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            fallback = {
                                Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(64.dp))
                            }
                        )
                        if (canEdit) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${plTracks.size} ${strings.trackCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlayAll,
                            enabled = displayTracks.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.play, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onPlayRemix,
                            enabled = displayTracks.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.remix, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (canEdit) {
                            OutlinedButton(
                                onClick = { showAddTracks = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.addTracks, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    if (canEdit) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "custom" to strings.customOrder,
                                "alphabetical" to strings.alphabetical,
                                "artist" to strings.sortByArtist,
                                "timeAdded" to strings.timeAdded,
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = playlist.sortMode == mode,
                                    onClick = { onChangeSort(mode) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (displayTracks.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = if (playlist.isSystem) "No liked songs yet" else "No tracks yet. Tap \"${strings.addTracks}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            } else if (customOrder) {
                itemsIndexed(reorderItems, key = { _, t -> t.id }) { i, t ->
                    ReorderableItem(state = reorderableState, key = t.id) { isDragging ->
                        Box(modifier = Modifier.animateItem()) {
                            M3PlaylistTrackRow(
                                track = t,
                                isDragged = isDragging,
                                onClick = { onTrackClick(reorderItems.toList(), i) },
                                onMore = { trackMenuTrack = t },
                                dragHandleModifier = Modifier.draggableHandle(),
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(displayTracks, key = { _, t -> t.id }) { i, t ->
                    Box(
                        modifier = Modifier.animateItem(
                            fadeInSpec = androidx.compose.animation.core.tween(250),
                            fadeOutSpec = androidx.compose.animation.core.tween(200),
                            placementSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 380f)
                        )
                    ) {
                        M3PlaylistTrackRow(
                            track = t,
                            onClick = { onTrackClick(displayTracks, i) },
                            onMore = { trackMenuTrack = t },
                        )
                    }
                }
            }
        }
    }
    }

    trackMenuTrack?.let { menuTrack ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        M3BottomSheetWrapper(
            onDismissRequest = { trackMenuTrack = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                LineageListItemWithThumbnail(
                    headline = menuTrack.title,
                    supporting = menuTrack.artist,
                    thumbnailModel = menuTrack.albumArtUri,
                    placeholderIcon = Icons.Filled.MusicNote
                )
                Spacer(Modifier.height(8.dp))
                SegmentedColumn {
                    item {
                        BaseWidget(
                            icon = Icons.Filled.Delete,
                            title = strings.removeFromPlaylist,
                            onClick = { trackMenuTrack = null; onRemoveTrack(menuTrack.id) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/**
 * One track in a playlist. A non-null [dragHandleModifier] shows the drag handle used for custom order.
 */
@Composable
private fun M3PlaylistTrackRow(
    track: AudioTrack,
    onClick: () -> Unit,
    onMore: () -> Unit,
    isDragged: Boolean = false,
    dragHandleModifier: Modifier? = null,
) {
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragged) 6.dp else 0.dp,
        label = "m3TrackElevation"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .shadow(elevation, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = m3ItemCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (dragHandleModifier != null) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .then(dragHandleModifier)
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
