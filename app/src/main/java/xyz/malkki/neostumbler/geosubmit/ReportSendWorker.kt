package xyz.malkki.neostumbler.geosubmit

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import kotlinx.coroutines.flow.first
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.entities.ReportWithData
import java.net.SocketTimeoutException
import java.time.Instant
import kotlin.collections.isNotEmpty
import kotlin.collections.map

class ReportSendWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        private const val REPORT_SEND_NOTIFICATION_ID = 55555

        //Send max 2000 reports in one request to avoid creating too large payloads
        private const val MAX_REPORTS_PER_BATCH = 2000

        const val PERIODIC_WORK_NAME = "report_upload_periodic"
        const val ONE_TIME_WORK_NAME = "report_upload_one_time"

        const val INPUT_REUPLOAD_FROM = "reupload_from"
        const val INPUT_REUPLOAD_TO = "reupload_to"

        const val OUTPUT_REPORTS_SENT = "reports_sent"
        const val OUTPUT_ERROR_MESSAGE = "error_message"
    }

    private val application = applicationContext as StumblerApplication

    private val db = application.reportDb.value

    private suspend fun getGeosubmitApi(): Geosubmit {
        val geosubmitParams = application.geosubmitParamsFlow().first()

        Timber.d("Using endpoint ${geosubmitParams.path} with API key ${geosubmitParams.apiKey} for Geosubmit")

        return MLSGeosubmit(application.httpClientProvider.await(), geosubmitParams)
    }

    override suspend fun doWork(): Result {
        val geosubmit = getGeosubmitApi()

        val reupload =
            inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_FROM)
                && inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_TO)

        val reportsToUpload = if (!reupload) {
            db.reportDao().getAllReportsNotUploaded()
        } else {
            val from = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_FROM, 0))
            val to = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_TO, 0))

            db.reportDao().getAllReportsForTimerange(from, to)
        }

        if (reportsToUpload.isEmpty()) {
            Timber.i("No Geosubmit reports to send")
            return Result.success(createResultData(0))
        }

        var reportsSent = 0

        return try {
            reportsToUpload
                .chunked(MAX_REPORTS_PER_BATCH)
                .forEach {
                    sendReports(geosubmit, it)

                    reportsSent += it.size

                    setProgress(createResultData(reportsSent))
                }

            Result.success(createResultData(reportsSent))
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to send Geosubmit reports")

            if (shouldRetry(ex)) {
                Result.retry()
            } else {
                Result.failure(createResultData(reportsSent, errorMessage = ex.message))
            }
        }
    }

    private suspend fun sendReports(geosubmitApi: Geosubmit, reports: List<ReportWithData>) {
        val geosubmitReports = reports.map { report ->
            Report(
                timestamp = report.report.timestamp.toEpochMilli(),
                position = Report.Position.fromDbEntity(report.positionEntity),
                wifiAccessPoints = report.wifiAccessPointEntities
                    .map(Report.WifiAccessPoint::fromDbEntity)
                    .takeIf { it.isNotEmpty() },
                cellTowers = report.cellTowerEntities
                    .map(Report.CellTower::fromDbEntity)
                    .takeIf { it.isNotEmpty() },
                bluetoothBeacons = report.bluetoothBeaconEntities
                    .map(Report.BluetoothBeacon::fromDbEntity)
                    .takeIf { it.isNotEmpty() }
            )
        }

        geosubmitApi.sendReports(geosubmitReports)

        val now = Instant.now()

        val updatedReports = reports
            .filter {
                //Do not update upload timestamp for reports which were reuploaded
                !it.report.uploaded
            }
            .map {
                it.report.copy(uploaded = true, uploadTimestamp = now)
            }
            .toTypedArray()
        db.reportDao().update(*updatedReports)
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

    private fun shouldRetry(exception: Exception): Boolean {
        //By default, WorkManager will retry indefinitely
        //If uploading hasn't been successful after 5 retries, just return a failure to stop retrying
        if (runAttemptCount >= 5) {
            return false
        }

        if (exception is SocketTimeoutException) {
            //Retry timeouts because most likely we are just temporarily disconnected
            return true
        }

        if (exception is MLSGeosubmit.MLSException && exception.httpStatusCode in 500..599) {
            //Retry server-side errors (HTTP status 5xx)
            return true
        }

        return false
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            REPORT_SEND_NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(applicationContext, StumblerApplication.REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentTitle(ContextCompat.getString(applicationContext, R.string.notification_sending_reports))
            .setSmallIcon(R.drawable.sync_24)
            .build()
    }
}