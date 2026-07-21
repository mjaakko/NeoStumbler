package xyz.malkki.neostumbler.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ui.composables.shared.Dialog
import xyz.malkki.neostumbler.ui.composables.shared.formattedDate
import xyz.malkki.neostumbler.ui.viewmodel.ReportWithTimestamp

@Composable
fun SelectReport(selectableReports: List<ReportWithTimestamp>, showReport: (Long?) -> Unit) {
    Dialog(onDismissRequest = { showReport(null) }, title = stringResource(R.string.reports)) {
        Column {
            selectableReports.forEach { (reportId, timestamp) ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(36.dp)
                            .clickable(onClick = { showReport(reportId) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = formattedDate(timestamp))
                }
            }
        }
    }
}
