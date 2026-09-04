// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.screens.m3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.m3.M3WavySlider
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
    val enterSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 180f
        )
    }

    LaunchedEffect(Unit) {
        hasEnteredRest = false
        dragOffset.animateTo(0f, animationSpec = enterSpring)
        hasEnteredRest = true
    }

    var measuredHeightPx by remember { mutableFloatStateOf(with(density) { configuration.screenHeightDp.dp.toPx() }) }
    val screenHeightPx = measuredHeightPx

    var isInteractingWithSheet by remember { mutableStateOf(false) }
    val isFullyExpanded by remember {
        derivedStateOf { hasEnteredRest && !isInteractingWithSheet && dragOffset.value <= 0.5f }
    }
    val sheetCornerRadius by animateDpAsState(
        targetValue = if (isFullyExpanded) 0.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sheetCornerRadiusM3"
    )

    val dismissWithAnimation: () -> Unit = {
        dragScope.launch {
            val remaining = (screenHeightPx - dragOffset.value).coerceAtLeast(0f)
            val duration = (remaining / screenHeightPx * 220).toLong().coerceIn(120L, 220L)
            dragOffset.animateTo(
                targetValue = screenHeightPx,
                animationSpec = tween(
                    durationMillis = duration.toInt(),
                    easing = FastOutLinearInEasing
                )
            )
            onDismiss()
        }
    }

    BackHandler(enabled = !showQueue && !showLyrics) {
        dismissWithAnimation()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { measuredHeightPx = it.height.toFloat() }
            .graphicsLayer { translationY = dragOffset.value.coerceAtLeast(0f) }
            .clip(RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    val next = (dragOffset.value + delta).coerceAtLeast(0f)
                    dragScope.launch { dragOffset.snapTo(next) }
                },
                onDragStarted = { isInteractingWithSheet = true },
                onDragStopped = { velocity ->
                    isInteractingWithSheet = false
                    if (dragOffset.value > dismissThresholdPx || velocity > 2000f) {
                        dismissWithAnimation()
                    } else {
                        dragScope.launch {
                            dragOffset.animateTo(0f, animationSpec = enterSpring)
                        }
                    }
                }
            )
    ) {
        if (track?.albumArtUri != null) {
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(1.35f) }
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
                    .blur(60.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Drag indicator handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }

            // Header info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.nowPlaying,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (state.currentPlaylistName != null) {
                        Text(
                            text = state.currentPlaylistName.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (track?.albumArtUri != null) {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = track?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            M3NowPlayingProgress(
                playerViewModel = playerViewModel,
                isPlaying = state.isPlaying,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = strings.shuffle,
                        tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = strings.previous,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                FilledIconButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
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
                IconButton(onClick = { playerViewModel.skipToNext() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = strings.next,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                    val icon = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    val tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                    Icon(
                        imageVector = icon,
                        contentDescription = strings.repeat,
                        tint = tint
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 28.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = strings.queue,
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { showLyrics = true }) {
                    Icon(
                        imageVector = Icons.Filled.Lyrics,
                        contentDescription = strings.lyrics,
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                val trackId = track?.id ?: -1L
                val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)
                IconButton(onClick = { if (trackId > 0) playerViewModel.toggleLike(trackId) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = strings.likedSongs,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    if (showQueue) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = strings.queue,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(state.queue) { queueTrack ->
                    val isCurrent = queueTrack.id == state.currentTrack?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val index = state.queue.indexOfFirst { it.id == queueTrack.id }
                                if (index >= 0) playerViewModel.playQueueItem(index)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = queueTrack.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = queueTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val index = state.queue.indexOfFirst { it.id == queueTrack.id }
                                if (index >= 0) playerViewModel.removeFromQueue(index)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = strings.removeFromQueue,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
        val activeIndex = if (syncedLyrics.isNotEmpty()) {
            syncedLyrics.indexOfLast { it.timeMs <= progress.positionMs }.coerceAtLeast(0)
        } else {
            -1
        }
        ModalBottomSheet(
            onDismissRequest = { showLyrics = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = strings.lyrics,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            if (syncedLyrics.isEmpty()) {
                Text(
                    text = strings.noLyricsFound,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    items(syncedLyrics) { line ->
                        val index = syncedLyrics.indexOf(line)
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (index == activeIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * PixelPlayer-style progress: Material 3 expressive wavy slider + time labels.
 * While the user drags, the thumb stays where it was dropped until playback
 * catches up on its own.
 *
 * The position stream from the player updates every 500 ms, which makes the
 * wavy bar look frozen between ticks. We extrapolate between samples at frame
 * rate (PixelPlayer's rememberSmoothProgress feeds the same idea) so the wave
 * and thumb glide continuously.
 */
@Composable
private fun M3NowPlayingProgress(
    playerViewModel: PlayerViewModel,
    isPlaying: Boolean,
) {
    val progress by playerViewModel.progress.collectAsState()
    val durationMs = progress.durationMs
    val baseFraction = rememberSmoothFraction(
        positionMs = progress.positionMs,
        durationMs = durationMs,
        isPlaying = isPlaying,
    )

    var seekFraction by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(baseFraction, durationMs) {
        val held = seekFraction ?: return@LaunchedEffect
        if (kotlin.math.abs(held - baseFraction) < 0.03f || kotlin.math.abs(held - baseFraction) > 0.1f) {
            seekFraction = null
        }
    }

    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.25f)
    val thumbColor = Color.White
    val timeColor = Color.White.copy(alpha = 0.7f)

    Column(modifier = Modifier.fillMaxWidth()) {
        M3WavySlider(
            value = { seekFraction ?: baseFraction },
            onValueChange = { fraction ->
                seekFraction = fraction
                if (durationMs > 0L) {
                    playerViewModel.onSeekPreview((fraction * durationMs).toLong())
                }
            },
            onValueCommit = { fraction ->
                if (durationMs > 0L) {
                    playerViewModel.onSeekCommit((fraction * durationMs).toLong())
                }
            },
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor,
            thumbColor = thumbColor,
            isPlaying = isPlaying,
            isVisible = true,
            trackEdgePadding = 8.dp,
            semanticsLabel = LocalStrings.current.playbackPosition,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = m3FormatMillis(
                    if (seekFraction != null) (seekFraction!! * durationMs).toLong() else progress.positionMs
                ),
                style = MaterialTheme.typography.labelMedium,
                color = timeColor
            )
            Text(
                text = m3FormatMillis(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = timeColor
            )
        }
    }
}

/**
 * Frame-clock smoothed playback fraction. The player emits a new position every
 * ~500 ms; while playing we extrapolate from the last sample with the elapsed
 * time (capped at one extra tick so a stalled stream cannot run ahead) which
 * produces a genuinely continuous 60 fps progress for the wavy bar.
 */
@Composable
private fun rememberSmoothFraction(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
): Float {
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
            val displayedPos = if (isPlaying) {
                (lastSamplePosMs + elapsed).coerceAtMost(pos + 500L)
            } else {
                lastSamplePosMs
            }
            smooth = (displayedPos.toFloat() / durMs).coerceIn(0f, 1f)
        }
    }
    return smooth
}

private fun m3FormatMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
