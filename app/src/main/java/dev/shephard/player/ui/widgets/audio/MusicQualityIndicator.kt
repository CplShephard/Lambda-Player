// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.widgets.audio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shephard.player.R
import dev.shephard.player.ui.theme.YosRoundedCornerShape
import dev.shephard.player.ui.widgets.basic.YosWrapper
import dev.shephard.player.ui.widgets.effects.overlayEffect
import dev.shephard.player.ui.nowplaying.util.MediaViewModelObject

/**
 * Music quality pill that floats under the seek bar of the now-playing
 * sheet.
 *
 * The widget inspects the audio metadata exposed by
 * [MediaViewModelObject] (which is fed from Media3 inside the sheet) and
 * shows a "Lossless" / "Hi-Res" / "Dolby" badge accordingly. When the
 * metadata is unknown — which is the common case for local files on
 * Android because Media3 only reports bitrate for streamed content — the
 * pill stays hidden, so the time labels below the slider stay correctly
 * centred.
 */
@Composable
fun MusicQualityIndicator() {
    val bitrate by MediaViewModelObject.bitrate
    val samplingRate by MediaViewModelObject.samplingRate
    val isDolby by MediaViewModelObject.isDolby

    val isLossless = bitrate >= 700 && samplingRate >= 44100 && bitrate > 0
    val isHiRes = bitrate >= 2000 && samplingRate >= 96000

    if (!isLossless && !isHiRes && !isDolby) {
        // No metadata — keep the slot empty so the time labels stay
        // centred.
        Box(modifier = Modifier.fillMaxWidth().height(22.dp))
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isDolby,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "qualityPill",
        ) { dolby ->
            if (dolby) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nowplaying_dolby_atmos),
                    contentDescription = "Dolby Atmos",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .height(19.dp)
                        .overlayEffect()
                        .padding(horizontal = 4.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .overlayEffect()
                        .background(
                            color = Color(0x14FFFFFF),
                            shape = YosRoundedCornerShape(5.dp),
                        )
                        .height(20.dp)
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        YosWrapper {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_quality_lossless),
                                contentDescription = "Lossless",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.height(9.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isHiRes) "Hi-Res" else "Lossless",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                        )
                    }
                }
            }
        }
    }
}
