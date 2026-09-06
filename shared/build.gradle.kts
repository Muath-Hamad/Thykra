import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        outputModuleName = "shared"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json.common)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            // OkHttp, not CIO — see HttpClientFactory.android.kt for why.
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

/** Kept in step with [com.jameeli.thykra.SERVER_PORT]. */
val SERVER_PORT = 8081

android {
    namespace = "com.jameeli.thykra.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Where this build talks to. 10.0.2.2 is the emulator's loopback to the host;
        // a device build needs a real origin, so `local.properties` or the environment
        // can override it without touching source.
        val localProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        fun setting(key: String, default: String): String =
            localProps.getProperty(key)
                ?: System.getenv("THYKRA_$key")
                ?: default

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${setting("API_BASE_URL", "http://10.0.2.2:$SERVER_PORT")}\"",
        )
        buildConfigField(
            "String",
            "WEB_BASE_URL",
            "\"${setting("WEB_BASE_URL", "http://10.0.2.2:8080")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }
}
