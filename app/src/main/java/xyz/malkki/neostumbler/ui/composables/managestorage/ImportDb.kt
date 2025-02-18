package xyz.malkki.neostumbler.ui.composables.managestorage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlin.io.copyTo
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.ConfirmationDialog

@Composable
fun ImportDb() {
    val context = LocalContext.current

    val reportDatabaseManager: ReportDatabaseManager = koinInject()

    val coroutineContext = rememberCoroutineScope()

    val confirmationDialogOpen = rememberSaveable { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                // No file was chosen
                return@rememberLauncherForActivityResult
            }

            coroutineContext.launch {
                val tempDbFile = createTempFile(context.cacheDir.toPath(), "temp", ".db")

                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        context.showToast(
                            ContextCompat.getString(context, R.string.failed_to_open_selected_file)
                        )
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
                        reportDatabaseManager.importDb(tempDbFile)

                        val reportCount =
                            reportDatabaseManager.reportDb.value
                                .reportDao()
                                .getReportCount()
                                .first()
                        context.showToast(
                            context.getQuantityString(
                                R.plurals.import_database_successful,
                                reportCount,
                                reportCount,
                            )
                        )
                    } else {
                        context.showToast(
                            ContextCompat.getString(context, R.string.import_database_not_valid)
                        )
                    }
                } catch (_: Exception) {
                    context.showToast(
                        ContextCompat.getString(context, R.string.failed_to_open_selected_file)
                    )
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

                // Specifying correct mime type seems to cause DB files to not be selectable in some
                // cases -> use wildcard
                launcher.launch("*/*")
            },
            onNegativeAction = { confirmationDialogOpen.value = false },
        )
    }

    Button(onClick = { confirmationDialogOpen.value = true }) {
        Text(text = stringResource(id = R.string.import_raw_database))
    }
}
