package xyz.malkki.neostumbler.data.location

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.data.location.internal.getGnssStatusFlow

class AndroidGpsStatusSource(context: Context) : GpsStatusSource {
    private val locationManager = context.applicationContext.getSystemService<LocationManager>()!!

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
}
