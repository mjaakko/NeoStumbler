package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.db.entities.CellTowerEntity

@Serializable
data class CellTowerDto(
    val radioType: String,
    val mobileCountryCode: Int? = null,
    val mobileCountryCodeStr: String? = null,
    val mobileNetworkCode: Int? = null,
    val mobileNetworkCodeStr: String? = null,
    val locationAreaCode: Int? = null,
    val cellId: Long? = null,
    val age: Long? = null,
    val asu: Int? = null,
    val primaryScramblingCode: Int? = null,
    val serving: Int? = null,
    val signalStrength: Int? = null,
    val timingAdvance: Int? = null,
    val arfcn: Int? = null,
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
