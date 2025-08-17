package xyz.malkki.neostumbler.data.airpressure

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.airpressure.AirPressureObservation

fun interface AirPressureSource {
    fun getAirPressureFlow(interval: Duration): Flow<AirPressureObservation>
}
