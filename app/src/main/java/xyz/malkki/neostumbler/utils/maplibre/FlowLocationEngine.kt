package xyz.malkki.neostumbler.utils.maplibre

import android.app.PendingIntent
import android.location.Location
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import xyz.malkki.neostumbler.core.observation.PositionObservation

class FlowLocationEngine(
    private val positionFlow: SharedFlow<PositionObservation>,
    private val coroutineScope: CoroutineScope,
) : LocationEngine {
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult?>) {
        positionFlow.replayCache.lastOrNull()?.let { position ->
            callback.onSuccess(LocationEngineResult.create(position.asPlatformLocation()))
        }
    }

    private val callbackToJob = ConcurrentHashMap<LocationEngineCallback<*>, Job>()

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult?>,
        looper: Looper?,
    ) {
        callbackToJob[callback] =
            coroutineScope.launch {
                positionFlow.collect { position ->
                    callback.onSuccess(LocationEngineResult.create(position.asPlatformLocation()))
                }
            }
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult?>) {
        callbackToJob.remove(callback)?.cancel()
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        pendingIntent: PendingIntent?,
    ) {
        // not needed
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        // not needed
    }
}

private fun PositionObservation.asPlatformLocation(): Location {
    return Location("manual").apply {
        this.latitude = position.latitude
        this.longitude = position.longitude
        if (position.accuracy != null) {
            this.accuracy = position.accuracy!!.toFloat()
        }
    }
}
