@file:OptIn(ExperimentalFoundationApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.rememberBounceOverscrollEffect
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.MinimalSeekBar
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val track = state.currentTrack
    val strings = LocalStrings.current

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    // Animatable drag offset so a partial pull-down springs smoothly back
    // to the top instead of snapping (fixes the "buggy" feel).
    val dragOffset = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()

    // Albüm kapağında yatay kaydırma: sağa → önceki, sola → sonraki.
    // Kapak parmakla birlikte kayar, bırakınca yumuşakça yerine döner.
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    val artSwipeX = remember { Animatable(0f) }
    var artSwipeHandled by remember { mutableStateOf(false) }

    // Şarkı geçiş yönü takibi
    var lastTrackId by remember { mutableStateOf<Long?>(null) }
    var slideForward by remember { mutableStateOf(true) }
    val currentId = track?.id ?: -1L
    if (lastTrackId != null && lastTrackId != currentId) {
        slideForward = currentId > (lastTrackId ?: 0L)
    }
    if (lastTrackId != currentId) lastTrackId = currentId

    LaunchedEffect(track?.id) {
        artSwipeX.snapTo(0f)
        artSwipeHandled = false
    }

    val glow = Color(state.glowColorArgb)
    val pulse by animateFloatAsState(
        targetValue = 0.8f + state.amplitude * 0.6f,
        animationSpec = tween(120),
        label = "glowPulse"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (track == null) 0f else 0.6f + state.amplitude * 0.35f,
        animationSpec = tween(220),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer { translationY = dragOffset.value.coerceAtLeast(0f) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // Only allow dragging downward; clamp at the top edge.
                    val next = (dragOffset.value + delta).coerceAtLeast(0f)
                    dragScope.launch { dragOffset.snapTo(next) }
                },
                onDragStopped = { velocity ->
                    if (dragOffset.value > dismissThresholdPx || velocity > 2500f) {
                        onDismiss()
                        // Reset for next time after the close animation runs.
                        dragScope.launch { dragOffset.snapTo(0f) }
                    } else {
                        // Smoothly glide back up to the top.
                        dragScope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 320f
                                )
                            )
                        }
                    }
                }
            )
    ) {
        // ----- Ambient glow background -----
        // Two stacked layers so the color reads as a vertical wash that
        // covers the top ~70% of the screen and fades smoothly into pure
        // black at the bottom 30%:
        //   1) A vertical gradient wash (broad, screen-wide).
        //   2) A radial highlight centered roughly where the cover sits,
        //      which gives the glow a focal point.
        // Both layers share the same dynamic glow color (driven by the
        // dominant color extracted from the album art) and pulse together
        // with the playback amplitude.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowAlpha }
                .blur(110.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            // 0%   – top, brightest
                            0.00f to glow.copy(alpha = 0.95f),
                            // 35%  – upper-mid, still vivid
                            0.35f to glow.copy(alpha = 0.65f),
                            // 65%  – start of fade
                            0.65f to glow.copy(alpha = 0.18f),
                            // 75%  – almost gone
                            0.75f to glow.copy(alpha = 0.04f),
                            // 80%+ – pure black
                            0.80f to Color.Transparent,
                            1.00f to Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowAlpha }
                .blur(150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glow.copy(alpha = 0.65f),
                            glow.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 320f),
                        radius = 680f * pulse
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .pointerInput(track?.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            artSwipeHandled = false
                        },
                        onDragEnd = {
                            dragScope.launch {
                                artSwipeX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = 400f
                                    )
                                )
                            }
                            artSwipeHandled = false
                        },
                        onDragCancel = {
                            dragScope.launch {
                                artSwipeX.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.8f, stiffness = 400f)
                                )
                            }
                            artSwipeHandled = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        dragScope.launch {
                            val next = artSwipeX.value + dragAmount * 0.6f
                            artSwipeX.snapTo(next)
                        }
                        if (!artSwipeHandled) {
                            if (artSwipeX.value <= -swipeThresholdPx) {
                                playerViewModel.skipToNext()
                                artSwipeHandled = true
                            } else if (artSwipeX.value >= swipeThresholdPx) {
                                playerViewModel.skipToPrevious()
                                artSwipeHandled = true
                            }
                        }
                    }
                }
        ) {
            // Top row: collapse sol + nowplaying orta + boş sağ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ -> }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BouncyIconButton(
                    onClick = onDismiss,
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = strings.cancel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Playlist Context Header (if playing from a playlist)
                    if (state.currentPlaylistName != null) {
                        Text(
                            text = state.currentPlaylistName!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = strings.nowPlaying,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Sağ üst boş placeholder (Queue sağ alta taşındı)
                Box(modifier = Modifier.size(48.dp))
            }

            // Albüm kapağı + başlık — geçiş animasyonlu
            AnimatedContent(
                targetState = track?.id ?: -1L,
                transitionSpec = {
                    val dir = if (slideForward)
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right
                    (slideIntoContainer(dir, tween(380)) + fadeIn() + scaleIn(initialScale = 0.92f))
                        .togetherWith(slideOutOfContainer(dir, tween(380)) + fadeOut() + scaleOut(targetScale = 0.92f))
                },
                label = "trackSwap",
                modifier = Modifier.fillMaxWidth()
            ) { _ ->
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .aspectRatio(1f)
                            // Kapak parmakla birlikte kaysın + uçlara doğru hafif soluklaşsın.
                            .graphicsLayer {
                                translationX = artSwipeX.value
                                alpha = 1f - (kotlin.math.abs(artSwipeX.value) / (size.width.coerceAtLeast(1f)))
                                    .coerceIn(0f, 0.35f)
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        var artLoaded by remember(track?.id) { mutableStateOf(false) }
                        AsyncImage(
                            model = track?.albumArtUri,
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
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = track?.title ?: strings.nothingPlaying,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = track?.artist ?: strings.pickASong,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Real-time Floating Lyrics (compact typography between title & progress)
                        if (!state.currentLyric.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.currentLyric!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Spacer pushes the seek bar + controls toward the bottom of the
            // screen, leaving a clean black area at the bottom (~30% of the
            // screen height on typical phones). Combined with the vertical
            // gradient glow that fades to transparent at ~80%, this gives
            // the layout the "top 70% colored / bottom 30% pure black" feel.
            Spacer(modifier = Modifier.weight(1f))

            // Seek bar
            Column(modifier = Modifier.fillMaxWidth()) {
                MinimalSeekBar(
                    progress = if (state.durationMs > 0)
                        state.positionMs.toFloat() / state.durationMs.toFloat() else 0f,
                    onSeekPreview = { fraction ->
                        playerViewModel.onSeekPreview((fraction * state.durationMs).toLong())
                    },
                    onSeekFinished = { fraction ->
                        playerViewModel.onSeekCommit((fraction * state.durationMs).toLong())
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatMillis(state.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatMillis(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Queue + Lyrics + Add buttons — aligned row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dedicated Lyrics toggle button
                BouncyIconButton(
                    onClick = { /* TODO: open lyrics sheet or toggle */ },
                    icon = Icons.Filled.QueueMusic, // placeholder icon; can be replaced with a lyrics icon
                    contentDescription = "Lyrics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )

                Spacer(Modifier.width(8.dp))

                // Smart "+" button (placeholder for later batch)
                BouncyIconButton(
                    onClick = { /* handled in batch 3 */ },
                    icon = Icons.Filled.Add,
                    contentDescription = "Add to playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )

                Spacer(Modifier.width(8.dp))

                var showQueue by remember { mutableStateOf(false) }
                BouncyIconButton(
                    onClick = { showQueue = true },
                    icon = Icons.Filled.QueueMusic,
                    contentDescription = strings.queue,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
                if (showQueue) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showQueue = false },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        QueueList(
                            queue = state.queue,
                            currentTrackId = state.currentTrack?.id,
                            onMove = { from, to -> playerViewModel.moveQueueItem(from, to) }
                        )
                    }
                }
            }

            // Kontroller — alt kenara daha yakın (Spacer(weight=1f) ile aşağı itildi).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 32.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ -> }
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BouncyIconButton(
                    onClick = { playerViewModel.toggleShuffle() },
                    icon = Icons.Filled.Shuffle,
                    contentDescription = strings.shuffle,
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
                BouncyIconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = strings.previous,
                    tint = MaterialTheme.colorScheme.onBackground,
                    iconSize = 36.dp
                )
                // Bouncing scale + rotating Play/Pause icon
                var playButtonScale by remember { mutableStateOf(1f) }
                val animatedScale by animateFloatAsState(
                    targetValue = playButtonScale,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
                    label = "playButtonBounce"
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick {
                            playButtonScale = 0.75f
                            playerViewModel.togglePlayPause()
                            // reset scale shortly after
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(80)
                                playButtonScale = 1f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.6f))
                                .togetherWith(fadeOut(tween(120)))
                        },
                        label = "playPauseIcon"
                    ) { isPlaying ->
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) strings.pause else strings.play,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                BouncyIconButton(
                    onClick = { playerViewModel.skipToNext() },
                    icon = Icons.Filled.SkipNext,
                    contentDescription = strings.next,
                    tint = MaterialTheme.colorScheme.onBackground,
                    iconSize = 36.dp
                )
                BouncyIconButton(
                    onClick = { playerViewModel.cycleRepeatMode() },
                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = strings.repeat,
                    tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
            }
        }


    }
}

@Composable
private fun QueueList(
    queue: List<AudioTrack>,
    currentTrackId: Long?,
    onMove: (from: Int, to: Int) -> Unit
) {
    val strings = LocalStrings.current
    val density = LocalDensity.current

    // Only show current track + upcoming (Spotify-style: past tracks are hidden).
    val currentStartIndex = remember(queue, currentTrackId) {
        queue.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
    }

    // Local mirror of the visible slice so reordering feels instant while dragging.
    val items = remember { mutableStateListOf<AudioTrack>() }
    LaunchedEffect(queue, currentStartIndex) {
        items.clear()
        items.addAll(queue.drop(currentStartIndex))
    }

    val itemHeightDp = 64.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // Index currently being dragged, and the accumulated vertical offset.
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = strings.queue,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .overscroll(rememberBounceOverscrollEffect())
        ) {
            itemsIndexed(items, key = { _, t -> t.id }) { index, track ->
                val isDragged = index == draggedIndex
                val isPlaying = track.id == currentTrackId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        // Lift the dragged row visually and let it float above siblings.
                        .graphicsLayer {
                            if (isDragged) {
                                translationY = dragOffsetY
                                shadowElevation = 12f
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                        }
                        .zIndex(if (isDragged) 1f else 0f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isDragged -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            track.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Up / Down + drag handle — hidden for the currently playing track (index 0).
                    if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(enabled = index > 1) {
                                if (index > 1) onMove(index + currentStartIndex, index + currentStartIndex - 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = if (index > 1) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(enabled = index < items.size - 1) {
                                if (index < items.size - 1) onMove(index + currentStartIndex, index + currentStartIndex + 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = if (index < items.size - 1) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Drag handle — live reordering as you drag.
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .pointerInput(track.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        // Persist the final order to the player.
                                        // items is a slice starting at currentStartIndex, so offset back.
                                        val from = queue.indexOfFirst { it.id == track.id }
                                        val toSlice = items.indexOfFirst { it.id == track.id }
                                        val to = if (toSlice >= 0) toSlice + currentStartIndex else -1
                                        if (from >= 0 && to >= 0 && from != to) {
                                            onMove(from, to)
                                        }
                                        draggedIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        dragOffsetY = 0f
                                    }
                                ) { change, dragAmount: Offset ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val cur = draggedIndex
                                    if (cur < 0) return@detectDragGestures
                                    // Swap with neighbour once dragged past half a row.
                                    // cur > 1: index 0 is currently playing, never swap above it.
                                    if (dragOffsetY > itemHeightPx / 2 && cur < items.size - 1) {
                                        items.add(cur + 1, items.removeAt(cur))
                                        draggedIndex = cur + 1
                                        dragOffsetY -= itemHeightPx
                                    } else if (dragOffsetY < -itemHeightPx / 2 && cur > 1) {
                                        items.add(cur - 1, items.removeAt(cur))
                                        draggedIndex = cur - 1
                                        dragOffsetY += itemHeightPx
                                    }
                                }
                            }
                    )
                    } // end if (!isPlaying)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

internal fun formatMillis(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
