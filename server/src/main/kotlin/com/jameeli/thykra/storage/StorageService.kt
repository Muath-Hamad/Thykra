package com.jameeli.thykra.storage

interface StorageService {
    suspend fun generatePresignedUpload(key: String, contentType: String): PresignedUpload
    fun getPublicUrl(key: String): String
    suspend fun delete(key: String)
    suspend fun readBytes(key: String): ByteArray?
    suspend fun writeBytes(key: String, bytes: ByteArray)
}

data class PresignedUpload(
    val uploadUrl: String,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    val expiresIn: Int = 3600
)
