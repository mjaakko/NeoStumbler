package xyz.malkki.neostumbler.activescan.internal

import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.core.observation.PositionObservation

// Maximum age difference between two consecutive locations
private val LOCATION_MAX_AGE_DIFF = 5.seconds

// Smoothening factor, i.e. how much to weight previous speed vs. current speed
private const val A = 0.15
private const val B = 1 - A

internal fun Flow<PositionObservation>.toSmoothenedSpeedFlow(): Flow<Double> {
    return pairwise().map { (prevLocation, currentLocation) ->
        if (currentLocation.position.speed != null) {
            // Smoothen the speed value by using data from two consecutive locations
            if (
                prevLocation.position.speed != null &&
                    abs(prevLocation.timestamp - currentLocation.timestamp).milliseconds <=
                        LOCATION_MAX_AGE_DIFF
            ) {
                prevLocation.position.speed!! * A + currentLocation.position.speed!! * B
            } else {
                currentLocation.position.speed!!
            }
        } else {
            0.0
        }
    }
}
