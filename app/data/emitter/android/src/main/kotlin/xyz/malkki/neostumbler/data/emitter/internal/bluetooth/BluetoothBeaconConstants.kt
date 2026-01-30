package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Companion.parseBeaconLayout
import xyz.malkki.neostumbler.beaconparser.BeaconParser

internal object BluetoothBeaconConstants {
    // These could be defined in a text file
    val BEACON_LAYOUTS =
        listOf(
                // AltBeacon
                "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25",
                // iBeacon
                "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24",
                // Eddystone-UID
                "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19",
                // RuuviTag v5
                // https://docs.ruuvi.com/communication/bluetooth-advertisements/data-format-5-rawv2
                "m:0-2=990405,i:20-25",
            )
            .map { it.parseBeaconLayout() }

    val BEACON_PARSERS = BEACON_LAYOUTS.map { BeaconParser(it) }

    /**
     * List of known beacon manufacturer IDs. These are used for scan filters when the screen is off
     * as otherwise Android does not allow active Bluetooth scanning
     */
    @Suppress("MagicNumber")
    val KNOWN_BEACON_MANUFACTURERS =
        listOf(
            // Apple
            0x04c,
            // Radius Networks
            0x0118,
            // Ruuvi
            0x0499,
        )
}
