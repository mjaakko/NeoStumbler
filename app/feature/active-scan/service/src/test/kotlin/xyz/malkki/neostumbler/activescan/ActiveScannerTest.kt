package xyz.malkki.neostumbler.activescan

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.Position.Source
import xyz.malkki.neostumbler.core.airpressure.AirPressureObservation
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.data.movement.MovementDetector
import xyz.malkki.neostumbler.report.postprocessor.HiddenWifiFilterer

private val settings =
    ActiveScanSettings(
        wifiScanDistance = 100,
        cellScanDistance = 100,
        ignoreWifiScanThrottling = true,
        lowBatteryThreshold = null,
    )

private val movementDetectorProvider = {
    MovementDetector {
        channelFlow {
            send(true)

            awaitClose {}
        }
    }
}

class ActiveScannerTest {
    @Test
    fun `No reports are created with no data`() = runTest {
        val activeScanner =
            ActiveScanner(
                locationSourceProvider = {
                    LocationSource { _, _ ->
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
                    }
                },
                airPressureSource = { emptyFlow() },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { _, _ -> emptyFlow() },
                bluetoothBeaconSource = { emptyFlow() },
                movementDetectorProvider = movementDetectorProvider,
                batteryLevelMonitor = { flowOf(1.0f) },
                postProcessorProvider = { emptyList() },
            )

        val reportsFlow =
            activeScanner.getReportsFlow(
                scanSettings = settings,
                onGpsActive = {},
                onScanStateChange = {},
            )

        val reports = buildList {
            reportsFlow
                .timeout(5.seconds)
                .catch {
                    if (it !is TimeoutCancellationException) {
                        throw it
                    }
                }
                .collect(::add)
        }

        assertTrue(reports.isEmpty())
    }

    @Test
    fun `No report is created with old data`() = runTest {
        val activeScanner =
            ActiveScanner(
                locationSourceProvider = {
                    LocationSource { _, _ ->
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
                    }
                },
                airPressureSource = { emptyFlow() },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { _, _ -> emptyFlow() },
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
                movementDetectorProvider = movementDetectorProvider,
                batteryLevelMonitor = { flowOf(1.0f) },
                postProcessorProvider = { emptyList() },
            )

        val reportsFlow =
            activeScanner.getReportsFlow(
                scanSettings = settings,
                onGpsActive = {},
                onScanStateChange = {},
            )

        val reports = buildList {
            reportsFlow
                .timeout(5.seconds)
                .catch {
                    if (it !is TimeoutCancellationException) {
                        throw it
                    }
                }
                .collect(::add)
        }

        assertTrue(reports.isEmpty())
    }

    @Test
    fun `Wi-Fi networks are filtered with postprocessors`() = runTest {
        val activeScanner =
            ActiveScanner(
                locationSourceProvider = {
                    LocationSource { _, _ ->
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
                    }
                },
                airPressureSource = { emptyFlow() },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { _, _ ->
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
                movementDetectorProvider = movementDetectorProvider,
                batteryLevelMonitor = { flowOf(1.0f) },
                postProcessorProvider = { listOf(HiddenWifiFilterer()) },
            )

        val reportsFlow =
            activeScanner.getReportsFlow(
                scanSettings = settings,
                onGpsActive = {},
                onScanStateChange = {},
            )

        val reports = buildList {
            reportsFlow
                .timeout(5.seconds)
                .catch {
                    if (it !is TimeoutCancellationException) {
                        throw it
                    }
                }
                .collect(::add)
        }

        assertTrue(reports.isEmpty())
    }

    @Test
    fun `Report is created`() = runTest {
        val activeScanner =
            ActiveScanner(
                locationSourceProvider = {
                    LocationSource { _, _ ->
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
                    }
                },
                airPressureSource = {
                    flowOf(AirPressureObservation(airPressure = 1013.25f, timestamp = 0L))
                },
                cellInfoSource = { emptyFlow() },
                wifiAccessPointSource = { _, _ ->
                    flowOf(
                        listOf(
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
                            )
                        )
                    )
                },
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
                movementDetectorProvider = movementDetectorProvider,
                batteryLevelMonitor = { flowOf(1.0f) },
                postProcessorProvider = { listOf(HiddenWifiFilterer()) },
            )

        val reportsFlow =
            activeScanner.getReportsFlow(
                scanSettings = settings,
                onGpsActive = {},
                onScanStateChange = {},
            )

        val reports = buildList {
            reportsFlow
                .timeout(5.seconds)
                .catch {
                    if (it !is TimeoutCancellationException) {
                        throw it
                    }
                }
                .collect(::add)
        }

        val report = reports.firstOrNull()
        assertNotNull(report)

        assertEquals(
            MacAddress("01:01:01:01:01:01"),
            report?.bluetoothBeacons?.firstOrNull()?.emitter?.macAddress,
        )

        assertNotNull(report?.position?.position?.pressure)
        assertEquals(1013.25, report?.position?.position?.pressure!!, 0.01)
    }
}
