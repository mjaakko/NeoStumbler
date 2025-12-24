plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":app:core:domain"))
    implementation(project(":libs:executors"))

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
