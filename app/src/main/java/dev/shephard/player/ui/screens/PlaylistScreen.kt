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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.shephard.player.ui.components.captureForTopBarBlur
import dev.shephard.player.ui.navigation.PageTransitions
import dev.shephard.player.ui.navigation.SubmenuNavGuard
import dev.shephard.player.ui.components.rememberDrawerDismiss
import dev.shephard.player.ui.miuix.OutlinedTextField
import dev.shephard.player.ui.miuix.Text
import dev.shephard.player.ui.miuix.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.shephard.player.ui.glass.miuixBlurSurface
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.miuixWidgetClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
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

    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var openIndex by remember { mutableStateOf<Int?>(null) }

val playlistDetailGuard = remember { SubmenuNavGuard() }
    var trackPickerForIndex by remember { mutableStateOf<Int?>(null) }
    var pickerSelected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showCoverPickerForIndex by remember { mutableStateOf<Int?>(null) }
    var playlistMenuIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistIndex by remember { mutableStateOf<Int?>(null) }
    var editPlaylistName by remember { mutableStateOf("") }
    var showRemoveCoverConfirm by remember { mutableStateOf(false) }
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<LocalPlaylist?>(null) }

var newCoverUri by remember { mutableStateOf<Uri?>(null) }
    var showRemoveCreateCoverConfirm by remember { mutableStateOf(false) }
    var newCoverCropOutputUri by remember { mutableStateOf<Uri?>(null) }

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

if (context.contentResolver.getType(sourceUri) == "image/gif") {
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
            return
        }

val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
        val file = java.io.File(dir, "playlist_cover_${System.currentTimeMillis()}.jpg")
        val outputUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        playlistCoverCropOutputUri = outputUri
        playlistCoverCropForIndex = index

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

val newCoverCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = newCoverCropOutputUri
        if (result.resultCode == android.app.Activity.RESULT_OK && output != null) {
            newCoverUri = output
        }
    }

fun launchNewCoverCrop(sourceUri: Uri) {

if (context.contentResolver.getType(sourceUri) == "image/gif") {
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null) newCoverUri = persisted
            }
            return
        }

val dir = java.io.File(context.filesDir, "persisted_covers").apply { mkdirs() }
        val file = java.io.File(dir, "playlist_cover_${System.currentTimeMillis()}.jpg")
        val outputUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        newCoverCropOutputUri = outputUri

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
            newCoverCropLauncher.launch(cropIntent)
        } else {
            scope.launch {
                val persisted = dev.shephard.player.player.ImagePersistence.persistCover(context, sourceUri)
                if (persisted != null) newCoverUri = persisted
            }
        }
    }

val newCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            launchNewCoverCrop(uri)
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

androidx.activity.compose.BackHandler(enabled = openIndex != null) {
        playlistDetailGuard.pop { openIndex = null }
    }

