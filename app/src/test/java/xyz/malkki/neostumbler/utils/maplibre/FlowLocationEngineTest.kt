package xyz.malkki.neostumbler.utils.maplibre

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.observation.PositionObservation

class FlowLocationEngineTest {
    @Test
    fun `Get last location from replay cache`() = runTest {
        val flow = MutableSharedFlow<PositionObservation>(replay = 1)
        flow.emit(
            PositionObservation(
                position =
                    Position(
                        latitude = 30.091234,
                        longitude = 31.210964,
                        source = Position.Source.GPS,
                    ),
                timestamp = 0,
            )
        )

        val locationEngine: LocationEngine = FlowLocationEngine(flow, backgroundScope)

        var locationEngineResult: LocationEngineResult? = null

        locationEngine.getLastLocation(
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult?) {
                    locationEngineResult = result
                }

                override fun onFailure(ex: Exception) {}
            }
        )

        assertNotNull(locationEngineResult)
        assertNotNull(locationEngineResult!!.lastLocation)
    }

    @Test
    fun `Receive location updates from the flow`() = runTest {
        val flow = MutableSharedFlow<PositionObservation>()

        val locationEngine: LocationEngine = FlowLocationEngine(flow, backgroundScope)

        val locationEngineResults: MutableList<LocationEngineResult> = mutableListOf()

        val callback =
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    locationEngineResults.add(result)
                }

                override fun onFailure(ex: Exception) {}
            }

        locationEngine.requestLocationUpdates(
            LocationEngineRequest.Builder(0).build(),
            callback,
            null,
        )

        flow.subscriptionCount.first { it > 0 }

        flow.emit(
            PositionObservation(
                position =
                    Position(
                        latitude = 30.091234,
                        longitude = 31.210964,
                        source = Position.Source.GPS,
                    ),
                timestamp = 0,
            )
        )

        flow.emit(
            PositionObservation(
                position =
                    Position(
                        latitude = -34.629426,
                        longitude = -58.395057,
                        source = Position.Source.GPS,
                    ),
                timestamp = 1000,
            )
        )

        locationEngine.removeLocationUpdates(callback)

        assertEquals(2, locationEngineResults.size)
    }
}
