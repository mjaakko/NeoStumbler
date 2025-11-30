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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DatabaseExportWorker(appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        const val INPUT_OUTPUT_URI = "uri"
        const val INPUT_COMPRESS = "compress"

        const val INPUT_NOTIFICATION_CHANNEL = "notification_channel"
        const val INPUT_NOTIFICATION_TITLE = "notification_title"
        const val INPUT_NOTIFICATION_DRAWABLE = "notification_drawable"

        private const val DATABASE_EXPORT_NOTIFICATION_ID = 200001
    }

    private val databaseExporter: DatabaseExporter by inject()

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
                applicationContext,
                params.inputData.getString(INPUT_NOTIFICATION_CHANNEL)!!,
            )
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(
                applicationContext.getString(params.inputData.getInt(INPUT_NOTIFICATION_TITLE, 0))
            )
            .setSmallIcon(params.inputData.getInt(INPUT_NOTIFICATION_DRAWABLE, 0))
            .build()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            DATABASE_EXPORT_NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    override suspend fun doWork(): Result {
        if (!params.inputData.hasKeyWithValueOfType<String>(INPUT_OUTPUT_URI)) {
            Timber.e("Cannot export data because output URI is not specified")
            return Result.failure()
        }

        setForeground(getForegroundInfo())

        val compress = params.inputData.getBoolean(INPUT_COMPRESS, false)
        val uri = params.inputData.getString(INPUT_OUTPUT_URI)!!.toUri()

        val outputStream = applicationContext.contentResolver.openOutputStream(uri)!!

        databaseExporter.exportToOutputStream(outputStream, compress)

        return Result.success()
    }
}
