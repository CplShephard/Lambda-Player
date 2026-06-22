package dev.shephard.player.player

import android.app.Application
import android.content.ComponentName
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.google.common.util.concurrent.ListenableFuture
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.sin
import kotlin.random.Random

data class LyricLine(val timeMs: Long, val text: String)

data class PlayerUiState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<AudioTrack> = emptyList(),
    val crossfadeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val playWithOthers: Boolean = false,
    val totalListeningMs: Long = 0L,
    val glowColorArgb: Int = 0xFF22C55E.toInt(),
    val amplitude: Float = 0f,
    // v2.1
    val playlistContextName: String? = null,
    val likedIds: Set<Long> = emptySet(),
    val currentLyrics: List<LyricLine> = emptyList(),
    val currentLyricText: String = ""
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val prefs = PreferencesManager(application)
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var queueTracks: List<AudioTrack> = emptyList()
    private var isUserSeeking: Boolean = false
    private var lastPlaybackTickMs: Long? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (isPlaying) lastPlaybackTickMs = System.currentTimeMillis() else flushListeningTime()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushListeningTime()
            val track = queueTracks.find { it.id.toString() == mediaItem?.mediaId }
            _uiState.value = _uiState.value.copy(
                currentTrack = track,
                positionMs = 0L,
                durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
            )
            if (_uiState.value.crossfadeEnabled) performCrossfadeIn()
            extractGlowColor(track)
            loadLyrics(track)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L)
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            val mode = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _uiState.value = _uiState.value.copy(repeatMode = mode)
        }
    }

    init {
        connectToService()
        observePosition()
        observeSettings()
        startAmplitudePulse()
        viewModelScope.launch {
            prefs.likedSongIdsJson.collect { json ->
                val ids = try {
                    val arr = JSONArray(json)
                    (0 until arr.length()).map { arr.getLong(it) }.toSet()
                } catch (_: Exception) { emptySet() }
                _uiState.value = _uiState.value.copy(likedIds = ids)
            }
        }
    }

    private fun connectToService() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
            applyAudioFocusSetting(_uiState.value.playWithOthers)
            syncFromController()
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    private fun syncFromController() {
        val c = controller ?: return
        val count = c.mediaItemCount
        if (count == 0) return
        val restored = (0 until count).map { index ->
            val item = c.getMediaItemAt(index)
            item.toAudioTrack()
        }
        queueTracks = restored
        val currentIndex = c.currentMediaItemIndex.coerceIn(0, restored.size - 1)
        val current = restored.getOrNull(currentIndex)
        val mode = when (c.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
        _uiState.value = _uiState.value.copy(
            queue = restored,
            currentTrack = current,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = mode
        )
        if (c.isPlaying) lastPlaybackTickMs = System.currentTimeMillis()
        extractGlowColor(current)
        loadLyrics(current)
    }

    private fun MediaItem.toAudioTrack(): AudioTrack {
        val md = mediaMetadata
        val uri = localConfiguration?.uri ?: android.net.Uri.EMPTY
        return AudioTrack(
            id = mediaId.toLongOrNull() ?: 0L,
            title = md.title?.toString() ?: "",
            artist = md.artist?.toString() ?: "",
            album = md.albumTitle?.toString() ?: "",
            durationMs = 0L,
            uri = uri,
            albumArtUri = md.artworkUri
        )
    }

    private fun observePosition() {
        viewModelScope.launch {
            while (true) {
                val c = controller
                if (c != null && !isUserSeeking) {
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    val lyricText = findLyricAt(pos, _uiState.value.currentLyrics)
                    _uiState.value = _uiState.value.copy(
                        positionMs = pos,
                        durationMs = c.duration.coerceAtLeast(0L),
                        currentLyricText = lyricText
                    )
                }
                if (c?.isPlaying == true) accrueListeningTime()
                delay(120)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch { prefs.crossfadeEnabled.collect { _uiState.value = _uiState.value.copy(crossfadeEnabled = it) } }
        viewModelScope.launch { prefs.gaplessEnabled.collect { _uiState.value = _uiState.value.copy(gaplessEnabled = it) } }
        viewModelScope.launch { prefs.playWithOthers.collect { enabled -> _uiState.value = _uiState.value.copy(playWithOthers = enabled); applyAudioFocusSetting(enabled) } }
        viewModelScope.launch { prefs.totalListeningMs.collect { total -> _uiState.value = _uiState.value.copy(totalListeningMs = total) } }
    }

    private fun applyAudioFocusSetting(playWithOthers: Boolean) {
        val handleAudioFocus = !playWithOthers
        controller?.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            handleAudioFocus
        )
    }

    private fun accrueListeningTime() {
        val now = System.currentTimeMillis()
        val last = lastPlaybackTickMs
        if (last != null) {
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) viewModelScope.launch { prefs.addListeningTime(delta) }
        }
        lastPlaybackTickMs = now
    }
    private fun flushListeningTime() {
        val last = lastPlaybackTickMs
        if (last != null) {
            val now = System.currentTimeMillis()
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) viewModelScope.launch { prefs.addListeningTime(delta) }
        }
        lastPlaybackTickMs = null
    }

    private fun performCrossfadeIn() {
        val c = controller ?: return
        viewModelScope.launch {
            val steps = 8; val durationMs = 600L; val stepDelay = durationMs / steps
            for (i in 0..steps) { c.volume = i.toFloat() / steps; delay(stepDelay) }
            c.volume = 1f
        }
    }

    fun setQueueAndPlay(tracks: List<AudioTrack>, startIndex: Int, playlistName: String? = null) {
        val c = controller ?: return
        queueTracks = tracks
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.albumArtUri)
                        .build()
                ).build()
        }
        c.setMediaItems(mediaItems, startIndex.coerceIn(0, tracks.size-1), 0L)
        c.prepare(); c.play()
        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentTrack = tracks.getOrNull(startIndex),
            positionMs = 0L,
            playlistContextName = playlistName
        )
    }

    fun togglePlayPause() { val c = controller ?: return; if (c.isPlaying) c.pause() else c.play() }
    fun skipToNext() { controller?.seekToNextMediaItem() }
    fun skipToPrevious() { controller?.seekToPreviousMediaItem() }

    fun onSeekPreview(positionMs: Long) { isUserSeeking = true; _uiState.value = _uiState.value.copy(positionMs = positionMs) }
    fun onSeekCommit(positionMs: Long) { controller?.seekTo(positionMs); _uiState.value = _uiState.value.copy(positionMs = positionMs); isUserSeeking = false }

    // Shuffle: each tap produces a NEW random order, shown in queue
    fun toggleShuffle() {
        val c = controller ?: return
        val enabling = !c.shuffleModeEnabled
        c.shuffleModeEnabled = enabling
        if (enabling) {
            val current = _uiState.value.currentTrack
            val rest = queueTracks.filter { it.id != current?.id }.shuffled(Random(System.currentTimeMillis()))
            val newOrder = listOfNotNull(current) + rest
            reorderQueue(newOrder, preservePlaying = true)
        }
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        val current = _uiState.value.queue
        if (fromIndex == toIndex || fromIndex !in current.indices) return
        val target = toIndex.coerceIn(0, current.size - 1)
        val newOrder = current.toMutableList().apply { add(target, removeAt(fromIndex)) }
        queueTracks = newOrder
        _uiState.value = _uiState.value.copy(queue = newOrder)
        runCatching { c.moveMediaItem(fromIndex, target) }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        val q = _uiState.value.queue.toMutableList()
        if (index !in q.indices) return
        val playingIdx = q.indexOfFirst { it.id == _uiState.value.currentTrack?.id }
        if (index == playingIdx) return // don't remove currently playing
        q.removeAt(index)
        queueTracks = q
        _uiState.value = _uiState.value.copy(queue = q)
        runCatching { c.removeMediaItem(index) }
    }

    fun moveToNextUp(queueIndex: Int) {
        val q = _uiState.value.queue
        val playingIdx = q.indexOfFirst { it.id == _uiState.value.currentTrack?.id }
        if (queueIndex <= playingIdx) return
        moveQueueItem(queueIndex, playingIdx + 1)
    }

    fun playQueueIndex(index: Int) {
        controller?.seekToDefaultPosition(index)
        controller?.play()
    }

    fun reorderQueue(newOrder: List<AudioTrack>, preservePlaying: Boolean = true) {
        val c = controller ?: return
        queueTracks = newOrder
        _uiState.value = _uiState.value.copy(queue = newOrder)
        val working = _uiState.value.queue.toMutableList()
        // Simpler: rebuild media items, but try to keep position
        val currentId = _uiState.value.currentTrack?.id
        val currentPos = c.currentPosition
        val wasPlaying = c.isPlaying
        val mediaItems = newOrder.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title).setArtist(track.artist)
                        .setAlbumTitle(track.album).setArtworkUri(track.albumArtUri).build()
                ).build()
        }
        val newIndex = newOrder.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        c.setMediaItems(mediaItems, newIndex, currentPos)
        c.prepare()
        if (wasPlaying) c.play()
    }

    // Liked songs
    fun isLiked(trackId: Long): Boolean = trackId in _uiState.value.likedIds

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setCrossfadeEnabled(enabled) }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setGaplessEnabled(enabled) }
    }

    fun setPlayWithOthers(enabled: Boolean) {
        viewModelScope.launch { prefs.setPlayWithOthers(enabled) }
    }

    fun toggleLike(track: AudioTrack) {
        viewModelScope.launch {
            val currentJson = prefs.likedSongIdsJson.first()
            val arr = try { JSONArray(currentJson) } catch (_: Exception) { JSONArray() }
            val set = mutableSetOf<Long>()
            for (i in 0 until arr.length()) set.add(arr.optLong(i))
            if (track.id in set) set.remove(track.id) else set.add(track.id)
            val out = JSONArray(); set.forEach { out.put(it) }
            prefs.setLikedSongIdsJson(out.toString())
        }
    }

    private fun extractGlowColor(track: AudioTrack?) {
        val uri = track?.albumArtUri ?: return
        viewModelScope.launch {
            val color = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream) ?: return@use null
                        Palette.from(bmp).generate().vibrantSwatch?.rgb
                            ?: Palette.from(bmp).generate().dominantSwatch?.rgb
                    }
                }.getOrNull()
            }
            if (color != null) _uiState.value = _uiState.value.copy(glowColorArgb = color)
        }
    }

    private fun startAmplitudePulse() {
        viewModelScope.launch {
            var t = 0f
            while (true) {
                t += 0.12f
                val playing = _uiState.value.isPlaying
                val target = if (playing) (0.55f + 0.45f * ((sin(t.toDouble()) + 1.0) / 2.0).toFloat()) else 0f
                _uiState.value = _uiState.value.copy(amplitude = target)
                delay(80)
            }
        }
    }

    // Lyrics
    private fun loadLyrics(track: AudioTrack?) {
        viewModelScope.launch {
            if (track == null) { _uiState.value = _uiState.value.copy(currentLyrics = emptyList(), currentLyricText=""); return@launch }
            val ctx = getApplication<Application>()
            val lyrics = withContext(Dispatchers.IO) { LyricsParser.loadForTrack(ctx, track) }
            _uiState.value = _uiState.value.copy(currentLyrics = lyrics, currentLyricText = "")
        }
    }
    private fun findLyricAt(posMs: Long, lyrics: List<LyricLine>): String {
        if (lyrics.isEmpty()) return ""
        var out = ""
        for (l in lyrics) { if (posMs >= l.timeMs) out = l.text else break }
        return out
    }

    override fun onCleared() {
        flushListeningTime()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
