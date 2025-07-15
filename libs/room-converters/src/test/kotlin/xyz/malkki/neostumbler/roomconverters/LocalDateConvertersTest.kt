package xyz.malkki.neostumbler.roomconverters

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDateConvertersTest {
    @Test
    fun `Test converting a LocalDate`() {
        val converter = LocalDateConverters()

        val a = LocalDate.now()

        val b = converter.toLocalDate(converter.fromLocalDate(a))

        assertEquals(a, b)
    }
}
