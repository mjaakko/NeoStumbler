plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":app:core:domain"))
    implementation(libs.androidx.collection)

    testImplementation(libs.junit)
}
