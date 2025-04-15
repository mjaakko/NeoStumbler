package xyz.malkki.neostumbler.ui.composables.managestorage

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.REPORT_DB_VERSION
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.extensions.getEstimatedSize
import xyz.malkki.neostumbler.extensions.getTableNames
import xyz.malkki.neostumbler.ui.composables.export.ExportCsvButton
import xyz.malkki.neostumbler.ui.composables.export.ExportDatabaseButton

private fun Flow<ReportDatabase>.dbSizeFlow(): Flow<Long> = flatMapLatest { db ->
    val tableNames = db.openHelper.readableDatabase.getTableNames()

    db.invalidationTracker.createFlow(*tableNames.toTypedArray(), emitInitialState = true).map {
        db.openHelper.readableDatabase.getEstimatedSize()
    }
}

@Composable
fun ManageStorage() {
    val context = LocalContext.current

    val reportDatabaseManager: ReportDatabaseManager = koinInject()
    val reportDb = reportDatabaseManager.reportDb

    val dbSize = remember(reportDb) { reportDb.dbSizeFlow() }.collectAsState(null)

    val dbSizeFormatted = dbSize.value?.let { Formatter.formatShortFileSize(context, it) } ?: "..."

    Column {
        Text(
            text = stringResource(id = R.string.db_size, dbSizeFormatted),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(id = R.string.db_schema_version, REPORT_DB_VERSION),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.delete_data),
                style = MaterialTheme.typography.titleSmall,
            )

            DeleteReportsByDate(reportDb = reportDb)

            DeleteReportsFromArea(reportDb = reportDb)

            DeleteAllReportsButton(reportDb = reportDb)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.export_data),
                style = MaterialTheme.typography.titleSmall,
            )

            ExportCsvButton()

            ExportDatabaseButton()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.import_data),
                style = MaterialTheme.typography.titleSmall,
            )

            ImportDb()
        }
    }
}
