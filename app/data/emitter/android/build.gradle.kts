plugins {
    id("convention.android-library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "xyz.malkki.neostumbler.emitters.android"

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    api(project(":app:data:emitter:api"))

    implementation(project(":libs:executors"))
    implementation(project(":libs:broadcast-receiver-flow"))
    implementation(project(":libs:coroutine-broadcast-receiver"))

    implementation(project(":libs:beacon-parser"))

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)

    implementation(libs.kotlinx.serializationJson)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockitoKotlin)
    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)

    testImplementation(libs.androidBeaconLibrary)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testJunit)
    androidTestImplementation(libs.androidx.testEspresso)

    androidTestImplementation(platform(libs.kotlinx.coroutinesBom))
    androidTestImplementation(libs.kotlinx.coroutinesTest)
}
