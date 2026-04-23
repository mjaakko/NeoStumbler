package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.MacAddress

data class BluetoothBeacon(
    val macAddress: MacAddress,
    val beaconType: Int? = null,
    val id1: String? = null,
    val id2: String? = null,
    val id3: String? = null,
    override val signalStrength: Int,
) : Emitter<MacAddress> {
    override val uniqueKey: MacAddress
        get() = macAddress
}
