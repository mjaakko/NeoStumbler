package xyz.malkki.neostumbler.ichnaeaupload

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AndroidIchnaeaAutoUploadToggler(context: Context) : IchnaeaAutoUploadToggler {
    private val appContext: Context = context.applicationContext

    private val workManager = WorkManager.getInstance(appContext)

    override fun getAutoUploadMode(): Flow<AutoUploadMode> {
        return workManager
            .getWorkInfosForUniqueWorkFlow(ReportSendWorker.PERIODIC_WORK_NAME)
            .map { workInfos ->
                workInfos.find { workInfo ->
                    workInfo.state != WorkInfo.State.CANCELLED &&
                        workInfo.state != WorkInfo.State.FAILED
                }
            }
            .map { workInfo ->
                if (workInfo == null) {
                    AutoUploadMode.NEVER
                } else if (workInfo.constraints.requiredNetworkType == NetworkType.UNMETERED) {
                    AutoUploadMode.UNMETERED_NETWORK
                } else {
                    AutoUploadMode.ANY_NETWORK
                }
            }
    }

    override suspend fun setAutoUploadMode(autoUploadMode: AutoUploadMode) {
        when (autoUploadMode) {
            AutoUploadMode.NEVER -> {
                workManager.cancelUniqueWork(ReportSendWorker.PERIODIC_WORK_NAME).await()
            }
            else -> {
                val workRequest =
                    PeriodicWorkRequestBuilder<ReportSendWorker>(
                            ReportSendWorker.UPLOAD_INTERVAL.toJavaDuration()
                        )
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
                workManager
                    .enqueueUniquePeriodicWork(
                        ReportSendWorker.PERIODIC_WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest,
                    )
                    .await()
            }
        }
    }
}
