package xyz.malkki.wifiscannerformls.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive

/**
 * Creates a flow that emits Wi-Fi scan results when they become available. Note that this flow does not start new scans
 *
 * @return Flow of Wi-Fi scan results
 */
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE])
fun getWifiScanFlow(context: Context): Flow<List<ScanResult>> {
    val appContext = context.applicationContext
    val wifiManager = appContext.getSystemService<WifiManager>()!!

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getWifiScanFlowR(wifiManager)
    } else {
        getWifiScanFlowLegacy(appContext, wifiManager)
    }
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