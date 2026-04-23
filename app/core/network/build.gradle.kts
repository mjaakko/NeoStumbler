plugins { id("convention.kotlin-library") }

dependencies {
    api(platform(libs.okhttpBom))
    api(libs.okhttp)

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
