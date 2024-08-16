package xyz.malkki.neostumbler.scanner.source

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

//Minimum scan interval that can be used when Wi-Fi scan throttling is active
private val MIN_INTERVAL_THROTTLED = 30.seconds

class WifiManagerWifiAccessPointSource(context: Context) : WifiAccessPointSource {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService<WifiManager>()!!

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE])
    override fun getWifiAccessPointFlow(interval: Duration): Flow<List<WifiAccessPoint>> = channelFlow {
        launch(Dispatchers.Default) {
            var lastScan = SystemClock.elapsedRealtime()

            while (true) {
                @Suppress("DEPRECATION")
                if (wifiManager.startScan()) {
                    lastScan = SystemClock.elapsedRealtime()
                    delay(interval)
                } else {
                    val delayMillis = maxOf(0, (lastScan + MIN_INTERVAL_THROTTLED.inWholeMilliseconds) - SystemClock.elapsedRealtime())

                    Timber.d("Wi-Fi scan was not started, maybe we hit scan throttling? Trying again in ${delayMillis}ms")

                    delay(delayMillis)
                }
            }
        }

        val scanResultFlow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWifiScanFlowR(wifiManager)
        } else {
            getWifiScanFlowLegacy(appContext, wifiManager)
        }

        scanResultFlow
            .map { scanResults ->
                scanResults.map { scanResult ->
                    WifiAccessPoint.fromScanResult(scanResult)
                }
            }
            .collect(::send)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getWifiScanFlowR(wifiManager: WifiManager): Flow<List<ScanResult>> = callbackFlow {
        val callback = object : WifiManager.ScanResultsCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResultsAvailable() {
                if (isActive) {
                    trySendBlocking(wifiManager.scanResults)
                }
            }
        }

        wifiManager.registerScanResultsCallback(ImmediateExecutor, callback)

        awaitClose {
            wifiManager.unregisterScanResultsCallback(callback)
        }
    }

    private fun getWifiScanFlowLegacy(appContext: Context, wifiManager: WifiManager): Flow<List<ScanResult>> = callbackFlow {
        val broadcastReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                if (isActive) {
                    trySendBlocking(wifiManager.scanResults)
                }
            }
        }

        appContext.registerReceiver(broadcastReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        awaitClose {
            appContext.unregisterReceiver(broadcastReceiver)
        }
    }
}