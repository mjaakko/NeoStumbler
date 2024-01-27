package xyz.malkki.neostumbler.geosubmit

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import com.google.gson.GsonBuilder
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.gson.InstantTypeAdapter
import xyz.malkki.neostumbler.gson.OnlyFiniteNumberTypeAdapterFactory
import java.net.SocketTimeoutException
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class ReportSendWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        const val PERIODIC_WORK_NAME = "report_upload_periodic"
        const val ONE_TIME_WORK_NAME = "report_upload_one_time"

        const val INPUT_REUPLOAD_FROM = "reupload_from"
        const val INPUT_REUPLOAD_TO = "reupload_to"

        private val GSON = GsonBuilder()
            .registerTypeAdapterFactory(OnlyFiniteNumberTypeAdapterFactory())
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    private val application = applicationContext as StumblerApplication

    private val geosubmit = MLSGeosubmit(application.httpClient, GSON)

    private val db = application.reportDb

    override suspend fun doWork(): Result {
        val reupload = inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_FROM) && inputData.hasKeyWithValueOfType<Long>(INPUT_REUPLOAD_TO)

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
                report.wifiAccessPoints.map(Report.WifiAccessPoint::fromDbEntity).takeIf { it.isNotEmpty() },
                report.cellTowers.map(Report.CellTower::fromDbEntity).takeIf { it.isNotEmpty() },
                report.bluetoothBeacons.map(Report.BluetoothBeacon::fromDbEntity).takeIf { it.isNotEmpty() }
            )
        }

        if (geosubmitReports.isEmpty()) {
            Timber.i("No Geosubmit reports to send")
            return Result.success()
        }

        return try {
            val duration = measureTime {
                geosubmit.sendReports(geosubmitReports)
            }

            Timber.i("Successfully sent ${geosubmitReports.size} reports to MLS in ${duration.toString(DurationUnit.SECONDS, 2)}")

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

            Result.success()
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to send Geosubmit reports")

            if (shouldRetry(ex)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
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
}