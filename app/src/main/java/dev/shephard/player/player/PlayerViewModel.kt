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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

data class SyncedLyricLine(val timeMs: Long, val text: String)

/**
 * Çalma konumu, [PlayerUiState]'ten AYRI tutulur.
 *
 * Eskiden `positionMs`/`durationMs` doğrudan `PlayerUiState` içindeydi ve 500 ms'de bir
 * güncelleniyordu. `uiState`'i toplayan HER composable (NowPlayingSheet'in tamamı dahil —
 * kapak swipe grid'i, gradientler, butonlar, drawer'lar) saniyede iki kez yeniden
 * besteleniyordu. NowPlaying'deki kasmanın asıl sebebi buydu.
 *
 * Artık konum yalnızca [PlayerViewModel.progress] üzerinden yayınlanıyor ve bunu sadece
 * seek bar / süre etiketleri / lyrics gibi küçük yaprak composable'lar dinliyor.
 */
data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

data class PlayerUiState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
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
    val syncedLyrics: List<SyncedLyricLine> = emptyList(),
    val lyricsVisible: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Çalma konumu 500 ms'de bir SADECE bu flow üzerinden yayınlanır.
    // uiState'e yazılmadığı için NowPlayingSheet'in tamamı artık her tikte recompose olmaz.
    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

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

    // The animated NowPlaying glow must not live inside PlayerUiState: updating that
    // data class every ~80ms invalidates MainContainer/NavGraph/Settings while music is
    // playing. Keep it in a tiny dedicated flow that only NowPlayingSheet observes.
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    // Live listening time for the Settings widget. It updates in memory every tick so the
    // counter moves second-by-second, while DataStore writes remain batched for performance.
    private val _totalListeningMsLive = MutableStateFlow(0L)
    val totalListeningMsLive: StateFlow<Long> = _totalListeningMsLive.asStateFlow()

    // -1 = backward, 1 = forward, 0 = unknown
    private val _navigationDirection = MutableStateFlow(1)
    val navigationDirection: StateFlow<Int> = _navigationDirection.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                val now = System.currentTimeMillis()
                lastPlaybackTickMs = now
                lastListeningStoreWriteMs = now
            } else {
                flushListeningTime()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushListeningTime()
            val track = queueTracks.find { it.id.toString() == mediaItem?.mediaId }
            _uiState.value = _uiState.value.copy(currentTrack = track)
            _progress.value = PlaybackProgress(
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
            _progress.value = _progress.value.copy(
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
            isPlaying = c.isPlaying,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = mode
        )
        _progress.value = PlaybackProgress(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L)
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
                    // SADECE _progress güncellenir; _uiState'e dokunulmaz.
                    // Böylece uiState toplayan hiçbir composable bu tikten etkilenmez.
                    _progress.value = PlaybackProgress(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = c.duration.coerceAtLeast(0L)
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

    private fun accrueListeningTime() {
        val now = System.currentTimeMillis()
        val last = lastPlaybackTickMs
        if (last != null) {
            val delta = (now - last).coerceIn(0L, 2000L)
            if (delta > 0) addListeningDeltaInMemory(delta, updateUiState = false)
        }
        lastPlaybackTickMs = now

        // Do not write DataStore every 500ms. That was the hidden Settings jank source:
        // every write emitted totalListeningMs, causing collectors to wake up constantly.
        // Batch writes while keeping exact time in memory and flush on pause/track switch.
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
            // Embedded album art'ı arka planda çek
            val artUri: Uri? = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(getApplication(), uri)
                    val bytes = retriever.embeddedPicture
                    retriever.release()
                    if (bytes != null) {
                        // Bitmap'i app cache'e yaz, content URI olarak dön
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

        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentTrack = tracks.getOrNull(startIndex),
            currentPlaylistName = playlistName
        )
        _progress.value = PlaybackProgress()
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

    /**
     * Remix, SADECE çalan parçadan SONRAKİ parçaları karıştırır/geri alır.
     *
     * Eski davranış çalan parçayı da index 0'a taşıyordu; bu yüzden art arda basınca
     * player'daki `moveMediaItem` çağrıları çalan parçayı oynatıyor ve NowPlayingSheet'teki
     * kapak grid'i "şu an çalan / sıradaki şarkı yer değiştirdi" gibi görünüyordu.
     * Artık çalan parça ve öncesindeki parçalar HİÇ dokunulmadan kalır — remix'in tek
     * görünür etkisi Queue drawer'ındaki sıradaki şarkı listesidir.
     */
    fun remixQueue() {
        val c = controller ?: return
        val current = _uiState.value.queue
        val currentTrack = _uiState.value.currentTrack
        if (current.size <= 1 || currentTrack == null) return

        val currentIndex = current.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
        val head = current.take(currentIndex + 1)            // çalan parça dahil, dokunulmaz
        val upcoming = current.drop(currentIndex + 1)        // çalan parçadan sonrası

        if (remixActive) {
            // Geri al: sıradaki parçaları orijinal göreli sıralarına döndür.
            // Orijinal sıradan yalnızca hâlâ "sıradaki" kümede olan parçalar alınır;
            // böylece bu arada queue'dan silinen/eklenen parçalar sorun çıkarmaz.
            val upcomingIds = upcoming.map(AudioTrack::id).toSet()
            val restoredUpcoming = originalQueue.filter { it.id in upcomingIds }
            val restoreQueue = if (restoredUpcoming.size == upcoming.size) head + restoredUpcoming else current
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

        // MADDE 7 — Eğer sırada (çalan parçadan sonra) karıştırılacak parça yoksa ya da tek bir
        // parça kaldıysa remix yine de çalışmalı: TÜM sırayı yeniden karıştır. Böylece son şarkıya
        // gelinse bile remix butonu işe yarıyor.
        //
        // MADDE 11 (glitch fix) — Çalan parçayı moveMediaItem ile taşımıyoruz (yerinde tutuyoruz);
        // sadece çalan parça dışındaki parçalar karıştırılıp çalan parçanın ORİJİNAL index'ine geri
        // konur. Çünkü çalan parçayı taşımak ExoPlayer'ın onMediaItemTransition fırlatıp
        // currentTrack/progress'i sıfırlamasına ve cover'ın 1 saniye yeniden yüklenmesine (glitch)
        // yol açıyordu. Çalan parça taşınmadığı için transition tetiklenmez, gerçek remix olur.
        if (upcoming.size <= 1) {
            originalQueue = current
            val currentIndex = current.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
            val rest = current.filterNot { it.id == currentTrack.id }.shuffled()
            // Çalan parçayı orijinal index'ine koy, diğerlerini (karıştırılmış) çevresine diz.
            val newOrder = rest.toMutableList().apply { add(currentIndex, currentTrack) }
            reorderPlayerPlaylist(c, current, newOrder)
            queueTracks = newOrder
            _uiState.value = _uiState.value.copy(queue = newOrder, shuffleEnabled = true)
            remixActive = true
            _isRemixed.value = true
            return
        }

        originalQueue = current
        val newOrder = head + upcoming.shuffled()
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
        _progress.value = PlaybackProgress()
    }

    /**
     * Reorders the player's playlist from [fromOrder] into [toOrder] using
     * ExoPlayer's [Player.moveMediaItem]. Unlike setMediaItems, moveMediaItem never
     * re-prepares the currently playing item, so playback continues uninterrupted
     * while the upcoming queue is reshuffled.
     */
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
                    // Palette sadece renk dağılımıyla ilgilendiği için kapak resmini
                    // düşük çözünürlükte decode etmek yeterli — tam boyutlu (örn. 3000x3000)
                    // bir bitmap'i decode edip Palette'e vermek hem CPU hem bellek israfı,
                    // ve şarkı geçişlerindeki lag'in ana kaynaklarından biriydi.
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
                // 1) Kullanıcının daha önce indirip kalıcı kaydettiği lyrics (en yüksek öncelik)
                loadSavedLyrics(track.id)
                    // 2) Yoksa gömülü lyrics / .lrc dosyası dene
                    ?: runCatching {
                        loadLyricsFromRetriever(track.uri)
                            ?: loadLyricsFromLrcFile(track)
                    }.getOrNull()
                    ?: emptyList()
            }
            _uiState.value = _uiState.value.copy(lyrics = lyrics, lyricsVisible = lyrics.isNotEmpty())
        }
    }

    /** Prefs'te trackId'ye göre kaydedilmiş lyrics varsa döndürür. */
    private suspend fun loadSavedLyrics(trackId: Long): List<String>? {
        return runCatching {
            val json = prefs.lyricsJson.first()
            val obj = org.json.JSONObject(json)
            val arr = obj.optJSONArray(trackId.toString()) ?: return null
            (0 until arr.length()).map { arr.getString(it) }.takeIf { it.isNotEmpty() }
        }.getOrNull()
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
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        val synced = content.lines().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val ms = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            val text = match.groupValues[4].trim()
            if (text.isEmpty()) null else SyncedLyricLine((min * 60 + sec) * 1000 + ms, text)
        }
        if (synced.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(syncedLyrics = synced)
            return synced.map { it.text }
        }
        _uiState.value = _uiState.value.copy(syncedLyrics = emptyList())
        return content.lines().map { it.trim() }.filter { it.isNotBlank() }
    }

    fun toggleLyricsVisible() {
        _uiState.value = _uiState.value.copy(lyricsVisible = !_uiState.value.lyricsVisible)
    }

    fun parseLrcPublic(content: String): List<String> = parseLrc(content)

    /**
     * Metadata override kaydedildikten sonra çağrılır.
     * currentTrack ve queue'deki ilgili track'i anında günceller →
     * NowPlayingSheet ve MiniPlayer yeni title/cover'ı hemen gösterir.
     */
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

    /**
     * Playlist'e yeni track eklendiğinde queue'ye de ekler.
     * remixActive = true → rastgele pozisyona yerleştirir (currentTrack'ten sonra)
     * remixActive = false → queue'nun sonuna ekler
     */
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

    fun setManualLyrics(lines: List<String>) {
        _uiState.value = _uiState.value.copy(lyrics = lines, lyricsVisible = true)
        // Kalıcı olarak kaydet — uygulamadan çıkıp tekrar girince lyrics kaybolmasın.
        val trackId = _uiState.value.currentTrack?.id ?: return
        if (lines.isEmpty()) return
        viewModelScope.launch {
            val json = prefs.lyricsJson.first()
            val obj = runCatching { org.json.JSONObject(json) }.getOrNull() ?: org.json.JSONObject()
            val arr = org.json.JSONArray()
            lines.forEach { arr.put(it) }
            obj.put(trackId.toString(), arr)
            prefs.setLyricsJson(obj.toString())
        }
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
