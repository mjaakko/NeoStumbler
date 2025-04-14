package xyz.malkki.neostumbler.scanner.movement

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.Position.Source

class LocationBasedMovementDetectorTest {
    @Test
    fun `Test that no movement is detected when the location does not change`() {
        val locationFlow =
            flowOf(
                Position(latitude = 0.0, longitude = 0.0, source = Source.GPS, timestamp = 0),
                Position(latitude = 0.0, longitude = 0.0, source = Source.GPS, timestamp = 0),
            )

        val movementDetector =
            LocationBasedMovementDetector(notMovingDelay = 0.seconds) { locationFlow }

        runBlocking {
            val isMoving = movementDetector.getIsMovingFlow().toList()

            assertTrue(isMoving.isNotEmpty())
            assertFalse(isMoving.last())
        }
    }

    @Test
    fun `Test that movement is detected when the location changes`() {
        val locationFlow =
            flowOf(
                Position(latitude = 0.0, longitude = 0.0, source = Source.GPS, timestamp = 0),
                Position(latitude = 10.0, longitude = 10.0, source = Source.GPS, timestamp = 0),
            )

        val movementDetector =
            LocationBasedMovementDetector(notMovingDelay = 0.seconds) { locationFlow }

        runBlocking {
            val isMoving = movementDetector.getIsMovingFlow().toList()

            assertTrue(isMoving.isNotEmpty())
            isMoving.forEach { assertTrue(it) }
        }
    }
}
