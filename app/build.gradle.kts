plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "nl.ritbon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.ritbon.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "1.20"
    }

    buildFeatures { compose = true }
    val updateKeystorePath = System.getenv("VLOTRIT_KEYSTORE_PATH")
    val updateKeystorePassword = System.getenv("VLOTRIT_KEYSTORE_PASSWORD") ?: "android"
    val updateKeyAlias = System.getenv("VLOTRIT_KEY_ALIAS") ?: "androiddebugkey"
    val updateKeyPassword = System.getenv("VLOTRIT_KEY_PASSWORD") ?: "android"
    signingConfigs {
        create("vlotritUpdates") {
            storeFile = updateKeystorePath?.let { rootProject.file(it) } ?: rootProject.file("../../work/android-home/debug.keystore")
            storePassword = updateKeystorePassword
            keyAlias = updateKeyAlias
            keyPassword = updateKeyPassword
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("vlotritUpdates") }
        getByName("release") { signingConfig = signingConfigs.getByName("vlotritUpdates") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core-ktx:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

