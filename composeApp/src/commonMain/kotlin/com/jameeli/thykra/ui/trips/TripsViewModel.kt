package com.jameeli.thykra.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.CreateAlbumRequest
import com.jameeli.thykra.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Design part 3 §08.
 *
 * The list has one rule that shapes the whole state model: a failure with a cached list
 * is a Toast, and a failure with nothing cached is an empty state. So the last good list
 * is never thrown away on error — [trips] keeps it and [error] sits beside it.
 */
class TripsViewModel(
    private val albumApi: AlbumApi,
    private val profileApi: ProfileApi,
    private val networkMonitor: NetworkMonitor? = null,
) : ViewModel() {

    private val _trips = MutableStateFlow<List<AlbumDto>>(emptyList())
    val trips: StateFlow<List<AlbumDto>> = _trips.asStateFlow()

    private val _profile = MutableStateFlow<UserDto?>(null)
    val profile: StateFlow<UserDto?> = _profile.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Set on failure and cleared on the next success. Never clears [trips]. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** True once a load has completed, so "empty" and "not loaded yet" stay distinct. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    val connected: StateFlow<Boolean> =
        networkMonitor?.isConnected ?: MutableStateFlow(true).asStateFlow()

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) _refreshing.value = true else _loading.value = true
            try {
                val response = albumApi.getAlbums()
                val data = response.data
                if (response.success && data != null) {
                    _trips.value = data.sortedForList()
                    _error.value = null
                } else {
                    _error.value = response.error ?: "We couldn't load this."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "We couldn't load this."
            } finally {
                _loading.value = false
                _refreshing.value = false
                _loaded.value = true
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = profileApi.getProfile()
                if (response.success) _profile.value = response.data
            } catch (_: Exception) {
                // The greeting falls back to "Your trips"; a missing name is not an error
                // worth showing anyone.
            }
        }
    }

    /**
     * Validation happens on submit, not as you type — a counter that turns red while
     * someone is still typing the first letter is a scold, not a help.
     */
    fun createTrip(title: String, description: String?, onCreated: (AlbumDto) -> Unit) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _createError.value = "Give the trip a name"
            return
        }
        viewModelScope.launch {
            _creating.value = true
            _createError.value = null
            try {
                val response = albumApi.createAlbum(
                    CreateAlbumRequest(trimmed, description?.trim()?.ifBlank { null }),
                )
                val data = response.data
                if (response.success && data != null) {
                    _trips.value = (listOf(data) + _trips.value).sortedForList()
                    onCreated(data)
                } else {
                    _createError.value = response.error ?: "Couldn't create the trip."
                }
            } catch (e: Exception) {
                _createError.value = e.message ?: "Couldn't create the trip."
            } finally {
                _creating.value = false
            }
        }
    }

    fun clearCreateError() {
        _createError.value = null
    }

    fun clearError() {
        _error.value = null
    }

    /** Newest activity first, then newest trip — what moved most recently is on top. */
    private fun List<AlbumDto>.sortedForList(): List<AlbumDto> =
        sortedByDescending { it.lastActivityAt ?: it.createdAt }
}
