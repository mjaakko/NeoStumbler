package xyz.malkki.neostumbler.geography

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.malkki.neostumbler.geography.Circle.Companion.isInside

class CircleTest {
    @Test
    fun `Test point inside circle`() {
        val circle =
            Circle(center = LatLng(latitude = 60.169520, longitude = 24.952269), radius = 500.0)

        assertTrue(circle.center.destination(250.0, 0.0).isInside(circle))

        assertFalse(circle.center.destination(1000.0, 0.0).isInside(circle))
    }
}
