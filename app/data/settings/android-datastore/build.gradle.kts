plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.settings.datastore" }

dependencies {
    api(project(":app:data:settings:api"))

    api(libs.androidx.datastore.preferences)
}
