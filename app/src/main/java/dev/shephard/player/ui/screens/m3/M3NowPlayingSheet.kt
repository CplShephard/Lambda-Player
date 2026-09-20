// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 - Now Playing Screen
package dev.shephard.player.ui.screens.m3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3NowPlayingSheet(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val track = state.currentTrack
    val strings = LocalStrings.current

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    val configuration = LocalConfiguration.current
    val dragOffsetInitialHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val dragOffset = remember { Animatable(dragOffsetInitialHeight) }
    val dragScope = rememberCoroutineScope()

    var hasEnteredRest by remember { mutableStateOf(false) }
    val enterSpring = remember { spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 180f) }

    LaunchedEffect(Unit) {
        hasEnteredRest = false
        dragOffset.animateTo(0f, animationSpec = enterSpring)
        hasEnteredRest = true
    }

    var measuredHeightPx by remember { mutableFloatStateOf(with(density) { configuration.screenHeightDp.dp.toPx() }) }
    val screenHeightPx = measuredHeightPx

    var isInteractingWithSheet by remember { mutableStateOf(false) }
    val isFullyExpanded by remember { derivedStateOf { hasEnteredRest && !isInteractingWithSheet && dragOffset.value <= 0.5f } }
    val sheetCornerRadius by animateDpAsState(
        targetValue = if (isFullyExpanded) 0.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "sheetCornerRadiusM3"
    )

    val dismissWithAnimation: () -> Unit = {
        dragScope.launch {
            val remaining = (screenHeightPx - dragOffset.value).coerceAtLeast(0f)
            val duration = (remaining / screenHeightPx * 220).toLong().coerceIn(120L, 220L)
            dragOffset.animateTo(targetValue = screenHeightPx, animationSpec = tween(durationMillis = duration.toInt(), easing = FastOutLinearInEasing))
            onDismiss()
        }
    }

    BackHandler(enabled = !showQueue && !showLyrics) { dismissWithAnimation() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { measuredHeightPx = it.height.toFloat() }
            .graphicsLayer { translationY = dragOffset.value.coerceAtLeast(0f) }
            .clip(RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    val next = (dragOffset.value + delta).coerceAtLeast(0f)
                    dragScope.launch { dragOffset.snapTo(next) }
                },
                onDragStarted = { isInteractingWithSheet = true },
                onDragStopped = { velocity ->
                    isInteractingWithSheet = false
                    if (dragOffset.value > dismissThresholdPx || velocity > 2000f) dismissWithAnimation()
                    else dragScope.launch { dragOffset.animateTo(0f, animationSpec = enterSpring) }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Drag handle
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
            }

            // Fixed: top-left says Now Playing instead of song name
            androidx.compose.material3.TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = strings.nowPlaying,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track != null) {
                            Text(
                                text = "${track.title} • ${track.artist}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { dismissWithAnimation() }) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Album art card - 16dp radius, margin 40dp like Twelve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track?.albumArtUri != null) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }

            // Labels - margin 40dp bottom 20dp like Twelve
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track?.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.album ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Progress slider
            M3NowPlayingProgressLineage(playerViewModel = playerViewModel, isPlaying = state.isPlaying)

            // Fixed: prev/next have squircle background smaller like pause
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.toggleShuffle() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Shuffle, strings.shuffle, tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                androidx.compose.material3.FilledTonalIconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.SkipPrevious, strings.previous, modifier = Modifier.size(28.dp))
                }
                androidx.compose.material3.FilledIconButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) strings.pause else strings.play,
                        modifier = Modifier.size(36.dp)
                    )
                }
                androidx.compose.material3.FilledTonalIconButton(
                    onClick = { playerViewModel.skipToNext() },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.SkipNext, strings.next, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { playerViewModel.cycleRepeatMode() }, modifier = Modifier.size(48.dp)) {
                    val icon = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    Icon(icon, strings.repeat, tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }

            // Bottom bar card - fixed colors and removed non-working buttons #3
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showQueue = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, strings.queue, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    IconButton(onClick = { showLyrics = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Lyrics, strings.lyrics, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    val trackId = track?.id ?: -1L
                    val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)
                    var showAddToPlaylist by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { if (trackId > 0) playerViewModel.toggleLike(trackId) }
                            .combinedClickable(
                                onClick = { if (trackId > 0) playerViewModel.toggleLike(trackId) },
                                onLongClick = { showAddToPlaylist = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            strings.likedSongs,
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (showAddToPlaylist && track != null) {
                        M3AddToPlaylistDrawer(
                            trackId = trackId,
                            track = track,
                            playerViewModel = playerViewModel,
                            onDismiss = { showAddToPlaylist = false },
                            strings = strings
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showQueue) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val queue = state.queue
        val currentStartIndex = remember(queue, state.currentTrack?.id) {
            queue.indexOfFirst { it.id == state.currentTrack?.id }.coerceAtLeast(0)
        }
        val items = remember { mutableStateListOf<dev.shephard.player.data.AudioTrack>() }
        LaunchedEffect(queue, currentStartIndex) {
            val sliced = queue.drop(currentStartIndex)
            if (items.map { it.id } != sliced.map { it.id }) {
                items.clear(); items.addAll(sliced)
            }
        }
        val listState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int,Int>?>(null) }
        val reorderableState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
            val cur = dragInfo
            dragInfo = if (cur == null) from.index to to.index else cur.first to to.index
            items.add(to.index, items.removeAt(from.index))
        }
        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from,to) ->
                    dragInfo=null
                    if (from!=to) {
                        val f = from+currentStartIndex; val t = to+currentStartIndex
                        if (f!=t) playerViewModel.moveQueueItem(f,t)
                    }
                }
            }
        }
        M3BottomSheetWrapper(
            onDismissRequest = { showQueue = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
            Text(strings.queue, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(items, key = { _, t -> t.id }) { idx, queueTrack ->
                    ReorderableItem(state = reorderableState, key = queueTrack.id) { isDragging ->
                        val isCurrent = queueTrack.id == state.currentTrack?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(12.dp))
                                .zIndex(if (isDragging) 1f else 0f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    val index = queue.indexOfFirst { it.id == queueTrack.id }
                                    if (index>=0) playerViewModel.playQueueItem(index)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                if (queueTrack.albumArtUri != null) {
                                    AsyncImage(model = queueTrack.albumArtUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(queueTrack.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(queueTrack.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            // trash then drag handle per spec
                            IconButton(onClick = {
                                val index = queue.indexOfFirst { it.id == queueTrack.id }
                                if (index>=0) playerViewModel.removeFromQueue(index)
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Delete, strings.removeFromQueue, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                            if (!isCurrent) {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = strings.reorder,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp).draggableHandle()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLyrics) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val progress by playerViewModel.progress.collectAsState()
        val syncedLyrics = state.syncedLyrics
        val allLyrics = state.lyrics
        val activeIndex = if (syncedLyrics.isNotEmpty()) syncedLyrics.indexOfLast { it.timeMs <= progress.positionMs }.coerceAtLeast(0) else -1
        val context = LocalContext.current
        val lyricsScope = rememberCoroutineScope()
        var lyricsEditMode by remember { mutableStateOf(false) }
        var editedLyricsText by remember(allLyrics, syncedLyrics) {
            mutableStateOf(
                if (syncedLyrics.isNotEmpty()) syncedLyrics.joinToString("\n") { "[${"%02d".format(it.timeMs/60000)}:${"%02d".format((it.timeMs%60000)/1000)}.${"%02d".format((it.timeMs%1000)/10)}]${it.text}" }
                else allLyrics.joinToString("\n")
            )
        }
        var isFetchingLyrics by remember { mutableStateOf(false) }
        fun applyLyricsText(raw: String) {
            // parseLrcPublic sets syncedLyrics as side effect and returns plain lines
            val lines = playerViewModel.parseLrcPublic(raw)
            if (lines.isNotEmpty()) {
                playerViewModel.setManualLyrics(lines)
            } else {
                val plainLines = raw.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
                if (plainLines.isNotEmpty()) playerViewModel.setManualLyrics(plainLines)
            }
        }
        val lrcLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                lyricsScope.launch(Dispatchers.IO) {
                    try {
                        val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@launch
                        withContext(Dispatchers.Main) {
                            editedLyricsText = text
                            lyricsEditMode = true
                        }
                    } catch(_:Exception){}
                }
            }
        }
        fun httpGet(urlStr: String, timeoutMs: Int = 8000): String? = try {
            val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "LambdaPlayer/2.5")
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
        } catch (_: Exception) { null }

        suspend fun fetchLyrics(): String? = withContext(Dispatchers.IO) {
            val t = track ?: return@withContext null
            val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
            try {
                var body = httpGet("https://lrclib.net/api/get?artist_name=${enc(t.artist)}&track_name=${enc(t.title)}")
                if (body != null) {
                    val obj = runCatching { org.json.JSONObject(body) }.getOrNull()
                    if (obj != null) {
                        val synced = obj.optString("syncedLyrics")
                        if (synced.isNotBlank()) return@withContext synced
                        val plain = obj.optString("plainLyrics")
                        if (plain.isNotBlank()) return@withContext plain
                    }
                }
                body = httpGet("https://api.lyrics.ovh/v1/${enc(t.artist)}/${enc(t.title)}")
                if (body != null) {
                    val obj = runCatching { org.json.JSONObject(body) }.getOrNull()
                    val lyrics = obj?.optString("lyrics")
                    if (!lyrics.isNullOrBlank()) return@withContext lyrics
                }
            } catch(_:Exception){}
            null
        }

        M3BottomSheetWrapper(
            onDismissRequest = { showLyrics = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.lyrics, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { lrcLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Import", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        if (!lyricsEditMode) {
                            editedLyricsText = if (syncedLyrics.isNotEmpty()) syncedLyrics.joinToString("\n"){ it.text } else allLyrics.joinToString("\n")
                        }
                        lyricsEditMode = !lyricsEditMode
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = if (lyricsEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        if (!isFetchingLyrics) {
                            isFetchingLyrics = true
                            lyricsScope.launch {
                                val fetched = fetchLyrics()
                                isFetchingLyrics = false
                                if (!fetched.isNullOrBlank()) {
                                    applyLyricsText(fetched)
                                    editedLyricsText = fetched
                                }
                            }
                        }
                    }) {
                        if (isFetchingLyrics) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Download")
                    }
                }
            }
            if (lyricsEditMode) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = editedLyricsText,
                        onValueChange = { editedLyricsText = it },
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        label = { Text(strings.lyrics) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { lyricsEditMode = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            applyLyricsText(editedLyricsText)
                            lyricsEditMode = false
                        }) { Text("Save") }
                    }
                }
            } else {
                val hasLyrics = allLyrics.isNotEmpty() || syncedLyrics.isNotEmpty()
                if (!hasLyrics) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.noLyricsFound, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            isFetchingLyrics = true
                            lyricsScope.launch {
                                val fetched = fetchLyrics()
                                isFetchingLyrics = false
                                if (!fetched.isNullOrBlank()) applyLyricsText(fetched)
                            }
                        }) { Text("Download lyrics") }
                    }
                } else {
                    // Show synced if available else plain lyrics, with click to seek
                    if (syncedLyrics.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                            items(syncedLyrics) { line ->
                                val index = syncedLyrics.indexOf(line)
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (index == activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { playerViewModel.seekTo(line.timeMs) }
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                            items(allLyrics) { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3NowPlayingProgressLineage(
    playerViewModel: PlayerViewModel,
    isPlaying: Boolean,
) {
    val progress by playerViewModel.progress.collectAsState()
    val durationMs = progress.durationMs
    val baseFraction = rememberSmoothFractionLineage(positionMs = progress.positionMs, durationMs = durationMs, isPlaying = isPlaying)

    var seekFraction by remember { mutableStateOf<Float?>(null) }
    androidx.compose.runtime.LaunchedEffect(baseFraction, durationMs) {
        val held = seekFraction ?: return@LaunchedEffect
        if (kotlin.math.abs(held - baseFraction) < 0.03f || kotlin.math.abs(held - baseFraction) > 0.1f) {
            seekFraction = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        androidx.compose.material3.Slider(
            value = seekFraction ?: baseFraction,
            onValueChange = { fraction ->
                seekFraction = fraction
                if (durationMs > 0L) playerViewModel.onSeekPreview((fraction * durationMs).toLong())
            },
            onValueChangeFinished = {
                val fraction = seekFraction ?: baseFraction
                if (durationMs > 0L) playerViewModel.onSeekCommit((fraction * durationMs).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = m3FormatMillisLineage(if (seekFraction != null) (seekFraction!! * durationMs).toLong() else progress.positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = m3FormatMillisLineage(durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun rememberSmoothFractionLineage(positionMs: Long, durationMs: Long, isPlaying: Boolean): Float {
    var smooth by remember { mutableFloatStateOf(0f) }
    var lastSamplePosMs by remember { mutableLongStateOf(0L) }
    var lastSampleAtMs by remember { mutableLongStateOf(0L) }
    val durMs = durationMs.coerceAtLeast(1L)

    LaunchedEffect(positionMs, durationMs, isPlaying) {
        val pos = positionMs.coerceAtLeast(0L)
        val now = android.os.SystemClock.elapsedRealtime()
        lastSamplePosMs = pos
        lastSampleAtMs = now
        smooth = (pos.toFloat() / durMs).coerceIn(0f, 1f)
        while (true) {
            withFrameNanos { }
            val elapsed = (android.os.SystemClock.elapsedRealtime() - lastSampleAtMs).coerceAtLeast(0L)
            val displayedPos = if (isPlaying) (lastSamplePosMs + elapsed).coerceAtMost(pos + 500L) else lastSamplePosMs
            smooth = (displayedPos.toFloat() / durMs).coerceIn(0f, 1f)
        }
    }
    return smooth
}

private fun m3FormatMillisLineage(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3AddToPlaylistDrawer(
    trackId: Long,
    track: dev.shephard.player.data.AudioTrack?,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val json by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(json) { dev.shephard.player.ui.screens.parsePlaylists(json) }
    val likedJson by prefs.likedSongIds.collectAsState(initial = "[]")
    val likedIds = remember(likedJson) {
        try { org.json.JSONArray(likedJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } } }
        catch (_: Exception) { emptyList() }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    M3BottomSheetWrapper(
            onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(strings.addToPlaylist, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(playlists.filterNot { it.isSystem }) { pl ->
                    val contains = trackId in pl.trackIds
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(pl.name) },
                        supportingContent = { Text("${pl.trackIds.size} tracks") },
                        trailingContent = {
                            if (contains) Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val all = playlists.toMutableList()
                                val idx = all.indexOf(pl)
                                if (idx >= 0) {
                                    val current = all[idx]
                                    val newIds = if (contains) current.trackIds - trackId else current.trackIds + trackId
                                    all[idx] = current.copy(trackIds = newIds)
                                    prefs.setPlaylistsJson(dev.shephard.player.ui.screens.encodePlaylists(all))
                                }
                            }
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
