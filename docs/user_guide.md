# NeoStumbler 

NeoStumbler is an Android application for collecting locations of wireless transmitters, such as Wi-Fi networks and cell towers, to geolocation services compatible with the Ichnaea API (i.e. Mozilla Location Services).
The most commonly used such service is [beaconDB](https://beacondb.net/), but self-hosted Ichnaea instances and custom implementations with the same API are also usable.

## 1. Installing

There are two variants available:
* **full** - includes all features, uses closed components (i.e. Google Play Services)
* **fdroid** - does not use closed components, some features missing

The **full** variant can be downloaded from:
* [Accrescent](https://accrescent.app/app/xyz.malkki.neostumbler)
* [GitHub releases](https://github.com/mjaakko/NeoStumbler/releases)

The **fdroid** variant can be downloaded from:
* [F-Droid](https://f-droid.org/packages/xyz.malkki.neostumbler.fdroid/)
* [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/xyz.malkki.neostumbler.fdroid)
* [GitHub releases](https://github.com/mjaakko/NeoStumbler/releases)

The versions marked as *pre-release* are beta versions of the next version of NeoStumbler.

If you're downloading NeoStumbler from GitHub releases, you can use a tool such as [Obtainium](https://github.com/ImranR98/Obtainium) to automate installing new versions.

## 2. Getting started

When first opening NeoStumbler, you're asked if you want to send data to beaconDB.
If you select *yes*, beaconDB is configured as the endpoint and you're ready to start scanning.
Otherwise you need to configure an alternative endpoint from the settings

To start scanning, press the *Start* button on the bottom left of the screen.
You will be asked to grant necessary permissions for scanning and to disable battery optimizations.
For the optimal scan results, make sure to disable the battery optimizations here.

Reports containing the scan data will start appearing on the reports list after 10 seconds.
Each item on the list will show the address or the coordinate where the report was made and the number of Wi-Fi access points, cell towers and Bluetooth beacons in the report.

When you want to stop scanning, press the *Stop scanning* button to stop scanning.
When you stop scanning for the first time, you're prompted to add scanning toggle to the quick settings menu.
It's recommended to do this so that you can easily start scanning when needed.

To send the collected data, press the *Send reports* button on the top right.

## 3. Settings

In the settings, you can configure some features of NeoStumbler.

Some of the important settings to consider are listed here:
* *Send reports automatically* - if enabled, NeoStumbler will try to send reports automatically in the background every 8 hours
* *Delete sent reports* - when the sent reports are deleted. When reports are deleted, their data will also disappear from the map and the statistics.
  * Note that the database can grow to a very large size if reports are not deleted and this can cause significant performance issues, especially on a low end device
* *Movement detection* - how to detect that the device is not moving to pause scanning. This can also be disabled, but it's recommended to keep the movement detection enabled to avoid generating useless reports
* *Prefer fused location provider* - if enabled, [fused location provider](https://developers.google.com/location-context/fused-location-provider) is used instead of raw GPS. If available, fused location provider can determine the location also when indoors, but it's usually less accurate than GPS
* *Ignore Wi-Fi scan throttling* - if enabled, normal Wi-Fi scan restrictions imposed by Android are ignored.
Enabling this option is possible only if you have also disabled Wi-Fi scan throttling in the developer settings of your device. If this option is disabled, only four Wi-Fi scans are done in a two-minute period
* *Wi-Fi scan frequency* / *Cell tower scan frequency* - configures how often the scans are done. This is based on the speed that the device is moving. The selected value should only considered to be advisory. In practice scans are usually done more often unless moving at a constant speed. 
Note that there is also a minimum delay between scans, which might be noticeable when moving at a fast speed - this is 1.5 seconds for Wi-Fi scans and 5 seconds for cell scans
* *Passive data collection* - whether to create reports in the background from the data that other apps have requested
* *Filter Wi-Fis by SSID* - configures which Wi-Fi access points to exclude from the reports. Note that the filtering is case-insensitive
  * *Tip:* you can find a list of commonly moving Wi-Fis [here](https://cdn.beacondb.net/config/ssid-blacklist.txt)
* *Send reports with less metadata* - if enabled, reports are sent in a randomized order and the report timestamp will truncated to midnight with speed and direction of travel slightly rounded

In the settings, there is also an option to *Manage storage*. From here, you can see the database size, delete reports, export the data as CSV or as a raw SQLite file or import data from a raw database file

## 4. Other features

### Map

The map screen shows purple hexagons in areas where reports have been made. Darker hexagons indicate that more reports have been created inside of it.

The larger yellowish hexagons show areas where the geolocation service has coverage. This needs to be configured in the settings (*Map coverage layer*)

Source for map background tiles can be configured from the button on the top right.

### Statistics

The statistics screen shows the number of unique transmitters detected over time. 

## Improving scan performance

* Make sure that you have disabled battery optimizations for NeoStumbler as they can significantly decrease the frequency of scans and location updates
* Also check other background restriction and battery optimization settings that the device might have
  * [dontkillmyapp.com](https://dontkillmyapp.com/) can be a helpful resource for this
* Disable permission auto-reset for NeoStumbler in the device settings

## Troubleshooting

### Cannot enable automatic scanning

Automatic scanning uses Google Play Services activity recognition API to detect when the device is moving. In case you're using sandboxed Google Play Services (e.g. with GrapheneOS), you need to grant the physical activity permission for *both* NeoStumbler and Google Play Services. See [this section](https://grapheneos.org/usage#sandboxed-google-play-location-sharing) from GrapheneOS documentation for more details

### Reports are not being created / location does not work

Custom ROMs which do not use Google Play Services for the fused location provider (notably GrapheneOS) might not work with the *Prefer fused location provider* option enabled in NeoStumbler. In this case, try disabling that option or using the `fdroid` variant of NeoStumbler, which does not the ability to use fused location provider at all
