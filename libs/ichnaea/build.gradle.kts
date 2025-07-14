plugins {
    id("convention.kotlin-library")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(platform(libs.okhttpBom))
    api(libs.okhttp)
    implementation(libs.okhttpCoroutines)
    testImplementation(libs.okhttpMockWebServer)

    implementation(libs.kotlinx.serializationJson)

    implementation(platform(libs.kotlinx.coroutinesBom))
    implementation(libs.kotlinx.coroutinesCore)
    testImplementation(libs.kotlinx.coroutinesTest)

    implementation(project(":libs:utils"))
}
