package xyz.malkki.neostumbler.geosubmit.dto

import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity

@Serializable
data class ReportDto(
    val timestamp: Long,
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
    ) {
        companion object {
            fun fromDbEntity(positionEntity: PositionEntity): PositionDto {
                return PositionDto(
                    positionEntity.latitude,
                    positionEntity.longitude,
                    positionEntity.accuracy?.takeUnless { it.isNaN() },
                    positionEntity.age,
                    positionEntity.altitude?.takeUnless { it.isNaN() },
                    positionEntity.altitudeAccuracy?.takeUnless { it.isNaN() },
                    positionEntity.heading?.takeUnless { it.isNaN() },
                    positionEntity.pressure?.takeUnless { it.isNaN() },
                    positionEntity.speed?.takeUnless { it.isNaN() },
                    positionEntity.source,
                )
            }
        }
    }

    @Serializable
    data class WifiAccessPointDto(
        val macAddress: String,
        val radioType: String?,
        val age: Long,
        val channel: Int?,
        val frequency: Int?,
        val signalStrength: Int?,
        val signalToNoiseRatio: Int?,
        val ssid: String?,
    ) {
        companion object {
            fun fromDbEntity(wifiAccessPointEntity: WifiAccessPointEntity): WifiAccessPointDto {
                return WifiAccessPointDto(
                    wifiAccessPointEntity.macAddress,
                    wifiAccessPointEntity.radioType,
                    wifiAccessPointEntity.age,
                    wifiAccessPointEntity.channel,
                    wifiAccessPointEntity.frequency,
                    wifiAccessPointEntity.signalStrength,
                    wifiAccessPointEntity.signalToNoiseRatio,
                    wifiAccessPointEntity.ssid,
                )
            }
        }
    }

    @Serializable
    data class CellTowerDto(
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
        val arfcn: Int?,
    ) {
        companion object {
            fun fromDbEntity(cellTowerEntity: CellTowerEntity): CellTowerDto {
                return CellTowerDto(
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
                    cellTowerEntity.arfcn,
                )
            }
        }
    }

    @Serializable
    data class BluetoothBeaconDto(
        val macAddress: String,
        val name: String?,
        val beaconType: Int?,
        val id1: String?,
        val id2: String?,
        val id3: String?,
        val age: Long,
        val signalStrength: Int?,
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
}
