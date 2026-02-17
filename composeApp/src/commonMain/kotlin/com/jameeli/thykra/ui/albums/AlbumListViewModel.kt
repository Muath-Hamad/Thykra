package com.jameeli.thykra.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.CreateAlbumRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumListViewModel(private val albumApi: AlbumApi) : ViewModel() {

    private val _albums = MutableStateFlow<List<AlbumDto>>(emptyList())
    val albums: StateFlow<List<AlbumDto>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = albumApi.getAlbums()
                if (response.success && response.data != null) {
                    _albums.value = response.data
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAlbum(title: String, description: String?, onCreated: (AlbumDto) -> Unit) {
        viewModelScope.launch {
            try {
                val response = albumApi.createAlbum(CreateAlbumRequest(title, description))
                if (response.success && response.data != null) {
                    _albums.value = _albums.value + response.data
                    onCreated(response.data)
                }
            } catch (_: Exception) {
            }
        }
    }
}
