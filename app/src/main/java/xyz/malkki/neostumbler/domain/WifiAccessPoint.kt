package xyz.malkki.neostumbler.domain

import android.net.wifi.ScanResult
import android.os.Build
import xyz.malkki.neostumbler.extensions.ssidString
import xyz.malkki.neostumbler.extensions.timestampMillis

data class WifiAccessPoint(
    val macAddress: String,
    val radioType: RadioType?,
    val channel: Int?,
    val frequency: Int?,
    val signalStrength: Int?,
    val ssid: String?,
    override val timestamp: Long,
) : ObservedDevice<String> {
    override val uniqueKey: String
        get() = macAddress

    companion object {
        fun fromScanResult(scanResult: ScanResult): WifiAccessPoint {
            val radioType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (scanResult.wifiStandard) {
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
                when (scanResult.channelWidth) {
                    ScanResult.CHANNEL_WIDTH_20MHZ -> scanResult.frequency
                    else -> scanResult.centerFreq0
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
                macAddress = scanResult.BSSID,
                radioType = radioType,
                channel = channelNumber,
                frequency = frequency,
                signalStrength = scanResult.level,
                ssid = scanResult.ssidString,
                timestamp = scanResult.timestampMillis,
            )
        }
    }

    enum class RadioType {
        G,
        N,
        AC,
        AX,
        BE,
    }
}
