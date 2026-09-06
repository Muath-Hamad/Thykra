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
 * Turns Credential Manager's failure into something a person can act on.
 *
 * Google reports a misconfigured project as an opaque bracketed code inside the exception
 * message rather than as a distinct exception type, so the message has to be read.
 */
private fun explain(type: String, message: String?): String {
    val detail = message.orEmpty()
    return when {
        detail.contains("28444") || detail.contains("Developer console is not set up") ->
            "Google doesn't recognise this build. Register the app's package and signing " +
                "SHA-1 as an Android OAuth client in the same Cloud project as the client id."

        detail.contains("10:") || detail.contains("DEVELOPER_ERROR") ->
            "The Google client id and this build don't match. Check the package name and " +
                "signing SHA-1 against the Cloud project."

        detail.contains("network", ignoreCase = true) ->
            "Couldn't reach Google. Check the connection and try again."

        else -> debugDetail("Couldn't sign in with Google.", "$type $detail")
    }
}

/** A debug build shows the raw cause; a release build never does. */
private fun debugDetail(headline: String, detail: String?): String =
    if (BuildConfig.DEBUG && !detail.isNullOrBlank()) "$headline ($detail)" else headline

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
                    // The framework only logs "error returned from framework"; the useful
                    // part is in the exception message, which carries Google's own console
                    // codes — [28444] is a package/SHA-1 that no OAuth client matches.
                    Log.e("GoogleSignIn", "Credential Manager failed: ${e.type} ${e.message}", e)
                    onError(explain(e.type, e.message))
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Sign-in failed: ${e.message}", e)
                    onError(debugDetail("Couldn't sign in.", e.message))
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
