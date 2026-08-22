plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.gostudios.console.watch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gostudios.console.watch"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val home = System.getenv("USERPROFILE") ?: System.getProperty("user.home")
            val keystore = System.getenv("ANDROID_APP_KEYSTORE") ?: "$home/.android/debug.keystore"
            storeFile = file(keystore)
            storePassword = System.getenv("ANDROID_APP_STORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("ANDROID_APP_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("ANDROID_APP_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            lint {
                checkReleaseBuilds = false
                abortOnError = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Wear Compose
    implementation("androidx.wear.compose:compose-material:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-material3:1.5.6")

    // Wear OS
    implementation("com.google.android.wearable:wearable:2.9.0")
    implementation("androidx.wear:wear:1.3.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
}