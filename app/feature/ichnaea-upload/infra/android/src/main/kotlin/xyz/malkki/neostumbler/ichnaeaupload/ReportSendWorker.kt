package xyz.malkki.neostumbler.ichnaeaupload

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import java.io.IOException
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.ichnaea.IchnaeaClient
import xyz.malkki.neostumbler.network.NetworkErrorHandler

// By default, WorkManager will retry indefinitely.
// If uploading hasn't been successful after 5 retries,
// just return a failure to stop retrying
private const val MAX_RETRIES = 5

private val HTTP_STATUS_CODE_SERVER_ERROR = 500..599

class ReportSendWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        private const val REPORT_SEND_NOTIFICATION_ID = 55555

        val UPLOAD_INTERVAL = 8.hours

        const val PERIODIC_WORK_NAME = "report_upload_periodic"
        const val ONE_TIME_WORK_NAME = "report_upload_one_time"

        const val INPUT_REUPLOAD_FROM = "reupload_from"
        const val INPUT_REUPLOAD_TO = "reupload_to"

        const val INPUT_NOTIFICATION_CHANNEL_ID = "notification_channel_id"
        const val INPUT_NOTIFICATION_TITLE = "notification_title"
        const val INPUT_NOTIFICATION_ICON = "notification_icon"

        const val OUTPUT_REPORTS_SENT = "reports_sent"
        const val OUTPUT_ERROR_TYPE = "error_type"
        const val OUTPUT_ERROR_MESSAGE = "error_message"

        const val ERROR_TYPE_NO_ENDPOINT_CONFIGURED: Int = 1000
    }

    private val networkErrorHandler: NetworkErrorHandler by inject()

    private val reportProvider: ReportProvider by inject()
    private val reportSaver: ReportSaver by inject()

    private val settings: Settings by inject()

    private val ichnaeaClientProvider: IchnaeaClientProvider by inject()

    override suspend fun doWork(): Result {
        val geosubmit =
            ichnaeaClientProvider.ichnaeaClient.first()
                ?: return Result.failure(
                    Data.Builder()
                        .putInt(OUTPUT_ERROR_TYPE, ERROR_TYPE_NO_ENDPOINT_CONFIGURED)
                        .build()
                )

        val reportSender =
            ReportSender(
                geosubmit = geosubmit,
                reportProvider = reportProvider,
                reportSaver = reportSaver,
                settings = settings,
            )

        val reupload =
            inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_FROM) &&
                inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_TO)

        var reportsSent = 0

        return try {
            val progressListener: suspend (Int) -> Unit = {
                reportsSent = it

                setProgress(createResultData(reportsSent))
            }

            if (reupload) {
                val from = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_FROM, 0))
                val to = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_TO, 0))

                reportSender.reuploadReports(from, to, progressListener)
            } else {
                reportSender.sendNotUploadedReports(progressListener)
            }

            Result.success(createResultData(reportsSent))
        } catch (ex: IOException) {
            Timber.w(ex, "Failed to send Geosubmit reports")

            if (shouldRetry(ex)) {
                Result.retry()
            } else {
                Result.failure(createResultData(reportsSent, errorMessage = ex.message))
            }
        }
    }

    private fun createResultData(reportsSent: Int, errorMessage: String? = null): Data {
        return Data.Builder()
            .apply {
                putInt(OUTPUT_REPORTS_SENT, reportsSent)

                if (errorMessage != null) {
                    putString(OUTPUT_ERROR_MESSAGE, errorMessage)
                }
            }
            .build()
    }

    private fun shouldRetry(exception: IOException): Boolean {
        if (runAttemptCount >= MAX_RETRIES) {
            return false
        }

        if (
            exception is IchnaeaClient.HttpException &&
                exception.httpStatusCode in HTTP_STATUS_CODE_SERVER_ERROR
        ) {
            // Retry server-side errors (HTTP status 5xx)
            return true
        }

        return networkErrorHandler.isRetryable(exception)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            REPORT_SEND_NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
                applicationContext,
                inputData.getString(INPUT_NOTIFICATION_CHANNEL_ID)!!,
            )
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentTitle(
                applicationContext.getString(inputData.getInt(INPUT_NOTIFICATION_TITLE, 0))
            )
            .setSmallIcon(inputData.getInt(INPUT_NOTIFICATION_ICON, 0))
            .build()
    }
}
