package com.jameeli.thykra.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null
)
