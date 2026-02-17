package com.jameeli.thykra.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.InviteLinkDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumDetailViewModel(private val albumApi: AlbumApi) : ViewModel() {

    private val _album = MutableStateFlow<AlbumDto?>(null)
    val album: StateFlow<AlbumDto?> = _album.asStateFlow()

    private val _members = MutableStateFlow<List<AlbumMemberDto>>(emptyList())
    val members: StateFlow<List<AlbumMemberDto>> = _members.asStateFlow()

    private val _inviteLink = MutableStateFlow<InviteLinkDto?>(null)
    val inviteLink: StateFlow<InviteLinkDto?> = _inviteLink.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val albumResponse = albumApi.getAlbum(albumId)
                if (albumResponse.success && albumResponse.data != null) {
                    _album.value = albumResponse.data
                }
                val membersResponse = albumApi.getMembers(albumId)
                if (membersResponse.success && membersResponse.data != null) {
                    _members.value = membersResponse.data
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
                if (response.success && response.data != null) {
                    _inviteLink.value = response.data
                }
            } catch (_: Exception) {
            }
        }
    }
}
