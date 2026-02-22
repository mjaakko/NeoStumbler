package xyz.malkki.neostumbler.activescan

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import xyz.malkki.neostumbler.data.location.GpsStatus

interface ActiveScanManager {
    val scanningActive: StateFlow<Boolean>
    val reportsCreated: Flow<Int>
    val gpsStatus: Flow<GpsStatus?>

    /**
     * @param autostart Set to true if this action is done from the background without user
     *   interaction
     */
    fun startScanning(autostart: Boolean = false)

    /**
     * @param autostart Set to true if this action is done from the background without user
     *   interaction
     */
    fun stopScanning(autostart: Boolean = false)
}
