package xyz.malkki.neostumbler.report.postprocessor

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.someBluetoothBeacon
import xyz.malkki.neostumbler.core.observation.someEmitterObservation
import xyz.malkki.neostumbler.core.report.someReportData
import xyz.malkki.neostumbler.core.somePosition
import xyz.malkki.neostumbler.core.somePositionObservation
import xyz.malkki.neostumbler.core.someWifiAccessPoint
import xyz.malkki.neostumbler.core.values.SignalStrength

class AutoDetectingMovingWifiBluetoothFiltererTest {
    @Test
    fun `Test moving Wi-Fi and Bluetooth are filtered`() = runTest {
        val filterer =
            AutoDetectingMovingWifiBluetoothFilterer(
                enabled = { true },
                maxDistanceFromExisting = 500.0,
            )

        val position1 =
            somePositionObservation(
                position =
                    somePosition(
                        latitude = 40.689100,
                        longitude = -74.044300,
                    )
            )

        val report1 =
            someReportData(
                position = position1,
                wifiAccessPoints =
                    listOf(
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01"))
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("02:02:02:02:02:02"))
                        ),
                    ),
                bluetoothBeacons =
                    listOf(
                        someEmitterObservation(
                            emitter =
                                someBluetoothBeacon(
                                    macAddress = MacAddress("04:04:04:04:04:04"),
                                    signalStrength = SignalStrength(-80),
                                )
                        )
                    ),
            )

        filterer.postProcessReport(report1)

        val afterMoving = position1.position.latLng.destination(700.0, 43.15)

        val position2 =
            somePositionObservation(
                position =
                    somePosition(
                        latitude = afterMoving.latitude,
                        longitude = afterMoving.longitude,
                    )
            )

        val report2 = report1.copy(position = position2)

        val filteredReport2 = filterer.postProcessReport(report2)

        assertEquals(0, filteredReport2.wifiAccessPoints.size)
        assertEquals(0, filteredReport2.bluetoothBeacons.size)

        // Test processing the first report again -> content should get filtered
        val filteredReport1 = filterer.postProcessReport(report1)

        assertEquals(0, filteredReport1.wifiAccessPoints.size)
        assertEquals(0, filteredReport1.bluetoothBeacons.size)
    }

    @Test
    fun `Test stationary Wi-Fi is not filtered`() = runTest {
        val filterer =
            AutoDetectingMovingWifiBluetoothFilterer(
                enabled = { true },
                maxDistanceFromExisting = 500.0,
            )

        val position1 =
            somePositionObservation(
                position =
                    somePosition(
                        latitude = 40.689100,
                        longitude = -74.044300,
                    )
            )

        val report1 =
            someReportData(
                position = position1,
                wifiAccessPoints =
                    listOf(
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01"))
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("02:02:02:02:02:02"))
                        ),
                    ),
            )

        filterer.postProcessReport(report1)

        val afterMoving = position1.position.latLng.destination(700.0, 43.15)

        val position2 =
            somePositionObservation(
                position =
                    somePosition(
                        latitude = afterMoving.latitude,
                        longitude = afterMoving.longitude,
                    )
            )

        val report2 =
            someReportData(
                position = position2,
                wifiAccessPoints =
                    listOf(
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("03:03:03:03:03:03"))
                        ),
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("04:04:04:04:04:04"))
                        ),
                    ),
            )

        val filteredReport = filterer.postProcessReport(report2)

        assertEquals(2, filteredReport.wifiAccessPoints.size)
        assertTrue(
            filteredReport.wifiAccessPoints.any {
                it.emitter.macAddress.value == "03:03:03:03:03:03"
            }
        )
    }

    @Test
    fun `Test moving device can be included in a report in non-deterministic mode`() = runTest {
        val filterer =
            AutoDetectingMovingWifiBluetoothFilterer(
                enabled = { true },
                maxDistanceFromExisting = 500.0,
                deterministic = false,
            )

        val position1 =
            somePositionObservation(
                position =
                    somePosition(
                        latitude = 40.689100,
                        longitude = -74.044300,
                    )
            )

        val report1 =
            someReportData(
                position = position1,
                wifiAccessPoints =
                    listOf(
                        someEmitterObservation(
                            emitter =
                                someWifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01"))
                        )
                    ),
            )

        filterer.postProcessReport(report1)

        val afterMoving = position1.position.latLng.destination(700.0, 43.15)

        val position2 =
            somePositionObservation(
                position =
                    somePosition(
                        latitude = afterMoving.latitude,
                        longitude = afterMoving.longitude,
                    )
            )

        val report2 = report1.copy(position = position2)

        var notFiltered = false

        @Suppress("UnusedPrivateProperty")
        for (i in 1..1000) {
            if (filterer.postProcessReport(report2).wifiAccessPoints.isNotEmpty()) {
                notFiltered = true
                break
            }
        }

        assertTrue(notFiltered)
    }
}
