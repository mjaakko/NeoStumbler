package xyz.malkki.neostumbler.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import java.time.ZonedDateTime
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.constants.PreferenceKeys

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

    private val settingsStore: DataStore<Preferences> by inject<DataStore<Preferences>>(PREFERENCES)

    private suspend fun getMaxAgeDays(): Int? {
        return settingsStore.data
            .map { prefs -> prefs[intPreferencesKey(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS)] }
            .firstOrNull()
    }

    override suspend fun doWork(): Result {
        val reportDao = reportDatabaseManager.reportDb.value.reportDao()

        val maxAgeDays = getMaxAgeDays() ?: DEFAULT_MAX_AGE_DAYS
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
