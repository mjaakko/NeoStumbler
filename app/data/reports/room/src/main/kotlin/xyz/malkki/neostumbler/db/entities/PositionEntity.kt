package xyz.malkki.neostumbler.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportPosition

@Entity(
    foreignKeys =
        [
            ForeignKey(
                entity = Report::class,
                parentColumns = ["id"],
                childColumns = ["reportId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["latitude", "longitude"])],
)
internal data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    @ColumnInfo(index = true) val reportId: Long,
) {
    companion object {
        fun createFromPositionObservation(
            reportId: Long,
            reportTimestamp: Long,
            positionObservation: PositionObservation,
        ): PositionEntity {
            val age = reportTimestamp - positionObservation.timestamp

            return PositionEntity(
                id = 0,
                latitude = positionObservation.position.latitude,
                longitude = positionObservation.position.longitude,
                accuracy = positionObservation.position.accuracy,
                age = age,
                altitude = positionObservation.position.altitude,
                altitudeAccuracy = positionObservation.position.altitudeAccuracy,
                heading = positionObservation.position.heading,
                pressure = positionObservation.position.pressure,
                speed = positionObservation.position.speed,
                reportId = reportId,
                source = positionObservation.position.source.name.lowercase(),
            )
        }
    }
}

internal fun PositionEntity.toReportPosition(): ReportPosition {
    return ReportPosition(
        position =
            Position(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                altitude = altitude,
                altitudeAccuracy = altitudeAccuracy,
                heading = heading,
                speed = speed,
                pressure = pressure,
                source = Position.Source.valueOf(source.uppercase()),
            ),
        age = age,
    )
}
