package xyz.malkki.neostumbler.scanner.passive

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.data.emitter.PassiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.PassiveCellTowerSource
import xyz.malkki.neostumbler.data.emitter.PassiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.geography.LatLng

class PassiveScanReportCreatorTest {
    private lateinit var passiveWifiAccessPointSource: PassiveWifiAccessPointSource
    private lateinit var passiveCellTowerSource: PassiveCellTowerSource
    private lateinit var passiveBluetoothBeaconSource: PassiveBluetoothBeaconSource
    private lateinit var passiveScanStateManager: PassiveScanStateManager
    private lateinit var reportSaver: ReportSaver

    private lateinit var passiveScanReportCreator: PassiveScanReportCreator

    @Before
    fun setup() {
        passiveWifiAccessPointSource = mock<PassiveWifiAccessPointSource>()
        passiveCellTowerSource = mock<PassiveCellTowerSource>()
        passiveBluetoothBeaconSource =
            mock<PassiveBluetoothBeaconSource> {
                onBlocking { getBluetoothBeacons() } doReturn emptyList()
            }
        passiveScanStateManager =
            mock<PassiveScanStateManager> {
                val locations = mutableMapOf<PassiveScanStateManager.DataType, LatLng>()
                val timestamps = mutableMapOf<PassiveScanStateManager.DataType, Long>()

                onBlocking { updateLastReportLocation(any(), any()) } doAnswer
                    { invocation ->
                        locations[invocation.arguments[0] as PassiveScanStateManager.DataType] =
                            invocation.arguments[1] as LatLng
                    }
                onBlocking { getLastReportLocation(any()) } doAnswer
                    { invocation ->
                        locations[invocation.arguments[0]]
                    }
                onBlocking { updateMaxTimestamp(any(), any()) } doAnswer
                    { invocation ->
                        timestamps[invocation.arguments[0] as PassiveScanStateManager.DataType] =
                            invocation.arguments[1] as Long
                    }
                onBlocking { getMaxTimestamp(any()) } doAnswer
                    { invocation ->
                        timestamps[invocation.arguments[0] as PassiveScanStateManager.DataType]
                    }
            }
        reportSaver = mock<ReportSaver>()

        passiveScanReportCreator =
            PassiveScanReportCreator(
                passiveWifiAccessPointSource = passiveWifiAccessPointSource,
                passiveCellTowerSource = passiveCellTowerSource,
                passiveBluetoothBeaconSource = passiveBluetoothBeaconSource,
                passiveScanStateManager = passiveScanStateManager,
                reportSaver = reportSaver,
                postProcessors = emptyList(),
                activeScanningRunning = { false },
            )
    }

    @Test
    fun `No report is created for inaccurate location`() = runTest {
        whenever(passiveWifiAccessPointSource.getWifiAccessPoints()) doReturn
            listOf(
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01")),
                    timestamp = 0,
                ),
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("ab:ab:ab:ab:ab:ab")),
                    timestamp = 0,
                ),
            )

        whenever(passiveCellTowerSource.getCellTowers()) doReturn emptyList()

        passiveScanReportCreator.createPassiveScanReport(
            listOf(
                PositionObservation(
                    position =
                        Position(
                            latitude = -22.951752,
                            longitude = -43.210653,
                            accuracy = 300.0,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                )
            )
        )
        verifyNoInteractions(reportSaver)
    }

    @Test
    fun `Reports are created with passively collected data`() = runTest {
        whenever(passiveWifiAccessPointSource.getWifiAccessPoints()) doReturn
            listOf(
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01")),
                    timestamp = 0,
                ),
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("ab:ab:ab:ab:ab:ab")),
                    timestamp = 0,
                ),
            )

        whenever(passiveCellTowerSource.getCellTowers()) doReturn emptyList()

        passiveScanReportCreator.createPassiveScanReport(
            listOf(
                PositionObservation(
                    position =
                        Position(
                            latitude = -22.951752,
                            longitude = -43.210653,
                            accuracy = 5.0,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                )
            )
        )

        argumentCaptor {
            verify(reportSaver, times(1)).createReport(capture())

            assertEquals(2, firstValue.wifiAccessPoints.size)
        }
    }

    @Test
    fun `Reports are not created if the location has not changed`() = runTest {
        whenever(passiveWifiAccessPointSource.getWifiAccessPoints()) doReturn
            listOf(
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01")),
                    timestamp = 0,
                ),
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("ab:ab:ab:ab:ab:ab")),
                    timestamp = 0,
                ),
            )

        whenever(passiveCellTowerSource.getCellTowers()) doReturn emptyList()

        val location = LatLng(latitude = -22.951752, longitude = -43.210653)
        passiveScanReportCreator.createPassiveScanReport(
            listOf(
                PositionObservation(
                    position =
                        Position(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = 5.0,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                )
            )
        )
        verify(reportSaver, times(1)).createReport(any())

        whenever(passiveWifiAccessPointSource.getWifiAccessPoints()) doReturn
            listOf(
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01")),
                    timestamp = 1000,
                ),
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("ab:ab:ab:ab:ab:ab")),
                    timestamp = 1000,
                ),
            )

        passiveScanReportCreator.createPassiveScanReport(
            listOf(
                PositionObservation(
                    position =
                        Position(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = 5.0,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                )
            )
        )
        verifyNoMoreInteractions(reportSaver)
    }

    @Test
    fun `Report is created if the location has not changed but there's new data`() = runTest {
        whenever(passiveWifiAccessPointSource.getWifiAccessPoints()) doReturn
            listOf(
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01")),
                    timestamp = 0,
                ),
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("ab:ab:ab:ab:ab:ab")),
                    timestamp = 0,
                ),
            )

        whenever(passiveCellTowerSource.getCellTowers()) doReturn emptyList()

        val location = LatLng(latitude = -22.951752, longitude = -43.210653)
        passiveScanReportCreator.createPassiveScanReport(
            listOf(
                PositionObservation(
                    position =
                        Position(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = 5.0,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                )
            )
        )
        verify(reportSaver, times(1)).createReport(any())

        whenever(passiveWifiAccessPointSource.getWifiAccessPoints()) doReturn
            listOf(
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("01:01:01:01:01:01")),
                    timestamp = 1000,
                ),
                EmitterObservation(
                    WifiAccessPoint(macAddress = MacAddress("ab:ab:ab:ab:ab:ab")),
                    timestamp = 1000,
                ),
            )
        whenever(passiveCellTowerSource.getCellTowers()) doReturn
            listOf(
                EmitterObservation(
                    emitter =
                        CellTower(
                            radioType = CellTower.RadioType.LTE,
                            mobileCountryCode = "001",
                            mobileNetworkCode = "01",
                            cellId = 1000,
                            locationAreaCode = 1000,
                            asu = null,
                            primaryScramblingCode = null,
                            serving = null,
                            timingAdvance = null,
                            arfcn = null,
                        ),
                    timestamp = 0,
                )
            )

        passiveScanReportCreator.createPassiveScanReport(
            listOf(
                PositionObservation(
                    position =
                        Position(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = 5.0,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                )
            )
        )
        verify(reportSaver, times(1)).createReport(any())
    }
}
