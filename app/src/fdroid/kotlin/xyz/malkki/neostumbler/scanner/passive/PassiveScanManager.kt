package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.data.emitter.PassiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.utils.PermissionHelper

class PassiveScanManager(
    private val context: Context,
    private val settings: Settings,
    private val passiveScanStateManager: PassiveScanStateManager,
    private val passiveBluetoothBeaconSource: PassiveBluetoothBeaconSource,
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

        if (PermissionHelper.hasBluetoothScanPermission(context)) {
            passiveBluetoothBeaconSource.enable()
        }
    }

    suspend fun disablePassiveScanning() {
        passiveScanStateManager.reset()

        val locationManager = context.getSystemService<LocationManager>()!!

        locationManager.removeUpdates(PlatformPassiveLocationReceiver.getPendingIntent(context))

        if (PermissionHelper.hasBluetoothScanPermission(context)) {
            passiveBluetoothBeaconSource.disable()
        }
    }
}
