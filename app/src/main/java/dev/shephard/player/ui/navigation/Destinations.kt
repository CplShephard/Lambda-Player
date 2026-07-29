package dev.shephard.player.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

sealed class Destination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Music : Destination(
        route = "music",
        label = "Music",
        selectedIcon = MiuixIcons.Music,
        unselectedIcon = MiuixIcons.Music
    )

    data object Playlists : Destination(
        route = "playlists",
        label = "Playlists",
        selectedIcon = MiuixIcons.Playlist,
        unselectedIcon = MiuixIcons.Playlist
    )

    data object Settings : Destination(
        route = "settings",
        label = "Settings",
        selectedIcon = MiuixIcons.Settings,
        unselectedIcon = MiuixIcons.Settings
    )
}

object SettingsRoutes {
    const val Theme = "settings/theme"
    const val Player = "settings/player"
    const val About = "settings/about"
}

object PlaylistRoutes {
    const val DetailBase = "playlist/detail"
    const val DetailPattern = "playlist/detail/{index}"
    fun detail(index: Int) = "$DetailBase/$index"
}

val bottomNavDestinations = listOf(
    Destination.Music,
    Destination.Playlists,
    Destination.Settings
)
