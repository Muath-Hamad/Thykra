package com.jameeli.thykra.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.InviteApi
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.InvitePreviewDto
import com.jameeli.thykra.model.InviteStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The nine states of design part 3 §05, driven by one endpoint.
 *
 * [Error] is the only one the server does not name: every other outcome comes back as an
 * HTTP 200 whose `status` is the answer, which is what lets a revoked token render a
 * designed dead end instead of a failure.
 */
sealed interface InviteUiState {
    data object Loading : InviteUiState
    data class Ready(val preview: InvitePreviewDto) : InviteUiState
    data object Joining : InviteUiState
    data class Joined(val album: AlbumDto) : InviteUiState
    data class Error(val message: String, val offline: Boolean) : InviteUiState
}

class InviteViewModel(private val inviteApi: InviteApi) : ViewModel() {

    private val _state = MutableStateFlow<InviteUiState>(InviteUiState.Loading)
    val state: StateFlow<InviteUiState> = _state.asStateFlow()

    /** Kept so a failed refresh can leave the cover and title on screen. */
    private var lastPreview: InvitePreviewDto? = null

    fun load(token: String) {
        viewModelScope.launch {
            _state.value = lastPreview?.let { InviteUiState.Ready(it) } ?: InviteUiState.Loading
            try {
                val response = inviteApi.getPreview(token)
                val data = response.data
                if (response.success && data != null) {
                    lastPreview = data
                    _state.value = InviteUiState.Ready(data)
                } else {
                    _state.value = InviteUiState.Error(
                        message = response.error ?: "Couldn't check this link.",
                        offline = false,
                    )
                }
            } catch (e: Exception) {
                // The cover and title stay if the preview had loaded once.
                _state.value = lastPreview?.let { InviteUiState.Ready(it) }
                    ?: InviteUiState.Error("Couldn't check this link. You're offline.", offline = true)
            }
        }
    }

    fun join(token: String, onJoined: (AlbumDto) -> Unit) {
        viewModelScope.launch {
            _state.value = InviteUiState.Joining
            try {
                val response = inviteApi.join(token)
                val data = response.data
                if (response.success && data != null) {
                    _state.value = InviteUiState.Joined(data)
                    onJoined(data)
                } else {
                    // Back to where they were, with the reason.
                    _state.value = lastPreview?.let { InviteUiState.Ready(it) }
                        ?: InviteUiState.Error(response.error ?: "Couldn't join.", offline = false)
                }
            } catch (e: Exception) {
                _state.value = lastPreview?.let { InviteUiState.Ready(it) }
                    ?: InviteUiState.Error(e.message ?: "Couldn't join.", offline = true)
            }
        }
    }

    /** True for the statuses that show a trip at all. */
    fun showsTrip(status: InviteStatus): Boolean = when (status) {
        InviteStatus.VALID, InviteStatus.EXPIRED, InviteStatus.ALREADY_MEMBER -> true
        // BLOCKED and an unknown token are byte-identical on purpose: a blocked person is
        // shown no trip name and no cover, and neither is someone with a made-up token.
        InviteStatus.REVOKED, InviteStatus.BLOCKED -> false
    }
}
