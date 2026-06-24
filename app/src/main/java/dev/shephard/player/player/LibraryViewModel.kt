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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

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

    private var contentObserver: ContentObserver? = null

    init {
        registerObserver()
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
            _tracks.value = result
            _isLoading.value = false
            _hasScanned.value = true
            applyFilter()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
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
}
