plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gostudios.console.touchscreen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gostudios.console.touchscreen"
        minSdk = 23
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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
