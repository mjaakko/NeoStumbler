package xyz.malkki.neostumbler.ui.composables.managestorage

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportStorageMetadataProvider
import xyz.malkki.neostumbler.ui.composables.export.ExportCsvButton
import xyz.malkki.neostumbler.ui.composables.export.ExportDatabaseButton

@Composable
fun ManageStorage(reportStorageMetadataProvider: ReportStorageMetadataProvider = koinInject()) {
    val context = LocalContext.current

    val dbSize = reportStorageMetadataProvider.getEstimatedSize().collectAsStateWithLifecycle(null)

    val dbSizeFormatted = dbSize.value?.let { Formatter.formatShortFileSize(context, it) } ?: "..."

    Column {
        Text(
            text = stringResource(id = R.string.db_size, dbSizeFormatted),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text =
                stringResource(
                    id = R.string.db_schema_version,
                    reportStorageMetadataProvider.getSchemaVersion(),
                ),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = stringResource(id = R.string.delete_data),
                style = MaterialTheme.typography.titleSmall,
            )

            DeleteReportsByDate()

            DeleteReportsFromArea()

            DeleteAllReportsButton()
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
