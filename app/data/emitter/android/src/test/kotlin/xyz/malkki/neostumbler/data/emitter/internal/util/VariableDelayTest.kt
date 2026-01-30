package xyz.malkki.neostumbler.data.emitter.internal.util

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableDelayTest {
    @Test
    fun `Test variable delay with constant duration`() = runTest {
        val delay =
            testScheduler.timeSource.measureTime {
                delayWithMinDuration(
                    testScheduler.currentTime,
                    { testScheduler.currentTime },
                    flowOf(1.seconds),
                )
            }

        // Use 990.milliseconds instead of 1.seconds to avoid timing differences due to system
        // performance
        assertTrue(
            "Expected delay to be at least 1 second (was ${delay.toString(DurationUnit.SECONDS)})",
            delay >= 990.milliseconds,
        )
    }

    @Test
    fun `Test variable delay with decreasing duration`() = runTest {
        val durationFlow = flow {
            emit(1.days)

            delay(200)

            emit(1.hours)

            delay(200)

            emit(1.seconds)
        }

        val delay =
            testScheduler.timeSource.measureTime {
                delayWithMinDuration(
                    testScheduler.currentTime,
                    { testScheduler.currentTime },
                    durationFlow,
                )
            }

        // Use 990.milliseconds instead of 1.seconds to avoid timing differences due to system
        // performance
        assertTrue(
            "Expected delay to be at least 1 second (was ${delay.toString(DurationUnit.MILLISECONDS)})",
            delay >= 990.milliseconds,
        )
    }

    @Test
    fun `Test variable delay with increasing duration`() = runTest {
        val durationFlow = flow {
            emit(1.seconds)

            delay(200)

            emit(1.hours)

            delay(200)

            emit(1.days)
        }

        val delay =
            testScheduler.timeSource.measureTime {
                delayWithMinDuration(
                    testScheduler.currentTime,
                    { testScheduler.currentTime },
                    durationFlow,
                )
            }

        // Use 990.milliseconds instead of 1.seconds to avoid timing differences due to system
        // performance
        assertTrue(
            "Expected delay to be at least 1 second (was ${delay.toString(DurationUnit.SECONDS)})",
            delay >= 990.milliseconds,
        )
    }
}
