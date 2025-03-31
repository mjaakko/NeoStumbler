package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class PassiveScanManager(
    private val context: Context,
    private val settingsStore: DataStore<Preferences>,
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

    fun disablePassiveScanning() {
        val locationManager = context.getSystemService<LocationManager>()!!

        locationManager.removeUpdates(PlatformPassiveLocationReceiver.getPendingIntent(context))
    }
}
