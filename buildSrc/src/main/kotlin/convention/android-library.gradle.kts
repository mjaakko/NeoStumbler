package convention

import constants.AndroidSdkVersions.COMPILE_SDK
import constants.AndroidSdkVersions.MIN_SDK
import constants.JvmVersion

plugins {
    id("convention.kotlin")
    id("com.android.library")
}

android {
    compileSdk {
        version = COMPILE_SDK
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JvmVersion.JVM_TARGET_VERSION)
        targetCompatibility = JavaVersion.toVersion(JvmVersion.JVM_TARGET_VERSION)
    }

    defaultConfig {
        minSdk {
            version = MIN_SDK
        }

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
