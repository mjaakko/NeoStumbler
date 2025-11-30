package xyz.malkki.neostumbler.export

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class WorkerDatabaseExportManager(
    context: Context,
    private val notificationChannelId: String,
    @StringRes private val notificationTitle: Int,
    @DrawableRes private val notificationIconDrawable: Int,
) : DatabaseExportManager {
    private val appContext: Context = context.applicationContext

    override fun startExportRawDatabase(outputFile: String, compress: Boolean) {
        WorkManager.getInstance(appContext)
            .enqueue(
                OneTimeWorkRequest.Builder(DatabaseExportWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putString(DatabaseExportWorker.INPUT_OUTPUT_URI, outputFile)
                            .putBoolean(DatabaseExportWorker.INPUT_COMPRESS, compress)
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
