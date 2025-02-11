package xyz.malkki.neostumbler.export

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import java.nio.file.Files
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.extensions.copyTo

class DatabaseExportWorker(appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        const val INPUT_OUTPUT_URI = "uri"

        private const val DATABASE_EXPORT_NOTIFICATION_ID = 200001
    }

    private val reportDatabaseManager: ReportDatabaseManager by inject()

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
                applicationContext,
                StumblerApplication.EXPORT_NOTIFICATION_CHANNEL_ID,
            )
            .setOngoing(true)
            .setLocalOnly(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(
                ContextCompat.getString(applicationContext, R.string.notification_exporting_data)
            )
            .setSmallIcon(R.drawable.upload_file_24)
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

        val uri = Uri.parse(params.inputData.getString(INPUT_OUTPUT_URI)!!)

        val tempFile = createTempFile(applicationContext.cacheDir.toPath(), "export", "db")

        try {
            val reportDb = reportDatabaseManager.reportDb.value

            reportDb.openHelper.writableDatabase.copyTo(tempFile)

            applicationContext.contentResolver.openOutputStream(uri)!!.buffered().use { outputStream
                ->
                Files.copy(tempFile, outputStream)
            }

            return Result.success()
        } finally {
            tempFile.deleteIfExists()
        }
    }
}
