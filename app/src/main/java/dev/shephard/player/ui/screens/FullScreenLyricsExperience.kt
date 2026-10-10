// SPDX-License-Identifier: GPL-3.0-only
// Full-screen, shared lyrics experience for Material 3 and Miuix Now Playing.
@file:OptIn(androidx.activity.ExperimentalActivityApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.res.painterResource
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.components.MinimalSeekBar
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.m3.M3WavySlider
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.player.LyricsSearchResult
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.SyncedLyricLine
import dev.shephard.player.theme.PredictiveBackAnimation
import dev.shephard.player.ui.util.rememberDeviceCornerRadius
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity

private val LocalLyricsForeground = staticCompositionLocalOf { Color.White }

@Composable
fun FullScreenLyricsExperience(
    playerViewModel: PlayerViewModel,
    useMiuix: Boolean,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val state by playerViewModel.uiState.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val searchState by playerViewModel.lyricsSearchState.collectAsState()
    val syncOffsetMs by playerViewModel.lyricsSyncOffsetMs.collectAsState()
    val isRemixed by playerViewModel.isRemixed.collectAsState()
    val context = LocalContext.current
    val preferences = remember(context) { PreferencesManager(context) }
    val predictiveBack by preferences.predictiveBackAnimation.collectAsState(initial = PredictiveBackAnimation.MIUIX)
    val useClassicPredictiveBack = predictiveBack == PredictiveBackAnimation.CLASSIC
    val blurEnabled by preferences.lyricsBackgroundBlurEnabled.collectAsState(initial = true)
    val savedBlurStrength by preferences.lyricsBackgroundBlurStrength.collectAsState(initial = 36f)
    val autoScroll by preferences.lyricsAutoScrollEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val lyricsListState = rememberLazyListState()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val searchListState = rememberLazyListState()

    val track = state.currentTrack
    val primary = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val textStyle = if (useMiuix) MiuixAppTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium
    val titleStyle = if (useMiuix) MiuixAppTheme.typography.titleLarge else MaterialTheme.typography.titleLarge
    val coverTint = Color(state.glowColorArgb)
    val m3LyricsBackground by animateColorAsState(
        targetValue = lerp(MaterialTheme.colorScheme.surface, coverTint, 0.34f),
        animationSpec = tween(520),
        label = "m3LyricsCoverTint",
    )
    val lyricsBackground = if (useMiuix) Color(0xFF111318) else m3LyricsBackground
    val lyricsForeground = if (useMiuix || lyricsBackground.luminance() < 0.52f) Color.White else Color(0xFF17131A)
    var blurStrength by remember { mutableFloatStateOf(36f) }
    LaunchedEffect(savedBlurStrength) { blurStrength = savedBlurStrength.coerceIn(0f, 64f) }

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var searchQuery by remember(track?.id) {
        mutableStateOf(track?.title.orEmpty())
    }
    var seekFraction by remember { mutableStateOf<Float?>(null) }

    val synced = state.syncedLyrics
    // The player reports its position every 500 ms. Lines (and the karaoke sweep) follow a
    // frame-accurate position extrapolated between those reports instead, so a line lights up
    // on time and word colouring moves smoothly.
    val smoothPositionMs = rememberSmoothLyricsPosition(
        positionMs = progress.positionMs,
        isPlaying = state.isPlaying,
        enabled = synced.isNotEmpty(),
    )
    val activeIndex by remember(synced, syncOffsetMs) {
        derivedStateOf {
            if (synced.isNotEmpty()) synced.indexOfLast { it.timeMs + syncOffsetMs <= smoothPositionMs.longValue } else -1
        }
    }
    val durationMs = progress.durationMs.coerceAtLeast(0L)
    val displayedFraction = seekFraction ?: if (durationMs > 0L) {
        (progress.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Plain LRC for the editor (word tags would be noise there). Saving an unchanged text is
    // skipped so opening the editor never throws away word-by-word timings.
    val editSource = remember(state.lyrics, synced) {
        if (synced.isNotEmpty()) {
            synced.joinToString("\n") { line -> "[${formatLrcTime(line.timeMs)}]${line.text}" }
        } else state.lyrics.joinToString("\n")
    }
    LaunchedEffect(track?.id, editSource, isEditing) {
        if (!isEditing) editText = editSource
    }

    LaunchedEffect(track?.id) {
        searchQuery = track?.title.orEmpty()
        playerViewModel.clearLyricsSearch()
    }

    // "Online first" lyrics mode: the first time lyrics are opened for a track, go straight to online results.
    // Declared after the reset above so the search it starts is not cleared.
    LaunchedEffect(track?.id) {
        if (track != null && playerViewModel.consumeFirstLyricsOpenSearch()) {
            showSearch = true
            playerViewModel.searchLyrics(track.title)
        }
    }

    LaunchedEffect(activeIndex, autoScroll, showSearch, showSettings, isEditing, track?.id) {
        if (autoScroll && !showSearch && !isEditing && synced.isNotEmpty() && activeIndex >= 0) {
            val layoutInfo = lyricsListState.layoutInfo
            val viewportHeight = layoutInfo.viewportSize.height
            val targetIndex = activeIndex.coerceIn(synced.indices)
            // Every line is laid out at the active size and only *scaled* down when inactive, so
            // line heights never change. The landing position is therefore known exactly before
            // the move starts, and the scroll and the grow/shrink run as one motion with no
            // correction (no few-dp jump) at the end.
            val visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            val itemHeight = visible?.size ?: run {
                val textWidthPx = (layoutInfo.viewportSize.width - with(density) { (LyricLineHorizontalPadding * 2).toPx() })
                    .toInt().coerceAtLeast(1)
                textMeasurer.measure(
                    text = synced[targetIndex].text,
                    style = lyricLineLayoutStyle(textStyle),
                    constraints = Constraints(maxWidth = textWidthPx),
                ).size.height + with(density) { (LyricLineVerticalPadding * 2).toPx() }.toInt()
            }
            // Item offsets start after the top content padding; aim the line's centre at the anchor.
            val desiredOffset = viewportHeight * LyricAnchorFraction - itemHeight / 2f - layoutInfo.beforeContentPadding
            if (visible != null) {
                lyricsListState.animateScrollBy(
                    value = visible.offset - desiredOffset,
                    animationSpec = tween(LyricMoveDurationMs, easing = FastOutSlowInEasing),
                )
            } else {
                lyricsListState.animateScrollToItem(index = targetIndex, scrollOffset = -desiredOffset.toInt())
            }
        }
    }

    LaunchedEffect(progress.positionMs, durationMs) {
        if (seekFraction != null && durationMs > 0L) {
            val heldMs = (seekFraction!! * durationMs).toLong()
            if (abs(heldMs - progress.positionMs) < 1_200L) seekFraction = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val raw = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!raw.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { playerViewModel.setManualLyricsText(raw) }
                }
            }
        }
    }

    // Exported as enhanced LRC when the lyrics are word-synced, so karaoke timings survive.
    val exportText = remember(state.lyrics, synced) {
        if (synced.isNotEmpty()) {
            synced.joinToString("\n") { line -> line.toLrcLine() }
        } else state.lyrics.joinToString("\n")
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && exportText.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(exportText)
                    }
                }
            }
        }
    }

    val backProgress = remember { Animatable(0f) }
    var backEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    if (useClassicPredictiveBack) {
        PredictiveBackHandler(enabled = true) { events ->
            try {
                events.collect { event ->
                    backEdge = event.swipeEdge
                    backProgress.snapTo(event.progress.coerceIn(0f, 1f))
                }
                if (backProgress.value < 1f) backProgress.animateTo(1f, tween(150))
                onDismiss()
            } catch (cancelled: CancellationException) {
                backProgress.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 380f))
                throw cancelled
            }
        }
    }
    val deviceCornerRadius = rememberDeviceCornerRadius()

    // decorFitsSystemWindows = false lets the lyrics background draw behind the status bar.
    // The content already applies statusBarsPadding()/navigationBarsPadding(), so the header
    // is laid out in the status bar's inset area instead of starting below it.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        CompositionLocalProvider(LocalLyricsForeground provides lyricsForeground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .graphicsLayer {
                        val p = backProgress.value.coerceIn(0f, 1f)
                        val direction = if (backEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                        translationX = size.width * 0.12f * p * direction
                        scaleX = 1f - 0.035f * p
                        scaleY = 1f - 0.035f * p
                        alpha = 1f - 0.12f * p
                        shape = RoundedCornerShape(deviceCornerRadius * p)
                        clip = p > 0.001f
                    }
            ) {
                if (useMiuix) {
                    Crossfade(
                        targetState = track?.albumArtUri,
                        animationSpec = tween(durationMillis = 650),
                        label = "lyricsBackdropCrossfade",
                        modifier = Modifier.fillMaxSize(),
                    ) { artUri ->
                        if (artUri != null && blurEnabled && blurStrength > 0f) {
                            AsyncImage(
                                model = artUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                                    .blur(blurStrength.dp),
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.2f) }),
                            )
                        } else if (artUri != null) {
                            AsyncImage(
                                model = artUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Color(0xFF111318)))
                        }
                    }
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.58f), Color.Black.copy(alpha = 0.82f))
                            )
                        )
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(lyricsBackground))
                }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            LyricsHeader(
                title = strings.lyrics,
                subtitle = track?.let { "${it.title} · ${it.artist}" }.orEmpty(),
                titleStyle = titleStyle,
                bodyStyle = textStyle,
                primary = primary,
                onClose = onDismiss,
                onSearch = {
                    showSettings = false
                    showSearch = !showSearch
                },
                onImport = { importLauncher.launch(arrayOf("text/*", "application/octet-stream", "application/x-lrc")) },
                onEdit = {
                    if (isEditing) {
                        if (editText != editSource) playerViewModel.setManualLyricsText(editText)
                        isEditing = false
                    } else {
                        showSearch = false
                        isEditing = true
                    }
                },
                isEditing = isEditing,
                onSettings = { showSettings = !showSettings; showSearch = false },
            )

            if (showSettings) {
                LyricsSettingsPanel(
                    useMiuix = useMiuix,
                    primary = primary,
                    textStyle = textStyle,
                    blurEnabled = blurEnabled,
                    blurStrength = blurStrength,
                    autoScroll = autoScroll,
                    syncOffsetMs = syncOffsetMs,
                    onBlurEnabledChange = { enabled -> scope.launch { preferences.setLyricsBackgroundBlurEnabled(enabled) } },
                    onBlurStrengthChange = { blurStrength = it },
                    onBlurStrengthChangeFinished = { scope.launch { preferences.setLyricsBackgroundBlurStrength(blurStrength) } },
                    onAutoScrollChange = { enabled -> scope.launch { preferences.setLyricsAutoScrollEnabled(enabled) } },
                    onOffsetChange = playerViewModel::setLyricsSyncOffset,
                    onExport = {
                        val safeTitle = track?.title?.replace(Regex("[^A-Za-z0-9._ -]"), "_")?.ifBlank { "lyrics" } ?: "lyrics"
                        exportLauncher.launch("$safeTitle.lrc")
                    },
                    exportEnabled = exportText.isNotBlank(),
                    strings = strings,
                )
            }

            when {
                showSearch -> {
                    LyricsSearchPanel(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        isLoading = searchState.isLoading,
                        results = searchState.results,
                        error = searchState.error,
                        primary = primary,
                        textStyle = textStyle,
                        listState = searchListState,
                        onSearch = { playerViewModel.searchLyrics(searchQuery) },
                        onSelect = { result ->
                            playerViewModel.selectLyricsSearchResult(result)
                            showSearch = false
                        },
                        strings = strings,
                    )
                }
                isEditing -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(LocalLyricsForeground.current.copy(alpha = 0.10f))
                            .padding(14.dp),
                    ) {
                        BasicTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = textStyle.copy(color = LocalLyricsForeground.current, fontSize = 16.sp, lineHeight = 25.sp),
                            cursorBrush = SolidColor(primary),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxSize()) {
                                    if (editText.isBlank()) {
                                        BasicText(
                                            text = strings.lyrics,
                                            style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.55f)),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }
                state.lyrics.isEmpty() -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.Lyrics, contentDescription = null, tint = LocalLyricsForeground.current.copy(alpha = 0.72f), modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(14.dp))
                        BasicText(strings.noLyricsFound, style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.78f)))
                        Spacer(Modifier.height(16.dp))
                        LyricsPill(
                            label = strings.downloadLyrics,
                            primary = primary,
                            onClick = {
                                showSearch = true
                                playerViewModel.searchLyrics(searchQuery.ifBlank { track?.title.orEmpty() })
                            },
                        )
                        if (searchState.isLoading) {
                            Spacer(Modifier.height(14.dp))
                            CircularProgressIndicator(color = primary, modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = lyricsListState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
                        contentPadding = PaddingValues(top = 28.dp, bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(if (synced.isNotEmpty()) 0.dp else 6.dp),
                    ) {
                        if (synced.isNotEmpty()) {
                            itemsIndexed(synced, key = { index, line -> "${line.timeMs}_$index" }) { index, line ->
                                val isActive = index == activeIndex
                                val distance = if (activeIndex < 0) 0 else abs(index - activeIndex)
                                val lineBlur = if (blurEnabled && activeIndex >= 0) {
                                    min(10f, distance * (blurStrength / 24f)).dp
                                } else 0.dp
                                // Grow/shrink is a pure scale (layout size is fixed), timed and eased
                                // exactly like the scroll so the line lands at full size in place.
                                val activeAmount by animateFloatAsState(
                                    targetValue = if (isActive) 1f else 0f,
                                    animationSpec = tween(LyricMoveDurationMs, easing = FastOutSlowInEasing),
                                    label = "lyricLineSize",
                                )
                                val animatedBlur by animateDpAsState(
                                    targetValue = lineBlur,
                                    animationSpec = tween(LyricMoveDurationMs),
                                    label = "lyricLineBlur",
                                )
                                val foreground = LocalLyricsForeground.current
                                val karaoke = isActive && line.words.isNotEmpty()
                                val targetColor = when {
                                    // Word-synced: the unsung part stays dim, sung words are painted over it.
                                    karaoke -> foreground.copy(alpha = 0.45f)
                                    isActive -> foreground
                                    distance > 3 -> foreground.copy(alpha = 0.40f)
                                    else -> foreground.copy(alpha = 0.72f)
                                }
                                val animatedColor by animateColorAsState(
                                    targetValue = targetColor,
                                    animationSpec = tween(LyricMoveDurationMs),
                                    label = "lyricLineColor",
                                )
                                var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                                BasicText(
                                    text = line.text,
                                    onTextLayout = { textLayout = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { playerViewModel.seekTo((line.timeMs + syncOffsetMs).coerceAtLeast(0)) }
                                        .graphicsLayer {
                                            val scale = LyricInactiveScale + (1f - LyricInactiveScale) * activeAmount
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .blur(animatedBlur)
                                        .padding(horizontal = LyricLineHorizontalPadding, vertical = LyricLineVerticalPadding)
                                        .drawWithContent {
                                            drawContent()
                                            if (karaoke) {
                                                textLayout?.let { layout ->
                                                    drawKaraokeHighlight(
                                                        layout = layout,
                                                        line = line,
                                                        positionMs = smoothPositionMs.longValue - syncOffsetMs,
                                                        color = foreground,
                                                    )
                                                }
                                            }
                                        },
                                    style = lyricLineLayoutStyle(textStyle).copy(color = animatedColor),
                                )
                            }
                        } else {
                            itemsIndexed(state.lyrics, key = { index, line -> "plain_${index}_$line" }) { _, line ->
                                BasicText(
                                    text = line,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                                    style = textStyle.copy(
                                        color = LocalLyricsForeground.current.copy(alpha = 0.86f),
                                        fontSize = 19.sp,
                                        lineHeight = 28.sp,
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            LyricsPlaybackControls(
                track = track,
                isPlaying = state.isPlaying,
                progressMs = progress.positionMs,
                durationMs = durationMs,
                seekFraction = displayedFraction,
                primary = primary,
                textStyle = textStyle,
                onSeekChange = { fraction ->
                    seekFraction = fraction
                    if (durationMs > 0L) playerViewModel.onSeekPreview((fraction * durationMs).toLong())
                },
                onSeekFinished = {
                    val fraction = seekFraction ?: displayedFraction
                    if (durationMs > 0L) playerViewModel.onSeekCommit((fraction * durationMs).toLong())
                },
                useMiuix = useMiuix,
                // Miuix's sheet uses "remix" in the first slot, M3's sheet uses "shuffle".
                shuffleActive = if (useMiuix) isRemixed else state.shuffleEnabled,
                onShuffle = if (useMiuix) playerViewModel::remixQueue else playerViewModel::toggleShuffle,
                repeatMode = state.repeatMode,
                onRepeat = playerViewModel::cycleRepeatMode,
                onPrevious = playerViewModel::skipToPrevious,
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::skipToNext,
                strings = strings,
            )
        }
            }
        }
    }
}

@Composable
private fun LyricsHeader(
    title: String,
    subtitle: String,
    titleStyle: TextStyle,
    bodyStyle: TextStyle,
    primary: Color,
    onClose: () -> Unit,
    onSearch: () -> Unit,
    onImport: () -> Unit,
    onEdit: () -> Unit,
    isEditing: Boolean,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LyricsIconButton(Icons.AutoMirrored.Filled.ArrowBack, title, onClose)
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 4.dp)) {
            BasicText(title, style = titleStyle.copy(color = LocalLyricsForeground.current, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(subtitle, style = bodyStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.70f), fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LyricsIconButton(Icons.Filled.Search, "Search lyrics", onSearch)
        LyricsIconButton(Icons.Filled.FolderOpen, "Import lyrics", onImport)
        LyricsIconButton(if (isEditing) Icons.Filled.Check else Icons.Filled.Edit, if (isEditing) "Save lyrics" else "Edit lyrics", onEdit, selected = isEditing, primary = primary)
        LyricsIconButton(Icons.Filled.Tune, "Lyrics settings", onSettings, selected = false, primary = primary)
    }
}

@Composable
private fun LyricsSettingsPanel(
    useMiuix: Boolean,
    primary: Color,
    textStyle: TextStyle,
    blurEnabled: Boolean,
    blurStrength: Float,
    autoScroll: Boolean,
    syncOffsetMs: Int,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBlurStrengthChange: (Float) -> Unit,
    onBlurStrengthChangeFinished: () -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onOffsetChange: (Int) -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    if (useMiuix) {
        MiuixLyricsSettingsPanel(
            primary = primary,
            textStyle = textStyle,
            blurEnabled = blurEnabled,
            blurStrength = blurStrength,
            autoScroll = autoScroll,
            syncOffsetMs = syncOffsetMs,
            onBlurEnabledChange = onBlurEnabledChange,
            onBlurStrengthChange = onBlurStrengthChange,
            onBlurStrengthChangeFinished = onBlurStrengthChangeFinished,
            onAutoScrollChange = onAutoScrollChange,
            onOffsetChange = onOffsetChange,
            onExport = onExport,
            exportEnabled = exportEnabled,
            strings = strings,
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(LocalLyricsForeground.current.copy(alpha = 0.11f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LyricsPill(
                label = "${strings.blurEffect}: ${if (blurEnabled) "On" else "Off"}",
                primary = primary,
                selected = blurEnabled,
                onClick = { onBlurEnabledChange(!blurEnabled) },
            )
            LyricsPill(
                label = "${strings.autoscroll}: ${if (autoScroll) "On" else "Off"}",
                primary = primary,
                selected = autoScroll,
                onClick = { onAutoScrollChange(!autoScroll) },
            )
            Spacer(Modifier.weight(1f))
            LyricsIconButton(Icons.Filled.FileDownload, "Export lyrics", onExport, enabled = exportEnabled)
        }
        BasicText(
            "${strings.blurEffect} · ${blurStrength.toInt()} dp",
            style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.75f), fontSize = 12.sp),
        )
        Slider(
            value = blurStrength,
            onValueChange = onBlurStrengthChange,
            onValueChangeFinished = onBlurStrengthChangeFinished,
            valueRange = 0f..64f,
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
                inactiveTrackColor = LocalLyricsForeground.current.copy(alpha = 0.22f),
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText("${strings.lyricsSyncOffset}: ${if (syncOffsetMs >= 0) "+" else ""}${syncOffsetMs} ms", style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.82f), fontSize = 12.sp), modifier = Modifier.weight(1f))
            LyricsPill("−100", primary, onClick = { onOffsetChange(syncOffsetMs - 100) })
            LyricsPill("+100", primary, onClick = { onOffsetChange(syncOffsetMs + 100) })
        }
    }
}

/**
 * Miuix rendering of the lyrics options: Miuix switches, slider and buttons instead of the
 * Material pills/slider. The lyrics screen is always dark in Miuix (#111318 + white text), so
 * the "off" colours are set for a dark surface regardless of the app's light/dark mode.
 */
@Composable
private fun MiuixLyricsSettingsPanel(
    primary: Color,
    textStyle: TextStyle,
    blurEnabled: Boolean,
    blurStrength: Float,
    autoScroll: Boolean,
    syncOffsetMs: Int,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBlurStrengthChange: (Float) -> Unit,
    onBlurStrengthChangeFinished: () -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onOffsetChange: (Int) -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    val foreground = LocalLyricsForeground.current
    val labelStyle = textStyle.copy(color = foreground.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val switchColors = top.yukonga.miuix.kmp.basic.SwitchDefaults.switchColors(
        checkedTrackColor = primary,
        checkedThumbColor = Color.White,
        uncheckedTrackColor = foreground.copy(alpha = 0.20f),
        uncheckedThumbColor = Color.White,
    )
    val buttonColors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
        color = foreground.copy(alpha = 0.12f),
        disabledColor = foreground.copy(alpha = 0.06f),
        textColor = foreground,
        disabledTextColor = foreground.copy(alpha = 0.35f),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(foreground.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(strings.blurEffect, style = labelStyle, modifier = Modifier.weight(1f))
            top.yukonga.miuix.kmp.basic.Switch(
                checked = blurEnabled,
                onCheckedChange = onBlurEnabledChange,
                colors = switchColors,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(strings.autoscroll, style = labelStyle, modifier = Modifier.weight(1f))
            top.yukonga.miuix.kmp.basic.Switch(
                checked = autoScroll,
                onCheckedChange = onAutoScrollChange,
                colors = switchColors,
            )
        }
        BasicText(
            "${strings.blurEffect} · ${blurStrength.toInt()} dp",
            style = textStyle.copy(color = foreground.copy(alpha = 0.75f), fontSize = 12.sp),
        )
        top.yukonga.miuix.kmp.basic.Slider(
            value = blurStrength,
            onValueChange = onBlurStrengthChange,
            onValueChangeFinished = onBlurStrengthChangeFinished,
            valueRange = 0f..64f,
            enabled = blurEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = top.yukonga.miuix.kmp.basic.SliderDefaults.sliderColors(
                foregroundColor = primary,
                disabledForegroundColor = foreground.copy(alpha = 0.30f),
                backgroundColor = foreground.copy(alpha = 0.16f),
                disabledBackgroundColor = foreground.copy(alpha = 0.08f),
                thumbColor = Color.White,
                disabledThumbColor = foreground.copy(alpha = 0.55f),
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                "${strings.lyricsSyncOffset}: ${if (syncOffsetMs >= 0) "+" else ""}${syncOffsetMs} ms",
                style = textStyle.copy(color = foreground.copy(alpha = 0.82f), fontSize = 13.sp),
                modifier = Modifier.weight(1f),
            )
            top.yukonga.miuix.kmp.basic.TextButton(
                text = "−100",
                onClick = { onOffsetChange(syncOffsetMs - 100) },
                colors = buttonColors,
                minHeight = 36.dp,
            )
            top.yukonga.miuix.kmp.basic.TextButton(
                text = "+100",
                onClick = { onOffsetChange(syncOffsetMs + 100) },
                colors = buttonColors,
                minHeight = 36.dp,
            )
            top.yukonga.miuix.kmp.basic.IconButton(
                onClick = onExport,
                enabled = exportEnabled,
                backgroundColor = foreground.copy(alpha = 0.12f),
                minHeight = 36.dp,
                minWidth = 36.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = "Export lyrics",
                    tint = if (exportEnabled) foreground else foreground.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun LyricsSearchPanel(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    results: List<LyricsSearchResult>,
    error: String?,
    primary: Color,
    textStyle: TextStyle,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSearch: () -> Unit,
    onSelect: (LyricsSearchResult) -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(LocalLyricsForeground.current.copy(alpha = 0.12f))
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                if (query.isBlank()) BasicText(strings.lyricsSearchPrompt, style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.5f)))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = textStyle.copy(color = LocalLyricsForeground.current, fontSize = 16.sp),
                    cursorBrush = SolidColor(primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
            }
            LyricsIconButton(Icons.Filled.Search, strings.search, onSearch, selected = true, primary = primary)
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primary, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(20.dp), contentAlignment = Alignment.Center) {
                BasicText(
                    text = if (error == "network") strings.lyricsSearchFailed else strings.noSearchResults,
                    style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.76f), textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                contentPadding = PaddingValues(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { result -> result.sourceId ?: "${result.source}_${result.id ?: (result.title + result.artist).hashCode()}" }) { result ->
                    LyricsSearchResultCard(result = result, primary = primary, textStyle = textStyle, onClick = { onSelect(result) })
                }
            }
        }
    }
}

@Composable
private fun LyricsSearchResultCard(
    result: LyricsSearchResult,
    primary: Color,
    textStyle: TextStyle,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LocalLyricsForeground.current.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(result.title, style = textStyle.copy(color = LocalLyricsForeground.current, fontWeight = FontWeight.SemiBold, fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(
            listOfNotNull(result.artist.takeIf(String::isNotBlank), result.album.takeIf(String::isNotBlank)).joinToString(" · "),
            style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.70f), fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                text = result.durationSeconds?.let { formatLyricsDuration((it * 1000.0).toLong()) } ?: "",
                style = textStyle.copy(color = LocalLyricsForeground.current.copy(alpha = 0.60f), fontSize = 12.sp),
            )
            LyricsPill(result.source, primary, selected = result.source == "QQ Music", onClick = onClick)
            if (result.syncedLyrics != null) LyricsPill("Synced", primary, selected = true, onClick = onClick)
            if (result.instrumental) LyricsPill("Instrumental", primary, selected = false, onClick = onClick)
        }
    }
}

@Composable
private fun LyricsPlaybackControls(
    track: AudioTrack?,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    seekFraction: Float,
    primary: Color,
    textStyle: TextStyle,
    useMiuix: Boolean,
    shuffleActive: Boolean,
    repeatMode: RepeatMode,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    val foreground = LocalLyricsForeground.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)) {
        if (useMiuix) {
            // Same progress bar as the MIUIX Now Playing sheet.
            MinimalSeekBar(
                progress = seekFraction,
                onSeekPreview = onSeekChange,
                onSeekFinished = { fraction ->
                    onSeekChange(fraction)
                    onSeekFinished()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                trackHeight = 6.dp,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.3f),
            )
        } else {
            // Same wavy progress slider as the M3 Now Playing sheet.
            M3WavySlider(
                value = { seekFraction },
                onValueChange = onSeekChange,
                onValueCommit = { fraction ->
                    onSeekChange(fraction)
                    onSeekFinished()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                enabled = durationMs > 0L,
                isPlaying = isPlaying,
                activeTrackColor = primary,
                inactiveTrackColor = foreground.copy(alpha = 0.25f),
                thumbColor = primary,
                semanticsLabel = strings.playbackPosition,
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(formatLyricsDuration(if (durationMs > 0L) (seekFraction * durationMs).toLong() else progressMs), style = textStyle.copy(color = foreground.copy(alpha = 0.68f), fontSize = 12.sp))
            BasicText(formatLyricsDuration(durationMs), style = textStyle.copy(color = foreground.copy(alpha = 0.68f), fontSize = 12.sp))
        }
        if (useMiuix) {
            LyricsMiuixTransport(
                isPlaying = isPlaying,
                shuffleActive = shuffleActive,
                repeatMode = repeatMode,
                primary = primary,
                onShuffle = onShuffle,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onRepeat = onRepeat,
                strings = strings,
            )
        } else {
            LyricsM3Transport(
                isPlaying = isPlaying,
                shuffleActive = shuffleActive,
                repeatMode = repeatMode,
                foreground = foreground,
                onShuffle = onShuffle,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onRepeat = onRepeat,
                strings = strings,
            )
        }
        if (track != null) {
            BasicText(
                text = "${track.title} · ${track.artist}",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                style = textStyle.copy(color = foreground.copy(alpha = 0.62f), fontSize = 11.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Transport row of the MIUIX Now Playing sheet: remix, previous, play/pause, next, repeat. */
@Composable
private fun LyricsMiuixTransport(
    isPlaying: Boolean,
    shuffleActive: Boolean,
    repeatMode: RepeatMode,
    primary: Color,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    val playScale = remember { Animatable(1f) }
    val playScope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BouncyIconButton(
            onClick = onShuffle,
            icon = Icons.Filled.Shuffle,
            contentDescription = strings.remix,
            tint = if (shuffleActive) primary else Color.White,
            iconSize = 28.dp,
        )
        BouncyIconButton(
            onClick = onPrevious,
            painter = painterResource(id = dev.shephard.player.R.drawable.ic_nowplaying_rewind),
            contentDescription = strings.previous,
            tint = Color.White,
            iconSize = 36.dp,
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .bounceClick {
                    playScope.launch {
                        playScale.animateTo(0.85f, tween(80))
                        playScale.animateTo(1.1f, spring(dampingRatio = 0.45f, stiffness = 600f))
                        playScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 600f))
                    }
                    onPlayPause()
                }
                .graphicsLayer {
                    scaleX = playScale.value
                    scaleY = playScale.value
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)).togetherWith(fadeOut(animationSpec = tween(100)))
                },
                label = "lyricsMiuixPlayPause",
            ) { playing ->
                Icon(
                    painter = painterResource(
                        id = if (playing) dev.shephard.player.R.drawable.ic_nowplaying_pause else dev.shephard.player.R.drawable.ic_nowplaying_play,
                    ),
                    contentDescription = if (playing) strings.pause else strings.play,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        BouncyIconButton(
            onClick = onNext,
            painter = painterResource(id = dev.shephard.player.R.drawable.ic_nowplaying_fforward),
            contentDescription = strings.next,
            tint = Color.White,
            iconSize = 36.dp,
        )
        BouncyIconButton(
            onClick = onRepeat,
            icon = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = strings.repeat,
            tint = if (repeatMode != RepeatMode.OFF) primary else Color.White,
            iconSize = 28.dp,
        )
    }
}

/** Transport row of the M3 Now Playing sheet: shuffle, tonal previous/next, filled play, repeat. */
@Composable
private fun LyricsM3Transport(
    isPlaying: Boolean,
    shuffleActive: Boolean,
    repeatMode: RepeatMode,
    foreground: Color,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.Shuffle,
                strings.shuffle,
                tint = if (shuffleActive) MaterialTheme.colorScheme.primary else foreground,
            )
        }
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Filled.SkipPrevious, strings.previous, modifier = Modifier.size(28.dp))
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) strings.pause else strings.play,
                modifier = Modifier.size(36.dp),
            )
        }
        FilledTonalIconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Filled.SkipNext, strings.next, modifier = Modifier.size(28.dp))
        }
        IconButton(onClick = onRepeat, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = strings.repeat,
                tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else foreground,
            )
        }
    }
}

