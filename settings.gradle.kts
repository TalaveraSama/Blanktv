pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // A libmpv built on this machine (OwnTV_libmpv's buildscripts/wsl_build.sh), only while the
        // build command carries -Powntv.libmpvLocalRepo=<folder> (never a file) — :player-core then
        // asks for version "local".
        providers.gradleProperty("owntv.libmpvLocalRepo").orNull?.let { dir ->
            maven {
                name = "LocalLibmpv"
                url = uri(file(dir))
                content { includeVersion("tv.own.owntv", "libmpv", "local") }
            }
        }
        google()
        mavenCentral()
        // OwnTV's public Maven repository, still needed for exactly one artifact:
        // tv.own.owntv:libmpv, the mpv/FFmpeg native engine built by ahXN00/OwnTV_libmpv. The
        // engine's Kotlin modules (:core, :player-core) are part of this build and no longer come
        // from here. See docs/FORK.md for what that means for a future fork of libmpv.
        maven {
            name = "OwnTV"
            url = uri("https://ahxn00.github.io/OwnTV_Core/maven")
            content { includeGroup("tv.own.owntv") }
        }
    }
}

rootProject.name = "BlankTV"

// The playback engine and the data/document/settings engine. Both are libraries — no screens —
// vendored from ahXN00/OwnTV_Core (see NOTICE.md) and built from source in this repository, so a
// change here and a change in :app are always the same build.
include(":player-core")
include(":core")

// The app itself: Android TV + touchscreen, Compose for TV, vendored from ahXN00/OwnTV.
include(":app")

// Baseline-profile generator. Test-only module: it ships nothing to users, it records the cold
// start on a device and writes the profile :app packages.
include(":baselineprofile")
