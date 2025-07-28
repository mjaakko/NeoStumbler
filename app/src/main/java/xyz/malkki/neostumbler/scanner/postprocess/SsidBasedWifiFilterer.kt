package xyz.malkki.neostumbler.scanner.postprocess

import xyz.malkki.neostumbler.core.report.ReportData

/** Removes Wi-Fi access points that have a SSID containing any substring on the filter list */
class SsidBasedWifiFilterer(private val wifiFilterList: Collection<String>) : ReportPostProcessor {
    override fun postProcessReport(reportData: ReportData): ReportData? {
        val filteredWifiAccessPoints =
            reportData.wifiAccessPoints.filter { wifiAccessPoint ->
                wifiFilterList.none { filteredSsid ->
                    wifiAccessPoint.emitter.ssid?.lowercase()?.contains(filteredSsid.lowercase()) ==
                        true
                }
            }

        return reportData.copy(wifiAccessPoints = filteredWifiAccessPoints)
    }
}
