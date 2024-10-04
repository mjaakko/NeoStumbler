package xyz.malkki.neostumbler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.Call
import org.altbeacon.beacon.AltBeaconParser
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import timber.log.Timber
import xyz.malkki.neostumbler.beacons.IBeaconParser
import xyz.malkki.neostumbler.beacons.StubDistanceCalculator
import xyz.malkki.neostumbler.db.DbPruneWorker
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.http.getCallFactory
import java.time.Duration

@OptIn(DelicateCoroutinesApi::class)
class StumblerApplication : Application() {
    val reportDb by lazy {
        Room.databaseBuilder(this,
                ReportDatabase::class.java,
                "report-db")
            .build()
    }

    val httpClientProvider: Deferred<Call.Factory> = GlobalScope.async(start = CoroutineStart.LAZY) {
        getCallFactory(this@StumblerApplication)
    }

    val settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    val oneTimeActionsStore: DataStore<Preferences> by preferencesDataStore(name = "one_time_actions")

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        //Disable manifest checking, which seems to cause crashes on certain devices
        BeaconManager.setManifestCheckingDisabled(true)

        //Use stub distance calculator to avoid making unnecessary requests for fetching distance calibrations used by the Beacon Library
        Beacon.setDistanceCalculator(StubDistanceCalculator)

        val beaconManager = BeaconManager.getInstanceForApplication(this)

        //Try forcing foreground mode (this doesn't seem to work)
        beaconManager.setEnableScheduledScanJobs(false)
        @Suppress("DEPRECATION")
        beaconManager.backgroundMode = false

        beaconManager.backgroundBetweenScanPeriod = 5 * 1000
        beaconManager.backgroundScanPeriod = 1100

        beaconManager.foregroundBetweenScanPeriod = 5 * 1000
        beaconManager.foregroundScanPeriod = 1100
        //Max age for beacons: 10 seconds
        beaconManager.setMaxTrackingAge(10 * 1000)

        //Add parsers for common beacons types
        beaconManager.beaconParsers.apply {
            add(IBeaconParser)
            add(AltBeaconParser())
            add(BeaconParser(BeaconParser.URI_BEACON_LAYOUT))
            add(BeaconParser(BeaconParser.EDDYSTONE_TLM_LAYOUT))
            add(BeaconParser(BeaconParser.EDDYSTONE_UID_LAYOUT))
            add(BeaconParser(BeaconParser.EDDYSTONE_URL_LAYOUT))
        }

        val workManager = WorkManager.getInstance(this)

        //Schedule worker for removing old reports
        workManager.enqueueUniquePeriodicWork(
            DbPruneWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DbPruneWorker>(Duration.ofDays(1))
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.NOT_REQUIRED,
                        requiresCharging = false,
                        requiresStorageNotLow = false,
                        requiresDeviceIdle = true,
                        requiresBatteryNotLow = true
                    )
                )
                .build()
        )

        setupNotificationChannels()
    }

    private fun setupNotificationChannels() {
        val notificationManager = getSystemService<NotificationManager>()!!

        val scannerNotificationChannel = NotificationChannel(
            STUMBLING_NOTIFICATION_CHANNEL_ID,
            ContextCompat.getString(this, R.string.scanner_status_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setBypassDnd(false)
        }
        notificationManager.createNotificationChannel(scannerNotificationChannel)

        val reportUploadNotificationChannel = NotificationChannel(
            REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID,
            ContextCompat.getString(this, R.string.report_upload_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setBypassDnd(false)
        }
        notificationManager.createNotificationChannel(reportUploadNotificationChannel)

        val exportNotificationChannel = NotificationChannel(
            EXPORT_NOTIFICATION_CHANNEL_ID,
            ContextCompat.getString(this, R.string.export_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
            setBypassDnd(false)
        }
        notificationManager.createNotificationChannel(exportNotificationChannel)
    }

    companion object {
        const val STUMBLING_NOTIFICATION_CHANNEL_ID = "wifi_scan"

        const val EXPORT_NOTIFICATION_CHANNEL_ID = "data_exports"

        const val REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID = "report_upload"
    }
}