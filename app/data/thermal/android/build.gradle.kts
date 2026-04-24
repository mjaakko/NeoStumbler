plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.thermal.android" }

dependencies {
    api(project(":app:data:thermal:api"))

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)

    implementation(libs.androidx.core)
    implementation(libs.timber)
}
