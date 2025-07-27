plugins {
    id("convention.android-library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom)
}

private val DB_SCHEMAS_DIR = "$projectDir/schemas"

android {
    namespace = "xyz.malkki.neostumbler.reports.room"

    room { schemaDirectory(DB_SCHEMAS_DIR) }
}

dependencies {
    api(project(":app:data:reports:api"))

    implementation(libs.androidx.roomRuntime)
    annotationProcessor(libs.androidx.roomCompiler)
    ksp(libs.androidx.roomCompiler)
    implementation(libs.androidx.roomKtx)
    implementation(libs.androidx.roomPaging)
    androidTestImplementation(libs.androidx.roomTesting)

    implementation(project(":libs:room-converters"))
    implementation(libs.timber)

    androidTestImplementation(libs.androidx.testJunit)
    androidTestImplementation(libs.androidx.testEspresso)

    androidTestImplementation(platform(libs.kotlinx.coroutinesBom))
    androidTestImplementation(libs.kotlinx.coroutinesTest)
}
