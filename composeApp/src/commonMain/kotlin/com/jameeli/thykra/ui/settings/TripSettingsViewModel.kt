package com.jameeli.thykra.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.BlockedMemberDto
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.UpdateAlbumRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Design part 3 §09.
 *
 * One view model behind two screens. Which screen you get is decided by role, not by
 * hiding controls: an owner and a member want different things from this page, and
 * greying half of it out would only tell a member what they cannot have.
 */
class TripSettingsViewModel(
    private val albumApi: AlbumApi,
    private val profileApi: ProfileApi,
    private val networkMonitor: NetworkMonitor? = null,
) : ViewModel() {

    private val _album = MutableStateFlow<AlbumDto?>(null)
    val album: StateFlow<AlbumDto?> = _album.asStateFlow()

    private val _members = MutableStateFlow<List<AlbumMemberDto>>(emptyList())
    val members: StateFlow<List<AlbumMemberDto>> = _members.asStateFlow()

    private val _blocked = MutableStateFlow<List<BlockedMemberDto>>(emptyList())
    val blocked: StateFlow<List<BlockedMemberDto>> = _blocked.asStateFlow()

    private val _invites = MutableStateFlow<List<InviteLinkDto>>(emptyList())
    val invites: StateFlow<List<InviteLinkDto>> = _invites.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val connected: StateFlow<Boolean> =
        networkMonitor?.isConnected ?: MutableStateFlow(true).asStateFlow()

    val role: MemberRole?
        get() {
            val me = _currentUserId.value ?: return null
            _members.value.firstOrNull { it.userId == me }?.let { return it.role }
            return if (_album.value?.ownerId == me) MemberRole.OWNER else null
        }

    val isOwner: Boolean get() = role == MemberRole.OWNER

    fun load(albumId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                if (_currentUserId.value == null) {
                    val profile = profileApi.getProfile()
                    if (profile.success) _currentUserId.value = profile.data?.id
                }
                albumApi.getAlbum(albumId).let { if (it.success) _album.value = it.data }
                albumApi.getMembers(albumId).let {
                    if (it.success && it.data != null) _members.value = it.data!!
                }
                // Both are owner-only on the server; asking as a member would 403 and the
                // section is hidden for them anyway.
                if (isOwner) {
                    albumApi.listBlocked(albumId).let {
                        if (it.success && it.data != null) _blocked.value = it.data!!
                    }
                    albumApi.listInviteLinks(albumId).let {
                        if (it.success && it.data != null) _invites.value = it.data!!
                    }
                }
            } catch (_: Exception) {
            } finally {
                _loading.value = false
                _loaded.value = true
            }
        }
    }

    fun updateTrip(albumId: String, title: String, description: String?) {
        viewModelScope.launch {
            try {
                val response = albumApi.updateAlbum(
                    albumId,
                    UpdateAlbumRequest(title = title.trim(), description = description?.trim()),
                )
                if (response.success && response.data != null) {
                    _album.value = response.data
                } else {
                    _message.value = "Couldn't save that. You're offline · Retry"
                }
            } catch (_: Exception) {
                _message.value = "Couldn't save that. You're offline · Retry"
            }
        }
    }

    /** Optimistic: the switch moves now and reverts if the server disagrees. */
    fun setVisibility(albumId: String, visibility: AlbumVisibility) {
        val previous = _album.value?.visibility
        _album.value = _album.value?.copy(visibility = visibility)
        viewModelScope.launch {
            try {
                val response = albumApi.updateAlbum(albumId, UpdateAlbumRequest(visibility = visibility))
                if (response.success && response.data != null) {
                    _album.value = response.data
                } else {
                    previous?.let { _album.value = _album.value?.copy(visibility = it) }
                    _message.value = "Couldn't change this. You're offline"
                }
            } catch (_: Exception) {
                previous?.let { _album.value = _album.value?.copy(visibility = it) }
                _message.value = "Couldn't change this. You're offline"
            }
        }
    }

    fun removeMember(albumId: String, userId: String, name: String) {
        viewModelScope.launch {
            try {
                if (albumApi.removeMember(albumId, userId).success) {
                    _members.value = _members.value.filterNot { it.userId == userId }
                    _message.value = "$name removed"
                } else {
                    _message.value = "Couldn't remove $name."
                }
            } catch (_: Exception) {
                _message.value = "Couldn't remove $name. You're offline"
            }
        }
    }

    fun blockMember(albumId: String, userId: String, name: String) {
        viewModelScope.launch {
            try {
                if (albumApi.blockMember(albumId, userId).success) {
                    _members.value = _members.value.filterNot { it.userId == userId }
                    albumApi.listBlocked(albumId).let {
                        if (it.success && it.data != null) _blocked.value = it.data!!
                    }
                    _message.value = "$name blocked"
                }
            } catch (_: Exception) {
                _message.value = "Couldn't block $name. You're offline"
            }
        }
    }

    fun unblockMember(albumId: String, userId: String, name: String) {
        viewModelScope.launch {
            try {
                if (albumApi.unblockMember(albumId, userId).success) {
                    _blocked.value = _blocked.value.filterNot { it.userId == userId }
                    _message.value = "$name unblocked"
                }
            } catch (_: Exception) {
                _message.value = "Couldn't unblock $name. You're offline"
            }
        }
    }

    fun createInviteLink(albumId: String, expiresInDays: Int, onCreated: (InviteLinkDto) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = albumApi.createInviteLink(albumId, expiresInDays)
                val data = response.data
                if (response.success && data != null) {
                    _invites.value = listOf(data) + _invites.value
                    onCreated(data)
                }
            } catch (_: Exception) {
                _message.value = "Couldn't create a link. You're offline"
            }
        }
    }

    fun revokeInviteLink(albumId: String, token: String) {
        viewModelScope.launch {
            try {
                if (albumApi.revokeInviteLink(albumId, token).success) {
                    _invites.value = _invites.value.filterNot { it.token == token }
                    _message.value = "Link revoked"
                }
            } catch (_: Exception) {
                _message.value = "Couldn't revoke that link. You're offline"
            }
        }
    }

    fun deleteTrip(albumId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                if (albumApi.deleteAlbum(albumId).success) onDeleted()
                else _message.value = "Couldn't delete the trip."
            } catch (_: Exception) {
                _message.value = "Couldn't delete the trip. You're offline"
            }
        }
    }

    /** Leaving is removing yourself. Owners cannot; the row does not exist for them. */
    fun leaveTrip(albumId: String, onLeft: () -> Unit) {
        val me = _currentUserId.value ?: return
        viewModelScope.launch {
            try {
                if (albumApi.removeMember(albumId, me).success) onLeft()
                else _message.value = "Couldn't leave the trip."
            } catch (_: Exception) {
                _message.value = "Couldn't leave the trip. You're offline"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
