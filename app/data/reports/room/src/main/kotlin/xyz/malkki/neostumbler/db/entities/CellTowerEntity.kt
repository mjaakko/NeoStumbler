package xyz.malkki.neostumbler.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation

@Entity(
    foreignKeys =
        [
            ForeignKey(
                entity = Report::class,
                parentColumns = ["id"],
                childColumns = ["reportId"],
                onDelete = ForeignKey.CASCADE,
            )
        ]
)
internal data class CellTowerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val radioType: String,
    val mobileCountryCode: String?,
    val mobileNetworkCode: String?,
    val cellId: Long?,
    val locationAreaCode: Int?,
    val asu: Int?,
    val primaryScramblingCode: Int?,
    val serving: Int?,
    val signalStrength: Int?,
    val timingAdvance: Int?,
    val arfcn: Int?,
    val age: Long,
    @ColumnInfo(index = true) val reportId: Long?,
) {
    companion object {
        fun fromCellTower(
            emitterObservation: EmitterObservation<CellTower, String>,
            reportTimestamp: Long,
            reportId: Long,
        ): CellTowerEntity {
            val cellTower = emitterObservation.emitter

            // Report time is truncated to seconds -> age can be negative by some milliseconds
            val age = reportTimestamp - emitterObservation.timestamp

            return CellTowerEntity(
                id = 0,
                radioType = cellTower.radioType.name.lowercase(),
                mobileCountryCode = cellTower.mobileCountryCode!!,
                mobileNetworkCode = cellTower.mobileNetworkCode!!,
                cellId = cellTower.cellId,
                locationAreaCode = cellTower.locationAreaCode,
                asu = cellTower.asu,
                primaryScramblingCode = cellTower.primaryScramblingCode,
                serving = cellTower.serving,
                signalStrength = cellTower.signalStrength,
                timingAdvance = cellTower.timingAdvance,
                arfcn = cellTower.arfcn,
                age = age,
                reportId = reportId,
            )
        }
    }
}

internal fun CellTowerEntity.toCellTower(): CellTower {
    return CellTower(
        radioType = CellTower.RadioType.valueOf(radioType.uppercase()),
        mobileCountryCode = mobileCountryCode,
        mobileNetworkCode = mobileNetworkCode,
        cellId = cellId,
        locationAreaCode = locationAreaCode,
        asu = asu,
        primaryScramblingCode = primaryScramblingCode,
        serving = serving,
        timingAdvance = timingAdvance,
        arfcn = arfcn,
        signalStrength = signalStrength,
    )
}
