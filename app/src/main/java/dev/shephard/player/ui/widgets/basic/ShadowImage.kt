package dev.shephard.player.ui.widgets.basic

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import dev.shephard.player.R
import dev.shephard.player.ui.theme.YosRoundedCornerShape
import dev.shephard.player.ui.widgets.effects.ShadowType
import dev.shephard.player.ui.widgets.effects.dropShadow

@Stable
enum class ImageQuality {
    RAW, LOW, HIGH
}

private fun getSizeFromQuality(quality: ImageQuality): Int {
    return when (quality) {
        ImageQuality.RAW -> 0
        ImageQuality.LOW -> 128
        ImageQuality.HIGH -> 400
    }
}

@Composable
fun ShadowImage(
    dataLambda: () -> Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shadowAlpha: Float = 0.23f,
    shadowType: ShadowType = ShadowType.Large,
    shadowOverlay: Boolean = false,
    cornerRadius: Dp = 10.dp,
    imageQuality: ImageQuality
) = YosWrapper {
    val shape = YosRoundedCornerShape(cornerRadius)
    val density = LocalDensity.current
    val shadowAlphaPx = remember(dataLambda()) {
        with(density) { shadowAlpha.dp.toPx() }
    }
    val url = dataLambda()
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(data = url).crossfade(true)
            .error(R.drawable.placeholder_music_default_artwork)
            .placeholder(R.drawable.placeholder_music_default_artwork)
            .fallback(R.drawable.placeholder_music_default_artwork)
            .placeholderMemoryCacheKey(url.toString())
            .diskCacheKey(url.toString())
            .allowHardware(true)
            .crossfade(true)
            .apply {
                if (imageQuality != ImageQuality.RAW) {
                    val size = getSizeFromQuality(imageQuality)
                    this.size(size)
                    if (imageQuality == ImageQuality.LOW) {
                        this.precision(Precision.INEXACT)
                    }
                }
            }
            .build(),
        contentDescription = contentDescription.toString(),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(shape, shadowAlpha, shadowType, shadowOverlay)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
                /*this.shape = shape
                shadowElevation = shadowAlphaPx
                spotShadowColor = Color.Black.copy(alpha = 0.8f)*/
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val outline = shape.createOutline(
                        Size(size.width, size.height),
                        LayoutDirection.Ltr,
                        density
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.Gray.copy(alpha = 0.1f),
                        style = Stroke(width = 12f)
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.Gray.copy(alpha = 0.5f),
                        style = Stroke(width = 12f),
                        blendMode = BlendMode.Overlay
                    )
                }
            }

    )
}

@Composable
fun ShadowImageWithCache(
    dataLambda: () -> Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shadowAlpha: Float = 0.23f,
    shadowType: ShadowType = ShadowType.Large,
    shadowOverlay: Boolean = false,
    cornerRadius: Dp = 8.dp,
    imageQuality: ImageQuality
) = YosWrapper {
    val shape = YosRoundedCornerShape(cornerRadius)
    val url = dataLambda()
    val density = LocalDensity.current
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(data = url).crossfade(true)
            .error(R.drawable.placeholder_music_default_artwork)
            .placeholder(R.drawable.placeholder_music_default_artwork)
            .fallback(R.drawable.placeholder_music_default_artwork)
            .placeholderMemoryCacheKey(url.toString())
            .memoryCacheKey(url.toString())
            .allowHardware(true)
            .crossfade(true)
            .apply {
                if (imageQuality != ImageQuality.RAW) {
                    val size = getSizeFromQuality(imageQuality)
                    this.size(size)
                    if (imageQuality == ImageQuality.LOW) {
                        this.precision(Precision.INEXACT)
                    }
                } else {
                    this.precision(Precision.EXACT)
                    this.size(coil.size.Size.ORIGINAL)
                }
            }
            .build(),
        contentDescription = contentDescription.toString(),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(shape, shadowAlpha, shadowType, shadowOverlay)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
                /*this.shape = shape
                shadowElevation = shadowAlphaPx
                spotShadowColor = Color.Black.copy(alpha = 0.8f)*/
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    val outline = shape.createOutline(
                        Size(size.width, size.height),
                        LayoutDirection.Ltr,
                        density
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.Gray.copy(alpha = 0.1f),
                        style = Stroke(width = 12f)
                    )
                    drawOutline(
                        outline = outline,
                        color = Color.Gray.copy(alpha = 0.5f),
                        style = Stroke(width = 12f),
                        blendMode = BlendMode.Overlay
                    )
                }
            }

    )
}