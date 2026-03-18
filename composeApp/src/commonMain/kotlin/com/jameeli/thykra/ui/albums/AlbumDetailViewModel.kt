package com.jameeli.thykra.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadRequest
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.ui.media.PlatformMediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val albumApi: AlbumApi,
    private val mediaApi: MediaApi,
    private val uploadQueueManager: UploadQueueManager
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumDto?>(null)
    val album: StateFlow<AlbumDto?> = _album.asStateFlow()

    private val _members = MutableStateFlow<List<AlbumMemberDto>>(emptyList())
    val members: StateFlow<List<AlbumMemberDto>> = _members.asStateFlow()

    private val _inviteLink = MutableStateFlow<InviteLinkDto?>(null)
    val inviteLink: StateFlow<InviteLinkDto?> = _inviteLink.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _media = MutableStateFlow<List<MediaDto>>(emptyList())
    val media: StateFlow<List<MediaDto>> = _media.asStateFlow()

    val uploads = uploadQueueManager.uploads

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
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
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createInviteLink(albumId: String) {
        viewModelScope.launch {
            try {
                val response = albumApi.createInviteLink(albumId)
                val data = response.data
                if (response.success && data != null) {
                    _inviteLink.value = data
                }
            } catch (_: Exception) {
            }
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
