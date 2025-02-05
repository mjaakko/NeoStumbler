package xyz.malkki.neostumbler.domain

import android.location.Location
import xyz.malkki.neostumbler.extensions.elapsedRealtimeMillisCompat

data class Position(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val altitudeAccuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val pressure: Double? = null,
    val source: String,
    /**
     * Timestamp in milliseconds since boot
     */
    val timestamp: Long
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

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
