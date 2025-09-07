plugins { id("convention.kotlin-library") }

dependencies {
    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
