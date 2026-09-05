package com.jameeli.thykra.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadRequest
import com.jameeli.thykra.chapters.Chapter
import com.jameeli.thykra.chapters.groupIntoChapters
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.UpdateAlbumRequest
import com.jameeli.thykra.ui.media.PlatformMediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Days or Sheet. Remembered per trip. */
enum class TripLayout { Days, Sheet }

/**
 * Design part 3 §06. The screen the app is for.
 *
 * Media is grouped once per load rather than on every recomposition — a 300-photo trip
 * regroups in the view model and the grid just reads the result.
 */
class TripViewModel(
    private val albumApi: AlbumApi,
    private val mediaApi: MediaApi,
    private val profileApi: ProfileApi,
    private val uploadQueueManager: UploadQueueManager,
    private val networkMonitor: NetworkMonitor? = null,
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumDto?>(null)
    val album: StateFlow<AlbumDto?> = _album.asStateFlow()

    private val _members = MutableStateFlow<List<AlbumMemberDto>>(emptyList())
    val members: StateFlow<List<AlbumMemberDto>> = _members.asStateFlow()

    private val _media = MutableStateFlow<List<MediaDto>>(emptyList())
    val media: StateFlow<List<MediaDto>> = _media.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter<MediaDto>>>(emptyList())
    val chapters: StateFlow<List<Chapter<MediaDto>>> = _chapters.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _layout = MutableStateFlow(TripLayout.Days)
    val layout: StateFlow<TripLayout> = _layout.asStateFlow()

    /** Non-empty means selection mode. Entered by long-pressing a plate. */
    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    private val _shareLink = MutableStateFlow<String?>(null)
    val shareLink: StateFlow<String?> = _shareLink.asStateFlow()

    val connected: StateFlow<Boolean> =
        networkMonitor?.isConnected ?: MutableStateFlow(true).asStateFlow()

    val uploads = uploadQueueManager.uploads

    /** The signed-in person's role in this trip, which decides the action bar. */
    val role: MemberRole?
        get() {
            val me = _currentUserId.value ?: return null
            _members.value.firstOrNull { it.userId == me }?.let { return it.role }
            return if (_album.value?.ownerId == me) MemberRole.OWNER else null
        }

    fun load(albumId: String, refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) _refreshing.value = true else _loading.value = true
            try {
                if (_currentUserId.value == null) {
                    val profile = profileApi.getProfile()
                    if (profile.success) _currentUserId.value = profile.data?.id
                }

                val albumResponse = albumApi.getAlbum(albumId)
                if (albumResponse.success && albumResponse.data != null) {
                    _album.value = albumResponse.data
                    _error.value = null
                } else if (_album.value == null) {
                    _error.value = albumResponse.error ?: "We couldn't load this."
                }

                val membersResponse = albumApi.getMembers(albumId)
                if (membersResponse.success && membersResponse.data != null) {
                    _members.value = membersResponse.data!!
                }

                val mediaResponse = mediaApi.getAlbumMedia(albumId)
                if (mediaResponse.success && mediaResponse.data != null) {
                    setMedia(mediaResponse.data!!)
                }
            } catch (e: Exception) {
                if (_album.value == null) _error.value = e.message ?: "We couldn't load this."
            } finally {
                _loading.value = false
                _refreshing.value = false
                _loaded.value = true
            }
        }
    }

    private fun setMedia(items: List<MediaDto>) {
        _media.value = items
        // Grouped here, once, rather than in the grid on every frame.
        _chapters.value = groupIntoChapters(items)
    }

    fun setLayout(layout: TripLayout) {
        _layout.value = layout
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun toggleSelection(mediaId: String) {
        val current = _selection.value
        _selection.value = if (mediaId in current) current - mediaId else current + mediaId
    }

    fun startSelection(mediaId: String) {
        _selection.value = setOf(mediaId)
    }

    fun selectAll() {
        _selection.value = _media.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    /** Remove is allowed on your own photographs, or on anything if you own the trip. */
    fun canRemoveSelection(): Boolean {
        val me = _currentUserId.value ?: return false
        if (role == MemberRole.OWNER) return true
        val selected = _media.value.filter { it.id in _selection.value }
        return selected.isNotEmpty() && selected.all { it.uploaderId == me }
    }

    fun removeSelected(albumId: String, onDone: (Int) -> Unit) {
        val ids = _selection.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            var removed = 0
            ids.forEach { id ->
                try {
                    if (mediaApi.deleteMedia(albumId, id).success) removed++
                } catch (_: Exception) {
                }
            }
            setMedia(_media.value.filterNot { it.id in ids })
            clearSelection()
            onDone(removed)
        }
    }

    // ── Sharing ───────────────────────────────────────────────────────────────

    /**
     * A share link is an invite link. Creating one is also what turns a private trip into
     * a shared one, so the two happen together rather than leaving a link that 404s.
     */
    fun ensureShareLink(albumId: String, onReady: (String) -> Unit) {
        val existing = _shareLink.value
        if (existing != null) {
            onReady(existing)
            return
        }
        viewModelScope.launch {
            try {
                val response = albumApi.createInviteLink(albumId, expiresInDays = 7)
                val data = response.data
                if (response.success && data != null) {
                    val url = "https://thykra.com/invite/${data.token}"
                    _shareLink.value = url
                    onReady(url)
                } else {
                    _error.value = response.error ?: "Couldn't create a link."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't create a link."
            }
        }
    }

    fun setVisibility(albumId: String, visibility: AlbumVisibility) {
        viewModelScope.launch {
            try {
                val response = albumApi.updateAlbum(albumId, UpdateAlbumRequest(visibility = visibility))
                if (response.success && response.data != null) _album.value = response.data
            } catch (_: Exception) {
            }
        }
    }

    // ── Uploads ───────────────────────────────────────────────────────────────

    fun uploadFiles(albumId: String, files: List<PlatformMediaFile>) {
        viewModelScope.launch {
            files.forEach { file ->
                uploadQueueManager.enqueueWithPersistence(
                    UploadRequest(
                        albumId = albumId,
                        filename = file.name,
                        contentType = file.contentType,
                        fileSize = file.size,
                        readBytes = file.readBytes,
                        width = file.width,
                        height = file.height,
                    ),
                )
            }
        }
    }

    fun refreshMedia(albumId: String) {
        viewModelScope.launch {
            try {
                val response = mediaApi.getAlbumMedia(albumId)
                if (response.success && response.data != null) setMedia(response.data!!)
            } catch (_: Exception) {
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
