package convention

import constants.AndroidSdkVersions.COMPILE_SDK
import constants.AndroidSdkVersions.MIN_SDK
import constants.AndroidSdkVersions.TARGET_SDK
import constants.JvmVersion

plugins {
    id("convention.kotlin")
    id("com.android.application")
}

android {
    compileSdk {
        version = COMPILE_SDK
    }

    defaultConfig {
        minSdk {
            version = MIN_SDK
        }
        targetSdk {
            version = TARGET_SDK
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JvmVersion.JVM_TARGET_VERSION)
        targetCompatibility = JavaVersion.toVersion(JvmVersion.JVM_TARGET_VERSION)
    }

    dependenciesInfo {
        // Add dependencies info block only if specifically enabled with a Gradle property
        // See https://android.izzysoft.de/articles/named/iod-scan-apkchecks#blobs
        includeInApk = project.hasProperty("includeDependenciesInfo")
        includeInBundle = project.hasProperty("includeDependenciesInfo")
    }
}

tasks.register("lintAll") {
    dependsOn(
        tasks.named { it.startsWith("lint") && !it.startsWith("lintFix") && it.endsWith("Debug") }
    )
}

tasks.register("assembleAll") {
    dependsOn(
        tasks.named("assembleDebug"),
        tasks.named("assembleAndroidTest"),
        tasks.named { it.startsWith("assemble") && it.endsWith("DebugUnitTest") },
    )
}

tasks.register("unitTest") {
    dependsOn(tasks.named { it.startsWith("test") && it.endsWith("DebugUnitTest") })
}

tasks.register("androidTest") { dependsOn(tasks.named("connectedAndroidTest")) }
