package xyz.malkki.neostumbler.ichnaea

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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.first
import okhttp3.Call
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.getTextCompat
import xyz.malkki.neostumbler.http.isRetryable
import xyz.malkki.neostumbler.ichnaea.mapper.getIchnaeaParams

// By default, WorkManager will retry indefinitely.
// If uploading hasn't been successful after 5 retries,
// just return a failure to stop retrying
private const val MAX_RETRIES = 5

private val HTTP_STATUS_CODE_SERVER_ERROR = 500..599

class ReportSendWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        private const val REPORT_SEND_NOTIFICATION_ID = 55555

        const val PERIODIC_WORK_NAME = "report_upload_periodic"
        const val ONE_TIME_WORK_NAME = "report_upload_one_time"

        const val INPUT_REUPLOAD_FROM = "reupload_from"
        const val INPUT_REUPLOAD_TO = "reupload_to"

        const val OUTPUT_REPORTS_SENT = "reports_sent"
        const val OUTPUT_ERROR_TYPE = "error_type"
        const val OUTPUT_ERROR_MESSAGE = "error_message"

        const val ERROR_TYPE_NO_ENDPOINT_CONFIGURED: Int = 1000
    }

    private val httpClientProvider: Deferred<Call.Factory> by inject<Deferred<Call.Factory>>()

    private val settings: Settings by inject()

    private val reportProvider: ReportProvider by inject()
    private val reportSaver: ReportSaver by inject()

    private suspend fun getGeosubmitApi(ichnaeaParams: IchnaeaParams): Geosubmit {
        Timber.d(
            "Using endpoint ${ichnaeaParams.submissionPath} with API key ${ichnaeaParams.apiKey} for Geosubmit"
        )

        return IchnaeaClient(httpClientProvider.await(), ichnaeaParams)
    }

    override suspend fun doWork(): Result {
        val ichnaeaParams = settings.getIchnaeaParams()
        val geosubmit = ichnaeaParams?.let { getGeosubmitApi(it) }
        if (geosubmit == null) {
            return Result.failure(
                Data.Builder().putInt(OUTPUT_ERROR_TYPE, ERROR_TYPE_NO_ENDPOINT_CONFIGURED).build()
            )
        }

        val reportSender =
            ReportSender(
                geosubmit = geosubmit,
                reportProvider = reportProvider,
                reportSaver = reportSaver,
            )

        val sendWithReducedMetadata =
            settings.getBooleanFlow(PreferenceKeys.REDUCED_METADATA, false).first()

        val reupload =
            inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_FROM) &&
                inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_TO)

        var reportsSent = 0

        return try {
            val progressListener: suspend (Int) -> Unit = {
                reportsSent = it

                setProgress(createResultData(reportsSent))
            }

            val ageShift =
                /**
                 * Workaround for https://codeberg.org/beacondb/beacondb/issues/138
                 * - delete this when the fix is merged
                 */
                if (ichnaeaParams.baseUrl.contains("beacondb")) {
                    Timber.i(
                        "Shifting age values in the report by 45 seconds" +
                            "to handle BeaconDB not supporting negative values"
                    )

                    45.seconds
                } else {
                    0.seconds
                }

            if (reupload) {
                val from = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_FROM, 0))
                val to = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_TO, 0))

                reportSender.reuploadReports(
                    from,
                    to,
                    sendWithReducedMetadata,
                    ageShift = ageShift,
                    progressListener,
                )
            } else {
                reportSender.sendNotUploadedReports(
                    sendWithReducedMetadata,
                    ageShift = ageShift,
                    progressListener,
                )
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

        return exception.isRetryable()
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
                StumblerApplication.REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID,
            )
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentTitle(
                applicationContext.getTextCompat(R.string.notification_sending_reports)
            )
            .setSmallIcon(R.drawable.sync_24)
            .build()
    }
}
