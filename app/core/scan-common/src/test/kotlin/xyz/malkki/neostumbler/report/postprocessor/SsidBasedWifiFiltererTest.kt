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

class SsidBasedWifiFiltererTest {
    @Test
    fun `No access points are filtered without a filter`() = runTest {
        val ssidBasedWifiFilterer = SsidBasedWifiFilterer({ emptyList() })

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
                                    .copy(ssid = "test_ssid_1")
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("02:02:02:02:02:02"))
                                    .copy(ssid = "test_ssid_2")
                        ),
                    ),
            )

        val filteredReport = ssidBasedWifiFilterer.postProcessReport(report)

        assertEquals(2, filteredReport.wifiAccessPoints.size)
    }

    @Test
    fun `Access points with SSID on the filter list get filtered`() = runTest {
        val ssidBasedWifiFilterer = SsidBasedWifiFilterer({ listOf("test_ssid") })

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
                                    .copy(ssid = "test_ssid_1")
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("02:02:02:02:02:02"))
                                    .copy(ssid = "test_ssid_2")
                        ),
                    ),
            )

        val filteredReport = ssidBasedWifiFilterer.postProcessReport(report)

        assertEquals(0, filteredReport.wifiAccessPoints.size)
    }
}
