@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package dev.shephard.player.ui.screens

import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.formattedDuration
import dev.shephard.player.data.slideForwardInQueue
import dev.shephard.player.data.trackById
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.glass.GlassTint
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurface
import dev.shephard.player.ui.glass.blurSurfaceCompact
import dev.shephard.player.ui.glass.blurSheetSurface
import dev.shephard.player.ui.components.MinimalSeekBar
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.components.overScrollVertical
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.absoluteValue

@Composable
fun NowPlayingSheet(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val track = state.currentTrack
    val strings = LocalStrings.current
    val nowPlayingLiquidGlassOn = LocalBlurEnabled.current

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    val dragOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val dragScope = rememberCoroutineScope()

    // Sheet her ekrana geldiğinde dragOffset'i 0'a al: parmakla kapatıp tekrar açınca
    // eski sürükleme miktarının (translationY) kalıcı gri boşluk bırakmasını engeller.
    LaunchedEffect(Unit) {
        dragOffset.snapTo(0f)
    }

    // Kapak resmi swipe-to-change-song: OuterTune'daki gibi sürekli aktif (togglesız),
    // LazyHorizontalGrid + snap fling ile akıcı, native hızda swipe. Önceki/mevcut/sonraki
    // parça queue içindeki index'e göre bulunur (controller'a gerek yok).
    val currentQueueIndex = remember(state.queue, track?.id) {
        state.queue.indexOfFirst { it.id == track?.id }
    }
    val previousArtTrack = if (currentQueueIndex > 0) state.queue.getOrNull(currentQueueIndex - 1) else null
    val nextArtTrack = if (currentQueueIndex >= 0) state.queue.getOrNull(currentQueueIndex + 1) else null
    val artSwipeItems = listOfNotNull(previousArtTrack, track, nextArtTrack)
    val artSwipeCurrentIndex = artSwipeItems.indexOfFirst { it.id == track?.id }.coerceAtLeast(0)

    val artGridState = rememberLazyGridState()
    val artGridScrollOffset by remember { derivedStateOf { artGridState.firstVisibleItemScrollOffset } }
    val artGridFirstVisibleIndex by remember { derivedStateOf { artGridState.firstVisibleItemIndex } }

    // Kullanıcı bırakıp snap tamamlanınca (scroll durdu, offset==0) hedef indexe göre
    // gerçek şarkı değişimini tetikle. OuterTune'daki Player.kt ile birebir aynı desen:
    // key sadece scroll offset'i, scroll-in-progress kontrolü effect içinde.
    LaunchedEffect(artGridScrollOffset) {
        if (artGridState.isScrollInProgress || artGridScrollOffset != 0) return@LaunchedEffect
        if (artGridFirstVisibleIndex > artSwipeCurrentIndex) {
            playerViewModel.skipToNext()
        } else if (artGridFirstVisibleIndex < artSwipeCurrentIndex) {
            playerViewModel.skipToPrevious()
        }
    }

    // Şarkı değiştiğinde (dışarıdan ya da swipe sonucu) grid'i doğru öğeye ortala.
    LaunchedEffect(track?.id, state.queue) {
        val index = artSwipeCurrentIndex
        artGridState.scrollToItem(index)
    }

    val glow = Color(state.glowColorArgb)

    // Drag dismiss için pencerenin GERÇEK ölçülen yüksekliği (px). Daha önce
    // LocalConfiguration.screenHeightDp kullanılıyordu, bu ise her zaman cihazın TÜM ekran
    // yüksekliğini varsayıyor — Android freeform/split-screen pencerelerde (uygulama ekranın
    // sadece bir kısmını kaplarken) bu değer gerçek pencere boyutundan çok daha büyük çıkıyor,
    // bu da aşağı sürükleyip kapatma animasyonunun yanlış mesafeye gitmesine sebep oluyordu.
    // onSizeChanged ile Compose'un bize verdiği GERÇEK ölçülen pencere boyutunu kullanıyoruz.
    val configuration = LocalConfiguration.current
    var measuredHeightPx by remember { mutableFloatStateOf(with(density) { configuration.screenHeightDp.dp.toPx() }) }
    val screenHeightPx = measuredHeightPx

    // pulse artık gradient radius'unu değil sadece graphicsLayer scale'ini etkiliyor
    // → Brush her frame yeniden oluşturulmuyor, sadece transform matrix değişiyor
    val pulse by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 0.85f + state.amplitude * 0.15f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "glowPulse"
    )
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (track == null) 0f else 0.55f + state.amplitude * 0.2f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "glowAlpha"
    )

    // Glow brush'ları remember ile stabilize et — glow rengi değişince yeniden oluşturulsun,
    // ama pulse/amplitude her değiştiğinde Brush allocation olmasın
    val verticalGlowBrush = remember(glow) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to glow.copy(alpha = 0.90f),
                0.40f to glow.copy(alpha = 0.55f),
                0.70f to glow.copy(alpha = 0.12f),
                0.80f to Color.Transparent,
                1.00f to Color.Transparent
            )
        )
    }
    val radialGlowBrush = remember(glow) {
        Brush.radialGradient(
            colors = listOf(
                glow.copy(alpha = 0.55f),
                glow.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(0f, 320f),
            radius = 680f
        )
    }

    val playButtonScale = remember { androidx.compose.animation.core.Animatable(1f) }
    val playButtonScope = rememberCoroutineScope()

    // Sheet tam ekran halindeyken (dragOffset == 0, yani parmakla etkileşimde değilken
    // dinlenme pozisyonunda) üst cornerlar ekranı tamamen doldurmalı — tıpkı iOS'taki gibi.
    // Kullanıcı sheet'i aşağı sürüklemeye başlar başlamaz corner'lar geri gelsin.
    //
    // Açılış animasyonu (MainContainer'daki AnimatedVisibility + slideInVertically) sırasında
    // corner radius 30dp olmalı (ekran henüz tam kaplanmadığı için köşeler görünür durumda
    // kalmalı), animasyon bitip sheet dinlenme konumuna oturunca 0dp'ye insin — bu sayede
    // ekran görüntüsü/ekran kaydında köşeler görünmez. hasEnteredRest, sheet ilk açıldığında
    // bir kere gecikmeyle true olur ve girişte 30dp'nin gösterilmesini sağlar.
    var hasEnteredRest by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasEnteredRest = false
        kotlinx.coroutines.delay(380) // MainContainer'daki spring(stiffness=180) giriş süresine yakın
        hasEnteredRest = true
    }
    var isInteractingWithSheet by remember { mutableStateOf(false) }
    val isFullyExpanded by remember {
        androidx.compose.runtime.derivedStateOf { hasEnteredRest && !isInteractingWithSheet && dragOffset.value <= 0.5f }
    }
    val sheetCornerRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isFullyExpanded) 0.dp else 30.dp,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "sheetCornerRadius"
    )
    val sheetShape = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = sheetCornerRadius,
        topEnd = sheetCornerRadius
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { measuredHeightPx = it.height.toFloat() }
            .graphicsLayer { translationY = dragOffset.value.coerceAtLeast(0f) }
            .clip(sheetShape)
            .background(MaterialTheme.colorScheme.background)
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
                            // Sheet ekran dışında — direkt kapat, reset LaunchedEffect(Unit)'e bırakılır
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
        // Ambient glow background — blur artık graphicsLayer renderEffect ile (API 31+)
        // veya daha küçük dp değeriyle yapılıyor; Brush sabit, scale animasyonu ucuz
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = glowAlpha
                    scaleX = pulse
                    scaleY = pulse
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(180f, 180f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .background(verticalGlowBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = glowAlpha * 0.85f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(220f, 220f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                }
                .background(radialGlowBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Top row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.nowPlaying,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.currentPlaylistName != null) {
                        Text(
                            text = state.currentPlaylistName!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Box(modifier = Modifier.size(48.dp))
            }

            // Album art + title
            Column {
                // Kapak resmi: OuterTune'daki "swipe to change songs" ile birebir aynı mekanizma —
                // LazyHorizontalGrid + snap fling, togglesız her zaman aktif. Önceki/mevcut/sonraki
                // kapak yan yana dizilir, snap tamamlanınca gerçek şarkı değişimi tetiklenir.
                // Elle yazılmış detectHorizontalDragGestures + Animatable yerine native scroll
                // fiziği (fling velocity, snap animasyonu) kullanıldığı için akıcılık OS seviyesinde.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val maxArtHeight = maxHeight * 0.42f
                    val artItemWidth = maxWidth

                    val artSnapLayoutInfoProvider = remember(artGridState) {
                        SnapLayoutInfoProvider(
                            lazyGridState = artGridState,
                            snapPosition = androidx.compose.foundation.gestures.snapping.SnapPosition.Center
                        )
                    }

                    if (artSwipeItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .aspectRatio(1f)
                                .heightIn(max = maxArtHeight)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    } else {
                        LazyHorizontalGrid(
                            state = artGridState,
                            rows = GridCells.Fixed(1),
                            flingBehavior = rememberSnapFlingBehavior(artSnapLayoutInfoProvider),
                            userScrollEnabled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .aspectRatio(1f)
                                .heightIn(max = maxArtHeight)
                        ) {
                            gridItemsIndexed(
                                items = artSwipeItems,
                                key = { _, item -> item.id }
                            ) { _, itemTrack ->
                                Box(
                                    modifier = Modifier
                                        .width(artItemWidth)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    var artLoaded by remember(itemTrack.id) { mutableStateOf(false) }
                                    AsyncImage(
                                        model = itemTrack.albumArtUri,
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
                            }
                        }
                    }
                }

                // Başlık/artist: şarkı id'sine göre ayrı fade+slide geçişi (kapaktan bağımsız,
                // yön kuyruktaki konuma göre belirlenir).
                androidx.compose.animation.AnimatedContent(
                    targetState = track?.id ?: -1L,
                    transitionSpec = {
                        val dir = if (slideForwardInQueue(state.queue, initialState, targetState))
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        else
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        androidx.compose.animation.ContentTransform(
                            targetContentEnter = slideIntoContainer(dir, androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn(),
                            initialContentExit = slideOutOfContainer(dir, androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeOut()
                        )
                    },
                    label = "titleSwap",
                    modifier = Modifier.fillMaxWidth()
                ) { targetId ->
                    val displayTrack = state.queue.trackById(targetId) ?: track
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayTrack?.title ?: strings.nothingPlaying,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = displayTrack?.artist ?: strings.pickASong,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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

            // Action buttons row: Queue, Lyrics, +/check
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showQueue by remember { mutableStateOf(false) }
                var showPlaylists by remember { mutableStateOf(false) }
                var showLyrics by remember { mutableStateOf(false) }

                // State'ler if dışında — Compose composition kuralı
                val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val queueSheetScope = rememberCoroutineScope()
                val lyricsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val lyricsSheetScope = rememberCoroutineScope()
                val trackId = track?.id ?: -1L
                val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)

                BouncyIconButton(
                    onClick = { showQueue = true },
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = strings.queue,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp,
                    backgroundColor = if (nowPlayingLiquidGlassOn) Color.Transparent else null
                )
                BouncyIconButton(
                    onClick = { showLyrics = true },
                    icon = Icons.Filled.Lyrics,
                    contentDescription = strings.lyrics,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp,
                    backgroundColor = if (nowPlayingLiquidGlassOn) Color.Transparent else null
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (nowPlayingLiquidGlassOn) {
                                Modifier.blurSurfaceCompact(
                                    enabled = true,
                                    shape = CircleShape,
                                    tint = if (isLiked) GlassTint.ACCENT else GlassTint.SURFACE
                                )
                            } else {
                                Modifier.background(
                                    if (isLiked) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        )
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
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = if (isLiked) "Added" else "Add",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (showQueue) {
                    ModalBottomSheet(
                        onDismissRequest = { showQueue = false },
                        sheetState = queueSheetState,
                        containerColor = if (nowPlayingLiquidGlassOn) Color.Transparent else MaterialTheme.colorScheme.surface,
                        dragHandle = {
                            var handleDragAmount by remember { mutableStateOf(0f) }
                            val handleDismissThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (nowPlayingLiquidGlassOn)
                                            Modifier.blurSheetSurface(
                                                enabled = true,
                                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                            )
                                        else Modifier
                                    )
                                    .padding(vertical = 10.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { handleDragAmount = 0f },
                                            onDragEnd = {
                                                // Sadece gerçekten aşağı doğru anlamlı bir mesafe
                                                // sürüklenmişse kapat (elle tutup çekme). Küçük
                                                // dokunuş/titremeler artık kapatmıyor.
                                                if (handleDragAmount > handleDismissThresholdPx) {
                                                    queueSheetScope.launch {
                                                        queueSheetState.hide()
                                                        showQueue = false
                                                    }
                                                }
                                                handleDragAmount = 0f
                                            },
                                            onDragCancel = { handleDragAmount = 0f }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            handleDragAmount += dragAmount.y
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                )
                            }
                        }
                    ) {
                        QueueList(
                            queue = state.queue,
                            currentTrackId = state.currentTrack?.id,
                            onMove = { from, to -> playerViewModel.moveQueueItem(from, to) },
                            onPlay = { playerViewModel.playQueueItem(it) },
                            onRemove = { playerViewModel.removeFromQueue(it) },
                            onPlayNext = { playerViewModel.playNext(it) },
                            strings = strings
                        )
                    }
                }

                if (showLyrics) {
                    val lyricsContext = LocalContext.current
                    var isDownloading by remember { mutableStateOf(false) }
                    var downloadError by remember { mutableStateOf<String?>(null) }
                    val lyricListState = rememberLazyListState()
                    val syncedLyrics = state.syncedLyrics
                    val currentMs = state.positionMs
                    val activeIndex = if (syncedLyrics.isNotEmpty()) {
                        syncedLyrics.indexOfLast { it.timeMs <= currentMs }.coerceAtLeast(0)
                    } else -1

                    // Auto-scroll to active line
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0 && syncedLyrics.isNotEmpty()) {
                            lyricListState.animateScrollToItem(activeIndex)
                        }
                    }

                    val lyricsFilePicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) {
                            lyricsSheetScope.launch {
                                val lines = withContext(Dispatchers.IO) {
                                    try {
                                        lyricsContext.contentResolver.openInputStream(uri)
                                            ?.bufferedReader()?.readText()
                                            ?.let { playerViewModel.parseLrcPublic(it) }
                                    } catch (_: Exception) { null }
                                }
                                if (lines != null) playerViewModel.setManualLyrics(lines)
                            }
                        }
                    }

                    ModalBottomSheet(
                        onDismissRequest = { showLyrics = false },
                        sheetState = lyricsSheetState,
                        containerColor = if (nowPlayingLiquidGlassOn) Color.Transparent else MaterialTheme.colorScheme.surface,
                        dragHandle = {
                            var handleDragAmount by remember { mutableStateOf(0f) }
                            val handleDismissThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (nowPlayingLiquidGlassOn)
                                            Modifier.blurSheetSurface(
                                                enabled = true,
                                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                            )
                                        else Modifier
                                    )
                                    .padding(vertical = 10.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { handleDragAmount = 0f },
                                            onDragEnd = {
                                                if (handleDragAmount > handleDismissThresholdPx) {
                                                    lyricsSheetScope.launch {
                                                        lyricsSheetState.hide()
                                                        showLyrics = false
                                                    }
                                                }
                                                handleDragAmount = 0f
                                            },
                                            onDragCancel = { handleDragAmount = 0f }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            handleDragAmount += dragAmount.y
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (nowPlayingLiquidGlassOn) Modifier.blurSheetSurface(enabled = true, shape = RoundedCornerShape(0.dp))
                                    else Modifier
                                )
                                .padding(16.dp)
                                .heightIn(min = 200.dp, max = 520.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(strings.lyrics, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (state.lyrics.isEmpty()) {
                                    // File picker button
                                    androidx.compose.material3.IconButton(onClick = { lyricsFilePicker.launch(arrayOf("text/*", "application/octet-stream")) }) {
                                        Icon(Icons.Filled.FolderOpen, contentDescription = strings.addLyricsFromFile, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (state.lyrics.isEmpty()) {
                                Text(strings.noLyricsFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                val currentTrack = state.currentTrack
                                if (currentTrack != null) {
                                    if (isDownloading) {
                                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        androidx.compose.material3.FilledTonalButton(
                                            onClick = {
                                                isDownloading = true
                                                downloadError = null
                                                lyricsSheetScope.launch {
                                                    val result = withContext(Dispatchers.IO) {
                                                        fetchLyricsFromApi(currentTrack.artist, currentTrack.title)
                                                    }
                                                    isDownloading = false
                                                    if (result != null) playerViewModel.setManualLyrics(result)
                                                    else downloadError = strings.noLyricsFound
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Filled.Lyrics, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(strings.downloadLyrics)
                                        }
                                    }
                                    downloadError?.let {
                                        Spacer(Modifier.height(8.dp))
                                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = lyricListState,
                                    modifier = Modifier.overScrollVertical()
                                ) {
                                    itemsIndexed(state.lyrics) { idx, line ->
                                        val isActive = idx == activeIndex
                                        Text(
                                            text = line,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (syncedLyrics.isNotEmpty()) Modifier.clickable {
                                                        playerViewModel.seekTo(syncedLyrics.getOrNull(idx)?.timeMs ?: 0L)
                                                    } else Modifier
                                                )
                                                .padding(vertical = 6.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                if (showPlaylists) {
                    AddToPlaylistDrawer(
                        trackId = trackId,
                        track = track,
                        playerViewModel = playerViewModel,
                        onDismiss = { showPlaylists = false },
                        strings = strings
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 32.dp)
                    .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRemixed by playerViewModel.isRemixed.collectAsState()
                BouncyIconButton(
                    onClick = { playerViewModel.remixQueue() },
                    icon = Icons.Filled.Shuffle,
                    contentDescription = strings.remix,
                    tint = if (isRemixed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
                BouncyIconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = strings.previous,
                    tint = MaterialTheme.colorScheme.onBackground,
                    iconSize = 36.dp
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .then(
                            if (nowPlayingLiquidGlassOn) {
                                Modifier.blurSurface(
                                    enabled = true,
                                    shape = CircleShape,
                                    tint = GlassTint.ACCENT
                                )
                            } else {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            }
                        )
                        .bounceClick {
                            playButtonScope.launch {
                                playButtonScale.animateTo(0.85f, androidx.compose.animation.core.tween(80))
                                playButtonScale.animateTo(1.1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 600f))
                                playButtonScale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 600f))
                            }
                            playerViewModel.togglePlayPause()
                        }
                        .graphicsLayer {
                            scaleX = playButtonScale.value
                            scaleY = playButtonScale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = {
                            androidx.compose.animation.ContentTransform(
                                targetContentEnter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)) + androidx.compose.animation.scaleIn(initialScale = 0.5f),
                                initialContentExit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100)) + androidx.compose.animation.scaleOut(targetScale = 1.5f)
                            )
                        },
                        label = "playPauseIcon"
                    ) { isPlaying ->
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) strings.pause else strings.play,
                            tint = if (nowPlayingLiquidGlassOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
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
private fun AddToPlaylistDrawer(
    trackId: Long,
    track: AudioTrack?,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val json by prefs.playlistsJson.collectAsState(initial = "[]")
    val playlists = remember(json) { parsePlaylists(json) }
    val likedJson by prefs.likedSongIds.collectAsState(initial = "[]")
    val likedIds = remember(likedJson) {
        try { org.json.JSONArray(likedJson).let { arr -> (0 until arr.length()).map { arr.getLong(it) } } }
        catch (_: Exception) { emptyList() }
    }
    val isLiked = likedIds.contains(trackId)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addToPlaylistLiquidGlassOn = LocalBlurEnabled.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (addToPlaylistLiquidGlassOn) Color.Transparent else MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (addToPlaylistLiquidGlassOn)
                            Modifier.blurSheetSurface(
                                enabled = true,
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                            )
                        else Modifier
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (addToPlaylistLiquidGlassOn) Modifier.blurSheetSurface(enabled = true, shape = RoundedCornerShape(0.dp))
                    else Modifier
                )
                .padding(16.dp)
                .height(420.dp)
        ) {
            Text(strings.addToPlaylist, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.overScrollVertical()
            ) {
                // Liked Songs at top
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch {
                                    val newIds = if (isLiked) likedIds - trackId else likedIds + trackId
                                    val arr = org.json.JSONArray().apply { newIds.forEach { put(it) } }
                                    prefs.setLikedSongIds(arr.toString())
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.likedSongs, fontWeight = FontWeight.SemiBold)
                            Text("${likedIds.size} ${strings.trackCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(if (isLiked) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                // User playlists
                itemsIndexed(playlists) { idx, pl ->
                    if (pl.isSystem) return@itemsIndexed
                    val containsTrack = pl.trackIds.contains(trackId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val updated = if (containsTrack)
                                    pl.copy(trackIds = pl.trackIds - trackId)
                                else
                                    pl.copy(trackIds = pl.trackIds + trackId)
                                val all = playlists.toMutableList()
                                all[idx] = updated
                                scope.launch { prefs.setPlaylistsJson(encodePlaylists(all)) }
                                // Queue'ye ekle (yalnızca ekleniyorsa, kaldırılmıyorsa)
                                if (!containsTrack && track != null) {
                                    playerViewModel.addTrackToQueue(track)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, fontWeight = FontWeight.SemiBold)
                            Text("${pl.trackIds.size} ${strings.trackCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(if (containsTrack) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (containsTrack) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (containsTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onRemove: (index: Int) -> Unit,
    onPlayNext: (index: Int) -> Unit,
    strings: dev.shephard.player.ui.i18n.Strings
) {
    val currentStartIndex = remember(queue, currentTrackId) {
        queue.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
    }
    // OuterTune'daki gibi: gerçek liste mutableStateListOf ile tutulur, drag sırasında
    // reorderableState onMove callback'i doğrudan bu listeyi günceller (item flicker olmadan).
    val items = remember { mutableStateListOf<AudioTrack>() }
    LaunchedEffect(queue, currentStartIndex) {
        if (items.map { it.id } != queue.drop(currentStartIndex).map { it.id }) {
            items.clear()
            items.addAll(queue.drop(currentStartIndex))
        }
    }

    val listState = rememberLazyListState()

    // Drag başlayıp bitene kadar olan from/to'yu tek seferde biriktirip
    // gerçek move işlemini (onMove) sadece drag bitince tetikliyoruz —
    // OuterTune'un Queue.kt'deki reorderableState + LaunchedEffect(isAnyItemDragging) deseni.
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

    val queueLiquidGlassOn = LocalBlurEnabled.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (queueLiquidGlassOn) Modifier.blurSheetSurface(enabled = true, shape = RoundedCornerShape(0.dp))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = strings.queue,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .overScrollVertical()
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
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 120.dp.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val duration = remember(track.id) { track.formattedDuration() }
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragged) 8.dp else 0.dp,
        label = "queueItemElevation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .offset(x = with(density) { offsetX.toDp() })
            .shadow(elevation, RoundedCornerShape(12.dp))
            .zIndex(if (isDragged) 1f else 0f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isDragged -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .clickable { onPlay() }
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX < -swipeThresholdPx -> onPlayNext()
                            offsetX > swipeThresholdPx -> onRemove()
                        }
                        offsetX = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount
                }
            }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            var loaded by remember { mutableStateOf(false) }
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true }
            )
            if (!loaded) {
                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            duration,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        if (!isPlaying) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .then(dragHandleModifier)
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

/** Parantez/köşeli parantez içindeki gereksiz ekleri temizler: "Title (feat. X)" → "Title" */
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

/** lrclib.net — synced lyrics desteği olan birincil kaynak */
private fun fetchFromLrclib(artist: String, title: String): List<String>? {
    val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
    // Önce tam eşleşme dene
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

/** lrclib search endpoint — tam eşleşme bulunamazsa en iyi sonucu alır */
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

/** lyrics.ovh — basit fallback, synced desteklemez ama geniş kataloğu var */
private fun fetchFromLyricsOvh(artist: String, title: String): List<String>? {
    val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
    val body = httpGet("https://api.lyrics.ovh/v1/${enc(artist)}/${enc(cleanTitle(title))}")
        ?: return null
    val json = runCatching { org.json.JSONObject(body) }.getOrNull() ?: return null
    val lyrics = json.optString("lyrics")
    return lyrics.takeIf { it.isNotBlank() }
        ?.lines()?.map { it.trimEnd() }?.filter { it.isNotBlank() }
}

/**
 * Çoklu kaynak sırasıyla denenir:
 * 1. lrclib GET (tam eşleşme)
 * 2. lrclib SEARCH (bulanık eşleşme, temizlenmiş title)
 * 3. lyrics.ovh (fallback)
 */
private fun fetchLyricsFromApi(artist: String, title: String): List<String>? =
    fetchFromLrclib(artist, title)
        ?: fetchFromLrclibSearch(artist, title)
        ?: fetchFromLyricsOvh(artist, title)
