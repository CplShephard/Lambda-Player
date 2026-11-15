// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.CarouselStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.abs

private const val SETTLED_PAGE_OFFSET_EPSILON = 0.001f

/** Pixel's pager, own-scroll accounting and parallax layout, adapted to Lambda queue and art. */
@Composable
fun AlbumCarousel(
    queue: List<AudioTrack>,
    currentIndex: Int,
    requestedIndex: Int?,
    isPlaying: Boolean,
    carouselStyle: String,
    onSongSelected: (Int) -> Unit,
    onAlbumClick: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier,
    placeholderColor: Color = Color.DarkGray,
    placeholderContentColor: Color = Color.White,
) {
    if (queue.isEmpty()) return
    val actualIndex = currentIndex.coerceIn(queue.indices)
    val latestOnSongSelected by rememberUpdatedState(onSongSelected)
    val pagerState = rememberPagerState(initialPage = actualIndex) { queue.size }
    val targetIndex = requestedIndex?.takeIf { it in queue.indices } ?: actualIndex
    val requestedTargetIndex = requestedIndex?.takeIf { it in queue.indices }
    // The original player kept the cover bound to the current track, not its queue position.
    // A key containing the index destroys/recreates that cover when remix moves the item.
    // Give duplicates distinct keys, reserving a stable key for the currently playing copy.
    val (queueKeys, duplicateIds) = remember(queue) {
        val counts = queue.groupingBy { it.id }.eachCount()
        val occurrences = HashMap<Long, Int>()
        val keys = queue.map { track ->
            val occurrence = occurrences.getOrDefault(track.id, 0)
            occurrences[track.id] = occurrence + 1
            "queue_carousel_${track.id}_$occurrence"
        }
        keys to counts.filterValues { it > 1 }.keys
    }
    val context = LocalContext.current
    DisposableEffect(context, queue, targetIndex) {
        val requests = listOf(targetIndex - 1, targetIndex + 1).mapNotNull { index ->
            val uri = queue.getOrNull(index)?.albumArtUri ?: return@mapNotNull null
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(uri).size(512).build())
        }
        onDispose { requests.forEach { it.dispose() } }
    }

    var ownScrollsInFlight by remember { mutableIntStateOf(0) }
    var lastSettledSongId by remember { mutableStateOf(queue.getOrNull(actualIndex)?.id) }
    var isUserDragging by remember { mutableStateOf(false) }
    var userScrollSettlePending by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> { isUserDragging = true; userScrollSettlePending = true }
                is DragInteraction.Stop, is DragInteraction.Cancel -> isUserDragging = false
            }
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress && ownScrollsInFlight == 0 }
            .filter { it }.collect { userScrollSettlePending = true }
    }
    LaunchedEffect(targetIndex, requestedTargetIndex, queue) {
        if (isUserDragging) snapshotFlow { isUserDragging }.first { !it }
        val betweenPages = abs(pagerState.currentPageOffsetFraction) > SETTLED_PAGE_OFFSET_EPSILON
        if (pagerState.currentPage != targetIndex || betweenPages) {
            val currentId = queue.getOrNull(actualIndex)?.id
            val shiftOnly = currentId != null && currentId == lastSettledSongId && requestedTargetIndex == null
            ownScrollsInFlight++
            try {
                if (shiftOnly) pagerState.scrollToPage(targetIndex)
                else pagerState.animateScrollToPage(
                    targetIndex,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            } finally { ownScrollsInFlight-- }
        }
        lastSettledSongId = queue.getOrNull(actualIndex)?.id
    }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pagerState, actualIndex, queue) {
        snapshotFlow { pagerState.isScrollInProgress }.distinctUntilChanged().filter { !it }.collect {
            if (!userScrollSettlePending) return@collect
            userScrollSettlePending = false
            val settled = pagerState.currentPage
            if (settled != actualIndex && settled in queue.indices) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                latestOnSongSelected(settled)
            }
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else .95f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "albumCarouselPausedScale",
    )
    BoxWithConstraints(modifier = modifier.graphicsLayer { scaleX = artScale; scaleY = artScale }) {
        ParallaxAlbumPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            itemSpacing = 8.dp,
            itemCornerRadius = 24.dp,
            carouselStyle = if (pagerState.pageCount == 1) CarouselStyle.NO_PEEK else carouselStyle,
            carouselWidth = maxWidth,
            itemKey = { index ->
                val track = queue.getOrNull(index)
                if (index == actualIndex && track != null && track.id in duplicateIds)
                    "queue_carousel_${track.id}_playing"
                else queueKeys.getOrNull(index) ?: "queue_item_$index"
            },
        ) { index ->
            val track = queue[index]
            val isFocused = pagerState.currentPage == index
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp))
                    .background(placeholderColor)
                    .clickable(
                        enabled = isFocused && ((track.albumId ?: 0L) > 0L || track.album.isNotBlank()),
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = { onAlbumClick(track) },
                    ), contentAlignment = Alignment.Center) {
                    var loaded by remember(track.albumArtUri) { mutableStateOf(false) }
                    if (!loaded) Icon(Icons.Filled.MusicNote, contentDescription = null, tint = placeholderContentColor, modifier = Modifier.size(64.dp))
                    AsyncImage(
                        model = if (isFocused) track.albumArtUri else rememberStaticArtwork(track.albumArtUri),
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        onSuccess = { loaded = true }, onError = { loaded = false },
                    )
                    // Only the focused page plays motion covers, always through the silent player.
                    if (isFocused) SilentVideoCover(track.albumArtUri, Modifier.matchParentSize())
                }
            }
        }
    }
}
