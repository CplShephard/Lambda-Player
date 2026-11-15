package dev.shephard.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shephard.player.player.PlayerViewModel
import dev.shephard.player.player.LyricsAutoSearchMode
import dev.shephard.player.player.VolumeNormalization
import dev.shephard.player.ui.i18n.LocalStrings
import androidx.compose.ui.platform.LocalContext

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
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
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

        val lyricsModes = LyricsAutoSearchMode.entries
        MiuixSpinnerRow(
            title = strings.lyricsAutoSearchTitle,
            summary = strings.lyricsAutoSearchDescription,
            items = lyricsModes.map { lyricsAutoSearchLabel(it, strings) },
            selectedIndex = lyricsModes.indexOf(state.lyricsAutoSearchMode),
            onSelectedIndexChange = { playerViewModel.setLyricsAutoSearchMode(lyricsModes[it]) }
        )

        HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

        val normalizationModes = VolumeNormalization.entries
        MiuixSpinnerRow(
            title = strings.volumeNormalizationTitle,
            summary = strings.volumeNormalizationDescription,
            items = normalizationModes.map { volumeNormalizationLabel(it, strings) },
            selectedIndex = normalizationModes.indexOf(state.volumeNormalization),
            onSelectedIndexChange = { playerViewModel.setVolumeNormalization(normalizationModes[it]) }
        )

        HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

        // Lyrics translation language (QQ Music style)
        val context = androidx.compose.ui.platform.LocalContext.current
        val prefs = remember(context) { dev.shephard.player.player.PreferencesManager(context) }
        val transLang by prefs.lyricsTranslationLanguage.collectAsState(initial = "off")
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val transOptions = dev.shephard.player.player.LyricsTranslator.supportedLanguages
        MiuixSpinnerRow(
            title = strings.lyricsTranslationLanguage,
            summary = strings.lyricsTranslationLanguageDescription,
            items = transOptions.map { dev.shephard.player.player.LyricsTranslator.labelFor(it.first, strings) },
            selectedIndex = transOptions.indexOfFirst { it.first == transLang }.coerceAtLeast(0),
            onSelectedIndexChange = { idx ->
                val code = transOptions[idx].first
                scope.launch { prefs.setLyricsTranslationLanguage(code); prefs.setLyricsTranslationVisible(code != "off") }
                if (code != "off") playerViewModel.ensureLyricsTranslation(code)
            }
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

        HorizontalDivider(color = MiuixAppTheme.colorScheme.surfaceVariant)

        // Orphan data cleanup (GEREKSIZ metadata) with progress bar to 100%
        OrphanCleanupSection(playerViewModel = playerViewModel)
    }
}

@Composable
private fun OrphanCleanupSection(playerViewModel: PlayerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = LocalStrings.current
    var showDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var progress by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var report by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<PlayerViewModel.OrphanCleanupReport?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val libraryVm: dev.shephard.player.player.LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val libraryTracks by libraryVm.tracks.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = "Storage orphan cleanup", style = MiuixAppTheme.typography.bodyLarge, color = MiuixAppTheme.colorScheme.onBackground)
                Text(text = "Scan DataStore for tracks not on device and remove only unnecessary (GEREKSIZ) leftover metadata", style = MiuixAppTheme.typography.bodyMedium, color = MiuixAppTheme.colorScheme.onSurfaceVariant)
                if (report != null) {
                    Text(text = report!!.summary(), style = MiuixAppTheme.typography.bodyMedium, color = MiuixAppTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                }
            }
            MiuixAppTheme.let {
                top.yukonga.miuix.kmp.basic.Button(
                    onClick = {
                        progress = 0; report = null; showDialog = true
                        scope.launch {
                            val ids = libraryTracks.map { it.id.toString() }.toSet()
                            val stable = libraryTracks.map { dev.shephard.player.data.TrackFingerprint.stableKey(it) }.toSet()
                            val res = playerViewModel.scanAndCleanOrphanedData(ids, stable) { p -> progress = p }
                            report = res
                            kotlinx.coroutines.delay(400)
                            showDialog = false
                            android.widget.Toast.makeText(context, res.summary(), android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                ) { Text("Scan & clean") }
            }
        }
        if (report != null) {
            Text(text = "${report!!.totalRemoved} orphan items removed — listening ${report!!.removedListening}, lyrics ${report!!.removedLyrics}", style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
    if (showDialog) {
        OrphanCleanupProgressDialog(progress = progress, onDismiss = { showDialog = false })
    }
}

@Composable
private fun OrphanCleanupProgressDialog(progress: Int, onDismiss: () -> Unit) {
    var show by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(progress) { if (progress >= 100) kotlinx.coroutines.delay(350) }
    dev.shephard.player.ui.miuix.MiuixDialog(
        onDismissRequest = { show = false; onDismiss() },
        title = "Scanning DataStore… ${progress}%",
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
                // Linear progress bar filling to 100%
                Box(
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)).background(MiuixAppTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(fraction = progress / 100f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(MiuixAppTheme.colorScheme.primary))
                }
                Text(text = "${progress}% — checking listening stats, lyrics, overrides…", style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                Text(text = "Only GEREKSIZ (orphaned) entries will be deleted from DataStore directory", style = MiuixAppTheme.typography.bodySmall, color = MiuixAppTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp))
            }
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                top.yukonga.miuix.kmp.basic.TextButton(onClick = { show = false; onDismiss() }, text = "Cancel")
            }
        }
    )
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
