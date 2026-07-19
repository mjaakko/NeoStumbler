package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.values.SignalStrength

data class BluetoothBeacon(
    val macAddress: MacAddress,
    val beaconType: Int? = null,
    val id1: String? = null,
    val id2: String? = null,
    val id3: String? = null,
    override val signalStrength: SignalStrength,
) : Emitter<MacAddress> {
    override val uniqueKey: MacAddress
        get() = macAddress
}
