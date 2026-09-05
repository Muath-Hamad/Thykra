plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

group = "com.jameeli.thykra"
version = "1.0.0"
application {
    mainClass.set("com.jameeli.thykra.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.named<JavaExec>("run") {
    val envFile = file(".env")
    if (envFile.exists()) {
        envFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") && '=' in it }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                environment(key.trim(), value.trim())
            }
    }
}

tasks.withType<Test> {
    // Each test class boots the whole Ktor module against its own H2 database.
    // Exposed's Database.connect is a JVM-global singleton with thread-local
    // transaction managers, so classes sharing a JVM can bleed into each other —
    // fork a fresh JVM per class for real isolation.
    forkEvery = 1
    maxParallelForks = 1
}

dependencies {
    implementation(projects.shared)

    // Ktor Server
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.forwardedHeader)
    implementation(libs.ktor.serialization.json)

    // Ktor Client (for OAuth token verification)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json.common)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlinDatetime)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hikariCp)
    implementation(libs.thumbnailator)
    implementation(libs.metadataExtractor)

    // AWS SDK v2 (S3 + presigner) for production / S3-compatible storage (LocalStack, MinIO, etc.)
    implementation(libs.aws.s3)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Test
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.kotlin.testJunit)
}
