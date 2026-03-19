package com.jameeli.thykra.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.model.MediaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MediaViewerViewModel(private val mediaApi: MediaApi) : ViewModel() {

    private val _media = MutableStateFlow<List<MediaDto>>(emptyList())
    val media: StateFlow<List<MediaDto>> = _media.asStateFlow()

    private var loadJob: Job? = null

    fun loadMedia(albumId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val response = mediaApi.getAlbumMedia(albumId)
                val data = response.data
                if (response.success && data != null) {
                    _media.value = data
                }
            } catch (_: Exception) {
            }
        }
    }
}
