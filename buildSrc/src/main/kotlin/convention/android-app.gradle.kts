package convention

plugins {
    id("convention.kotlin")
    id("org.jetbrains.kotlin.android")
    id("com.android.application")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(project.extra["jvmTarget"]!!)
        targetCompatibility = JavaVersion.toVersion(project.extra["jvmTarget"]!!)
    }

    // These are not needed because the app is not published to Google Play
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
