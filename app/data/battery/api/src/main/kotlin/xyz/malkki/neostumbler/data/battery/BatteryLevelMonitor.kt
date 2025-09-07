package xyz.malkki.neostumbler.data.battery

import kotlinx.coroutines.flow.Flow

interface BatteryLevelMonitor {
    fun getBatteryLevelFlow(): Flow<Float>
}
