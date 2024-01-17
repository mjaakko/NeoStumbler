package xyz.malkki.neostumbler.db.converters

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class InstantConvertersTest {
    @Test
    fun `Test converting an Instant`() {
        val converter = InstantConverters()

        val a = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        val b = converter.toInstant(converter.fromInstant(a))

        assertEquals(a, b)
    }
}