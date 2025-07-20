package xyz.malkki.neostumbler.db

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import java.time.ZonedDateTime
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getIntFlow

/** Worker for deleting old scan reports from the local DB */
class DbPruneWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    companion object {
        const val PERIODIC_WORK_NAME = "db_prune_periodic"

        const val OUTPUT_REPORTS_DELETED = "reports_deleted"

        // By default delete reports older than 60 days
        const val DEFAULT_MAX_AGE_DAYS: Int = 60
    }

    private val reportDatabaseManager: ReportDatabaseManager by inject()

    private val settings: Settings by inject()

    private suspend fun getMaxAgeDays(): Int {
        return settings
            .getIntFlow(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS, DEFAULT_MAX_AGE_DAYS)
            .first()
    }

    override suspend fun doWork(): Result {
        val reportDao = reportDatabaseManager.reportDb.value.reportDao()

        val maxAgeDays = getMaxAgeDays()
        if (maxAgeDays < 0) {
            // If the max age is negative, DB pruning has been disabled in the settings -> succeed
            // immediately
            return Result.success()
        }

        val minTimestamp = ZonedDateTime.now().minusDays(maxAgeDays.toLong()).toInstant()

        Timber.i("Deleting reports older than $minTimestamp")

        val (deleteCount, duration) = measureTimedValue { reportDao.deleteOlderThan(minTimestamp) }

        Timber.i("Deleted $deleteCount reports in ${duration.toString(DurationUnit.SECONDS, 1)}")

        return Result.success(Data.Builder().putInt(OUTPUT_REPORTS_DELETED, deleteCount).build())
    }
}
