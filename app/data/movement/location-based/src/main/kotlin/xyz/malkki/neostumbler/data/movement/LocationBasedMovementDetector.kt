package xyz.malkki.neostumbler.data.movement

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningFold
import xyz.malkki.neostumbler.data.location.LocationSourceProvider
import xyz.malkki.neostumbler.geography.LatLng

// Distance between coordinates in metres
private const val HORIZONTAL_DIFFERENCE_THRESHOLD = 10.0

// Distance in altitude in metres
private const val VERTICAL_DIFFERENCE_THRESHOLD = 25.0

private val LOCATION_INTERVAL = 3.seconds

/**
 * @property notMovingDelay Delay before emitting false (i.e. not moving)
 *
 * Determines whether the device is moving by checking the difference in coordinates and altitude
 */
class LocationBasedMovementDetector(
    private val notMovingDelay: Duration = 45.seconds,
    private val locationSourceProvider: LocationSourceProvider,
) : MovementDetector {
    override fun getIsMovingFlow(): Flow<Boolean> {
        return flow { emit(locationSourceProvider.getLocationSource()) }
            .flatMapLatest { it.getLocations(LOCATION_INTERVAL, usePassiveProvider = false) }
            .map { LatLngAlt(latLng = it.position.latLng, altitude = it.position.altitude) }
            .runningFold<LatLngAlt, Pair<LatLngAlt?, Boolean>>(null to true) {
                (oldLocation, _),
                newLocation ->
                val isMoving =
                    when {
                        oldLocation == null ||
                            oldLocation.horizontalLocationChangedSignificantly(newLocation) ||
                            oldLocation.verticalLocationChangedSignificantly(newLocation) -> {
                            true
                        }
                        else -> {
                            false
                        }
                    }

                val location =
                    if (isMoving) {
                        if (newLocation.altitude == null) {
                            newLocation.copy(altitude = oldLocation?.altitude)
                        } else {
                            newLocation
                        }
                    } else {
                        oldLocation
                    }

                location to isMoving
            }
            .map { it.second }
            .distinctUntilChanged()
            .mapLatest { isMoving ->
                if (!isMoving) {
                    // When movement stops, notify about it with a small delay - otherwise it
                    // wouldn't be possible to collect data from a single location
                    delay(notMovingDelay)
                }

                isMoving
            }
    }
}

private data class LatLngAlt(val latLng: LatLng, val altitude: Double?)

private fun LatLngAlt.horizontalLocationChangedSignificantly(newLocation: LatLngAlt): Boolean {
    return latLng.distanceTo(newLocation.latLng) >= HORIZONTAL_DIFFERENCE_THRESHOLD
}

private fun LatLngAlt.verticalLocationChangedSignificantly(newLocation: LatLngAlt): Boolean {
    val oldAltitude = altitude ?: 0.0
    val newAltitude =
        newLocation.altitude
            // If the new location does not have altitude info, assume it hasn't changed
            ?: oldAltitude

    return abs(oldAltitude - newAltitude) >= VERTICAL_DIFFERENCE_THRESHOLD
}
