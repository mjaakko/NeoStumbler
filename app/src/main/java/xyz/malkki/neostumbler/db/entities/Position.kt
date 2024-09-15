package xyz.malkki.neostumbler.db.entities

import android.location.Location
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity(
    foreignKeys = [ForeignKey(entity = Report::class, parentColumns = ["id"], childColumns = ["reportId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["latitude", "longitude"])]
)
data class Position(
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
    @ColumnInfo(index = true) val reportId: Long
) {
    companion object {
        fun createFromLocation(reportId: Long, currentTime: Instant, location: Location, locationSource: String): Position {
            return Position(
                null,
                location.latitude,
                location.longitude,
                if (location.hasAccuracy()) {
                    location.accuracy.toDouble().takeIf { it.isFinite() }
                } else {
                    null
                },
                //Current time is truncated to seconds -> age can be negative by some milliseconds
                maxOf(0, Instant.ofEpochMilli(location.time).until(currentTime, ChronoUnit.MILLIS)),
                if (location.hasAltitude()) {
                    location.altitude.takeIf { it.isFinite() }
                } else {
                    null
                },
                if (location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters.toDouble().takeIf { it.isFinite() }
                } else {
                    null
                },
                if (location.hasBearing()) {
                    location.bearing.toDouble().takeIf { it.isFinite() }
                } else {
                    null
                },
                null,
                if (location.hasSpeed()) {
                    location.speed.toDouble().takeIf { it.isFinite() }
                } else {
                    null
                },
                locationSource,
                reportId
            )
        }
    }
}
