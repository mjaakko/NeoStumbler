plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":app:data:location:api"))

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
