package xyz.malkki.neostumbler.domain

import android.location.Location
import android.os.SystemClock
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import xyz.malkki.neostumbler.extensions.elapsedRealtimeMillisCompat
import xyz.malkki.neostumbler.geography.LatLng

data class Position(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val altitudeAccuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val pressure: Double? = null,
    val source: Source,
    /** Timestamp in milliseconds since boot */
    val timestamp: Long,
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    companion object {
        /*
         * On some devices, elapsed realtime millis is broken and returns a value that would be a long time in the past.
         * If the timestamp is more than 30 seconds away from the current time,
         * then we assume the value is broken and get the timestamp from wall clock time instead
         */
        private val MAX_TIMESTAMP_DRIFT = 30.seconds

        fun fromLocation(
            location: Location,
            source: Source,
            airPressure: Double? = null,
        ): Position {
            val timestamp =
                if (
                    abs(location.elapsedRealtimeMillisCompat - SystemClock.elapsedRealtime())
                        .milliseconds >= MAX_TIMESTAMP_DRIFT
                ) {
                    SystemClock.elapsedRealtime() - (System.currentTimeMillis() - location.time)
                } else {
                    location.elapsedRealtimeMillisCompat
                }

            return Position(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy =
                    location.accuracy
                        .takeIf { location.hasAccuracy() && it.isFinite() }
                        ?.toDouble(),
                altitude = location.altitude.takeIf { location.hasAltitude() && it.isFinite() },
                altitudeAccuracy =
                    location.verticalAccuracyMeters
                        .takeIf { location.hasVerticalAccuracy() && it.isFinite() }
                        ?.toDouble(),
                heading =
                    location.bearing.takeIf { location.hasBearing() && it.isFinite() }?.toDouble(),
                speed = location.speed.takeIf { location.hasSpeed() && it.isFinite() }?.toDouble(),
                pressure = airPressure,
                source = source,
                timestamp = timestamp,
            )
        }
    }

    enum class Source {
        /** Position received from a GNSS system (e.g. GPS) */
        GPS,
        /** Position received from a network location provider */
        NETWORK,
        /** Position received from a fused location provider (e.g. Google Play Services) */
        FUSED,
        /** Position with manually specified location */
        MANUAL,
    }
}
