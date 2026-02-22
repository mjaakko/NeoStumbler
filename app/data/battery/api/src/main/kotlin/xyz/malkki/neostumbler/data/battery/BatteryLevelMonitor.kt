package xyz.malkki.neostumbler.data.battery

import kotlinx.coroutines.flow.Flow

fun interface BatteryLevelMonitor {
    /** @return Flow of device battery level, from 0.0 to 1.0 */
    fun getBatteryLevelFlow(): Flow<Float>
}
