plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.coroutinebroadcastreceiver" }

dependencies {
    implementation(libs.timber)

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.mockitoKotlin)
}
