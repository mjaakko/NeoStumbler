package convention

import constants.JvmVersion

plugins {
    id("convention.kotlin")
    id("com.android.library")
}

android {
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JvmVersion.JVM_TARGET_VERSION)
        targetCompatibility = JavaVersion.toVersion(JvmVersion.JVM_TARGET_VERSION)
    }

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

tasks.register("lintAll") { dependsOn(tasks.named("lintDebug")) }

tasks.register("assembleAll") {
    dependsOn(
        tasks.named("assembleDebug"),
        tasks.named("assembleAndroidTest"),
        tasks.named("assembleDebugUnitTest"),
    )
}

tasks.register("unitTest") { dependsOn(tasks.named("testDebugUnitTest")) }

tasks.register("androidTest") { dependsOn(tasks.named("connectedAndroidTest")) }
