plugins { id("convention.kotlin-library") }

dependencies {
    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)

    api(project(":app:data:settings:api"))
    api(project(":app:data:reports:api"))
    api(project(":app:core:network"))
    api(project(":libs:ichnaea"))
    implementation(project(":app:core:domain"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.mockitoKotlin)
}
