package com.jameeli.thykra.auth

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorization
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSError
import platform.darwin.NSObject

@Composable
actual fun PlatformAppleSignInButton(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    Button(onClick = {
        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeEmail, ASAuthorizationScopeFullName)
        }
        val controller = ASAuthorizationController(listOf(request))
        val delegate = AppleSignInDelegate(onIdToken, onError)
        controller.delegate = delegate
        controller.performRequests()
    }) {
        Text("Sign in with Apple")
    }
}

@OptIn(ExperimentalForeignApi::class)
private class AppleSignInDelegate(
    private val onIdToken: (String) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), ASAuthorizationControllerDelegateProtocol {

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        val credential = didCompleteWithAuthorization.credential
        if (credential is ASAuthorizationAppleIDCredential) {
            val tokenData = credential.identityToken
            if (tokenData != null) {
                val idToken = tokenData.bytes
                    ?.readBytes(tokenData.length.toInt())
                    ?.decodeToString()
                if (idToken != null) {
                    onIdToken(idToken)
                    return
                }
            }
        }
        onError("Failed to get Apple ID token")
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        onError(didCompleteWithError.localizedDescription)
    }
}
