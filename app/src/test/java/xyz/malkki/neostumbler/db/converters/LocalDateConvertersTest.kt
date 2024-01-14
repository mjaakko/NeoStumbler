package xyz.malkki.neostumbler.db.converters

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LocalDateConvertersTest {
    @Test
    fun `Test converting a LocalDate`() {
        val converter = LocalDateConverters()

        val a = LocalDate.now()

        val b = converter.toLocalDate(converter.fromLocalDate(a))

        assertEquals(a, b)
    }
}