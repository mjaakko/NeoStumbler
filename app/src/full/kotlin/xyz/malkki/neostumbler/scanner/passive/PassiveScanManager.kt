package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationRequest as LocationRequestFused
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.first
import timber.log.Timber
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.isGoogleApisAvailable

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
    suspend fun enablePassiveScanning() {
        disablePassiveScanning()

        val preferFusedLocationProvider =
            settings.getBooleanFlow(PreferenceKeys.PREFER_FUSED_LOCATION, true).first()

        if (context.isGoogleApisAvailable() && preferFusedLocationProvider) {
            enableFusedPassiveScanningFused()
        } else {
            enablePlatformPassiveScanning(context)
        }
    }

    suspend fun disablePassiveScanning() {
        passiveScanStateManager.reset()

        LocationServices.getFusedLocationProviderClient(context)
            .removeLocationUpdates(FusedPassiveLocationReceiver.getPendingIntent(context))

        val locationManager = context.getSystemService<LocationManager>()!!
        locationManager.removeUpdates(PlatformPassiveLocationReceiver.getPendingIntent(context))
    }

    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ]
    )
    private fun enableFusedPassiveScanningFused() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest =
            LocationRequestFused.Builder(
                    PassiveScanConstants.PASSIVE_LOCATION_INTERVAL.inWholeMilliseconds
                )
                .setPriority(Priority.PRIORITY_PASSIVE)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMinUpdateDistanceMeters(0f)
                .setMaxUpdateDelayMillis(
                    PassiveScanConstants.PASSIVE_LOCATION_MAX_DELAY.inWholeMilliseconds
                )
                .setMaxUpdateAgeMillis(
                    PassiveScanConstants.PASSIVE_LOCATION_MAX_DELAY.inWholeMilliseconds
                )
                .setMaxUpdates(Int.MAX_VALUE)
                .setDurationMillis(Long.MAX_VALUE)
                .build()

        fusedLocationProviderClient
            .requestLocationUpdates(
                locationRequest,
                FusedPassiveLocationReceiver.getPendingIntent(context),
            )
            .addOnSuccessListener {
                Timber.i("Enabled passive location updates with fused location provider")
            }
            .addOnFailureListener { ex ->
                Timber.w(
                    ex,
                    "Failed to enable passive location updates with fused location provider, using platform location provider instead",
                )

                enablePlatformPassiveScanning(context)
            }
    }
}
