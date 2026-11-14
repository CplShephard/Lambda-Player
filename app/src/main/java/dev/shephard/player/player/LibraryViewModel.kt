package dev.shephard.player.player

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.shephard.player.data.AudioTrack
import dev.shephard.player.data.MediaStoreScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

data class TrackOverride(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasScanned = MutableStateFlow(false)
    val hasScanned: StateFlow<Boolean> = _hasScanned.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val filteredTracks: StateFlow<List<AudioTrack>> = _filteredTracks.asStateFlow()

    private val _overrides = MutableStateFlow<Map<String, TrackOverride>>(emptyMap())
    val overrides: StateFlow<Map<String, TrackOverride>> = _overrides.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var scanJob: Job? = null
    private var scanGeneration = 0L
    private var rawTracks: List<AudioTrack> = emptyList()
    private val overridesReady = CompletableDeferred<Unit>()

    init {
        registerObserver()
        viewModelScope.launch {
            try {
                prefs.trackOverridesJson.collect { json ->
                    _overrides.value = parseOverrides(json)
                    if (!overridesReady.isCompleted) overridesReady.complete(Unit)
                    if (_hasScanned.value) {
                        _tracks.value = applyOverridesToList(rawTracks, _overrides.value)
                        applyFilter()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Could not read track overrides", e)
                if (!overridesReady.isCompleted) overridesReady.complete(Unit)
            }
        }
    }

    private fun registerObserver() {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                loadTracks()
            }
        }
        getApplication<Application>().contentResolver.registerContentObserver(
            uri, true, contentObserver!!
        )
    }

    fun loadTracks() {
        val generation = ++scanGeneration
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                // Never publish raw MediaStore metadata before persisted edits are loaded.
                overridesReady.await()
                val result = withContext(Dispatchers.IO) {
                    MediaStoreScanner.queryAudioTracks(getApplication())
                }
                rawTracks = result
                _tracks.value = applyOverridesToList(result, _overrides.value)
                _hasScanned.value = true
                applyFilter()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed scan must not replace a previously valid library with an empty one.
                android.util.Log.e("LibraryViewModel", "Library scan failed", e)
            } finally {
                if (generation == scanGeneration) _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun saveTrackOverride(trackId: Long, title: String, artist: String, album: String, coverUri: String?) {
        viewModelScope.launch {
            try {
                prefs.updateTrackOverride(trackId, TrackOverride(title, artist, album, coverUri))
                // DataStore's flow updates all active library screens with the merged result.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Could not save track override", e)
            }
        }
    }

    fun getOverride(trackId: Long): TrackOverride? = _overrides.value[trackId.toString()]

    private fun applyOverridesToList(tracks: List<AudioTrack>, overrides: Map<String, TrackOverride>): List<AudioTrack> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }

        return tracks.map { track ->
            val ov = overrides[track.id.toString()] ?: return@map track
            track.copy(
                title = ov.title?.takeIf { it.isNotBlank() } ?: track.title,
                artist = ov.artist?.takeIf { it.isNotBlank() } ?: track.artist,
                album = ov.album?.takeIf { it.isNotBlank() } ?: track.album,
                albumArtUri = ov.coverUri?.let { Uri.parse(it) } ?: track.albumArtUri
            )
        }.sortedWith(trackTitleComparator(collator))
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase(Locale.getDefault())
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        val filtered = if (query.isEmpty()) {
            _tracks.value
        } else {
            _tracks.value.filter { track ->
                track.title.lowercase(Locale.getDefault()).contains(query) ||
                track.artist.lowercase(Locale.getDefault()).contains(query) ||
                track.album.lowercase(Locale.getDefault()).contains(query)
            }
        }
        _filteredTracks.value = filtered.sortedWith(trackTitleComparator(collator))
    }

    private fun trackTitleComparator(collator: Collator): Comparator<AudioTrack> = Comparator { a, b ->
        val byTitle = collator.compare(a.title.trim(), b.title.trim())
        if (byTitle != 0) return@Comparator byTitle
        val byArtist = collator.compare(a.artist.trim(), b.artist.trim())
        if (byArtist != 0) return@Comparator byArtist
        a.id.compareTo(b.id)
    }

    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
        super.onCleared()
    }

    companion object {
        fun parseOverrides(json: String): Map<String, TrackOverride> {
            return try {
                val obj = JSONObject(json)
                val map = mutableMapOf<String, TrackOverride>()
                obj.keys().forEach { key ->
                    val v = obj.getJSONObject(key)
                    map[key] = TrackOverride(
                        title = v.optString("title").takeIf { it.isNotEmpty() },
                        artist = v.optString("artist").takeIf { it.isNotEmpty() },
                        album = v.optString("album").takeIf { it.isNotEmpty() },
                        coverUri = v.optString("coverUri").takeIf { it.isNotEmpty() }
                    )
                }
                map
            } catch (_: Exception) { emptyMap() }
        }

        fun encodeOverrides(map: Map<String, TrackOverride>): String {
            val obj = JSONObject()
            map.forEach { (key, ov) ->
                val v = JSONObject()
                ov.title?.let { v.put("title", it) }
                ov.artist?.let { v.put("artist", it) }
                ov.album?.let { v.put("album", it) }
                ov.coverUri?.let { v.put("coverUri", it) }
                obj.put(key, v)
            }
            return obj.toString()
        }
    }
}
