package xyz.malkki.neostumbler.data.emitter.mapper

import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingResult
import android.os.Build
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.ranging.EstimatedDistance
import xyz.malkki.neostumbler.core.values.Distance
import xyz.malkki.neostumbler.core.values.SignalStrength

private val ScanResult.radioType: WifiAccessPoint.RadioType?
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (wifiStandard) {
                ScanResult.WIFI_STANDARD_11BE -> WifiAccessPoint.RadioType.BE
                ScanResult.WIFI_STANDARD_11AX -> WifiAccessPoint.RadioType.AX
                ScanResult.WIFI_STANDARD_11AC -> WifiAccessPoint.RadioType.AC
                ScanResult.WIFI_STANDARD_11N -> WifiAccessPoint.RadioType.N
                ScanResult.WIFI_STANDARD_LEGACY -> WifiAccessPoint.RadioType.G
                else -> null
            }
        } else {
            null
        }

internal fun ScanResult.toWifiAccessPoint(
    rangingResult: RangingResult? = null
): EmitterObservation<WifiAccessPoint, MacAddress> {

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

    val rangingResultHasValidRssi = (rangingResult != null) && (rangingResult.rssi != 0)

    return EmitterObservation(
        emitter =
            WifiAccessPoint(
                macAddress = MacAddress(BSSID),
                radioType = radioType,
                channel = channelNumber,
                frequency = frequency,
                signalStrength =
                    SignalStrength(
                        if (rangingResultHasValidRssi) {
                            rangingResult.rssi
                        } else {
                            level
                        }
                    ),
                ssid = ssidString,
            ),
        timestamp =
            // Match timestamp with the RSSI data source
            if (rangingResultHasValidRssi) {
                rangingResult.rangingTimestampMillis
            } else {
                timestampMillis
            },
        estimatedDistance = rangingResult?.toEstimatedDistance(this),
    )
}

private const val M_IN_MM = 1000

private fun RangingResult.toEstimatedDistance(scanResult: ScanResult): EstimatedDistance {
    val isTwosided =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            is80211mcMeasurement
        } else {
            scanResult.is80211mcResponder
        }

    return EstimatedDistance(
        distance = Distance(distanceMm.toDouble() / M_IN_MM),
        accuracy =
            Distance(distanceStdDevMm.toDouble() / M_IN_MM)
                // Only take the standard deviation when it's valid (i.e. more than one measurement)
                .takeIf { numSuccessfulMeasurements > 1 },
        rangingType =
            if (isTwosided) {
                EstimatedDistance.RangingType.TWO_SIDED
            } else {
                EstimatedDistance.RangingType.ONE_SIDED
            },
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
