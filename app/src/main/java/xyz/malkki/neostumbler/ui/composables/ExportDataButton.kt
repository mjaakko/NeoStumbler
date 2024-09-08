package xyz.malkki.neostumbler.ui.composables

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.export.DataExportWorker
import xyz.malkki.neostumbler.extensions.selectedDateRange
import xyz.malkki.neostumbler.utils.SelectableDatesFromSet
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private fun getSelectableDatesSet(context: Context): LiveData<Set<LocalDate>> {
    return (context.applicationContext as StumblerApplication).reportDb.reportDao()
        .getReportDates()
        .map { it.toSet() }
}

@Composable
fun ExportDataButton() {
    val context = LocalContext.current

    val selectableDates = getSelectableDatesSet(context).observeAsState()

    val dialogOpen = remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(selectableDates = SelectableDatesFromSet(selectableDates::value))
    
    val selectedDates = dateRangePickerState.selectedDateRange()

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri == null) {
                Toast.makeText(context, ContextCompat.getString(context, R.string.export_no_file_chosen), Toast.LENGTH_SHORT).show()
            } else {
                val dateFormat = DateFormat.getDateFormat(context)

                val localTimeZone = ZoneId.systemDefault()

                val fromFormatted = dateFormat.format(Date.from(selectedDates!!.start.atStartOfDay(localTimeZone).toInstant()))
                val toFormatted = dateFormat.format(Date.from(selectedDates.endInclusive.atStartOfDay(localTimeZone).toInstant()))

                Toast.makeText(context, ContextCompat.getString(context, R.string.export_started).format(fromFormatted, toFormatted), Toast.LENGTH_SHORT).show()

                //Convert to local time
                val from = selectedDates.start.atStartOfDay(localTimeZone).toInstant().toEpochMilli()
                val to = selectedDates.endInclusive
                    //Add one day to include data for the last day in the selected range
                    .plusDays(1)
                    .atStartOfDay(localTimeZone)
                    .toInstant()
                    .toEpochMilli()

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequest.Builder(DataExportWorker::class.java)
                        .setInputData(Data.Builder()
                            .putString(DataExportWorker.INPUT_OUTPUT_URI, uri.toString())
                            .putLong(DataExportWorker.INPUT_FROM, from)
                            .putLong(DataExportWorker.INPUT_TO, to)
                            .build())
                        .setConstraints(Constraints.NONE)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                )

                dialogOpen.value = false
            }
        }
    )

    if (dialogOpen.value) {
        DatePickerDialog(
            onDismissRequest = {
                dialogOpen.value = false
            },
            confirmButton = {
                Button(
                    enabled = selectedDates != null,
                    onClick = {
                        val fromFormatted = selectedDates!!.start.format(DateTimeFormatter.BASIC_ISO_DATE)
                        val toFormatted = selectedDates.endInclusive.format(DateTimeFormatter.BASIC_ISO_DATE)

                        activityLauncher.launch("neostumbler_export_${fromFormatted}_$toFormatted.zip")
                    }
                ) {
                    Text(stringResource(id = R.string.export_data))
                }
            }) {
            Column {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 12.dp, 12.dp, 8.dp),
                    text = stringResource(id = R.string.export_data)
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
                        dateFormatter = DatePickerDefaults.dateFormatter(selectedDateSkeleton = "d/MM/yyyy"),
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
                                    dateFormatter = DatePickerDefaults.dateFormatter(selectedDateSkeleton = "d/MM/yyyy"),
                                    modifier = Modifier.scale(0.9f)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
    
    Button(
        enabled = true,
        onClick = {
            dialogOpen.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.export_data))
    }
}