package com.jameeli.thykra.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

/**
 * Production-grade S3 storage backed by AWS SDK for Java v2.
 *
 * Supports both real AWS S3 (when [endpoint] is null/empty) and S3-compatible
 * services such as LocalStack and MinIO (when [endpoint] is set, typically
 * paired with [pathStyleAccess] = true).
 *
 * Credentials precedence:
 * 1. Static [accessKey]/[secretKey] if both are non-blank.
 * 2. Otherwise the AWS default credential provider chain (env vars, profile,
 *    container/instance role).
 */
class S3StorageService(
    private val bucket: String,
    private val region: String,
    private val endpoint: String? = null,
    private val accessKey: String? = null,
    private val secretKey: String? = null,
    private val publicBaseUrl: String? = null,
    private val pathStyleAccess: Boolean = false,
    private val presignExpirySeconds: Int = 3600,
) : StorageService, AutoCloseable {

    init {
        require(bucket.isNotBlank()) { "S3 bucket must be configured (set S3_BUCKET)" }
        require(region.isNotBlank()) { "S3 region must be configured (set S3_REGION)" }
    }

    private val credentialsProvider = if (!accessKey.isNullOrBlank() && !secretKey.isNullOrBlank()) {
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
    } else {
        DefaultCredentialsProvider.create()
    }

    private val s3Configuration = S3Configuration.builder()
        .pathStyleAccessEnabled(pathStyleAccess)
        .build()

    private val client: S3Client = S3Client.builder().apply {
        region(Region.of(region))
        credentialsProvider(credentialsProvider)
        serviceConfiguration(s3Configuration)
        if (!endpoint.isNullOrBlank()) endpointOverride(URI.create(endpoint))
    }.build()

    private val presigner: S3Presigner = S3Presigner.builder().apply {
        region(Region.of(region))
        credentialsProvider(credentialsProvider)
        serviceConfiguration(s3Configuration)
        if (!endpoint.isNullOrBlank()) endpointOverride(URI.create(endpoint))
    }.build()

    override suspend fun generatePresignedUpload(key: String, contentType: String): PresignedUpload =
        withContext(Dispatchers.IO) {
            val putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build()
            val presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignExpirySeconds.toLong()))
                .putObjectRequest(putReq)
                .build()
            val presigned = presigner.presignPutObject(presignReq)
            PresignedUpload(
                uploadUrl = presigned.url().toString(),
                method = "PUT",
                headers = mapOf("Content-Type" to contentType),
                expiresIn = presignExpirySeconds,
            )
        }

    override fun getPublicUrl(key: String): String {
        val base = publicBaseUrl?.takeIf { it.isNotBlank() }
        if (base != null) {
            return "${base.trimEnd('/')}/$key"
        }
        if (!endpoint.isNullOrBlank()) {
            // S3-compatible (LocalStack/MinIO) — use path-style URL since pathStyleAccess is typically true.
            return "${endpoint.trimEnd('/')}/$bucket/$key"
        }
        // Real AWS — virtual-hosted-style URL.
        return "https://$bucket.s3.$region.amazonaws.com/$key"
    }

    override suspend fun delete(key: String) {
        withContext(Dispatchers.IO) {
            client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            )
        }
    }

    override suspend fun readBytes(key: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            ).asByteArray()
        } catch (_: NoSuchKeyException) {
            null
        }
    }

    override suspend fun writeBytes(key: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build(),
                RequestBody.fromBytes(bytes)
            )
        }
    }

    override fun close() {
        runCatching { presigner.close() }
        runCatching { client.close() }
    }
}
