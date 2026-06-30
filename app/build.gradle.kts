plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.gua.browser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gua.browser"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(project(":core"))

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.navigation)
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // GeckoView（运行时下载，不打包进 APK）
    compileOnly(libs.geckoview)

    // Storage
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.datastore.preferences)
}
