@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class, androidx.activity.ExperimentalActivityApi::class)

package dev.shephard.player.ui.screens


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.MiuixDrawerDefaults
import dev.shephard.player.ui.components.QueueRemoveButton
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.ImagePersistence
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.components.MinimalSeekBar
import dev.shephard.player.ui.components.MiuixPlayPauseButton
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
fun NowPlayingSheet(
playerViewModel: PlayerViewModel = viewModel(),
collapsedBounds: androidx.compose.ui.geometry.Rect? = null,
onAlbumClick: (AudioTrack) -> Unit = {},
onDismiss: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val navigationDirection by playerViewModel.navigationDirection.collectAsState()
    val pendingCarouselIndex by playerViewModel.pendingCarouselIndex.collectAsState()
    val context = LocalContext.current
    val track = state.currentTrack
    val strings = LocalStrings.current
    val nowPlayingLiquidGlassOn = LocalBlurEnabled.current
    // Detect gif/mp4 cover (override coverUri or track uri) for miuix handling
    val isAnimatedCover = remember(track?.albumArtUri) {
        track?.albumArtUri?.let { uri ->
            ImagePersistence.isAnimatedCover(context, uri)
        } ?: false
    }
    val carouselPrefs = remember(context) { PreferencesManager(context) }
    val carouselStyle by carouselPrefs.carouselStyle.collectAsState(initial = dev.shephard.player.player.CarouselStyle.NO_PEEK)

    var showQueue by remember { mutableStateOf(false) }
    var showPlaylists by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }

    val dragOffsetScreenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val dragOffsetInitialHeight = with(density) { dragOffsetScreenHeight.dp.toPx() }
    val dragOffset = remember { androidx.compose.animation.core.Animatable(dragOffsetInitialHeight) }
    val dragScope = rememberCoroutineScope()

    var hasEnteredRest by remember { mutableStateOf(false) }
    val nowPlayingEnterSpring = androidx.compose.animation.core.spring<Float>(
    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
    stiffness = 180f
)
    LaunchedEffect(Unit) {
        hasEnteredRest = false
        dragOffset.animateTo(0f, animationSpec = nowPlayingEnterSpring)
        hasEnteredRest = true
    }

    val artSlideSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = 260,
    easing = androidx.compose.animation.core.FastOutSlowInEasing
)
    val artFadeSpec = androidx.compose.animation.core.tween<Float>(
    durationMillis = 180,
    easing = androidx.compose.animation.core.LinearOutSlowInEasing
)

    val configuration = LocalConfiguration.current
    var measuredHeightPx by remember { mutableFloatStateOf(with(density) { configuration.screenHeightDp.dp.toPx() }) }
    val screenHeightPx = measuredHeightPx
    // True while a predictive back gesture drags the sheet (rounds the corners like a drag).
    var isBackGestureActive by remember { mutableStateOf(false) }

    val dismissWithAnimation: () -> Unit = {
        dragScope.launch {
            val remaining = (screenHeightPx - dragOffset.value).coerceAtLeast(0f)
            val duration = (remaining / screenHeightPx * 220).toLong().coerceIn(120L, 220L)
            dragOffset.animateTo(
            targetValue = screenHeightPx,
            animationSpec = androidx.compose.animation.core.tween(
            durationMillis = duration.toInt(),
            easing = androidx.compose.animation.core.FastOutLinearInEasing
        )
        )
            onDismiss()
        }
    }

    // Same predictive back as the Miuix drawers (WindowBottomSheet): while the back gesture runs
    // the sheet follows it downwards, releasing slides it off screen, cancelling snaps it back.
    PredictiveBackHandler(enabled = !showQueue && !showLyrics && !showPlaylists) { events ->
        isBackGestureActive = true
        try {
            events.collect { event ->
                dragOffset.snapTo(event.progress.coerceIn(0f, 1f) * screenHeightPx)
            }
            isBackGestureActive = false
            dismissWithAnimation()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            isBackGestureActive = false
            dragScope.launch { dragOffset.animateTo(0f, androidx.compose.animation.core.tween(150)) }
            throw cancelled
        }
    }


    var isInteractingWithSheet by remember { mutableStateOf(false) }
    val isFullyExpanded by remember {
        androidx.compose.runtime.derivedStateOf { hasEnteredRest && !isInteractingWithSheet && !isBackGestureActive && dragOffset.value <= 0.5f }
    }
    val sheetCornerRadius by androidx.compose.animation.core.animateDpAsState(
    targetValue = if (isFullyExpanded) 0.dp else 30.dp,
    animationSpec = androidx.compose.animation.core.spring(
    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
),
    label = "sheetCornerRadius"
)
    val sheetShape = androidx.compose.foundation.shape.RoundedCornerShape(sheetCornerRadius)

    Box(
    modifier = Modifier
    .fillMaxSize()
    .onSizeChanged { measuredHeightPx = it.height.toFloat() }
    .graphicsLayer {
        val p = (dragOffset.value / screenHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        translationY = dragOffset.value.coerceAtLeast(0f)
        alpha = (1f - p * .22f).coerceIn(0f, 1f)
        shape = sheetShape
        clip = true
    }
    .background(MiuixAppTheme.colorScheme.background)
    .draggable(
    orientation = Orientation.Vertical,
    state = rememberDraggableState { delta ->
        val next = (dragOffset.value + delta).coerceAtLeast(0f)
        dragScope.launch { dragOffset.snapTo(next) }
    },
    onDragStarted = {
        isInteractingWithSheet = true
    },
    onDragStopped = { velocity ->
        if (dragOffset.value > dismissThresholdPx || velocity > 2500f) {
            dragScope.launch {
                val remaining = (screenHeightPx - dragOffset.value).coerceAtLeast(0f)
                val duration = (remaining / screenHeightPx * 180).toLong().coerceIn(60L, 180L)
                dragOffset.animateTo(
                targetValue = screenHeightPx,
                animationSpec = androidx.compose.animation.core.tween(
                durationMillis = duration.toInt(),
                easing = androidx.compose.animation.core.FastOutLinearInEasing
            )
            )

                onDismiss()
            }
        } else {
            dragScope.launch {
                dragOffset.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.82f,
                stiffness = 320f
            )
            )
                isInteractingWithSheet = false
            }
        }
    }
)
    ) {

        if (!isAnimatedCover) AmbientGlowBackground(track = track, isPlaying = state.isPlaying, isAnimated = false)

        val isLandscape = dev.shephard.player.ui.util.isLandscapeWindow()
        if (isLandscape) {
            MiuixLandscapeContent(
            playerViewModel = playerViewModel,
            state = state,
            track = track,
            navigationDirection = navigationDirection,
            pendingCarouselIndex = pendingCarouselIndex,
            carouselStyle = carouselStyle,
            onAlbumClick = onAlbumClick,
            strings = strings,
            showQueue = showQueue,
            onShowQueueChange = { showQueue = it },
            showPlaylists = showPlaylists,
            onShowPlaylistsChange = { showPlaylists = it },
        )
        } else {
            Column(
            modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            ) {

                Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                        text = strings.nowPlaying,
                        style = MiuixAppTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                        if (state.currentPlaylistName != null) {
                            Text(
                            text = state.currentPlaylistName!!,
                            style = MiuixAppTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        }
                    }
                    Box(modifier = Modifier.size(48.dp))
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        val pageFraction = when (carouselStyle) {
                            dev.shephard.player.player.CarouselStyle.NO_PEEK -> 1f
                            dev.shephard.player.player.CarouselStyle.TWO_PEEK -> .6f
                            else -> .8f
                        }
                        val artHeight = minOf(maxWidth * pageFraction, configuration.screenHeightDp.dp * .42f)
                        if (state.queue.isNotEmpty()) {
                            dev.shephard.player.ui.components.AlbumCarousel(
                            queue = state.queue, currentIndex = state.currentQueueIndex,
                            requestedIndex = pendingCarouselIndex, isPlaying = state.isPlaying, carouselStyle = carouselStyle,
                            onSongSelected = playerViewModel::playQueueItemAbsolute,
                            onAlbumClick = onAlbumClick,
                            modifier = Modifier.fillMaxWidth().height(artHeight),
                            placeholderColor = MiuixAppTheme.colorScheme.surfaceVariant,
                            placeholderContentColor = MiuixAppTheme.colorScheme.primary,
                        )
                        } else {
                            Box(Modifier.fillMaxWidth().height(artHeight), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(artHeight).clip(RoundedCornerShape(24.dp)).background(MiuixAppTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.MusicNote, null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                                }
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        val forward = navigationDirection >= 0
                        (slideInHorizontally(artSlideSpec) { if (forward) it else -it } + fadeIn(artFadeSpec)) togetherWith
                        (slideOutHorizontally(artSlideSpec) { if (forward) -it else it } + fadeOut(artFadeSpec))
                    },
                    label = "nowPlayingMetadata",
                    ) { current ->
                        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (current != null) {
                                Text(current.title, style = MiuixAppTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
                                Text(current.artist, style = MiuixAppTheme.typography.bodyMedium, color = Color.White.copy(alpha = .7f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
                                if (current.album.isNotBlank()) Text(current.album, style = MiuixAppTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = .55f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                SeekBarRow(playerViewModel = playerViewModel)

                // M3 layout match: first row = prev/next, pause, shuffle, repeat (same as M3 first row)
                Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
                ) {
                    val isRemixed by playerViewModel.isRemixed.collectAsState()
                    BouncyIconButton(
                    onClick = { playerViewModel.remixQueue() },
                    icon = Icons.Filled.Shuffle,
                    contentDescription = strings.remix,
                    tint = if (isRemixed) MiuixAppTheme.colorScheme.primary else Color.White,
                    iconSize = 28.dp
                )
                    BouncyIconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    painter = androidx.compose.ui.res.painterResource(dev.shephard.player.R.drawable.ic_nowplaying_rewind),
                    contentDescription = strings.previous,
                    tint = Color.White,
                    iconSize = 36.dp
                )

                    MiuixPlayPauseButton(
                    isPlaying = state.isPlaying,
                    onClick = playerViewModel::togglePlayPause,
                    playDescription = strings.play,
                    pauseDescription = strings.pause,
                    tint = Color.White,
                )
                    BouncyIconButton(
                    onClick = { playerViewModel.skipToNext() },
                    painter = androidx.compose.ui.res.painterResource(dev.shephard.player.R.drawable.ic_nowplaying_fforward),
                    contentDescription = strings.next,
                    tint = Color.White,
                    iconSize = 36.dp
                )
                    BouncyIconButton(
                    onClick = { playerViewModel.cycleRepeatMode() },
                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = strings.repeat,
                    tint = if (state.repeatMode != RepeatMode.OFF) MiuixAppTheme.colorScheme.primary else Color.White,
                    iconSize = 28.dp
                )
                }

                // M3 layout match: second row = addToPlaylist, queue, lyrics with shared background Card
                val lyricsSheetScope = rememberCoroutineScope()
                val trackId = track?.id ?: -1L
                val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)

                Box(
                modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 42.dp),
                contentAlignment = Alignment.Center
                ) {
                    Row(
                    modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                        BouncyIconButton(
                        onClick = { showQueue = true },
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = strings.queue,
                        tint = Color.White,
                        iconSize = 28.dp
                    )
                        BouncyIconButton(
                        onClick = { showLyrics = true },
                        icon = Icons.Filled.Lyrics,
                        contentDescription = strings.lyrics,
                        tint = Color.White,
                        iconSize = 28.dp
                    )
                        Box(
                        modifier = Modifier
                        .size(48.dp)
                        .bounceClick {
                            if (trackId > 0) {
                                if (isLiked) {
                                    showPlaylists = true
                                } else {
                                    playerViewModel.addToLiked(trackId)
                                }
                            }
                        },
                        contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedContent(
                            targetState = isLiked,
                            transitionSpec = {
                                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) +
                                androidx.compose.animation.scaleIn(
                                initialScale = 0.45f,
                                animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.55f,
                                stiffness = 400f
                            )
                            )).togetherWith(
                                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) +
                                androidx.compose.animation.scaleOut(targetScale = 1.35f)
                            )
                            },
                            label = "likeMorph"
                            ) { liked ->
                                Icon(
                                imageVector = if (liked) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = if (liked) "Added" else "Add",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            }
                        }
                    }
                }

            }
        }
        if (showQueue) {
            QueueDrawer(playerViewModel = playerViewModel, useMiuix = true, onDismiss = { showQueue = false })
        }

        if (showLyrics && !isLandscape) {
            FullScreenLyricsExperience(
            playerViewModel = playerViewModel,
            useMiuix = true,
            onDismiss = { showLyrics = false },
        )
        }

        if (showPlaylists) {
            AddToPlaylistDrawer(track?.id ?: -1L, track, playerViewModel, { showPlaylists = false }, strings)
        }
    }
}


