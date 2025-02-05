package xyz.malkki.neostumbler.scanner.movement

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningFold
import xyz.malkki.neostumbler.domain.Position
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

//Distance between coordinates in metres
private const val HORIZONTAL_DIFFERENCE_THRESHOLD = 10.0

//Distance in altitude in metres
private const val VERTICAL_DIFFERENCE_THRESHOLD = 15.0

/**
 * @property notMovingDelay Delay before emitting false (i.e. not moving)
 *
 * Determines whether the device is moving by checking the difference in coordinates and altitude
 */
class LocationBasedMovementDetector(private val notMovingDelay: Duration = 45.seconds, private val locationFlowProvider: () -> Flow<Position>) : MovementDetector {
    override fun getIsMovingFlow(): Flow<Boolean> {
        return locationFlowProvider.invoke()
            .runningFold<Position, Pair<Position?, Boolean>>(null to true) { (oldLocation, _), newLocation ->
                if (oldLocation == null
                    || oldLocation.latLng.distanceTo(newLocation.latLng) >= HORIZONTAL_DIFFERENCE_THRESHOLD
                    || abs((oldLocation.altitude ?: 0.0) - (newLocation.altitude ?: 0.0)) >= VERTICAL_DIFFERENCE_THRESHOLD) {
                    newLocation to true
                } else {
                    oldLocation to false
                }
            }
            .map { it.second }
            .distinctUntilChanged()
            .mapLatest { isMoving ->
                if (!isMoving) {
                    //When movement stops, notify about it with a small delay - otherwise it wouldn't be possible to collect data from a single location
                    delay(notMovingDelay)
                }

                isMoving
            }
    }
}