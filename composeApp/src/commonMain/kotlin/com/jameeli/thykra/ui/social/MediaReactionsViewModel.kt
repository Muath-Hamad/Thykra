package com.jameeli.thykra.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.model.MediaReactionsDto
import com.jameeli.thykra.model.ReactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaReactionsViewModel(
    private val reactionApi: ReactionApi
) : ViewModel() {

    private val _reactions = MutableStateFlow<MediaReactionsDto?>(null)
    val reactions: StateFlow<MediaReactionsDto?> = _reactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var loadJob: Job? = null

    fun load(albumId: String, mediaId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = reactionApi.list(albumId, mediaId)
                if (response.success && response.data != null) {
                    _reactions.value = response.data!!
                    _error.value = null
                } else {
                    _error.value = response.error ?: "Failed to load reactions"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggle(albumId: String, mediaId: String, type: ReactionType) {
        viewModelScope.launch {
            try {
                val response = reactionApi.toggle(albumId, mediaId, type)
                if (response.success && response.data != null) {
                    _reactions.value = response.data!!
                    _error.value = null
                } else {
                    _error.value = response.error ?: "Failed to toggle reaction"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
