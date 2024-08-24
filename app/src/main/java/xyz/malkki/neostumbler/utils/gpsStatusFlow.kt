package xyz.malkki.neostumbler.utils

import android.Manifest
import android.content.Context
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
fun getGpsStatsFlow(context: Context): Flow<GpsStats> = callbackFlow {
    val locationManager = context.applicationContext.getSystemService<LocationManager>()!!

    val callback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val usedInFixCount = (0 until status.satelliteCount)
                .count { satelliteIndex ->
                    status.usedInFix(satelliteIndex)
                }

            trySendBlocking(GpsStats(satellitesUsedInFix = usedInFixCount, satellitesTotal = status.satelliteCount))
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        locationManager.registerGnssStatusCallback(ImmediateExecutor, callback)
    } else {
        locationManager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
    }

    awaitClose {
        locationManager.unregisterGnssStatusCallback(callback)
    }
}

data class GpsStats(val satellitesUsedInFix: Int, val satellitesTotal: Int)