package xyz.malkki.wifiscannerformls.scanner

import android.net.wifi.ScanResult
import xyz.malkki.wifiscannerformls.extensions.ssidString

/**
 * Filters Wi-Fi networks that should not be sent to MLS.
 * Wi-Fi networks which should not be sent to MLS are those with hidden SSID or SSID ending in "_nomap"
 *
 * @return Filtered list of scan results
 */
fun List<ScanResult>.filterForMLS(): List<ScanResult> = filter { scanResult ->
    val ssid = scanResult.ssidString

    !ssid.isNullOrBlank() && !ssid.endsWith("_nomap")
}