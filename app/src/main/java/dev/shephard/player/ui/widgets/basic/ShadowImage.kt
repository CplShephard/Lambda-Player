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
import coil.size.Size as CoilSize
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

private fun buildRequest(
    context: android.content.Context,
    url: Any?,
    imageQuality: ImageQuality,
    withMemoryKey: Boolean,
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(url)
        .crossfade(true)
        .error(R.drawable.placeholder_music_default_artwork)
        .placeholder(R.drawable.placeholder_music_default_artwork)
        .fallback(R.drawable.placeholder_music_default_artwork)
        .allowHardware(true)
    if (withMemoryKey) {
        builder.memoryCacheKey(url?.toString())
            .diskCacheKey(url?.toString())
    } else {
        builder.placeholderMemoryCacheKey(url?.toString())
            .diskCacheKey(url?.toString())
    }
    if (imageQuality == ImageQuality.RAW) {
        builder.precision(Precision.EXACT)
        builder.size(CoilSize.ORIGINAL)
    } else {
        val s = getSizeFromQuality(imageQuality)
        builder.size(s)
        if (imageQuality == ImageQuality.LOW) {
            builder.precision(Precision.INEXACT)
        }
    }
    return builder.build()
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
    val url = dataLambda()
    AsyncImage(
        model = buildRequest(LocalContext.current, url, imageQuality, withMemoryKey = false),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(shape, shadowAlpha, shadowType, shadowOverlay)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
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
        model = buildRequest(LocalContext.current, url, imageQuality, withMemoryKey = true),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .dropShadow(shape, shadowAlpha, shadowType, shadowOverlay)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                this.shape = shape
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
