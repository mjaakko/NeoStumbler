package xyz.malkki.neostumbler.activescan

import kotlinx.coroutines.flow.StateFlow
import xyz.malkki.neostumbler.data.location.GpsStatus

interface ActiveScanManager {
    /** State of the scanner service */
    val state: StateFlow<ScanState>

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

sealed interface ScanState {
    /** Whether the scanner service is running */
    val serviceActive: Boolean

    object Stopped : ScanState {
        override val serviceActive = false
    }

    /** Scanner service is running and collecting data */
    object Active : ScanState {
        override val serviceActive = true
    }

    /** Scanner service is running, but not collecting data */
    data class Paused(val reasons: Set<PauseReason>) : ScanState {
        override val serviceActive = true

        enum class PauseReason {
            NOT_MOVING,
            LOW_BATTERY,
            OVERHEAT,
        }
    }
}
