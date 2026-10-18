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
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Release signing uses repository Actions secrets. Never commit a keystore/password.
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
        getByName("release") {
            isMinifyEnabled = false
            val keyStorePath = System.getenv("JARVIS_KEYSTORE_PATH")
            if (!keyStorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
