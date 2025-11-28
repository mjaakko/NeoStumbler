plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.crashlog.android" }

dependencies {
    api(project(":app:data:crashlog:api"))

    testImplementation(libs.junit)
    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)
}
