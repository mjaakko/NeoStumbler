package xyz.malkki.wifiscannerformls.extensions

import android.net.wifi.ScanResult
import android.os.Build

val ScanResult.ssidString: String?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        wifiSsid?.toString()
    } else {
        SSID
    }

val ScanResult.timestampMillis: Long
    get() = timestamp / 1000