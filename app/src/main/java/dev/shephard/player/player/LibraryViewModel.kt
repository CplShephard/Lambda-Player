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
import dev.shephard.player.data.parseTrackCacheJson
import dev.shephard.player.data.toCacheJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    // trackId.toString() -> TrackOverride
    private val _overrides = MutableStateFlow<Map<String, TrackOverride>>(emptyMap())
    val overrides: StateFlow<Map<String, TrackOverride>> = _overrides.asStateFlow()

    private var contentObserver: ContentObserver? = null

    // OuterTune tarzı cooldown: son başarılı MediaStore taramasından sonra bu süre (ms)
    // geçmeden tekrar MediaStore sorgulanmaz. Böylece her uygulama açılışında şarkılar
    // baştan taranmaz — önbellekten gelir, tarama yalnızca cooldown dolunca ya da medya
    // gerçekten değişince yapılır.
    private val scanCooldownMs: Long = 6 * 60 * 60 * 1000L // 6 saat (OuterTune AUTO_SCAN_COOLDOWN'a benzer)

    init {
        registerObserver()
        viewModelScope.launch {
            val json = prefs.trackOverridesJson.first()
            _overrides.value = parseOverrides(json)
            // Önbelleği hemen yükle (anında gösterim) — asıl MediaStore taraması aşağıda,
            // cooldown'a tabi.
            refreshLibrary(force = false)
        }
    }

    private fun registerObserver() {
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Medya gerçekten değişti (şarkı eklendi/silindi) → cooldown'u beklemeden tazele.
                refreshLibrary(force = true)
            }
        }
        getApplication<Application>().contentResolver.registerContentObserver(
            uri, true, contentObserver!!
        )
    }

    /** Eski loadTracks arayüzü — init ve ContentObserver üzerinden gelen çağrılar için. */
    fun loadTracks() {
        refreshLibrary(force = true)
    }

    /**
     * Kütüphaneyi yükler.
     *
     *  - Önce önbellek (DataStore) boş değilse onu gösterir → anında açılır, tarama beklemez.
     *  - Sonra MediaStore'u tarar; ama `force=false` iken ve cooldown dolmamışsa MediaStore'u
     *    ATLAR (önbellekteki liste kullanılır). `force=true` ise (medya gerçekten değiştiyse
     *    ya da ilk açılışta cache yoksa) her zaman taze tarar.
     */
    fun refreshLibrary(force: Boolean = false) {
        viewModelScope.launch {
            // 1) Önbelleği hemen göster (varsa)
            val cacheJson = prefs.libraryCacheJson.first()
            val cached = parseTrackCacheJson(cacheJson)
            if (cached.isNotEmpty() && _tracks.value.isEmpty()) {
                _tracks.value = applyOverridesToList(cached, _overrides.value)
                _hasScanned.value = true
                applyFilter()
            }

            // 2) Cooldown kontrolü — force değilse ve son taramadan çok geçmediyse atla.
            val lastScan = prefs.lastLibraryScanMs.first()
            if (!force && lastScan > 0 && System.currentTimeMillis() - lastScan < scanCooldownMs) {
                _isLoading.value = false
                return@launch
            }

            // 3) MediaStore'u tara + önbelleği güncelle.
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                MediaStoreScanner.queryAudioTracks(getApplication())
            }
            _tracks.value = applyOverridesToList(result, _overrides.value)
            runCatching { prefs.setLibraryCacheJson(result.toCacheJson()) }
            runCatching { prefs.setLastLibraryScanMs(System.currentTimeMillis()) }
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
            // Re-apply overrides to current raw track list (önbellekten de okunabilir).
            val newOverrides = parseOverrides(prefs.trackOverridesJson.first())
            _tracks.value = applyOverridesToList(_tracks.value, newOverrides)
            applyFilter()
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
