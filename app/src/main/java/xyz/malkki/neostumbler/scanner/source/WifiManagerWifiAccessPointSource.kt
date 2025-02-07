package xyz.malkki.neostumbler.scanner.source

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
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
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.extensions.buffer
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import xyz.malkki.neostumbler.utils.RateLimiter
import xyz.malkki.neostumbler.utils.broadcastReceiverFlow
import xyz.malkki.neostumbler.utils.delayWithMinDuration

// https://developer.android.com/develop/connectivity/wifi/wifi-scan#wifi-scan-throttling
private val ANDROID_WIFI_SCAN_THROTTLE_PERIOD = 2.minutes
private const val ANDROID_WIFI_SCAN_THROTTLE_COUNT = 4

private val MAX_INTERVAL = 1.minutes

// Minimum scan interval that can be used when Wi-Fi scan throttling is active.
// This is slightly lower than the throttle period divided by number of scans to allow for bursts
private val MIN_INTERVAL_THROTTLED: Duration =
    (ANDROID_WIFI_SCAN_THROTTLE_PERIOD / ANDROID_WIFI_SCAN_THROTTLE_COUNT) * 0.75

// Minimum scan interval when WI-Fi scanning is not throttled
private val MIN_INTERVAL_UNTHROTTLED = 1.5.seconds

// Buffer scan results because sometimes multiple scan results are received in a short succession
private val WIFI_BUFFER_WINDOW = 5.seconds

class WifiManagerWifiAccessPointSource(
    context: Context,
    wifiScanThrottled: Boolean,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime,
) : WifiAccessPointSource {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService<WifiManager>()!!

    private val rateLimiter =
        if (wifiScanThrottled) {
            RateLimiter(
                ANDROID_WIFI_SCAN_THROTTLE_COUNT,
                ANDROID_WIFI_SCAN_THROTTLE_PERIOD,
                timeSource,
            )
        } else {
            null
        }

    private val minScanInterval =
        if (wifiScanThrottled) {
            MIN_INTERVAL_THROTTLED
        } else {
            MIN_INTERVAL_UNTHROTTLED
        }

    private suspend fun doWifiScan() {
        Timber.d("Starting Wi-Fi scan")

        if (rateLimiter != null) {
            rateLimiter.doRateLimited { @Suppress("DEPRECATION") wifiManager.startScan() }
        } else {
            @Suppress("DEPRECATION") wifiManager.startScan()
        }
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE]
    )
    override fun getWifiAccessPointFlow(interval: Flow<Duration>): Flow<List<WifiAccessPoint>> =
        channelFlow {
            launch(Dispatchers.Default) {
                val scanInterval =
                    interval
                        .map {
                            it.coerceIn(minimumValue = minScanInterval, maximumValue = MAX_INTERVAL)
                        }
                        .stateIn(
                            this,
                            started = SharingStarted.Eagerly,
                            initialValue = MAX_INTERVAL,
                        )

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
                    scanResults.map { scanResult -> WifiAccessPoint.fromScanResult(scanResult) }
                }
                .buffer(WIFI_BUFFER_WINDOW)
                .map { wifis ->
                    wifis
                        .flatten()
                        .groupingBy { it.macAddress }
                        .reduce { _, a, b ->
                            maxOf(a, b, Comparator.comparingLong(WifiAccessPoint::timestamp))
                        }
                        .values
                        .toList()
                }
                .collect(::send)
        }

    @RequiresApi(Build.VERSION_CODES.R)
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
}
