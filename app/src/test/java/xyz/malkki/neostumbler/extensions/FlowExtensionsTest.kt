package xyz.malkki.neostumbler.extensions

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowExtensionsTest {

    @Test
    fun `Test maxAge`() = runTest {
        val a = flow {
            emit(1)
            delay(2.seconds)
            emit(2)
        }

        val values = a.maxAge(1.seconds).toList()

        assertEquals(listOf(1, null, 2, null), values)
    }
}
