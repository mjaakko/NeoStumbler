plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.export" }

dependencies {
    api(project(":app:feature:export:infra:api"))
    api(project(":app:feature:export:service"))

    api(libs.bundles.androidxWork)

    implementation(libs.androidx.core)

    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)

    implementation(libs.timber)
}
