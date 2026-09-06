package com.jameeli.thykra.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val createdAt: Instant
)
