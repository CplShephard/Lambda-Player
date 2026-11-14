@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.shephard.player.ui.components

import android.graphics.Matrix
import android.net.Uri
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.shephard.player.player.ImagePersistence

/** A visual-only layer over a still thumbnail. It cannot acquire audio focus or play audio. */
@Composable
fun SilentVideoCover(model: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uri = remember(model) {
        when (model) {
            is Uri -> model
            is String -> runCatching { Uri.parse(model) }.getOrNull()
            else -> null
        }
    } ?: return
    // Limit playback to app-private remuxed covers: arbitrary media is never sent to this player.
    val isPrivateVideo = remember(uri) {
        uri.scheme == "content" && uri.authority == "${context.packageName}.fileprovider" &&
            ImagePersistence.isMp4(context, uri)
    }
    if (!isPrivateVideo) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember(context, uri) {
        var created: ExoPlayer? = null
        try {
            val selector = DefaultTrackSelector(context).apply {
                setParameters(buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true))
            }
            val instance = ExoPlayer.Builder(context).setTrackSelector(selector).build()
            created = instance
            instance.volume = 0f
            instance.setAudioAttributes(AudioAttributes.DEFAULT, false)
            instance.repeatMode = Player.REPEAT_MODE_ONE
            instance.setMediaItem(MediaItem.fromUri(uri))
            instance.playWhenReady = false
            instance.prepare()
            instance
        } catch (failure: Exception) {
            created?.release()
            android.util.Log.w("SilentVideoCover", "Unable to initialize cover video", failure)
            null
        }
    } ?: return
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            player.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        player.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        onDispose { lifecycle.removeObserver(observer); player.playWhenReady = false; player.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                val updateCrop = {
                    val video = player.videoSize
                    if (width > 0 && height > 0 && video.width > 0 && video.height > 0) {
                        val videoAspect = video.width.toFloat() * video.pixelWidthHeightRatio / video.height
                        val viewAspect = width.toFloat() / height
                        val sx = if (videoAspect > viewAspect) videoAspect / viewAspect else 1f
                        val sy = if (videoAspect < viewAspect) viewAspect / videoAspect else 1f
                        setTransform(Matrix().apply { setScale(sx, sy, width / 2f, height / 2f) })
                    }
                }
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCrop() }
                val listener = object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) { updateCrop() }
                }
                player.addListener(listener)
                // The view is recreated with the composition; player.release clears listeners.
                player.setVideoTextureView(this)
            }
        },
        update = { player.setVideoTextureView(it) }
    )
}
