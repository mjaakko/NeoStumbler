package xyz.malkki.neostumbler.ui.composables.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlin.enums.enumEntries
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ichnaea.ReportSendWorker

private val UPLOAD_INTERVAL = 8.hours

private enum class AutoUploadMode(@StringRes val description: Int) {
    NEVER(R.string.send_reports_automatically_never),
    ANY_NETWORK(R.string.send_reports_automatically_on_any_network),
    UNMETERED_NETWORK(R.string.send_reports_automatically_on_unmetered_network),
}

private fun WorkManager.getAutoUploadWorkInfo(): Flow<WorkInfo?> =
    getWorkInfosForUniqueWorkFlow(ReportSendWorker.PERIODIC_WORK_NAME).map { workInfos ->
        workInfos.find { workInfo ->
            workInfo.state != WorkInfo.State.CANCELLED && workInfo.state != WorkInfo.State.FAILED
        }
    }

@Composable
fun AutoUploadToggle() {
    val context = LocalContext.current

    val workManager = WorkManager.getInstance(context)

    val autoUploadWorkInfo by workManager.getAutoUploadWorkInfo().collectAsStateWithLifecycle(null)

    MultiChoiceSettings(
        title = stringResource(R.string.send_reports_automatically_title),
        options = enumEntries<AutoUploadMode>(),
        selectedOption =
            if (autoUploadWorkInfo == null) {
                AutoUploadMode.NEVER
            } else if (
                autoUploadWorkInfo!!.constraints.requiredNetworkType == NetworkType.UNMETERED
            ) {
                AutoUploadMode.UNMETERED_NETWORK
            } else {
                AutoUploadMode.ANY_NETWORK
            },
        titleProvider = { ContextCompat.getString(context, it.description) },
        onValueSelected = { autoUploadMode ->
            if (autoUploadMode == AutoUploadMode.NEVER) {
                workManager.cancelUniqueWork(ReportSendWorker.PERIODIC_WORK_NAME)
            } else {
                val workRequest =
                    PeriodicWorkRequestBuilder<ReportSendWorker>(UPLOAD_INTERVAL.toJavaDuration())
                        .setConstraints(
                            Constraints(
                                requiredNetworkType =
                                    if (autoUploadMode == AutoUploadMode.UNMETERED_NETWORK) {
                                        NetworkType.UNMETERED
                                    } else {
                                        NetworkType.CONNECTED
                                    },
                                requiresCharging = false,
                                requiresStorageNotLow = false,
                                requiresDeviceIdle = true,
                                requiresBatteryNotLow = true,
                            )
                        )
                        .build()

                // Schedule automatic report uploading to the configured endpoint
                workManager.enqueueUniquePeriodicWork(
                    ReportSendWorker.PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest,
                )
            }
        },
    )
}
