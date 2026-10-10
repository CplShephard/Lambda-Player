package dev.shephard.player.player

import android.app.Application
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import dev.shephard.player.data.ListenEvent
import dev.shephard.player.data.ListenStatsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

private const val DEFAULT_GLOW_COLOR = -14498466 // ARGB #22C55E
private const val QQ_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36 LambdaPlayer/2.5"

/** Sentences QQ Music returns instead of lyrics for instrumental tracks. */
private val QQ_INSTRUMENTAL_MARKERS = listOf("此歌曲为没有填词的纯音乐", "纯音乐，请欣赏", "純音樂，請欣賞")

data class SyncedLyricLine(val timeMs: Long, val text: String)

data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

data class LyricsSearchResult(
    val id: Long?,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Double?,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val source: String = "LRCLIB",
    val sourceId: String? = null,
) {
    val rawLyrics: String get() = syncedLyrics?.takeIf(String::isNotBlank) ?: plainLyrics.orEmpty()
}

data class LyricsSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<LyricsSearchResult> = emptyList(),
    val error: String? = null,
)

private data class LyricsDocument(
    val rawText: String,
    val lines: List<String>,
    val syncedLines: List<SyncedLyricLine>,
)

data class PlayerUiState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<AudioTrack> = emptyList(),
    val crossfadeEnabled: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val lyricsAutoSearchMode: LyricsAutoSearchMode = LyricsAutoSearchMode.OFF,
    val volumeNormalization: VolumeNormalization = VolumeNormalization.NONE,
    val playWithOthers: Boolean = false,
    val totalListeningMs: Long = 0L,
    val glowColorArgb: Int = DEFAULT_GLOW_COLOR,
    val amplitude: Float = 0f,
    val currentPlaylistName: String? = null,
    val likedSongIds: List<Long> = emptyList(),
    val lyrics: List<String> = emptyList(),
    val syncedLyrics: List<SyncedLyricLine> = emptyList(),
    val lyricsVisible: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Tracks whose lyrics were already opened once in ONLINE_FIRST mode. */
    private val openedLyricsTrackIds = mutableSetOf<Long>()

    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private val _lyricsSearchState = MutableStateFlow(LyricsSearchUiState())
    val lyricsSearchState: StateFlow<LyricsSearchUiState> = _lyricsSearchState.asStateFlow()
    private var lyricsSearchJob: Job? = null
    private var lyricsLoadJob: Job? = null
    private var glowColorJob: Job? = null

    private val _lyricsSyncOffsetMs = MutableStateFlow(0)
    val lyricsSyncOffsetMs: StateFlow<Int> = _lyricsSyncOffsetMs.asStateFlow()

    private var queueTracks: List<AudioTrack> = emptyList()
    private var originalQueue: List<AudioTrack> = emptyList()
    private var remixActive: Boolean = false
    private val _isRemixed = MutableStateFlow(false)
    val isRemixed: StateFlow<Boolean> = _isRemixed.asStateFlow()
    private var isUserSeeking: Boolean = false
    private var lastPlaybackTickMs: Long? = null
    private var pendingListeningDeltaMs: Long = 0L
    private var lastListeningStoreWriteMs: Long = 0L
    private var pendingExternalUri: Uri? = null

    private var statsSessionTrackId: Long? = null
    private var statsSessionQualified: Boolean = false
    private var statsSessionQualifyDayStartMs: Long = 0L
    private var statsListenedMsByDayStart: LinkedHashMap<Long, Long> = LinkedHashMap()
    private var statsEvents: List<ListenEvent> = emptyList()
    private var statsEventsLoaded: Boolean = false
    private val _statsEventsFlow = MutableStateFlow<List<ListenEvent>>(emptyList())
    val statsEventsFlow: StateFlow<List<ListenEvent>> = _statsEventsFlow.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _totalListeningMsLive = MutableStateFlow(0L)
    val totalListeningMsLive: StateFlow<Long> = _totalListeningMsLive.asStateFlow()

    private val _navigationDirection = MutableStateFlow(1)
    val navigationDirection: StateFlow<Int> = _navigationDirection.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                val c = controller
                if (c != null && !isUserSeeking) {
                    _progress.value = PlaybackProgress(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = currentDurationMs(_uiState.value.currentTrack)
                    )
                }
                val now = System.currentTimeMillis()
                lastPlaybackTickMs = now
                lastListeningStoreWriteMs = now
            } else {
                flushListeningTime()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _uiState.value = _uiState.value.copy(isPlaying = playWhenReady)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushListeningTime()
            VolumeNormalizer.requestReset()
            val track = queueTracks.find { it.id.toString() == mediaItem?.mediaId }
            finalizeStatsSession()
            statsSessionTrackId = track?.id
            _uiState.value = _uiState.value.copy(
                currentTrack = track,
                lyrics = emptyList(),
                syncedLyrics = emptyList(),
                lyricsVisible = false,
            )
            if (track != null) {
                recordRecentTrackStart(track)
            }
            // Media3 can report TIME_UNSET for a short period while the next
            // item is preparing. Use the scanned track duration immediately so
            // the seek bar does not briefly render the new song as 0:00.
            _progress.value = PlaybackProgress(
                positionMs = 0L,
                durationMs = currentDurationMs(track, preferTrackDuration = true)
            )
            if (_uiState.value.crossfadeEnabled) {
                performCrossfadeIn()
            }
            extractGlowColor(track)
            loadLyrics(track)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val currentTrack = _uiState.value.currentTrack
            val durationMs = currentDurationMs(
                currentTrack,
                preferTrackDuration = playbackState != Player.STATE_READY
            )
            val positionMs = if (playbackState == Player.STATE_READY) {
                controller?.currentPosition?.coerceAtLeast(0L) ?: _progress.value.positionMs
            } else {
                _progress.value.positionMs
            }
            _progress.value = PlaybackProgress(
                positionMs = positionMs,
                durationMs = durationMs
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

    /**
     * `Player.duration` is TIME_UNSET while a local item is preparing. Prefer
     * the MediaStore duration in that window, then switch to Media3's timeline
     * duration once it is available.
     */
    private fun currentDurationMs(
        track: AudioTrack? = _uiState.value.currentTrack,
        preferTrackDuration: Boolean = false,
    ): Long {
        val playerDuration = controller?.duration?.takeIf { it > 0L && it != C.TIME_UNSET }
        val indexedDuration = track?.durationMs?.takeIf { it > 0L }
        return if (preferTrackDuration) {
            indexedDuration ?: playerDuration ?: 0L
        } else {
            playerDuration ?: indexedDuration ?: 0L
        }
    }

    init {
        connectToService()
        observePosition()
        observeSettings()
        observeLikedSongs()
        loadStatsEvents()
    }

    private fun loadStatsEvents() {
        viewModelScope.launch {
            val json = prefs.listenStatsEventsJson.first()
            val loadedEvents = ListenStatsCalculator.decodeEvents(json)

            statsEvents = if (statsEvents.isEmpty()) {
                loadedEvents
            } else {
                loadedEvents + statsEvents
            }
            statsEventsLoaded = true
            _statsEventsFlow.value = statsEvents

            if (statsEvents.size != loadedEvents.size) {
                persistStatsEvents()
            }
        }
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
            val pending = pendingExternalUri
            if (pending != null) {
                pendingExternalUri = null
                playExternalUri(pending)
            } else {
                syncFromController()
            }
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
            isPlaying = c.playWhenReady,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = mode
        )
        _progress.value = PlaybackProgress(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = currentDurationMs(current)
        )
        if (c.isPlaying) {
            val now = System.currentTimeMillis()
            lastPlaybackTickMs = now
            lastListeningStoreWriteMs = now
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
                    if (c.isPlaying) {
                        accrueListeningTime()
                    }

                    _progress.value = PlaybackProgress(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = currentDurationMs(_uiState.value.currentTrack)
                    )
                } else if (c?.isPlaying == true) {
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
            prefs.lyricsAutoSearchMode.collect { mode ->
                _uiState.value = _uiState.value.copy(lyricsAutoSearchMode = mode)
            }
        }
        viewModelScope.launch {
            prefs.volumeNormalization.collect { mode ->
                _uiState.value = _uiState.value.copy(volumeNormalization = mode)
            }
        }
        viewModelScope.launch {
            // Only adds ids: this set is also updated directly when lyrics are opened.
            prefs.lyricsOnlineOpenedTrackIds.collect { raw ->
                raw.split(',').mapNotNullTo(openedLyricsTrackIds) { it.toLongOrNull() }
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
                val liveTotal = maxOf(_totalListeningMsLive.value, total)
                _totalListeningMsLive.value = liveTotal
                _uiState.value = _uiState.value.copy(totalListeningMs = liveTotal)
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

    private fun accrueStatsSession(deltaMs: Long, nowMs: Long) {
        val trackId = statsSessionTrackId ?: return
        val dayStart = ListenStatsCalculator.dayStartMs(nowMs)
        statsListenedMsByDayStart[dayStart] = (statsListenedMsByDayStart[dayStart] ?: 0L) + deltaMs

        if (!statsSessionQualified) {
            val durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
            val positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            if (durationMs > 0 && positionMs.toDouble() / durationMs.toDouble() >= 0.5) {
                statsSessionQualified = true
                statsSessionQualifyDayStartMs = dayStart
            }
        }

        if (statsEvents.isNotEmpty()) {
            val lastIdx = statsEvents.lastIndex
            val lastEv = statsEvents[lastIdx]
            if (lastEv.trackId == trackId && lastEv.dayStartMs == dayStart) {
                val updatedList = statsEvents.toMutableList()
                updatedList[lastIdx] = lastEv.copy(
                    listenedMs = lastEv.listenedMs + deltaMs,
                    countsAsPlay = statsSessionQualified
                )
                statsEvents = updatedList
                _statsEventsFlow.value = statsEvents
            }
        }
    }

    private fun finalizeStatsSession() {
        val trackId = statsSessionTrackId
        if (trackId != null && statsListenedMsByDayStart.isNotEmpty()) {
            val totalListened = statsListenedMsByDayStart.values.sum()
            if (statsEvents.isNotEmpty() && statsEvents.last().trackId == trackId) {
                val lastIdx = statsEvents.lastIndex
                val lastEv = statsEvents[lastIdx]
                val updatedList = statsEvents.toMutableList()
                updatedList[lastIdx] = lastEv.copy(
                    listenedMs = totalListened.coerceAtLeast(lastEv.listenedMs),
                    countsAsPlay = statsSessionQualified
                )
                statsEvents = updatedList
                _statsEventsFlow.value = statsEvents
                persistStatsEvents()
            } else {
                val track = queueTracks.find { it.id == trackId } ?: _uiState.value.currentTrack
                if (track != null) {
                    val newEvents = statsListenedMsByDayStart.entries
                        .filter { it.value > 0 }
                        .map { (dayStart, listenedMs) ->
                            ListenEvent(
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                dayStartMs = dayStart,
                                timestampMs = System.currentTimeMillis(),
                                listenedMs = listenedMs,
                                countsAsPlay = dayStart == statsSessionQualifyDayStartMs && statsSessionQualified,
                                albumArtUri = track.albumArtUri?.toString()
                            )
                        }
                    if (newEvents.isNotEmpty()) {
                        statsEvents = statsEvents + newEvents
                        _statsEventsFlow.value = statsEvents
                        persistStatsEvents()
                    }
                }
            }
        }
        statsSessionTrackId = null
        statsSessionQualified = false
        statsSessionQualifyDayStartMs = 0L
        statsListenedMsByDayStart = LinkedHashMap()
    }

    private fun recordRecentTrackStart(track: AudioTrack) {
        val now = System.currentTimeMillis()
        val event = ListenEvent(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            dayStartMs = ListenStatsCalculator.dayStartMs(now),
            timestampMs = now,
            listenedMs = 0L,
            countsAsPlay = false,
            albumArtUri = track.albumArtUri?.toString()
        )
        statsEvents = statsEvents + event
        _statsEventsFlow.value = statsEvents
        persistStatsEvents()
    }

    private fun persistStatsEvents() {
        if (!statsEventsLoaded) return
        val snapshot = statsEvents
        viewModelScope.launch {
            prefs.setListenStatsEventsJson(ListenStatsCalculator.encodeEvents(snapshot))
        }
    }

    fun clearListenStats() {
        statsEvents = emptyList()
        _statsEventsFlow.value = emptyList()
        persistStatsEvents()
    }

    private fun accrueListeningTime() {
        val now = System.currentTimeMillis()
        val last = lastPlaybackTickMs
        if (last != null) {
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) {
                addListeningDeltaInMemory(delta, updateUiState = false)
                accrueStatsSession(delta, now)
            }
        }
        lastPlaybackTickMs = now

        if (pendingListeningDeltaMs >= 10_000L || now - lastListeningStoreWriteMs >= 10_000L) {
            flushPendingListeningTime()
        }
    }

    private fun flushListeningTime() {
        val last = lastPlaybackTickMs
        if (last != null) {
            val now = System.currentTimeMillis()
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) addListeningDeltaInMemory(delta)
        }
        lastPlaybackTickMs = null
        flushPendingListeningTime()
    }

    private fun addListeningDeltaInMemory(delta: Long, updateUiState: Boolean = true) {
        pendingListeningDeltaMs += delta
        val nextTotal = _totalListeningMsLive.value + delta
        _totalListeningMsLive.value = nextTotal
        if (updateUiState) {
            _uiState.value = _uiState.value.copy(totalListeningMs = nextTotal)
        }
    }

    private fun flushPendingListeningTime() {
        val delta = pendingListeningDeltaMs
        if (delta <= 0L) return
        pendingListeningDeltaMs = 0L
        lastListeningStoreWriteMs = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) { prefs.addListeningTime(delta) }
        persistStatsEvents()
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

    fun playExternalUri(uri: Uri) {
        val c = controller
        if (c == null) {
            pendingExternalUri = uri
            return
        }
        remixActive = false
        _isRemixed.value = false
        originalQueue = emptyList()

        viewModelScope.launch {

            val artUri: Uri? = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(getApplication(), uri)
                    val bytes = retriever.embeddedPicture
                    retriever.release()
                    if (bytes != null) {

                        val cacheFile = java.io.File(getApplication<Application>().cacheDir, "ext_art_${uri.hashCode()}.jpg")
                        cacheFile.writeBytes(bytes)
                        Uri.fromFile(cacheFile)
                    } else null
                }.getOrNull()
            }

            val rawTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "External audio"
            val track = AudioTrack(
                id = uri.toString().hashCode().toLong(),
                title = rawTitle,
                artist = "",
                album = "",
                durationMs = 0L,
                uri = uri,
                albumArtUri = artUri
            )
            val item = MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(artUri)
                        .build()
                )
                .build()

            queueTracks = listOf(track)
            c.setMediaItem(item)
            c.prepare()
            c.play()
            _uiState.value = _uiState.value.copy(
                queue = listOf(track),
                currentTrack = track,
                currentPlaylistName = null
            )
            _progress.value = PlaybackProgress()
            extractGlowColor(track)
            loadLyrics(track)
        }
    }

    fun setQueueAndPlay(tracks: List<AudioTrack>, startIndex: Int, playlistName: String? = null) {
        val c = controller ?: return
        remixActive = false
        _isRemixed.value = false
        originalQueue = emptyList()
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

        val startTrack = tracks.getOrNull(startIndex)
        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentTrack = startTrack,
            currentPlaylistName = playlistName
        )
        _progress.value = PlaybackProgress(
            durationMs = currentDurationMs(startTrack, preferTrackDuration = true)
        )
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.playWhenReady) c.pause() else c.play()
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
        _progress.value = _progress.value.copy(positionMs = positionMs)
    }

    fun onSeekCommit(positionMs: Long) {
        controller?.seekTo(positionMs)
        _progress.value = _progress.value.copy(positionMs = positionMs)
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
        val current = _uiState.value.queue
        val currentTrack = _uiState.value.currentTrack
        if (current.size <= 1 || currentTrack == null) return

        if (remixActive) {

            val queueIds = current.map(AudioTrack::id).toSet()
            val restored = originalQueue.filter { it.id in queueIds }

            val extras = current.filterNot { it.id in restored.map { it.id }.toSet() }
            val restoreQueue = restored + extras
            if (restoreQueue != current) {
                reorderPlayerPlaylist(c, current, restoreQueue)
            }
            queueTracks = restoreQueue
            _uiState.value = _uiState.value.copy(queue = restoreQueue, shuffleEnabled = false)
            remixActive = false
            _isRemixed.value = false
            originalQueue = emptyList()
            return
        }

        originalQueue = current
        val rest = current.filterNot { it.id == currentTrack.id }.shuffled()
        val newOrder = listOf(currentTrack) + rest
        reorderPlayerPlaylist(c, current, newOrder)

        queueTracks = newOrder
        _uiState.value = _uiState.value.copy(queue = newOrder, shuffleEnabled = true)
        remixActive = true
        _isRemixed.value = true
    }

    fun setQueueAndPlayRemixed(tracks: List<AudioTrack>, playlistName: String? = null) {
        val c = controller ?: return
        if (tracks.isEmpty()) return

        val startIndex = tracks.indices.random()
        val startTrack = tracks[startIndex]
        val newOrder = listOf(startTrack) + tracks.filterIndexed { i, _ -> i != startIndex }.shuffled()
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

        originalQueue = tracks
        remixActive = true
        _isRemixed.value = true
        queueTracks = newOrder
        c.setMediaItems(mediaItems, 0, 0L)
        c.prepare()
        c.play()
        _uiState.value = _uiState.value.copy(
            queue = newOrder,
            currentTrack = startTrack,
            shuffleEnabled = true,
            currentPlaylistName = playlistName
        )
        _progress.value = PlaybackProgress(
            durationMs = currentDurationMs(startTrack, preferTrackDuration = true)
        )
    }

    private fun reorderPlayerPlaylist(
        c: Player,
        fromOrder: List<AudioTrack>,
        toOrder: List<AudioTrack>
    ) {
        val working = fromOrder.toMutableList()
        for (target in toOrder.indices) {
            val desiredId = toOrder[target].id
            val currentPos = working.indexOfFirst { it.id == desiredId }
            if (currentPos < 0 || currentPos == target) continue
            runCatching { c.moveMediaItem(currentPos, target) }
            working.add(target, working.removeAt(currentPos))
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
            _navigationDirection.value = actualIndex.compareTo(currentStart).let { if (it < 0) -1 else 1 }
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
        finalizeStatsSession()
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
        glowColorJob?.cancel()
        val uri = track?.albumArtUri
        if (track == null || uri == null) {
            _uiState.value = _uiState.value.copy(glowColorArgb = DEFAULT_GLOW_COLOR)
            return
        }
        val trackId = track.id
        glowColorJob = viewModelScope.launch {
            val color = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver

                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { s -> BitmapFactory.decodeStream(s, null, bounds) }
                    val (w, h) = bounds.outWidth to bounds.outHeight
                    val targetSize = 128
                    var sampleSize = 1
                    if (w > 0 && h > 0) {
                        while ((w / (sampleSize * 2)) >= targetSize && (h / (sampleSize * 2)) >= targetSize) {
                            sampleSize *= 2
                        }
                    }
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    resolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream, null, decodeOptions) ?: return@use null
                        val palette = Palette.from(bmp).generate()
                        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch
                        swatch?.rgb
                    }
                }.getOrNull()
            }
            val current = _uiState.value.currentTrack
            if (current != null && current.id == trackId && current.albumArtUri == uri) {
                _uiState.value = _uiState.value.copy(glowColorArgb = color ?: DEFAULT_GLOW_COLOR)
            }
        }
    }

    private fun loadLyrics(track: AudioTrack?) {
        lyricsLoadJob?.cancel()
        lyricsSearchJob?.cancel()
        _lyricsSearchState.value = LyricsSearchUiState()
        if (track == null) {
            _lyricsSyncOffsetMs.value = 0
            _uiState.value = _uiState.value.copy(
                lyrics = emptyList(),
                syncedLyrics = emptyList(),
                lyricsVisible = false,
            )
            return
        }

        _lyricsSyncOffsetMs.value = 0
        lyricsLoadJob = viewModelScope.launch {
            // This coroutine is not wrapped by anything else. An uncaught exception here (bad
            // lyrics text, network or parser surprise) would crash the whole app, so any failure
            // is contained: the lyrics are cleared and the player keeps running.
            try {
                val mode = prefs.lyricsAutoSearchMode.first()

                // A lyrics result the user chose manually always wins.
                val saved = withContext(Dispatchers.IO) { loadSavedLyrics(track.id) }
                if (saved != null) {
                    applyLyricsDocument(track, saved)
                    return@launch
                }

                val embedded = withContext(Dispatchers.IO) { loadEmbeddedLyricsDocument(track) }
                val hasEmbedded = embedded != null && embedded.lines.isNotEmpty()
                when (mode) {
                    LyricsAutoSearchMode.OFF,
                    LyricsAutoSearchMode.ONLINE_FIRST -> applyLyricsDocument(track, embedded)

                    LyricsAutoSearchMode.EMBEDDED_FIRST -> {
                        if (hasEmbedded) {
                            applyLyricsDocument(track, embedded)
                        } else {
                            autoSearchLyricsAndApply(track, fallback = null)
                        }
                    }

                    LyricsAutoSearchMode.BEST_MATCH -> autoSearchLyricsAndApply(track, fallback = embedded)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                runCatching { applyLyricsDocument(track, null) }
            }
        }
    }

    private fun loadEmbeddedLyricsDocument(track: AudioTrack): LyricsDocument? = runCatching {
        val raw = loadLyricsFromRetriever(track.uri) ?: loadLyricsFromLrcFile(track)
        raw?.let(::parseLyricsDocument)
    }.getOrNull()

    private suspend fun applyLyricsDocument(track: AudioTrack, document: LyricsDocument?) {
        if (_uiState.value.currentTrack?.id != track.id) return
        val resolved = document ?: LyricsDocument(rawText = "", lines = emptyList(), syncedLines = emptyList())
        _uiState.value = _uiState.value.copy(
            lyrics = resolved.lines,
            syncedLyrics = resolved.syncedLines,
            lyricsVisible = resolved.lines.isNotEmpty(),
        )
        _lyricsSyncOffsetMs.value = loadLyricsSyncOffset(track.id)
    }

    /**
     * Searches online sources, applies the best-scoring candidate and stores it like a manual choice.
     * Falls back to [fallback] (embedded or .lrc lyrics) when no candidate is good enough.
     */
    private suspend fun autoSearchLyricsAndApply(track: AudioTrack, fallback: LyricsDocument?) {
        val best = findBestOnlineLyrics(track)
        if (_uiState.value.currentTrack?.id != track.id) return
        val onlineDocument = best?.let { parseLyricsDocument(it.rawLyrics) }
        if (onlineDocument != null && onlineDocument.lines.isNotEmpty()) {
            persistLyricsDocument(track.id, onlineDocument)
            applyLyricsDocument(track, onlineDocument)
        } else {
            applyLyricsDocument(track, fallback)
        }
    }

    private suspend fun findBestOnlineLyrics(track: AudioTrack): LyricsSearchResult? {
        fun usable(value: String) = value.trim().takeIf { it.isNotEmpty() && !it.equals("<unknown>", ignoreCase = true) }
        val title = usable(track.title)
        val artist = usable(track.artist)
        val queries = listOfNotNull(
            listOfNotNull(title, artist).joinToString(" ").takeIf { it.isNotBlank() },
            // Second round with the title alone: helps when the artist tag is romanized
            // ("Jay Chou") or otherwise written differently from the service's catalogue.
            title?.takeIf { artist != null },
        )
        for (query in queries) {
            val candidates = coroutineScope {
                val lrclib = async(Dispatchers.IO) { runCatching { searchLrclib(query) }.getOrDefault(emptyList()) }
                val qqMusic = async { runCatching { searchQqLyrics(query) }.getOrDefault(emptyList()) }
                lrclib.await() + qqMusic.await()
            }
            pickBestLyricsCandidate(candidates, track)?.let { return it }
            if (_uiState.value.currentTrack?.id != track.id) return null
        }
        return null
    }

    /**
     * Returns true the first time lyrics are opened for the current track in ONLINE_FIRST mode.
     * The lyrics screen then opens the online search results directly.
     */
    fun consumeFirstLyricsOpenSearch(): Boolean {
        val state = _uiState.value
        val trackId = state.currentTrack?.id ?: return false
        if (state.lyricsAutoSearchMode != LyricsAutoSearchMode.ONLINE_FIRST) return false
        if (!openedLyricsTrackIds.add(trackId)) return false
        viewModelScope.launch { prefs.addLyricsOnlineOpenedTrackId(trackId) }
        return true
    }

    fun setLyricsAutoSearchMode(mode: LyricsAutoSearchMode) {
        viewModelScope.launch { prefs.setLyricsAutoSearchMode(mode) }
    }

    fun setVolumeNormalization(mode: VolumeNormalization) {
        viewModelScope.launch { prefs.setVolumeNormalization(mode) }
    }

    private suspend fun loadSavedLyrics(trackId: Long): LyricsDocument? = runCatching {
        val json = prefs.lyricsJson.first()
        val value = org.json.JSONObject(json).opt(trackId.toString()) ?: return null
        when (value) {
            is org.json.JSONArray -> {
                val raw = (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }
                    .joinToString("\n")
                raw.takeIf(String::isNotBlank)?.let(::parseLyricsDocument)
            }
            is org.json.JSONObject -> {
                val raw = value.optString("rawText").takeIf(String::isNotBlank)
                    ?: value.optJSONArray("lines")?.let { lines ->
                        (0 until lines.length()).mapNotNull { lines.optString(it).takeIf(String::isNotBlank) }
                            .joinToString("\n")
                    }
                raw?.takeIf(String::isNotBlank)?.let(::parseLyricsDocument)
            }
            else -> null
        }
    }.getOrNull()

    private fun loadLyricsFromRetriever(uri: android.net.Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication(), uri)
            retriever.extractMetadata(28)?.takeIf(String::isNotBlank)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun loadLyricsFromLrcFile(track: AudioTrack): String? {
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
                        return stream.bufferedReader().readText()
                    }
                }
            }
        }
        return null
    }

    private fun parseLyricsDocument(content: String): LyricsDocument {
        val timestampRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
        val metadataRegex = Regex("\\[[A-Za-z]+:[^]]*]")
        val synced = mutableListOf<SyncedLyricLine>()
        val plain = mutableListOf<String>()

        content.lineSequence().forEach { sourceLine ->
            val tags = timestampRegex.findAll(sourceLine).toList()
            if (tags.isNotEmpty()) {
                val text = sourceLine
                    .replace(timestampRegex, "")
                    .replace(metadataRegex, "")
                    .trim()
                if (text.isNotBlank()) {
                    tags.forEach { tag ->
                        val minutes = tag.groupValues[1].toLongOrNull() ?: 0L
                        val seconds = tag.groupValues[2].toLongOrNull() ?: 0L
                        val fractionText = tag.groupValues.getOrNull(3).orEmpty()
                        val fraction = fractionText.toLongOrNull() ?: 0L
                        val millis = when (fractionText.length) {
                            1 -> fraction * 100L
                            2 -> fraction * 10L
                            else -> fraction
                        }
                        synced += SyncedLyricLine((minutes * 60L + seconds) * 1000L + millis, text)
                    }
                }
            } else {
                val text = sourceLine.replace(metadataRegex, "").trim()
                if (text.isNotBlank()) plain += text
            }
        }

        val sortedSynced = synced.sortedBy { it.timeMs }
        val lines = if (sortedSynced.isNotEmpty()) sortedSynced.map { it.text } else plain
        return LyricsDocument(content, lines, sortedSynced)
    }

    private suspend fun loadLyricsSyncOffset(trackId: Long): Int = runCatching {
        val offsets = org.json.JSONObject(prefs.lyricsSyncOffsetsJson.first())
        offsets.optInt(trackId.toString(), 0).coerceIn(-10_000, 10_000)
    }.getOrDefault(0)

    fun toggleLyricsVisible() {
        _uiState.value = _uiState.value.copy(lyricsVisible = !_uiState.value.lyricsVisible)
    }

    fun parseLrcPublic(content: String): List<String> = parseLyricsDocument(content).lines

    fun setManualLyrics(lines: List<String>) = setManualLyricsText(lines.joinToString("\n"))

    fun setManualLyricsText(rawText: String) {
        val document = parseLyricsDocument(rawText)
        if (document.lines.isEmpty()) return
        val trackId = _uiState.value.currentTrack?.id ?: return
        _uiState.value = _uiState.value.copy(
            lyrics = document.lines,
            syncedLyrics = document.syncedLines,
            lyricsVisible = true,
        )
        viewModelScope.launch { persistLyricsDocument(trackId, document) }
    }

    private suspend fun persistLyricsDocument(trackId: Long, document: LyricsDocument) {
        val json = prefs.lyricsJson.first()
        val obj = runCatching { org.json.JSONObject(json) }.getOrNull() ?: org.json.JSONObject()
        val payload = org.json.JSONObject().apply {
            put("rawText", document.rawText)
            put("lines", org.json.JSONArray(document.lines))
            put("syncedLyrics", org.json.JSONArray().apply {
                document.syncedLines.forEach { line ->
                    put(org.json.JSONObject().put("timeMs", line.timeMs).put("text", line.text))
                }
            })
        }
        obj.put(trackId.toString(), payload)
        prefs.setLyricsJson(obj.toString())
    }

    fun searchLyrics(query: String) {
        val cleanedQuery = query.trim()
        val trackId = _uiState.value.currentTrack?.id
        lyricsSearchJob?.cancel()
        if (cleanedQuery.isBlank()) {
            _lyricsSearchState.value = LyricsSearchUiState(query = cleanedQuery, error = "empty")
            return
        }
        _lyricsSearchState.value = LyricsSearchUiState(query = cleanedQuery, isLoading = true)
        lyricsSearchJob = viewModelScope.launch {
            try {
                val (lrclibAttempt, qqAttempt) = coroutineScope {
                    val lrclib = async(Dispatchers.IO) { runCatching { searchLrclib(cleanedQuery) } }
                    val qqMusic = async {
                        try {
                            Result.success(searchQqLyrics(cleanedQuery))
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Result.failure(error)
                        }
                    }
                    lrclib.await() to qqMusic.await()
                }
                if (trackId != _uiState.value.currentTrack?.id) return@launch
                val results = (lrclibAttempt.getOrDefault(emptyList()) + qqAttempt.getOrDefault(emptyList()))
                    .distinctBy { "${it.source}|${it.sourceId ?: it.id ?: (it.title.trim().lowercase() + "|" + it.artist.trim().lowercase())}" }
                    .take(30)
                val networkFailed = results.isEmpty() && lrclibAttempt.isFailure && qqAttempt.isFailure
                _lyricsSearchState.value = LyricsSearchUiState(
                    query = cleanedQuery,
                    results = results,
                    error = if (results.isNotEmpty()) null else if (networkFailed) "network" else "not_found",
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (trackId == _uiState.value.currentTrack?.id) {
                    _lyricsSearchState.value = LyricsSearchUiState(query = cleanedQuery, error = "network")
                }
            }
        }
    }

    fun searchLyricsForCurrentTrack() {
        val track = _uiState.value.currentTrack ?: return
        searchLyrics(track.title.trim())
    }

    fun selectLyricsSearchResult(result: LyricsSearchResult) {
        setManualLyricsText(result.rawLyrics)
        clearLyricsSearch()
    }

    fun clearLyricsSearch() {
        lyricsSearchJob?.cancel()
        lyricsSearchJob = null
        _lyricsSearchState.value = LyricsSearchUiState()
    }

    private fun searchLrclib(query: String): List<LyricsSearchResult> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val connection = (java.net.URL("https://lrclib.net/api/search?q=$encodedQuery").openConnection()
            as java.net.HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LambdaPlayer/2.5")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) throw java.io.IOException("LRCLIB returned HTTP $responseCode")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(body)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    fun text(key: String): String? = item.optString(key)
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val plain = text("plainLyrics")
                    val synced = text("syncedLyrics")
                    if (plain == null && synced == null) continue
                    add(
                        LyricsSearchResult(
                            id = item.optLong("id").takeIf { it > 0L },
                            title = text("trackName") ?: query,
                            artist = text("artistName") ?: "",
                            album = text("albumName") ?: "",
                            durationSeconds = item.optDouble("duration")
                                .takeIf { it.isFinite() && it > 0.0 },
                            instrumental = item.optBoolean("instrumental", false),
                            plainLyrics = plain,
                            syncedLyrics = synced,
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private data class QqSongCandidate(
        val songMid: String,
        val songId: Long?,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Double?,
    )

    private suspend fun searchQqLyrics(query: String): List<LyricsSearchResult> = coroutineScope {
        val candidates = withContext(Dispatchers.IO) { searchQqSongCandidates(query) }.take(6)
        candidates.map { candidate ->
            async(Dispatchers.IO) { runCatching { fetchQqLyrics(candidate) }.getOrNull() }
        }.awaitAll().filterNotNull()
    }

    /**
     * QQ Music song search. The old `c.y.qq.com/soso/fcgi-bin/client_search_cp` endpoint now
     * answers every request with HTTP 500, so QQ Music never returned anything. The search now
     * goes through the `musicu.fcg` gateway used by the QQ Music apps, with the smartbox
     * suggestion endpoint as a fallback.
     */
    private fun searchQqSongCandidates(query: String): List<QqSongCandidate> {
        val primary = runCatching { searchQqSongsViaMusicu(query) }.getOrDefault(emptyList())
        if (primary.isNotEmpty()) return primary
        return runCatching { searchQqSongsViaSmartbox(query) }.getOrDefault(emptyList())
    }

    private fun searchQqSongsViaMusicu(query: String): List<QqSongCandidate> {
        val payload = org.json.JSONObject()
            .put(
                "comm",
                org.json.JSONObject()
                    .put("ct", 11)
                    .put("cv", "1003006")
                    .put("v", "1003006")
                    .put("os_ver", "12")
                    .put("phonetype", "0")
                    .put("tmeAppID", "qqmusic")
                    .put("nettype", "NETWORK_WIFI")
                    .put("uin", "0")
            )
            .put(
                "req",
                org.json.JSONObject()
                    .put("module", "music.search.SearchCgiService")
                    .put("method", "DoSearchForQQMusicMobile")
                    .put(
                        "param",
                        org.json.JSONObject()
                            .put("search_type", 0)
                            .put("query", query)
                            .put("page_num", 1)
                            .put("num_per_page", 12)
                            .put("highlight", 0)
                            .put("nqc_flag", 0)
                            .put("multi_zhida", 0)
                            .put("cat", 2)
                            .put("grp", 1)
                            .put("sin", 0)
                            .put("sem", 0)
                    )
            )
        val connection = (java.net.URL("https://u.y.qq.com/cgi-bin/musicu.fcg").openConnection()
            as java.net.HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json,text/plain,*/*")
            setRequestProperty("Referer", "https://y.qq.com/")
            setRequestProperty("User-Agent", QQ_USER_AGENT)
        }
        return try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) throw java.io.IOException("QQ Music search returned HTTP $responseCode")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val songs = org.json.JSONObject(body)
                .optJSONObject("req")
                ?.optJSONObject("data")
                ?.optJSONObject("body")
                ?.let { it.optJSONArray("item_song") ?: it.optJSONObject("song")?.optJSONArray("list") }
                ?: return emptyList()
            buildList {
                val seen = HashSet<String>()
                for (index in 0 until songs.length()) {
                    val item = songs.optJSONObject(index) ?: continue
                    fun text(obj: org.json.JSONObject?, vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                        obj?.optString(key)?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    }
                    val songMid = text(item, "mid", "songmid") ?: continue
                    if (!seen.add(songMid)) continue
                    val title = text(item, "name", "title", "songname") ?: continue
                    val singers = item.optJSONArray("singer")
                    val artist = if (singers != null) {
                        (0 until singers.length()).mapNotNull { singerIndex ->
                            text(singers.optJSONObject(singerIndex), "name", "title")
                        }.joinToString(" / ")
                    } else ""
                    add(
                        QqSongCandidate(
                            songMid = songMid,
                            songId = item.optLong("id", item.optLong("songid")).takeIf { it > 0L },
                            title = title,
                            artist = artist,
                            album = text(item.optJSONObject("album"), "name", "title") ?: "",
                            durationSeconds = item.optDouble("interval", -1.0).takeIf { it > 0.0 },
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Fallback: the search-suggestion endpoint. It returns few songs and no durations. */
    private fun searchQqSongsViaSmartbox(query: String): List<QqSongCandidate> {
        val params = listOf(
            "key" to query,
            "format" to "json",
            "inCharset" to "utf-8",
            "outCharset" to "utf-8",
        ).joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        val connection = (java.net.URL("https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?$params").openConnection()
            as java.net.HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json,text/plain,*/*")
            setRequestProperty("Referer", "https://y.qq.com/")
            setRequestProperty("User-Agent", QQ_USER_AGENT)
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) throw java.io.IOException("QQ Music smartbox returned HTTP $responseCode")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val songs = org.json.JSONObject(body)
                .optJSONObject("data")
                ?.optJSONObject("song")
                ?.optJSONArray("itemlist")
                ?: return emptyList()
            buildList {
                for (index in 0 until songs.length()) {
                    val item = songs.optJSONObject(index) ?: continue
                    val songMid = item.optString("mid").takeIf { it.isNotBlank() } ?: continue
                    val title = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    add(
                        QqSongCandidate(
                            songMid = songMid,
                            songId = item.optString("id").toLongOrNull()?.takeIf { it > 0L },
                            title = title,
                            artist = item.optString("singer"),
                            album = "",
                            durationSeconds = null,
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchQqLyrics(candidate: QqSongCandidate): LyricsSearchResult? {
        val queryParams = buildList {
            add("songmid" to candidate.songMid)
            candidate.songId?.let { add("musicid" to it.toString()) }
            add("format" to "json")
            add("nobase64" to "1")
            add("g_tk" to "5381")
            add("loginUin" to "0")
            add("hostUin" to "0")
            add("inCharset" to "utf8")
            add("outCharset" to "utf-8")
            add("notice" to "0")
            add("platform" to "yqq.json")
            add("needNewCode" to "0")
        }.joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        val connection = (java.net.URL("https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?$queryParams").openConnection()
            as java.net.HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json,text/plain,*/*")
            setRequestProperty("Origin", "https://y.qq.com")
            setRequestProperty("Referer", "https://y.qq.com/portal/player.html")
            setRequestProperty("User-Agent", QQ_USER_AGENT)
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val jsonStart = body.indexOf('{')
            val jsonEnd = body.lastIndexOf('}')
            if (jsonStart < 0 || jsonEnd < jsonStart) return null
            val json = org.json.JSONObject(body.substring(jsonStart, jsonEnd + 1))
            val lyricText = json.optString("lyric").takeIf {
                it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("无", ignoreCase = true)
            } ?: return null
            // Html.fromHtml() was used here before; it collapses every newline into a space, so
            // QQ lyrics arrived as one single line. Entities are now decoded by hand instead.
            val decodedLyrics = decodeQqLyricText(lyricText).replace("\r", "").trim()
            if (decodedLyrics.isBlank()) return null
            // QQ returns a placeholder sentence for instrumentals; treat those as "no lyrics".
            if (QQ_INSTRUMENTAL_MARKERS.any { decodedLyrics.contains(it) } &&
                decodedLyrics.lineSequence().count { it.isNotBlank() } <= 3
            ) return null
            val hasTimestamps = Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?]").containsMatchIn(decodedLyrics)
            LyricsSearchResult(
                id = candidate.songId,
                title = candidate.title,
                artist = candidate.artist,
                album = candidate.album,
                durationSeconds = candidate.durationSeconds,
                instrumental = false,
                plainLyrics = decodedLyrics.takeUnless { hasTimestamps },
                syncedLyrics = decodedLyrics.takeIf { hasTimestamps },
                source = "QQ Music",
                sourceId = candidate.songMid,
            )
        } finally {
            connection.disconnect()
        }
    }

    /**
     * QQ lyrics are plain LRC text, older responses also contain HTML entities such as `&#58;`
     * or `&#10;`. Decode those without touching the real line breaks.
     */
    private fun decodeQqLyricText(raw: String): String {
        if (!raw.contains('&')) return raw
        return Regex("&(#[xX][0-9a-fA-F]+|#\\d+|amp|lt|gt|quot|apos|nbsp);").replace(raw) { match ->
            val entity = match.groupValues[1]
            when {
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    entity.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: match.value
                entity.startsWith("#") ->
                    entity.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: match.value
                entity == "amp" -> "&"
                entity == "lt" -> "<"
                entity == "gt" -> ">"
                entity == "quot" -> "\""
                entity == "apos" -> "'"
                entity == "nbsp" -> " "
                else -> match.value
            }
        }
    }

    fun setLyricsSyncOffset(offsetMs: Int) {
        val trackId = _uiState.value.currentTrack?.id ?: return
        val safeOffset = offsetMs.coerceIn(-10_000, 10_000)
        _lyricsSyncOffsetMs.value = safeOffset
        viewModelScope.launch {
            val offsets = runCatching {
                org.json.JSONObject(prefs.lyricsSyncOffsetsJson.first())
            }.getOrElse { org.json.JSONObject() }
            offsets.put(trackId.toString(), safeOffset)
            prefs.setLyricsSyncOffsetsJson(offsets.toString())
        }
    }

    fun notifyTrackUpdated(updatedTrack: AudioTrack) {
        val current = _uiState.value
        val updatedQueue = current.queue.map { if (it.id == updatedTrack.id) updatedTrack else it }
        queueTracks = queueTracks.map { if (it.id == updatedTrack.id) updatedTrack else it }
        val updatedCurrent = if (current.currentTrack?.id == updatedTrack.id) updatedTrack else current.currentTrack
        _uiState.value = current.copy(queue = updatedQueue, currentTrack = updatedCurrent)
        if (updatedCurrent?.id == updatedTrack.id) {
            extractGlowColor(updatedTrack)
        }
    }

    fun addTrackToQueue(track: AudioTrack) {
        val c = controller ?: return
        val current = _uiState.value
        val currentIdx = queueTracks.indexOfFirst { it.id == current.currentTrack?.id }.coerceAtLeast(0)
        val insertIdx = if (remixActive) {
            val remaining = queueTracks.size - currentIdx - 1
            if (remaining > 0) currentIdx + 1 + (0 until remaining).random()
            else queueTracks.size
        } else {
            queueTracks.size
        }
        val newQueue = queueTracks.toMutableList().also { it.add(insertIdx, track) }
        queueTracks = newQueue

        val mediaItem = MediaItem.Builder()
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
        c.addMediaItem(insertIdx, mediaItem)
        _uiState.value = current.copy(queue = newQueue)
    }

    private fun startAmplitudePulse() {
        viewModelScope.launch {
            var t = 0f
            while (true) {
                t += 0.12f
                val playing = _uiState.value.isPlaying
                val target = if (playing) (0.55f + 0.45f * ((sin(t.toDouble()) + 1.0) / 2.0).toFloat())
                else 0f
                _amplitude.value = target
                delay(80)
            }
        }
    }

    init {
        startAmplitudePulse()
    }
}
