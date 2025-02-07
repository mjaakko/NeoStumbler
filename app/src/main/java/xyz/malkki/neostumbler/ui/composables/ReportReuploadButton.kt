package xyz.malkki.neostumbler.ui.composables

import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.geosubmit.ReportSendWorker
import xyz.malkki.neostumbler.ui.composables.shared.DateRangePickerDialog
import xyz.malkki.neostumbler.ui.composables.shared.EffectOnWorkCompleted
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private fun ReportDatabaseManager.getSelectableDatesSet(): Flow<Set<LocalDate>> {
    return reportDb
        .flatMapLatest { it.reportDao().getReportDates() }
        .map { it.toSet() }
}

@Composable
fun ReportReuploadButton() {
    val context = LocalContext.current

    val reportDatabaseManager: ReportDatabaseManager = koinInject()

    val enqueuedUploadWork = rememberSaveable { mutableStateOf<UUID?>(null) }

    EffectOnWorkCompleted(
        workId = enqueuedUploadWork.value,
        onWorkSuccess = { workInfo ->
            val reportsUploaded =
                workInfo.outputData.getInt(ReportSendWorker.OUTPUT_REPORTS_SENT, 0)
            context.showToast(
                context.getQuantityString(
                    R.plurals.toast_reports_uploaded,
                    reportsUploaded,
                    reportsUploaded
                )
            )

            enqueuedUploadWork.value = null
        },
        onWorkFailed = { workInfo ->
            val errorType = workInfo.outputData.getInt(ReportSendWorker.OUTPUT_ERROR_TYPE, -1)

            when (errorType) {
                ReportSendWorker.ERROR_TYPE_NO_ENDPOINT_CONFIGURED -> {
                    context.showToast(
                        ContextCompat.getString(
                            context,
                            R.string.toast_reports_upload_failed_no_endpoint
                        )
                    )
                }

                else -> {
                    val errorMessage =
                        workInfo.outputData.getString(ReportSendWorker.OUTPUT_ERROR_MESSAGE)

                    val toastText = buildString {
                        append(
                            ContextCompat.getString(
                                context,
                                R.string.toast_reports_upload_failed
                            )
                        )

                        if (errorMessage != null) {
                            append("\n\n")
                            append(errorMessage)
                        }
                    }
                    context.showToast(toastText, length = Toast.LENGTH_LONG)
                }
            }

            enqueuedUploadWork.value = null
        }
    )

    val selectableDates = reportDatabaseManager.getSelectableDatesSet().collectAsStateWithLifecycle(null)

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

                    //Convert to local time
                    val from =
                        dateRange.start.atStartOfDay(localTimeZone).toInstant().toEpochMilli()
                    val to = dateRange.endInclusive
                        //Add one day to include data for the last day in the selected range
                        .plusDays(1)
                        .atStartOfDay(localTimeZone)
                        .toInstant()
                        .toEpochMilli()

                    val workId = UUID.randomUUID()

                    WorkManager.getInstance(context).enqueue(
                        OneTimeWorkRequest.Builder(ReportSendWorker::class.java)
                            .setId(workId)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 30.seconds.toJavaDuration())
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
            }
        )
    }

    Button(
        enabled = true,
        onClick = {
            dialogOpen.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.reupload_data))
    }
}