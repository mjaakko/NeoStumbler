package xyz.malkki.neostumbler.extensions

import android.net.wifi.ScanResult
import android.os.Build

private const val S_IN_MS = 1000

/** SSID of the network without quotation marks and surrounding whitespace */
val ScanResult.ssidString: String?
    get() {
        val ssid =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                wifiSsid?.toString()
            } else {
                @Suppress("DEPRECATION") SSID
            }

        return ssid
            ?.replace(Regex("(^\"|\"\$)"), "") // Remove quotation marks from beginning and end
            ?.trim() // Remove surrounding whitespace to avoid writing empty values to the DB
    }

val ScanResult.timestampMillis: Long
    get() = timestamp / S_IN_MS
