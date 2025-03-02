package xyz.malkki.neostumbler.db.entities

import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.domain.LatLng
import xyz.malkki.neostumbler.domain.Position

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
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long?,
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
        fun createFromPosition(
            reportId: Long,
            reportTimestamp: Instant,
            position: Position,
        ): PositionEntity {
            // Report time is truncated to seconds -> age can be negative by some milliseconds
            val age =
                maxOf(
                    0,
                    Instant.now()
                        .minusMillis(SystemClock.elapsedRealtime() - (position.timestamp))
                        .until(reportTimestamp, ChronoUnit.MILLIS),
                )

            return PositionEntity(
                null,
                latitude = position.latitude,
                longitude = position.longitude,
                accuracy = position.accuracy,
                age = age,
                altitude = position.altitude,
                altitudeAccuracy = position.altitudeAccuracy,
                heading = position.heading,
                pressure = position.pressure,
                speed = position.speed,
                source = position.source,
                reportId = reportId,
            )
        }
    }
}

val PositionEntity.latLng: LatLng
    get() = LatLng(latitude, longitude)
