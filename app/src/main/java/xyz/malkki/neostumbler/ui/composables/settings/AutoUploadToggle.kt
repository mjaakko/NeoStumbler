package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.geosubmit.ReportSendWorker
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction
import java.time.Duration

@Composable
fun AutoUploadToggle() {
    val context = LocalContext.current

    val workManager = WorkManager.getInstance(context)

    val autoUploadEnabled = workManager.getWorkInfosForUniqueWorkFlow(ReportSendWorker.PERIODIC_WORK_NAME)
        .map { workInfos ->
            workInfos.any { workInfo ->
                workInfo.state != WorkInfo.State.CANCELLED && workInfo.state != WorkInfo.State.FAILED
            }
        }
        .collectAsState(initial = null)

    ToggleWithAction(
        title = stringResource(R.string.send_reports_automatically),
        enabled = autoUploadEnabled.value != null,
        checked = autoUploadEnabled.value == true ,
        action = { checked ->
            if (checked) {
                //Schedule report uploading to MLS
                workManager
                    .enqueueUniquePeriodicWork(
                        ReportSendWorker.PERIODIC_WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        PeriodicWorkRequestBuilder<ReportSendWorker>(Duration.ofHours(8))
                            .setConstraints(
                                Constraints(
                                    requiredNetworkType = NetworkType.CONNECTED,
                                    requiresCharging = false,
                                    requiresStorageNotLow = false,
                                    requiresDeviceIdle = true,
                                    requiresBatteryNotLow = true
                                )
                            )
                            .build()
                    )
                    .await()
            } else {
                workManager.cancelUniqueWork(ReportSendWorker.PERIODIC_WORK_NAME).await()
            }
        }
    )
}