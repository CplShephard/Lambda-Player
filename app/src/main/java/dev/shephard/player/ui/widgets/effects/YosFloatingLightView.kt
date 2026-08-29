// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package dev.shephard.player.ui.widgets.effects

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.applyCanvas
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.shephard.player.ui.screens.NowPlayingPage
import dev.shephard.player.ui.widgets.basic.YosWrapper

/**
 * Floating-light background effect used by the Flamingo now-playing sheet.
 *
 * This is a streamlined port for Lambda Player: it loads the current
 * track's album art, applies a saturation + blur treatment to produce
 * the colourful backdrop, and animates an extra darkened overlay in and
 * out depending on the active [NowPlayingPage]. The original
 * KenBurnsView-driven pan/zoom animation is intentionally not ported —
 * it depends on RenderScript (deprecated) and an external library that
 * would require a separate dependency.
 */
@Composable
fun YosFloatingLight(
    modifier: Modifier,
    album: () -> Uri?,
    isPlaying: () -> Boolean,
    nowPage: () -> String,
    showMiniPlayer: () -> Boolean,
) {
    val context = LocalContext.current
    val imageLoader = remember { ImageLoader(context) }
    var processed by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(album()) {
        val uri = album() ?: run {
            processed = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(uri).build()
            val src = imageLoader.execute(request).drawable?.let { d ->
                android.graphics.drawable.BitmapDrawable.createFromStream(
                    context.contentResolver.openInputStream(uri), null,
                )?.bitmap
            }
            processed = src?.let { imageResolve(it) }
        }
    }

    val lossEffect = remember(nowPage) {
        derivedStateOf { nowPage() != NowPlayingPage.Lyric }
    }

    val useBackground = remember(album) {
        derivedStateOf { album() == null }
    }

    val alpha by animateFloatAsState(
        targetValue = if (lossEffect.value) 0.618f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "yosFloatingLightAlpha",
    )

    YosWrapper {
        AsyncImage(
            model = processed,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    onDrawBehind {
                        if (useBackground.value) drawRect(Color.Black)
                    }
                },
        )
    }

    YosWrapper {
        AsyncImage(
            model = processed,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    this.alpha = alpha
                },
            colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay),
        )
    }
}

private fun imageResolve(image: Bitmap, moreLight: Boolean = false): Bitmap {
    val resized = image.copy(Bitmap.Config.ARGB_8888, true)
    resized.applyCanvas {
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        val saturationMatrix = ColorMatrix().apply { setSaturation(3f) }
        paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
        drawBitmap(resized, 0f, 0f, paint)

        if (moreLight) {
            drawColor((0x1AFFFFFF).toInt())
            drawColor((0xFFFFFFFF).toInt(), PorterDuff.Mode.OVERLAY)
            drawColor((0x52FFFFFF).toInt())
            drawColor((0xBFFFFFFF).toInt(), PorterDuff.Mode.OVERLAY)
        } else {
            drawColor((0x33000000).toInt(), PorterDuff.Mode.OVERLAY)
            drawColor((0x40000000).toInt())
        }
    }
    return resized
}
