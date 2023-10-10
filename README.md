# NeoStumbler [![Build debug APK](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml/badge.svg)](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml)

NeoStumbler is an Android application for collecting locations of cell towers, Wi-Fi access points and Bluetooth beacons to [Mozilla Location Services](https://location.services.mozilla.com/)

## Features

* Supports new Android versions
* Collect data for cell towers, Wi-Fi access points and Bluetooth beacons
  * Currently 5G cell towers are not supported due to the lack of support in MLS
* Service for collecting data
  * Can be started automatically while moving (if Google Play Services available)

 ## Development

 * Build debug APK: `./gradlew build`
