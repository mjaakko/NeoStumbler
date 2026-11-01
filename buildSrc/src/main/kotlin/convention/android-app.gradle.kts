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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

val unitTest = tasks.register("unitTest")

val assembleAll =
    tasks.register("assembleAll") {
        dependsOn(
            tasks.named("assembleDebug"),
            tasks.named("assembleAndroidTest"),
        )
    }

val lintAll = tasks.register("lintAll")

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val variantName = variant.flavorName?.replaceFirstChar { it.uppercaseChar() }

        unitTest.configure {
            dependsOn("test${variantName}DebugUnitTest")
        }

        assembleAll.configure {
            dependsOn(tasks.named("compile${variantName}DebugUnitTestSources"))
        }

        lintAll.configure {
            dependsOn(tasks.named("lint${variantName}Debug"))
        }
    }
}

tasks.register("androidTest") { dependsOn(tasks.named("connectedAndroidTest")) }
