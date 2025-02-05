package xyz.malkki.neostumbler.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LatLngTest {
    @Test
    fun `Test calculating distance between coordinates`() {
        val statueOfLiberty = LatLng(
            latitude = 40.689100,
            longitude = -74.044300
        )

        val jfkAirport = LatLng(
            latitude = 40.645577,
            longitude = -73.784351
        )

        assertEquals(22452.3, statueOfLiberty.distanceTo(jfkAirport), 0.1)
    }
}