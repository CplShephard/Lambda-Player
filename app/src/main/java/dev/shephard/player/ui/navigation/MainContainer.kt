package dev.shephard.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import dev.shephard.player.R
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.components.MiniPlayer
import dev.shephard.player.ui.components.bounceClick
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.i18n.stringsFor
import dev.shephard.player.ui.screens.NowPlayingSheet
import kotlinx.coroutines.launch

@Composable
fun MainContainer(
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val languageCode by prefs.language.collectAsState(initial = "en")
    val strings = remember(languageCode) { stringsFor(languageCode) }
    val wallpaper by prefs.wallpaperUri.collectAsState(initial = "")
    val wallpaperBrightness by prefs.wallpaperBrightness.collectAsState(initial = 0.55f)

    CompositionLocalProvider(LocalStrings provides strings) {
        val navController = rememberNavController()
        var showNowPlaying by remember { mutableStateOf(false) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val playerState by playerViewModel.uiState.collectAsState()

        // Bounce-on-tap for the brand header. Driven through Modifier.bounceClick
        // (the same animation now used by every other tappable surface in the app).
        val bounce: () -> Unit = { /* delegated to bounceClick modifier */ }

        // Back: collapse player first, then pop nav stack.
        BackHandler(enabled = showNowPlaying || currentRoute != Destination.Music.route) {
            when {
                showNowPlaying -> showNowPlaying = false
                else -> navController.popBackStack(Destination.Music.route, false)
            }
        }

        val rootScale by animateFloatAsState(
            targetValue = if (showNowPlaying) 0.94f else 1f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
            label = "rootScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Wallpaper background — shown on all main screens (behind the player).
            if (wallpaper.isNotEmpty()) {
                AsyncImage(
                    model = wallpaper,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = rootScale; scaleY = rootScale },
                    contentScale = ContentScale.Crop
                )
                // Dark scrim keeps text/icons readable over any image.
                // Brightness slider lowers this scrim's opacity.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = rootScale; scaleY = rootScale }
                        .background(MaterialTheme.colorScheme.background.copy(alpha = wallpaperBrightness))
                )
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = rootScale; scaleY = rootScale },
                containerColor = if (wallpaper.isEmpty())
                    MaterialTheme.colorScheme.background else Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                topBar = {
                    BrandHeader(currentRoute = currentRoute)
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize(),
                        onTrackClick = { tracks, index ->
                            playerViewModel.setQueueAndPlay(tracks, index)
                            showNowPlaying = true
                        }
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        AnimatedVisibility(
                            visible = playerState.currentTrack != null,
                            enter = fadeIn(tween(200)) + slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(0.85f, 300f)
                            ),
                            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
                            MiniPlayer(
                                state = playerState,
                                onClick = { showNowPlaying = true },
                                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                                onNextClick = { playerViewModel.skipToNext() },
                                onPreviousClick = { playerViewModel.skipToPrevious() }
                            )
                        }

                        FloatingDock(
                            currentRoute = currentRoute,
                            onNavigate = { destination ->
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            AnimatedVisibility(
                visible = showNowPlaying,
                // Slower, smoother spring/ease open/close for a modern feel.
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 180f)
                ) + fadeIn(tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 180f)
                ) + fadeOut(tween(250)),
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
private fun BrandHeader(currentRoute: String?) {
    val strings = LocalStrings.current
    val sectionTitle = when (currentRoute) {
        Destination.Playlists.route -> strings.playlists
        Destination.Settings.route -> strings.settings
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = strings.appName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        // Sadece Playlists ve Settings ekranlarında section başlığı göster
        if (sectionTitle != null) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FloatingDock(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = 0.92f }
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .bounceClick { onNavigate(dest) }
                        .background(
                            color = if (selected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = dest.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
