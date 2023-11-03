package xyz.malkki.wifiscannerformls.utils

import android.Manifest
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresPermission
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import xyz.malkki.wifiscannerformls.common.LocationWithSource
import kotlin.time.Duration
import android.location.LocationRequest as FrameworkLocationRequest
import com.google.android.gms.location.LocationRequest as GoogleLocationRequest

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
fun getLocationFlow(context: Context, locationInterval: Duration): Flow<LocationWithSource> {
    val appContext = context.applicationContext

    return if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS) {
        Timber.d("Using Google Play location provider")

        getGoogleLocationFlow(context, locationInterval.inWholeMilliseconds)
    } else {
        Timber.d("Using framework location provider")

        getFrameworkLocationFlow(context, locationInterval.inWholeMilliseconds)
    }
}

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
private fun getFrameworkLocationFlow(context: Context, locationIntervalMillis: Long): Flow<LocationWithSource> = callbackFlow {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val locationListener = LocationListener {
        trySendBlocking(LocationWithSource(it, LocationWithSource.LocationSource.GPS))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val locationRequest = FrameworkLocationRequest.Builder(locationIntervalMillis)
            .setQuality(FrameworkLocationRequest.QUALITY_HIGH_ACCURACY)
            .setIntervalMillis(locationIntervalMillis)
            .setMinUpdateDistanceMeters(0.0f)
            .build()

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationRequest, ImmediateExecutor, locationListener)
    } else {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationIntervalMillis, 0.0f, locationListener)
    }

    awaitClose {
        locationManager.removeUpdates(locationListener)
    }
}

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
private fun getGoogleLocationFlow(context: Context, locationIntervalMillis: Long): Flow<LocationWithSource> = callbackFlow {
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    val locationRequest = GoogleLocationRequest.Builder(locationIntervalMillis)
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