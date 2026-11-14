package dev.shephard.player.player

import android.content.Context
import android.os.Bundle
import android.app.Service
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.shephard.player.R
import dev.shephard.player.ui.i18n.stringsFor
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
    private var likedIds: Set<Long> = emptySet()
    private var language: String = "en"
    private val likeCommand = SessionCommand(ACTION_LIKE, Bundle.EMPTY)
    private val closeCommand = SessionCommand(ACTION_CLOSE, Bundle.EMPTY)

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

        // The session and its preference observer stay alive while the controller is bound,
        // even after playback is cleared, so selecting another song works immediately.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        settingsScope = scope
        val prefs = PreferencesManager(this)
        val callback = object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val base = super.onConnect(session, controller)
                if (controller.packageName != packageName && !controller.isTrusted && !session.isMediaNotificationController(controller)) return base
                val commands = base.availableSessionCommands.buildUpon().add(likeCommand).add(closeCommand).build()
                return MediaSession.ConnectionResult.accept(commands, base.availablePlayerCommands)
            }

            override fun onCustomCommand(
                session: MediaSession, controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand, args: Bundle,
            ): ListenableFuture<SessionResult> {
                if (controller.packageName != packageName && !controller.isTrusted && !session.isMediaNotificationController(controller)) {
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
                }
                return when (customCommand.customAction) {
                    ACTION_LIKE -> {
                        val id = session.player.currentMediaItem?.mediaId?.toLongOrNull()
                        if (id == null || id <= 0L) Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                        else {
                            val result = SettableFuture.create<SessionResult>()
                            scope.launch {
                                try {
                                    prefs.updateLikedTrackId(id, "toggle")
                                    result.set(SessionResult(SessionResult.RESULT_SUCCESS))
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    result.cancel(false)
                                    throw e
                                } catch (e: Exception) {
                                    android.util.Log.w("PlaybackService", "Could not update liked song", e)
                                    result.set(SessionResult(SessionResult.RESULT_ERROR_IO))
                                }
                            }
                            result
                        }
                    }
                    ACTION_CLOSE -> {
                        session.player.pause()
                        session.player.stop()
                        session.player.clearMediaItems()
                        stopForeground(Service.STOP_FOREGROUND_REMOVE)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            }
        }
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .setCustomLayout(notificationButtons(player))
            .build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaSession?.setCustomLayout(notificationButtons(player))
            }
        })
        scope.launch {
            prefs.likedSongIds.distinctUntilChanged().collect { json ->
                likedIds = runCatching {
                    val array = org.json.JSONArray(json)
                    (0 until array.length()).mapNotNull { i -> array.optLong(i).takeIf { it > 0L } }.toSet()
                }.getOrDefault(emptySet())
                mediaSession?.setCustomLayout(notificationButtons(player))
            }
        }
        scope.launch {
            prefs.language.distinctUntilChanged().collect { value ->
                language = value
                mediaSession?.setCustomLayout(notificationButtons(player))
            }
        }

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

    private fun notificationButtons(player: Player): List<CommandButton> {
        val id = player.currentMediaItem?.mediaId?.toLongOrNull()
        val liked = id != null && id in likedIds
        val strings = stringsFor(language)
        val like = CommandButton.Builder(if (liked) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
            .setSessionCommand(likeCommand)
            .setDisplayName(if (liked) strings.notificationUnlike else strings.notificationLike)
            .setEnabled(id != null && id > 0L)
            .build()
        val close = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_notification_close)
            .setSessionCommand(closeCommand)
            .setDisplayName(strings.notificationClose)
            .setEnabled(player.currentMediaItem != null)
            .build()
        return listOf(like, close)
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
        const val ACTION_LIKE = "dev.shephard.player.notification.LIKE"
        const val ACTION_CLOSE = "dev.shephard.player.notification.CLOSE"
    }
}
