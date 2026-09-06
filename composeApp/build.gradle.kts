import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // The theme's platform hooks (haptics, reduced motion, theme preference) are
    // expect/actual objects, which are still flagged Beta by the compiler.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.core.ktx)
            // AppCompatDelegate.setApplicationLocales is the per-app locale switch the
            // Me screen uses; it lives in appcompat and nowhere else.
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServices)
            implementation(libs.googleid)
            implementation(libs.androidx.work.runtime)
            // Reads a photo's own capture date, so a queued upload lands in the
            // right day chapter before the server has seen it.
            implementation(libs.androidx.exifinterface)
            implementation(libs.coil.video)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // Android-only unit tests: Robolectric + Compose UI test on the JVM.
        // Runs headless via `:composeApp:testDebugUnitTest` — no emulator needed.
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.testJunit)
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.compose.ui.test.junit4)
            }
        }
    }
}

// Bundled Wanderlust Editions typefaces live in
// composeApp/src/commonMain/composeResources/font/. Pin the generated accessor's
// package so `com.jameeli.thykra.resources.Res.font.*` is stable across module renames.
compose.resources {
    publicResClass = false
    packageOfResClass = "com.jameeli.thykra.resources"
    generateResClass = auto
}

android {
    namespace = "com.jameeli.thykra"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jameeli.thykra"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val localProps = rootProject.file("local.properties")
        val props = Properties().apply {
            if (localProps.exists()) localProps.inputStream().use { load(it) }
        }
        val googleClientId = props.getProperty("GOOGLE_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
    }

    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Robolectric needs Android resources on the JVM unit-test classpath.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    // ui-test-manifest carries a debug AndroidManifest declaring ComponentActivity
    // so Robolectric can launch the host activity Compose's test rule needs.
    // It must be on the *Android* configuration, not androidUnitTest, because
    // Robolectric reads from the merged debug manifest.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
