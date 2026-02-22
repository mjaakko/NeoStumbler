package xyz.malkki.neostumbler.activescan.internal

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowExtensionsTest {

    @Test
    fun `Test combineWithLatestFrom with an empty flow`() = runTest {
        val a = flowOf(1)
        val b = emptyFlow<Int>()

        val list = a.combineWithLatestFrom(b) { valueA, valueB -> valueA to valueB }.toList()

        assertEquals(1, list.size)
        assertEquals(1 to null, list.first())
    }

    @Test
    fun `Test combineWithLatestFrom`() = runTest {
        val a = flowOf(1, 2).onEach { delay(500) }
        val b = (1..30).toList().asFlow().onEach { delay(100) }

        val list = a.combineWithLatestFrom(b) { valueA, valueB -> valueA to valueB }.toList()

        assertEquals(2, list.size)
        assertEquals(9, list.last().second)
    }

    @Test
    fun `Test pairwise`() = runTest {
        val flow = flowOf("a", "b", "c")

        val list = flow.pairwise().toList()
        assertEquals(listOf("a" to "b", "b" to "c"), list)
    }
}
