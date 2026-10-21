// Root build file for BlankTV — one build containing the app and the engine it runs on.
//
// The two engine modules (:core, :player-core) are vendored from ahXN00/OwnTV_Core at core-1.0.68
// (see NOTICE.md); :app is vendored from ahXN00/OwnTV. Upstream shipped them as three separate
// repositories with a pinned artifact between them. Here they are one build, which is the whole
// point of the fork: an engine change and an app change land together, and there is no version pin
// that can disagree with the code.
plugins {
    alias(libs.plugins.android.application) apply false
    // com.android.library ships inside AGP and is already on the build classpath by the time a
    // module asks for it by version, so it is declared here and applied without a version there.
    alias(libs.plugins.android.library) apply false
    // Test-only module that records the baseline profile :app packages.
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    // Kotlin comes from AGP 9's built-in Kotlin support. Compose compiler is pinned to that Kotlin
    // version; KSP supports built-in Kotlin, so no kotlin-android plugin is needed.
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}

// Feeds `version` on :core and :player-core, which keep the publishing block they were written with.
// Nothing in this repository publishes those artifacts — an app build never resolves them from a
// repository, it compiles them. Keep it in step with the upstream release the engine was vendored
// from so a stack trace or a manifest that prints it still points at the right upstream code.
extra["coreVersion"] = "1.0.68"