@Composable
private fun MiuixLandscapeContent(
    playerViewModel: PlayerViewModel,
    state: dev.shephard.player.player.PlayerUiState,
    track: AudioTrack?,
    navigationDirection: Int,
    pendingCarouselIndex: Int?,
    carouselStyle: String,
    onAlbumClick: (AudioTrack) -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
    showQueue: Boolean,
    onShowQueueChange: (Boolean) -> Unit,
    showPlaylists: Boolean,
    onShowPlaylistsChange: (Boolean) -> Unit,
) {
    val configuration = LocalConfiguration.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Reserve room for metadata and transport controls so the cover never pushes them
            // below the landscape viewport on short displays.
            val coverHeightLimit = (maxHeight - 245.dp).coerceAtLeast(40.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(32.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.nowPlaying,
                        style = MiuixAppTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    if (state.currentPlaylistName != null) {
                        Text(
                            text = state.currentPlaylistName!!,
                            style = MiuixAppTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Box(modifier = Modifier.size(32.dp))
            }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    val pageFraction = when (carouselStyle) {
                        dev.shephard.player.player.CarouselStyle.NO_PEEK -> 1f
                        dev.shephard.player.player.CarouselStyle.TWO_PEEK -> .6f
                        else -> .8f
                    }
                    val artHeight = minOf(maxWidth * pageFraction, coverHeightLimit, configuration.screenHeightDp.dp * .42f)
                    if (state.queue.isNotEmpty()) {
                        dev.shephard.player.ui.components.AlbumCarousel(
                            queue = state.queue,
                            currentIndex = state.currentQueueIndex,
                            requestedIndex = pendingCarouselIndex,
                            isPlaying = state.isPlaying,
                            carouselStyle = carouselStyle,
                            onSongSelected = playerViewModel::playQueueItemAbsolute,
                            onAlbumClick = onAlbumClick,
                            modifier = Modifier.fillMaxWidth().height(artHeight),
                            placeholderColor = MiuixAppTheme.colorScheme.surfaceVariant,
                            placeholderContentColor = MiuixAppTheme.colorScheme.primary,
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(artHeight), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(artHeight).clip(RoundedCornerShape(24.dp))
                                    .background(MiuixAppTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }
                androidx.compose.animation.AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        val forward = navigationDirection >= 0
                        val slideSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 320, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        val fadeSpec = androidx.compose.animation.core.tween<Float>(durationMillis = 220, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                        (slideInHorizontally(slideSpec) { if (forward) it else -it } + fadeIn(fadeSpec)) togetherWith
                            (slideOutHorizontally(slideSpec) { if (forward) -it else it } + fadeOut(fadeSpec))
                    },
                    label = "nowPlayingMetadataLand",
                ) { current ->
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (current != null) {
                            Text(
                                current.title, style = MiuixAppTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                current.artist, style = MiuixAppTheme.typography.bodySmall, color = Color.White.copy(alpha = .7f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BouncyIconButton(
                        onClick = { onShowQueueChange(true) },
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = strings.queue,
                        tint = Color.White,
                        iconSize = 22.dp,
                    )
                    BouncyIconButton(
                        onClick = { track?.id?.takeIf { it > 0 }?.let(playerViewModel::toggleLike) },
                        icon = if (track?.id != null && state.likedSongIds.contains(track.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = strings.likedSongs,
                        tint = if (track?.id != null && state.likedSongIds.contains(track.id)) MiuixAppTheme.colorScheme.primary else Color.White,
                        iconSize = 22.dp,
                    )
                }
                SeekBarRow(playerViewModel = playerViewModel, compact = true)
                // Compact landscape transport controls stay proportional to the cover.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp, start = 0.dp, end = 0.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isRemixed by playerViewModel.isRemixed.collectAsState()
                    BouncyIconButton(
                        onClick = { playerViewModel.remixQueue() },
                        icon = Icons.Filled.Shuffle,
                        contentDescription = strings.remix,
                        tint = if (isRemixed) MiuixAppTheme.colorScheme.primary else Color.White,
                        iconSize = 20.dp
                    )
                    BouncyIconButton(
                        onClick = { playerViewModel.skipToPrevious() },
                        painter = androidx.compose.ui.res.painterResource(dev.shephard.player.R.drawable.ic_nowplaying_rewind),
                        contentDescription = strings.previous,
                        tint = Color.White,
                        iconSize = 24.dp
                    )
                    MiuixPlayPauseButton(
                        isPlaying = state.isPlaying,
                        onClick = playerViewModel::togglePlayPause,
                        playDescription = strings.play,
                        pauseDescription = strings.pause,
                        tint = Color.White,
                        buttonSize = 52.dp,
                        iconSize = 30.dp,
                    )
                    BouncyIconButton(
                        onClick = { playerViewModel.skipToNext() },
                        painter = androidx.compose.ui.res.painterResource(dev.shephard.player.R.drawable.ic_nowplaying_fforward),
                        contentDescription = strings.next,
                        tint = Color.White,
                        iconSize = 24.dp
                    )
                    BouncyIconButton(
                        onClick = { playerViewModel.cycleRepeatMode() },
                        icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = strings.repeat,
                        tint = if (state.repeatMode != RepeatMode.OFF) MiuixAppTheme.colorScheme.primary else Color.White,
                        iconSize = 20.dp
                    )
                }
            }
            }
        }
        // Ortadan ikiye bölen dik çizgi — arkaplan kartı kaldırıldı
        Box(
            modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            MiuixEmbeddedLyrics(
                playerViewModel = playerViewModel,
                state = state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    if (showQueue) {
        QueueDrawer(playerViewModel = playerViewModel, useMiuix = true, onDismiss = { onShowQueueChange(false) })
    }
    if (showPlaylists) {
        AddToPlaylistDrawer(track?.id ?: -1L, track, playerViewModel, { onShowPlaylistsChange(false) }, strings)
    }
}

@Composable
internal fun MiuixEmbeddedLyrics(
playerViewModel: PlayerViewModel,
state: dev.shephard.player.player.PlayerUiState,
modifier: Modifier = Modifier,
foreground: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
titleStyle: androidx.compose.ui.text.TextStyle? = null,
baseTextStyle: androidx.compose.ui.text.TextStyle? = null
) {
    // Yatay mod artık dikeyle birebir aynı animasyonlar — kart kaldırıldı, dik çizgi ile bölünmüş
    val progress by playerViewModel.progress.collectAsState()
    val syncOffsetMs by playerViewModel.lyricsSyncOffsetMs.collectAsState()
    val synced = state.syncedLyrics
    val plain = state.lyrics
    val textStyle = baseTextStyle ?: MiuixAppTheme.typography.bodyMedium
    val primary = MiuixAppTheme.colorScheme.primary
    val isPlaying = state.isPlaying
    // Adaptive foreground: Miuix always white (dark bg), M3 will override via caller param
    val fg = foreground
    val lyricsListState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { dev.shephard.player.player.PreferencesManager(context) }
    val blurEnabled by preferences.lyricsBackgroundBlurEnabled.collectAsState(initial = true)
    val blurStrength by preferences.lyricsBackgroundBlurStrength.collectAsState(initial = 36f)

    val hasWordSync = remember(synced) { synced.any { it.words.isNotEmpty() } }
    val smoothPositionMs = rememberSmoothLyricsPosition(
        positionMs = progress.positionMs,
        isPlaying = isPlaying,
        enabled = synced.isNotEmpty(),
        frameAccurate = hasWordSync,
    )
    val activeIndex by remember(synced, syncOffsetMs) {
        androidx.compose.runtime.derivedStateOf {
            if (synced.isEmpty()) -1 else {
                var low = 0
                var high = synced.lastIndex
                val pos = smoothPositionMs.longValue - syncOffsetMs
                while (low <= high) {
                    val mid = (low + high).ushr(1)
                    if (synced[mid].timeMs <= pos) low = mid + 1 else high = mid - 1
                }
                high
            }
        }
    }
    val showNoteAnim by remember(synced, smoothPositionMs.longValue, activeIndex, syncOffsetMs) {
        androidx.compose.runtime.derivedStateOf {
            if (!hasWordSync) false else {
                val pos = smoothPositionMs.longValue - syncOffsetMs
                val curr = synced.getOrNull(activeIndex)
                val inWord = curr?.words?.any { w -> pos in w.startMs..w.endMs } == true
                if (inWord) false else {
                    if (activeIndex < 0) {
                        val first = synced.firstOrNull()?.timeMs ?: 0L
                        pos < first - 250
                    } else {
                        val next = synced.getOrNull(activeIndex + 1)
                        val lastEnd = curr?.words?.lastOrNull()?.endMs ?: curr?.timeMs ?: pos
                        val gapFromEnd = pos - lastEnd
                        val gapToNext = if (next != null) next.timeMs - pos else 0L
                        gapFromEnd > 700 || gapToNext > 800
                    }
                }
            }
        }
    }

    LaunchedEffect(activeIndex) {
        if (synced.isNotEmpty() && activeIndex >= 0) {
            try {
                val layoutInfo = lyricsListState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                val targetIndex = activeIndex.coerceIn(synced.indices)
                val visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                val itemHeight = visible?.size ?: run {
                    val textWidthPx = (layoutInfo.viewportSize.width - with(density) { (LyricLineHorizontalPadding * 2).toPx() }).toInt().coerceAtLeast(1)
                    textMeasurer.measure(
                        text = synced[targetIndex].text,
                        style = lyricLineLayoutStyle(textStyle),
                        constraints = androidx.compose.ui.unit.Constraints(maxWidth = textWidthPx),
                    ).size.height + with(density) { (LyricLineVerticalPadding * 2).toPx() }.toInt()
                }
                val desiredOffset = viewportHeight * LyricAnchorFraction - itemHeight / 2f - layoutInfo.beforeContentPadding
                if (visible != null) {
                    lyricsListState.animateScrollBy(visible.offset - desiredOffset, animationSpec = androidx.compose.animation.core.tween(LyricMoveDurationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                } else {
                    lyricsListState.animateScrollToItem(index = targetIndex, scrollOffset = -desiredOffset.toInt())
                }
            } catch (_: Exception) {}
        }
    }

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = dev.shephard.player.ui.i18n.LocalStrings.current.lyrics,
            style = (titleStyle ?: MiuixAppTheme.typography.titleMedium),
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (synced.isNotEmpty()) {
                LazyColumn(
                    state = lyricsListState,
                    modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                    contentPadding = PaddingValues(top = 28.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(synced, key = { index, line -> "${line.timeMs}_$index" }) { index, line ->
                        val isActive = index == activeIndex
                        val distance = if (activeIndex < 0) 0 else kotlin.math.abs(index - activeIndex)
                        val lineBlur = if (blurEnabled && activeIndex >= 0) kotlin.math.min(10f, distance * (blurStrength / 24f)).dp else 0.dp
                        val activeAmount by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isActive) 1f else 0f, animationSpec = androidx.compose.animation.core.tween(LyricMoveDurationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "lyricLineSizeH")
                        val animatedBlur by androidx.compose.animation.core.animateDpAsState(targetValue = lineBlur, animationSpec = androidx.compose.animation.core.tween(LyricMoveDurationMs), label = "lyricLineBlurH")
                        val fg = foreground
                        val karaoke = isActive && line.words.isNotEmpty()
                        val targetColor = when {
                            karaoke -> fg.copy(alpha = 0.45f)
                            isActive -> fg
                            index < activeIndex && line.words.isNotEmpty() && distance <= 2 -> fg.copy(alpha = 0.90f)
                            distance > 3 -> fg.copy(alpha = 0.40f)
                            else -> fg.copy(alpha = 0.72f)
                        }
                        val animatedColor by androidx.compose.animation.animateColorAsState(targetValue = targetColor, animationSpec = androidx.compose.animation.core.tween(LyricMoveDurationMs), label = "lyricLineColorH")
                        var textLayout by remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                        var shakeTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                        androidx.compose.runtime.LaunchedEffect(isActive) { shakeTrigger++ }
                        val shouldJiggle = isActive
                        val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "lyricJiggleH")
                        val jiggleX by infinite.animateFloat(initialValue = -0.3f, targetValue = 0.3f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(700, easing = androidx.compose.animation.core.LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "jiggleXH")
                        val jiggleY by infinite.animateFloat(initialValue = -0.3f, targetValue = 0.3f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(800, easing = androidx.compose.animation.core.LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "jiggleYH")
                        val lineSlideOffset by androidx.compose.animation.core.animateDpAsState(targetValue = if (isActive) 0.dp else if (distance == 1) 4.dp else 0.dp, animationSpec = androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "lineSlideH")
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { playerViewModel.seekTo((line.timeMs + syncOffsetMs).coerceAtLeast(0)) }.graphicsLayer {
                                val scale = LyricInactiveScale + (1f - LyricInactiveScale) * activeAmount
                                scaleX = scale; scaleY = scale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                translationY += lineSlideOffset.toPx() * (if (index < activeIndex) -1f else 1f)
                                if (shouldJiggle) { translationX = jiggleX * 0.6f; translationY += jiggleY * 0.5f }
                            }.blur(animatedBlur).padding(horizontal = LyricLineHorizontalPadding, vertical = LyricLineVerticalPadding),
                        ) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.foundation.text.BasicText(
                                    text = line.text,
                                    onTextLayout = { textLayout = it },
                                    modifier = Modifier.fillMaxWidth().drawWithContent {
                                        drawContent()
                                        if (karaoke) textLayout?.let { layout ->
                                            drawKaraokeHighlight(layout = layout, line = line, positionMs = smoothPositionMs.longValue - syncOffsetMs, color = fg)
                                        }
                                    },
                                    style = lyricLineLayoutStyle(textStyle).copy(color = animatedColor),
                                )
                            }
                            if (!line.translation.isNullOrBlank()) {
                                androidx.compose.foundation.text.BasicText(
                                    text = line.translation,
                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                                    style = textStyle.copy(color = animatedColor.copy(alpha = animatedColor.alpha * .78f), fontSize = 16.sp, lineHeight = 22.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Left),
                                )
                            }
                        }
                    }
                }
            } else if (plain.isNotEmpty()) {
                LazyColumn(state = lyricsListState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(plain.size) { idx -> Text(text = plain[idx], style = textStyle.copy(color = fg.copy(alpha = 0.82f), fontSize = 13.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        QqMusicNoteAnimation(primary = primary, foreground = fg)
                        Text(
                            dev.shephard.player.ui.i18n.LocalStrings.current.noLyricsFound,
                            color = fg.copy(alpha = 0.55f),
                            style = textStyle,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showNoteAnim,
                enter = androidx.compose.animation.fadeIn(tween(220)),
                exit = androidx.compose.animation.fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp).fillMaxWidth().padding(horizontal = 18.dp)
            ) {
                QqMusicNoteAnimation(primary = primary, foreground = fg)
            }
        }
    }
}


@Composable
private fun AmbientGlowBackground(
track: AudioTrack?,
isPlaying: Boolean,
isAnimated: Boolean = false
) {
    val darkOverlayAlpha by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (isPlaying) 0.35f else 0.68f,
    animationSpec = androidx.compose.animation.core.tween(400),
    label = "darkOverlayAlpha"
)
    Box(modifier = Modifier.fillMaxSize()) {
        // For animated gif/mp4 covers, show only cover animation with no blurred background
        if (isAnimated) {
            Box(
            modifier = Modifier
            .fillMaxSize()
            .background(MiuixAppTheme.colorScheme.background)
        )
        } else {
            androidx.compose.animation.Crossfade(
            targetState = track?.albumArtUri,
            animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 420,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
            label = "ambientAlbumArtFade"
            ) { artUri ->
                if (artUri != null) {
                    AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }
                    .blur(radius = 32.dp),
                    colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(1.25f) }
                )
                )
                } else {
                    Box(
                    modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixAppTheme.colorScheme.surfaceVariant)
                )
                }
            }
        }
        Box(
        modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = darkOverlayAlpha))
    )
    }
}

@Composable
private fun SeekBarRow(playerViewModel: PlayerViewModel, compact: Boolean = false) {
    val progress by playerViewModel.progress.collectAsState()
    val state by playerViewModel.uiState.collectAsState()
    val smoothFraction = dev.shephard.player.ui.components.rememberSmoothProgressFraction(
    positionMs = progress.positionMs,
    durationMs = progress.durationMs,
    isPlaying = state.isPlaying
)
    var seekFraction by remember { mutableStateOf<Float?>(null) }
    androidx.compose.runtime.LaunchedEffect(smoothFraction, progress.durationMs) {
        val held = seekFraction ?: return@LaunchedEffect
        if (kotlin.math.abs(held - smoothFraction) < 0.03f || kotlin.math.abs(held - smoothFraction) > 0.1f) {
            seekFraction = null
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        MinimalSeekBar(
        progress = seekFraction ?: smoothFraction,
        onSeekPreview = { fraction ->
            seekFraction = fraction
            if (progress.durationMs > 0) playerViewModel.onSeekPreview((fraction * progress.durationMs).toLong())
        },
        onSeekFinished = { fraction ->
            seekFraction = fraction
            if (progress.durationMs > 0) playerViewModel.onSeekCommit((fraction * progress.durationMs).toLong())
        },
        modifier = Modifier.padding(horizontal = if (compact) 10.dp else 22.dp),
        trackHeight = if (compact) 4.dp else 6.dp,
        expandOnPress = true,
        activeColor = Color.White,
        inactiveColor = Color.White.copy(alpha = 0.3f)
    )
        Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (compact) 2.dp else 6.dp, start = if (compact) 10.dp else 22.dp, end = if (compact) 10.dp else 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
            formatMillis(if (seekFraction != null) (seekFraction!! * progress.durationMs).toLong() else progress.positionMs),
            style = if (compact) MiuixAppTheme.typography.labelSmall else MiuixAppTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f)
        )
            Text(
            formatMillis(progress.durationMs),
            style = if (compact) MiuixAppTheme.typography.labelSmall else MiuixAppTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f)
        )
        }
    }
}

