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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

    // trackId.toString() -> TrackOverride
    private val _overrides = MutableStateFlow<Map<String, TrackOverride>>(emptyMap())
    val overrides: StateFlow<Map<String, TrackOverride>> = _overrides.asStateFlow()

    private var contentObserver: ContentObserver? = null

    init {
        registerObserver()
        viewModelScope.launch {
            val json = prefs.trackOverridesJson.first()
            _overrides.value = parseOverrides(json)
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
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                MediaStoreScanner.queryAudioTracks(getApplication())
            }
            _tracks.value = applyOverridesToList(result, _overrides.value)
            _isLoading.value = false
            _hasScanned.value = true
            applyFilter()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun saveTrackOverride(trackId: Long, title: String, artist: String, album: String, coverUri: String?) {
        viewModelScope.launch {
            val current = _overrides.value.toMutableMap()
            current[trackId.toString()] = TrackOverride(title, artist, album, coverUri)
            _overrides.value = current
            val json = encodeOverrides(current)
            prefs.setTrackOverridesJson(json)
            // Re-apply overrides to current raw track list
            val rawJson = prefs.trackOverridesJson.first()
            val newOverrides = parseOverrides(rawJson)
            val raw = withContext(Dispatchers.IO) {
                MediaStoreScanner.queryAudioTracks(getApplication())
            }
            _tracks.value = applyOverridesToList(raw, newOverrides)
            applyFilter()
        }
    }

    fun getOverride(trackId: Long): TrackOverride? = _overrides.value[trackId.toString()]

    private fun applyOverridesToList(tracks: List<AudioTrack>, overrides: Map<String, TrackOverride>): List<AudioTrack> {
        return tracks.map { track ->
            val ov = overrides[track.id.toString()] ?: return@map track
            track.copy(
                title = ov.title?.takeIf { it.isNotBlank() } ?: track.title,
                artist = ov.artist?.takeIf { it.isNotBlank() } ?: track.artist,
                album = ov.album?.takeIf { it.isNotBlank() } ?: track.album,
                albumArtUri = ov.coverUri?.let { Uri.parse(it) } ?: track.albumArtUri
            )
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        _filteredTracks.value = if (query.isEmpty()) {
            _tracks.value
        } else {
            _tracks.value.filter { track ->
                track.title.lowercase().contains(query) ||
                track.artist.lowercase().contains(query) ||
                track.album.lowercase().contains(query)
            }
        }
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
