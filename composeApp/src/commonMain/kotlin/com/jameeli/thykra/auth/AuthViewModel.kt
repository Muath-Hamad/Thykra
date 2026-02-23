package com.jameeli.thykra.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.TokenProvider
import com.jameeli.thykra.model.OAuthProvider
import com.jameeli.thykra.model.OAuthRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authApi: AuthApi,
    private val tokenProvider: TokenProvider
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = tokenProvider.getAccessToken()
            _authState.value = if (token != null) {
                AuthState.Authenticated(userId = "")
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                val response = authApi.loginWithOAuth(
                    OAuthRequest(provider = OAuthProvider.GOOGLE, idToken = idToken)
                )
                val data = response.data
                if (response.success && data != null) {
                    tokenProvider.saveTokens(data.accessToken, data.refreshToken)
                    _authState.value = AuthState.Authenticated(userId = data.user.id)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun loginWithApple(idToken: String) {
        viewModelScope.launch {
            try {
                val response = authApi.loginWithOAuth(
                    OAuthRequest(provider = OAuthProvider.APPLE, idToken = idToken)
                )
                val data = response.data
                if (response.success && data != null) {
                    tokenProvider.saveTokens(data.accessToken, data.refreshToken)
                    _authState.value = AuthState.Authenticated(userId = data.user.id)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authApi.logout()
            } catch (_: Exception) {
            }
            tokenProvider.clearTokens()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
