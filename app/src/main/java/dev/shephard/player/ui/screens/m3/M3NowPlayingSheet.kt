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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.m3.LineageListItemWithThumbnail
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

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

            // Toolbar like Twelve's NowPlaying toolbar - centered title, down arrow
            androidx.compose.material3.TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = track?.title ?: strings.nowPlaying,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.currentPlaylistName != null) {
                            Text(
                                text = state.currentPlaylistName.orEmpty(),
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

            // Media controls - 22dp horizontal margin, like Twelve
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.toggleShuffle() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Shuffle, strings.shuffle, tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { playerViewModel.skipToPrevious() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipPrevious, strings.previous, modifier = Modifier.size(24.dp))
                }
                // Primary 72dp like Twelve
                androidx.compose.material3.FilledIconButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) strings.pause else strings.play,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { playerViewModel.skipToNext() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipNext, strings.next, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { playerViewModel.cycleRepeatMode() }, modifier = Modifier.size(48.dp)) {
                    val icon = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    Icon(icon, strings.repeat, tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Bottom bar card - secondaryContainer, 12dp radius, tonal buttons
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { showQueue = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, strings.queue)
                    }
                    IconButton(onClick = { showLyrics = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Lyrics, strings.lyrics)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Info, strings.audioInformation)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.GraphicEq, "EQ")
                    }
                    val trackId = track?.id ?: -1L
                    val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)
                    IconButton(onClick = { if (trackId > 0) playerViewModel.toggleLike(trackId) }, modifier = Modifier.size(48.dp)) {
                        Icon(if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, strings.likedSongs, tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showQueue) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showQueue = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
            Text(strings.queue, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                items(state.queue) { queueTrack ->
                    val isCurrent = queueTrack.id == state.currentTrack?.id
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                val index = state.queue.indexOfFirst { it.id == queueTrack.id }
                                if (index >= 0) playerViewModel.playQueueItem(index)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(queueTrack.title, style = MaterialTheme.typography.bodyLarge, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(queueTrack.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = {
                            val index = state.queue.indexOfFirst { it.id == queueTrack.id }
                            if (index >= 0) playerViewModel.removeFromQueue(index)
                        }) {
                            Icon(Icons.Filled.Delete, strings.removeFromQueue, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        val activeIndex = if (syncedLyrics.isNotEmpty()) syncedLyrics.indexOfLast { it.timeMs <= progress.positionMs }.coerceAtLeast(0) else -1
        ModalBottomSheet(onDismissRequest = { showLyrics = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
            Text(strings.lyrics, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            if (syncedLyrics.isEmpty()) {
                Text(strings.noLyricsFound, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    items(syncedLyrics) { line ->
                        val index = syncedLyrics.indexOf(line)
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (index == activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
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
