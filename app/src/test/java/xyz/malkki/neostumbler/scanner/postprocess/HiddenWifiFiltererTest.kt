package xyz.malkki.neostumbler.scanner.postprocess

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.scanner.data.ReportData

class HiddenWifiFiltererTest {
    @Test
    fun `Hidden Wi-Fi access points are filtered`() {
        val hiddenWifiFilterer = HiddenWifiFilterer()

        val report =
            ReportData(
                position =
                    Position(
                        latitude = 45.51156,
                        longitude = 12.213415,
                        source = "gps",
                        timestamp = 0,
                    ),
                cellTowers = emptyList(),
                wifiAccessPoints =
                    listOf(
                        WifiAccessPoint(
                            macAddress = "01:01:01:01:01:01",
                            ssid = "my_network_nomap",
                            timestamp = 0,
                        ),
                        WifiAccessPoint(macAddress = "02:02:02:02:02:02", ssid = "", timestamp = 0),
                        WifiAccessPoint(
                            macAddress = "03:03:03:03:03:03",
                            ssid = "\u0000\u0000\u0000\u0000\u0000",
                            timestamp = 0,
                        ),
                        WifiAccessPoint(
                            macAddress = "04:04:04:04:04:04",
                            ssid = "free wifi",
                            timestamp = 0,
                        ),
                    ),
                bluetoothBeacons = emptyList(),
            )

        val filteredReport = hiddenWifiFilterer.postProcessReport(report)

        assertEquals(1, filteredReport?.wifiAccessPoints?.size)
        assertEquals("free wifi", filteredReport?.wifiAccessPoints?.first()?.ssid)
    }
}
