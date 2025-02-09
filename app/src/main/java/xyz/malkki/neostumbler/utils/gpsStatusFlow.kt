package xyz.malkki.neostumbler.utils

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.extensions.getGnssStatusFlow
import xyz.malkki.neostumbler.extensions.maxAge

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
fun getGpsStatsFlow(context: Context): Flow<GpsStats?> {
    val locationManager = context.applicationContext.getSystemService<LocationManager>()!!

    return locationManager
        .getGnssStatusFlow()
        .map { maybeStatus ->
            maybeStatus?.let { status ->
                val usedInFixCount =
                    (0 until status.satelliteCount).count { satelliteIndex ->
                        status.usedInFix(satelliteIndex)
                    }

                GpsStats(
                    satellitesUsedInFix = usedInFixCount,
                    satellitesTotal = status.satelliteCount,
                )
            }
        }
        .maxAge(30.seconds)
}

data class GpsStats(val satellitesUsedInFix: Int, val satellitesTotal: Int)
