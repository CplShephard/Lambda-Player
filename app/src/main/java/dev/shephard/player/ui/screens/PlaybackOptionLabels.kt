package dev.shephard.player.ui.screens

import dev.shephard.player.player.LyricsAutoSearchMode
import dev.shephard.player.player.VolumeNormalization
import dev.shephard.player.ui.i18n.Strings

/** Display label for an automatic lyrics search mode. */
fun lyricsAutoSearchLabel(mode: LyricsAutoSearchMode, strings: Strings): String = when (mode) {
    LyricsAutoSearchMode.OFF -> strings.lyricsAutoSearchOff
    LyricsAutoSearchMode.EMBEDDED_FIRST -> strings.lyricsAutoSearchEmbedded
    LyricsAutoSearchMode.BEST_MATCH -> strings.lyricsAutoSearchBestMatch
    LyricsAutoSearchMode.ONLINE_FIRST -> strings.lyricsAutoSearchOnline
}

/** Display label for a volume normalization preset. */
fun volumeNormalizationLabel(mode: VolumeNormalization, strings: Strings): String = when (mode) {
    VolumeNormalization.NONE -> strings.volumeNormalizationNone
    VolumeNormalization.LOW -> strings.volumeNormalizationLow
    VolumeNormalization.NORMAL -> strings.volumeNormalizationNormal
    VolumeNormalization.HIGH -> strings.volumeNormalizationHigh
}
