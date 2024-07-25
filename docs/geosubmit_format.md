NeoStumbler includes a few extra fields in its Geosubmit requests in addition to the fields specified in [Ichnaea's Geosubmit v2 format](https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html). The format is backward compatible with Geosubmit v2, but if you are running a custom server, make sure it can handle unknown values in its input.

## Extra fields

### Cell towers

| Field                  | Type    | Description                                                                         |
|------------------------|---------|-------------------------------------------------------------------------------------|
| `mobileCountryCodeStr` | String  | MCC as a string                                                                     |
| `mobileNetworkCodeStr` | String  | MNC as a string                                                                     |
| `arfcn`                | Integer | Cell tower ARFCN/UARFCN/EARFCN/NRARFCN                                              |

### Bluetooth beacons

| Field                  | Type    | Description                                                                             |
|------------------------|---------|-----------------------------------------------------------------------------------------|
| `beaconType`           | Integer | Type of the beacon extracted from the BLE advertisement packet, e.g. `533` for iBeacons |
| `id1`                  | String  | First identifier of the beacon                                                          |
| `id2`                  | String  | Second identifier of the beacon                                                         |
| `id3`                  | String  | Third identifier of the beacon                                                          |