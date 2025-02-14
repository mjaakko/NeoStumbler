package xyz.malkki.neostumbler.utils

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableDelayTest {
    private val timeSource = { System.nanoTime() / 1_000_000 }

    @Test
    fun `Test variable delay with constant duration`() = runBlocking {
        val delay = measureTime {
            delayWithMinDuration(timeSource.invoke(), timeSource, flowOf(1.seconds))
        }

        // Use 990.milliseconds instead of 1.seconds to avoid timming differences due to system
        // performance
        assertTrue(
            "Expected delay to be at least 1 second (was ${delay.toString(DurationUnit.SECONDS)})",
            delay >= 990.milliseconds,
        )
    }

    @Test
    fun `Test variable delay with decreasing duration`() = runBlocking {
        val durationFlow = flow {
            emit(1.days)

            delay(200)

            emit(1.hours)

            delay(200)

            emit(1.seconds)
        }

        val delay = measureTime {
            delayWithMinDuration(timeSource.invoke(), timeSource, durationFlow)
        }

        // Use 990.milliseconds instead of 1.seconds to avoid timming differences due to system
        // performance
        assertTrue(
            "Expected delay to be at least 1 second (was ${delay.toString(DurationUnit.MILLISECONDS)})",
            delay >= 990.milliseconds,
        )
    }

    @Test
    fun `Test variable delay with increasing duration`() = runBlocking {
        val durationFlow = flow {
            emit(1.seconds)

            delay(200)

            emit(1.hours)

            delay(200)

            emit(1.days)
        }

        val delay = measureTime {
            delayWithMinDuration(timeSource.invoke(), timeSource, durationFlow)
        }

        // Use 990.milliseconds instead of 1.seconds to avoid timming differences due to system
        // performance
        assertTrue(
            "Expected delay to be at least 1 second (was ${delay.toString(DurationUnit.SECONDS)})",
            delay >= 990.milliseconds,
        )
    }
}
