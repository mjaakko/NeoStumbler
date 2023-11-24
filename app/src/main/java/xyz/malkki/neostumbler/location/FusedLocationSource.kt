package xyz.malkki.neostumbler.location

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import xyz.malkki.neostumbler.common.LocationWithSource
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import kotlin.time.Duration

class FusedLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getLocations(interval: Duration): Flow<LocationWithSource> = callbackFlow {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)

        val locationIntervalMillis = interval.inWholeMilliseconds

        val locationRequest = LocationRequest.Builder(locationIntervalMillis)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(locationIntervalMillis / 3)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    trySendBlocking(LocationWithSource(location, LocationWithSource.LocationSource.FUSED))
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, ImmediateExecutor, locationCallback).await()

        awaitClose {
            fusedLocationProviderClient.flushLocations().addOnCompleteListener {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }
    }
}