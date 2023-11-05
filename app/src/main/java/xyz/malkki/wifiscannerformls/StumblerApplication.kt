package xyz.malkki.wifiscannerformls

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.altbeacon.beacon.AltBeaconParser
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import timber.log.Timber
import xyz.malkki.wifiscannerformls.beacons.IBeaconParser
import xyz.malkki.wifiscannerformls.db.DbPruneWorker
import xyz.malkki.wifiscannerformls.db.ReportDatabase
import xyz.malkki.wifiscannerformls.geosubmit.ReportSendWorker
import xyz.malkki.wifiscannerformls.utils.UserAgentInterceptor
import java.io.File
import java.time.Duration

class StumblerApplication : Application() {
    val reportDb by lazy {
        Room.databaseBuilder(this,
                ReportDatabase::class.java,
                "report-db")
            .build()
    }

    val httpClient by lazy {
        val cacheDir = File(cacheDir, "okhttp")
        cacheDir.mkdirs()

        //TODO: is this cache actually used for anything?
        val cache = Cache(cacheDir, 10 * 1024 * 1024) // 10MB

        val userAgentVersion = if (BuildConfig.DEBUG) {
            "dev"
        } else {
            BuildConfig.VERSION_CODE
        }
        val userAgentInterceptor = UserAgentInterceptor("${BuildConfig.APPLICATION_ID}/${userAgentVersion}")

        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(HttpLoggingInterceptor(Timber::d).apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .cache(cache)
            .build()
    }

    val settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    val oneTimeActionsStore: DataStore<Preferences> by preferencesDataStore(name = "one_time_actions")

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val beaconManager = BeaconManager.getInstanceForApplication(this)

        //Try forcing foreground mode (this doesn't seem to work)
        beaconManager.setEnableScheduledScanJobs(false)
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

        //Schedule report uploading to MLS
        workManager.enqueueUniquePeriodicWork(
            ReportSendWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReportSendWorker>(Duration.ofHours(8))
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED,
                        requiresCharging = false,
                        requiresStorageNotLow = false,
                        requiresDeviceIdle = true,
                        requiresBatteryNotLow = true
                    )
                )
                .build()
        )

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

        val notificationChannel = NotificationChannel(
            STUMBLING_NOTIFICATION_CHANNEL_ID,
            getString(R.string.scanner_status_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setBypassDnd(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(notificationChannel)
    }

    companion object {
        const val STUMBLING_NOTIFICATION_CHANNEL_ID = "wifi_scan"
    }
}