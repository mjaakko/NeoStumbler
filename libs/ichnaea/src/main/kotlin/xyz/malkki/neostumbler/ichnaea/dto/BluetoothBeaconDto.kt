package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothBeaconDto(
    val macAddress: String,
    val name: String? = null,
    val beaconType: Int? = null,
    val id1: String? = null,
    val id2: String? = null,
    val id3: String? = null,
    val age: Long? = null,
    val signalStrength: Int? = null,
)
