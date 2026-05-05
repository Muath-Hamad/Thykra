package com.jameeli.thykra.widget

import android.content.Context
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AndroidTokenStorage
import io.ktor.client.HttpClient

/**
 * Bundle of API clients used by widget update jobs. Built on demand each refresh — widgets
 * are infrequent and short-lived processes, so caching provides little benefit.
 */
class WidgetApi(context: Context) {
    private val tokenProvider = AndroidTokenStorage(context.applicationContext)
    private val httpClient = createApiClient(tokenProvider, isDebug = false)

    val albums = AlbumApi(httpClient)
    val media = MediaApi(httpClient)
    val reactions = ReactionApi(httpClient)
    val comments = CommentApi(httpClient)

    /** Underlying HTTP client, exposed for widget code that needs to download raw thumbnails. */
    val client: HttpClient get() = httpClient

    suspend fun isSignedIn(): Boolean = tokenProvider.getAccessToken() != null

    fun close() {
        httpClient.close()
    }
}
