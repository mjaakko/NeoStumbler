package xyz.malkki.neostumbler.scanner.postprocess

import java.util.Locale
import xyz.malkki.neostumbler.scanner.data.ReportData

/** Removes Wi-Fi access points that have a SSID containing any substring on the filter list */
class SsidBasedWifiFilterer(private val wifiFilterList: Collection<String>) : ReportPostProcessor {
    override fun postProcessReport(reportData: ReportData): ReportData? {
        val filteredWifiAccessPoints =
            reportData.wifiAccessPoints.filter { wifiAccessPoint ->
                wifiFilterList.none { filteredSsid ->
                    wifiAccessPoint.ssid
                        ?.lowercase(Locale.ROOT)
                        ?.contains(filteredSsid.lowercase(Locale.ROOT)) == true
                }
            }

        return reportData.copy(wifiAccessPoints = filteredWifiAccessPoints)
    }
}
