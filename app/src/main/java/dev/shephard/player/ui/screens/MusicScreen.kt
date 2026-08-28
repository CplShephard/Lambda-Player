@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package dev.shephard.player.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import dev.shephard.player.ui.miuix.Button
import dev.shephard.player.ui.miuix.CircularProgressIndicator
import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.HorizontalDivider
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.IconButton
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.MiuixDrawerActionHeader
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.OutlinedTextField
import dev.shephard.player.ui.miuix.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import dev.shephard.player.ui.miuix.TextButton
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
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurface
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun MusicScreen(
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

    var pendingDeleteUri by remember { mutableStateOf<android.net.Uri?>(null) }

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

    val topBarState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            dev.shephard.player.ui.components.InstallerXTopBar(
                title = strings.music,
                state = topBarState
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !permissionState.hasPermission -> {
                    PermissionRequest(onRequest = permissionState.requestPermission)
                }
                isLoading -> {
                    LoadingState()
                }
                hasScanned && tracks.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    val listState = rememberLazyListState()
                    val gridState = rememberLazyGridState()

                    if (musicsLayout == LayoutMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .captureForTopBarBlur(topBarState)
                                .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                                .overScrollVertical(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = innerPadding.calculateTopPadding() + 8.dp,
                                bottom = 200.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gridItemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                GridTrackCard(
                                    track = track,
                                    onClick = { onTrackClick(tracks, index) },
                                    onMenuClick = { selectedTrackForMenu = track }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .captureForTopBarBlur(topBarState)
                                .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                                .overScrollVertical(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = innerPadding.calculateTopPadding() + 8.dp,
                                bottom = 200.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                                TrackRow(
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

    selectedTrackForMenu?.let { track ->
        val menuLiquidGlassOn = LocalBlurEnabled.current
        MiuixDrawer(
            onDismissRequest = { selectedTrackForMenu = null },
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                            Icon(Icons.Filled.MusicNote, null, tint = MiuixAppTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                        Text(track.album, style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .bounceClick { trackToEdit = track; selectedTrackForMenu = null }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, null, tint = MiuixAppTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.editMusic, color = MiuixAppTheme.colorScheme.onBackground)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .bounceClick { trackToDelete = track; selectedTrackForMenu = null }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, null, tint = MiuixAppTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.delete, color = MiuixAppTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    trackToDelete?.let { track ->
        dev.shephard.player.ui.miuix.MiuixDialog(
            onDismissRequest = { trackToDelete = null },
            title = strings.delete,
            text = {
                Text(
                    text = strings.deleteTrackConfirm,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dev.shephard.player.ui.miuix.TextButton(
                        onClick = { trackToDelete = null },
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = strings.cancel,
                            style = MiuixAppTheme.typography.labelLarge,
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    dev.shephard.player.ui.miuix.TextButton(
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.primary.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = strings.delete,
                            style = MiuixAppTheme.typography.labelLarge,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        )
    }

    trackToEdit?.let { track ->
        EditMusicDrawer(
            track = track,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            onDismiss = { trackToEdit = null }
        )
    }
}

@Composable
private fun GridTrackCard(
    track: AudioTrack,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val liquidGlassOn = LocalBlurEnabled.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .then(

                Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                    tint = MiuixAppTheme.colorScheme.primary,
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
                    style = MiuixAppTheme.typography.bodyMedium,
                    color = MiuixAppTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MiuixAppTheme.typography.bodySmall,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, null, tint = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TrackRow(track: AudioTrack, onClick: () -> Unit, onMenuClick: () -> Unit) {
    val liquidGlassOn = LocalBlurEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .then(

                Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                    tint = MiuixAppTheme.colorScheme.primary
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
                style = MiuixAppTheme.typography.bodyLarge,
                color = MiuixAppTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MiuixAppTheme.typography.bodyMedium,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = track.formattedDuration(),
            style = MiuixAppTheme.typography.labelMedium,
            color = MiuixAppTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.MoreVert, null, tint = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EditMusicDrawer(
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
    var coverUri by remember { mutableStateOf<android.net.Uri?>(existing?.coverUri?.let { android.net.Uri.parse(it) }) }
    var showRemoveCoverConfirm by remember { mutableStateOf(false) }
    val coverScope = rememberCoroutineScope()

    // wallpaper gibi metin tercihleri kadar kalıcı olmalı, sadece seçilen content:

    var coverCropOutputUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val coverCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = coverCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            coverUri = output
        }
    }

    fun launchCoverCrop(sourceUri: android.net.Uri) {

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
        val outputUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        coverCropOutputUri = outputUri

        val cropIntent = android.content.Intent("com.android.camera.action.CROP").apply {
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
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "cover", sourceUri)
        }

        val resolvedActivities = context.packageManager.queryIntentActivities(cropIntent, 0)
        for (info in resolvedActivities) {
            val packageName = info.activityInfo?.packageName ?: continue
            try {
                context.grantUriPermission(
                    packageName,
                    outputUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            launchCoverCrop(uri)
        }
    }

    val editMusicLiquidGlassOn = LocalBlurEnabled.current
    MiuixDrawer(
        onDismissRequest = onDismiss,
    ) {

        val dismissDrawer = rememberDrawerDismiss()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            MiuixDrawerActionHeader(
                title = strings.editMusic,
                onCancel = dismissDrawer,
                onConfirm = {
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
                    dismissDrawer()
                }
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(168.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixAppTheme.colorScheme.surfaceVariant)
                    .bounceClick { coverPicker.launch(arrayOf("image/*")) },
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
                    Icon(Icons.Filled.MusicNote, null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MiuixAppTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, null, tint = MiuixAppTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }

            if (coverUri != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showRemoveCoverConfirm = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.removeCover, color = MiuixAppTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text(strings.title) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = artistText,
                onValueChange = { artistText = it },
                label = { Text(strings.artist) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = albumText,
                onValueChange = { albumText = it },
                label = { Text(strings.album) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showRemoveCoverConfirm) {
        dev.shephard.player.ui.miuix.MiuixDialog(
            onDismissRequest = { showRemoveCoverConfirm = false },
            title = strings.removeCover,
            text = {
                Text(
                    text = strings.removeCoverConfirm,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dev.shephard.player.ui.miuix.TextButton(
                        onClick = { showRemoveCoverConfirm = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = strings.cancel,
                            style = MiuixAppTheme.typography.labelLarge,
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    dev.shephard.player.ui.miuix.TextButton(
                        onClick = {
                            showRemoveCoverConfirm = false
                            coverUri = null
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.primary.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = strings.removeCover,
                            style = MiuixAppTheme.typography.labelLarge,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
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
            tint = MiuixAppTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "Access your music",
            style = MiuixAppTheme.typography.titleLarge,
            color = MiuixAppTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Lambda Player needs permission to read audio files stored on this device.",
            style = MiuixAppTheme.typography.bodyMedium,
            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onRequest,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Grant Access")
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MiuixAppTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState() {
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
            tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "No music found",
            style = MiuixAppTheme.typography.titleLarge,
            color = MiuixAppTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Add audio files to your device storage to see them here.",
            style = MiuixAppTheme.typography.bodyMedium,
            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
