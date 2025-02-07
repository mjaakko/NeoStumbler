package xyz.malkki.neostumbler.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlin.time.Duration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.utils.ImmediateExecutor

class PlatformLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getLocations(interval: Duration): Flow<Position> = callbackFlow {
        val locationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener =
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySendBlocking(Position.fromLocation(location, "gps"))
                }

                override fun onProviderDisabled(provider: String) {
                    Timber.w("Location provider $provider disabled")
                }

                override fun onProviderEnabled(provider: String) {
                    Timber.i("Location provider $provider enabled")
                }
            }

        val locationIntervalMillis = interval.inWholeMilliseconds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val locationRequest =
                LocationRequest.Builder(locationIntervalMillis)
                    .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                    .setIntervalMillis(locationIntervalMillis)
                    .setMinUpdateDistanceMeters(0.0f)
                    .build()

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                locationRequest,
                ImmediateExecutor,
                locationListener,
            )
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                locationIntervalMillis,
                0.0f,
                locationListener,
                Looper.getMainLooper(),
            )
        }

        awaitClose { locationManager.removeUpdates(locationListener) }
    }
}
