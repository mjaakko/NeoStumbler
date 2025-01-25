package xyz.malkki.neostumbler.scanner.speed

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.extensions.elapsedRealtimeMillisCompat
import xyz.malkki.neostumbler.extensions.pairwise
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

//Maximum age difference between two consecutive locations
private val LOCATION_MAX_AGE_DIFF = 5.seconds

//Smoothening factor, i.e. how much to weight previous speed vs. current speed
private const val A = 0.15
private const val B = 1 - A

/**
 * Calculates smoothened speed by using data from two consecutive locations
 */
class SmoothenedGpsSpeedSource(private val locationFlow: Flow<Location>) : SpeedSource {
    override fun getSpeedFlow(): Flow<Double> {
        return locationFlow
            .pairwise()
            .map { (prevLocation, currentLocation) ->
                if (currentLocation.hasSpeed()) {
                    //Smoothen the speed value by using data from two consecutive locations
                    if (prevLocation.hasSpeed() && abs(prevLocation.elapsedRealtimeMillisCompat - currentLocation.elapsedRealtimeMillisCompat).milliseconds <= LOCATION_MAX_AGE_DIFF) {
                        prevLocation.speed * A + currentLocation.speed * B
                    } else {
                        currentLocation.speed.toDouble()
                    }
                } else {
                    0.0
                }
            }
    }
}