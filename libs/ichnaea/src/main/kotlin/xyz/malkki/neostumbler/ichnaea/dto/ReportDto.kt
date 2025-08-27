package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportDto(
    val timestamp: Long,
    val username: String,
    val email: String,
    val position: PositionDto,
    val wifiAccessPoints: List<WifiAccessPointDto>?,
    val cellTowers: List<CellTowerDto>?,
    val bluetoothBeacons: List<BluetoothBeaconDto>?,
) {
    @Serializable
    data class PositionDto(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double?,
        val age: Long,
        val altitude: Double?,
        val altitudeAccuracy: Double?,
        val heading: Double?,
        val pressure: Double?,
        val speed: Double?,
        val source: String,
    )
}
