<a href="https://f-droid.org/packages/xyz.malkki.neostumbler.fdroid/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80"
    align="right">
</a>

[![Build status](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml/badge.svg)](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml) [![License](https://img.shields.io/github/license/mjaakko/NeoStumbler)](./LICENSE) [![Latest release](https://img.shields.io/github/v/release/mjaakko/NeoStumbler)](https://github.com/mjaakko/NeoStumbler/releases/latest) [![Matrix](https://img.shields.io/matrix/neostumbler%3Amatrix.org)](https://matrix.to/#/%23neostumbler:matrix.org) [![Translation status](https://hosted.weblate.org/widget/neostumbler/svg-badge.svg)](https://hosted.weblate.org/engage/neostumbler/)

# NeoStumbler

NeoStumbler is an Android application for collecting locations of cell towers, Wi-Fi access points and Bluetooth beacons to geolocation services, which have an API compatible with [Ichnaea](https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html) (i.e. Mozilla Location Services).

## Downloads

There are two variants available:
* *full* - includes all features
* *fdroid* - does not include closed components (i.e. Google Play Services)
  * Features missing:
    * [Fused location provider](https://developers.google.com/location-context/fused-location-provider)
    * Automatic scanning based on [activity recognition](https://developers.google.com/location-context/activity-recognition)
    * [Cronet](https://developer.android.com/develop/connectivity/cronet) HTTP engine

APKs of the application can be downloaded from [Releases](https://github.com/mjaakko/NeoStumbler/releases) page.

The *fdroid* variant is also available from [F-Droid](https://f-droid.org/packages/xyz.malkki.neostumbler.fdroid/) or alternatively from [IzzyOnDroid](https://android.izzysoft.de/repo/apk/xyz.malkki.neostumbler.fdroid). Reproducible builds are used for building NeoStumbler on F-Droid, which means that it's possible to upgrade an existing installation to a newer version from an alternative source.

Updates for the *full* variant have to be manually checked and downloaded from the Releases page. The *fdroid* variant can be automatically updated by the F-Droid application

## Features

* Supports latest Android versions
* Collect data for cell towers, Wi-Fi access points and Bluetooth beacons
  * Data collection can be started automatically while moving (if using the *full* variant with Google Play Services)
* Scanning can be automatically paused when not moving
* Map showing the areas where data has been collected
* Exporting scan data as CSV files or as a raw SQLite file

## Development

The application has two product flavors:
* `full` - includes all features
* `fdroid` - features dependent on closed-source components (i.e. Google Play Services) are removed

### Building

 * Build debug APK: `./gradlew buildFullDebug` or `./gradlew buildFdroidDebug`
 * Build release APK: `./gradlew buildFullRelease` or `./gradlew buildFdroidRelease`
   * Note that by default this will build a signed APK
     * This needs a Java keystore file named `keystore.jks` in the project root directory and setting values for environment variables (see `app/build.gradle`)
     * Alternatively, to build an unsigned APK, remove `signingConfigs` block from `app/build.gradle`

## Contributing

Contributions from the community are welcome and encouraged. Easiest ways to contribute are to create and update translations and to create bug reports. Requests for new features are welcome as well. If you want to implement a new feature, please create an issue first if there's some design or planning needed

### Translations

<a href="https://hosted.weblate.org/engage/neostumbler/">
<img src="https://hosted.weblate.org/widget/neostumbler/287x66-grey.png" alt="Translation status" />
</a>

[Weblate](https://hosted.weblate.org/projects/neostumbler/) is used for translations. If you want to add translations for your language or to update existing translations, you can do that easily from Weblate. If you prefer, you can also update translations via a PR
