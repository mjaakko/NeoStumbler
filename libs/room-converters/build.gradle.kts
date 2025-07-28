plugins { id("convention.android-library") }

android {
    namespace = "xyz.malkki.neostumbler.roomconverters"

    defaultConfig { minSdk = 26 }
}

dependencies {
    api(libs.androidx.roomRuntime)

    testImplementation(libs.junit)
}
