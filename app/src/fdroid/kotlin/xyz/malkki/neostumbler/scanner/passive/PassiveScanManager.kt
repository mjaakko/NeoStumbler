package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.data.settings.Settings

class PassiveScanManager(
    private val context: Context,
    private val settings: Settings,
    private val passiveScanStateManager: PassiveScanStateManager,
) {
    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ]
    )
    fun enablePassiveScanning() {
        enablePlatformPassiveScanning(context)
    }

    suspend fun disablePassiveScanning() {
        passiveScanStateManager.reset()

        val locationManager = context.getSystemService<LocationManager>()!!

        locationManager.removeUpdates(PlatformPassiveLocationReceiver.getPendingIntent(context))
    }
}
