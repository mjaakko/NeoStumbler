plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.activescan" }

dependencies {
    api(project(":app:feature:active-scan:infra:api"))
    api(project(":app:feature:active-scan:service"))

    api(project(":libs:coroutine-service"))

    api(libs.bundles.androidxWork)

    implementation(libs.androidx.core)

    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)
    implementation(libs.koinAndroid)

    implementation(libs.timber)
}
