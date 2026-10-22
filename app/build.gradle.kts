plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tokenmonitor.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tokenmonitor.app"
        minSdk = 31
        // The API 37 migration is documented in LocalNetworkAccess.kt; the
        // lint check below and this suppression only silence the edit detector.
        //noinspection EditedTargetSdkVersion
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    signingConfigs {
        // Debug builds are signed with a key checked into the repository so CI
        // and local builds produce the same signature. These are the standard
        // non-secret debug credentials; use a private release key for public
        // distribution.
        val stableDebug = rootProject.file("keystore/debug.keystore")
        if (stableDebug.exists()) {
            getByName("debug") {
                storeFile = stableDebug
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    buildFeatures {
        compose = true
    }
    lint {
        // The API 37 migration was reviewed: LAN access is covered by
        // ACCESS_LOCAL_NETWORK (LocalNetworkAccess.kt), and the remaining
        // API 37 changes do not affect this app. This IDE-only check flags the
        // edit itself, so record the review here instead of in the IDE.
        disable += "EditedTargetSdkVersion"
    }
}

// The androidTest runtime classpath does not receive the main platform BOM in
// this AGP version. Extend it from the app's implementation configuration so the
// Compose BOM applies there too, instead of declaring the same BOM twice.
configurations.named("androidTestRuntimeOnly") {
    // Resolve eagerly: the Provider overload of extendsFrom is @Incubating.
    extendsFrom(configurations.getByName("implementation"))
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}