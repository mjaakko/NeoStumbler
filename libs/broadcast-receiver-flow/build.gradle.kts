plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.broadcastreceiverflow" }

dependencies {
    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
