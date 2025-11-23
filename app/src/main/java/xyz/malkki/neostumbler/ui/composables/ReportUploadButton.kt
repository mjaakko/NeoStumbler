package xyz.malkki.neostumbler.ui.composables

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ichnaea.ReportSendWorker
import xyz.malkki.neostumbler.ui.composables.reports.ToastOnReportUpload
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel

/** Returns a flow which emits booleans indicating whether an upload can be started */
private fun WorkManager.getCanUploadFlow(): Flow<Boolean> =
    getWorkInfosForUniqueWorkFlow(ReportSendWorker.ONE_TIME_WORK_NAME)
        .map { workInfos ->
            workInfos.none { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED ||
                    workInfo.state == WorkInfo.State.RUNNING ||
                    workInfo.state == WorkInfo.State.BLOCKED
            }
        }
        .distinctUntilChanged()

@Composable
fun ReportUploadButton(modifier: Modifier = Modifier, reportsViewModel: ReportsViewModel) {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    val coroutineScope = rememberCoroutineScope()

    val canUpload = workManager.getCanUploadFlow().collectAsState(initial = false)
    val reportsNotUploaded = reportsViewModel.reportsNotUploaded.collectAsStateWithLifecycle(0)

    val enqueuing = remember { mutableStateOf(false) }

    val enqueuedUploadWork = rememberSaveable { mutableStateOf<UUID?>(null) }

    ToastOnReportUpload(workId = enqueuedUploadWork)

    FilledIconButton(
        modifier = modifier,
        enabled = reportsNotUploaded.value > 0 && canUpload.value && !enqueuing.value,
        onClick = {
            coroutineScope.launch {
                enqueuing.value = true

                val workId = UUID.randomUUID()

                workManager
                    .enqueueUniqueWork(
                        ReportSendWorker.ONE_TIME_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<ReportSendWorker>()
                            .setId(workId)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 30.seconds.toJavaDuration())
                            .setConstraints(
                                Constraints(
                                    requiredNetworkType = NetworkType.CONNECTED,
                                    requiresStorageNotLow = false,
                                    requiresDeviceIdle = false,
                                    requiresCharging = false,
                                    requiresBatteryNotLow = false,
                                )
                            )
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build(),
                    )
                    .await()

                enqueuedUploadWork.value = workId

                // Add small delay to avoid flickering the button
                delay(0.3.seconds)

                enqueuing.value = false
            }
        },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.upload_24px),
            contentDescription = stringResource(R.string.send_reports),
        )
    }
}
