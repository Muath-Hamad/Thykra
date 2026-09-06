package com.jameeli.thykra.auth

import com.jameeli.thykra.BuildConfig
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jameeli.thykra.ui.theme.ThykraIcons
import kotlinx.coroutines.launch

@Composable
actual fun PlatformGoogleSignInButton(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val result = credentialManager.getCredential(context, request)
                    val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    onIdToken(googleCredential.idToken)
                } catch (e: GetCredentialCancellationException) {
                    // Backing out of the sheet is a decision, not a failure.
                } catch (e: NoCredentialException) {
                    // Reached with filterByAuthorizedAccounts = false, so it is not
                    // "you have not used this app before" — the phone has no Google
                    // account at all, or no Play services to ask.
                    Log.w("GoogleSignIn", "No credentials available", e)
                    onError(
                        "No Google account on this phone. Add one in Settings and try again.",
                    )
                } catch (e: GetCredentialException) {
                    Log.e("GoogleSignIn", "Sign-in failed", e)
                    onError("Couldn't sign in with Google. Try again in a moment.")
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Sign-in failed", e)
                    onError("Couldn't sign in. Try again in a moment.")
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = ThykraIcons.Person,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign in with Google")
    }
}
