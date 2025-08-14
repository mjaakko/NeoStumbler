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

    defaultConfig {
        minSdk = 29
        targetSdk = 36
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
