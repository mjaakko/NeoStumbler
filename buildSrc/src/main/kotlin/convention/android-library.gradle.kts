package convention

plugins {
    id("convention.kotlin")
    id("org.jetbrains.kotlin.android")
    id("com.android.library")
}

android {
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(project.extra["jvmTarget"]!!)
        targetCompatibility = JavaVersion.toVersion(project.extra["jvmTarget"]!!)
    }
}