androidx.compose.animation.AnimatedContent(
        targetState = openIndex,
        transitionSpec = {
            if (targetState != null) {

androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.enterSubmenu,
                    initialContentExit = PageTransitions.exitSubmenu,
                    targetContentZIndex = 1f
                )
            } else {

androidx.compose.animation.ContentTransform(
                    targetContentEnter = PageTransitions.popEnterSubmenu,
                    initialContentExit = PageTransitions.popExitSubmenu,
                    targetContentZIndex = 0f
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
            onOpen = { idx -> playlistDetailGuard.push(openIndex, idx) { openIndex = idx } },
            onPlay = { pl ->
                val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
                if (plTracks.isNotEmpty()) onTrackClick(plTracks, 0, if (pl.isSystem) strings.likedSongs else pl.name)
            },
            onMenu = { playlistMenuIndex = it },
            onCreate = { showCreate = true; newName = ""; newCoverUri = null }
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
                onBack = { playlistDetailGuard.pop { openIndex = null } },
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

val dismissDrawer = rememberDrawerDismiss()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                MiuixDrawerActionHeader(
                    title = strings.createPlaylist,
                    onCancel = dismissDrawer,
                    onConfirm = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            val next = playlists + LocalPlaylist(
                                name,
                                emptyList(),
                                coverUri = newCoverUri?.toString(),
                                createdAt = System.currentTimeMillis()
                            )
                            scope.launch { prefs.setPlaylistsJson(encodePlaylists(next)) }
                        }
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
                        .bounceClick { newCoverPicker.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (newCoverUri != null) {
                        AsyncImage(
                            model = newCoverUri,
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
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

val pickerIdx = trackPickerForIndex
    if (pickerIdx != null) {
        val pickerLiquidGlassOn = LocalBlurEnabled.current
        MiuixDrawer(
            onDismissRequest = { trackPickerForIndex = null },
        ) {

val dismissDrawer = rememberDrawerDismiss()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                MiuixDrawerActionHeader(
                    title = strings.addTracks,
                    onCancel = dismissDrawer,
                    onConfirm = {
                        val pl = playlists[pickerIdx]
                        val existingIds = pl.trackIds.toMutableList()
                        val toAdd = pickerSelected - pl.trackIds.toSet()
                        val toRemove = pl.trackIds.toSet() - pickerSelected
                        val newIds = existingIds.filterNot { it in toRemove } + toAdd
                        val updated = pl.copy(trackIds = newIds)
                        val all = playlists.toMutableList()
                        all[pickerIdx] = updated
                        scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                        dismissDrawer()
                    }
                )
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(20.dp))
            }
        }
    }

val menuIdx = playlistMenuIndex
    if (menuIdx != null) {
        val pl = playlists[menuIdx]
        val plTracks = resolvePlaylistTracks(pl, tracks, likedIds)
        val menuLiquidGlassOn = LocalBlurEnabled.current
        MiuixDrawer(
            onDismissRequest = { playlistMenuIndex = null },
        ) {

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
                                playlistToDelete = pl
                                showDeletePlaylistConfirm = true
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

val editIdx = editPlaylistIndex
    if (editIdx != null) {
        val editLiquidGlassOn = LocalBlurEnabled.current
        val editingPlaylist = playlists.getOrNull(editIdx)
        MiuixDrawer(
            onDismissRequest = { editPlaylistIndex = null },
        ) {

val dismissDrawer = rememberDrawerDismiss()

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
                    if (editingPlaylist.coverUri != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.removeCover,
                            color = MiuixAppTheme.colorScheme.error,
                            style = MiuixAppTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .bounceClick { showRemoveCoverConfirm = true }
                                .padding(6.dp)
                        )
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
                            val idx = editPlaylistIndex
                            if (idx != null && idx in playlists.indices) {
                                val pl = playlists[idx]
                                val all = playlists.toMutableList()
                                all[idx] = pl.copy(coverUri = null)
                                scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                            }
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

if (showRemoveCreateCoverConfirm) {
        dev.shephard.player.ui.miuix.MiuixDialog(
            onDismissRequest = { showRemoveCreateCoverConfirm = false },
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
                        onClick = { showRemoveCreateCoverConfirm = false },
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
                            showRemoveCreateCoverConfirm = false
                            newCoverUri = null
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

if (showDeletePlaylistConfirm && playlistToDelete != null) {
        val toDel = playlistToDelete
        dev.shephard.player.ui.miuix.MiuixDeleteConfirmationDialog(
            show = true,
            title = strings.delete,
            message = strings.removePlaylistConfirm,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onDismiss = {
                showDeletePlaylistConfirm = false
                playlistToDelete = null
            },
            onConfirm = {
                showDeletePlaylistConfirm = false
                playlistToDelete = null
                if (toDel != null) {
                    val all = playlists.toMutableList().filterNot { it.name == toDel.name && it.isSystem == toDel.isSystem }
                    scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                }
            }
        )
    }
}

internal fun resolvePlaylistTracks(pl: LocalPlaylist, tracks: List<AudioTrack>, likedIds: List<Long> = emptyList()): List<AudioTrack> {
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

Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            dev.shephard.player.ui.components.InstallerXTopBar(
                title = strings.playlists,
                state = topBarState
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (playlists.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .captureForTopBarBlur(topBarState)
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
                val sortedPlaylists = remember(playlists) {
                    val (pinned, unpinned) = playlists.partition { it.pinned }
                    pinned + unpinned
                }
                val hasPinned = playlists.any { it.pinned }
                if (layout == LayoutMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = playlistGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .captureForTopBarBlur(topBarState)
                            .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                            .overScrollVertical(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            end = 16.dp,
                            bottom = 200.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "pinned_header", span = { GridItemSpan(2) }) {
                            AnimatedVisibility(
                                visible = hasPinned,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = strings.pinnedPlaylists,
                                    style = MiuixAppTheme.typography.labelMedium,
                                    color = MiuixAppTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        items(
                            count = sortedPlaylists.size,
                            key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }
                        ) { i ->
                            val pl = sortedPlaylists[i]
                            val realIdx = playlists.indexOf(pl)
                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                            Box(modifier = Modifier.animateItem()) {
                                PlaylistGridCard(playlist = pl, plTracks = plTracks, strings = strings,
                                    onClick = { onOpen(realIdx) }, onMenu = { onMenu(realIdx) }, onPlay = { onPlay(pl) })
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = playlistListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .captureForTopBarBlur(topBarState)
                            .nestedScroll(topBarState.scrollBehavior.nestedScrollConnection)
                            .overScrollVertical(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            end = 16.dp,
                            bottom = 200.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "pinned_header") {
                            AnimatedVisibility(
                                visible = hasPinned,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = strings.pinnedPlaylists,
                                    style = MiuixAppTheme.typography.labelMedium,
                                    color = MiuixAppTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                        items(
                            count = sortedPlaylists.size,
                            key = { i -> sortedPlaylists[i].name + "_" + sortedPlaylists[i].createdAt }
                        ) { i ->
                            val pl = sortedPlaylists[i]
                            val realIdx = playlists.indexOf(pl)
                            val plTracks = remember(pl, tracks, likedIds) { resolvePlaylistTracks(pl, tracks, likedIds) }
                            Box(modifier = Modifier.animateItem()) {
                                PlaylistListCard(playlist = pl, plTracks = plTracks, strings = strings,
                                    onClick = { onOpen(realIdx) }, onMenu = { onMenu(realIdx) }, onPlay = { onPlay(pl) })
                            }
                        }
                    }
                }
            }
        }
    }

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

Modifier.background(MiuixAppTheme.colorScheme.primary)
                    )
                    .bounceClick { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.play,

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
private fun PlaylistDetailTopBar(
    title: String,
    cover: android.net.Uri?,
    onBack: () -> Unit,
    collapse: Float,
    pageBackdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop?
) {
    val cs = MiuixAppTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .then(
                if (pageBackdrop != null) {
                    Modifier.miuixBlurSurface(
                        backdrop = pageBackdrop,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        blurRadius = 70f,
                        tintAlpha = if (collapse > 0.01f) (0.68f + collapse * 0.27f).coerceIn(0f, 0.95f) else 0f,
                        fallbackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                } else {
                    Modifier.background(cs.background.copy(alpha = collapse))
                }
            )
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {

Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(cs.surfaceVariant.copy(alpha = 0.75f))
                .bounceClick { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = cs.onBackground
            )
        }

Row(
            modifier = Modifier.graphicsLayer { alpha = collapse },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = cs.onBackground
            )
        }
    }
}

@Composable
internal fun PlaylistDetailView(
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

val detailPageBackdrop = if (liquidGlassOn) rememberLayerBackdrop() else null

val reorderItems = remember { mutableStateListOf<AudioTrack>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isReordering by remember { mutableStateOf(false) }

var pendingCommittedOrder by remember { mutableStateOf<List<Long>?>(null) }

LaunchedEffect(plTracks, isReordering) {
        if (isReordering) return@LaunchedEffect
        val pending = pendingCommittedOrder
        if (pending != null) {
            if (plTracks.map { it.id } == pending) {

pendingCommittedOrder = null
            }

return@LaunchedEffect
        }
        if (reorderItems.map { it.id } != plTracks.map { it.id }) {
            reorderItems.clear()
            reorderItems.addAll(plTracks)
        }
    }

val listState = androidx.compose.foundation.lazy.rememberLazyListState()

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
        if (reorderableState.isAnyItemDragging) {

pendingCommittedOrder = null
        }
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                dragInfo = null
                if (from != to && reorderItems.map { it.id } != plTracks.map { it.id }) {
                    pendingCommittedOrder = reorderItems.map { it.id }
                    onReorder(reorderItems.toList())
                }
            }
        }
    }

val detailTitle = if (playlist.isSystem) strings.likedSongs else playlist.name
    val detailCollapse by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (index > 0) 1f else (offset.toFloat() / 120f).coerceIn(0f, 1f)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixAppTheme.colorScheme.background)
    ) {

PlaylistDetailTopBar(
            title = detailTitle,
            cover = playlist.coverUri?.let { Uri.parse(it) }
                ?: plTracks.firstOrNull()?.albumArtUri,
            onBack = onBack,
            collapse = detailCollapse,
            pageBackdrop = detailPageBackdrop
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .then(
                    detailPageBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier
                )
                .fillMaxSize()
                .overScrollVertical(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 200.dp),

verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = detailTitle,
                    style = MiuixAppTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MiuixAppTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp)
                        .graphicsLayer { alpha = 1f - detailCollapse }
                )
                Text(
                    text = "${plTracks.size} ${strings.trackCount}",
                    style = MiuixAppTheme.typography.bodySmall,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
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
                            .background(MiuixAppTheme.colorScheme.primary)
                            .clickable(enabled = plTracks.isNotEmpty()) { onPlayAll() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = strings.play,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.play,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MiuixAppTheme.colorScheme.surfaceVariant)
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
                                .background(MiuixAppTheme.colorScheme.surfaceVariant)
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
