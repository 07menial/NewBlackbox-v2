plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.multicloner"
    compileSdk = 35
    ndkVersion = "29.0.13846066"

    defaultConfig {
        applicationId = "com.example.multicloner"
        minSdk = 21
        // targetSdk 28 e necesar pentru compatibilitate cu motorul de virtualizare (BlackBox)
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            // Semnat cu cheia debug ca sa poata fi instalat direct
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // compatibil cu Kotlin 1.9.23
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            excludes += "**/libandroidx.graphics.path.so"
        }
        resources {
            excludes += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    implementation(project(":Bcore"))

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
