package xyz.malkki.neostumbler.report.postprocessor

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.someBluetoothBeacon
import xyz.malkki.neostumbler.core.observation.someEmitterObservation
import xyz.malkki.neostumbler.core.report.someReportData
import xyz.malkki.neostumbler.core.somePosition
import xyz.malkki.neostumbler.core.somePositionObservation
import xyz.malkki.neostumbler.core.values.SignalStrength
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.LatLng

class RestrictedAreaFiltererTest {
    private val report =
        someReportData(
            position =
                somePositionObservation(
                    position =
                        somePosition(
                            latitude = 60.171047,
                            longitude = 24.941471,
                        )
                ),
            bluetoothBeacons =
                listOf(
                    someEmitterObservation(
                        emitter =
                            someBluetoothBeacon(
                                macAddress = MacAddress("ff:ff:ff:ff:ff:ff"),
                                signalStrength = SignalStrength(-80),
                            )
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
