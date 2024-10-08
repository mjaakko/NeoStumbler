package xyz.malkki.neostumbler.geosubmit

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val timestamp: Long,
    val position: Position,
    val wifiAccessPoints: List<WifiAccessPoint>?,
    val cellTowers: List<CellTower>?,
    val bluetoothBeacons: List<BluetoothBeacon>?
) {
    @Serializable
    data class Position(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double?,
        val age: Long,
        val altitude: Double?,
        val altitudeAccuracy: Double?,
        val heading: Double?,
        val pressure: Double?,
        val speed: Double?,
        val source: String
    ) {
        companion object {
            fun fromDbEntity(positionEntity: xyz.malkki.neostumbler.db.entities.PositionEntity): Position {
                return Position(
                    positionEntity.latitude,
                    positionEntity.longitude,
                    positionEntity.accuracy?.takeUnless { it.isNaN() },
                    positionEntity.age,
                    positionEntity.altitude?.takeUnless { it.isNaN() },
                    positionEntity.altitudeAccuracy?.takeUnless { it.isNaN() },
                    positionEntity.heading?.takeUnless { it.isNaN() },
                    positionEntity.pressure?.takeUnless { it.isNaN() },
                    positionEntity.speed?.takeUnless { it.isNaN() },
                    positionEntity.source
                )
            }
        }
    }

    @Serializable
    data class WifiAccessPoint(
        val macAddress: String,
        val radioType: String?,
        val age: Long,
        val channel: Int?,
        val frequency: Int?,
        val signalStrength: Int?,
        val signalToNoiseRatio: Int?,
        val ssid: String?
    ) {
        companion object {
            fun fromDbEntity(wifiAccessPointEntity: xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity): WifiAccessPoint {
                return WifiAccessPoint(
                    wifiAccessPointEntity.macAddress,
                    wifiAccessPointEntity.radioType,
                    wifiAccessPointEntity.age,
                    wifiAccessPointEntity.channel,
                    wifiAccessPointEntity.frequency,
                    wifiAccessPointEntity.signalStrength,
                    wifiAccessPointEntity.signalToNoiseRatio,
                    wifiAccessPointEntity.ssid
                )
            }
        }
    }

    @Serializable
    data class CellTower(
        val radioType: String,
        val mobileCountryCode: Int?,
        val mobileCountryCodeStr: String?,
        val mobileNetworkCode: Int?,
        val mobileNetworkCodeStr: String?,
        val locationAreaCode: Int?,
        val cellId: Long?,
        val age: Long,
        val asu: Int?,
        val primaryScramblingCode: Int?,
        val serving: Int?,
        val signalStrength: Int?,
        val timingAdvance: Int?,
        val arfcn: Int?
    ) {
        companion object {
            fun fromDbEntity(cellTowerEntity: xyz.malkki.neostumbler.db.entities.CellTowerEntity): CellTower {
                return CellTower(
                    cellTowerEntity.radioType,
                    cellTowerEntity.mobileCountryCode?.toIntOrNull(),
                    cellTowerEntity.mobileCountryCode,
                    cellTowerEntity.mobileNetworkCode?.toIntOrNull(),
                    cellTowerEntity.mobileNetworkCode,
                    cellTowerEntity.locationAreaCode,
                    cellTowerEntity.cellId,
                    cellTowerEntity.age,
                    cellTowerEntity.asu,
                    cellTowerEntity.primaryScramblingCode,
                    cellTowerEntity.serving,
                    cellTowerEntity.signalStrength,
                    cellTowerEntity.timingAdvance,
                    cellTowerEntity.arfcn
                )
            }
        }
    }

    @Serializable
    data class BluetoothBeacon(
        val macAddress: String,
        val name: String?,
        val beaconType: Int?,
        val id1: String?,
        val id2: String?,
        val id3: String?,
        val age: Long,
        val signalStrength: Int?
    ) {
        companion object {
            fun fromDbEntity(beacon: xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity): BluetoothBeacon {
                return BluetoothBeacon(
                    macAddress = beacon.macAddress,
                    name = beacon.name,
                    beaconType = beacon.beaconType,
                    id1 = beacon.id1,
                    id2 = beacon.id2,
                    id3 = beacon.id3,
                    age = beacon.age,
                    signalStrength = beacon.signalStrength
                )
            }
        }
    }
}
