package xyz.malkki.neostumbler.db.entities

import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.core.CellTower

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
data class CellTowerEntity(
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
            cellTower: CellTower,
            reportTimestamp: Instant,
            reportId: Long,
        ): CellTowerEntity {
            // Report time is truncated to seconds -> age can be negative by some milliseconds
            val age =
                maxOf(
                    0,
                    Instant.now()
                        .minusMillis(SystemClock.elapsedRealtime() - cellTower.timestamp)
                        .until(reportTimestamp, ChronoUnit.MILLIS),
                )

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
