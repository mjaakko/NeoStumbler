package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.malkki.neostumbler.broadcastreceiverflow.broadcastReceiverFlow
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.util.RateLimiter
import xyz.malkki.neostumbler.data.emitter.internal.util.delayWithMinDuration
import xyz.malkki.neostumbler.data.emitter.mapper.toWifiAccessPoint
import xyz.malkki.neostumbler.data.emitter.ranging.rangeDistances
import xyz.malkki.neostumbler.executors.ImmediateExecutor

// https://developer.android.com/develop/connectivity/wifi/wifi-scan#wifi-scan-throttling
private val ANDROID_WIFI_SCAN_THROTTLE_PERIOD = 2.minutes
private const val ANDROID_WIFI_SCAN_THROTTLE_COUNT = 4

private val MAX_INTERVAL = 1.minutes

/**
 * How much "burstiness" to allow when scanning is throttled. If 0, no scan bursts are allowed and
 * Wi-Fi scans are done at most every 30 seconds. If 1, all allowed scans can be done in
 * immediately, but then there will be a two-minute delay until we can do more scans.
 */
private const val THROTTLED_SCAN_BURST_FACTOR = 0.25

// Minimum scan interval that can be used when Wi-Fi scan throttling is active.
// This is slightly lower than the throttle period divided by number of scans to allow for bursts
private val MIN_INTERVAL_THROTTLED: Duration =
    (ANDROID_WIFI_SCAN_THROTTLE_PERIOD / ANDROID_WIFI_SCAN_THROTTLE_COUNT) *
        (1 - THROTTLED_SCAN_BURST_FACTOR)

// Minimum scan interval when Wi-Fi scanning is not throttled
private val MIN_INTERVAL_UNTHROTTLED = 1.5.seconds

class WifiManagerActiveWifiAccessPointSource(
    context: Context,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime,
) : ActiveWifiAccessPointSource {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService<WifiManager>()!!
    private val wifiRttManager: WifiRttManager? = appContext.getSystemService<WifiRttManager>()

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE]
    )
    override fun getWifiAccessPointFlow(
        scanThrottled: Boolean,
        scanInterval: Flow<Duration>,
        rangingMode: ActiveWifiAccessPointSource.RangingMode,
    ): Flow<List<EmitterObservation<WifiAccessPoint, MacAddress>>> = channelFlow {
        val rateLimiter =
            if (scanThrottled) {
                RateLimiter(
                    ANDROID_WIFI_SCAN_THROTTLE_COUNT,
                    ANDROID_WIFI_SCAN_THROTTLE_PERIOD,
                    timeSource,
                )
            } else {
                null
            }

        val minScanInterval =
            if (scanThrottled) {
                MIN_INTERVAL_THROTTLED
            } else {
                MIN_INTERVAL_UNTHROTTLED
            }

        suspend fun doWifiScan() {
            Timber.d("Starting Wi-Fi scan")

            if (rateLimiter != null) {
                rateLimiter.doRateLimited { @Suppress("DEPRECATION") wifiManager.startScan() }
            } else {
                @Suppress("DEPRECATION") wifiManager.startScan()
            }
        }

        launch(Dispatchers.Default) {
            val scanInterval =
                scanInterval
                    .map {
                        it.coerceIn(minimumValue = minScanInterval, maximumValue = MAX_INTERVAL)
                    }
                    .stateIn(this, started = SharingStarted.Eagerly, initialValue = MAX_INTERVAL)

            while (true) {
                doWifiScan()

                val scannedAt = timeSource.invoke()
                delayWithMinDuration(scannedAt, timeSource, scanInterval)
            }
        }

        val scanResultFlow =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWifiScanFlowR(wifiManager)
            } else {
                getWifiScanFlowLegacy(appContext, wifiManager)
            }

        scanResultFlow
            .map { scanResults ->
                val rangingResultsByMacAddress = scanResults.getRangingResults(rangingMode)

                scanResults.map { scanResult ->
                    scanResult.toWifiAccessPoint(
                        rangingResult =
                            rangingResultsByMacAddress[
                                android.net.MacAddress.fromString(scanResult.BSSID)]
                    )
                }
            }
            .collect(::send)
    }

    private suspend fun List<ScanResult>.getRangingResults(
        rangingMode: ActiveWifiAccessPointSource.RangingMode
    ): Map<android.net.MacAddress, RangingResult> {
        if (rangingMode == ActiveWifiAccessPointSource.RangingMode.NEVER) {
            return emptyMap()
        }

        if (wifiRttManager?.isAvailable != true) {
            Timber.d("Wi-Fi RTT ranging currently unavailable")
            return emptyMap()
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("No permission for Wi-Fi RTT ranging")
            return emptyMap()
        }

        @SuppressLint("MissingPermission")
        return wifiRttManager
            .rangeDistances(this, rangingMode)
            .asSequence()
            .filter { it.status == RangingResult.STATUS_SUCCESS }
            .distinctBy {
                /**
                 * Filter ranging results which have duplicate distances. The ranging API seems to
                 * return bad data in some cases even though the status is successful. In these
                 * cases, multiple results will have a fixed value.
                 */
                it.distanceMm
            }
            .filter { it.macAddress != null }
            .associateBy { it.macAddress!! }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
private fun getWifiScanFlowR(wifiManager: WifiManager): Flow<List<ScanResult>> = callbackFlow {
    val callback =
        object : WifiManager.ScanResultsCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResultsAvailable() {
                if (isActive) {
                    trySendBlocking(wifiManager.scanResults)
                }
            }
        }

    wifiManager.registerScanResultsCallback(ImmediateExecutor, callback)

    awaitClose { wifiManager.unregisterScanResultsCallback(callback) }
}

private fun getWifiScanFlowLegacy(
    appContext: Context,
    wifiManager: WifiManager,
): Flow<List<ScanResult>> {
    return appContext
        .broadcastReceiverFlow(IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        .map { @SuppressLint("MissingPermission") wifiManager.scanResults }
}
