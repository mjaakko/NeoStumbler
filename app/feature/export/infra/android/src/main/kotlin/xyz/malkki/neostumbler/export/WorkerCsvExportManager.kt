package xyz.malkki.neostumbler.export

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.time.Instant

class WorkerCsvExportManager(
    private val notificationChannelId: String,
    @StringRes private val notificationTitle: Int,
    @DrawableRes private val notificationIconDrawable: Int,
    context: Context,
) : CsvExportManager {
    private val appContext: Context = context.applicationContext

    override fun startExport(fromInstant: Instant, toInstant: Instant, outputFile: String) {
        WorkManager.getInstance(appContext)
            .enqueue(
                OneTimeWorkRequest.Builder(CsvExportWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putString(CsvExportWorker.INPUT_OUTPUT_URI, outputFile)
                            .putLong(CsvExportWorker.INPUT_FROM, fromInstant.toEpochMilli())
                            .putLong(CsvExportWorker.INPUT_TO, toInstant.toEpochMilli())
                            .putString(
                                CsvExportWorker.INPUT_NOTIFICATION_CHANNEL,
                                notificationChannelId,
                            )
                            .putInt(CsvExportWorker.INPUT_NOTIFICATION_TITLE, notificationTitle)
                            .putInt(
                                CsvExportWorker.INPUT_NOTIFICATION_DRAWABLE,
                                notificationIconDrawable,
                            )
                            .build()
                    )
                    .setConstraints(Constraints.NONE)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )
    }
}
