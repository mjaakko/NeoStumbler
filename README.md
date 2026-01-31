[![Build status](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml/badge.svg)](https://github.com/mjaakko/NeoStumbler/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/mjaakko/NeoStumbler)](./LICENSE)
[![Latest release](https://img.shields.io/github/v/release/mjaakko/NeoStumbler)](https://github.com/mjaakko/NeoStumbler/releases/latest)
[![Matrix](https://img.shields.io/matrix/neostumbler%3Amatrix.org)](https://matrix.to/#/%23neostumbler:matrix.org)
[![Translation status](https://hosted.weblate.org/widget/neostumbler/svg-badge.svg)](https://hosted.weblate.org/engage/neostumbler/)
![Downloads last month](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fgithub.com%2Fkitswas%2Ffdroid-metrics-dashboard%2Fraw%2Frefs%2Fheads%2Fmain%2Fprocessed%2Fmonthly%2Fxyz.malkki.neostumbler.fdroid.json&query=%24.total_downloads&logo=fdroid&label=Downloads%20last%20month)

<a href="https://f-droid.org/packages/xyz.malkki.neostumbler.fdroid/" style="float: right;">
  <img
    src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">
</a>

<a href="https://accrescent.app/app/xyz.malkki.neostumbler" style="float: right;">
  <img
    alt="Get it on Accrescent"
    src="https://accrescent.app/badges/get-it-on.png"
    height="80">
</a>

<a href="https://play.google.com/store/apps/details?id=xyz.malkki.neostumbler.gplay" style="float:right;">
  <img
    alt="Get it on Google Play"
    src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
    height="80">
</a>

# NeoStumbler

NeoStumbler is an Android application for collecting locations of cell towers, Wi-Fi access points
and Bluetooth beacons to geolocation services, which have an API compatible
with [Ichnaea](https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html) (i.e. Mozilla Location
Services) such as [beaconDB](https://beacondb.net/).

## Downloads

> [!TIP]
> See the [user guide](https://neostumbler.malkki.xyz/user_guide/) for more details on how to use NeoStumbler

There are three variants available:

* *fullDefault* - includes all features
* *fullGplay* - includes all features (except automatic scanning) and complies with Google Play store policies
* *fdroidDefault* - does not include closed components (i.e. Google Play Services)
    * Features missing:
        * [Fused location provider](https://developers.google.com/location-context/fused-location-provider)
        * Automatic scanning based
          on [activity recognition](https://developers.google.com/location-context/activity-recognition)
        * [Cronet](https://developer.android.com/develop/connectivity/cronet) HTTP engine

You can install NeoStumbler either from an app store or by downloading an APK
from [Releases](https://github.com/mjaakko/NeoStumbler/releases) page. Versions marked as
pre-release are release candidates for the next version of NeoStumbler. These versions should
generally be usable, but might have some bugs, UI / UX issues and missing translations.

If you install NeoStumbler directly from an APK, you need to check for updates manually or use a
tool such as Obtainium

### App stores

The *fdroid* variant is available
from [F-Droid](https://f-droid.org/packages/xyz.malkki.neostumbler.fdroid/) or alternatively
from [IzzyOnDroid](https://android.izzysoft.de/repo/apk/xyz.malkki.neostumbler.fdroid). Reproducible
builds are used for building NeoStumbler on F-Droid, which means that it's possible to upgrade an
existing installation to a newer version from an alternative source.

The *full* variant is available
from [Accrescent](https://accrescent.app/app/xyz.malkki.neostumbler). The advantage of using
Accrescent is that only the assets needed for your device are downloaded.

A modified version of the *full* variant is also available
on [Google Play](https://play.google.com/store/apps/details?id=xyz.malkki.neostumbler.gplay). This version does not
include the automatic scanning feature and contains some minor quirks to comply with Google Play policies.

## Features

* Supports latest Android versions
* Collect data for cell towers, Wi-Fi access points and Bluetooth beacons
    * Data collection can be started automatically while moving (if using the *full* variant with
      Google Play Services)
* Scanning can be automatically paused when not moving
* Map showing the areas where data has been collected
* Exporting scan data as CSV files or as a raw SQLite file

## Development

The application has three product flavors:

* `fdroidDefault` - no closed components, features dependent on closed-source components (i.e.
  Google Play Services) are not included
* `fullDefault` - includes closed components, no features missing
* `fullGplay` - based on `fullDefault`, but has some minor differences to make
  it compliant with Google Play requirements

### Building

* Build debug APK: `./gradlew :app:assembleFullDefaultDebug` or
  `./gradlew :app:assembleFdroidDefaultDebug`
* Build release APK: `./gradlew :app:assembleFullDefaultRelease` or
  `./gradlew :app:assembleFdroidDefaultRelease`
    * Note that by default this will build a signed APK
        * This needs a Java keystore file named `keystore.jks` in the project root directory and
          setting values for environment variables (see `app/build.gradle.kts`)
        * Alternatively, to build an unsigned APK, remove `signingConfigs` block from
          `app/build.gradle.kts`

## Contributing

Contributions from the community are welcome and encouraged. Easiest ways to contribute are to
create and update translations and to create bug reports. Requests for new features are welcome as
well. If you want to implement a new feature, please create an issue first if there's some design or
planning needed

### Translations

<a href="https://hosted.weblate.org/engage/neostumbler/">
<img src="https://hosted.weblate.org/widget/neostumbler/287x66-grey.png" alt="Translation status" />
</a>

[Weblate](https://hosted.weblate.org/projects/neostumbler/) is used for translations. If you want to
add translations for your language or to update existing translations, you can do that easily from
Weblate. If you prefer, you can also update translations via a PR

Note that once you've finished translating a new language, it needs to be enabled in the build
configuration.
See [this commit](https://github.com/mjaakko/NeoStumbler/commit/2c17e6f71825563fa78510b18a1d8e80596e4797)
for an example
