package com.bishal.adaptivecamera.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.gallery.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = false,
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedItem: MediaItem? = null,
    val isDeleting: Boolean = false,
    val message: String? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val items = repository.getRecentMedia()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    mediaItems = items
                )
            }
        }
    }

    fun selectItem(item: MediaItem?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch {
            val success = repository.deleteMedia(item)
            if (success) {
                _uiState.update { current ->
                    val updated = current.mediaItems.filter { it.id != item.id }
                    current.copy(
                        mediaItems = updated,
                        selectedItem = if (current.selectedItem?.id == item.id) null else current.selectedItem,
                        message = "Media deleted successfully"
                    )
                }
            } else {
                _uiState.update { it.copy(message = "Could not delete media") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
