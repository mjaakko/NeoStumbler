package xyz.malkki.neostumbler.ui.composables

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.invalidationTrackerFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.extensions.getEstimatedSize
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.getTableNames
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.export.ExportCsvButton
import xyz.malkki.neostumbler.ui.composables.export.ExportDatabaseButton
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

private fun Flow<ReportDatabase>.dbSizeFlow(): Flow<Long> = flatMapLatest { db ->
    val tableNames = db.openHelper.readableDatabase.getTableNames()

    db.invalidationTrackerFlow(*tableNames.toTypedArray(), emitInitialState = true)
        .map {
            db.openHelper.readableDatabase.getEstimatedSize()
        }
}

private fun Flow<ReportDatabase>.selectableDates(): Flow<Set<LocalDate>> {
    return flatMapLatest { db ->
        db.reportDao()
            .getReportDates()
            .map { it.toSet() }
    }
}

@Composable
fun ManageStorage() {
    val context = LocalContext.current
    val reportDb = (context.applicationContext as StumblerApplication).reportDb

    val dbSize = remember(reportDb) { reportDb.dbSizeFlow() }.collectAsState(null)

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

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.import_data),
                style = MaterialTheme.typography.titleSmall
            )

            ImportDb()
        }
    }
}

@Composable
private fun DeleteReportsByDate(reportDb: StateFlow<ReportDatabase>) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showDatePicker = rememberSaveable {
        mutableStateOf(false)
    }

    val selectableDates = remember(reportDb) { reportDb.selectableDates() }
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
                    val to = dateRange.endInclusive.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()

                    coroutineScope.launch {
                        val deletedCount = reportDb.value.reportDao().deleteFromTimeRange(from, to)

                        context.showToast(context.getQuantityString(R.plurals.toast_deleted_reports, deletedCount, deletedCount))
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
private fun DeleteAllReportsButton(reportDb: StateFlow<ReportDatabase>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val showConfirmationDialog = rememberSaveable { mutableStateOf(false) }

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
                        reportDb.value.clearAllTables()
                    }

                    context.showToast(ContextCompat.getString(context, R.string.toast_deleted_all_reports))
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

@Composable
private fun ImportDb() {
    val context = LocalContext.current
    val coroutineContext = rememberCoroutineScope()

    val confirmationDialogOpen = rememberSaveable {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            //No file was chosen
            return@rememberLauncherForActivityResult
        }

        coroutineContext.launch {
            val tempDbFile = createTempFile(context.cacheDir.toPath(), "temp", ".db")

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    context.showToast(ContextCompat.getString(context, R.string.failed_to_open_selected_file))
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    inputStream.use { input ->
                        tempDbFile.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (ReportDatabaseManager.validateDatabase(context, tempDbFile)) {
                    val app = context.applicationContext as StumblerApplication

                    app.reportDatabaseManager.importDb(tempDbFile)

                    val reportCount = app.reportDb.value.reportDao().getReportCount().first()
                    context.showToast(context.getQuantityString(R.plurals.import_database_successful, reportCount, reportCount))
                } else {
                    context.showToast(ContextCompat.getString(context, R.string.import_database_not_valid))
                }
            } catch (_: Exception) {
                context.showToast(ContextCompat.getString(context, R.string.failed_to_open_selected_file))
            } finally {
                tempDbFile.deleteIfExists()
            }
        }
    }

    if (confirmationDialogOpen.value) {
        ConfirmationDialog(
            title = stringResource(R.string.import_raw_database),
            description = stringResource(R.string.import_raw_database_description),
            positiveButtonText = stringResource(R.string.ok),
            negativeButtonText = stringResource(R.string.cancel),
            onPositiveAction = {
                confirmationDialogOpen.value = false

                //Specifying correct mime type seems to cause DB files to not be selectable in some cases -> use wildcard
                launcher.launch("*/*")
            },
            onNegativeAction = {
                confirmationDialogOpen.value = false
            }
        )
    }

    Button(
        onClick = {
            confirmationDialogOpen.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.import_raw_database))
    }
}