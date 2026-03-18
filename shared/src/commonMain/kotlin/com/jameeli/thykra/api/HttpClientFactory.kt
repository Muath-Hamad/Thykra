package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.RefreshTokenRequest
import com.jameeli.thykra.model.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(): HttpClient

fun createApiClient(tokenProvider: TokenProvider, isDebug: Boolean = false): HttpClient {
    // Separate unauthenticated client used only for token refresh to avoid recursion
    val refreshHttpClient = createPlatformHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    return createPlatformHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        if (isDebug) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[HTTP] $message")
                    }
                }
                level = LogLevel.INFO
            }
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenProvider.getAccessToken()
                    val refresh = tokenProvider.getRefreshToken()
                    if (access != null && refresh != null) {
                        BearerTokens(access, refresh)
                    } else null
                }
                refreshTokens {
                    val refreshToken = oldTokens?.refreshToken ?: return@refreshTokens null
                    try {
                        val response: ApiResponse<TokenResponse> =
                            refreshHttpClient.post("$API_BASE_URL/api/auth/refresh") {
                                contentType(ContentType.Application.Json)
                                setBody(RefreshTokenRequest(refreshToken))
                            }.body()
                        if (response.success && response.data != null) {
                            val newTokens = response.data!!
                            tokenProvider.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                            BearerTokens(newTokens.accessToken, newTokens.refreshToken)
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }
}

fun createRawHttpClient(isDebug: Boolean = false): HttpClient {
    return createPlatformHttpClient().config {
        if (isDebug) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[HTTP-RAW] $message")
                    }
                }
                level = LogLevel.INFO
            }
        }
    }
}
