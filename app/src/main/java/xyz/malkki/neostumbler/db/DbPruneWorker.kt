package xyz.malkki.neostumbler.db

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import java.time.ZonedDateTime
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/**
 * Worker for deleting old scan reports from the local DB
 */
class DbPruneWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        const val PERIODIC_WORK_NAME = "db_prune_periodic"

        const val OUTPUT_REPORTS_DELETED = "reports_deleted"
    }

    private suspend fun getMaxAgeDays(): Long? {
        return (applicationContext as StumblerApplication).settingsStore.data
            .map { prefs ->
                prefs[intPreferencesKey(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS)]?.toLong()
            }
            .firstOrNull()
    }

    override suspend fun doWork(): Result {
        val db = (applicationContext as StumblerApplication).reportDb
        val reportDao = db.reportDao()

        //By default delete reports older than 60 days
        val maxAgeDays = getMaxAgeDays() ?: 60
        if (maxAgeDays < 0) {
            //If the max age is negative, DB pruning has been disabled in the settings -> succeed immediately
            return Result.success()
        }

        val minTimestamp = ZonedDateTime.now().minusDays(maxAgeDays).toInstant()

        Timber.i("Deleting reports older than $minTimestamp")

        val (deleteCount, duration) = measureTimedValue {
            reportDao.deleteOlderThan(minTimestamp)
        }

        Timber.i("Deleted $deleteCount reports in ${duration.toString(DurationUnit.SECONDS, 1)}")

        return Result.success(Data.Builder().putInt(OUTPUT_REPORTS_DELETED, deleteCount).build())
    }
}