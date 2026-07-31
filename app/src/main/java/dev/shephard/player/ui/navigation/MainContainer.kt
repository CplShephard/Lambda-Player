// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.MiuixAppTheme
import top.yukonga.miuix.kmp.basic.Scaffold
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.glass.FloatingBottomBar
import dev.shephard.player.ui.glass.FloatingBottomBarItem
import dev.shephard.player.ui.glass.FloatingBottomBarMode
import dev.shephard.player.ui.glass.LocalAppBackdrop
import dev.shephard.player.ui.glass.LocalContentBackdrop
import dev.shephard.player.ui.glass.LocalBlurEnabled
import dev.shephard.player.ui.glass.isLiquidGlassSupported
import dev.shephard.player.ui.glass.miuixBlurSurface
import dev.shephard.player.ui.glass.rememberAppBlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.screens.NowPlayingSheet

// Açılış: yumuşak yaylı kayma
private val nowPlayingEnterSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 180f
)
// Kapanış: fazla overshoot olmadan hızlı ve smooth
private val nowPlayingExitSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 480f
)

@Composable
fun MainContainer(
    playerViewModel: PlayerViewModel = viewModel(),
    initialAudioUri: android.net.Uri? = null
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val languageCode by prefs.language.collectAsState(initial = "en")
    val strings = remember(languageCode) { stringsFor(languageCode) }
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = 0.55f)

    val blurEnabled = LocalBlurEnabled.current

    val backgroundBackdrop = rememberAppBlurBackdrop(blurEnabled)
    val contentBackdrop = rememberAppBlurBackdrop(blurEnabled)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalAppBackdrop provides backgroundBackdrop,
        LocalContentBackdrop provides contentBackdrop,
    ) {
        var showNowPlaying by remember { mutableStateOf(false) }

        val hasMiniPlayer by remember(playerViewModel) {
            playerViewModel.uiState
                .map { it.currentTrack != null }
                .distinctUntilChanged()
        }.collectAsState(initial = false)

        LaunchedEffect(initialAudioUri) {
            if (initialAudioUri != null) {
                playerViewModel.playExternalUri(initialAudioUri)
                delay(120)
                showNowPlaying = true
            }
        }

        BackHandler(enabled = showNowPlaying) {
            showNowPlaying = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixAppTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backgroundBackdrop != null) Modifier.layerBackdrop(backgroundBackdrop)
                        else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixAppTheme.colorScheme.background)
                )
                if (wallpaper.isNotEmpty()) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixAppTheme.colorScheme.background.copy(alpha = wallpaperBrightness))
                    )
                }
            }

            NavGraph(
                playerViewModel = playerViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (contentBackdrop != null) Modifier.layerBackdrop(contentBackdrop)
                        else Modifier
                    ),
                hasMiniPlayer = hasMiniPlayer,
                onTrackClick = { tracks, index, playlistName ->
                    playerViewModel.setQueueAndPlay(tracks, index, playlistName)
                },
                onPlaylistRemixClick = { tracks, playlistName ->
                    playerViewModel.setQueueAndPlayRemixed(tracks, playlistName)
                },
                onOpenNowPlaying = { showNowPlaying = true }
            )

            AnimatedVisibility(
                visible = showNowPlaying,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(0)),
                modifier = Modifier.fillMaxSize()
            ) {
                NowPlayingSheet(
                    playerViewModel = playerViewModel,
                    onDismiss = { showNowPlaying = false }
                )
            }
        }
    }
}

@Composable
fun MiniPlayerHost(
    playerViewModel: PlayerViewModel,
    visible: Boolean,
    onOpenNowPlaying: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(0.85f, 300f)
        ),
        exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        val playerState by playerViewModel.uiState.collectAsState()
        MiniPlayer(
            state = playerState,
            progressFlow = playerViewModel.progress,
            onClick = onOpenNowPlaying,
            onPlayPauseClick = { playerViewModel.togglePlayPause() },
            onNextClick = { playerViewModel.skipToNext() },
            onPreviousClick = { playerViewModel.skipToPrevious() }
        )
    }
}

@Composable
fun BrandHeader(currentPageIndex: Int) {
    val strings = LocalStrings.current
    val sectionTitle = when (currentPageIndex) {
        0 -> strings.music
        1 -> strings.playlists
        2 -> strings.settings
        else -> null
    }

    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    val blurOn = LocalBlurEnabled.current
    val headerBackdrop = LocalAppBackdrop.current
    val headerShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .then(
                if (blurOn && headerBackdrop != null) {
                    Modifier.miuixBlurSurface(
                        backdrop = headerBackdrop,
                        shape = headerShape,
                        blurRadius = 14f,
                        tintAlpha = 0.46f,
                        fallbackColor = MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                } else {
                    Modifier.background(MiuixAppTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), headerShape)
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = strings.appName.uppercase(),
                    style = MiuixAppTheme.typography.titleLarge.copy(
                        fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MiuixAppTheme.colorScheme.onBackground
                )
                if (versionName.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = versionName,
                        style = MiuixAppTheme.typography.titleLarge.copy(
                            fontFamily = dev.shephard.player.ui.theme.BrandFontFamily,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MiuixAppTheme.colorScheme.primary
                    )
                }
            }
            if (sectionTitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sectionTitle,
                    style = MiuixAppTheme.typography.titleMedium,
                    color = MiuixAppTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FloatingDock(
    selectedIndex: Int,
    onSelectPage: (Int) -> Unit
) {
    val blurOn = LocalBlurEnabled.current
    val backdrop = LocalContentBackdrop.current

    val mode = when {
        blurOn && backdrop != null && isLiquidGlassSupported -> FloatingBottomBarMode.LiquidGlass
        blurOn && backdrop != null -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }

    val strings = LocalStrings.current
    val dummyBackdrop = rememberLayerBackdrop()
    val effectiveBackdrop = backdrop ?: dummyBackdrop

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingBottomBar(
            selectedIndex = { selectedIndex },
            onSelected = { index -> onSelectPage(index) },
            backdrop = effectiveBackdrop,
            tabsCount = bottomNavDestinations.size,
            mode = mode
        ) {
            bottomNavDestinations.forEachIndexed { index, dest ->
                val selected = index == selectedIndex
                val label = when (dest) {
                    Destination.Music -> strings.music
                    Destination.Playlists -> strings.playlists
                    Destination.Settings -> strings.settings
                }
                FloatingBottomBarItem(
                    onClick = { onSelectPage(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label
                    )
                    Text(
                        text = label,
                        style = MiuixAppTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
