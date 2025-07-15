package xyz.malkki.neostumbler.roomconverters

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class InstantConvertersTest {
    @Test
    fun `Test converting an Instant`() {
        val converter = InstantConverters()

        val a = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        val b = converter.toInstant(converter.fromInstant(a))

        assertEquals(a, b)
    }
}
