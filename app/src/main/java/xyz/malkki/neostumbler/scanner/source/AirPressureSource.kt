package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.AirPressureObservation
import kotlin.time.Duration

fun interface AirPressureSource {
    fun getAirPressureFlow(interval: Duration): Flow<AirPressureObservation>
}