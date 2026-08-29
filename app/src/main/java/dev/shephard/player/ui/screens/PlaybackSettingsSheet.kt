package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import dev.shephard.player.player.PreferencesManager
import dev.shephard.player.ui.miuix.HorizontalDivider
import dev.shephard.player.ui.miuix.Icon
import dev.shephard.player.ui.miuix.IconButton
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.Switch
import dev.shephard.player.ui.miuix.SwitchDefaults
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

@Composable
fun PlaybackSettingsSheet(
    playerViewModel: PlayerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val translation by prefs.nowPlayingTranslation.collectAsState(initial = false)
    val lyricBlur by prefs.lyricBlurEffect.collectAsState(initial = true)
    val showVolumeBar by prefs.nowPlayingShowVolumeBar.collectAsState(initial = false)
    val backgroundEffect by prefs.nowplayingBackgroundEffect.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixAppTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MiuixAppTheme.colorScheme.onBackground
                )
            }
            Text(
                text = strings.playbackSettings,
                style = MiuixAppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MiuixAppTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Audio",
                style = MiuixAppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MiuixAppTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            SettingToggleRow(
                title = strings.crossfade,
                subtitle = "Smoothly blend the end of a track into the next",
                checked = state.crossfadeEnabled,
                onCheckedChange = { playerViewModel.setCrossfadeEnabled(it) }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            SettingToggleRow(
                title = strings.gapless,
                subtitle = "Remove silence between consecutive tracks",
                checked = state.gaplessEnabled,
                onCheckedChange = { playerViewModel.setGaplessEnabled(it) }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            SettingToggleRow(
                title = strings.playWithOthers,
                subtitle = "Allow audio from other apps to mix with Lambda Player",
                checked = state.playWithOthers,
                onCheckedChange = { playerViewModel.setPlayWithOthers(it) }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            Text(
                text = "Now playing",
                style = MiuixAppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MiuixAppTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            SettingToggleRow(
                title = "Lyric translation",
                subtitle = "Show translated lyrics when available",
                checked = translation,
                onCheckedChange = { scope.launch { prefs.setNowPlayingTranslation(it) } }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            SettingToggleRow(
                title = "Lyric blur effect",
                subtitle = "Blur inactive lyric lines for a softer look",
                checked = lyricBlur,
                onCheckedChange = { scope.launch { prefs.setLyricBlurEffect(it) } }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            SettingToggleRow(
                title = "Show volume bar",
                subtitle = "Display a volume slider above the lyric page",
                checked = showVolumeBar,
                onCheckedChange = { scope.launch { prefs.setNowPlayingShowVolumeBar(it) } }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            SettingToggleRow(
                title = "Background effect",
                subtitle = "Animated colour wash behind the now-playing sheet",
                checked = backgroundEffect,
                onCheckedChange = { scope.launch { prefs.setNowplayingBackgroundEffect(it) } }
            )

            HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

            ListeningStatsCard(totalListeningMs = state.totalListeningMs)
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MiuixAppTheme.typography.bodyLarge,
                color = MiuixAppTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MiuixAppTheme.typography.bodyMedium,
                color = MiuixAppTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MiuixAppTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun ListeningStatsCard(totalListeningMs: Long) {
    val strings = LocalStrings.current
    val totalSeconds = totalListeningMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val formatted = buildList {
        if (hours > 0) add("$hours${strings.hourShort}")
        if (minutes > 0 || hours > 0) add("$minutes${strings.minuteShort}")
        add("$seconds${strings.secondShort}")
    }.joinToString(" ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixAppTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Headphones,
                contentDescription = null,
                tint = MiuixAppTheme.colorScheme.primary
            )
            Text(
                text = strings.totalListeningTime,
                style = MiuixAppTheme.typography.bodyLarge,
                color = MiuixAppTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Text(
            text = formatted,
            style = MiuixAppTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MiuixAppTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = strings.totalListeningDescription,
            style = MiuixAppTheme.typography.bodyMedium,
            color = MiuixAppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
