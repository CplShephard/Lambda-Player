package dev.shephard.player.player

import android.app.Application
import android.database.ContentObserver
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

    private var mediaObserver: ContentObserver? = null

    fun loadTracks() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                MediaStoreScanner.queryAudioTracks(getApplication())
            }
            _tracks.value = result
            _isLoading.value = false
            _hasScanned.value = true
        }
    }

    // Real-time Media Scanner integration
    fun startMediaObserver() {
        if (mediaObserver == null) {
            mediaObserver = MediaStoreScanner.observeMediaChanges(getApplication()) {
                // Reload tracks instantly when new audio file detected
                loadTracks()
            }
        }
    }

    fun stopMediaObserver() {
        mediaObserver?.let {
            MediaStoreScanner.unregisterObserver(getApplication(), it)
            mediaObserver = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMediaObserver()
    }
}
