package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.values.SignalStrength

fun someBluetoothBeacon(
    macAddress: MacAddress = MacAddress("01:01:01:01:01:01"),
    beaconType: Int? = null,
    id1: String? = null,
    id2: String? = null,
    id3: String? = null,
    signalStrength: SignalStrength = SignalStrength(-80),
) =
    BluetoothBeacon(
        macAddress = macAddress,
        beaconType = beaconType,
        id1 = id1,
        id2 = id2,
        id3 = id3,
        signalStrength = signalStrength,
    )
