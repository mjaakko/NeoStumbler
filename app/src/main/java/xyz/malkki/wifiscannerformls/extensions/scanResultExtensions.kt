package xyz.malkki.wifiscannerformls.extensions

import android.net.wifi.ScanResult
import android.os.Build

val ScanResult.ssidString: String?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        wifiSsid?.toString()
    } else {
        SSID
    }?.replace(Regex("(^\"|\"\$)"), "") //Remove quotation marks from beginning and end

val ScanResult.timestampMillis: Long
    get() = timestamp / 1000