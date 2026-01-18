package xyz.malkki.neostumbler.data.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlin.time.Duration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.executors.ImmediateExecutor
import xyz.malkki.neostumbler.mapper.toPositionObservation

class PlatformLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getLocations(
        interval: Duration,
        usePassiveProvider: Boolean,
    ): Flow<PositionObservation> = callbackFlow {
        val locationManager = appContext.getSystemService<LocationManager>()!!

        val locationListener =
            object : LocationListenerCompat {
                override fun onLocationChanged(location: Location) {
                    trySendBlocking(
                        location.toPositionObservation(
                            source = location.provider.locationProviderToPositionSource()
                        )
                    )
                }

                override fun onProviderDisabled(provider: String) {
                    Timber.w("Location provider $provider disabled")
                }

                override fun onProviderEnabled(provider: String) {
                    Timber.i("Location provider $provider enabled")
                }
            }

        val locationIntervalMillis = interval.inWholeMilliseconds

        val provider =
            if (usePassiveProvider) {
                LocationManager.PASSIVE_PROVIDER
            } else {
                LocationManager.GPS_PROVIDER
            }

        locationManager
            .getLastKnownLocation(provider)
            ?.toPositionObservation(source = provider.locationProviderToPositionSource())
            ?.let { send(it) }

        val locationRequest =
            LocationRequestCompat.Builder(locationIntervalMillis)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(locationIntervalMillis)
                .setMaxUpdateDelayMillis(0)
                .setMinUpdateDistanceMeters(0.0f)
                .setMaxUpdates(Int.MAX_VALUE)
                .setDurationMillis(Long.MAX_VALUE)
                .build()

        LocationManagerCompat.requestLocationUpdates(
            locationManager,
            provider,
            locationRequest,
            ImmediateExecutor,
            locationListener,
        )

        awaitClose { LocationManagerCompat.removeUpdates(locationManager, locationListener) }
    }

    private fun String?.locationProviderToPositionSource(): Position.Source {
        return when (this) {
            LocationManager.NETWORK_PROVIDER -> Position.Source.NETWORK
            LocationManager.GPS_PROVIDER -> Position.Source.GPS
            else -> Position.Source.FUSED
        }
    }
}
