// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens.m3

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.rememberAudioPermissionState
import dev.shephard.player.ui.glass.LocalWallpaperEnabled
import dev.shephard.player.ui.glass.wallpaperAdaptiveTextColor
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Material 3 Music page. Content mirrors the Miuix MusicScreen with M3
 * components only (TopAppBar, Cards, AlertDialogs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreenM3(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
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
        containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(strings.music) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalWallpaperEnabled.current) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                    navigationIconContentColor = wallpaperAdaptiveTextColor(fallback = MaterialTheme.colorScheme.onSurface),
                ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !permissionState.hasPermission -> {
                    M3PermissionRequest(onRequest = permissionState.requestPermission)
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                hasScanned && tracks.isEmpty() -> {
                    M3EmptyState(strings)
                }
                else -> {
                    if (musicsLayout == LayoutMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = innerPadding.calculateTopPadding() + 8.dp,
                                bottom = if (hasMiniPlayer) 200.dp else 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gridItemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                M3GridTrackCard(
                                    track = track,
                                    onClick = { onTrackClick(tracks, index) },
                                    onMenuClick = { selectedTrackForMenu = track }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = innerPadding.calculateTopPadding() + 8.dp,
                                bottom = if (hasMiniPlayer) 200.dp else 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                M3TrackRow(
                                    track = track,
                                    onClick = { onTrackClick(tracks, index) },
                                    onMenuClick = { selectedTrackForMenu = track }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Track menu (M3 dialog) ───────────────────────────────────────────────
    selectedTrackForMenu?.let { track ->
        AlertDialog(
            onDismissRequest = { selectedTrackForMenu = null },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            var loaded by remember { mutableStateOf(false) }
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onState = { loaded = it is AsyncImagePainter.State.Success }
                            )
                            if (!loaded) {
                                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = track.title,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = track.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { trackToEdit = track; selectedTrackForMenu = null }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.editMusic)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { trackToDelete = track; selectedTrackForMenu = null }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.delete, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedTrackForMenu = null }) { Text(strings.cancel) }
            },
        )
    }

    // ── Delete confirmation + permission flows (same as Miuix page) ─────────
    trackToDelete?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text(strings.delete) },
            text = {
                Text(strings.deleteTrackConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
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
                                            android.provider.MediaStore.createDeleteRequest(
                                                resolver, listOf(toDelete.uri)
                                            ).intentSender
                                        }.getOrNull()
                                    android.os.Build.VERSION.SDK_INT >= 29 ->
                                        (e as? android.app.RecoverableSecurityException)
                                            ?.userAction?.actionIntent?.intentSender
                                    else -> null
                                }
                                if (intentSender != null) {
                                    val request = androidx.activity.result.IntentSenderRequest
                                        .Builder(intentSender).build()
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
        M3EditTrackDialog(
            track = track,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            onDismiss = { trackToEdit = null }
        )
    }
}

@Composable
private fun M3GridTrackCard(
    track: AudioTrack,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                var artLoaded by remember(track.id) { mutableStateOf(false) }
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { artLoaded = it is AsyncImagePainter.State.Success }
                )
                if (!artLoaded) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun M3TrackRow(track: AudioTrack, onClick: () -> Unit, onMenuClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                var artLoaded by remember(track.id) { mutableStateOf(false) }
                AsyncImage(
                    model = track.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    onState = { state -> artLoaded = state is AsyncImagePainter.State.Success }
                )
                if (!artLoaded) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
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

            Text(
                text = track.formattedDuration(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun M3PermissionRequest(onRequest: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = strings.accessYourMusic,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = strings.permissionDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onRequest,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(strings.grantAccess)
        }
    }
}

@Composable
private fun M3EmptyState(strings: dev.shephard.player.ui.i18n.Strings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = strings.noSongsToPlay,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = strings.emptyLibraryHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * M3 version of the Miuix EditMusicDrawer: same fields / cover crop flow.
 */
@Composable
private fun M3EditTrackDialog(
    track: AudioTrack,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val existing = remember(track.id) { libraryViewModel.getOverride(track.id) }

    var titleText by remember { mutableStateOf(existing?.title ?: track.title) }
    var artistText by remember { mutableStateOf(existing?.artist ?: track.artist) }
    var albumText by remember { mutableStateOf(existing?.album ?: track.album) }
    var coverUri by remember { mutableStateOf<Uri?>(existing?.coverUri?.let { Uri.parse(it) }) }
    var showRemoveCoverConfirm by remember { mutableStateOf(false) }
    val coverScope = rememberCoroutineScope()

    var coverCropOutputUri by remember { mutableStateOf<Uri?>(null) }

    val coverCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = coverCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            coverUri = output
        }
    }

    fun launchCoverCrop(sourceUri: Uri) {
        val mimeType = context.contentResolver.getType(sourceUri)
        if (mimeType == "image/gif") {
            coverScope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null) coverUri = persisted
            }
            return
        }

        val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
        val file = java.io.File(dir, "cover_${System.currentTimeMillis()}.jpg")
        val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        coverCropOutputUri = outputUri

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
            clipData = android.content.ClipData.newUri(context.contentResolver, "cover", sourceUri)
        }

        val resolvedActivities = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolvedActivities) {
            val packageName = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    packageName,
                    outputUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
        }

        if (resolvedActivities.isNotEmpty()) {
            coverCropLauncher.launch(cropIntent)
        } else {
            coverScope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null) coverUri = persisted
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            launchCoverCrop(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editMusic) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(120.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { coverPicker.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    var loaded by remember { mutableStateOf(false) }
                    val displayUri = coverUri ?: track.albumArtUri
                    AsyncImage(
                        model = displayUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { loaded = it is AsyncImagePainter.State.Success }
                    )
                    if (!loaded) {
                        Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }

                if (coverUri != null) {
                    TextButton(
                        onClick = { showRemoveCoverConfirm = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(strings.removeCover, color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(strings.title) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artistText,
                    onValueChange = { artistText = it },
                    label = { Text(strings.artist) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = albumText,
                    onValueChange = { albumText = it },
                    label = { Text(strings.album) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    libraryViewModel.saveTrackOverride(
                        trackId = track.id,
                        title = titleText,
                        artist = artistText,
                        album = albumText,
                        coverUri = coverUri?.toString()
                    )
                    val updatedTrack = track.copy(
                        title = titleText.ifBlank { track.title },
                        artist = artistText.ifBlank { track.artist },
                        album = albumText.ifBlank { track.album },
                        albumArtUri = coverUri ?: track.albumArtUri
                    )
                    playerViewModel.notifyTrackUpdated(updatedTrack)
                    onDismiss()
                },
            ) { Text(strings.done) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )

    if (showRemoveCoverConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveCoverConfirm = false },
            title = { Text(strings.removeCover) },
            text = { Text(strings.removeCoverConfirm, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showRemoveCoverConfirm = false; coverUri = null }) {
                    Text(strings.removeCover)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveCoverConfirm = false }) { Text(strings.cancel) }
            },
        )
    }
}
