package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class GeolocateRequestDto(
    val considerIp: Boolean,
    val bluetoothBeacons: List<BluetoothBeaconDto>,
    val cellTowers: List<CellTowerDto>,
    val wifiAccessPoints: List<WifiAccessPointDto>,
)
