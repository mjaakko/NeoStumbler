package xyz.malkki.neostumbler.ui.composables

import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.geosubmit.ReportSendWorker
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Returns a flow which emits booleans indicating whether an upload can be started
 */
private fun WorkManager.getCanUploadFlow(): Flow<Boolean> = getWorkInfosForUniqueWorkLiveData(ReportSendWorker.ONE_TIME_WORK_NAME)
    .asFlow()
    .map { workInfos ->
        workInfos.none { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED
                || workInfo.state == WorkInfo.State.RUNNING
                || workInfo.state == WorkInfo.State.BLOCKED
        }
    }
    .distinctUntilChanged()

@Composable
fun ReportUploadButton() {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    val coroutineScope = rememberCoroutineScope()

    val canUpload = workManager.getCanUploadFlow().collectAsState(initial = false)

    val enqueuing = remember {
        mutableStateOf(false)
    }

    val enqueuedUploadWork = remember { mutableStateOf<UUID?>(null) }

    EffectOnWorkCompleted(
        workId = enqueuedUploadWork.value,
        onWorkSuccess = { workInfo ->
            val reportsUploaded = workInfo.outputData.getInt(ReportSendWorker.OUTPUT_REPORTS_SENT, 0)
            Toast.makeText(context, context.getString(R.string.toast_reports_uploaded, reportsUploaded), Toast.LENGTH_SHORT).show()
        },
        onWorkFailed = {
            Toast.makeText(context, context.getString(R.string.toast_reports_upload_failed), Toast.LENGTH_SHORT).show()
        }
    )

    Button(
        enabled = canUpload.value && !enqueuing.value,
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
                            .setConstraints(
                                Constraints(
                                    requiredNetworkType = NetworkType.CONNECTED,
                                    requiresStorageNotLow = false,
                                    requiresDeviceIdle = false,
                                    requiresCharging = false,
                                    requiresBatteryNotLow = false
                                )
                            )
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()
                    )
                    .await()

                enqueuedUploadWork.value = workId

                //Add small delay to avoid flickering the button
                delay(0.3.seconds)

                enqueuing.value = false
            }
        }
    ) {
        Text(text = stringResource(R.string.send_reports))
    }
}