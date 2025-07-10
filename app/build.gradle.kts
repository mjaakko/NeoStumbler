import kotlin.math.roundToInt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.accrescent.tools.bundletool")
    id("com.ncorti.ktfmt.gradle")
    id("io.gitlab.arturbosch.detekt")
}

private val DB_SCHEMAS_DIR = "$projectDir/schemas"

bundletool {
    signingConfig {
        storeFile = file("../keystore.jks")
        storePassword = System.getenv("SIGNING_STORE_PASSWORD")
        keyAlias = System.getenv("SIGNING_KEY_ALIAS")
        keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
    }
}

ksp {
    arg("room.schemaLocation", DB_SCHEMAS_DIR)
    arg("room.incremental", "true")
}

android {
    namespace = "xyz.malkki.neostumbler"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file("../keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "xyz.malkki.neostumbler"
        minSdk = 29
        targetSdk = 36
        versionCode = 38
        versionName = "2.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        androidResources {
            // Configure supported languages here to avoid including incomplete translations in the
            // releases
            localeFilters +=
                listOf(
                    "en",
                    "cs",
                    "de",
                    "es",
                    "fi",
                    "fr",
                    "hu",
                    "it",
                    "iw",
                    "ja",
                    "lt",
                    "nb-rNO",
                    "nl",
                    "pl",
                    "pt",
                    "pt-rBR",
                    "ru",
                    "sv",
                    "ta",
                    "uk",
                    "zh-rCN",
                    "zh-rTW",
                )
        }

        // Add supported locales to a build config field for the language picker UI
        buildConfigField(
            "String",
            "SUPPORTED_LOCALES",
            "\"" + androidResources.localeFilters.joinToString(",") + "\"",
        )
    }

    sourceSets { getByName("androidTest") { assets { srcDir(files(DB_SCHEMAS_DIR)) } } }

    androidResources { generateLocaleConfig = true }

    bundle {
        // Don't split the app bundle by language, because we have an in-app language switcher which
        // needs all languages to be available
        language { enableSplit = false }
    }

    buildTypes {
        getByName("debug") { applicationIdSuffix = ".dev" }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")

            isDebuggable = false

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "altbeacon.pro",
            )

            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
        }
    }

    buildFeatures { flavorDimensions += "version" }

    productFlavors {
        create("fdroid") {
            dimension = "version"

            applicationId = defaultConfig.applicationId + ".fdroid"
        }
        create("full") {
            dimension = "version"

            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
        }
    }

    applicationVariants.configureEach {
        if (buildType.isDebuggable) {
            resValue("string", "app_name", "NeoStumbler (dev, $flavorName)")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // We don"t use these codec - they are probably pulled in with some library
            excludes += "**/apache/**/codec/**/*"
            // No need for beacon distance models used by Android Beacon Library
            excludes += "**/model-distance-calculations.json"
        }
    }

    splits {
        abi {
            isEnable = project.hasProperty("enableAbiSplit")

            reset()
            include("armeabi-v7a", "arm64-v8a")

            isUniversalApk = true
        }
    }

    // These are not needed because the app is not published to Google Play
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint { lintConfig = file("app/lint.xml") }
}

tasks.register("printVersionName") {
    val versionName = project.android.defaultConfig.versionName

    doLast { println(versionName) }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.io.path.ExperimentalPathApi")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.FlowPreview")
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }

    maxParallelForks =
        (Runtime.getRuntime().availableProcessors() / 2.0).roundToInt().coerceAtLeast(1)

    // Don't generate test reports, because currently we don't use them for anything
    reports.html.required = false
    reports.junitXml.required = false
}

dependencies {
    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)
    implementation(libs.koinAndroid)
    implementation(libs.koinAndroidxCompose)

    implementation(libs.androidx.core)
    implementation(libs.androidx.activityCompose)

    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.splashscreen)

    implementation(libs.androidx.lifecycleRuntime)
    implementation(libs.androidx.lifecycleRuntimeCompose)
    implementation(libs.androidx.lifecycleViewmodel)
    implementation(libs.androidx.lifecycleViewmodelCompose)

    implementation(libs.androidx.pagingCommon)
    implementation(libs.androidx.pagingCompose)

    val composeBom = platform(libs.androidx.composeBom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.composeUi)
    implementation(libs.androidx.composeUiGraphics)
    implementation(libs.androidx.composeUiToolingPreview)

    debugImplementation(libs.androidx.composeUiTooling)
    debugImplementation(libs.androidx.composeUiTestManifest)

    implementation(libs.androidx.composeMaterial3)
    implementation(libs.androidx.composeMaterialIconsExtended)

    androidTestImplementation(libs.androidx.composeUiTestJunit)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.testJunit)
    androidTestImplementation(libs.androidx.testEspresso)
    androidTestImplementation(libs.androidx.testRules)

    implementation(platform(libs.kotlinx.coroutinesBom))
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.coroutinesAndroid)
    "fullImplementation"(libs.kotlinx.coroutinesPlayServices)

    testImplementation(libs.kotlinx.coroutinesTest)
    androidTestImplementation(libs.kotlinx.coroutinesTest)

    "fullImplementation"(libs.playservices.location)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.roomRuntime)
    annotationProcessor(libs.androidx.roomCompiler)
    ksp(libs.androidx.roomCompiler)
    implementation(libs.androidx.roomKtx)
    implementation(libs.androidx.roomPaging)
    androidTestImplementation(libs.androidx.roomTesting)

    implementation(libs.androidx.workRuntime)
    implementation(libs.androidx.workRuntimeKtx)

    implementation(libs.timber)

    implementation(platform(libs.okhttpBom))
    implementation(libs.okhttp)
    implementation(libs.okhttpLoggingInterceptor)
    testImplementation(libs.okhttpMockWebServer)

    implementation(libs.kotlinx.serializationJson)

    implementation(libs.androidBeaconLibrary)

    implementation(libs.mapLibre)
    implementation(libs.mapLibreAnnotationsPlugin)

    implementation(libs.geohex)

    implementation(libs.fastcsv)

    implementation(libs.vicoCompose)
    implementation(libs.vicoComposeMaterial3)

    "fullImplementation"(libs.playservices.cronet)
    "fullImplementation"(libs.cronetOkhttp)

    testImplementation(libs.mockitoKotlin)

    androidTestImplementation(libs.awaitilityKotlin)
}

configurations.configureEach {
    resolutionStrategy {
        force("org.hamcrest:hamcrest-core:3+")
        force("org.hamcrest:hamcrest-library:3+")
    }
}

ktfmt { kotlinLangStyle() }
