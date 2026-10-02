package xyz.malkki.neostumbler.ui.composables.managestorage

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportRemover
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog

@Composable
fun DeleteReportsByDate(
    reportProvider: ReportProvider = koinInject(),
    reportRemover: ReportRemover = koinInject(),
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showDatePicker = rememberSaveable { mutableStateOf(false) }

    val selectableDates =
        reportProvider.getReportDates().collectAsStateWithLifecycle(initialValue = null)

    if (showDatePicker.value) {
        DateRangePickerDialog(
            title = stringResource(R.string.delete_reports_by_date),
            selectButtonText = stringResource(R.string.delete),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                showDatePicker.value = false

                if (dateRange != null) {
                    val from = dateRange.start.atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()
                    val to =
                        dateRange.endInclusive
                            .plusDays(1)
                            .atStartOfDay()
                            .atOffset(ZoneOffset.UTC)
                            .toInstant()

                    coroutineScope.launch {
                        val deletedCount = reportRemover.deleteByDate(from, to)

                        context.showToast(
                            context.getQuantityString(
                                R.plurals.toast_deleted_reports,
                                deletedCount,
                                deletedCount,
                            )
                        )
                    }
                }
            },
        )
    }

    Button(onClick = { showDatePicker.value = true }) {
        Text(text = stringResource(id = R.string.delete_reports_by_date))
    }
}
