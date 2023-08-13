package xyz.malkki.wifiscannerformls.geosubmit

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import timber.log.Timber
import xyz.malkki.wifiscannerformls.WifiScannerApplication
import xyz.malkki.wifiscannerformls.gson.InstantTypeAdapter
import java.net.SocketTimeoutException
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class ReportSendWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as WifiScannerApplication

        val geosubmit = MLSGeosubmit(application.httpClient, GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create())

        val db = application.reportDb

        val notUploadedReports = db.reportDao().getAllReportsNotUploaded()
        val geosubmitReports = notUploadedReports.map {
            Report(
                it.report.timestamp,
                Report.Position.fromDbEntity(it.position),
                it.wifiAccessPoints.map(Report.WifiAccessPoint::fromDbEntity),
                it.cellTowers.map(Report.CellTower::fromDbEntity),
                it.bluetoothBeacons.map(Report.BluetoothBeacon::fromDbEntity)
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

            val now = Instant.now()
            db.reportDao().update(*notUploadedReports.map { it.report.copy(uploaded = true, uploadTimestamp = now) }.toTypedArray())

            Timber.i("Successfully sent ${geosubmitReports.size} reports to MLS in ${duration.toString(DurationUnit.SECONDS, 2)}")

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