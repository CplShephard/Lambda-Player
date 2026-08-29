@file:Suppress("DEPRECATION")

package dev.shephard.player.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderPositions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding
import dev.shephard.player.R
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.nowplaying.components.AirPlay
import dev.shephard.player.ui.nowplaying.util.Haptics
import dev.shephard.player.ui.nowplaying.util.MediaViewModelObject
import dev.shephard.player.ui.nowplaying.util.SettingsLibrary
import dev.shephard.player.ui.nowplaying.util.YosMediaEvent
import dev.shephard.player.ui.nowplaying.util.YosUIConfig
import dev.shephard.player.ui.nowplaying.util.formatTimeSeconds
import dev.shephard.player.ui.widgets.YosLyricView
import dev.shephard.player.ui.widgets.audio.MusicQualityIndicator
import dev.shephard.player.ui.widgets.basic.ImageQuality
import dev.shephard.player.ui.widgets.basic.ShadowImageWithCache
import dev.shephard.player.ui.widgets.basic.YosWrapper
import dev.shephard.player.ui.widgets.effects.ShadowType
import dev.shephard.player.ui.widgets.effects.YosFloatingLight
import dev.shephard.player.ui.widgets.effects.overlayEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalSharedTransitionApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun NowPlayingSheet(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit,
) {
    val uiState by playerViewModel.uiState.collectAsState()
    val currentTrack = uiState.currentTrack
    val isPlaying = uiState.isPlaying
    val queue = uiState.queue
    val likedIds = uiState.likedSongIds
    val repeatMode = uiState.repeatMode
    val progress by playerViewModel.progress.collectAsState()
    val context = LocalContext.current

    val showControl = rememberSaveable(key = "NowPlaying_showControl") { mutableStateOf(true) }
    val lastClickTime = rememberSaveable(key = "NowPlaying_lastClickTime") { mutableStateOf(0L) }
    val translation = rememberSaveable(key = "NowPlaying_translation") { mutableStateOf(false) }

    val isOnLyricPage = rememberSaveable(key = "NowPlaying_nowPage") { mutableStateOf("Album") }

    SettingsLibrary.Bind()

    // Feed the imported YosLyricView's expected shape (list of
    // (timestamp_ms, text) pairs per line) from Lambda's synced lyrics.
    val syncedLyrics = uiState.syncedLyrics
    LaunchedEffect(syncedLyrics, currentTrack?.id) {
        MediaViewModelObject.bitmap.value = currentTrack?.albumArtUri
        MediaViewModelObject.lrcEntries.value = if (syncedLyrics.isNotEmpty()) {
            // Wrap each line in its own list — the lyric view expects
            // [timestampMs, text] pairs in inner lists so it can render
            // the original + translation pair side by side.
            syncedLyrics.map { line -> listOf(line.timeMs.toFloat() to line.text) }
        } else {
            // Fall back to plain (un-timed) lyrics with timestamp 0.
            uiState.lyrics.map { line -> listOf(0f to line) }
        }
    }

    LaunchedEffect(showControl.value, isOnLyricPage.value, lastClickTime.value) {
        if (isOnLyricPage.value != "Lyric" && !showControl.value) {
            showControl.value = true
        }
        if (showControl.value) {
            val time = 2500L
            delay(time)
            withContext(Dispatchers.Main) {
                if (System.currentTimeMillis() - lastClickTime.value >= time &&
                    isOnLyricPage.value == "Lyric"
                ) {
                    showControl.value = false
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(isOnLyricPage.value) {
        val targetAlpha = if (isOnLyricPage.value == "Lyric") 1f else 0f
        scope.launch { alphaAnim.animateTo(targetAlpha) }
    }
    val translationButtonEnabled = remember("NowPlaying_translationButtonEnabled") {
        derivedStateOf { showControl.value && alphaAnim.value != 0f }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        contentColor = Color.White,
        color = Color.Transparent,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background floating light
            YosFloatingLight(
                album = { currentTrack?.albumArtUri },
                isPlaying = { isPlaying },
                modifier = Modifier.fillMaxSize(),
                nowPage = { isOnLyricPage.value },
                showMiniPlayer = { true },
            )

            // Lyric overlay (hidden for now since Lambda has no lyrics engine)
            if (false) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            this.alpha = alphaAnim.value
                        },
                ) {}
            }

            // Top grabber handle
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .overlayEffect()
                            .size(width = 32.dp, height = 4.5.dp)
                            .background(Color(0x4DFFFFFF), RoundedCornerShape(2.25.dp))
                            .clip(RoundedCornerShape(2.25.dp)),
                    )
                }
            }

            // Main view: Album / Lyric / PlayingList crossfade
            SharedTransitionLayout {
                androidx.compose.animation.Crossfade(
                    targetState = isOnLyricPage.value,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 22.dp),
                ) { page ->
                    when (page) {
                        "Album" -> AlbumPage(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            onArtClick = { isOnLyricPage.value = "Lyric" },
                            isFavorite = currentTrack?.id?.let { it in likedIds } == true,
                            onToggleFavorite = { track ->
                                if (track.id > 0) playerViewModel.addToLiked(track.id)
                            },
                        )
                        "Lyric" -> Column(Modifier.fillMaxSize()) {
                            PlayingBar(
                                modifier = Modifier,
                                track = currentTrack,
                                isFavorite = currentTrack?.id?.let { it in likedIds } == true,
                                onAlbumClick = { isOnLyricPage.value = "Album" },
                                onToggleFavorite = {
                                    currentTrack?.id?.let { playerViewModel.addToLiked(it) }
                                },
                            )
                            // Real Flamingo-style lyric view, fed from
                            // Lambda's PlayerViewModel via
                            // MediaViewModelObject.lrcEntries.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 4.dp),
                            ) {
                                YosLyricView(
                                    lrcEntriesLambda = { MediaViewModelObject.lrcEntries.value },
                                    liveTimeLambda = { progress.positionMs.toInt() },
                                    mediaEvent = object : YosMediaEvent {
                                        override fun onSeek(position: Int) {
                                            playerViewModel.seekTo(position.toLong())
                                        }
                                    },
                                    translationLambda = { SettingsLibrary.NowPlayingTranslation },
                                    blurLambda = { SettingsLibrary.LyricBlurEffect },
                                    uiConfig = YosUIConfig(noLrcText = "No lyrics available"),
                                    weightLambda = { showControl.value },
                                    onBackClick = {
                                        showControl.value = true
                                        lastClickTime.value = System.currentTimeMillis()
                                    },
                                )
                            }
                        }
                        "PlayingList" -> Column(
                            Modifier
                                .fillMaxSize()
                                .clickable(enabled = false, onClick = {}),
                        ) {
                            PlayingBar(
                                modifier = Modifier,
                                track = currentTrack,
                                isFavorite = currentTrack?.id?.let { it in likedIds } == true,
                                onAlbumClick = { isOnLyricPage.value = "Album" },
                                onToggleFavorite = {
                                    currentTrack?.id?.let { playerViewModel.addToLiked(it) }
                                },
                            )
                            PlayingList(
                                queue = queue,
                                currentTrackId = currentTrack?.id,
                                shuffleEnabled = uiState.shuffleEnabled,
                                repeatMode = repeatMode,
                                onShuffleToggle = { /* not exposed in Lambda */ },
                                onCycleRepeat = { playerViewModel.cycleRepeatMode() },
                                onPlayItem = { playerViewModel.playQueueItem(it) },
                            )
                        }
                    }
                }
            }

            // Music control
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxHeight(0.437f)
                        .fillMaxWidth(),
                ) {
                    if (showControl.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                ),
                        )
                    }

                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        AnimatedVisibility(
                            visible = showControl.value,
                            enter = fadeIn() + expandVertically(
                                expandFrom = Alignment.Top,
                                initialHeight = { (it / 1.4).toInt() },
                            ),
                            exit = fadeOut() + shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                targetHeight = { (it / 1.4).toInt() },
                            ),
                        ) {
                            // Translation toggle (top-right of control area)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                        this.alpha = alphaAnim.value
                                    },
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .overlayEffect()
                                        .alpha(0.4f)
                                        .clickable(
                                            enabled = translationButtonEnabled.value,
                                            onClick = {
                                                Haptics.click(context)
                                                translation.value = !translation.value
                                                showControl.value = true
                                                lastClickTime.value = System.currentTimeMillis()
                                            },
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AnimatedContent(
                                        targetState = translation.value,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    ) { tr ->
                                        Icon(
                                            painterResource(
                                                id = if (tr) R.drawable.ic_nowplaying_translateon
                                                else R.drawable.ic_nowplaying_translate,
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp),
                                        )
                                    }
                                }
                            }

                            PlayerControl(
                                isPlaying = isPlaying,
                                onPrevious = {
                                    Haptics.click(context)
                                    playerViewModel.skipToPrevious()
                                    showControl.value = true
                                    lastClickTime.value = System.currentTimeMillis()
                                },
                                onStatus = { playing ->
                                    if (isPlaying != playing) playerViewModel.togglePlayPause()
                                    showControl.value = true
                                    lastClickTime.value = System.currentTimeMillis()
                                },
                                onNext = {
                                    Haptics.click(context)
                                    playerViewModel.skipToNext()
                                    showControl.value = true
                                    lastClickTime.value = System.currentTimeMillis()
                                },
                                onSeek = { position ->
                                    playerViewModel.seekTo(position.toLong())
                                },
                                onLyrics = {
                                    isOnLyricPage.value =
                                        if (isOnLyricPage.value == "Lyric") "Album" else "Lyric"
                                },
                                onPlaylist = {
                                    isOnLyricPage.value =
                                        if (isOnLyricPage.value == "PlayingList") "Album" else "PlayingList"
                                },
                                nowPage = { isOnLyricPage.value },
                                onSlider = {
                                    showControl.value = true
                                    lastClickTime.value = System.currentTimeMillis()
                                },
                                modifier = Modifier.padding(top = 52.dp),
                                onWhile = { /* not used */ },
                                durationMs = progress.durationMs,
                                positionMs = progress.positionMs,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Album(
    modifier: Modifier = Modifier,
    track: AudioTrack?,
    isPlaying: Boolean,
    onArtClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: (AudioTrack) -> Unit,
) {
    val springSpec: AnimationSpec<Float> = remember("Album_springSpec") {
        SpringSpec(stiffness = 300f, dampingRatio = 1f, visibilityThreshold = 0.001f)
    }
    val tweenSpec: AnimationSpec<Float> = remember("Album_tweenSpec") {
        TweenSpec(durationMillis = 350, easing = EaseOutQuart)
    }
    val scale = animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = if (isPlaying) springSpec else tweenSpec,
        visibilityThreshold = 0.001f,
    )
    Box(
        Modifier
            .weight(1f)
            .padding(top = 20.dp, start = 15.dp, end = 15.dp, bottom = 33.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val dp = (7 + (27 * scale.value)).dp
        ShadowImageWithCache(
            dataLambda = { track?.albumArtUri },
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .padding(start = dp, end = dp, bottom = dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onArtClick,
                ),
            imageQuality = ImageQuality.RAW,
            shadowOverlay = true,
        )
    }
}

@Composable
private fun AlbumPage(
    track: AudioTrack?,
    isPlaying: Boolean,
    onArtClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: (AudioTrack) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false, onClick = {}),
    ) {
        Column(Modifier.fillMaxHeight(0.595f)) {
            Album(
                modifier = Modifier,
                track = track,
                isPlaying = isPlaying,
                onArtClick = onArtClick,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
            )
            AnimatedContent(
                targetState = track,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.padding(horizontal = 32.dp),
            ) { t ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(end = 15.dp),
                    ) {
                        Text(
                            text = t?.title ?: "",
                            fontSize = 19.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = t?.artist ?: "",
                            fontSize = 18.5.sp,
                            modifier = Modifier.overlayEffect(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.35f),
                        )
                    }
                    ActionButtonsRow(
                        track = t,
                        isFavorite = isFavorite,
                        onToggleFavorite = { if (t != null) onToggleFavorite(t) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayingList(
    queue: List<AudioTrack>,
    currentTrackId: Long?,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onShuffleToggle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayItem: (Int) -> Unit,
) {
    val context = LocalContext.current
    val musicList = queue
    Spacer(modifier = Modifier.height(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.545f),
    ) {
        val hide = remember("PlayingList_hide") {
            derivedStateOf { musicList.isEmpty() || shuffleEnabled }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(top = 10.dp)
                .height(65.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.fillMaxWidth().weight(1f)) {
                Text(
                    text = "Up next",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${musicList.size} tracks",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .overlayEffect()
                        .alpha(0.35f),
                )
            }
            Row(modifier = Modifier.overlayEffect().alpha(0.6f)) {
                val dp = 36.dp
                val shuffleBackgroundAlpha =
                    animateFloatAsState(targetValue = if (shuffleEnabled) 0.9f else 0f)
                Box(
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                Haptics.click(context)
                                onShuffleToggle()
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        )
                        .size(36.dp)
                        .background(
                            Color.White.copy(alpha = shuffleBackgroundAlpha.value),
                            shape = dev.shephard.player.ui.theme.YosRoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val shuffleIconTint =
                        animateColorAsState(targetValue = if (shuffleEnabled) Color.Black else Color.White)
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(dp),
                        tint = shuffleIconTint.value,
                    )
                }
                val repeatHighlight = repeatMode == RepeatMode.ALL || repeatMode == RepeatMode.ONE
                val repeatBackgroundAlpha =
                    animateFloatAsState(targetValue = if (repeatHighlight) 0.9f else 0f)
                Box(
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                Haptics.click(context)
                                onCycleRepeat()
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        )
                        .padding(start = 10.dp)
                        .size(36.dp)
                        .background(
                            Color.White.copy(alpha = repeatBackgroundAlpha.value),
                            shape = dev.shephard.player.ui.theme.YosRoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = repeatMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                    ) { mode ->
                        when (mode) {
                            RepeatMode.ONE -> Icon(
                                painterResource(id = R.drawable.ic_nowplaying_repeatone),
                                contentDescription = null,
                                modifier = Modifier.size(dp),
                                tint = animateColorAsState(
                                    targetValue = if (repeatHighlight) Color.Black else Color.White,
                                ).value,
                            )
                            else -> Icon(
                                painterResource(id = R.drawable.ic_nowplaying_repeat),
                                contentDescription = null,
                                modifier = Modifier.size(dp),
                                tint = animateColorAsState(
                                    targetValue = if (repeatHighlight) Color.Black else Color.White,
                                ).value,
                            )
                        }
                    }
                }
            }
        }

        if (hide.value) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_uitabbar_library),
                    contentDescription = null,
                    modifier = Modifier
                        .overlayEffect()
                        .size(70.dp)
                        .alpha(0.6f),
                )
                Text(
                    text = "No items to play",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
                )
                Text(
                    text = if (musicList.isEmpty()) "Queue is empty." else "Currently in shuffle play mode.",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier
                        .overlayEffect()
                        .alpha(0.4f),
                )
            }
        } else {
            val musicIndex = remember(musicList, currentTrackId) {
                musicList.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
            }
            val scope = rememberCoroutineScope()
            val state = rememberLazyListState(
                initialFirstVisibleItemIndex = musicIndex + 1,
                initialFirstVisibleItemScrollOffset = -15,
            )
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                val colors = listOf(
                                    Color.Transparent,
                                    Color.Black, Color.Black, Color.Black, Color.Black,
                                    Color.Black, Color.Black, Color.Black, Color.Black,
                                    Color.Black, Color.Black, Color.Black, Color.Black,
                                    Color.Black, Color.Black, Color.Transparent,
                                )
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(colors),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        },
                ) {
                    item("blank_before") { Spacer(modifier = Modifier.height(12.dp)) }
                    items(musicList, key = { it.id }) { music ->
                        SmallMusicListItem(music) {
                            val index = musicList.indexOf(music)
                            if (index >= 0) onPlayItem(index)
                        }
                    }
                    item("blank_after") { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SmallMusicListItem(music: AudioTrack, itemClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .clickable { itemClick() }
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShadowImageWithCache(
            dataLambda = { music.albumArtUri },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            cornerRadius = 4.dp,
            shadowAlpha = 0f,
            imageQuality = ImageQuality.LOW,
        )
        Column(Modifier.padding(start = 14.dp)) {
            Text(
                text = music.title,
                modifier = Modifier.padding(bottom = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                lineHeight = 16.sp,
            )
            Text(
                text = music.artist,
                modifier = Modifier.alpha(0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.5.sp,
                lineHeight = 11.5.sp,
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    track: AudioTrack?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.overlayEffect(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dp = 28.dp
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        if (track != null) {
                            Haptics.click(context)
                            onToggleFavorite()
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .size(dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isFavorite,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { fav ->
                Icon(
                    painterResource(
                        id = if (fav) R.drawable.ic_nowplaying_favorited
                        else R.drawable.ic_nowplaying_favorite,
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dp)
                        .let { if (!fav) it.overlayEffect() else it },
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        // "More" placeholder (no-op)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 90f
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .size(dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = false,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { _ ->
                Icon(
                    painterResource(id = R.drawable.ic_nowplaying_more),
                    contentDescription = null,
                    modifier = Modifier
                        .size(dp)
                        .overlayEffect(),
                )
            }
        }
    }
}

@Composable
private fun PlayingBar(
    modifier: Modifier,
    track: AudioTrack?,
    isFavorite: Boolean,
    onAlbumClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) = YosWrapper {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.5.dp)
            .padding(top = 22.dp)
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShadowImageWithCache(
            dataLambda = { track?.albumArtUri },
            contentDescription = null,
            modifier = modifier
                .size(69.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onAlbumClick,
                ),
            cornerRadius = 5.dp,
            imageQuality = ImageQuality.LOW,
            shadowType = ShadowType.Small,
            shadowOverlay = true,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 15.dp),
        ) {
            Text(
                text = track?.title ?: "",
                fontSize = 16.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.5.sp,
            )
            Text(
                text = track?.artist ?: "",
                fontSize = 15.sp,
                modifier = Modifier.overlayEffect(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.35f),
            )
        }
        ActionButtonsRow(
            track = track,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControl(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onStatus: (Boolean) -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyrics: () -> Unit,
    onPlaylist: () -> Unit,
    nowPage: () -> String,
    onSlider: () -> Unit,
    onWhile: suspend () -> Unit,
    modifier: Modifier,
    durationMs: Long,
    positionMs: Long,
) {
    val playingDuration = rememberSaveable(key = "PlayerControl_playingDuration") {
        mutableLongStateOf(0L)
    }
    val playingPosition = rememberSaveable(key = "PlayerControl_playingPosition") {
        mutableLongStateOf(0L)
    }
    val context = LocalContext.current
    val playedTime = rememberSaveable(key = "PlayerControl_playedTime") { mutableStateOf("0:00") }
    val remainingTime = rememberSaveable(key = "PlayerControl_remainingTime") { mutableStateOf("-0:00") }
    val sliderPosition = remember("PlayerControl_sliderPosition") { mutableFloatStateOf(0f) }
    val isSliding = remember("PlayerControl_isSliding") { mutableStateOf(false) }

    LaunchedEffect(durationMs, positionMs) {
        if (!isSliding.value) {
            playingDuration.longValue = durationMs
            playingPosition.longValue = positionMs
            if (durationMs > 0L) {
                sliderPosition.floatValue = positionMs.coerceAtLeast(0).toFloat()
                val totalSeconds = positionMs.coerceAtLeast(0) / 1000
                playedTime.value = formatTimeSeconds(totalSeconds)
                val remainingSeconds = durationMs.coerceAtLeast(0) / 1000 - totalSeconds
                remainingTime.value = "-${formatTimeSeconds(remainingSeconds)}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
            .padding(bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Polling loop kept simple by reading from PlayerViewModel's flow above
        val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
        LaunchedEffect(Unit) {
            var lastPosition = 0L
            while (true) {
                if (lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)) {
                    onWhile()
                }
                delay(700)
            }
        }

        // Slider
        Slider(
            value = sliderPosition.floatValue,
            onValueChange = { newValue ->
                isSliding.value = true
                sliderPosition.floatValue = newValue
                val newTotalSeconds = newValue.toLong() / 1000
                playedTime.value = formatTimeSeconds(newTotalSeconds)
                val newRemainingSeconds = playingDuration.longValue / 1000 - newTotalSeconds
                remainingTime.value = "-${formatTimeSeconds(newRemainingSeconds)}"
                onSlider()
            },
            onValueChangeFinished = {
                Haptics.longClick(context)
                onSeek(sliderPosition.floatValue)
                isSliding.value = false
            },
            valueRange = 0f..playingDuration.longValue.toFloat().coerceAtLeast(0f),
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0x0DFFFFFF),
            ),
            modifier = Modifier
                .overlayEffect()
                .alpha(0.45f)
                .height(14.dp),
            thumb = {},
            track = {
                Track(
                    sliderPositions = SliderPositions(
                        initialActiveRange = if (playingDuration.longValue > 0)
                            0f..(sliderPosition.floatValue / playingDuration.longValue)
                        else 0f..0f,
                    ),
                    height = 7.dp,
                )
            },
        )

        // Time labels + quality indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 7.dp)
                .height(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = playedTime.value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.overlayEffect(),
                )
                Text(
                    text = remainingTime.value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.overlayEffect(),
                )
            }
            MusicQualityIndicator()
        }

        // Control buttons
        Box(
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(61.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = {
                                Haptics.click(context)
                                onPrevious()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_rewind),
                        contentDescription = "Previous",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                    )
                }
                Spacer(modifier = Modifier.width(43.dp))
                Box(
                    modifier = Modifier
                        .size(58.5.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = {
                                Haptics.click(context)
                                onStatus(!isPlaying)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(
                                scaleOut(targetScale = 0.3f) + fadeOut(),
                            )
                        },
                    ) { playing ->
                        Icon(
                            painterResource(
                                id = if (playing) R.drawable.ic_nowplaying_pause
                                else R.drawable.ic_nowplaying_play,
                            ),
                            contentDescription = if (playing) "Pause" else "Play",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (playing) 10.dp else 9.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(43.dp))
                Box(
                    modifier = Modifier
                        .size(61.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = {
                                Haptics.click(context)
                                onNext()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_fforward),
                        contentDescription = "Next",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                    )
                }
            }
        }

        // Bottom row: lyrics / airplay / queue
        Row(
            modifier = Modifier
                .overlayEffect()
                .fillMaxWidth()
                .alpha(0.4f),
            horizontalArrangement = Arrangement.Center,
        ) {
            val dp = 32.dp
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f)
                    .clickable(
                        onClick = { onLyrics() },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = nowPage() == "Lyric",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { onLyric ->
                    Icon(
                        painterResource(
                            id = if (onLyric) R.drawable.ic_nowplaying_lyricson
                            else R.drawable.ic_nowplaying_lyrics,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.1f))
            AirPlay()
            Spacer(modifier = Modifier.weight(0.1f))
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f)
                    .clickable(
                        onClick = { onPlaylist() },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = nowPage() == "PlayingList",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { onQueue ->
                    Icon(
                        painterResource(
                            id = if (onQueue) R.drawable.ic_nowplaying_queueon
                            else R.drawable.ic_nowplaying_queue,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSlider_Unused() {
    // Removed from the Lambda Player build. The original volume slider
    // helper was tied to a private BroadcastReceiver that lived in the
    // `yos.music.player.code` package; rather than porting that whole
    // subsystem we just rely on the system media volume keys for
    // volume control in the now-playing sheet. The function is kept
    // as a stub to make the diff against the imported Flamingo source
    // easier to review.
}

@Composable
private fun Track(
    sliderPositions: SliderPositions,
    modifier: Modifier = Modifier,
    height: Dp,
) = YosWrapper {
    val inactiveTrackColor = Color.White.copy(alpha = 0.5f)
    val activeTrackColor = Color.White
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = height.toPx()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(0f, center.y)
        val sliderRight = Offset(size.width, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        drawLine(inactiveTrackColor, sliderStart, sliderEnd, trackStrokeWidth, StrokeCap.Round)
        val sliderValueEnd = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.endInclusive,
            center.y,
        )
        val sliderValueStart = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.start,
            center.y,
        )
        drawLine(activeTrackColor, sliderValueStart, sliderValueEnd, trackStrokeWidth, StrokeCap.Round)
        sliderPositions.tickFractions.groupBy {
            it > sliderPositions.activeRange.endInclusive ||
                it < sliderPositions.activeRange.start
        }.forEach { (_, list) ->
            drawPoints(
                list.fastMap { Offset(lerp(sliderStart, sliderEnd, it).x, center.y) },
                PointMode.Points,
                inactiveTrackColor,
                tickSize,
                StrokeCap.Round,
            )
        }
    }
}
