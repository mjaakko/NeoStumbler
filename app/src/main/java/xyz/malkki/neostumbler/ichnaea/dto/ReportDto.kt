package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.db.entities.PositionEntity

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
                // Ichnaea Geosubmit officially only supports these sources
                // https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html#position-fields
                val source =
                    if (positionEntity.source == "gps") {
                        "gps"
                    } else {
                        "fused"
                    }

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
                    source,
                )
            }
        }
    }
}
