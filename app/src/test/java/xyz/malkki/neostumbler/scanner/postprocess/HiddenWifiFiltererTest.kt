package xyz.malkki.neostumbler.scanner.postprocess

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.Position.Source
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData

class HiddenWifiFiltererTest {
    @Test
    fun `Hidden Wi-Fi access points are filtered`() {
        val hiddenWifiFilterer = HiddenWifiFilterer()

        val report =
            ReportData(
                position =
                    PositionObservation(
                        position =
                            Position(
                                latitude = 45.51156,
                                longitude = 12.213415,
                                source = Source.GPS,
                            ),
                        timestamp = 0,
                    ),
                cellTowers = emptyList(),
                wifiAccessPoints =
                    listOf(
                        EmitterObservation(
                            WifiAccessPoint(
                                macAddress = MacAddress("01:01:01:01:01:01"),
                                ssid = "my_network_nomap",
                            ),
                            timestamp = 0,
                        ),
                        EmitterObservation(
                            WifiAccessPoint(
                                macAddress = MacAddress("02:02:02:02:02:02"),
                                ssid = "",
                            ),
                            timestamp = 0,
                        ),
                        EmitterObservation(
                            WifiAccessPoint(
                                macAddress = MacAddress("03:03:03:03:03:03"),
                                ssid = "\u0000\u0000\u0000\u0000\u0000",
                            ),
                            timestamp = 0,
                        ),
                        EmitterObservation(
                            WifiAccessPoint(
                                macAddress = MacAddress("04:04:04:04:04:04"),
                                ssid = "free wifi",
                            ),
                            timestamp = 0,
                        ),
                    ),
                bluetoothBeacons = emptyList(),
            )

        val filteredReport = hiddenWifiFilterer.postProcessReport(report)

        assertEquals(1, filteredReport?.wifiAccessPoints?.size)
        assertEquals("free wifi", filteredReport?.wifiAccessPoints?.first()?.emitter?.ssid)
    }
}
