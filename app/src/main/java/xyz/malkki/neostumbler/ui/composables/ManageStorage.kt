package xyz.malkki.neostumbler.ui.composables

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.map
import androidx.room.invalidationTrackerFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.extensions.getEstimatedSize
import xyz.malkki.neostumbler.extensions.getTableNames
import xyz.malkki.neostumbler.ui.composables.export.ExportCsvButton
import xyz.malkki.neostumbler.ui.composables.export.ExportDatabaseButton
import java.time.LocalDate
import java.time.ZoneOffset

private fun ReportDatabase.dbSizeFlow(): Flow<Long> = channelFlow {
    val tableNames = openHelper.readableDatabase.getTableNames()

    invalidationTrackerFlow(*tableNames.toTypedArray(), emitInitialState = true)
        .map {
            openHelper.readableDatabase.getEstimatedSize()
        }
        .collect(::send)
}

private fun ReportDatabase.selectableDates(): LiveData<Set<LocalDate>> {
    return reportDao()
        .getReportDates()
        .map { it.toSet() }
}

@Composable
fun ManageStorage() {
    val context = LocalContext.current
    val reportDb = (context.applicationContext as StumblerApplication).reportDb

    val dbSize = reportDb.dbSizeFlow().collectAsStateWithLifecycle(initialValue = null)

    val dbSizeFormatted = dbSize.value?.let { Formatter.formatShortFileSize(context, it) } ?: "..."

    Column {
        Text(
            text = stringResource(id = R.string.db_size, dbSizeFormatted),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.delete_data),
                style = MaterialTheme.typography.titleSmall
            )

            DeleteReportsByDate(reportDb = reportDb)

            DeleteAllReportsButton(reportDb = reportDb)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.export_data),
                style = MaterialTheme.typography.titleSmall
            )

            ExportCsvButton()

            ExportDatabaseButton()
        }
    }
}

@Composable
private fun DeleteReportsByDate(reportDb: ReportDatabase) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showDatePicker = remember {
        mutableStateOf(false)
    }

    val selectableDates = reportDb.selectableDates().observeAsState(initial = null)

    if (showDatePicker.value) {
        DateRangePickerDialog(
            title = stringResource(R.string.delete_reports_by_date),
            selectButtonText = stringResource(R.string.delete),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                showDatePicker.value = false

                if (dateRange != null) {
                    val from = dateRange.start.atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()
                    val to = dateRange.endInclusive.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()

                    coroutineScope.launch {
                        val deletedCount = reportDb.reportDao().deleteFromTimeRange(from, to)

                        Toast.makeText(context, ContextCompat.getString(context, R.string.toast_deleted_reports).format(deletedCount), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Button(
        onClick = {
            showDatePicker.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.delete_reports_by_date))
    }
}

@Composable
private fun DeleteAllReportsButton(reportDb: ReportDatabase) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val showConfirmationDialog = remember { mutableStateOf(false) }

    if (showConfirmationDialog.value) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_all_reports),
            description = stringResource(R.string.delete_all_reports_confirmation),
            positiveButtonText = stringResource(R.string.yes),
            negativeButtonText = stringResource(R.string.no),
            onPositiveAction = {
                showConfirmationDialog.value = false

                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        reportDb.clearAllTables()
                    }

                    Toast.makeText(context, ContextCompat.getString(context, R.string.toast_deleted_all_reports), Toast.LENGTH_SHORT).show()
                }
            },
            onNegativeAction = {
                showConfirmationDialog.value = false
            }
        )
    }

    Button(
        onClick = {
            showConfirmationDialog.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.delete_all_reports))
    }
}