package xyz.malkki.neostumbler.location

import android.Manifest
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import xyz.malkki.neostumbler.common.LocationWithSource
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import kotlin.time.Duration

class PlatformLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getLocations(interval: Duration): Flow<LocationWithSource> = callbackFlow {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = LocationListener {
            trySendBlocking(LocationWithSource(it, LocationWithSource.LocationSource.GPS))
        }

        val locationIntervalMillis = interval.inWholeMilliseconds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val locationRequest = LocationRequest.Builder(locationIntervalMillis)
                .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                .setIntervalMillis(locationIntervalMillis)
                .setMinUpdateDistanceMeters(0.0f)
                .build()

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationRequest, ImmediateExecutor, locationListener)
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationIntervalMillis, 0.0f, locationListener, Looper.getMainLooper())
        }

        awaitClose {
            locationManager.removeUpdates(locationListener)
        }
    }
}