// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 + M3 padding/radius + bottom sheet menu
package dev.shephard.player.ui.screens.m3

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.m3.BaseWidget
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.components.m3.SegmentedColumn
import dev.shephard.player.ui.i18n.Strings
import dev.shephard.player.ui.screens.LocalPlaylist
import dev.shephard.player.ui.screens.encodePlaylists
import dev.shephard.player.ui.screens.parsePlaylists
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    onAddTracks: () -> Unit,
    onPickCover: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(text = playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backContentDescription)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text(strings.addTracks) }, leadingIcon = { Icon(Icons.Filled.Add, null) }, onClick = { showMenu = false; showAddTracks = true })
                            DropdownMenuItem(text = { Text(strings.editPlaylist) }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = { showMenu = false; renameText = playlist.name; renameDialog = true })
                            DropdownMenuItem(text = { Text(strings.cover) }, leadingIcon = { Icon(Icons.Filled.QueueMusic, null) }, onClick = { showMenu = false; coverPicker.launch(arrayOf("image/*")) })
                            DropdownMenuItem(text = { Text(strings.delete) }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = { showMenu = false; deleteConfirm = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = onPlayAll,
                    icon = { Icon(Icons.Filled.PlayArrow, null) },
                    text = { Text(strings.play) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
                ExtendedFloatingActionButton(
                    onClick = onPlayRemix,
                    icon = { Icon(Icons.Filled.Shuffle, null) },
                    text = { Text(strings.remix) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playlist.coverUri != null) {
                            AsyncImage(
                                model = playlist.coverUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(64.dp))
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${plTracks.size} tracks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(plTracks, key = { it.id }) { track ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    LineageListItemWithThumbnail(
                        headline = track.title,
                        supporting = track.artist,
                        thumbnailModel = track.albumArtUri,
                        placeholderIcon = Icons.Filled.MusicNote,
                        trailingIcon = Icons.Filled.MoreVert,
                        onClick = { onTrackClick(listOf(track), 0) },
                        onTrailingClick = { trackMenuTrack = track }
                    )
                }
            }
        }
    }

    trackMenuTrack?.let { menuTrack ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
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