@Composable
private fun LyricsIconButton(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    selected: Boolean = false,
    enabled: Boolean = true,
    primary: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) primary.copy(alpha = 0.25f) else LocalLyricsForeground.current.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = image,
            contentDescription = description,
            tint = if (!enabled) LocalLyricsForeground.current.copy(alpha = 0.35f) else LocalLyricsForeground.current,
            modifier = Modifier.size(if (size >= 52.dp) 27.dp else 20.dp),
        )
    }
}

@Composable
private fun LyricsPill(
    label: String,
    primary: Color,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) primary.copy(alpha = 0.30f) else LocalLyricsForeground.current.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = MaterialTheme.typography.labelSmall.copy(color = LocalLyricsForeground.current, fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
    }
}

// Synced lyrics line geometry. Lines are laid out at the active size (23sp bold) and inactive
// lines are scaled to 18/23 of it, so a line's height is the same whether it is active or not.
private const val LyricInactiveScale = 18f / 23f
private const val LyricMoveDurationMs = 650
private const val LyricAnchorFraction = 0.40f
private val LyricLineHorizontalPadding = 14.dp
private val LyricLineVerticalPadding = 9.dp

private fun lyricLineLayoutStyle(base: TextStyle): TextStyle = base.copy(
    fontSize = 23.sp,
    lineHeight = 31.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
)

