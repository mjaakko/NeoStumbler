package xyz.malkki.neostumbler.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.ichnaea.ReportSendWorker
import xyz.malkki.neostumbler.ui.composables.reports.ToastOnReportUpload
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog

@Composable
fun ReportReuploadButton(reportProvider: ReportProvider = koinInject()) {
    val context = LocalContext.current

    val enqueuedUploadWork = rememberSaveable { mutableStateOf<UUID?>(null) }

    ToastOnReportUpload(workId = enqueuedUploadWork)

    val selectableDates = reportProvider.getReportDates().collectAsStateWithLifecycle(null)

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        DateRangePickerDialog(
            title = stringResource(id = R.string.reupload_data),
            selectButtonText = stringResource(id = R.string.reupload_data),
            selectableDates = selectableDates,
            onDatesSelected = { dateRange ->
                dialogOpen.value = false

                if (dateRange != null) {
                    val localTimeZone = ZoneId.systemDefault()

                    // Convert to local time
                    val from =
                        dateRange.start.atStartOfDay(localTimeZone).toInstant().toEpochMilli()
                    val to =
                        dateRange.endInclusive
                            // Add one day to include data for the last day in the selected range
                            .plusDays(1)
                            .atStartOfDay(localTimeZone)
                            .toInstant()
                            .toEpochMilli()

                    val workId = UUID.randomUUID()

                    WorkManager.getInstance(context)
                        .enqueue(
                            OneTimeWorkRequest.Builder(ReportSendWorker::class.java)
                                .setId(workId)
                                .setBackoffCriteria(
                                    BackoffPolicy.LINEAR,
                                    30.seconds.toJavaDuration(),
                                )
                                .setInputData(
                                    Data.Builder()
                                        .putLong(ReportSendWorker.INPUT_REUPLOAD_FROM, from)
                                        .putLong(ReportSendWorker.INPUT_REUPLOAD_TO, to)
                                        .build()
                                )
                                .setConstraints(Constraints.NONE)
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                        )
                    enqueuedUploadWork.value = workId
                }
            },
        )
    }

    Button(enabled = true, onClick = { dialogOpen.value = true }) {
        Text(text = stringResource(id = R.string.reupload_data))
    }
}
