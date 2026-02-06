plugins { id("convention.kotlin-library") }

dependencies {
    api(libs.androidx.collection)

    testImplementation(libs.junit)
}
