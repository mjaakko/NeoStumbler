plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.airpressure.android" }

dependencies {
    api(project(":app:data:airpressure:api"))

    testImplementation(libs.junit)
    testImplementation(libs.mockitoKotlin)

    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)
}
