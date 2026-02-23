package com.jameeli.thykra.storage

/**
 * S3 storage implementation (stub).
 *
 * To enable, add the AWS SDK for Java v2 dependency:
 *   software.amazon.awssdk:s3 (with presigner)
 * And configure: STORAGE_TYPE=s3, S3_BUCKET, S3_REGION env vars.
 */
class S3StorageService : StorageService {
    override suspend fun generatePresignedUpload(key: String, contentType: String): PresignedUpload {
        throw NotImplementedError("S3 storage not yet configured. See S3StorageService for setup instructions.")
    }

    override fun getPublicUrl(key: String): String {
        throw NotImplementedError("S3 storage not yet configured.")
    }

    override suspend fun delete(key: String) {
        throw NotImplementedError("S3 storage not yet configured.")
    }
}
