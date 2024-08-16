package xyz.malkki.neostumbler.scanner

import xyz.malkki.neostumbler.domain.WifiAccessPoint

/**
 * Filters Wi-Fi networks that should not be sent to MLS.
 * Wi-Fi networks which should not be sent to MLS are those with hidden SSID or SSID ending in "_nomap"
 *
 * @return Filtered list of scan results
 */
fun List<WifiAccessPoint>.filterForMLS(): List<WifiAccessPoint> = filter { wifiAccessPoint ->
    val ssid = wifiAccessPoint.ssid

    !ssid.isNullOrBlank() && !ssid.endsWith("_nomap")
}