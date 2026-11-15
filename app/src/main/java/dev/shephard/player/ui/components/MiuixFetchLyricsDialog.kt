// SPDX-License-Identifier: GPL-3.0-only
package dev.shephard.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.ui.i18n.LocalStrings
import dev.shephard.player.ui.miuix.MiuixAppTheme
import dev.shephard.player.ui.miuix.MiuixDialog
import dev.shephard.player.ui.miuix.Switch
import dev.shephard.player.ui.miuix.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxWidth

/** Miuix-window counterpart of the automatic missing-lyrics prompt. */
@Composable
fun MiuixFetchLyricsDialog(song: AudioTrack?, dontShowAgain: Boolean,
    onDontShowAgainChange: (Boolean) -> Unit, onSearch: (Boolean) -> Unit,
    onImport: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val cs = MiuixAppTheme.colorScheme
    var forcePick by remember { mutableStateOf(false) }
    MiuixDialog(onDismissRequest = onDismiss, title = s.lyrics, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(song?.title ?: s.noLyricsFound, color = cs.onBackground, fontWeight = FontWeight.Bold,
                style = MiuixAppTheme.typography.titleLarge, maxLines = 2)
            song?.artist?.let { Text(it, color = cs.primary, style = MiuixAppTheme.typography.bodyMedium) }
            Text(s.lyricsFindPrompt, color = cs.onSurfaceVariant, style = MiuixAppTheme.typography.bodyMedium)
            @Composable fun Toggle(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceVariant)
                    .clickable { change(!checked) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = cs.onBackground, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = cs.onSurfaceVariant, style = MiuixAppTheme.typography.bodySmall)
                    }
                    Switch(checked = checked, onCheckedChange = change)
                }
            }
            Toggle(s.lyricsShowOptionsTitle, s.lyricsShowOptionsSubtitle, forcePick) { forcePick = it }
            Toggle(s.lyricsNeverPrompt, s.lyricsNeverPromptDescription, dontShowAgain, onDontShowAgainChange)
        }
    }, buttons = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.search, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.primary)
                .clickable { onSearch(forcePick) }.padding(14.dp), color = cs.onPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Text(s.lyricsImport, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceVariant)
                .clickable(onClick = onImport).padding(14.dp), color = cs.onBackground, textAlign = TextAlign.Center)
            Text(s.cancel, modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(12.dp),
                color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    })
}
