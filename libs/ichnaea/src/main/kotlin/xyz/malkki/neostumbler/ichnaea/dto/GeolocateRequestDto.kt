package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class GeolocateRequestDto(
    val considerIp: Boolean,
    val bluetoothBeacons: List<BluetoothBeaconDto>,
    val cellTowers: List<GeolocateRequestDto.CellTowerDto>,
    val wifiAccessPoints: List<WifiAccessPointDto>,
) {
    /**
     * This is slightly different than the DTO used in submissions
     *
     * See https://ichnaea.readthedocs.io/en/latest/api/geolocate.html#cell-tower-fields
     */
    @Serializable
    data class CellTowerDto(
        val radioType: String,
        val mobileCountryCode: Int? = null,
        val mobileNetworkCode: Int? = null,
        val locationAreaCode: Int? = null,
        val cellId: Long? = null,
        val age: Long? = null,
        val signalStrength: Int? = null,
        val psc: Int? = null,
        val timingAdvance: Int? = null,
    )
}
