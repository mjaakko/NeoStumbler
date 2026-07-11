plugins {
    id("convention.android-library")
    alias(libs.plugins.kotlinSerialization)
}

android { namespace = "xyz.malkki.neostumbler.settings.restrictedareas" }

dependencies {
    api(project(":app:data:restricted-areas:api"))

    api(libs.androidx.datastore)

    implementation(libs.kotlinx.serializationJson)

    testImplementation(libs.junit)
    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)
}
