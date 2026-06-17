package dev.shephard.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Music : Destination(
        route = "music",
        label = "Music",
        selectedIcon = Icons.Filled.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote
    )

    data object Playlists : Destination(
        route = "playlists",
        label = "Playlists",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )

    data object Settings : Destination(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavDestinations = listOf(
    Destination.Music,
    Destination.Playlists,
    Destination.Settings
)
