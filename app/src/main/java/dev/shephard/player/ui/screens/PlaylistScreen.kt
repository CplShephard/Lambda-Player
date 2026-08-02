@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package dev.shephard.player.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.filled.Shuffle
import dev.shephard.player.ui.miuix.Card
import dev.shephard.player.ui.miuix.CardDefaults
import dev.shephard.player.ui.miuix.Checkbox

import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.FloatingActionButton
import dev.shephard.player.ui.miuix.HorizontalDivider
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.IconButton
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.MiuixDrawerActionHeader
import dev.shephard.player.ui.navigation.PageTransitions
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.OutlinedTextField
import dev.shephard.player.ui.miuix.Text
import dev.shephard.player.ui.miuix.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.LayoutMode
import dev.shephard.player.player.LibraryViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.glass.GlassTint
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurface
import dev.shephard.player.ui.glass.blurSurfaceCompact
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
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
    onTrackClick: (List<AudioTrack>, Int, String?) -> Unit = { _, _, _ -> },
    onPlaylistRemixClick: (List<AudioTrack>, String?) -> Unit = { _, _ -> }
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

    // Playlist kapak resmi için kırpma çıktısı KALICI depolamaya (filesDir) yazılır.
    var playlistCoverCropOutputUri by remember { mutableStateOf<Uri?>(null) }
    var playlistCoverCropForIndex by remember { mutableStateOf<Int?>(null) }

    val playlistCoverCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = playlistCoverCropOutputUri
        val idx = playlistCoverCropForIndex
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null && idx != null && idx in playlists.indices) {
            val pl = playlists[idx]
            val updated = pl.copy(coverUri = output.toString())
            val all = playlists.toMutableList()
            all[idx] = updated
            scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
        }
        playlistCoverCropForIndex = null
    }

    fun launchPlaylistCoverCrop(sourceUri: Uri, index: Int) {
        val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
        val file = java.io.File(dir, "playlist_cover_${System.currentTimeMillis()}.jpg")
        val outputUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        playlistCoverCropOutputUri = outputUri
        playlistCoverCropForIndex = index

        val cropIntent = android.content.Intent("com.android.camera.action.CROP").apply {
            setDataAndType(sourceUri, "image/*")
            putExtra("crop", "true")
            putExtra("scale", true)
            // Playlist kapakları her zaman 1:1 (kare) olmalı.
            putExtra("outputX", 512)
            putExtra("outputY", 512)
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra("outputFormat", android.graphics.Bitmap.CompressFormat.JPEG.toString())
            putExtra("return-data", false)
            putExtra("noFaceDetection", true)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "playlist_cover", sourceUri)
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
            playlistCoverCropLauncher.launch(cropIntent)
        } else {
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null && index in playlists.indices) {
                    val pl = playlists[index]
                    val updated = pl.copy(coverUri = persisted.toString())
                    val all = playlists.toMutableList()
                    all[index] = updated
                    prefs.setPlaylistsJson(encodePlaylists(all))
                }
            }
        }
    }

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
            if (idx != null) {
                launchPlaylistCoverCrop(uri, idx)
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

    // MADDE 2 (bu tur) — playlist detayına girerken artık Theme/Playback/About sayfalarının
    // kullandığı GERÇEK Miuix NavDisplay varsayılan geçişi kullanılıyor (enterPush/exitPush
    // DEĞİL — o, InstallerX'in eski NavHost döneminden kalma bir taklitti ve kullanıcı
    // "saçma sapan" bularak bunun yerine Theme/Playback/About'un animasyonunu istedi).
    androidx.compose.animation.AnimatedContent(
        targetState = openIndex,
        transitionSpec = {
            if (targetState != null) {
                // Detaya giriş: Theme/Playback/About'a girerken kullanılan animasyonun aynısı.
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.enterSubmenu,
                    initialContentExit = PageTransitions.exitSubmenu
                )
            } else {
                // Geri: aynı animasyonun ters yönü.
                androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.popEnterSubmenu,
                    initialContentExit = PageTransitions.popExitSubmenu
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
                onPlayRemix = { if (plTracks.isNotEmpty()) onPlaylistRemixClick(plTracks, if (pl.isSystem) strings.likedSongs else pl.name) },
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
        val createLiquidGlassOn = LocalBlurEnabled.current
        MiuixDrawer(
            onDismissRequest = { showCreate = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(min = 260.dp)
            ) {
                Text(strings.createPlaylist, style = MiuixAppTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showCreate = false }) { Text(strings.cancel) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            val next = playlists + LocalPlaylist(name, emptyList(), createdAt = System.currentTimeMillis())
                            scope.launch { prefs.setPlaylistsJson(encodePlaylists(next)) }
                        }
                        showCreate = false
                    }) { Text(strings.save) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val pickerIdx = trackPickerForIndex
    if (pickerIdx != null) {
        val pickerLiquidGlassOn = LocalBlurEnabled.current
        MiuixDrawer(
            onDismissRequest = { trackPickerForIndex = null },
        ) {
            // MADDE 6 — "add tracks" drawer'ı alçaltıldı.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f)
                    .padding(20.dp)
            ) {
                Text(strings.addTracks, style = MiuixAppTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val pickerListState = rememberLazyListState()
                LazyColumn(
                    state = pickerListState,
                    modifier = Modifier.weight(1f).overScrollVertical(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tracks) { t ->
                        val checked = t.id in pickerSelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (checked) Modifier.background(
                                        if (pickerLiquidGlassOn) Color.White.copy(alpha = 0.12f)
                                        else MiuixAppTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    ) else Modifier
                                )
                                .clickable { pickerSelected = if (checked) pickerSelected - t.id else pickerSelected + t.id }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on -> pickerSelected = if (on) pickerSelected + t.id else pickerSelected - t.id }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.title, color = MiuixAppTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artist, style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { trackPickerForIndex = null }) { Text(strings.cancel) }
                    Spacer(Modifier.width(8.dp))
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
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Playlist menu drawer
    val menuIdx = playlistMenuIndex
    if (menuIdx != null) {
        val pl = playlists[menuIdx]
        val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
        val menuLiquidGlassOn = LocalBlurEnabled.current
        MiuixDrawer(
            onDismissRequest = { playlistMenuIndex = null },
        ) {
            // MADDE 6 — playlist menüsünde üç satır var; içerik kadar yer kaplıyor.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (pl.isSystem) strings.likedSongs else pl.name,
                            style = MiuixAppTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val dateStr = remember(pl.createdAt) {
                            if (pl.createdAt > 0) SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(pl.createdAt)) else ""
                        }
                        if (dateStr.isNotEmpty()) {
                            Text(
                                text = "${strings.createdAt}: $dateStr",
                                style = MiuixAppTheme.typography.bodySmall,
                                color = MiuixAppTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${plTracks.size} ${strings.trackCount}",
                        style = MiuixAppTheme.typography.bodySmall,
                        color = MiuixAppTheme.colorScheme.onSurfaceVariant
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
                    Icon(Icons.Filled.Edit, null, tint = MiuixAppTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.editPlaylist, color = MiuixAppTheme.colorScheme.onBackground)
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
                    Icon(if (pl.pinned) Icons.Filled.PinDrop else Icons.Filled.PushPin, null, tint = MiuixAppTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(if (pl.pinned) strings.unpinPlaylist else strings.pinPlaylist, color = MiuixAppTheme.colorScheme.onBackground)
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
                        Icon(Icons.Filled.Delete, null, tint = MiuixAppTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.delete, color = MiuixAppTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Edit playlist name
    val editIdx = editPlaylistIndex
    if (editIdx != null) {
        val editLiquidGlassOn = LocalBlurEnabled.current
        val editingPlaylist = playlists.getOrNull(editIdx)
        MiuixDrawer(
            onDismissRequest = { editPlaylistIndex = null },
        ) {
            // ÖNEMLİ: onCancel/onConfirm'de doğrudan `editPlaylistIndex = null` yerine
            // `rememberDrawerDismiss()` kullanıyoruz — aksi halde MiuixDrawer'ın çıkış
            // animasyonu (visible=false → WindowBottomSheet exit) hiç oynamadan drawer
            // aniden composition'dan kalkıyordu.
            val dismissDrawer = rememberDrawerDismiss()
            // MADDE 4 + MADDE 6 — Playlist düzenleme drawer'ı:
            //  * artık kapak düzenleme alanı da burada ve 1:1 (kare),
            //  * Cancel/Save yazı butonları yerine Miuix'in × / ✓ ikonları,
            //  * sabit `heightIn(min = 260.dp)` kaldırıldı; içerik kadar yer kaplıyor.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                MiuixDrawerActionHeader(
                    title = strings.editPlaylist,
                    onCancel = dismissDrawer,
                    onConfirm = {
                        val name = editPlaylistName.trim()
                        if (name.isNotEmpty() && editIdx in playlists.indices) {
                            val pl = playlists[editIdx]
                            val all = playlists.toMutableList()
                            all[editIdx] = pl.copy(name = name)
                            scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                        }
                        dismissDrawer()
                    }
                )
                Spacer(Modifier.height(16.dp))
                if (editingPlaylist != null && !editingPlaylist.isSystem) {
                    val editCoverTracks = remember(editingPlaylist, tracks, likedIds) {
                        resolvePlaylistTracks(editingPlaylist, tracks, likedIds)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(168.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant)
                            .bounceClick {
                                showCoverPickerForIndex = editIdx
                                coverPicker.launch(arrayOf("image/*"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val editCover = editingPlaylist.coverUri?.let { Uri.parse(it) }
                            ?: editCoverTracks.firstOrNull()?.albumArtUri
                        if (editCover != null) {
                            AsyncImage(
                                model = editCover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.LibraryMusic,
                                contentDescription = null,
                                tint = MiuixAppTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuixAppTheme.colorScheme.surface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MiuixAppTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = editPlaylistName,
                    onValueChange = { editPlaylistName = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun resolvePlaylistTracks(pl: LocalPlaylist, tracks: List<AudioTrack>, likedIds: List<Long> = emptyList()): List<AudioTrack> {
    if (pl.isSystem) {
        val trackMap = tracks.associateBy { it.id }
        return likedIds.mapNotNull { trackMap[it] }
    }
    val mapped = pl.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
    val collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }
    return when (pl.sortMode) {
        "alphabetical" -> mapped.sortedWith { a, b ->
            val byTitle = collator.compare(a.title.trim(), b.title.trim())
            if (byTitle != 0) byTitle else {
                val byArtist = collator.compare(a.artist.trim(), b.artist.trim())
                if (byArtist != 0) byArtist else a.id.compareTo(b.id)
            }
        }
        "artist" -> mapped.sortedWith { a, b ->
            val byArtist = collator.compare(a.artist.trim(), b.artist.trim())
            if (byArtist != 0) byArtist else {
                val byTitle = collator.compare(a.title.trim(), b.title.trim())
                if (byTitle != 0) byTitle else a.id.compareTo(b.id)
            }
        }
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
    val topBarState = dev.shephard.player.ui.components.rememberCollapsingTopBarState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.noPlaylistsYet,
                    style = MiuixAppTheme.typography.bodyLarge,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val playlistGridState = rememberLazyGridState()
            val playlistListState = rememberLazyListState()
            if (layout == LayoutMode.GRID) {
                val pinnedPlaylists = playlists.filter { it.pinned }
                val unpinnedPlaylists = playlists.filter { !it.pinned }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = playlistGridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                        .overScrollVertical(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        dev.shephard.player.ui.components.CollapsingPageTitle(
                            title = strings.playlists,
                            state = topBarState,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    if (pinnedPlaylists.size >= 1) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = strings.pinnedPlaylists,
                                style = MiuixAppTheme.typography.labelMedium,
                                color = MiuixAppTheme.colorScheme.primary,
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
                    state = playlistListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                        .overScrollVertical(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        dev.shephard.player.ui.components.CollapsingPageTitle(
                            title = strings.playlists,
                            state = topBarState,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    if (pinnedPlaylists.size >= 1) {
                        item {
                            Text(
                                text = strings.pinnedPlaylists,
                                style = MiuixAppTheme.typography.labelMedium,
                                color = MiuixAppTheme.colorScheme.primary,
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

        // MADDE 4 — Theme/Playback/About ile birebir aynı sabit üst başlık.
        dev.shephard.player.ui.components.CollapsingTopBar(
            title = strings.playlists,
            state = topBarState
        )

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 24.dp)
                .bounceClick(onClick = onCreate),
            shape = RoundedCornerShape(20.dp),
            containerColor = MiuixAppTheme.colorScheme.primary
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
    val liquidGlassOn = LocalBlurEnabled.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .then(
                // Liste elemanı: pahalı GPU blur yerine ucuz yarı saydam tint (performans)
                Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixAppTheme.colorScheme.background),
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
                                    MiuixAppTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MiuixAppTheme.colorScheme.tertiary.copy(alpha = 0.6f)
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
                        tint = MiuixAppTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (playlist.isSystem) strings.likedSongs else playlist.name,
                    style = MiuixAppTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixAppTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${plTracks.size} ${strings.trackCount}",
                    style = MiuixAppTheme.typography.bodySmall,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            }
            BouncyIconButton(
                onClick = onPlay,
                icon = Icons.Filled.PlayArrow,
                contentDescription = strings.play,
                tint = MiuixAppTheme.colorScheme.primary
            )
            BouncyIconButton(
                onClick = onMenu,
                icon = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint = MiuixAppTheme.colorScheme.onSurfaceVariant
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
    val liquidGlassOn = LocalBlurEnabled.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .then(
                // Liste elemanı: pahalı GPU blur yerine ucuz yarı saydam tint (performans)
                Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            )
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                                MiuixAppTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MiuixAppTheme.colorScheme.tertiary.copy(alpha = 0.6f)
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
                    tint = MiuixAppTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            }
            BouncyIconButton(
                onClick = onMenu,
                icon = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
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
                    .then(
                        // Liste elemanı: pahalı GPU blur yerine ucuz yarı saydam tint (performans)
                        Modifier.background(MiuixAppTheme.colorScheme.primary)
                    )
                    .bounceClick { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.play,
                    // MADDE 11 — daire accent (primary) renkle dolduruluyor ama ikon,
                    // Liquid Glass AÇIKKEN yine `primary` ile boyanıyordu: accent üstüne
                    // accent = görünmez ikon, yani "içi boş daire". Dolgu her iki durumda
                    // da `primary` olduğuna göre ikon HER ZAMAN `onPrimary` olmalı.
                    tint = MiuixAppTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (playlist.isSystem) strings.likedSongs else playlist.name,
            style = MiuixAppTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${plTracks.size} ${strings.trackCount}",
            style = MiuixAppTheme.typography.bodySmall,
            color = MiuixAppTheme.colorScheme.onSurfaceVariant
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
    onPlayRemix: () -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onAddTracks: () -> Unit,
    onPickCover: () -> Unit = {},
    onReorder: (List<AudioTrack>) -> Unit = {},
    onChangeSort: (String) -> Unit = {}
) {
    val liquidGlassOn = LocalBlurEnabled.current

    // OuterTune'daki LocalPlaylistScreen deseni: gerçek liste mutableStateListOf,
    // reorderableState onMove callback'i doğrudan bu listeyi günceller, drag bitince
    // (isAnyItemDragging false olunca) tek seferde onReorder tetiklenir.
    //
    // ÖNEMLİ BUGFIX: plTracks, üst ekranda tracks/likedIds değiştikçe (remember key'leri
    // yüzünden) sürükleme sırasında bile YENİDEN oluşan bir liste. Bu yüzden LaunchedEffect(plTracks)
    // her tetiklendiğinde reorderItems'ı resetlemek, kullanıcı henüz parmağını kaldırmadan
    // listeyi eski sıraya döndürüp item'ların "iç içe girmesine" (index kayması, yanlış item'ın
    // sürüklenmesi) sebep oluyordu. Çözüm: aktif drag sırasında (isAnyItemDragging) hiçbir zaman
    // resetleme yapma; sadece drag bitmişken ve gerçekten farklı bir veri geldiğinde senkronize et.
    val reorderItems = remember { mutableStateListOf<AudioTrack>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isReordering by remember { mutableStateOf(false) }

    LaunchedEffect(plTracks, isReordering) {
        if (!isReordering && reorderItems.map { it.id } != plTracks.map { it.id }) {
            reorderItems.clear()
            reorderItems.addAll(plTracks)
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // ÖNEMLİ BUGFIX: reorderableState'in verdiği from.index / to.index, LazyColumn'daki TÜM
    // item'lara göre mutlak konumdur (header, kapak/butonlar, sort selector gibi sürüklenemeyen
    // item{} blokları dahil). reorderItems ise sadece parça listesini tutar ve bu header'ları
    // içermez. İkisi arasındaki index farkını düzeltmezsek yanlış öğe taşınır/kaldırılır —
    // dokunulan öğe yerinden oynamaz, komşu satırlar ise (yanlışlıkla) titreşerek tepki verir.
    // Bu ekranda reorderable item'lardan önce İKİ ayrı item{} bloğu var: (1) geri tuşu + başlık/
    // parça sayısı satırı, (2) kapak resmi + play/remix/add-tracks butonları + sıralama seçici.
    // Bu yüzden sabit offset 2.
    val reorderableHeaderItemCount = 2
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState
    ) { from, to ->
        val fromIdx = from.index - reorderableHeaderItemCount
        val toIdx = to.index - reorderableHeaderItemCount
        if (fromIdx in reorderItems.indices && toIdx in reorderItems.indices) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                fromIdx to toIdx
            } else {
                currentDragInfo.first to toIdx
            }
            reorderItems.add(toIdx, reorderItems.removeAt(fromIdx))
        }
    }
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        isReordering = reorderableState.isAnyItemDragging
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                dragInfo = null
                if (from != to && reorderItems.map { it.id } != plTracks.map { it.id }) {
                    onReorder(reorderItems.toList())
                }
            }
        }
    }

    // MADDE 5 — Playlist detayına girerken artık Theme/Playback Settings ile birebir aynı
    // düz (flat) arkaplan kullanılıyor: temaya göre siyah (koyu mod) ya da beyaz (açık mod).
    // Önceden hiç arkaplan yoktu, bu yüzden alttaki wallpaper/BgEffect katmanı görünüyordu —
    // menülerle (Theme/Player/About) tutarsız bir görünüme neden oluyordu.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixAppTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp),
            // MADDE 2 — alphabetical / artist / timeAdded sıralamalarında parça kartları
            // dipdibe duruyor, arka plandaki card'lar iç içe geçmiş gibi görünüyordu.
            // MusicScreen'in liste görünümündeki ile aynı nefes payını veriyoruz.
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BouncyIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.cancel,
                        tint = MiuixAppTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (playlist.isSystem) strings.likedSongs else playlist.name,
                            style = MiuixAppTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MiuixAppTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${plTracks.size} ${strings.trackCount}",
                            style = MiuixAppTheme.typography.bodySmall,
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant
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
                        .background(MiuixAppTheme.colorScheme.surfaceVariant)
                        .then(if (!playlist.isSystem) Modifier.clickable { onPickCover() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (playlist.isSystem) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.linearGradient(
                                    colors = listOf(
                                        MiuixAppTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        MiuixAppTheme.colorScheme.tertiary.copy(alpha = 0.6f)
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
                            tint = MiuixAppTheme.colorScheme.primary,
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
                            .background(MiuixAppTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MiuixAppTheme.colorScheme.onSurface,
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
                            .then(
                                if (liquidGlassOn) Modifier.blurSurface(
                                    enabled = true,
                                    shape = RoundedCornerShape(14.dp),
                                    tint = GlassTint.ACCENT
                                )
                                else Modifier.background(MiuixAppTheme.colorScheme.primary)
                            )
                            .clickable(enabled = plTracks.isNotEmpty()) { onPlayAll() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = strings.play,
                            tint = if (liquidGlassOn) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.play,
                            color = if (liquidGlassOn) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (liquidGlassOn) Modifier.blurSurface(enabled = true, shape = RoundedCornerShape(14.dp))
                                else Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant)
                            )
                            .clickable(enabled = plTracks.isNotEmpty()) { onPlayRemix() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Shuffle,
                            contentDescription = strings.remix,
                            tint = MiuixAppTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.remix,
                            color = MiuixAppTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!playlist.isSystem) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (liquidGlassOn) Modifier.blurSurface(enabled = true, shape = RoundedCornerShape(14.dp))
                                    else Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant)
                                )
                                .clickable { onAddTracks() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = strings.addTracks,
                                tint = MiuixAppTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.addTracks,
                                color = MiuixAppTheme.colorScheme.onBackground
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
                                style = MiuixAppTheme.typography.labelMedium,
                                color = if (selected) MiuixAppTheme.colorScheme.onPrimary else MiuixAppTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.surfaceVariant)
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
                        style = MiuixAppTheme.typography.bodyMedium,
                        color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                if (playlist.sortMode == "custom" && !playlist.isSystem) {
                    itemsIndexed(reorderItems, key = { _, t -> t.id }) { i, t ->
                        ReorderableItem(
                            state = reorderableState,
                            key = t.id
                        ) { isDragging ->
                            DraggablePlaylistTrackRow(
                                track = t,
                                isDragged = isDragging,
                                onTrackClick = { onTrackClick(reorderItems.toList(), i) },
                                onRemove = { onRemoveTrack(t.id) },
                                dragHandleModifier = Modifier.draggableHandle()
                            )
                        }
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
    val liquidGlassOn = LocalBlurEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .then(
                // Liste elemanı: pahalı GPU blur yerine ucuz yarı saydam tint (performans)
                Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                    tint = MiuixAppTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = MiuixAppTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                style = MiuixAppTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(track.formattedDuration(), style = MiuixAppTheme.typography.labelSmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun DraggablePlaylistTrackRow(
    track: AudioTrack,
    isDragged: Boolean,
    onTrackClick: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier
) {
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragged) 6.dp else 0.dp,
        label = "playlistItemElevation"
    )
    // MADDE 3 — custom sıralamada, diğer üç sıralamanın aksine parçaların arkasında hiç
    // kart yoktu (arka plan `Color.Transparent`'tı). Artık `PlaylistTrackRow` ile BİREBİR
    // aynı kartı kullanıyor: aynı köşe yarıçapı (20.dp) ve aynı yarı saydam
    // surfaceVariant dolgusu. Sürükleme sırasında bunun ÜSTÜNE accent tint biniyor,
    // böylece taşınan öğe hâlâ ayırt edilebiliyor.
    val rowShape = RoundedCornerShape(20.dp)
    val cardColor = MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .miuixWidgetClick(pressScale = 0.94f, maxTiltDegrees = 7f) { onTrackClick() }
            .shadow(elevation, rowShape)
            .zIndex(if (isDragged) 1f else 0f)
            .clip(rowShape)
            .background(cardColor, rowShape)
            .then(
                if (isDragged) Modifier.background(
                    MiuixAppTheme.colorScheme.primary.copy(alpha = 0.14f),
                    rowShape
                ) else Modifier
            )
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                Icon(Icons.Filled.MusicNote, null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = MiuixAppTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = MiuixAppTheme.colorScheme.onSurfaceVariant, style = MiuixAppTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(track.formattedDuration(), style = MiuixAppTheme.typography.labelSmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Reorder",
            tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(28.dp)
                .then(dragHandleModifier)
        )
    }
}
