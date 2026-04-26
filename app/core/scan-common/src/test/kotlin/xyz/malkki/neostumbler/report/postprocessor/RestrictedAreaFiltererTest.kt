package xyz.malkki.neostumbler.report.postprocessor

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.LatLng

class RestrictedAreaFiltererTest {
    private val report =
        ReportData(
            position =
                PositionObservation(
                    position =
                        Position(
                            latitude = 60.171047,
                            longitude = 24.941471,
                            source = Position.Source.GPS,
                        ),
                    timestamp = 0,
                ),
            cellTowers = emptyList(),
            wifiAccessPoints = emptyList(),
            bluetoothBeacons =
                listOf(
                    EmitterObservation(
                        emitter =
                            BluetoothBeacon(
                                macAddress = MacAddress("ff:ff:ff:ff:ff:ff"),
                                signalStrength = -80,
                            ),
                        timestamp = 0,
                    )
                ),
        )

    @Test
    fun `Report is created when there is no restricted areas`() = runTest {
        val filterer = RestrictedAreaFilterer(restrictedAreasProvider = { emptyList() })

        assertNotNull(filterer.postProcessReport(report))
    }

    @Test
    fun `Report is not created when inside a restricted area`() = runTest {
        val filterer =
            RestrictedAreaFilterer(
                restrictedAreasProvider = {
                    listOf(
                        Circle(
                            center = LatLng(latitude = 60.17105, longitude = 24.94148),
                            radius = 1000.0,
                        )
                    )
                }
            )

        assertNull(filterer.postProcessReport(report))
    }

    @Test
    fun `Report is created when not inside a restricted area`() = runTest {
        val filterer =
            RestrictedAreaFilterer(
                restrictedAreasProvider = {
                    listOf(
                        Circle(
                            center = LatLng(latitude = 74.54256, longitude = -12.14316),
                            radius = 1000.0,
                        )
                    )
                }
            )

        assertNotNull(filterer.postProcessReport(report))
    }
}
