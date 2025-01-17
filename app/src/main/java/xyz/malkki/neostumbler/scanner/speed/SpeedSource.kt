package xyz.malkki.neostumbler.scanner.speed

import kotlinx.coroutines.flow.Flow

fun interface SpeedSource {
    /**
     * @return Flow emitting the speed in meters per second
     */
    fun getSpeedFlow(): Flow<Double>
}