package dev.shephard.player.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var settingsScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Renderers that route audio through the volume normalization processor.
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(VolumeNormalizationAudioProcessor()))
                .build()
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()

        // Settings are applied on the main thread, where ExoPlayer must be used.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        settingsScope = scope
        val prefs = PreferencesManager(this)

        // Gapless: the next queue item is prepared ahead of time, so the transition has no silent gap.
        scope.launch {
            prefs.gaplessEnabled.distinctUntilChanged().collect { enabled ->
                player.setPreloadConfiguration(
                    ExoPlayer.PreloadConfiguration(if (enabled) GAPLESS_PRELOAD_DURATION_US else 0L)
                )
            }
        }

        // Volume normalization: the processor reads this target on every audio block.
        scope.launch {
            prefs.volumeNormalization.distinctUntilChanged().collect { mode ->
                VolumeNormalizer.target = mode
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        settingsScope?.cancel()
        settingsScope = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private companion object {
        /** How far ahead the next queue item is prepared. 0 disables the look-ahead. */
        const val GAPLESS_PRELOAD_DURATION_US = 10_000_000L
    }
}
