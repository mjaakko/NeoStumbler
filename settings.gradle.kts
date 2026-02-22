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

include(":app:core:domain")

include(":app:core:domain:mapper:android-location")

include(":app:core:network")

include(":app:core:scan-common")

include(":app:data:airpressure:api")

include(":app:data:airpressure:android")

include(":app:data:battery:api")

include(":app:data:battery:android")

include(":app:data:crashlog:api")

include(":app:data:crashlog:android")

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

include(":app:data:movement:api")

include(":app:data:movement:android")

include(":app:data:movement:location-based")

include(":app:feature:export:service")

include(":app:feature:export:infra:api")

include(":app:feature:export:infra:android")

include(":app:feature:ichnaea-upload:service")

include(":app:feature:ichnaea-upload:infra:api")

include(":app:feature:ichnaea-upload:infra:android")

include(":app:feature:active-scan:service")

include(":app:feature:active-scan:infra:api")

include(":app:feature:active-scan:infra:android")

include(":libs:beacon-parser")

include(":libs:room-converters")

include(":libs:geography")

include(":libs:utils")

include(":libs:ichnaea")

include(":libs:executors")

include(":libs:broadcast-receiver-flow")

include(":libs:coroutine-broadcast-receiver")

include(":libs:coroutine-service")
