package xyz.malkki.neostumbler.data.location

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import xyz.malkki.neostumbler.broadcastreceiverflow.broadcastReceiverFlow
import xyz.malkki.neostumbler.data.location.internal.getGnssStatusFlow

class AndroidGpsStatusSource(context: Context) : GpsStatusSource {
    private val appContext = context.applicationContext

    private val locationManager = appContext.getSystemService<LocationManager>()!!

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getGpsStatusFlow(): Flow<GpsStatus?> {
        return locationManager.getGnssStatusFlow().map { maybeStatus ->
            maybeStatus?.let { status ->
                val usedInFixCount =
                    (0 until status.satelliteCount).count { satelliteIndex ->
                        status.usedInFix(satelliteIndex)
                    }

                GpsStatus(
                    satellitesUsedInFix = usedInFixCount,
                    satellitesTotal = status.satelliteCount,
                )
            }
        }
    }

    override fun isGpsAvailable(): Flow<Boolean> {
        return appContext
            .broadcastReceiverFlow(
                IntentFilter().apply { addAction(LocationManager.PROVIDERS_CHANGED_ACTION) }
            )
            .map {}
            .onStart { emit(Unit) }
            .map {
                LocationManagerCompat.hasProvider(locationManager, LocationManager.GPS_PROVIDER)
            }
            .distinctUntilChanged()
    }
}
