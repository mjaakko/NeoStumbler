package xyz.malkki.neostumbler.ichnaeaupload

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AndroidIchnaeaReportUploadStarter(
    context: Context,
    private val notificationChannelId: String,
    @StringRes private val notificationTitle: Int,
    @DrawableRes private val notificationIcon: Int,
) : IchnaeaReportUploadStarter {
    private val appContext: Context = context.applicationContext

    private val workManager = WorkManager.getInstance(appContext)

    override val reportUploadEnqueued =
        workManager
            .getWorkInfosForUniqueWorkFlow(ReportSendWorker.ONE_TIME_WORK_NAME)
            .map { workInfos ->
                workInfos.any { workInfo ->
                    workInfo.state == WorkInfo.State.ENQUEUED ||
                        workInfo.state == WorkInfo.State.RUNNING ||
                        workInfo.state == WorkInfo.State.BLOCKED
                }
            }
            .distinctUntilChanged()

    override suspend fun awaitUntilUploaded(jobId: UUID): UploadResult? {
        val workInfo =
            workManager.getWorkInfoByIdFlow(jobId).first { workInfo ->
                workInfo == null || workInfo.state.isFinished
            }

        if (workInfo == null) {
            return null
        }

        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
            return UploadResult.Success(
                uploadedCount = workInfo.outputData.getInt(ReportSendWorker.OUTPUT_REPORTS_SENT, 0)
            )
        } else if (workInfo.state == WorkInfo.State.FAILED) {
            return UploadResult.Failure(
                type =
                    if (
                        workInfo.outputData.getInt(ReportSendWorker.OUTPUT_ERROR_TYPE, -1) ==
                            ReportSendWorker.ERROR_TYPE_NO_ENDPOINT_CONFIGURED
                    ) {
                        UploadResult.Failure.FailureType.NO_ENDPOINT
                    } else {
                        UploadResult.Failure.FailureType.OTHER
                    },
                errorMessage = workInfo.outputData.getString(ReportSendWorker.OUTPUT_ERROR_MESSAGE),
            )
        } else {
            return null
        }
    }

    override suspend fun startUploadNotUploadedReports(): UUID {
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

        return workId
    }

    override suspend fun startReuploadReports(from: Instant, to: Instant): UUID {
        val workId = UUID.randomUUID()

        workManager
            .enqueue(
                OneTimeWorkRequest.Builder(ReportSendWorker::class.java)
                    .setId(workId)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30.seconds.toJavaDuration())
                    .setInputData(
                        Data.Builder()
                            .putLong(ReportSendWorker.INPUT_REUPLOAD_FROM, from.toEpochMilli())
                            .putLong(ReportSendWorker.INPUT_REUPLOAD_TO, to.toEpochMilli())
                            .putInt(ReportSendWorker.INPUT_NOTIFICATION_TITLE, notificationTitle)
                            .putInt(ReportSendWorker.INPUT_NOTIFICATION_ICON, notificationIcon)
                            .putString(
                                ReportSendWorker.INPUT_NOTIFICATION_CHANNEL_ID,
                                notificationChannelId,
                            )
                            .build()
                    )
                    .setConstraints(Constraints.NONE)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )
            .await()

        return workId
    }
}
