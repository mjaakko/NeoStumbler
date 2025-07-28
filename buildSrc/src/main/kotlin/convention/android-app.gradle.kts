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
        project.android.productFlavors
            .map { productFlavor -> productFlavor.name.replaceFirstChar { it.uppercase() } }
            .map { flavorName -> tasks.named("lint${flavorName}Debug") }
    )
}

tasks.register("assembleAll") {
    dependsOn(
        tasks.named("assembleDebug"),
        tasks.named("assembleAndroidTest"),
        project.android.productFlavors
            .map { productFlavor -> productFlavor.name.replaceFirstChar { it.uppercase() } }
            .map { flavorName -> tasks.named("assemble${flavorName}DebugUnitTest") },
    )
}

tasks.register("unitTest") {
    dependsOn(
        project.android.productFlavors
            .map { productFlavor -> productFlavor.name.replaceFirstChar { it.uppercase() } }
            .map { flavorName -> tasks.named("test${flavorName}DebugUnitTest") }
    )
}

tasks.register("androidTest") { dependsOn(tasks.named("connectedAndroidTest")) }
