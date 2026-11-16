@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.activity.ExperimentalActivityApi::class)
// SPDX-License-Identifier: GPL-3.0-only
// LineageOS Twelve 1:1 - Now Playing Screen
package dev.shephard.player.ui.screens.m3



import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import dev.shephard.player.ui.components.M3BottomSheetWrapper
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.min
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dev.shephard.player.player.PlayerUiState
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.QueueRemoveButton
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3NowPlayingSheet(
playerViewModel: PlayerViewModel = viewModel(),
collapsedBounds: androidx.compose.ui.geometry.Rect? = null,
onAlbumClick: (dev.shephard.player.data.AudioTrack) -> Unit = {},
onDismiss: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val baseScheme = MaterialTheme.colorScheme
    val accent by androidx.compose.animation.animateColorAsState(
    targetValue = Color(state.glowColorArgb),
    animationSpec = tween(620),
    label = "nowPlayingCoverAccent",
)
    val tintedSurface = lerp(baseScheme.surface, accent, 0.25f)
    val tintedSurfaceVariant = lerp(baseScheme.surfaceVariant, accent, 0.30f)
    val tintedSecondaryContainer = lerp(baseScheme.secondaryContainer, accent, 0.48f)
    val tintedPrimaryContainer = lerp(baseScheme.primaryContainer, accent, 0.42f)
    val tintedScheme = baseScheme.copy(
    primary = accent,
    onPrimary = contrastColorFor(accent),
    primaryContainer = tintedPrimaryContainer,
    onPrimaryContainer = contrastColorFor(tintedPrimaryContainer),
    secondary = accent,
    onSecondary = contrastColorFor(accent),
    secondaryContainer = tintedSecondaryContainer,
    onSecondaryContainer = contrastColorFor(tintedSecondaryContainer),
    surface = tintedSurface,
    onSurface = contrastColorFor(tintedSurface),
    surfaceVariant = tintedSurfaceVariant,
    onSurfaceVariant = contrastColorFor(tintedSurfaceVariant),
    surfaceContainerLowest = lerp(baseScheme.surfaceContainerLowest, accent, 0.10f),
    surfaceContainerLow = lerp(baseScheme.surfaceContainerLow, accent, 0.18f),
    surfaceContainer = lerp(baseScheme.surfaceContainer, accent, 0.28f),
    surfaceContainerHigh = lerp(baseScheme.surfaceContainerHigh, accent, 0.36f),
    surfaceContainerHighest = lerp(baseScheme.surfaceContainerHighest, accent, 0.42f),
    outline = lerp(baseScheme.outline, accent, 0.28f),
)
    MaterialTheme(colorScheme = tintedScheme) {
        M3NowPlayingSheetContent(
        playerViewModel = playerViewModel,
        state = state,
        collapsedBounds = collapsedBounds,
        onAlbumClick = onAlbumClick,
        onDismiss = onDismiss,
    )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3NowPlayingSheetContent(
playerViewModel: PlayerViewModel,
state: PlayerUiState,
collapsedBounds: androidx.compose.ui.geometry.Rect?,
onAlbumClick: (dev.shephard.player.data.AudioTrack) -> Unit,
onDismiss: () -> Unit,
) {
    val track = state.currentTrack
    val isRemixed by playerViewModel.isRemixed.collectAsState()
    val navigationDirection by playerViewModel.navigationDirection.collectAsState()
    val pendingCarouselIndex by playerViewModel.pendingCarouselIndex.collectAsState()
    val carouselContext = androidx.compose.ui.platform.LocalContext.current
    val carouselPrefs = remember(carouselContext) { dev.shephard.player.player.PreferencesManager(carouselContext) }
    val carouselStyle by carouselPrefs.carouselStyle.collectAsState(initial = dev.shephard.player.player.CarouselStyle.NO_PEEK)
    val strings = LocalStrings.current
    val nowPlayingBackground = MaterialTheme.colorScheme.surface

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

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

    // Same predictive back as the M3 drawers (ModalBottomSheet): the sheet shrinks towards the
    // bottom edge while the gesture runs; releasing slides it down (keeping that scale, exactly
    // like ModalBottomSheet's hide), cancelling springs it back to full size.
    PredictiveBackHandler(enabled = !showQueue && !showLyrics && !showAddToPlaylist) { events ->
        try {
            events.collect { event ->
                dragOffset.snapTo(event.progress.coerceIn(0f, 1f) * screenHeightPx)
            }
            dismissWithAnimation()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            dragScope.launch { dragOffset.animateTo(0f, animationSpec = enterSpring) }
            throw cancelled
        }
    }

    Box(
    modifier = Modifier
    .fillMaxSize()
    .onSizeChanged { measuredHeightPx = it.height.toFloat() }
    .graphicsLayer {
        val p = (dragOffset.value / screenHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        translationY = dragOffset.value.coerceAtLeast(0f)
        alpha = (1f - p * .22f).coerceIn(0f, 1f)
        shape = RoundedCornerShape(sheetCornerRadius)
        clip = true
    }
    .background(nowPlayingBackground)
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
        val isLandscape = dev.shephard.player.ui.util.isLandscapeWindow()
        if (isLandscape) {
            M3LandscapeContent(
            playerViewModel = playerViewModel,
            state = state,
            navigationDirection = navigationDirection,
            pendingCarouselIndex = pendingCarouselIndex,
            carouselStyle = carouselStyle,
            onAlbumClick = onAlbumClick,
            onShowQueueChange = { showQueue = it },
            onShowAddToPlaylistChange = { showAddToPlaylist = it },
            strings = strings,
        )
        } else {
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

                // Minimal, transparent heading: let the cover-derived Now Playing palette show through.
                Text(
                text = strings.nowPlaying,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

                androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
                ) {
                    val pageFraction = when (carouselStyle) {
                        dev.shephard.player.player.CarouselStyle.NO_PEEK -> 1f
                        dev.shephard.player.player.CarouselStyle.TWO_PEEK -> .6f
                        else -> .8f
                    }
                    val artHeight = minOf(maxWidth * pageFraction, configuration.screenHeightDp.dp * .38f)
                    if (state.queue.isNotEmpty()) {
                        dev.shephard.player.ui.components.AlbumCarousel(
                        queue = state.queue, currentIndex = state.currentQueueIndex,
                        requestedIndex = pendingCarouselIndex, isPlaying = state.isPlaying, carouselStyle = carouselStyle,
                        onSongSelected = playerViewModel::playQueueItemAbsolute,
                        onAlbumClick = onAlbumClick,
                        modifier = Modifier.fillMaxWidth().height(artHeight),
                        placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                        placeholderContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    } else {
                        Box(Modifier.fillMaxWidth().height(artHeight), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(artHeight).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(64.dp))
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

                dev.shephard.player.ui.components.m3.AnimatedPlaybackControls(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                isPlayingProvider = { state.isPlaying },
                onPrevious = playerViewModel::skipToPrevious,
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::skipToNext,
                height = 80.dp,
                pressAnimationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                releaseDelay = 0L,
            )
                Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    dev.shephard.player.ui.components.m3.M3PlaybackModeButton(
                    icon = Icons.Filled.Shuffle, description = strings.remix,
                    active = isRemixed, onClick = playerViewModel::remixQueue,
                )
                    dev.shephard.player.ui.components.m3.M3PlaybackModeButton(
                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    description = strings.repeat, active = state.repeatMode != RepeatMode.OFF,
                    onClick = playerViewModel::cycleRepeatMode,
                )
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
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showQueue) {
        dev.shephard.player.ui.screens.QueueDrawer(playerViewModel = playerViewModel, useMiuix = false, onDismiss = { showQueue = false })
    }
    if (showAddToPlaylist && track != null) {
        M3AddToPlaylistDrawer(
            trackId = track.id,
            track = track,
            playerViewModel = playerViewModel,
            onDismiss = { showAddToPlaylist = false },
            strings = strings,
        )
    }

    val isLandscapeOutside = dev.shephard.player.ui.util.isLandscapeWindow()
    if (showLyrics && !isLandscapeOutside) {
        dev.shephard.player.ui.screens.FullScreenLyricsExperience(
        playerViewModel = playerViewModel,
        useMiuix = false,
        onDismiss = { showLyrics = false },
    )
    }

}


@Composable
private fun M3LandscapeContent(
    playerViewModel: PlayerViewModel,
    state: dev.shephard.player.player.PlayerUiState,
    navigationDirection: Int,
    pendingCarouselIndex: Int?,
    carouselStyle: String,
    onAlbumClick: (dev.shephard.player.data.AudioTrack) -> Unit,
    onShowQueueChange: (Boolean) -> Unit,
    onShowAddToPlaylistChange: (Boolean) -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    val configuration = LocalConfiguration.current
    val track = state.currentTrack
    Row(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            // Keep transport and lyrics usable on short landscape windows by sizing the cover
            // from the space left after the fixed metadata and control rows.
            val coverHeightLimit = (maxHeight - 218.dp).coerceAtLeast(48.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
            }
            Text(
                text = strings.nowPlaying,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                val pageFraction = when (carouselStyle) {
                    dev.shephard.player.player.CarouselStyle.NO_PEEK -> 1f
                    dev.shephard.player.player.CarouselStyle.TWO_PEEK -> .6f
                    else -> .8f
                }
                val artHeight = minOf(maxWidth * pageFraction, coverHeightLimit, configuration.screenHeightDp.dp * .45f) * 1.15f
                if (state.queue.isNotEmpty()) {
                    dev.shephard.player.ui.components.AlbumCarousel(
                        queue = state.queue, currentIndex = state.currentQueueIndex,
                        requestedIndex = pendingCarouselIndex, isPlaying = state.isPlaying, carouselStyle = carouselStyle,
                        onSongSelected = playerViewModel::playQueueItemAbsolute,
                        onAlbumClick = onAlbumClick,
                        modifier = Modifier.fillMaxWidth().height(artHeight),
                        placeholderColor = MaterialTheme.colorScheme.secondaryContainer,
                        placeholderContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(artHeight), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(artHeight).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(track?.title ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track?.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                val trackId = track?.id ?: -1L
                val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BouncyIconButton(
                        onClick = { onShowQueueChange(true) },
                        modifier = Modifier.size(44.dp),
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = strings.queue,
                        tint = MaterialTheme.colorScheme.onSurface,
                        iconSize = 22.dp,
                    )
                    Box(
                        modifier = Modifier.size(40.dp).combinedClickable(
                            onClick = { if (trackId > 0) playerViewModel.toggleLike(trackId) },
                            onLongClick = { onShowAddToPlaylistChange(true) },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = strings.likedSongs,
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                M3NowPlayingProgressLineage(playerViewModel = playerViewModel, isPlaying = state.isPlaying, compact = true)
                // Keep the landscape transport buttons on the same expressive M3 style as portrait.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isRemixed by playerViewModel.isRemixed.collectAsState()
                    dev.shephard.player.ui.components.m3.M3PlaybackModeButton(
                        icon = Icons.Filled.Shuffle,
                        description = strings.remix,
                        active = isRemixed,
                        onClick = playerViewModel::remixQueue,
                        modifier = Modifier.size(44.dp),
                    )
                    dev.shephard.player.ui.components.m3.AnimatedPlaybackControls(
                        modifier = Modifier.weight(1f),
                        isPlayingProvider = { state.isPlaying },
                        onPrevious = playerViewModel::skipToPrevious,
                        onPlayPause = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::skipToNext,
                        height = 44.dp,
                        pressAnimationSpec = androidx.compose.animation.core.tween(
                            240,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                        releaseDelay = 0L,
                        playPauseIconSize = 24.dp,
                        iconSize = 20.dp,
                    )
                    dev.shephard.player.ui.components.m3.M3PlaybackModeButton(
                        icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        description = strings.repeat,
                        active = state.repeatMode != RepeatMode.OFF,
                        onClick = playerViewModel::cycleRepeatMode,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            }
        }
        Box(
            modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            dev.shephard.player.ui.screens.MiuixEmbeddedLyrics(
                playerViewModel = playerViewModel,
                state = state,
                modifier = Modifier.fillMaxSize(),
                foreground = MaterialTheme.colorScheme.onSurface,
                titleStyle = MaterialTheme.typography.titleMedium,
                baseTextStyle = MaterialTheme.typography.bodyMedium,
                navigationDirection = navigationDirection,
            )
        }
    }
}


@Composable
internal fun M3NowPlayingProgressLineage(playerViewModel: PlayerViewModel, isPlaying: Boolean, compact: Boolean = false) {
    val progress by playerViewModel.progress.collectAsState()
    val smoothFraction = dev.shephard.player.ui.components.rememberSmoothProgressFraction(
        positionMs = progress.positionMs,
        durationMs = progress.durationMs,
        isPlaying = isPlaying,
    )
    var seekFraction by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(smoothFraction, progress.durationMs) {
        val held = seekFraction ?: return@LaunchedEffect
        if (kotlin.math.abs(held - smoothFraction) < 0.03f || kotlin.math.abs(held - smoothFraction) > 0.1f) {
            seekFraction = null
        }
    }

    val displayFraction = seekFraction ?: smoothFraction
    val horizontalInset = if (compact) 30.dp else 22.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        dev.shephard.player.ui.components.m3.M3WavySlider(
            value = { seekFraction ?: smoothFraction },
            onValueChange = { fraction ->
                seekFraction = fraction
                if (progress.durationMs > 0L) {
                    playerViewModel.onSeekPreview((fraction * progress.durationMs).toLong())
                }
            },
            onValueCommit = { fraction ->
                seekFraction = fraction
                if (progress.durationMs > 0L) {
                    playerViewModel.onSeekCommit((fraction * progress.durationMs).toLong())
                }
            },
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = horizontalInset)
                .then(if (compact) Modifier.height(24.dp) else Modifier),
            enabled = progress.durationMs > 0L,
            isPlaying = isPlaying,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
            thumbColor = MaterialTheme.colorScheme.primary,
            strokeWidth = if (compact) 4.dp else 5.dp,
            thumbRadius = if (compact) 6.dp else 8.dp,
            thumbLineHeightWhenInteracting = if (compact) 18.dp else 24.dp,
            semanticsLabel = LocalStrings.current.playbackPosition,
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = horizontalInset)
                .padding(top = if (compact) 0.dp else 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = m3FormatMillisLineage((displayFraction * progress.durationMs).toLong()),
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = m3FormatMillisLineage(progress.durationMs),
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun m3FormatMillisLineage(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun M3AddToPlaylistDrawer(
trackId: Long,
track: dev.shephard.player.data.AudioTrack?,
playerViewModel: PlayerViewModel,
onDismiss: () -> Unit,
strings: dev.shephard.player.ui.i18n.Strings
) {
    val context = LocalContext.current
    val prefs = remember { dev.shephard.player.player.PreferencesManager(context) }
    val json by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(json) { dev.shephard.player.ui.screens.parsePlaylists(json) }
    val likedJson by prefs.likedSongIds.collectAsState(initial = "[]")
    val likedIds = remember(likedJson) {
        try { org.json.JSONArray(likedJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } } }
        catch (_: Exception) { emptyList() }
    }
    val isLiked = likedIds.contains(trackId)
    val visiblePlaylists = remember(playlists) { playlists.filterNot { it.isSystem } }
    val togglePlaylist: (dev.shephard.player.ui.screens.LocalPlaylist) -> Unit = { playlist ->
        playerViewModel.viewModelScope.launch {
            try {
                prefs.updatePlaylistsJson { json ->
                    dev.shephard.player.ui.screens.updatePlaylistMembership(json, playlist.name, playlist.createdAt, listOf(trackId), "toggle")
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    M3BottomSheetWrapper(
    onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(strings.addToPlaylist, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            // Same insets as the queue sheet: cards keep 16dp from the screen edges and 6dp
            // between each other instead of touching the edges and their neighbours.
            androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                item {
                    androidx.compose.material3.ListItem(
                    headlineContent = { Text(strings.likedSongs) },
                    supportingContent = { Text("${likedIds.size} ${strings.trackCount}") },
                    leadingContent = { Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = {
                        Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                        playerViewModel.toggleLike(trackId)
                    }
                )
                }
                items(visiblePlaylists.size) { idxFiltered ->
                    val pl = visiblePlaylists[idxFiltered]
                    val contains = trackId in pl.trackIds
                    androidx.compose.material3.ListItem(
                    headlineContent = { Text(pl.name) },
                    supportingContent = { Text("${pl.trackIds.size} ${strings.trackCount}") },
                    trailingContent = {
                        Icon(
                        imageVector = if (contains) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (contains) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                        togglePlaylist(pl)
                    }
                )
                }
            }
        }
    }
}


private fun contrastColorFor(color: Color): Color =
if (color.luminance() >= 0.56f) Color(0xFF17131A) else Color.White

/** Easing Material 3 applies to predictive back progress (PredictiveBack.transform). */
private val M3PredictiveBackEasing = androidx.compose.animation.core.CubicBezierEasing(0.1f, 0.1f, 0f, 1f)