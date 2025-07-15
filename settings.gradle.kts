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

include(":libs:beacon-library-utils")

include(":libs:room-converters")

include(":libs:geography")

include(":libs:utils")

include(":libs:ichnaea")
