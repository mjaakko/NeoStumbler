package xyz.malkki.neostumbler.scanner.source

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.AirPressureObservation

fun interface AirPressureSource {
    fun getAirPressureFlow(interval: Duration): Flow<AirPressureObservation>
}
