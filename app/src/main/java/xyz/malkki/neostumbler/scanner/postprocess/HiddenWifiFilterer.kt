package xyz.malkki.neostumbler.scanner.postprocess

import xyz.malkki.neostumbler.scanner.data.ReportData

/**
 * Filters Wi-Fi networks that should not be sent to geolocation services, i.e. hidden networks with
 * empty SSID or those with SSID ending in "_nomap"
 */
class HiddenWifiFilterer : ReportPostProcessor {
    override fun postProcessReport(reportData: ReportData): ReportData? {
        return reportData.copy(
            wifiAccessPoints =
                reportData.wifiAccessPoints.filter { wifiAccessPoint ->
                    val ssid = wifiAccessPoint.ssid

                    !ssid.isNullOrBlank() &&
                        !ssid.endsWith("_nomap")
                        // Some access points have a SSID with only null characters
                        &&
                        ssid.all { char -> char != '\u0000' }
                }
        )
    }
}
