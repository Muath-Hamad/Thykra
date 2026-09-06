package com.jameeli.thykra.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.ActivityFeedApi
import com.jameeli.thykra.model.ActivityEventDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant

/**
 * Design part 3 §10.
 *
 * The seen marker is posted when the tab is *left*, not when it is opened — a glance at
 * the top of the list should not mark the bottom of it seen. That is also what clears the
 * nav-bar dot.
 */
class ActivityViewModel(
    private val api: ActivityFeedApi,
    /** null for the global feed; set for a trip's own. */
    private val albumId: String? = null,
) : ViewModel() {

    private val _events = MutableStateFlow<List<ActivityEventDto>>(emptyList())
    val events: StateFlow<List<ActivityEventDto>> = _events.asStateFlow()

    private val _lastSeenAt = MutableStateFlow<Instant?>(null)
    val lastSeenAt: StateFlow<Instant?> = _lastSeenAt.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _endOfList = MutableStateFlow(false)
    val endOfList: StateFlow<Boolean> = _endOfList.asStateFlow()

    private var cursor: String? = null

    /** True when anything in the list is newer than the marker. Drives the nav-bar dot. */
    val hasUnseen: Boolean
        get() {
            val seen = _lastSeenAt.value ?: return _events.value.isNotEmpty()
            return _events.value.any { it.createdAt > seen }
        }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) _refreshing.value = true else _loading.value = true
            try {
                val response = if (albumId == null) api.feed() else api.feedForAlbum(albumId)
                val data = response.data
                if (response.success && data != null) {
                    _events.value = data.items
                    _lastSeenAt.value = data.lastSeenAt
                    cursor = data.nextCursor
                    _endOfList.value = data.nextCursor == null
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

    fun loadMore() {
        val next = cursor ?: return
        viewModelScope.launch {
            try {
                val response = if (albumId == null) {
                    api.feed(cursor = next)
                } else {
                    api.feedForAlbum(albumId, cursor = next)
                }
                val data = response.data
                if (response.success && data != null) {
                    _events.value = _events.value + data.items
                    cursor = data.nextCursor
                    _endOfList.value = data.nextCursor == null
                }
            } catch (_: Exception) {
                // Paging silently stops; the list already has something to read.
            }
        }
    }

    /** Called on leaving the tab. */
    fun markSeen() {
        viewModelScope.launch {
            try {
                val response = api.markSeen()
                if (response.success) _lastSeenAt.value = response.data?.seenAt
            } catch (_: Exception) {
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
