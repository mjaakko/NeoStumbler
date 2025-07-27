package xyz.malkki.neostumbler.data.location

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.observation.PositionObservation

fun interface LocationSource {
    /**
     * @param interval Interval at which to receive the location updates
     * @param usePassiveProvider Whether to use a passive provider for locations (i.e. no power
     *   usage)
     */
    fun getLocations(interval: Duration, usePassiveProvider: Boolean): Flow<PositionObservation>
}
