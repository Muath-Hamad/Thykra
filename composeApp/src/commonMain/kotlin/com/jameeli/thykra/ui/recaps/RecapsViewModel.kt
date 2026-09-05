package com.jameeli.thykra.ui.recaps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.RecapApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.RecapDto
import com.jameeli.thykra.model.RecapViewDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A trip is eligible for a recap at twelve photographs. Fewer is not a story. */
const val RecapMinimumPhotos = 12

class RecapsViewModel(
    private val recapApi: RecapApi,
    private val albumApi: AlbumApi,
) : ViewModel() {

    private val _recaps = MutableStateFlow<List<RecapDto>>(emptyList())
    val recaps: StateFlow<List<RecapDto>> = _recaps.asStateFlow()

    private val _trips = MutableStateFlow<List<AlbumDto>>(emptyList())
    val trips: StateFlow<List<AlbumDto>> = _trips.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Trips whose recap is being generated right now — the card shows a skeleton. */
    private val _building = MutableStateFlow<Set<String>>(emptySet())
    val building: StateFlow<Set<String>> = _building.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(albumId: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val recapResponse = if (albumId == null) {
                    recapApi.listForUser()
                } else {
                    recapApi.listForAlbum(albumId)
                }
                if (recapResponse.success && recapResponse.data != null) {
                    _recaps.value = recapResponse.data!!.sortedByDescending { it.createdAt }
                }
                if (albumId == null) {
                    albumApi.getAlbums().let {
                        if (it.success && it.data != null) _trips.value = it.data!!
                    }
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "We couldn't load this."
            } finally {
                _loading.value = false
                _loaded.value = true
            }
        }
    }

    fun build(albumId: String) {
        if (albumId in _building.value) return
        viewModelScope.launch {
            _building.value = _building.value + albumId
            try {
                val response = recapApi.build(albumId)
                val data = response.data
                if (response.success && data != null) {
                    _recaps.value = listOf(data) + _recaps.value.filterNot { it.id == data.id }
                } else {
                    _message.value = response.error ?: "Couldn't build that recap."
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Couldn't build that recap."
            } finally {
                _building.value = _building.value - albumId
            }
        }
    }

    /** Trips with no recap yet, so the list can offer Build or say how many more are needed. */
    fun tripsWithoutRecap(): List<AlbumDto> {
        val withRecap = _recaps.value.map { it.albumId }.toSet()
        return _trips.value.filterNot { it.id in withRecap }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

/** The reader is its own screen and its own fetch — the share link works without a session. */
class RecapReaderViewModel(private val recapApi: RecapApi) : ViewModel() {

    private val _view = MutableStateFlow<RecapViewDto?>(null)
    val view: StateFlow<RecapViewDto?> = _view.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(shareToken: String) {
        viewModelScope.launch {
            try {
                val response = recapApi.read(shareToken)
                if (response.success && response.data != null) {
                    _view.value = response.data
                    _error.value = null
                } else {
                    _error.value = response.error ?: "This recap isn't available."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "This recap isn't available."
            }
        }
    }
}
