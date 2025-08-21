package xyz.malkki.neostumbler.data.location.internal

import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import xyz.malkki.neostumbler.executors.ImmediateExecutor

@RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
internal fun LocationManager.getGnssStatusFlow(): Flow<GnssStatusCompat?> = callbackFlow {
    val callback =
        object : GnssStatusCompat.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
                trySendBlocking(status)
            }

            override fun onStopped() {
                // Set status to null when GNSS stops
                trySendBlocking(null)
            }
        }

    LocationManagerCompat.registerGnssStatusCallback(
        this@getGnssStatusFlow,
        ImmediateExecutor,
        callback,
    )

    awaitClose {
        LocationManagerCompat.unregisterGnssStatusCallback(this@getGnssStatusFlow, callback)
    }
}
