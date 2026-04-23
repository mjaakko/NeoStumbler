plugins { id("convention.kotlin-library") }

dependencies {
    implementation(libs.fastcsv)
    implementation(project(":libs:utils"))

    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)

    api(project(":app:data:reports:api"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutinesTest)
}
