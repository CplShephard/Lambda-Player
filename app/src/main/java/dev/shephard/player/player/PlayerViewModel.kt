package dev.shephard.player.player

import android.app.Application
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

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
    val currentPlaylistName: String? = null,
    val likedSongIds: List<Long> = emptyList(),
    val lyrics: List<String> = emptyList(),
    val lyricsVisible: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var queueTracks: List<AudioTrack> = emptyList()
    private var originalQueue: List<AudioTrack> = emptyList()
    private var remixActive: Boolean = false
    private val _isRemixed = MutableStateFlow(false)
    val isRemixed: StateFlow<Boolean> = _isRemixed.asStateFlow()
    private var isUserSeeking: Boolean = false
    private var lastPlaybackTickMs: Long? = null
    private var lastShuffleSeed: Int = Random.nextInt()

    // -1 = backward, 1 = forward, 0 = unknown
    private val _navigationDirection = MutableStateFlow(1)
    val navigationDirection: StateFlow<Int> = _navigationDirection.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                lastPlaybackTickMs = System.currentTimeMillis()
            } else {
                flushListeningTime()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushListeningTime()
            val track = queueTracks.find { it.id.toString() == mediaItem?.mediaId }
            _uiState.value = _uiState.value.copy(
                currentTrack = track,
                positionMs = 0L,
                durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
            )
            if (_uiState.value.crossfadeEnabled) {
                performCrossfadeIn()
            }
            extractGlowColor(track)
            loadLyrics(track)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(
                durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
            )
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
        observeLikedSongs()
    }

    private fun connectToService() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
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
        if (c.isPlaying) {
            lastPlaybackTickMs = System.currentTimeMillis()
        }
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
                    _uiState.value = _uiState.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = c.duration.coerceAtLeast(0L)
                    )
                }
                if (c?.isPlaying == true) {
                    accrueListeningTime()
                }
                delay(500)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            prefs.crossfadeEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(crossfadeEnabled = enabled)
            }
        }
        viewModelScope.launch {
            prefs.gaplessEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(gaplessEnabled = enabled)
            }
        }
        viewModelScope.launch {
            prefs.playWithOthers.collect { enabled ->
                _uiState.value = _uiState.value.copy(playWithOthers = enabled)
                applyAudioFocusSetting(enabled)
            }
        }
        viewModelScope.launch {
            prefs.totalListeningMs.collect { total ->
                _uiState.value = _uiState.value.copy(totalListeningMs = total)
            }
        }
    }

    private fun observeLikedSongs() {
        viewModelScope.launch {
            prefs.likedSongIds.collect { json ->
                val ids = try {
                    org.json.JSONArray(json).let { arr ->
                        (0 until arr.length()).map { arr.getLong(it) }
                    }
                } catch (_: Exception) { emptyList() }
                _uiState.value = _uiState.value.copy(likedSongIds = ids)
            }
        }
    }

    private fun applyAudioFocusSetting(playWithOthers: Boolean) {
        val handleAudioFocus = !playWithOthers
        controller?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            handleAudioFocus
        )
    }

    private fun accrueListeningTime() {
        val now = System.currentTimeMillis()
        val last = lastPlaybackTickMs
        if (last != null) {
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) {
                viewModelScope.launch { prefs.addListeningTime(delta) }
            }
        }
        lastPlaybackTickMs = now
    }

    private fun flushListeningTime() {
        val last = lastPlaybackTickMs
        if (last != null) {
            val now = System.currentTimeMillis()
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) {
                viewModelScope.launch { prefs.addListeningTime(delta) }
            }
        }
        lastPlaybackTickMs = null
    }

    private fun performCrossfadeIn() {
        val c = controller ?: return
        viewModelScope.launch {
            val steps = 8
            val durationMs = 600L
            val stepDelay = durationMs / steps
            for (i in 0..steps) {
                val volume = i.toFloat() / steps
                c.volume = volume
                delay(stepDelay)
            }
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
                )
                .build()
        }

        c.setMediaItems(mediaItems, startIndex, 0L)
        c.prepare()
        c.play()

        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentTrack = tracks.getOrNull(startIndex),
            positionMs = 0L,
            currentPlaylistName = playlistName
        )
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun skipToNext() {
        _navigationDirection.value = 1
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        _navigationDirection.value = -1
        controller?.seekToPreviousMediaItem()
    }

    fun onSeekPreview(positionMs: Long) {
        isUserSeeking = true
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
    }

    fun onSeekCommit(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
        isUserSeeking = false
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun remixQueue() {
        val c = controller ?: return
        if (remixActive) {
            // Un-remix: restore original order
            val currentTrack = _uiState.value.currentTrack
            val restoreQueue = if (originalQueue.isNotEmpty()) originalQueue else _uiState.value.queue
            queueTracks = restoreQueue
            _uiState.value = _uiState.value.copy(queue = restoreQueue, shuffleEnabled = false)
            val mediaItems = restoreQueue.map { track ->
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
                    )
                    .build()
            }
            val startIndex = restoreQueue.indexOfFirst { it.id == currentTrack?.id }.coerceAtLeast(0)
            c.setMediaItems(mediaItems, startIndex, c.currentPosition)
            c.prepare()
            c.play()
            remixActive = false
            _isRemixed.value = false
            originalQueue = emptyList()
            return
        }

        val current = _uiState.value.queue
        if (current.size <= 1) return

        originalQueue = current
        lastShuffleSeed = Random.nextInt()
        val currentIndex = current.indexOfFirst { it.id == _uiState.value.currentTrack?.id }.coerceAtLeast(0)
        val currentTrack = current.getOrNull(currentIndex) ?: return
        val rest = current.filterIndexed { i, _ -> i != currentIndex }.shuffled(Random(lastShuffleSeed))
        val newOrder = listOf(currentTrack) + rest

        queueTracks = newOrder
        _uiState.value = _uiState.value.copy(queue = newOrder, shuffleEnabled = true)

        val mediaItems = newOrder.map { track ->
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
                )
                .build()
        }
        c.setMediaItems(mediaItems, 0, c.currentPosition)
        c.prepare()
        c.play()
        remixActive = true
        _isRemixed.value = true
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setCrossfadeEnabled(enabled) }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setGaplessEnabled(enabled) }
    }

    fun setPlayWithOthers(enabled: Boolean) {
        viewModelScope.launch { prefs.setPlayWithOthers(enabled) }
    }

    fun playQueueItem(index: Int) {
        val c = controller ?: return
        val currentStart = queueTracks.indexOfFirst { it.id == _uiState.value.currentTrack?.id }.coerceAtLeast(0)
        val actualIndex = currentStart + index
        if (actualIndex in queueTracks.indices) {
            c.seekTo(actualIndex, 0L)
            c.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        val currentStart = queueTracks.indexOfFirst { it.id == _uiState.value.currentTrack?.id }.coerceAtLeast(0)
        val actualIndex = currentStart + index
        if (actualIndex in queueTracks.indices) {
            val newList = queueTracks.toMutableList().apply { removeAt(actualIndex) }
            queueTracks = newList
            _uiState.value = _uiState.value.copy(queue = newList)
            c.removeMediaItem(actualIndex)
        }
    }

    fun playNext(queueIndex: Int) {
        val c = controller ?: return
        val currentStart = queueTracks.indexOfFirst { it.id == _uiState.value.currentTrack?.id }.coerceAtLeast(0)
        val actualIndex = currentStart + queueIndex
        if (actualIndex in queueTracks.indices) {
            val track = queueTracks[actualIndex]
            val newList = queueTracks.toMutableList().apply {
                removeAt(actualIndex)
                add(currentStart + 1, track)
            }
            queueTracks = newList
            _uiState.value = _uiState.value.copy(queue = newList)
            c.moveMediaItem(actualIndex, currentStart + 1)
        }
    }

    override fun onCleared() {
        flushListeningTime()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        val current = _uiState.value.queue
        if (fromIndex == toIndex) return
        if (fromIndex !in current.indices) return
        val target = toIndex.coerceIn(0, current.size - 1)
        if (fromIndex == target) return

        val newOrder = current.toMutableList().apply {
            add(target, removeAt(fromIndex))
        }
        queueTracks = newOrder
        _uiState.value = _uiState.value.copy(queue = newOrder)
        runCatching { c.moveMediaItem(fromIndex, target) }
    }

    fun reorderQueue(newOrder: List<AudioTrack>) {
        val c = controller ?: return
        val old = _uiState.value.queue
        queueTracks = newOrder
        _uiState.value = _uiState.value.copy(queue = newOrder)

        val working = old.toMutableList()
        for (targetIndex in newOrder.indices) {
            val track = newOrder[targetIndex]
            val currentIndex = working.indexOfFirst { it.id == track.id }
            if (currentIndex >= 0 && currentIndex != targetIndex) {
                working.add(targetIndex, working.removeAt(currentIndex))
                runCatching { c.moveMediaItem(currentIndex, targetIndex) }
            }
        }
    }

    fun toggleLike(trackId: Long) {
        viewModelScope.launch {
            val current = _uiState.value.likedSongIds.toMutableList()
            val newList = if (current.contains(trackId)) current.filter { it != trackId } else current + trackId
            val json = org.json.JSONArray().apply { newList.forEach { put(it) } }.toString()
            prefs.setLikedSongIds(json)
        }
    }

    fun isLiked(trackId: Long): Boolean = _uiState.value.likedSongIds.contains(trackId)

    fun addToLiked(trackId: Long) {
        viewModelScope.launch {
            val current = _uiState.value.likedSongIds.toMutableList()
            if (!current.contains(trackId)) {
                current.add(trackId)
                val json = org.json.JSONArray().apply { current.forEach { put(it) } }.toString()
                prefs.setLikedSongIds(json)
            }
        }
    }

    fun removeFromLiked(trackId: Long) {
        viewModelScope.launch {
            val current = _uiState.value.likedSongIds.toMutableList()
            if (current.contains(trackId)) {
                current.removeAll { it == trackId }
                val json = org.json.JSONArray().apply { current.forEach { put(it) } }.toString()
                prefs.setLikedSongIds(json)
            }
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
            if (color != null) {
                _uiState.value = _uiState.value.copy(glowColorArgb = color)
            }
        }
    }

    private fun loadLyrics(track: AudioTrack?) {
        if (track == null) {
            _uiState.value = _uiState.value.copy(lyrics = emptyList(), lyricsVisible = false)
            return
        }
        viewModelScope.launch {
            val lyrics = withContext(Dispatchers.IO) {
                runCatching {
                    loadLyricsFromRetriever(track.uri)
                        ?: loadLyricsFromLrcFile(track)
                }.getOrNull() ?: emptyList()
            }
            _uiState.value = _uiState.value.copy(lyrics = lyrics, lyricsVisible = lyrics.isNotEmpty())
        }
    }

    private fun loadLyricsFromRetriever(uri: android.net.Uri): List<String>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication(), uri)
            val lyrics = retriever.extractMetadata(28 /* MediaMetadataRetriever.METADATA_KEY_LYRICS */)
            retriever.release()
            lyrics?.lines()?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            retriever.release()
            null
        }
    }

    private fun loadLyricsFromLrcFile(track: AudioTrack): List<String>? {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)
        val cursor = resolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("%.lrc"),
            null
        )
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val baseName = track.title.replace(" ", "_")
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: continue
                if (name.contains(baseName, ignoreCase = true) ||
                    track.uri.lastPathSegment?.let { name.contains(it.substringBeforeLast("."), ignoreCase = true) } == true
                ) {
                    val lrcUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"), id
                    )
                    resolver.openInputStream(lrcUri)?.use { stream ->
                        return parseLrc(stream.bufferedReader().readText())
                    }
                }
            }
        }
        return null
    }

    private fun parseLrc(content: String): List<String> {
        val regex = Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\](.*)")
        return content.lines()
            .mapNotNull { line ->
                val match = regex.find(line)
                match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            }
            .distinct()
    }

    fun toggleLyricsVisible() {
        _uiState.value = _uiState.value.copy(lyricsVisible = !_uiState.value.lyricsVisible)
    }

    private fun startAmplitudePulse() {
        viewModelScope.launch {
            var t = 0f
            while (true) {
                t += 0.12f
                val playing = _uiState.value.isPlaying
                val target = if (playing) (0.55f + 0.45f * ((sin(t.toDouble()) + 1.0) / 2.0).toFloat())
                else 0f
                _uiState.value = _uiState.value.copy(amplitude = target)
                delay(80)
            }
        }
    }

    init {
        startAmplitudePulse()
    }
}
