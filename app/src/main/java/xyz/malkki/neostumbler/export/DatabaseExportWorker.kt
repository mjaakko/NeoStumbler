package xyz.malkki.neostumbler.export

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.FileUtils
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.data.reports.RawReportImportExport
import xyz.malkki.neostumbler.extensions.getTextCompat

class DatabaseExportWorker(appContext: Context, private val params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        const val INPUT_OUTPUT_URI = "uri"
        const val INPUT_COMPRESS = "compress"

        private const val DATABASE_EXPORT_NOTIFICATION_ID = 200001
    }

    private val rawReportsImportExport: RawReportImportExport by inject()

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

        val tempFile = createTempFile(applicationContext.cacheDir.toPath(), "export", "db")

        try {
            rawReportsImportExport.exportRawReports(tempFile)

            val rawOutputStream = applicationContext.contentResolver.openOutputStream(uri)!!

            val outputStream =
                if (compress) {
                    GZIPOutputStream(rawOutputStream)
                } else {
                    rawOutputStream
                }

            tempFile.inputStream().use { input ->
                outputStream.use { output -> FileUtils.copy(input, output) }
            }

            return Result.success()
        } finally {
            tempFile.deleteIfExists()
        }
    }
}
