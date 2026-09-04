package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.ui.i18n.LocalStrings

@Composable
fun PlaybackSettingsSheet(
    playerViewModel: PlayerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by playerViewModel.uiState.collectAsState()
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixAppTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.backContentDescription,
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

        ListeningStatsCard(totalListeningMs = state.totalListeningMs)
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
            // Real Miuix switch colors: white thumb, blue track when on,
            // gray track when off — follows the custom colors in Monet mode.
            colors = SwitchDefaults.colors()
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
