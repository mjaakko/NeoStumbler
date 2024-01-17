package xyz.malkki.neostumbler.extensions

import androidx.compose.material3.DateRangePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Returns the selected date range if both start and end date are selected
 *
 * @return The selected date range or null if either start or end date was not selected
 */
fun DateRangePickerState.selectedDateRange(): ClosedRange<LocalDate>? {
    if (selectedStartDateMillis == null || selectedEndDateMillis == null) {
        return null
    }

    val from = Instant.ofEpochMilli(selectedStartDateMillis!!).atOffset(ZoneOffset.UTC).toLocalDate()
    val to = Instant.ofEpochMilli(selectedEndDateMillis!!).atOffset(ZoneOffset.UTC).toLocalDate()

    return from..to
}