plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.geocoder.android" }

dependencies {
    api(project(":app:data:geocoder:api"))

    implementation(project(":libs:executors"))

    testImplementation(libs.junit)
    testImplementation(libs.mockitoKotlin)
    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)
}
