package xyz.malkki.neostumbler.utils

import androidx.compose.material3.SelectableDates
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelectableDatesFromSetTest {
    private lateinit var selectableDates: SelectableDates

    @Before
    fun setup() {
        selectableDates = SelectableDatesFromSet { setOf(LocalDate.of(2000, 1, 1)) }
    }

    @Test
    fun `Test dates in the set can be selected`() {
        val dateMillis =
            LocalDate.of(2000, 1, 1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

        assertTrue(selectableDates.isSelectableDate(dateMillis))
    }

    @Test
    fun `Test dates not in the set can't be selected`() {
        val dateMillis =
            LocalDate.of(2000, 1, 2)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

        assertFalse(selectableDates.isSelectableDate(dateMillis))
    }
}
