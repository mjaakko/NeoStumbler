package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import timber.log.Timber

/** Enables passive scanning using Android platform features */
@RequiresPermission(
    allOf =
        [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION]
)
fun enablePlatformPassiveScanning(context: Context) {
    val appContext = context.applicationContext

    val locationManager = appContext.getSystemService<LocationManager>()!!

    val pendingIntent: PendingIntent = PlatformPassiveLocationReceiver.getPendingIntent(appContext)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val locationRequest =
            LocationRequest.Builder(LocationRequest.PASSIVE_INTERVAL)
                .setMaxUpdateDelayMillis(
                    PassiveScanConstants.PASSIVE_LOCATION_MAX_DELAY.inWholeMilliseconds
                )
                .setMinUpdateIntervalMillis(
                    PassiveScanConstants.PASSIVE_LOCATION_INTERVAL.inWholeMilliseconds
                )
                .setMaxUpdates(Int.MAX_VALUE)
                .setMinUpdateDistanceMeters(0f)
                .setDurationMillis(Long.MAX_VALUE)
                .build()

        locationManager.requestLocationUpdates(
            LocationManager.PASSIVE_PROVIDER,
            locationRequest,
            pendingIntent,
        )
    } else {
        locationManager.requestLocationUpdates(
            LocationManager.PASSIVE_PROVIDER,
            PassiveScanConstants.PASSIVE_LOCATION_INTERVAL.inWholeMilliseconds,
            0.0f,
            pendingIntent,
        )
    }

    Timber.i("Enabled passive location updates with platform location provider")
}
