plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":libs:geography"))

    testImplementation(libs.junit)
}
