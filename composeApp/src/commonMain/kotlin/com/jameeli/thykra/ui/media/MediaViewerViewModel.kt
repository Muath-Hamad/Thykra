package com.jameeli.thykra.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.MediaDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewerViewModel(
    private val mediaApi: MediaApi,
    private val albumApi: AlbumApi? = null,
    private val profileApi: ProfileApi? = null
) : ViewModel() {

    private val _media = MutableStateFlow<List<MediaDto>>(emptyList())
    val media: StateFlow<List<MediaDto>> = _media.asStateFlow()

    private val _album = MutableStateFlow<AlbumDto?>(null)
    val album: StateFlow<AlbumDto?> = _album.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private var loadJob: Job? = null
    private var albumJob: Job? = null
    private var profileJob: Job? = null

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
        if (albumApi != null) {
            albumJob?.cancel()
            albumJob = viewModelScope.launch {
                try {
                    val response = albumApi.getAlbum(albumId)
                    val data = response.data
                    if (response.success && data != null) {
                        _album.value = data
                    }
                } catch (_: Exception) {
                }
            }
        }
        if (profileApi != null && _currentUserId.value == null) {
            profileJob?.cancel()
            profileJob = viewModelScope.launch {
                try {
                    val response = profileApi.getProfile()
                    val data = response.data
                    if (response.success && data != null) {
                        _currentUserId.value = data.id
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
