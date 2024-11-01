package xyz.malkki.neostumbler.ui.composables.export

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import xyz.malkki.neostumbler.export.CsvExportWorker
import xyz.malkki.neostumbler.ui.composables.DateRangePickerDialog
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
fun ExportCsvButton() {
    val context = LocalContext.current

    val selectableDates = getSelectableDatesSet(context).observeAsState()

    val dialogOpen = remember {
        mutableStateOf(false)
    }

    val selectedDates = remember {
        mutableStateOf<ClosedRange<LocalDate>?>(null)
    }

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri == null) {
                Toast.makeText(context, ContextCompat.getString(context, R.string.export_no_file_chosen), Toast.LENGTH_SHORT).show()
            } else {
                val dateFormat = DateFormat.getDateFormat(context)

                val localTimeZone = ZoneId.systemDefault()

                val fromDate = selectedDates.value!!.start
                val toDate = selectedDates.value!!.endInclusive

                val fromFormatted = dateFormat.format(Date.from(fromDate.atStartOfDay(localTimeZone).toInstant()))
                val toFormatted = dateFormat.format(Date.from(toDate.atStartOfDay(localTimeZone).toInstant()))

                Toast.makeText(context, ContextCompat.getString(context, R.string.export_started).format(fromFormatted, toFormatted), Toast.LENGTH_SHORT).show()

                //Convert to local time
                val from = fromDate.atStartOfDay(localTimeZone).toInstant().toEpochMilli()
                val to = toDate
                    //Add one day to include data for the last day in the selected range
                    .plusDays(1)
                    .atStartOfDay(localTimeZone)
                    .toInstant()
                    .toEpochMilli()

                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequest.Builder(CsvExportWorker::class.java)
                        .setInputData(Data.Builder()
                            .putString(CsvExportWorker.INPUT_OUTPUT_URI, uri.toString())
                            .putLong(CsvExportWorker.INPUT_FROM, from)
                            .putLong(CsvExportWorker.INPUT_TO, to)
                            .build())
                        .setConstraints(Constraints.NONE)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                )

                selectedDates.value = null
                dialogOpen.value = false
            }
        }
    )

    if (dialogOpen.value) {
        DateRangePickerDialog(
            title = stringResource(id = R.string.export_data),
            selectButtonText = stringResource(id = R.string.export_data),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                selectedDates.value = dateRange

                if (selectedDates.value != null) {
                    val fromFormatted =
                        selectedDates.value!!.start.format(DateTimeFormatter.BASIC_ISO_DATE)
                    val toFormatted =
                        selectedDates.value!!.endInclusive.format(DateTimeFormatter.BASIC_ISO_DATE)

                    activityLauncher.launch("neostumbler_export_${fromFormatted}_$toFormatted.zip")
                } else {
                    dialogOpen.value = false
                }
            }
        )
    }
    
    Button(
        enabled = true,
        onClick = {
            dialogOpen.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.export_csv))
    }
}