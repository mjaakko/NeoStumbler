package xyz.malkki.neostumbler.data.emitter.mapper

import android.net.wifi.ScanResult
import android.os.Build
import xyz.malkki.neostumbler.core.WifiAccessPoint
import xyz.malkki.neostumbler.core.WifiAccessPoint.RadioType

internal fun ScanResult.toWifiAccessPoint(): WifiAccessPoint {
    val radioType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (wifiStandard) {
                ScanResult.WIFI_STANDARD_11BE -> RadioType.BE
                ScanResult.WIFI_STANDARD_11AX -> RadioType.AX
                ScanResult.WIFI_STANDARD_11AC -> RadioType.AC
                ScanResult.WIFI_STANDARD_11N -> RadioType.N
                ScanResult.WIFI_STANDARD_LEGACY -> RadioType.G
                else -> null
            }
        } else {
            null
        }

    val frequency =
        when (channelWidth) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> frequency
            else -> centerFreq0
        }

    val channelNumber =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ScanResult.convertFrequencyMhzToChannelIfSupported(frequency).takeIf {
                it != ScanResult.UNSPECIFIED
            }
        } else {
            null
        }

    return WifiAccessPoint(
        macAddress = BSSID,
        radioType = radioType,
        channel = channelNumber,
        frequency = frequency,
        signalStrength = level,
        ssid = ssidString,
        timestamp = timestampMillis,
    )
}

private const val S_IN_MS = 1000

/** SSID of the network without quotation marks and surrounding whitespace */
private val ScanResult.ssidString: String?
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

private val ScanResult.timestampMillis: Long
    get() = timestamp / S_IN_MS
