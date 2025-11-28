import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule

plugins {
    id("convention.android-app")
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.bundletool)
    alias(libs.plugins.aboutLibraries)
}

bundletool {
    signingConfig {
        storeFile.set(rootProject.layout.projectDirectory.file("keystore.jks"))
        storePassword.set(providers.environmentVariable("SIGNING_STORE_PASSWORD"))
        keyAlias.set(providers.environmentVariable("SIGNING_KEY_ALIAS"))
        keyPassword.set(providers.environmentVariable("SIGNING_KEY_PASSWORD"))
    }
}

android {
    namespace = "xyz.malkki.neostumbler"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file("../keystore.jks")
            storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
        }
    }

    defaultConfig {
        applicationId = "xyz.malkki.neostumbler"
        versionCode = 51
        versionName = "2.2.0-rc2"

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

    flavorDimensions +=
        listOf(
            // Whether the flavor includes non-free components such as Google Play Services
            "nonfreeComponents",
            // Whether the flavor is compatible with Google Play policies
            "gplayCompatible",
        )

    productFlavors {
        // fdroid build flavor must not include any nonfree components
        create("fdroid") {
            dimension = "nonfreeComponents"

            applicationId = defaultConfig.applicationId + ".fdroid"
        }
        // full build flavor can include nonfree components such as Google Play Services
        create("full") {
            dimension = "nonfreeComponents"
            isDefault = true

            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
        }

        // Default build flavor which doesn't need to be compatible with Google Play Policies
        create("default") {
            dimension = "gplayCompatible"
            isDefault = true
        }

        create("gplay") {
            dimension = "gplayCompatible"

            applicationId = defaultConfig.applicationId + ".gplay"
        }
    }

    sourceSets {
        getByName("gplay") {
            assets {
                directories +=
                    project.layout.buildDirectory.dir("privacypolicy").get().asFile.absolutePath
            }
        }
    }

    applicationVariants.configureEach {
        if (buildType.isDebuggable) {
            resValue("string", "app_name", "NS (dev, $flavorName)")
        }

        if (productFlavors.any { it.name == "gplay" }) {
            mergeAssetsProvider.configure { dependsOn(tasks.named("copyPrivacyPolicy")) }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            // Licenses
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/**/LICENSE.txt"
            // OSGI and GraalVM metadata not needed on Android
            excludes += "/META-INF/**/OSGI-INF/*"
            excludes += "/META-INF/native-image/**/*"
            // We don"t use these codecs for anything
            excludes += "**/apache/**/codec/**/*"
            // No need for beacon distance models used by Android Beacon Library
            excludes += "**/model-distance-calculations.json"
            // https://github.com/Kotlin/kotlinx.coroutines#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
            excludes += "DebugProbesKt.bin"
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

    lint {
        checkReleaseBuilds = false

        lintConfig = projectDir.resolve("lint.xml")
    }
}

tasks.register<Copy>("copyPrivacyPolicy") {
    from(rootProject.layout.projectDirectory.file("docs/privacy_policy.md"))
    into(project.layout.buildDirectory.dir("privacypolicy"))
}

androidComponents {
    beforeVariants { variant ->
        val flavorByDimension = variant.productFlavors.toMap()

        if (
            flavorByDimension["nonfreeComponents"] == "fdroid" &&
                flavorByDimension["gplayCompatible"] == "gplay"
        ) {
            // fdroid variant does not have to be compliant with Google Play policies, so let's
            // disable the variant
            variant.enable = false
        }
    }
}

tasks.register("printVersionName") {
    val versionName = project.android.defaultConfig.versionName

    doLast { println(versionName) }
}

tasks.register("printVersionCode") {
    val versionCode = project.android.defaultConfig.versionCode

    doLast { println(versionCode) }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
    }
}

aboutLibraries {
    offlineMode = true

    library {
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.GROUP
    }
}

dependencies {
    implementation(project(":libs:beacon-library-utils"))
    implementation(project(":libs:geography"))
    implementation(project(":libs:ichnaea"))
    implementation(project(":libs:utils"))
    implementation(project(":libs:executors"))
    implementation(project(":libs:coroutine-broadcast-receiver"))

    implementation(project(":app:core"))
    implementation(project(":app:core:mapper:android-location"))

    implementation(project(":app:data:emitter:android"))

    implementation(project(":app:data:airpressure:android"))
    implementation(project(":app:data:battery:android"))

    implementation(project(":app:data:geocoder:android"))

    implementation(project(":app:data:location:android"))
    "fullImplementation"(project(":app:data:location:googleplay"))

    implementation(project(":app:data:settings:android-datastore"))

    implementation(project(":app:data:reports:room"))

    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)
    implementation(libs.koinAndroid)
    implementation(libs.koinAndroidxCompose)

    implementation(libs.androidx.collection)

    implementation(libs.androidx.browser)

    implementation(libs.androidx.core)
    implementation(libs.androidx.activityCompose)

    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.splashscreen)

    implementation(libs.bundles.androidxLifecycle)

    implementation(libs.androidx.pagingCommon)
    implementation(libs.androidx.pagingCompose)

    implementation(libs.bundles.androidxNavigation)

    val composeBom = platform(libs.androidx.composeBom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.composeUi)
    implementation(libs.androidx.composeUiGraphics)
    implementation(libs.androidx.composeUiToolingPreview)

    debugImplementation(libs.androidx.composeUiTooling)
    debugImplementation(libs.androidx.composeUiTestManifest)

    implementation(libs.androidx.composeMaterial3)
    implementation(libs.androidx.composeMaterial3AdaptiveNavigationSuite)

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

    implementation(libs.bundles.androidxWork)

    implementation(libs.timber)

    implementation(platform(libs.okhttpBom))
    implementation(libs.okhttp)
    implementation(libs.okhttpCoroutines)
    implementation(libs.okhttpLoggingInterceptor)
    testImplementation(libs.okhttpMockWebServer)

    implementation(libs.kotlinx.serializationJson)

    implementation(libs.androidBeaconLibrary)

    implementation(libs.bundles.mapLibre)

    implementation(libs.geohex)

    implementation(libs.fastcsv)

    implementation(libs.bundles.vico)

    implementation(libs.bundles.aboutLibraries)

    "gplayImplementation"(libs.bundles.multiplatformMarkdownRenderer)

    "fullImplementation"(libs.playservices.cronet)
    "fullImplementation"(libs.cronetOkhttp)

    testImplementation(libs.mockitoKotlin)

    androidTestImplementation(libs.awaitilityKotlin)
}
