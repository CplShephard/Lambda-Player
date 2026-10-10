// SPDX-License-Identifier: GPL-3.0-only
// Full-screen, shared lyrics experience for Material 3 and Miuix Now Playing.
@file:OptIn(androidx.activity.ExperimentalActivityApi::class)

package dev.shephard.player.ui.screens

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
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
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

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
    val context = LocalContext.current
    val preferences = remember(context) { PreferencesManager(context) }
    val blurEnabled by preferences.lyricsBackgroundBlurEnabled.collectAsState(initial = true)
    val savedBlurStrength by preferences.lyricsBackgroundBlurStrength.collectAsState(initial = 36f)
    val autoScroll by preferences.lyricsAutoScrollEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val lyricsListState = rememberLazyListState()
    val searchListState = rememberLazyListState()

    val track = state.currentTrack
    val primary = if (useMiuix) MiuixAppTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val textStyle = if (useMiuix) MiuixAppTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium
    val titleStyle = if (useMiuix) MiuixAppTheme.typography.titleLarge else MaterialTheme.typography.titleLarge
    var blurStrength by remember { mutableFloatStateOf(36f) }
    LaunchedEffect(savedBlurStrength) { blurStrength = savedBlurStrength.coerceIn(0f, 64f) }

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var searchQuery by remember(track?.id) {
        mutableStateOf(track?.let { "${it.artist} ${it.title}" } ?: "")
    }
    var seekFraction by remember { mutableStateOf<Float?>(null) }

    val synced = state.syncedLyrics
    val activeIndex = if (synced.isNotEmpty()) {
        synced.indexOfLast { it.timeMs + syncOffsetMs <= progress.positionMs }
    } else -1
    val durationMs = progress.durationMs.coerceAtLeast(0L)
    val displayedFraction = seekFraction ?: if (durationMs > 0L) {
        (progress.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LaunchedEffect(track?.id, state.lyrics, synced, isEditing) {
        if (!isEditing) {
            editText = if (synced.isNotEmpty()) {
                synced.joinToString("\n") { line -> "[${formatLrcTime(line.timeMs)}]${line.text}" }
            } else state.lyrics.joinToString("\n")
        }
    }

    LaunchedEffect(track?.id) {
        searchQuery = track?.let { "${it.artist} ${it.title}" } ?: ""
        playerViewModel.clearLyricsSearch()
    }

    LaunchedEffect(activeIndex, autoScroll, showSearch, showSettings, isEditing, track?.id) {
        if (autoScroll && !showSearch && !isEditing && synced.isNotEmpty() && activeIndex >= 0) {
            val viewportHeight = lyricsListState.layoutInfo.viewportSize.height
            lyricsListState.animateScrollToItem(
                index = activeIndex.coerceIn(synced.indices),
                scrollOffset = -(viewportHeight * 0.34f).toInt(),
            )
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

    val exportText = remember(state.lyrics, synced) {
        if (synced.isNotEmpty()) {
            synced.joinToString("\n") { line -> "[${formatLrcTime(line.timeMs)}]${line.text}" }
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
    PredictiveBackHandler(enabled = true) { events ->
        try {
            events.collect { event ->
                backEdge = event.swipeEdge
                backProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            if (backProgress.value < 1f) {
                backProgress.animateTo(1f, tween(150))
            }
            onDismiss()
        } catch (cancelled: CancellationException) {
            backProgress.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 380f))
            throw cancelled
        }
    }

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
            }
    ) {
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
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.58f), Color.Black.copy(alpha = 0.82f))
                    )
                )
        )

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
                        playerViewModel.setManualLyricsText(editText)
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
                            .background(Color.White.copy(alpha = 0.10f))
                            .padding(14.dp),
                    ) {
                        BasicTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = textStyle.copy(color = Color.White, fontSize = 16.sp, lineHeight = 25.sp),
                            cursorBrush = SolidColor(primary),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxSize()) {
                                    if (editText.isBlank()) {
                                        BasicText(
                                            text = strings.lyrics,
                                            style = textStyle.copy(color = Color.White.copy(alpha = 0.55f)),
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
                        Icon(Icons.Filled.Lyrics, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(14.dp))
                        BasicText(strings.noLyricsFound, style = textStyle.copy(color = Color.White.copy(alpha = 0.78f)))
                        Spacer(Modifier.height(16.dp))
                        LyricsPill(
                            label = strings.downloadLyrics,
                            primary = primary,
                            onClick = {
                                showSearch = true
                                playerViewModel.searchLyrics(searchQuery.ifBlank { track?.let { "${it.artist} ${it.title}" }.orEmpty() })
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
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (synced.isNotEmpty()) {
                            itemsIndexed(synced, key = { index, line -> "${line.timeMs}_$index" }) { index, line ->
                                val isActive = index == activeIndex
                                val distance = if (activeIndex < 0) 0 else abs(index - activeIndex)
                                val lineBlur = if (blurEnabled && activeIndex >= 0) {
                                    min(10f, distance * (blurStrength / 24f)).dp
                                } else 0.dp
                                BasicText(
                                    text = line.text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { playerViewModel.seekTo((line.timeMs + syncOffsetMs).coerceAtLeast(0)) }
                                        .blur(lineBlur)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    style = textStyle.copy(
                                        color = when {
                                            isActive -> Color.White
                                            distance > 3 -> Color.White.copy(alpha = 0.40f)
                                            else -> Color.White.copy(alpha = 0.72f)
                                        },
                                        fontSize = if (isActive) 23.sp else 18.sp,
                                        lineHeight = if (isActive) 31.sp else 26.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }
                        } else {
                            itemsIndexed(state.lyrics, key = { index, line -> "plain_${index}_$line" }) { _, line ->
                                BasicText(
                                    text = line,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                                    style = textStyle.copy(
                                        color = Color.White.copy(alpha = 0.86f),
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
                onPrevious = playerViewModel::skipToPrevious,
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::skipToNext,
                strings = strings,
            )
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
            BasicText(title, style = titleStyle.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(subtitle, style = bodyStyle.copy(color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LyricsIconButton(Icons.Filled.Search, "Search lyrics", onSearch)
        LyricsIconButton(Icons.Filled.FolderOpen, "Import lyrics", onImport)
        LyricsIconButton(if (isEditing) Icons.Filled.Check else Icons.Filled.Edit, if (isEditing) "Save lyrics" else "Edit lyrics", onEdit, selected = isEditing, primary = primary)
        LyricsIconButton(Icons.Filled.Tune, "Lyrics settings", onSettings, selected = false, primary = primary)
    }
}

@Composable
private fun LyricsSettingsPanel(
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.11f))
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
            style = textStyle.copy(color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp),
        )
        Slider(
            value = blurStrength,
            onValueChange = onBlurStrengthChange,
            onValueChangeFinished = onBlurStrengthChangeFinished,
            valueRange = 0f..64f,
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText("${strings.lyricsSyncOffset}: ${if (syncOffsetMs >= 0) "+" else ""}${syncOffsetMs} ms", style = textStyle.copy(color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp), modifier = Modifier.weight(1f))
            LyricsPill("−100", primary, onClick = { onOffsetChange(syncOffsetMs - 100) })
            LyricsPill("+100", primary, onClick = { onOffsetChange(syncOffsetMs + 100) })
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
                .background(Color.White.copy(alpha = 0.12f))
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                if (query.isBlank()) BasicText(strings.lyricsSearchPrompt, style = textStyle.copy(color = Color.White.copy(alpha = 0.5f)))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = textStyle.copy(color = Color.White, fontSize = 16.sp),
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
                    style = textStyle.copy(color = Color.White.copy(alpha = 0.76f), textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                contentPadding = PaddingValues(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { result -> result.id ?: (result.title + result.artist).hashCode().toLong() }) { result ->
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
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(result.title, style = textStyle.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(
            listOfNotNull(result.artist.takeIf(String::isNotBlank), result.album.takeIf(String::isNotBlank)).joinToString(" · "),
            style = textStyle.copy(color = Color.White.copy(alpha = 0.70f), fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                text = result.durationSeconds?.let { formatLyricsDuration((it * 1000.0).toLong()) } ?: "",
                style = textStyle.copy(color = Color.White.copy(alpha = 0.60f), fontSize = 12.sp),
            )
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
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)) {
        Slider(
            value = seekFraction,
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            enabled = durationMs > 0L,
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(formatLyricsDuration(if (durationMs > 0L) (seekFraction * durationMs).toLong() else progressMs), style = textStyle.copy(color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp))
            BasicText(formatLyricsDuration(durationMs), style = textStyle.copy(color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LyricsIconButton(Icons.Filled.SkipPrevious, strings.previous, onPrevious, size = 52.dp)
            LyricsIconButton(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (isPlaying) strings.pause else strings.play,
                onPlayPause,
                size = 60.dp,
                selected = true,
                primary = primary,
            )
            LyricsIconButton(Icons.Filled.SkipNext, strings.next, onNext, size = 52.dp)
        }
        if (track != null) {
            BasicText(
                text = "${track.title} · ${track.artist}",
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                style = textStyle.copy(color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            .background(if (selected) primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = image,
            contentDescription = description,
            tint = if (!enabled) Color.White.copy(alpha = 0.35f) else Color.White,
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
            .background(if (selected) primary.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
    }
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
