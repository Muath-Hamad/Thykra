package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.InvitePreviewDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

/**
 * The invite landing.
 *
 * [getPreview] is auth-aware but not auth-required: someone who has never signed in has
 * to be able to see what they were invited to, because asking them to sign in first loses
 * them. Every outcome is an HTTP 200 whose `status` IS the answer, so a revoked or
 * unknown token renders a designed dead end rather than an error.
 */
class InviteApi(private val client: HttpClient) {

    suspend fun getPreview(token: String): ApiResponse<InvitePreviewDto> =
        client.get("$API_BASE_URL/api/invites/$token/preview").body()

    /** Joining needs a session; the caller sends the user to sign-in first if there isn't one. */
    suspend fun join(token: String): ApiResponse<AlbumDto> =
        client.post("$API_BASE_URL/api/albums/join/$token").body()
}