/**
 * Position used by the lyrics: follows [positionMs] but keeps counting every frame while
 * playing. Small backwards corrections from the next report are ignored so the karaoke sweep
 * never jumps back; real seeks (bigger differences) are followed immediately.
 */
@Composable
private fun rememberSmoothLyricsPosition(positionMs: Long, isPlaying: Boolean, enabled: Boolean): MutableLongState {
    val smooth = remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs, isPlaying, enabled) {
        val current = smooth.longValue
        val base = if (isPlaying && positionMs <= current && current - positionMs < 400L) current else positionMs
        smooth.longValue = base
        if (!isPlaying || !enabled) return@LaunchedEffect
        var startNanos = -1L
        while (true) {
            withFrameNanos { now ->
                if (startNanos < 0L) startNanos = now
                smooth.longValue = base + (now - startNanos) / 1_000_000L
            }
        }
    }
    return smooth
}

/**
 * QQ-Music-style karaoke: paints the already-sung part of [line] in [color] over the dim base
 * text, sweeping through the current word/character in proportion to its sung time.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKaraokeHighlight(
    layout: TextLayoutResult,
    line: SyncedLyricLine,
    positionMs: Long,
    color: Color,
) {
    val textLength = layout.layoutInput.text.length
    if (textLength == 0) return
    var sungChars = 0f
    for (word in line.words) {
        if (positionMs >= word.endMs) {
            sungChars = word.endChar.toFloat()
        } else {
            if (positionMs > word.startMs) {
                val fraction = (positionMs - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1L)
                sungChars = word.startChar + fraction.coerceIn(0f, 1f) * (word.endChar - word.startChar)
            }
            break
        }
    }
    sungChars = sungChars.coerceIn(0f, textLength.toFloat())
    if (sungChars <= 0f) return
    for (lineIndex in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(lineIndex)
        if (sungChars <= lineStart) break
        val lineEnd = layout.getLineEnd(lineIndex, visibleEnd = true)
        val top = layout.getLineTop(lineIndex)
        val bottom = layout.getLineBottom(lineIndex)
        val rtl = layout.getParagraphDirection(lineStart) == androidx.compose.ui.text.style.ResolvedTextDirection.Rtl
        val edge = if (sungChars >= lineEnd || lineEnd <= lineStart) {
            if (rtl) layout.getLineLeft(lineIndex) else layout.getLineRight(lineIndex)
        } else {
            val charIndex = sungChars.toInt().coerceIn(lineStart, lineEnd - 1)
            val box = layout.getBoundingBox(charIndex)
            val within = sungChars - charIndex
            if (rtl) box.right - box.width * within else box.left + box.width * within
        }
        clipRect(
            left = if (rtl) edge else 0f,
            top = top,
            right = if (rtl) size.width else edge,
            bottom = bottom,
        ) {
            drawText(layout, color = color)
        }
    }
}

/** `[mm:ss.xx]text`, or enhanced LRC (`[t]<t1>word<t2>word<end>`) for word-synced lines. */
private fun SyncedLyricLine.toLrcLine(): String {
    if (words.isEmpty()) return "[${formatLrcTime(timeMs)}]$text"
    val builder = StringBuilder("[${formatLrcTime(timeMs)}]")
    builder.append(text, 0, words.first().startChar)
    words.forEachIndexed { index, word ->
        builder.append('<').append(formatLrcTime(word.startMs)).append('>')
        val next = words.getOrNull(index + 1)?.startChar ?: text.length
        builder.append(text, word.startChar, next.coerceAtLeast(word.startChar))
    }
    builder.append('<').append(formatLrcTime(words.last().endMs)).append('>')
    return builder.toString()
}

private fun formatLrcTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val centiseconds = (safe % 1_000L) / 10L
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}

private fun formatLyricsDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1_000L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
