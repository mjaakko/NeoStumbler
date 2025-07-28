plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.beaconlibrary" }

dependencies { api(libs.androidBeaconLibrary) }
