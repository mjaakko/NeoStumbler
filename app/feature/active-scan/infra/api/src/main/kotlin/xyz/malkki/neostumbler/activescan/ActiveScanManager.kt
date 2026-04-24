package xyz.malkki.neostumbler.activescan

import kotlinx.coroutines.flow.StateFlow
import xyz.malkki.neostumbler.data.location.GpsStatus

interface ActiveScanManager {
    /** Whether the scanner service is running */
    val serviceRunning: StateFlow<Boolean>

    /**
     * Number of reports created during the current scanning session. 0 if the service is not
     * running
     */
    val reportsCreated: StateFlow<Int>

    /** Current GPS status. `null` when the service is not running or GPS status unknown */
    val gpsStatus: StateFlow<GpsStatus?>

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
