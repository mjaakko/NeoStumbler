[![Build status](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml/badge.svg)](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml) [![License](https://img.shields.io/github/license/mjaakko/NeoStumbler)](./LICENSE) [![Latest release](https://img.shields.io/github/v/release/mjaakko/NeoStumbler)](https://github.com/mjaakko/NeoStumbler/releases/latest)
# NeoStumbler

NeoStumbler is an Android application for collecting locations of cell towers, Wi-Fi access points and Bluetooth beacons to [Mozilla Location Services](https://location.services.mozilla.com/)

## Downloads

APKs of the application can be downloaded from [Releases](https://github.com/mjaakko/NeoStumbler/releases) page. Currently the application is not listed in Google Play or any other app stores

## Features

* Supports new Android versions
* Collect data for cell towers, Wi-Fi access points and Bluetooth beacons
  * Currently 5G cell towers are not supported due to the lack of support by MLS
* Service for collecting data
  * Can be started automatically while moving (if Google Play Services available)
* Exporting scan data as a .zip

 ## Development

The application has two product flavors:
* `full` - includes all features
* `fdroid` - features dependent on closed-source components (i.e. Google Play Services) are removed

### Building

 * Build debug APK: `./gradlew buildFull` or `./gradlew buildFdroid`
 * Build unsigned release APK: `./gradlew buildFullRelease` or `./gradlew buildFdroidRelease`
