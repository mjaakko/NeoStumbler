package xyz.malkki.neostumbler.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

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
}