package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.utils.SelectableDatesFromSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val DATE_FORMATTER = DatePickerDefaults.dateFormatter(selectedDateSkeleton = "d/MM/yyyy")

/**
 * @param onDatesSelected Callback for when the button is clicked. If no dates have been selected, the parameter is null
 */
@Composable
fun DateRangePickerDialog(
    title: String,
    selectButtonText: String,
    selectableDates: State<Set<LocalDate>?>,
    onDatesSelected: (ClosedRange<LocalDate>?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(selectableDates = SelectableDatesFromSet(selectableDates::value))

    LaunchedEffect(
        dateRangePickerState.selectedStartDateMillis,
        dateRangePickerState.selectedEndDateMillis,
        selectableDates.value
    ) {
        if (selectableDates.value?.size == 1 && dateRangePickerState.selectedStartDateMillis != null) {
            //If there is only one day of data, automatically select the end date
            dateRangePickerState.setSelection(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedStartDateMillis)
        }
    }

    DatePickerDialog(
        onDismissRequest = {
            onDatesSelected(null)
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDatesSelected(null)
                }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = dateRangePickerState.isValid,
                onClick = {
                    onDatesSelected(dateRangePickerState.selectedDateRange())
                }
            ) {
                Text(text = selectButtonText)
            }
        }
    ) {
        Column {
            Text(
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(24.dp, 24.dp, 12.dp, 8.dp),
                text = title
            )
            if (selectableDates.value == null) {
                Box(
                    modifier = Modifier.height(400.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.height(400.dp),
                    dateFormatter = DATE_FORMATTER,
                    headline = {
                        //Wrap headline in a box to center the text on a single line
                        Box(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            DateRangePickerDefaults.DateRangePickerHeadline(
                                selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                                selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                                displayMode = dateRangePickerState.displayMode,
                                dateFormatter = DATE_FORMATTER,
                                modifier = Modifier.scale(0.9f)
                            )
                        }
                    }
                )
            }
        }
    }
}

private val DateRangePickerState.isValid: Boolean
    get() = selectedStartDateMillis != null && selectedEndDateMillis != null

/**
 * Returns the selected date range if both start and end date are selected
 *
 * @return The selected date range or null if either start or end date was not selected
 */
private fun DateRangePickerState.selectedDateRange(): ClosedRange<LocalDate>? {
    if (!isValid) {
        return null
    }

    val from = Instant.ofEpochMilli(selectedStartDateMillis!!).atOffset(ZoneOffset.UTC).toLocalDate()
    val to = Instant.ofEpochMilli(selectedEndDateMillis!!).atOffset(ZoneOffset.UTC).toLocalDate()

    return from..to
}