plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":app:core"))

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
