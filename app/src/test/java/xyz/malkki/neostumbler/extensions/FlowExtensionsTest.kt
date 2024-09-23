package xyz.malkki.neostumbler.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class FlowExtensionsTest {
    @Test
    fun `Test flow buffer`() {
        runBlocking {
            val flow = flow {
                repeat(4) { i ->
                    delay(500.milliseconds)

                    emit(i)
                }
            }

            val list = flow.buffer(1100.milliseconds).toList()

            assertEquals(2, list.size)
            assertEquals(listOf(0, 1), list[0])
            assertEquals(listOf(2, 3), list[1])
        }
    }

    @Test
    fun `Test flow combineAny`() = runBlocking {
        val a = emptyFlow<Int>()
        val b = flow {
            emit(1)
            delay(1000)
            emit(2)
            delay(1000)
            emit(3)
        }
        val c = flow {
            emit(1)
        }

        val output = listOf(a, b, c)
            .combineAny {
                it
            }
            .toList()

        assertEquals(4, output.size)
        assertArrayEquals(arrayOf(null, 1, null), output[0])
    }

    @Test
    fun `Test parallel map`() = runBlocking {
        val flow = flow {
            repeat(10) {
                emit(Random.Default.nextDouble(0.0, Double.MAX_VALUE))
            }
        }

        val duration = withContext(Dispatchers.IO) {
            measureTime {
                flow
                    .parallelMap {
                        delay(100)
                        sqrt(it)
                    }
                    .collect {}
            }
        }

        assertTrue(duration <= 500.milliseconds)
    }

    @Test
    fun `Test throttle last`() = runBlocking {
        val flow = (1..30).toList().asFlow().onEach { delay(100) }

        val values = flow.throttleLast(1.seconds).toList()

        assertEquals(listOf(1, 10, 20, 30), values)
    }

    @Test
    fun `Test combineWithLatestFrom with an empty flow`() = runBlocking {
        val a = flowOf(1)
        val b = emptyFlow<Int>()

        val list = a.combineWithLatestFrom(b) { valueA, valueB -> valueA to valueB }.toList()

        assertEquals(1, list.size)
        assertEquals(1 to null, list.first())
    }

    @Test
    fun `Test combineWithLatestFrom`() = runBlocking {
        val a = flowOf(1, 2).onEach { delay(500) }
        val b = (1..30).toList().asFlow().onEach { delay(100) }

        val list = a.combineWithLatestFrom(b) { valueA, valueB -> valueA to valueB }.toList()

        assertEquals(2, list.size)
        assertEquals(9, list.last().second)
    }

    @Test
    fun `Test maxAge`() = runBlocking {
        val a = flow {
            emit(1)
            delay(2.seconds)
            emit(2)
        }

        val values = a.maxAge(1.seconds).toList()

        assertEquals(listOf(1, null, 2, null), values)
    }
}