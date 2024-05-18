package xyz.malkki.neostumbler.geosubmit

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.gson.InstantTypeAdapter
import xyz.malkki.neostumbler.gson.OnlyFiniteNumberTypeAdapterFactory
import java.net.SocketTimeoutException
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class ReportSendWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        private const val REPORT_SEND_NOTIFICATION_ID = 55555

        const val PERIODIC_WORK_NAME = "report_upload_periodic"
        const val ONE_TIME_WORK_NAME = "report_upload_one_time"

        const val INPUT_REUPLOAD_FROM = "reupload_from"
        const val INPUT_REUPLOAD_TO = "reupload_to"

        const val OUTPUT_REPORTS_SENT = "reports_sent"

        private val GSON = GsonBuilder()
            .registerTypeAdapterFactory(OnlyFiniteNumberTypeAdapterFactory())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    private val application = applicationContext as StumblerApplication

    private val geosubmit by lazy {
        val (endpoint, apiKey) = getEndpointAndApiKey()

        Timber.d("Using endpoint $endpoint with API key $apiKey for Geosubmit")

        MLSGeosubmit(application.httpClient, GSON, endpoint, apiKey)
    }

    private val db = application.reportDb

    private fun getEndpointAndApiKey(): Pair<String, String?> = runBlocking {
        application.settingsStore.data
            .map { prefs ->
                val endpoint = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)] ?: MLSGeosubmit.DEFAULT_ENDPOINT
                val apiKey = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)]

                endpoint to apiKey
            }
            .first()
    }

    override suspend fun doWork(): Result {
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
        val geosubmitReports = reportsToUpload.map { report ->
            Report(
                report.report.timestamp,
                Report.Position.fromDbEntity(report.position),
                report.wifiAccessPoints.map(Report.WifiAccessPoint::fromDbEntity)
                    .takeIf { it.isNotEmpty() },
                report.cellTowers.map(Report.CellTower::fromDbEntity).takeIf { it.isNotEmpty() },
                report.bluetoothBeacons.map(Report.BluetoothBeacon::fromDbEntity)
                    .takeIf { it.isNotEmpty() }
            )
        }

        if (geosubmitReports.isEmpty()) {
            Timber.i("No Geosubmit reports to send")
            return createResult(0)
        }

        return try {
            val duration = measureTime {
                geosubmit.sendReports(geosubmitReports)
            }

            Timber.i(
                "Successfully sent ${geosubmitReports.size} reports to MLS in ${
                    duration.toString(DurationUnit.SECONDS, 2)
                }"
            )

            val now = Instant.now()

            val updatedReports = reportsToUpload
                .filter {
                    //Do not update upload timestamp for reports which were reuploaded
                    !it.report.uploaded
                }
                .map {
                    it.report.copy(uploaded = true, uploadTimestamp = now)
                }
                .toTypedArray()
            db.reportDao().update(*updatedReports)

            createResult(geosubmitReports.size)
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to send Geosubmit reports")

            if (shouldRetry(ex)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun createResult(reportsSent: Int): Result {
        return Result.success(Data.Builder().putInt(OUTPUT_REPORTS_SENT, reportsSent).build())
    }

    private fun shouldRetry(exception: Exception): Boolean {
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
            .setContentTitle(applicationContext.getString(R.string.notification_sending_reports))
            .setSmallIcon(R.drawable.sync_24)
            .build()
    }
}