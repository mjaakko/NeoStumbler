package xyz.malkki.neostumbler.scanner.movement

import android.location.Location
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

class LocationBasedMovementDetectorTest {
    @Test
    fun `Test that no movement is detected when the location does not change`() {
        val mockLocation = mock<Location> {
            on { distanceTo(any()) } doReturn 0f
            on { altitude } doReturn 0.0
        }

        val locationFlow = flowOf(
            mockLocation,
            mockLocation
        )
        val movementDetector = LocationBasedMovementDetector(notMovingDelay = 0.seconds) {
            locationFlow
        }

        runBlocking {
            val isMoving = movementDetector.getIsMovingFlow().toList()

            assertTrue(isMoving.isNotEmpty())
            assertFalse(isMoving.last())
        }
    }

    @Test
    fun `Test that movement is detected when the location changes`() {
        val mockLocation = mock<Location> {
            on { distanceTo(any()) } doReturn 100f
            on { altitude } doReturn 0.0
        }

        val locationFlow = flowOf(
            mockLocation,
            mockLocation
        )
        val movementDetector = LocationBasedMovementDetector(notMovingDelay = 0.seconds) {
            locationFlow
        }

        runBlocking {
            val isMoving = movementDetector.getIsMovingFlow().toList()

            assertTrue(isMoving.isNotEmpty())
            isMoving.forEach {
                assertTrue(it)
            }
        }
    }
}