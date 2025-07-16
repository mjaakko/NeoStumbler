package xyz.malkki.neostumbler.core

data class BluetoothBeacon(
    val macAddress: String,
    val beaconType: Int? = null,
    val id1: String? = null,
    val id2: String? = null,
    val id3: String? = null,
    val signalStrength: Int? = null,
    override val timestamp: Long,
) : ObservedDevice<String> {
    override val uniqueKey: String
        get() = macAddress
}
