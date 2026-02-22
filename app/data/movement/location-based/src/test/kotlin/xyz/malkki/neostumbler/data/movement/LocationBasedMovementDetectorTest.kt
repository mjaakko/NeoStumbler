package xyz.malkki.neostumbler.data.movement

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.data.location.LocationSource

class LocationBasedMovementDetectorTest {
    @Test
    fun `Test that no movement is detected when the location does not change`() = runTest {
        val locationFlow =
            flowOf(
                    Position(latitude = 0.0, longitude = 0.0, source = Position.Source.GPS),
                    Position(latitude = 0.0, longitude = 0.0, source = Position.Source.GPS),
                )
                .map { PositionObservation(it, 0) }

        val movementDetector =
            LocationBasedMovementDetector(
                notMovingDelay = 0.seconds,
                locationSourceProvider = { LocationSource { _, _ -> locationFlow } },
            )

        val isMoving = movementDetector.getIsMovingFlow().toList()

        assertTrue(isMoving.isNotEmpty())
        assertFalse(isMoving.last())
    }

    @Test
    fun `Test that movement is detected when the location changes`() = runTest {
        val locationFlow =
            flowOf(
                    Position(latitude = 0.0, longitude = 0.0, source = Position.Source.GPS),
                    Position(latitude = 10.0, longitude = 10.0, source = Position.Source.GPS),
                )
                .map { PositionObservation(it, 0) }

        val movementDetector =
            LocationBasedMovementDetector(
                notMovingDelay = 0.seconds,
                locationSourceProvider = { LocationSource { _, _ -> locationFlow } },
            )

        val isMoving = movementDetector.getIsMovingFlow().toList()

        assertTrue(isMoving.isNotEmpty())
        isMoving.forEach { assertTrue(it) }
    }
}
