package xyz.malkki.neostumbler.ichnaea

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import kotlinx.coroutines.Deferred
import okhttp3.Call
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto

// Send max 2000 reports in one request to avoid creating too large payloads
private const val MAX_REPORTS_PER_BATCH = 2000

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

    private val settingsStore: DataStore<Preferences> by inject<DataStore<Preferences>>(PREFERENCES)

    private val reportDatabaseManager: ReportDatabaseManager by inject()

    private val reportDao = reportDatabaseManager.reportDb.value.reportDao()

    private suspend fun getGeosubmitApi(): Geosubmit? {
        return settingsStore.getIchnaeaParams()?.let { geosubmitParams ->
            Timber.d(
                "Using endpoint ${geosubmitParams.submissionPath} with API key ${geosubmitParams.apiKey} for Geosubmit"
            )

            IchnaeaClient(httpClientProvider.await(), geosubmitParams)
        }
    }

    override suspend fun doWork(): Result {
        val geosubmit = getGeosubmitApi()

        if (geosubmit == null) {
            return Result.failure(
                Data.Builder().putInt(OUTPUT_ERROR_TYPE, ERROR_TYPE_NO_ENDPOINT_CONFIGURED).build()
            )
        }

        val reupload =
            inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_FROM) &&
                inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_TO)

        val reportsToUpload =
            if (!reupload) {
                reportDao.getAllReportsNotUploaded()
            } else {
                val from = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_FROM, 0))
                val to = Instant.ofEpochMilli(inputData.getLong(INPUT_REUPLOAD_TO, 0))

                reportDao.getAllReportsForTimerange(from, to)
            }

        if (reportsToUpload.isEmpty()) {
            Timber.i("No Geosubmit reports to send")
            return Result.success(createResultData(0))
        }

        var reportsSent = 0

        return try {
            reportsToUpload.chunked(MAX_REPORTS_PER_BATCH).forEach {
                sendReports(geosubmit, it)

                reportsSent += it.size

                setProgress(createResultData(reportsSent))
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

    private suspend fun sendReports(geosubmitApi: Geosubmit, reports: List<ReportWithData>) {
        val geosubmitReports =
            reports.map { report ->
                ReportDto(
                    timestamp = report.report.timestamp.toEpochMilli(),
                    position = ReportDto.PositionDto.fromDbEntity(report.positionEntity),
                    wifiAccessPoints =
                        report.wifiAccessPointEntities
                            .map(WifiAccessPointDto::fromDbEntity)
                            .takeIf { it.isNotEmpty() },
                    cellTowers =
                        report.cellTowerEntities.map(CellTowerDto::fromDbEntity).takeIf {
                            it.isNotEmpty()
                        },
                    bluetoothBeacons =
                        report.bluetoothBeaconEntities
                            .map(BluetoothBeaconDto::fromDbEntity)
                            .takeIf { it.isNotEmpty() },
                )
            }

        geosubmitApi.sendReports(geosubmitReports)

        val now = Instant.now()

        val updatedReports =
            reports
                .filter {
                    // Do not update upload timestamp for reports which were reuploaded
                    !it.report.uploaded
                }
                .map { it.report.copy(uploaded = true, uploadTimestamp = now) }
                .toTypedArray()

        reportDao.update(*updatedReports)
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

        if (exception is SocketTimeoutException) {
            // Retry timeouts because most likely we are just temporarily disconnected
            return true
        }

        if (
            exception is IchnaeaClient.HttpException &&
                exception.httpStatusCode in HTTP_STATUS_CODE_SERVER_ERROR
        ) {
            // Retry server-side errors (HTTP status 5xx)
            return true
        }

        return false
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
                ContextCompat.getString(applicationContext, R.string.notification_sending_reports)
            )
            .setSmallIcon(R.drawable.sync_24)
            .build()
    }
}