@Composable
internal fun AddToPlaylistDrawer(
trackId: Long,
track: AudioTrack?,
playerViewModel: PlayerViewModel,
onDismiss: () -> Unit,
strings: dev.shephard.player.ui.i18n.Strings
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val json by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(json) { parsePlaylists(json) }
    val likedJson by prefs.likedSongIds.collectAsState(initial = "[]")
    val likedIds = remember(likedJson) {
        try { org.json.JSONArray(likedJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } } }
        catch (_: Exception) { emptyList() }
    }
    val isLiked = likedIds.contains(trackId)
    val visiblePlaylists = remember(playlists) { playlists.filterNot { it.isSystem } }
    val togglePlaylist: (LocalPlaylist) -> Unit = { playlist ->
        playerViewModel.viewModelScope.launch {
            try {
                prefs.updatePlaylistsJson { json ->
                    updatePlaylistMembership(json, playlist.name, playlist.createdAt, listOf(trackId), "toggle")
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, strings.actionFailed, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    MiuixDrawer(
    onDismissRequest = onDismiss,
    ) {
        val card = dev.shephard.player.ui.components.miuixDrawerCardColor()
        Column(
        modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .fillMaxHeight(0.62f)
        ) {
            Text(strings.addToPlaylist, style = MiuixAppTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
            modifier = Modifier
            .weight(1f)
            .overScrollVertical(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {

                item {
                    Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(card)
                    .clickable { playerViewModel.toggleLike(trackId) }
                    .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        .background(MiuixAppTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Favorite, null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.likedSongs, fontWeight = FontWeight.SemiBold)
                            Text("${likedIds.size} ${strings.trackCount}", style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(if (isLiked) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f) else MiuixAppTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                        ) {
                            Icon(
                            imageVector = if (isLiked) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (isLiked) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        }
                    }
                }

                itemsIndexed(visiblePlaylists) { _, pl ->
                    val containsTrack = pl.trackIds.contains(trackId)
                    Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(card)
                    .clickable { togglePlaylist(pl) }
                    .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, fontWeight = FontWeight.SemiBold)
                            Text("${pl.trackIds.size} ${strings.trackCount}", style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(if (containsTrack) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f) else MiuixAppTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                        ) {
                            Icon(
                            imageVector = if (containsTrack) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (containsTrack) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QueueList(
queue: List<AudioTrack>,
currentTrackId: Long?,
onMove: (from: Int, to: Int) -> Unit,
onPlay: (index: Int) -> Unit,
onPlayNext: (index: Int) -> Unit = {},
onRemove: (index: Int) -> Unit,
strings: dev.shephard.player.ui.i18n.Strings
) {
    val currentStartIndex = remember(queue, currentTrackId) {
        queue.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
    }

    val items = remember { mutableStateListOf<AudioTrack>() }
    LaunchedEffect(queue, currentStartIndex) {
        if (items.map { it.id } != queue.drop(currentStartIndex).map { it.id }) {
            items.clear()
            items.addAll(queue.drop(currentStartIndex))
        }
    }

    val listState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
    lazyListState = listState
    ) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) {
            from.index to to.index
        } else {
            currentDragInfo.first to to.index
        }
        items.add(to.index, items.removeAt(from.index))
    }
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                dragInfo = null
                if (from == to) return@LaunchedEffect
                val fromInQueue = from + currentStartIndex
                val toInQueue = to + currentStartIndex
                if (fromInQueue != toInQueue) onMove(fromInQueue, toInQueue)
            }
        }
    }

    Column(
    modifier = Modifier
    .fillMaxWidth()
    .fillMaxHeight(0.66f)
    .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
        text = strings.queue,
        style = MiuixAppTheme.typography.titleMedium,
        color = MiuixAppTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )
        LazyColumn(
        state = listState,
        modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .overScrollVertical(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            itemsIndexed(items, key = { _, t -> t.id }) { index, track ->
                ReorderableItem(
                state = reorderableState,
                key = track.id
                ) { isDragging ->
                    QueueTrackItem(
                    track = track,
                    isPlaying = track.id == currentTrackId,
                    isDragged = isDragging,
                    onPlay = { onPlay(index) },
                    onPlayNext = { onPlayNext(index) },
                    onRemove = { onRemove(index) },
                    dragHandleModifier = Modifier.draggableHandle()
                )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun QueueTrackItem(
track: AudioTrack,
isPlaying: Boolean,
isDragged: Boolean,
onPlay: () -> Unit,
onPlayNext: () -> Unit,
onRemove: () -> Unit,
dragHandleModifier: Modifier,
) {
    val strings = LocalStrings.current
    val duration = remember(track.id) { track.formattedDuration() }
    val rowShape = RoundedCornerShape(12.dp)
    // Cards sit on the drawer's surfaceContainer, so tint them darker than it (they used to be
    // the same colour and vanished). Opaque colours only: no elevation shadow while dragging —
    // a shadow behind a translucent card showed through below it and looked like a lagging
    // second item. The dragged card just takes a stronger accent tint (pressed look).
    val drawerColor = MiuixDrawerDefaults.backgroundColor()
    val cardColor = Color.Black.copy(alpha = if (drawerColor.luminance() < 0.5f) 0.28f else 0.06f)
    .compositeOver(drawerColor)
    val accent = MiuixAppTheme.colorScheme.primary
    val rowColor by androidx.compose.animation.animateColorAsState(
    targetValue = when {
        isDragged -> accent.copy(alpha = 0.24f).compositeOver(cardColor)
        isPlaying -> accent.copy(alpha = 0.12f).compositeOver(cardColor)
        else -> cardColor
    },
    label = "queueItemColor"
)
    Row(
    modifier = Modifier
    .fillMaxWidth()
    .height(64.dp)
    .clip(rowShape)
    .background(rowColor, rowShape)
    .clickable { onPlay() }
    .padding(vertical = 6.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
        modifier = Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(MiuixAppTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
        ) {
            var loaded by remember { mutableStateOf(false) }
            AsyncImage(
            model = dev.shephard.player.ui.components.rememberStaticArtwork(track.albumArtUri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true }
        )
            if (!loaded) {
                Icon(Icons.Filled.MusicNote, null, tint = MiuixAppTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
            track.title,
            color = if (isPlaying) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onBackground,
            style = MiuixAppTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
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
        Text(duration, style = MiuixAppTheme.typography.labelSmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        // Red trash button left of drag handle: first tap asks "Confirm?", second tap removes.
        if (!isPlaying) {
            QueueRemoveButton(
            trackId = track.id,
            contentDescription = strings.removeFromQueue,
            confirmLabel = strings.queueRemoveConfirm,
            color = Color(0xFFE53935),
            labelStyle = MiuixAppTheme.typography.labelSmall,
            onRemove = onRemove,
        )
            Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = strings.reorder,
            tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp).then(dragHandleModifier)
        )
        }
    }
}

internal fun formatMillis(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private fun cleanTitle(title: String): String =
title.trim()
.replace(Regex("\\s*\\(feat\\.?[^)]*\\)", RegexOption.IGNORE_CASE), "")
.replace(Regex("\\s*\\[feat\\.?[^]]*]", RegexOption.IGNORE_CASE), "")
.replace(Regex("\\s*\\(ft\\.?[^)]*\\)", RegexOption.IGNORE_CASE), "")
.replace(Regex("\\s*-\\s*(official|lyrics?|video|audio|remaster.*|live.*)$", RegexOption.IGNORE_CASE), "")
.trim()

private fun httpGet(urlStr: String, timeoutMs: Int = 8000): String? = try {
    val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
    conn.connectTimeout = timeoutMs
    conn.readTimeout = timeoutMs
    conn.setRequestProperty("Accept", "application/json")
    conn.setRequestProperty("User-Agent", "LambdaPlayer/2.5")
    if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
} catch (_: Exception) { null }

private fun fetchFromLrclib(artist: String, title: String): List<String>? {
    val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }

    val body = httpGet("https://lrclib.net/api/get?artist_name=${enc(artist)}&track_name=${enc(title)}")
    ?: httpGet("https://lrclib.net/api/get?artist_name=${enc(artist)}&track_name=${enc(cleanTitle(title))}")
    ?: return null
    val json = runCatching { org.json.JSONObject(body) }.getOrNull() ?: return null
    val plain = json.optString("plainLyrics")
    if (plain.isNotBlank()) return plain.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
    val synced = json.optString("syncedLyrics")
    if (synced.isNotBlank()) return synced.lines().mapNotNull { line ->
        line.replace(Regex("^\\[\\d+:\\d+\\.\\d+]\\s*"), "").trim().ifBlank { null }
    }
    return null
}

private fun fetchFromLrclibSearch(artist: String, title: String): List<String>? {
    val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
    val cleanT = cleanTitle(title)
    val body = httpGet("https://lrclib.net/api/search?artist_name=${enc(artist)}&track_name=${enc(cleanT)}")
    ?: return null
    val arr = runCatching { org.json.JSONArray(body) }.getOrNull() ?: return null
    if (arr.length() == 0) return null
    val best = arr.getJSONObject(0)
    val plain = best.optString("plainLyrics")
    if (plain.isNotBlank()) return plain.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
    val synced = best.optString("syncedLyrics")
    if (synced.isNotBlank()) return synced.lines().mapNotNull { line ->
        line.replace(Regex("^\\[\\d+:\\d+\\.\\d+]\\s*"), "").trim().ifBlank { null }
    }
    return null
}

private fun fetchFromLyricsOvh(artist: String, title: String): List<String>? {
    val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
    val body = httpGet("https://api.lyrics.ovh/v1/${enc(artist)}/${enc(cleanTitle(title))}")
    ?: return null
    val json = runCatching { org.json.JSONObject(body) }.getOrNull() ?: return null
    val lyrics = json.optString("lyrics")
    return lyrics.takeIf { it.isNotBlank() }
    ?.lines()?.map { it.trimEnd() }?.filter { it.isNotBlank() }
}

private fun fetchLyricsFromApi(artist: String, title: String): List<String>? =
fetchFromLrclib(artist, title)
?: fetchFromLrclibSearch(artist, title)
?: fetchFromLyricsOvh(artist, title)
