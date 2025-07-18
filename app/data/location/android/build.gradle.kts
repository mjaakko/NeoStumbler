plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.location.android" }

dependencies {
    api(project(":app:data:location:api"))

    implementation(project(":libs:executors"))
    implementation(project(":app:core:mapper:android-location"))

    implementation(libs.androidx.core)
    implementation(libs.timber)
}
