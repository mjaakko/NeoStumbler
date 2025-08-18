package xyz.malkki.neostumbler.scanner

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.Position.Source
import xyz.malkki.neostumbler.core.airpressure.AirPressureObservation
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.scanner.postprocess.HiddenWifiFilterer

class WirelessScannerTest {
    @Test
    fun `Test no reports are created with no data`() {
        val wirelessScanner =
            WirelessScanner(
                locationSource = {
                    flowOf(
                        PositionObservation(
                            position =
                                Position(
                                    latitude = 50.0,
                                    longitude = 10.0,
                                    accuracy = 15.0,
                                    source = Source.GPS,
                                ),
                            timestamp = 0,
                        )
                    )
                },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { emptyFlow() },
                bluetoothBeaconSource = { emptyFlow() },
                airPressureSource = { emptyFlow() },
            )

        val reportFlow = wirelessScanner.createReports()

        assertThrows(TimeoutCancellationException::class.java) {
            runBlocking { withTimeout(12.seconds) { reportFlow.first() } }
        }
    }

    @Test
    fun `Test no reports are created with old data`() {
        val wirelessScanner =
            WirelessScanner(
                locationSource = {
                    flowOf(
                        PositionObservation(
                            position =
                                Position(
                                    latitude = 50.0,
                                    longitude = 10.0,
                                    accuracy = 15.0,
                                    source = Source.GPS,
                                ),
                            timestamp = 60000,
                        )
                    )
                },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { emptyFlow() },
                bluetoothBeaconSource = {
                    flowOf(
                        listOf(
                            EmitterObservation(
                                emitter =
                                    BluetoothBeacon(
                                        macAddress = MacAddress("01:01:01:01:01"),
                                        signalStrength = -68,
                                        beaconType = null,
                                        id1 = null,
                                        id2 = null,
                                        id3 = null,
                                    ),
                                timestamp = 25000,
                            )
                        )
                    )
                },
                airPressureSource = { emptyFlow() },
            )

        val reportFlow = wirelessScanner.createReports()

        assertThrows(TimeoutCancellationException::class.java) {
            runBlocking { withTimeout(12.seconds) { reportFlow.first() } }
        }
    }

    @Test
    fun `Test no reports are created when Wi-Fi networks have an opt-out`() {
        val wirelessScanner =
            WirelessScanner(
                locationSource = {
                    flowOf(
                        PositionObservation(
                            position =
                                Position(
                                    latitude = 50.0,
                                    longitude = 10.0,
                                    accuracy = 15.0,
                                    source = Source.GPS,
                                ),
                            timestamp = 0,
                        )
                    )
                },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = {
                    flowOf(
                        listOf(
                            EmitterObservation(
                                emitter =
                                    WifiAccessPoint(
                                        macAddress = MacAddress("01:01:01:01:01:01"),
                                        radioType = WifiAccessPoint.RadioType.AC,
                                        channel = null,
                                        frequency = null,
                                        signalStrength = null,
                                        ssid = "test_nomap",
                                    ),
                                timestamp = 0,
                            ),
                            EmitterObservation(
                                emitter =
                                    WifiAccessPoint(
                                        macAddress = MacAddress("02:02:02:02:02:02"),
                                        radioType = WifiAccessPoint.RadioType.AC,
                                        channel = null,
                                        frequency = null,
                                        signalStrength = null,
                                        ssid = "",
                                    ),
                                timestamp = 0,
                            ),
                        )
                    )
                },
                bluetoothBeaconSource = { emptyFlow() },
                airPressureSource = { emptyFlow() },
                postProcessors = listOf(HiddenWifiFilterer()),
            )

        val reportFlow = wirelessScanner.createReports()

        assertThrows(TimeoutCancellationException::class.java) {
            runBlocking { withTimeout(12.seconds) { reportFlow.first() } }
        }
    }

    @Test
    fun `Test that a report is created`() {
        val wirelessScanner =
            WirelessScanner(
                locationSource = {
                    flow {
                        delay(1000)

                        val position =
                            PositionObservation(
                                position =
                                    Position(
                                        latitude = 50.0,
                                        longitude = 10.0,
                                        accuracy = 15.0,
                                        source = Source.GPS,
                                    ),
                                timestamp = 0,
                            )

                        emit(position)
                    }
                },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { emptyFlow() },
                bluetoothBeaconSource = {
                    flowOf(
                        listOf(
                            EmitterObservation(
                                emitter =
                                    BluetoothBeacon(
                                        macAddress = MacAddress("01:01:01:01:01:01"),
                                        beaconType = null,
                                        id1 = null,
                                        id2 = null,
                                        id3 = null,
                                        signalStrength = -75,
                                    ),
                                timestamp = 0,
                            )
                        )
                    )
                },
                airPressureSource = {
                    flowOf(AirPressureObservation(airPressure = 1013.25f, timestamp = 0L))
                },
            )

        val reportFlow = wirelessScanner.createReports()

        val reports = runBlocking {
            val output = mutableListOf<ReportData>()

            reportFlow
                .timeout(12.seconds)
                .catch {
                    if (it !is TimeoutCancellationException) {
                        throw it
                    }
                }
                .collect(output::add)

            output.toList()
        }

        assertEquals(1, reports.size)

        val report = reports.first()

        assertNotNull(report.position.position.pressure)
        assertEquals(1013.25, report.position.position.pressure!!, 0.01)
    }
}
