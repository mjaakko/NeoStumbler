package xyz.malkki.neostumbler.ui.composables

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaReportUploadStarter
import xyz.malkki.neostumbler.ui.composables.reports.ToastOnReportUpload
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel

@Composable
fun ReportUploadButton(
    modifier: Modifier = Modifier,
    reportsViewModel: ReportsViewModel,
    ichnaeaReportUploadStarter: IchnaeaReportUploadStarter = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()

    val uploadEnqueued by
        ichnaeaReportUploadStarter.reportUploadEnqueued.collectAsStateWithLifecycle(
            initialValue = true
        )

    val reportsNotUploaded by reportsViewModel.reportsNotUploaded.collectAsStateWithLifecycle(0)

    var enqueuing by remember { mutableStateOf(false) }

    var enqueuedUploadWork by rememberSaveable { mutableStateOf<UUID?>(null) }

    ToastOnReportUpload(
        workId = enqueuedUploadWork,
        onToastShown = { enqueuedUploadWork = null },
    )

    FilledIconButton(
        modifier = modifier,
        enabled = reportsNotUploaded > 0 && !uploadEnqueued && !enqueuing,
        onClick = {
            coroutineScope.launch {
                enqueuing = true

                enqueuedUploadWork = ichnaeaReportUploadStarter.startUploadNotUploadedReports()

                // Add small delay to avoid flickering the button
                delay(0.3.seconds)

                enqueuing = false
            }
        },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.upload_24px),
            contentDescription = stringResource(R.string.send_reports),
        )
    }
}
