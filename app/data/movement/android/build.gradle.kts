plugins { id("convention.android-library") }

android { namespace = "xyz.malkki.neostumbler.movement.android" }

dependencies {
    api(project(":app:data:movement:api"))
    api(project(":app:data:location:api"))
}
