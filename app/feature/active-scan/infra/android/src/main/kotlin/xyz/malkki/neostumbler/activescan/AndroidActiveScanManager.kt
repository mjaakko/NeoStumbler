package xyz.malkki.neostumbler.activescan

import android.content.Context
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow
import xyz.malkki.neostumbler.data.location.GpsStatus

class AndroidActiveScanManager(context: Context) : ActiveScanManager {
    private val appContext: Context = context.applicationContext

    override val state: StateFlow<ScanState>
        get() = ActiveScanService.scanState

    override val reportsCreated: StateFlow<Int>
        get() = ActiveScanService.reportsCreated

    override val gpsStatus: StateFlow<GpsStatus?>
        get() = ActiveScanService.gpsStatus

    override fun startScanning(autostart: Boolean) {
        ContextCompat.startForegroundService(
            appContext,
            ActiveScanService.startIntent(appContext, autostart),
        )
    }

    override fun stopScanning(autostart: Boolean) {
        if (state.value !is ScanState.Stopped) {
            appContext.startService(ActiveScanService.stopIntent(appContext, autostart))
        }
    }
}
