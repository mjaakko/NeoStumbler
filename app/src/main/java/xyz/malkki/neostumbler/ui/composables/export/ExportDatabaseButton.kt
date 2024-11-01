package xyz.malkki.neostumbler.ui.composables.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.export.DatabaseExportWorker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val DB_MIME_TYPE = "application/vnd.sqlite3"

@Composable
fun ExportDatabaseButton() {
    val context = LocalContext.current

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DB_MIME_TYPE),
        onResult = { uri ->
            if (uri == null) {
                Toast.makeText(context, ContextCompat.getString(context, R.string.export_no_file_chosen), Toast.LENGTH_SHORT).show()
            } else {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequest.Builder(DatabaseExportWorker::class.java)
                        .setInputData(Data.Builder()
                            .putString(DatabaseExportWorker.INPUT_OUTPUT_URI, uri.toString())
                            .build())
                        .setConstraints(Constraints.NONE)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                )
            }
        }
    )

    Button(
        enabled = true,
        onClick = {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            activityLauncher.launch("neostumbler_reports_$timestamp.db")
        }
    ) {
        Text(text = stringResource(id = R.string.export_raw_database))
    }
}