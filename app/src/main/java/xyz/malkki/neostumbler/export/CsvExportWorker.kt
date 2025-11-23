package xyz.malkki.neostumbler.export

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.extensions.getTextCompat

class CsvExportWorker(appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        const val INPUT_FROM = "from"
        const val INPUT_TO = "to"
        const val INPUT_OUTPUT_URI = "uri"

        private const val DATA_EXPORT_NOTIFICATION_ID = 200000
    }

    private val csvExporter: CsvExporter by inject()

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
                applicationContext,
                StumblerApplication.EXPORT_NOTIFICATION_CHANNEL_ID,
            )
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(applicationContext.getTextCompat(R.string.notification_exporting_data))
            .setSmallIcon(R.drawable.upload_24px)
            .build()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            DATA_EXPORT_NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    override suspend fun doWork(): Result {
        if (
            !params.inputData.hasKeyWithValueOfType<Long>(INPUT_FROM) ||
                !params.inputData.hasKeyWithValueOfType<Long>(INPUT_TO)
        ) {
            Timber.e("Cannot export data because time period is not specified")
            return Result.failure()
        }

        if (!params.inputData.hasKeyWithValueOfType<String>(INPUT_OUTPUT_URI)) {
            Timber.e("Cannot export data because output URI is not specified")
            return Result.failure()
        }

        setForeground(getForegroundInfo())

        val uri = params.inputData.getString(INPUT_OUTPUT_URI)!!.toUri()

        val from = Instant.ofEpochMilli(params.inputData.getLong(INPUT_FROM, 0))
        val to = Instant.ofEpochMilli(params.inputData.getLong(INPUT_TO, 0))

        val time = measureTime { csvExporter.exportToFile(uri, from, to) }

        Timber.i(
            "Exported data for time period [$from, $to] to $uri in ${
                time.toString(
                    DurationUnit.SECONDS,
                    1,
                )
            }"
        )

        return Result.success()
    }
}
