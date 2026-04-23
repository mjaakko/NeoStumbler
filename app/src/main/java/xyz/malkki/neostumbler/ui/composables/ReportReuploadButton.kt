package xyz.malkki.neostumbler.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaReportUploadStarter
import xyz.malkki.neostumbler.ui.composables.reports.ToastOnReportUpload
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog

@Composable
fun ReportReuploadButton(
    reportProvider: ReportProvider = koinInject(),
    ichnaeaReportUploadStarter: IchnaeaReportUploadStarter = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()

    val enqueuedUploadWork = rememberSaveable { mutableStateOf<UUID?>(null) }

    ToastOnReportUpload(workId = enqueuedUploadWork)

    val selectableDates = reportProvider.getReportDates().collectAsStateWithLifecycle(null)

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        DateRangePickerDialog(
            title = stringResource(id = R.string.reupload_data),
            selectButtonText = stringResource(id = R.string.reupload_data),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                dialogOpen.value = false

                if (dateRange != null) {
                    val localTimeZone = ZoneId.systemDefault()

                    // Convert to local time
                    val from = dateRange.start.atStartOfDay(localTimeZone).toInstant()
                    val to =
                        dateRange.endInclusive
                            // Add one day to include data for the last day in the selected range
                            .plusDays(1)
                            .atStartOfDay(localTimeZone)
                            .toInstant()

                    coroutineScope.launch {
                        enqueuedUploadWork.value =
                            ichnaeaReportUploadStarter.startReuploadReports(from, to)
                    }
                }
            },
        )
    }

    Button(enabled = true, onClick = { dialogOpen.value = true }) {
        Text(text = stringResource(id = R.string.reupload_data))
    }
}
