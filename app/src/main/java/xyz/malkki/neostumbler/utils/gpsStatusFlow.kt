package xyz.malkki.neostumbler.utils

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationManagerCompat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import xyz.malkki.neostumbler.extensions.maxAge

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
fun getGpsStatsFlow(context: Context): Flow<GpsStats?> =
    callbackFlow {
            val locationManager = context.applicationContext.getSystemService<LocationManager>()!!

            val callback =
                object : GnssStatusCompat.Callback() {
                    override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
                        val usedInFixCount =
                            (0 until status.satelliteCount).count { satelliteIndex ->
                                status.usedInFix(satelliteIndex)
                            }

                        trySendBlocking(
                            GpsStats(
                                satellitesUsedInFix = usedInFixCount,
                                satellitesTotal = status.satelliteCount,
                            )
                        )
                    }

                    override fun onStopped() {
                        // Set status to null when GNSS stops
                        trySendBlocking(null)
                    }
                }

            LocationManagerCompat.registerGnssStatusCallback(
                locationManager,
                ImmediateExecutor,
                callback,
            )

            awaitClose {
                LocationManagerCompat.unregisterGnssStatusCallback(locationManager, callback)
            }
        }
        .maxAge(30.seconds)

data class GpsStats(val satellitesUsedInFix: Int, val satellitesTotal: Int)
