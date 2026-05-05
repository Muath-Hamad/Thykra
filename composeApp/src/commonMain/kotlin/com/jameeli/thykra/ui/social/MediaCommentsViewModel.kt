package com.jameeli.thykra.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.model.CommentDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaCommentsViewModel(
    private val commentApi: CommentApi
) : ViewModel() {

    private val _comments = MutableStateFlow<List<CommentDto>>(emptyList())
    val comments: StateFlow<List<CommentDto>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var loadJob: Job? = null

    fun load(albumId: String, mediaId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = commentApi.list(albumId, mediaId)
                if (response.success && response.data != null) {
                    _comments.value = response.data!!
                    _error.value = null
                } else {
                    _error.value = response.error ?: "Failed to load comments"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun add(albumId: String, mediaId: String, body: String, onSuccess: (() -> Unit)? = null) {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed.length > 2000) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val response = commentApi.add(albumId, mediaId, trimmed)
                if (response.success && response.data != null) {
                    _comments.value = _comments.value + response.data!!
                    _error.value = null
                    onSuccess?.invoke()
                } else {
                    _error.value = response.error ?: "Failed to add comment"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isSending.value = false
            }
        }
    }

    fun update(albumId: String, mediaId: String, commentId: String, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed.length > 2000) return
        viewModelScope.launch {
            try {
                val response = commentApi.update(albumId, mediaId, commentId, trimmed)
                if (response.success && response.data != null) {
                    val updated = response.data!!
                    _comments.value = _comments.value.map { if (it.id == commentId) updated else it }
                    _error.value = null
                } else {
                    _error.value = response.error ?: "Failed to update comment"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun delete(albumId: String, mediaId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val response = commentApi.delete(albumId, mediaId, commentId)
                if (response.success) {
                    _comments.value = _comments.value.filterNot { it.id == commentId }
                    _error.value = null
                } else {
                    _error.value = response.error ?: "Failed to delete comment"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
