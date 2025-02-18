package xyz.malkki.neostumbler.ui.composables.managestorage

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.ConfirmationDialog

@Composable
fun DeleteAllReportsButton(reportDb: StateFlow<ReportDatabase>) {
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
                    withContext(Dispatchers.IO) { reportDb.value.clearAllTables() }

                    context.showToast(
                        ContextCompat.getString(context, R.string.toast_deleted_all_reports)
                    )
                }
            },
            onNegativeAction = { showConfirmationDialog.value = false },
        )
    }

    Button(onClick = { showConfirmationDialog.value = true }) {
        Text(text = stringResource(id = R.string.delete_all_reports))
    }
}
