package xyz.malkki.neostumbler.utils

import androidx.compose.material3.SelectableDates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Implementation of [SelectableDates] which only allows selecting dates that are present in the set
 *
 * @param dateSetProvider Function which returns the set of selectable dates
 */
class SelectableDatesFromSet(private val dateSetProvider: () -> Set<LocalDate>?) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val dates = dateSetProvider.invoke() ?: emptySet()

        return Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC).toLocalDate() in dates
    }
}
