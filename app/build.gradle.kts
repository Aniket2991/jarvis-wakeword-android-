plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jarvis.wakeword"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarvis.wakeword"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Owner-controlled signing is preferred. If no key is supplied, CI produces a
    // temporary debug-signed release preview for first-install testing only.
    signingConfigs {
        create("release") {
            val keyStorePath = System.getenv("JARVIS_KEYSTORE_PATH")
            if (!keyStorePath.isNullOrBlank()) {
                storeFile = file(keyStorePath)
                storePassword = System.getenv("JARVIS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("JARVIS_KEY_ALIAS")
                keyPassword = System.getenv("JARVIS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt")
            )
            val keyStorePath = System.getenv("JARVIS_KEYSTORE_PATH")
            signingConfig = if (!keyStorePath.isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                // Temporary CI preview only; do not use this certificate for distribution.
                signingConfigs.getByName("debug")
            }
        }
    }
}
