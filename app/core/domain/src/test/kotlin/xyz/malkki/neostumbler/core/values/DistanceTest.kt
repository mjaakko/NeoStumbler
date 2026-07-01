package xyz.malkki.neostumbler.core.values

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {
    @Test
    fun `Calculate duration`() {
        val speed = Speed(300.0)
        val distance = Distance(450.0)

        assertEquals(1.5.seconds, distance / speed)
    }
}
