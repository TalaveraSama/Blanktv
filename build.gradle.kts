// Top-level build file. Module plugins are declared here without being applied so every
// sub-project can alias them and Gradle still resolves a single plugin version across the build.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
