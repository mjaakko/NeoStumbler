package xyz.malkki.neostumbler.ui.composables.managestorage

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportRemover
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.AreaPickerDialog

@Composable
fun DeleteReportsFromArea(reportRemover: ReportRemover = koinInject()) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        AreaPickerDialog(
            title = stringResource(R.string.delete_reports_from_area),
            positiveButtonText = stringResource(R.string.delete),
            onAreaSelected = { circle ->
                dialogOpen.value = false

                if (circle != null) {
                    val (center, radius) = circle

                    coroutineScope.launch {
                        val deletedCount = reportRemover.deleteFromArea(center, radius)

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

    Button(onClick = { dialogOpen.value = true }) {
        Text(text = stringResource(R.string.delete_reports_from_area))
    }
}
