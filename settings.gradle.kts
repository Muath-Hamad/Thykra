rootProject.name = "Thykra"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

/**
 * Includes a module only when its directory is actually present.
 *
 * The server image builds from a context that deliberately omits the client modules
 * (see `.dockerignore`) so it needs neither the Android SDK nor Node. Gradle 8 tolerated
 * a configured project with no directory and only warned; Gradle 9 makes it a hard
 * failure, which broke the Docker build the moment the wrapper was upgraded.
 *
 * https://docs.gradle.org/current/userguide/multi_project_builds.html#include_existing_projects_only
 */
fun includeIfPresent(name: String) {
    if (settingsDir.resolve(name).isDirectory) {
        include(":$name")
    } else {
        logger.lifecycle("Skipping :$name — not present in this build context.")
    }
}

// Optional: absent from the server's build context, present everywhere else.
includeIfPresent("composeApp")

// Required: the server cannot be built without them, so a missing directory here is a
// broken checkout and should fail loudly rather than be skipped.
include(":server")
include(":shared")