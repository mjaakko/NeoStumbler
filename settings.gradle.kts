pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NeoStumbler"

include(":app")

include(":app:core")

include(":app:core:mapper:android-location")

include(":app:data:emitter:api")

include(":app:data:emitter:android")

include(":app:data:geocoder:api")

include(":app:data:geocoder:android")

include(":app:data:location:api")

include(":app:data:location:android")

include(":app:data:location:googleplay")

include(":app:data:settings:api")

include(":app:data:settings:android-datastore")

include(":app:data:reports:api")

include(":app:data:reports:room")

include(":libs:beacon-library-utils")

include(":libs:room-converters")

include(":libs:geography")

include(":libs:utils")

include(":libs:ichnaea")

include(":libs:executors")

include(":libs:broadcast-receiver-flow")
