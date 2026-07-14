---
title: Geosubmit format
order: 2
---


NeoStumbler includes a few additions in its Geosubmit requests compared to the
standard [Ichnaea Geosubmit v2 format](https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html).
The format is backward compatible with Geosubmit v2, but if you are running a custom server, make
sure it can handle unknown values in its input.

## Extra fields

Extra fields that are not present in the Geosubmit format

### Cell towers

| Field                  | Type    | Description                            |
|------------------------|---------|----------------------------------------|
| `mobileCountryCodeStr` | String  | MCC as a string                        |
| `mobileNetworkCodeStr` | String  | MNC as a string                        |
| `arfcn`                | Integer | Cell tower ARFCN/UARFCN/EARFCN/NRARFCN |

### Bluetooth beacons

| Field        | Type    | Description                                                                             |
|--------------|---------|:----------------------------------------------------------------------------------------|
| `beaconType` | Integer | Type of the beacon extracted from the BLE advertisement packet, e.g. `533` for iBeacons |
| `id1`        | String  | First identifier of the beacon                                                          |
| `id2`        | String  | Second identifier of the beacon                                                         |
| `id3`        | String  | Third identifier of the beacon                                                          |

### Wi-Fi access points

| Field               | Type                                      | Optional | Description                                            |
|---------------------|-------------------------------------------|----------|--------------------------------------------------------|
| `estimatedDistance` | [Estimated distance](#estimated-distance) | Yes      | Distance measured to the access point using Wi-Fi RTT. |

## Extra values

Extra values that are not present in the Geosubmit format

### Cell towers

| Field       | Extra values            |
|-------------|-------------------------|
| `radioType` | `nr` for 5G cell towers |

### Wi-Fi access points

| Field       | Extra values                    |
|-------------|---------------------------------|
| `radioType` | `802.11ax` for Wi-Fi 6 networks |

## Extra types

### Estimated distance

This type is used for estimated distances to the radio emitters. The distance can be estimated with a technology such
as [Wi-Fi RTT](https://en.wikipedia.org/wiki/IEEE_802.11mc).

| Field      | Type   | Optional | Description                                                     |
|------------|--------|----------|-----------------------------------------------------------------|
| `distance` | Number | No       | Estimated distance to the radio emitter in meters.              |
| `accuracy` | Number | Yes      | Accuracy of the estimated distance in meters.                   |
| `type`     | String | Yes      | Type of distance estimation. Either `ONE_SIDED` or `TWO_SIDED`. |

Note that for one-sided distance measurements, the reported distance can be incorrect by multiple orders of magnitude.
In such cases, the estimated distance should only be used with another one-sided
measurement to the same radio emitter to calculate the relative distance.
