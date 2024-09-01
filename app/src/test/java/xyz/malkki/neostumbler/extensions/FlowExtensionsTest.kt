package xyz.malkki.neostumbler.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
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
}