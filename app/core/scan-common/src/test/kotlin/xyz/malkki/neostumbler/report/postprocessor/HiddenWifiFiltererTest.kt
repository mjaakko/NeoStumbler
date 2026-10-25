package xyz.malkki.neostumbler.report.postprocessor

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.observation.someEmitterObservation
import xyz.malkki.neostumbler.core.report.someReportData
import xyz.malkki.neostumbler.core.somePosition
import xyz.malkki.neostumbler.core.somePositionObservation
import xyz.malkki.neostumbler.core.someWifiAccessPoint

class HiddenWifiFiltererTest {
    @Test
    fun `Hidden Wi-Fi access points are filtered`() = runTest {
        val hiddenWifiFilterer = HiddenWifiFilterer()

        val report =
            someReportData(
                position =
                    somePositionObservation(
                        position =
                            somePosition(
                                latitude = 45.51156,
                                longitude = 12.213415,
                            )
                    ),
                wifiAccessPoints =
                    listOf(
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01"))
                                    .copy(ssid = "my_network_nomap")
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("02:02:02:02:02:02"))
                                    .copy(ssid = "")
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("03:03:03:03:03:03"))
                                    .copy(ssid = "\u0000\u0000\u0000\u0000\u0000")
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("04:04:04:04:04:04"))
                                    .copy(ssid = "free wifi")
                        ),
                    ),
            )

        val filteredReport = hiddenWifiFilterer.postProcessReport(report)

        assertEquals(1, filteredReport.wifiAccessPoints.size)
        assertEquals("free wifi", filteredReport.wifiAccessPoints.first().emitter.ssid)
    }
}
