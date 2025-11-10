plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.emitters.android" }

dependencies {
    api(project(":app:data:emitter:api"))

    implementation(project(":libs:executors"))
    implementation(project(":libs:broadcast-receiver-flow"))

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)

    implementation(libs.timber)

    implementation(libs.androidBeaconLibrary)

    testImplementation(libs.junit)
    testImplementation(libs.mockitoKotlin)
    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)
}
