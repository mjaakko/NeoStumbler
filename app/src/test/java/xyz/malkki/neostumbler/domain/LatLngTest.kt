package xyz.malkki.neostumbler.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LatLngTest {
    @Test
    fun `Test calculating distance between coordinates`() {
        val statueOfLiberty = LatLng(latitude = 40.689100, longitude = -74.044300)

        val jfkAirport = LatLng(latitude = 40.645577, longitude = -73.784351)

        assertEquals(22452.3, statueOfLiberty.distanceTo(jfkAirport), 0.1)
    }

    @Test
    fun `Test calculating destination`() {
        val statueOfLiberty = LatLng(latitude = 40.689100, longitude = -74.044300)

        val destination = statueOfLiberty.destination(distance = 4970.0, bearing = 41.5)

        val canalStreetStation =
            LatLng(latitude = 40.722568951247304, longitude = -74.00522157851157)

        assertEquals(canalStreetStation, destination)
        assertEquals(4970.0, statueOfLiberty.distanceTo(destination), 0.001)
    }
}
