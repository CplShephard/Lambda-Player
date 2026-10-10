package dev.shephard.player

import android.app.Application
import android.os.Build
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

class LambdaPlayerApplication : Application(), ImageLoaderFactory {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate() {
        super.onCreate()
        // Compose 1.12.0 regression (b/530704636): moving focus from one text field to another
        // closes the keyboard and reopens it, because the old field's hideKeyboard is no longer
        // coalesced with the new field's showKeyboard. Going back to the Choreographer-based
        // scheduling fixes it. Must be set before any Compose UI exists. Remove once the
        // Compose BOM ships the upstream fix (aosp/4214447).
        AndroidComposeUiFlags.isOutOfFrameSchedulerForTextInputEventsEnabled = false
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .crossfade(200)

        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }

        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }

        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
