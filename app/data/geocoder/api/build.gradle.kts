plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":libs:geography"))

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
