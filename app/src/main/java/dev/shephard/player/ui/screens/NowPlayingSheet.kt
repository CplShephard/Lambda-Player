@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package dev.shephard.player.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.Delete

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.VerticalAlignCenter
import dev.shephard.player.ui.miuix.ExperimentalMaterial3Api
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.IconButton
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.OutlinedTextField
import dev.shephard.player.ui.components.MiuixDrawer
import dev.shephard.player.ui.components.rememberDrawerDismiss
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.player.RepeatMode
import dev.shephard.player.ui.components.BouncyIconButton
import dev.shephard.player.ui.glass.GlassTint
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.blurSurface
import dev.shephard.player.ui.glass.blurSurfaceCompact
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
    val navigationDirection by playerViewModel.navigationDirection.collectAsState()
    val track = state.currentTrack
    val strings = LocalStrings.current
    val nowPlayingLiquidGlassOn = LocalBlurEnabled.current

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    // Giriş (açılış) kaydırması artık burada `dragOffset` üzerinden yapılıyor (alttaki
    // LaunchedEffect). İlk frame'de boşluk/flaş olmaması için başlangıç değeri ekran
    // yüksekliği olarak veriliyor; sheet böylece ilk frame'de ekranın altında
    // (görünmez) başlıyor, sonra yukarı kayıyor.
    // NOT: LocalConfiguration.current bir @Composable çağrısıdır; remember/with lambda'sı
    // içinde ÇAĞRILAMAZ, bu yüzden en üst seviyede alınıp değer olarak kullanılıyor.
    val dragOffsetScreenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val dragOffsetInitialHeight = with(density) { dragOffsetScreenHeight.dp.toPx() }
    val dragOffset = remember { androidx.compose.animation.core.Animatable(dragOffsetInitialHeight) }
    val dragScope = rememberCoroutineScope()

    // Sheet her ekrana geldiğinde dragOffset'i 0'a al: parmakla kapatıp tekrar açınca
    // eski sürükleme miktarının (translationY) kalıcı gri boşluk bırakmasını engeller.
    // MADDE (köşe yarıçapı zamanlaması) — açılış kaydırması artık burada `dragOffset`
    // üzerinden yapılıyor. Sheet ekranın altından yukarı kayar; `dragOffset` 0'a
    // ulaşınca (yani sheet TAM oturunca) köşe yarıçapı 30dp -> 0dp'ye iner. Böylece
    // köşeler, açılış animasyonu bitince — değil, TAM bitince — 0'a düşer (sihirli
    // gecikme yok).
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

    // Sheet ilk açıldığında artGridState index=0'da başlar, ama gerçek
    // artSwipeCurrentIndex genelde 1'dir. Bu fark kullanıcı hiç dokunmadan skip
    // tetiklememeli; commit yalnızca grid gerçek kullanıcı scroll'u gördükten sonra yapılır.
    var hasUserScrolledArtGrid by remember { mutableStateOf(false) }
    var artSkipInFlight by remember { mutableStateOf(false) }
    var hasPlacedInitialArt by remember { mutableStateOf(false) }
    LaunchedEffect(artGridState.isScrollInProgress) {
        if (artGridState.isScrollInProgress) hasUserScrolledArtGrid = true
    }

    // Snap tamamlanınca hedef indexe göre şarkıyı değiştir. Key'e firstVisibleItemIndex'i
    // de dahil ediyoruz: bazı Compose sürümlerinde item tam snap noktasındayken offset 0
    // kalır, sadece index değişir; eski kod offset değişmediği için skip'i kaçırabiliyordu.
    LaunchedEffect(artGridFirstVisibleIndex, artGridScrollOffset, artGridState.isScrollInProgress) {
        if (!hasUserScrolledArtGrid || artSkipInFlight) return@LaunchedEffect
        if (artGridState.isScrollInProgress || artGridScrollOffset != 0) return@LaunchedEffect
        when {
            artGridFirstVisibleIndex > artSwipeCurrentIndex -> {
                artSkipInFlight = true
                hasUserScrolledArtGrid = false
                playerViewModel.skipToNext()
            }
            artGridFirstVisibleIndex < artSwipeCurrentIndex -> {
                artSkipInFlight = true
                hasUserScrolledArtGrid = false
                playerViewModel.skipToPrevious()
            }
        }
    }

    // Track/queue değişince yeni "önceki/mevcut/sonraki" penceresinin mevcut öğesine oturt.
    // Dışarıdan next/previous tuşuyla gelen değişimde eski kapak yeni pencerede komşu index'te
    // durur; önce o komşuya snap edip sonra merkeze animate ediyoruz. Böylece next: sağdan
    // ortaya, previous: soldan ortaya gelen eski NowPlaying animasyonu geri gelir. Swipe ile
    // tetiklenen değişimde ise kullanıcı zaten fiziksel scroll animasyonunu gördüğü için sadece
    // sessizce merkeze sabitliyoruz.
    //
    // ÖNEMLİ: Kayma animasyonu YALNIZCA çalan şarkı gerçekten değiştiğinde oynar.
    // Queue yalnızca yeniden sıralandığında (remix, sürükle-bırak, play next...) çalan
    // şarkı aynı kaldığı için sessizce merkeze snap edilir — remix tuşuna art arda
    // basınca "çalan şarkı sonrakiyle yer değiştiriyor" gibi görünen görsel hata buydu.
    var lastArtTrackId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(track?.id, state.queue) {
        hasUserScrolledArtGrid = false
        val trackChanged = track?.id != lastArtTrackId
        lastArtTrackId = track?.id
        if (artSwipeItems.isNotEmpty()) {
            val target = artSwipeCurrentIndex
            if (!hasPlacedInitialArt) {
                artGridState.scrollToItem(target)
                hasPlacedInitialArt = true
            } else if (artSkipInFlight) {
                artGridState.scrollToItem(target)
                artSkipInFlight = false
            } else if (!trackChanged) {
                // Sadece queue sırası değişti (örn. remix): animasyonsuz merkeze otur.
                artGridState.scrollToItem(target)
            } else {
                val start = when {
                    navigationDirection > 0 && target > 0 -> target - 1
                    navigationDirection < 0 && target < artSwipeItems.lastIndex -> target + 1
                    else -> target
                }
                if (start != target) {
                    // Stride'ı (bir kapak + aradaki 16dp boşluk) scroll'dan ÖNCE, mevcut
                    // layout bilgisinden hesapla — scrollToItem sonrası layoutInfo bir frame
                    // geride kalabiliyor.
                    val itemWidthPx = artGridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.width ?: 0
                    val stridePx = itemWidthPx + with(density) { 0.dp.toPx() }
                    artGridState.scrollToItem(start)
                    if (itemWidthPx > 0) {
                        // animateScrollToItem'ın sabit (çok hızlı) animasyonu yerine daha
                        // yavaş, yumuşak bir tween — şarkıdan şarkıya geçiş artık akıcı görünür.
                        artGridState.animateScrollBy(
                            value = stridePx * (target - start),
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 500,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                        // Olası yuvarlama sapmasını düzelt (görsel sıçrama yaratmaz, fark < 1px)
                        artGridState.scrollToItem(target)
                    } else {
                        artGridState.animateScrollToItem(target)
                    }
                } else {
                    artGridState.scrollToItem(target)
                }
            }
        } else {
            artSkipInFlight = false
        }
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
        // Ambient glow background — amplitude flow'unu (≈80ms'de bir) SADECE bu küçük
        // composable dinler; sheet'in geri kalanı amplitude tiklerinden etkilenmez.
        AmbientGlowBackground(
            playerViewModel = playerViewModel,
            glow = glow,
            hasTrack = track != null
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top row
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
                        color = MiuixAppTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.currentPlaylistName != null) {
                        Text(
                            text = state.currentPlaylistName!!,
                            style = MiuixAppTheme.typography.labelSmall,
                            color = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Box(modifier = Modifier.size(48.dp))
            }

            // Album art + title
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Kapak resmi: OuterTune'daki "swipe to change songs" ile birebir aynı mekanizma —
                // LazyHorizontalGrid + snap fling, togglesız her zaman aktif. Önceki/mevcut/sonraki
                // kapak yan yana dizilir, snap tamamlanınca gerçek şarkı değişimi tetiklenir.
                // Elle yazılmış detectHorizontalDragGestures + Animatable yerine native scroll
                // fiziği (fling velocity, snap animasyonu) kullanıldığı için akıcılık OS seviyesinde.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val maxArtHeight = maxHeight * 0.42f
                    val artItemWidth = maxWidth
                    // Kapak (kare, en fazla maxArtHeight) + altındaki başlık/artist bloğu için
                    // ayrılan sabit yükseklik. LazyHorizontalGrid'in satır yüksekliğini kendisi
                    // içerikten çıkaramadığı için (rows = Fixed(1) ile dış constraint gerekir)
                    // burada elle veriyoruz.
                    val titleBlockHeight = 92.dp
                    val artRowHeight = minOf(artItemWidth, maxArtHeight) + titleBlockHeight

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
                                .background(MiuixAppTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MiuixAppTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    } else {
                        LazyHorizontalGrid(
                            state = artGridState,
                            rows = GridCells.Fixed(1),
                            flingBehavior = rememberSnapFlingBehavior(artSnapLayoutInfoProvider),
                            userScrollEnabled = true,
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .height(artRowHeight)
                        ) {
                            gridItemsIndexed(
                                items = artSwipeItems,
                                key = { _, item -> item.id }
                            ) { _, itemTrack ->
                                // Kapak VE başlık/artist aynı swipe item'ının içinde: parmak
                                // kapağı kaydırırken ikisi de fiziksel olarak birlikte hareket
                                // eder — ayrı bir AnimatedContent ile "senkronize" etmeye gerek
                                // kalmaz, çünkü zaten aynı scroll offsetini paylaşıyorlar.
                                Column(
                                    modifier = Modifier.width(artItemWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .heightIn(max = maxArtHeight)
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
                                                tint = MiuixAppTheme.colorScheme.primary,
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
                                            text = itemTrack.title,
                                            style = MiuixAppTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MiuixAppTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 0.dp)
                                        )
                                        Text(
                                            text = itemTrack.artist,
                                            style = MiuixAppTheme.typography.bodyMedium,
                                            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 0.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Seek bar — 500ms'lik konum tiklerini SADECE bu satır dinler.
            // Sheet'in geri kalanı (kapak grid'i, glow, butonlar) artık her tikte
            // recompose OLMAZ; NowPlaying kasmasının ana çözümü budur.
            SeekBarRow(playerViewModel = playerViewModel)

            // Action buttons row: Queue, Lyrics, +/check
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showQueue by remember { mutableStateOf(false) }
                var showPlaylists by remember { mutableStateOf(false) }
                var showLyrics by remember { mutableStateOf(false) }

                // State'ler if dışında — Compose composition kuralı
                val lyricsSheetScope = rememberCoroutineScope()
                val trackId = track?.id ?: -1L
                val isLiked = trackId > 0 && state.likedSongIds.contains(trackId)

                // MADDE 10 — bu iki tuş Blur AÇILINCA arkalarında cam bir daire
                // kazanıyordu (`backgroundColor = Color.Transparent` verilince
                // BouncyIconButton `blurSurfaceCompact` uyguluyor). Oysa bu tuşların
                // blur ile bir ilgisi yok; her iki durumda da sade ikon olmalılar.
                BouncyIconButton(
                    onClick = {
                        showQueue = true
                    },
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = strings.queue,
                    tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
                BouncyIconButton(
                    onClick = {
                        showLyrics = true
                    },
                    icon = Icons.Filled.Lyrics,
                    contentDescription = strings.lyrics,
                    tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
                // MADDE 10 — beğen/ekle tuşu da Blur ile birlikte görünüm değiştiriyordu.
                // Artık her iki durumda da aynı düz dolgulu daire.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLiked) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MiuixAppTheme.colorScheme.surfaceVariant
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
                        tint = if (isLiked) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (showQueue) {
                    MiuixDrawer(
                        onDismissRequest = { showQueue = false },
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
                    // Konum tikleri sadece lyrics drawer açıkken ve sadece bu blokta dinlenir
                    // (senkronize sözlerde aktif satırın takibi için zaten gerekli).
                    val lyricsProgress by playerViewModel.progress.collectAsState()
                    val currentMs = lyricsProgress.positionMs
                    val activeIndex = if (syncedLyrics.isNotEmpty()) {
                        syncedLyrics.indexOfLast { it.timeMs <= currentMs }.coerceAtLeast(0)
                    } else -1

                    // MADDE 7 — düzenleme ve otomatik kaydırma (autoscroll) durumu.
                    var isEditing by remember { mutableStateOf(false) }
                    var isAutoScroll by remember { mutableStateOf(true) }
                    var editText by remember { mutableStateOf("") }
                    val isDarkTheme = MiuixAppTheme.colorScheme.background.luminance() < 0.5f

                    // MADDE 7 — autoscroll yalnızca açıkken ve sözler senkronizeyken
                    // aktif satıra kaydırır.
                    LaunchedEffect(activeIndex, isAutoScroll, syncedLyrics.size) {
                        if (isAutoScroll && activeIndex >= 0 && syncedLyrics.isNotEmpty()) {
                            val layoutInfo = lyricListState.layoutInfo
                            val totalCount = layoutInfo.totalItemsCount
                            if (totalCount > 0 && activeIndex < totalCount) {
                                val viewportHeight = layoutInfo.viewportSize.height
                                val targetOffset = -(viewportHeight / 3)
                                runCatching {
                                    lyricListState.animateScrollToItem(
                                        index = activeIndex,
                                        scrollOffset = targetOffset
                                    )
                                }
                            } else {
                                kotlinx.coroutines.delay(100)
                                runCatching {
                                    val vHeight = lyricListState.layoutInfo.viewportSize.height
                                    lyricListState.scrollToItem(activeIndex, -(vHeight / 3))
                                }
                            }
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

                    MiuixDrawer(
                        onDismissRequest = { showLyrics = false },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .fillMaxHeight(0.72f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                // MADDE 7 — içe aktarma (import) tuşu artık HER ZAMAN solda
                                // görünür (daha önce sözler içe aktarılınca kayboluyordu).
                                dev.shephard.player.ui.miuix.IconButton(
                                    onClick = { lyricsFilePicker.launch(arrayOf("text/*", "application/octet-stream")) }
                                ) {
                                    Icon(
                                        Icons.Filled.FolderOpen,
                                        contentDescription = strings.addLyricsFromFile,
                                        tint = MiuixAppTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    strings.lyrics,
                                    style = MiuixAppTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                                )
                                // MADDE 7 — autoscroll tuşu (edit'in hemen solunda).
                                // Açıkken tema rengi; kapalıyken beyaz (aydınlık modda siyah).
                                dev.shephard.player.ui.miuix.IconButton(
                                    onClick = { isAutoScroll = !isAutoScroll }
                                ) {
                                    Icon(
                                        Icons.Filled.VerticalAlignCenter,
                                        contentDescription = strings.autoscroll,
                                        tint = if (isAutoScroll) MiuixAppTheme.colorScheme.primary
                                        else if (isDarkTheme) Color.White else Color.Black
                                    )
                                }
                                // MADDE 7 — düzenleme tuşu (Miuix edit simgesi).
                                dev.shephard.player.ui.miuix.IconButton(
                                    onClick = {
                                        if (!isEditing) {
                                            editText = state.lyrics.joinToString("\n")
                                            isEditing = true
                                        } else {
                                            val lines = editText.lines()
                                                .map { it.trimEnd() }
                                                .filter { it.isNotBlank() }
                                            playerViewModel.setManualLyrics(lines)
                                            isEditing = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = strings.edit,
                                        tint = if (isEditing) MiuixAppTheme.colorScheme.primary
                                        else MiuixAppTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (isEditing) {
                                dev.shephard.player.ui.miuix.OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    label = { Text(strings.lyrics) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            } else if (state.lyrics.isEmpty()) {
                                Text(strings.noLyricsFound, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                val currentTrack = state.currentTrack
                                if (currentTrack != null) {
                                    if (isDownloading) {
                                        dev.shephard.player.ui.miuix.CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MiuixAppTheme.colorScheme.primary)
                                    } else {
                                        dev.shephard.player.ui.miuix.FilledTonalButton(
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
                                        Text(it, color = MiuixAppTheme.colorScheme.error, style = MiuixAppTheme.typography.bodySmall)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = lyricListState,
                                    modifier = Modifier.overScrollVertical().weight(1f)
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
                                            style = MiuixAppTheme.typography.bodyMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isActive) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onSurface
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
                    .padding(top = 8.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
                    .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRemixed by playerViewModel.isRemixed.collectAsState()
                BouncyIconButton(
                    onClick = { playerViewModel.remixQueue() },
                    icon = Icons.Filled.Shuffle,
                    contentDescription = strings.remix,
                    tint = if (isRemixed) MiuixAppTheme.colorScheme.primary else MiuixAppTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
                BouncyIconButton(
                    onClick = { playerViewModel.skipToPrevious() },
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = strings.previous,
                    tint = MiuixAppTheme.colorScheme.onBackground,
                    iconSize = 36.dp
                )
                // MADDE 10 — ÇAL/DURAKLAT tuşu Blur açılınca `blurSurface(GlassTint.ACCENT)`
                // ile yarı saydam ve KOYU bir daireye dönüşüyordu; üstündeki ikon ise
                // `onPrimary` (açık yeşil accent'te #111111, yani koyu) olduğu için tuş
                // neredeyse okunmaz hale geliyordu. Bu tuş çalar arayüzünün ana eylemi;
                // blur ayarından etkilenmemeli. Artık her zaman düz accent dolgulu.
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MiuixAppTheme.colorScheme.primary)
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
                            tint = MiuixAppTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                BouncyIconButton(
                    onClick = { playerViewModel.skipToNext() },
                    icon = Icons.Filled.SkipNext,
                    contentDescription = strings.next,
                    tint = MiuixAppTheme.colorScheme.onBackground,
                    iconSize = 36.dp
                )
                BouncyIconButton(
                    onClick = { playerViewModel.cycleRepeatMode() },
                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = strings.repeat,
                    tint = if (state.repeatMode != RepeatMode.OFF) MiuixAppTheme.colorScheme.primary
                    else MiuixAppTheme.colorScheme.onSurfaceVariant,
                    iconSize = 28.dp
                )
            }
        }
    }
}

/**
 * Ambient glow arka planı — amplitude flow'unu (≈80ms tik) yalnızca bu composable toplar,
 * böylece NowPlayingSheet'in geri kalanı müzik çalarken sürekli recompose olmaz.
 *
 * Siyah şerit düzeltmesi: eski kod pulse'ı `0.85f + amplitude * 0.15f` olarak hesaplıyordu.
 * Şarkı DURDURULUNCA amplitude 0'a düşüyor, glow katmanı merkezden %85'e küçülüyor ve üstte
 * (durum çubuğunun olduğu yerde) ekran yüksekliğinin %7.5'i kadar saf siyah arka plan açığa
 * çıkıyordu — kullanıcının gördüğü "siyah şerit çizgi" buydu. Ölçek artık hiçbir zaman
 * 1.0'ın altına inmez (1.0f + amplitude * 0.12f), yani glow her zaman ekranı tam kaplar.
 *
 * Gradient optimizasyonu: tam ekran iki katmana uygulanan 96px/128px RenderEffect blur'ları
 * kaldırıldı. Gradient'ler zaten yumuşak geçişli olduğu için blur görsel olarak fark
 * yaratmıyordu ama her frame'de iki tam ekran blur pass'i GPU'ya mal oluyordu.
 */
@Composable
private fun AmbientGlowBackground(
    playerViewModel: PlayerViewModel,
    glow: Color,
    hasTrack: Boolean
) {
    val amplitude by playerViewModel.amplitude.collectAsState()

    // pulse sadece graphicsLayer scale'ini etkiler; Brush sabit kalır (allocation yok).
    val pulse by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1.0f + amplitude * 0.12f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "glowPulse"
    )
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (!hasTrack) 0f else 0.55f + amplitude * 0.2f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "glowAlpha"
    )

    // Brush'lar glow rengine göre memoize — amplitude değişimlerinde yeniden oluşturulmaz.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = glowAlpha
                scaleX = pulse
                scaleY = pulse
            }
            .background(verticalGlowBrush)
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = glowAlpha * 0.85f }
            .background(radialGlowBrush)
    )
}

/**
 * Seek bar + süre etiketleri. [PlayerViewModel.progress] flow'unu (500ms tik) YALNIZCA bu
 * composable toplar — NowPlayingSheet'in geri kalanı konum güncellemelerinden tamamen izole.
 */
@Composable
private fun SeekBarRow(playerViewModel: PlayerViewModel) {
    val progress by playerViewModel.progress.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        MinimalSeekBar(
            progress = if (progress.durationMs > 0)
                progress.positionMs.toFloat() / progress.durationMs.toFloat() else 0f,
            onSeekPreview = { fraction ->
                playerViewModel.onSeekPreview((fraction * progress.durationMs).toLong())
            },
            onSeekFinished = { fraction ->
                playerViewModel.onSeekCommit((fraction * progress.durationMs).toLong())
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatMillis(progress.positionMs),
                style = MiuixAppTheme.typography.labelMedium,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatMillis(progress.durationMs),
                style = MiuixAppTheme.typography.labelMedium,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant
            )
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
    val addToPlaylistLiquidGlassOn = LocalBlurEnabled.current

    MiuixDrawer(
        onDismissRequest = onDismiss,
    ) {
        // MADDE 6 — "add to playlist" drawer'ı da alçaltıldı.
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
                    .overScrollVertical()
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
                                .background(if (isLiked) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f) else MiuixAppTheme.colorScheme.surfaceVariant),
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
                            Text("${pl.trackIds.size} ${strings.trackCount}", style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(if (containsTrack) MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f) else MiuixAppTheme.colorScheme.surfaceVariant),
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
    // MADDE 6 — sıra (queue) drawer'ı ekranın %88'ini kaplıyordu. Kaydırılabilir bir
    // liste içerdiği için sınırlı bir yüksekliğe ihtiyacı var; %66 hem listeyi rahat
    // gösteriyor hem de arkadaki çalar ekranını görünür bırakıyor.
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        val absOffset = kotlin.math.abs(offsetX)
        val progress = (absOffset / swipeThresholdPx).coerceIn(0f, 1f)
        if (absOffset > 10f) {
            val isSwipeRight = offsetX > 0f
            val isSwipeLeft = offsetX < 0f
            val isThresholdReached = absOffset >= swipeThresholdPx

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            isSwipeRight -> Color(0xFFE53935).copy(alpha = 0.16f * progress)
                            isSwipeLeft -> MiuixAppTheme.colorScheme.primary.copy(alpha = 0.16f * progress)
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isSwipeRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                val iconScale by animateFloatAsState(
                    targetValue = if (isThresholdReached) 1.25f else (0.8f + 0.2f * progress),
                    label = "swipeIconScale"
                )
                Icon(
                    imageVector = if (isSwipeRight) Icons.Filled.Delete else Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = if (isSwipeRight) "Remove" else "Pin to play next",
                    tint = if (isSwipeRight) Color(0xFFE53935) else MiuixAppTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = progress
                        }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = with(density) { offsetX.toDp() })
                .shadow(elevation, RoundedCornerShape(12.dp))
                .zIndex(if (isDragged) 1f else 0f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        isDragged -> MiuixAppTheme.colorScheme.primary.copy(alpha = 0.18f)
                        isPlaying -> MiuixAppTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else -> MiuixAppTheme.colorScheme.background
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
                    .background(MiuixAppTheme.colorScheme.surfaceVariant),
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
            Text(
                duration,
                style = MiuixAppTheme.typography.labelSmall,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            if (!isPlaying) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Reorder",
                    tint = MiuixAppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .then(dragHandleModifier)
                )
            }
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
