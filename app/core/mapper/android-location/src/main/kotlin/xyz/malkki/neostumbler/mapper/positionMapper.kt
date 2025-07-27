package xyz.malkki.neostumbler.mapper

import android.location.Location
import android.os.SystemClock
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.Position.Source
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.mapper.internal.elapsedRealtimeMillisCompat

/*
 * On some devices, elapsed realtime millis is broken and returns a value that would be a long time in the past.
 * If the timestamp is more than 30 seconds away from the current time,
 * then we assume the value is broken and get the timestamp from wall clock time instead
 */
private val MAX_TIMESTAMP_DRIFT = 30.seconds

fun Location.toPositionObservation(
    source: Source,
    airPressure: Double? = null,
): PositionObservation {
    val timestamp =
        if (
            abs(elapsedRealtimeMillisCompat - SystemClock.elapsedRealtime()).milliseconds >=
                MAX_TIMESTAMP_DRIFT
        ) {
            SystemClock.elapsedRealtime() - (System.currentTimeMillis() - time)
        } else {
            elapsedRealtimeMillisCompat
        }

    return PositionObservation(
        position =
            Position(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy.takeIf { hasAccuracy() && it.isFinite() }?.toDouble(),
                altitude = altitude.takeIf { hasAltitude() && it.isFinite() },
                altitudeAccuracy =
                    verticalAccuracyMeters
                        .takeIf { hasVerticalAccuracy() && it.isFinite() }
                        ?.toDouble(),
                heading = bearing.takeIf { hasBearing() && it.isFinite() }?.toDouble(),
                speed = speed.takeIf { hasSpeed() && it.isFinite() }?.toDouble(),
                pressure = airPressure,
                source = source,
            ),
        timestamp = timestamp,
    )
}
