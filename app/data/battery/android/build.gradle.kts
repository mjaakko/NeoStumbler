plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.battery.android" }

dependencies {
    api(project(":app:data:battery:api"))

    implementation(project(":libs:broadcast-receiver-flow"))
}
