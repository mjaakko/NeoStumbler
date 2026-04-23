package xyz.malkki.neostumbler.export

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import java.io.IOException
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CsvExportWorker(appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        const val INPUT_FROM = "from"
        const val INPUT_TO = "to"
        const val INPUT_OUTPUT_URI = "uri"

        const val INPUT_NOTIFICATION_CHANNEL = "notification_channel"
        const val INPUT_NOTIFICATION_TITLE = "notification_title"
        const val INPUT_NOTIFICATION_DRAWABLE = "notification_drawable"

        private const val DATA_EXPORT_NOTIFICATION_ID = 200000
    }

    private val csvExporter: CsvExporter by inject()

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
                applicationContext,
                params.inputData.getString(INPUT_NOTIFICATION_CHANNEL)!!,
            )
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(
                ContextCompat.getString(
                    applicationContext,
                    params.inputData.getInt(INPUT_NOTIFICATION_TITLE, 0),
                )
            )
            .setSmallIcon(params.inputData.getInt(INPUT_NOTIFICATION_DRAWABLE, 0))
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

    /** Exports data to the specified URI (content://) */
    private suspend fun CsvExporter.exportToFile(uri: Uri, from: Instant, to: Instant) {
        applicationContext.contentResolver.openOutputStream(uri, "wt").use { os ->
            if (os == null) {
                Timber.w(
                    "OutputStream was null, maybe the content provider handling %s crashed",
                    uri.toString(),
                )

                throw IOException("OutputStream was null")
            }

            exportToOutputStream(os, from, to)
        }
    }
}
