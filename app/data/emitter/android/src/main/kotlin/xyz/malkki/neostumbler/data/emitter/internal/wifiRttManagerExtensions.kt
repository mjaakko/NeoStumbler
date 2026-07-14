package xyz.malkki.neostumbler.data.emitter.internal

import android.Manifest
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import androidx.annotation.RequiresPermission
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import xyz.malkki.neostumbler.executors.ImmediateExecutor

@RequiresPermission(
    allOf =
        [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
        ]
)
internal suspend fun WifiRttManager.doRanging(rangingRequest: RangingRequest): List<RangingResult> {
    return suspendCoroutine { continuation ->
        startRanging(
            rangingRequest,
            ImmediateExecutor,
            object : RangingResultCallback() {
                override fun onRangingFailure(errorCode: Int) {
                    continuation.resumeWithException(WifiRttException(errorCode))
                }

                override fun onRangingResults(results: List<RangingResult>) {
                    continuation.resume(results)
                }
            },
        )
    }
}

internal class WifiRttException(val errorCode: Int) :
    Exception("Wi-Fi RTT ranging failed: $errorCode")
