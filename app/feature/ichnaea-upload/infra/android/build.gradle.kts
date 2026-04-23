plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.ichnaeaupload" }

dependencies {
    api(project(":app:feature:ichnaea-upload:infra:api"))
    api(project(":app:feature:ichnaea-upload:service"))

    implementation(project(":app:data:settings:api"))
    implementation(project(":app:data:reports:api"))
    implementation(project(":app:core:network"))

    api(libs.bundles.androidxWork)

    implementation(libs.androidx.core)

    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)

    implementation(libs.timber)
}
