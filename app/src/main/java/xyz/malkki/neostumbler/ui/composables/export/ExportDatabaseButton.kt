package xyz.malkki.neostumbler.ui.composables.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.export.DatabaseExportWorker
import xyz.malkki.neostumbler.extensions.showToast

private const val DB_MIME_TYPE = "application/vnd.sqlite3"

@Composable
fun ExportDatabaseButton() {
    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        ExportDatabaseDialog(onDialogClose = { dialogOpen.value = false })
    }

    Button(enabled = true, onClick = { dialogOpen.value = true }) {
        Text(text = stringResource(id = R.string.export_raw_database))
    }
}

@Composable
private fun ExportDatabaseDialog(onDialogClose: () -> Unit) {
    val context = LocalContext.current

    val compress = rememberSaveable { mutableStateOf(true) }

    fun onFileSelected(uri: Uri?) {
        if (uri == null) {
            context.showToast(ContextCompat.getString(context, R.string.export_no_file_chosen))
        } else {
            WorkManager.getInstance(context)
                .enqueue(
                    OneTimeWorkRequest.Builder(DatabaseExportWorker::class.java)
                        .setInputData(
                            Data.Builder()
                                .putString(DatabaseExportWorker.INPUT_OUTPUT_URI, uri.toString())
                                .putBoolean(DatabaseExportWorker.INPUT_COMPRESS, compress.value)
                                .build()
                        )
                        .setConstraints(Constraints.NONE)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                )
        }

        onDialogClose.invoke()
    }

    val rawDbFilePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(DB_MIME_TYPE),
            onResult = { onFileSelected(it) },
        )

    val compressedDbFilePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/gzip"),
            onResult = { onFileSelected(it) },
        )

    BasicAlertDialog(onDismissRequest = { onDialogClose.invoke() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(all = 24.dp)) {
                Text(
                    text = stringResource(R.string.export_raw_database),
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = compress.value,
                            onCheckedChange = { compress.value = it },
                        )

                        Text(text = stringResource(id = R.string.export_raw_database_compress))
                    }

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            val fileName = getExportedDbFileName(compress.value)

                            if (compress.value) {
                                compressedDbFilePicker.launch(fileName)
                            } else {
                                rawDbFilePicker.launch(fileName)
                            }
                        },
                    ) {
                        Text(text = stringResource(id = R.string.export_data))
                    }
                }
            }
        }
    }
}

private fun getExportedDbFileName(gzipped: Boolean): String {
    val timestamp =
        LocalDateTime.now()
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    val fileNameBase = "neostumbler_reports_$timestamp.db"

    return if (gzipped) {
        "$fileNameBase.gz"
    } else {
        fileNameBase
    }
}
