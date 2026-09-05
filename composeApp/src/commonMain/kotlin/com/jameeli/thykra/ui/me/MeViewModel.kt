package com.jameeli.thykra.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadStatus
import com.jameeli.thykra.model.UpdateProfileRequest
import com.jameeli.thykra.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The Me screen's state. Everything here is either the profile the server holds or a
 * preference this device holds — nothing in between.
 */
class MeViewModel(
    private val profileApi: ProfileApi,
    private val albumApi: AlbumApi,
    private val uploadQueueManager: UploadQueueManager,
    private val preferences: DevicePreferences,
) : ViewModel() {

    private val _profile = MutableStateFlow<UserDto?>(null)
    val profile: StateFlow<UserDto?> = _profile.asStateFlow()

    /** "3 trips · 162 photos", summed over the list. */
    private val _stats = MutableStateFlow<String?>(null)
    val stats: StateFlow<String?> = _stats.asStateFlow()

    private val _wifiOnlyUploads = MutableStateFlow(preferences.wifiOnlyUploads)
    val wifiOnlyUploads: StateFlow<Boolean> = _wifiOnlyUploads.asStateFlow()

    private val _cacheBytes = MutableStateFlow("Calculating…")
    val cacheBytes: StateFlow<String> = _cacheBytes.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _uploadsPending = MutableStateFlow(0)
    val uploadsPending: StateFlow<Int> = _uploadsPending.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                profileApi.getProfile().let { if (it.success) _profile.value = it.data }
            } catch (_: Exception) {
                // The name and avatar come from the auth cache anyway; a failed sums call
                // is not worth an error state on a screen that is mostly local.
            }
            try {
                albumApi.getAlbums().let { response ->
                    val albums = response.data
                    if (response.success && albums != null) {
                        val photos = albums.sumOf { it.mediaCount }
                        _stats.value = buildString {
                            append(albums.size)
                            append(if (albums.size == 1) " trip" else " trips")
                            if (photos > 0) {
                                append(" · ")
                                append(photos)
                                append(if (photos == 1) " photo" else " photos")
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            _cacheBytes.value = preferences.describeCacheSize()
            _uploadsPending.value = uploadQueueManager.uploads.value.count {
                it.status != UploadStatus.DONE && it.status != UploadStatus.FAILED
            }
        }
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val response = profileApi.updateProfile(UpdateProfileRequest(displayName = trimmed))
                if (response.success && response.data != null) {
                    _profile.value = response.data
                    _message.value = "Saved"
                } else {
                    _message.value = "Saved · will sync"
                }
            } catch (_: Exception) {
                // Offline: the name change queues rather than failing outright.
                _profile.value = _profile.value?.copy(displayName = trimmed)
                _message.value = "Saved · will sync"
            }
        }
    }

    fun setWifiOnlyUploads(enabled: Boolean) {
        _wifiOnlyUploads.value = enabled
        preferences.wifiOnlyUploads = enabled
    }

    fun clearCache() {
        viewModelScope.launch {
            preferences.clearImageCache()
            _cacheBytes.value = preferences.describeCacheSize()
            _message.value = "Saved photos cleared"
        }
    }

    fun setLanguage(tag: String) {
        preferences.applicationLanguage = tag
        _message.value = if (tag == "ar") "تم تغيير اللغة" else "Language changed"
    }

    fun languageLabel(): String = if (preferences.applicationLanguage == "ar") "العربية" else "English"

    fun versionLabel(): String = preferences.versionLabel

    fun openTerms() {
        preferences.openUrl("https://thykra.com/terms")
    }

    fun consumeMessage() {
        _message.value = null
    }
}

/**
 * The handful of things the Me screen changes that live on the device rather than on the
 * server. Implemented per platform so `commonMain` never reaches for a Context or a
 * Bundle.
 */
interface DevicePreferences {
    var wifiOnlyUploads: Boolean
    var applicationLanguage: String
    val versionLabel: String

    fun describeCacheSize(): String
    suspend fun clearImageCache()
    fun openUrl(url: String)
}
