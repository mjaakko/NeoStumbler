package xyz.malkki.neostumbler.domain

import android.location.Location
import xyz.malkki.neostumbler.extensions.elapsedRealtimeMillisCompat

data class Position(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val altitude: Double?,
    val altitudeAccuracy: Double?,
    val heading: Double?,
    val speed: Double?,
    val pressure: Double?,
    val source: String,
    val timestamp: Long
) {
    companion object {
        fun fromLocation(location: Location, source: String, airPressure: Double? = null): Position {
            return Position(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.takeIf { location.hasAccuracy() && it.isFinite() }?.toDouble(),
                altitude = location.altitude.takeIf { location.hasAltitude() && it.isFinite() },
                altitudeAccuracy = location.verticalAccuracyMeters.takeIf { location.hasVerticalAccuracy() && it.isFinite() }?.toDouble(),
                heading = location.bearing.takeIf { location.hasBearing() && it.isFinite() }?.toDouble(),
                speed = location.speed.takeIf { location.hasSpeed() && it.isFinite() }?.toDouble(),
                pressure = airPressure,
                source = source,
                timestamp = location.elapsedRealtimeMillisCompat
            )
        }
    }
}
