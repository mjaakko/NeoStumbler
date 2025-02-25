package xyz.malkki.neostumbler.ui.composables.managestorage

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog

private fun Flow<ReportDatabase>.selectableDates(): Flow<Set<LocalDate>> {
    return flatMapLatest { db -> db.reportDao().getReportDates().map { it.toSet() } }
}

@Composable
fun DeleteReportsByDate(reportDb: StateFlow<ReportDatabase>) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showDatePicker = rememberSaveable { mutableStateOf(false) }

    val selectableDates =
        remember(reportDb) { reportDb.selectableDates() }
            .collectAsStateWithLifecycle(initialValue = null)

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
                        val deletedCount = reportDb.value.reportDao().deleteFromTimeRange(from, to)

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
