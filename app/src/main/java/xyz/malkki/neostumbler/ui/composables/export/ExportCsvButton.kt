package xyz.malkki.neostumbler.ui.composables.export

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.export.CsvExportWorker
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog

@Composable
fun ExportCsvButton(reportProvider: ReportProvider = koinInject()) {
    val context = LocalContext.current

    val selectableDates = reportProvider.getReportDates().collectAsStateWithLifecycle(null)

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    val selectedDates = rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }

    val activityLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip"),
            onResult = { uri ->
                if (uri == null) {
                    context.showToast(
                        ContextCompat.getString(context, R.string.export_no_file_chosen)
                    )
                } else {
                    val dateFormat = DateFormat.getDateFormat(context)

                    val localTimeZone = ZoneId.systemDefault()

                    val fromDate = selectedDates.value!!.first
                    val toDate = selectedDates.value!!.second

                    val fromFormatted =
                        dateFormat.format(
                            Date.from(fromDate.atStartOfDay(localTimeZone).toInstant())
                        )
                    val toFormatted =
                        dateFormat.format(Date.from(toDate.atStartOfDay(localTimeZone).toInstant()))

                    context.showToast(
                        ContextCompat.getString(context, R.string.export_started)
                            .format(fromFormatted, toFormatted)
                    )

                    // Convert to local time
                    val from = fromDate.atStartOfDay(localTimeZone).toInstant().toEpochMilli()
                    val to =
                        toDate
                            // Add one day to include data for the last day in the selected range
                            .plusDays(1)
                            .atStartOfDay(localTimeZone)
                            .toInstant()
                            .toEpochMilli()

                    WorkManager.getInstance(context)
                        .enqueue(
                            OneTimeWorkRequest.Builder(CsvExportWorker::class.java)
                                .setInputData(
                                    Data.Builder()
                                        .putString(CsvExportWorker.INPUT_OUTPUT_URI, uri.toString())
                                        .putLong(CsvExportWorker.INPUT_FROM, from)
                                        .putLong(CsvExportWorker.INPUT_TO, to)
                                        .build()
                                )
                                .setConstraints(Constraints.NONE)
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                        )

                    selectedDates.value = null
                    dialogOpen.value = false
                }
            },
        )

    if (dialogOpen.value) {
        DateRangePickerDialog(
            title = stringResource(id = R.string.export_data),
            selectButtonText = stringResource(id = R.string.export_data),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                selectedDates.value = dateRange?.let { it.start to it.endInclusive }

                if (selectedDates.value != null) {
                    val fromFormatted =
                        selectedDates.value!!.first.format(DateTimeFormatter.BASIC_ISO_DATE)
                    val toFormatted =
                        selectedDates.value!!.second.format(DateTimeFormatter.BASIC_ISO_DATE)

                    activityLauncher.launch("neostumbler_export_${fromFormatted}_$toFormatted.zip")
                } else {
                    dialogOpen.value = false
                }
            },
        )
    }

    Button(enabled = true, onClick = { dialogOpen.value = true }) {
        Text(text = stringResource(id = R.string.export_csv))
    }
}
