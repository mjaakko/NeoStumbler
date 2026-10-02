package xyz.malkki.neostumbler.ui.composables.export

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.export.CsvExportManager
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog

@Composable
fun ExportCsvButton(
    reportProvider: ReportProvider = koinInject(),
    csvExportManager: CsvExportManager = koinInject(),
) {
    val context = LocalContext.current

    val selectableDates = reportProvider.getReportDates().collectAsStateWithLifecycle(null)

    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    var selectedDates by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }

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

                    val fromDate = selectedDates!!.first
                    val toDate = selectedDates!!.second

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
                    val from = fromDate.atStartOfDay(localTimeZone).toInstant()
                    val to =
                        toDate
                            // Add one day to include data for the last day in the selected range
                            .plusDays(1)
                            .atStartOfDay(localTimeZone)
                            .toInstant()

                    csvExportManager.startExport(
                        fromInstant = from,
                        toInstant = to,
                        outputFile = uri.toString(),
                    )

                    selectedDates = null
                    dialogOpen = false
                }
            },
        )

    if (dialogOpen) {
        DateRangePickerDialog(
            title = stringResource(id = R.string.export_data),
            selectButtonText = stringResource(id = R.string.export_data),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                selectedDates = dateRange?.let { it.start to it.endInclusive }

                val fileName = selectedDates?.let { (from, to) ->
                    val fromFormatted = from.format(DateTimeFormatter.BASIC_ISO_DATE)
                    val toFormatted = to.format(DateTimeFormatter.BASIC_ISO_DATE)

                    "neostumbler_export_${fromFormatted}_$toFormatted.zip"
                }

                if (fileName != null) {
                    activityLauncher.launch(fileName)
                } else {
                    dialogOpen = false
                }
            },
        )
    }

    Button(enabled = true, onClick = { dialogOpen = true }) {
        Text(text = stringResource(id = R.string.export_csv))
    }
}
