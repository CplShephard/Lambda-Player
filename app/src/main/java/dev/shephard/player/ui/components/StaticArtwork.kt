package dev.shephard.player.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest

/** First GIF frame for list thumbnails; a separate cache key prevents animated cache reuse. */
@Composable
fun rememberStaticArtwork(model: Any?): Any? {
    val context = LocalContext.current
    return remember(context, model) {
        val uri = when (model) {
            is Uri -> model
            is String -> runCatching { Uri.parse(model) }.getOrNull()
            else -> null
        }
        val lastSegment = uri?.lastPathSegment.orEmpty()
        val pathIsGif = lastSegment.endsWith(".gif", ignoreCase = true)
        val hasKnownExtension = lastSegment.substringAfterLast('.', "").lowercase() in
            setOf("gif", "jpg", "jpeg", "png", "webp", "bmp", "mp4")
        // MediaStore album-art URIs almost always refer to JPEGs and may not have a suffix.
        // Check less-common providers when there is no extension, without blocking on
        // every MediaStore thumbnail during list scrolling.
        val providerIsGif = if (!hasKnownExtension && uri != null && uri.scheme == "content" && uri.authority != "media") {
            runCatching { context.contentResolver.getType(uri)?.equals("image/gif", ignoreCase = true) == true }.getOrDefault(false)
        } else false
        if (uri != null && (pathIsGif || providerIsGif)) {
            ImageRequest.Builder(context)
                .data(model)
                .decoderFactory(BitmapFactoryDecoder.Factory())
                .memoryCacheKey("static-gif-frame:$uri")
                .build()
        } else model
    }
}
