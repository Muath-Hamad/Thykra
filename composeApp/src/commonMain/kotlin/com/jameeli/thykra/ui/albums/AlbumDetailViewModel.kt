package com.jameeli.thykra.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadRequest
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.BlockedMemberDto
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.UpdateAlbumRequest
import com.jameeli.thykra.ui.media.PlatformMediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val albumApi: AlbumApi,
    private val mediaApi: MediaApi,
    private val uploadQueueManager: UploadQueueManager,
    private val profileApi: ProfileApi
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumDto?>(null)
    val album: StateFlow<AlbumDto?> = _album.asStateFlow()

    private val _members = MutableStateFlow<List<AlbumMemberDto>>(emptyList())
    val members: StateFlow<List<AlbumMemberDto>> = _members.asStateFlow()

    private val _blockedMembers = MutableStateFlow<List<BlockedMemberDto>>(emptyList())
    val blockedMembers: StateFlow<List<BlockedMemberDto>> = _blockedMembers.asStateFlow()

    private val _inviteLink = MutableStateFlow<InviteLinkDto?>(null)
    val inviteLink: StateFlow<InviteLinkDto?> = _inviteLink.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _media = MutableStateFlow<List<MediaDto>>(emptyList())
    val media: StateFlow<List<MediaDto>> = _media.asStateFlow()

    /**
     * Authenticated user's id, used to detect ownership for owner-only UI affordances
     * (visibility toggle, block / remove, blocked-list management).
     */
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val uploads = uploadQueueManager.uploads

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (_currentUserId.value == null) {
                    val profileResponse = profileApi.getProfile()
                    val profileData = profileResponse.data
                    if (profileResponse.success && profileData != null) {
                        _currentUserId.value = profileData.id
                    }
                }
                val albumResponse = albumApi.getAlbum(albumId)
                val albumData = albumResponse.data
                if (albumResponse.success && albumData != null) {
                    _album.value = albumData
                }
                val membersResponse = albumApi.getMembers(albumId)
                val membersData = membersResponse.data
                if (membersResponse.success && membersData != null) {
                    _members.value = membersData
                }
                val mediaResponse = mediaApi.getAlbumMedia(albumId)
                val mediaData = mediaResponse.data
                if (mediaResponse.success && mediaData != null) {
                    _media.value = mediaData
                }
                if (_album.value?.ownerId == _currentUserId.value && _currentUserId.value != null) {
                    refreshBlocked(albumId)
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createInviteLink(albumId: String, expiresInDays: Int? = null) {
        viewModelScope.launch {
            try {
                val response = albumApi.createInviteLink(albumId, expiresInDays)
                val data = response.data
                if (response.success && data != null) {
                    _inviteLink.value = data
                }
            } catch (_: Exception) {
            }
        }
    }

    fun setVisibility(albumId: String, visibility: AlbumVisibility) {
        viewModelScope.launch {
            try {
                val response = albumApi.updateAlbum(
                    albumId,
                    UpdateAlbumRequest(visibility = visibility)
                )
                val data = response.data
                if (response.success && data != null) {
                    _album.value = data
                }
            } catch (_: Exception) {
            }
        }
    }

    fun removeMember(albumId: String, userId: String) {
        viewModelScope.launch {
            try {
                val response = albumApi.removeMember(albumId, userId)
                if (response.success) {
                    _members.value = _members.value.filterNot { it.userId == userId }
                    _statusMessage.value = "Member removed"
                }
            } catch (_: Exception) {
            }
        }
    }

    fun blockMember(albumId: String, userId: String) {
        viewModelScope.launch {
            try {
                val response = albumApi.blockMember(albumId, userId)
                if (response.success) {
                    _members.value = _members.value.filterNot { it.userId == userId }
                    refreshBlocked(albumId)
                    _statusMessage.value = "Member blocked"
                }
            } catch (_: Exception) {
            }
        }
    }

    fun unblockMember(albumId: String, userId: String) {
        viewModelScope.launch {
            try {
                val response = albumApi.unblockMember(albumId, userId)
                if (response.success) {
                    _blockedMembers.value = _blockedMembers.value.filterNot { it.userId == userId }
                    _statusMessage.value = "Member unblocked"
                }
            } catch (_: Exception) {
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    private suspend fun refreshBlocked(albumId: String) {
        try {
            val response = albumApi.listBlocked(albumId)
            val data = response.data
            if (response.success && data != null) {
                _blockedMembers.value = data
            }
        } catch (_: Exception) {
        }
    }

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
                        height = file.height
                    )
                )
            }
        }
    }

    fun refreshMedia(albumId: String) {
        viewModelScope.launch {
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
