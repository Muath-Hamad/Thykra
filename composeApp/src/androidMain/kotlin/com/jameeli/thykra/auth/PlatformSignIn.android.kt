package com.jameeli.thykra.auth

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jameeli.thykra.BuildConfig
import com.jameeli.thykra.ui.theme.ThykraIcons
import kotlinx.coroutines.launch

/**
 * Sign in with Google, through Credential Manager.
 *
 * Uses [GetSignInWithGoogleOption] rather than `GetGoogleIdOption`. The two look
 * interchangeable and are not:
 *
 * - `GetGoogleIdOption` is the *automatic* flow — the one-tap sheet that appears without
 *   being asked. Android applies a cooldown to it: dismiss the sheet a couple of times
 *   and every later request fails with `NoCredentialException` for around 24 hours, even
 *   with accounts on the device and `filterByAuthorizedAccounts` off.
 * - [GetSignInWithGoogleOption] is the *explicit* flow behind a button someone actually
 *   pressed. It always shows the account picker and has no cooldown.
 *
 * This is a button, so it wants the second one.
 */
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
                if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) {
                    // A build with no client id cannot possibly succeed, and the
                    // Credential Manager error for it is unreadable.
                    onError("This build has no Google client id. Set GOOGLE_CLIENT_ID in local.properties.")
                    return@launch
                }
                try {
                    val credentialManager = CredentialManager.create(context)
                    val signInWithGoogle = GetSignInWithGoogleOption
                        .Builder(BuildConfig.GOOGLE_CLIENT_ID)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogle)
                        .build()
                    val result = credentialManager.getCredential(context, request)
                    val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    onIdToken(googleCredential.idToken)
                } catch (e: GetCredentialCancellationException) {
                    // Backing out of the sheet is a decision, not a failure.
                } catch (e: NoCredentialException) {
                    // With the explicit flow and an account on the device, this is a
                    // configuration answer rather than a user one: Google could not match
                    // this build to an OAuth client. Almost always the app's package name
                    // and signing SHA-1 are not registered as an Android OAuth client in
                    // the same Cloud project as the Web client id above.
                    Log.w("GoogleSignIn", "No credential returned for the explicit flow", e)
                    onError(
                        "Google didn't recognise this build. Check that the app's package " +
                            "and signing SHA-1 are registered in the same Google Cloud project " +
                            "as the client id.",
                    )
                } catch (e: GetCredentialException) {
                    Log.e("GoogleSignIn", "Credential Manager failed", e)
                    onError("Couldn't sign in with Google. Try again in a moment.")
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Sign-in failed", e)
                    onError("Couldn't sign in. Try again in a moment.")
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
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
        Text("Continue with Google")
    }
}
