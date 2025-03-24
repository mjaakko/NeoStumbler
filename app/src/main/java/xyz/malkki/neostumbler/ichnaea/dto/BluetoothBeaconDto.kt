package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity

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
) {
    companion object {
        fun fromDbEntity(beacon: BluetoothBeaconEntity): BluetoothBeaconDto {
            return BluetoothBeaconDto(
                macAddress = beacon.macAddress,
                name = beacon.name,
                beaconType = beacon.beaconType,
                id1 = beacon.id1,
                id2 = beacon.id2,
                id3 = beacon.id3,
                age = beacon.age,
                signalStrength = beacon.signalStrength,
            )
        }
    }
}
