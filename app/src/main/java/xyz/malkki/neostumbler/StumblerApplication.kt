package xyz.malkki.neostumbler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.altbeacon.beacon.AltBeaconParser
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import timber.log.Timber
import xyz.malkki.neostumbler.beacons.IBeaconParser
import xyz.malkki.neostumbler.beacons.StubDistanceCalculator
import xyz.malkki.neostumbler.db.DbPruneWorker
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.utils.UserAgentInterceptor
import java.io.File
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

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
        val userAgentInterceptor = UserAgentInterceptor("${resources.getString(R.string.app_name)}/${userAgentVersion}")

        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(HttpLoggingInterceptor(Timber::d).apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .cache(cache)
            .connectTimeout(30.seconds.toJavaDuration())
            /* Read timeout should be long enough, because the Geosubmit API responds only when all data has been processed and
             that might take a while if a large amount of reports is sent at once */
            .readTimeout(2.minutes.toJavaDuration())
            .build()
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
            getString(R.string.scanner_status_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setBypassDnd(false)
        }
        notificationManager.createNotificationChannel(scannerNotificationChannel)

        val reportUploadNotificationChannel = NotificationChannel(
            REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID,
            getString(R.string.report_upload_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setBypassDnd(false)
        }
        notificationManager.createNotificationChannel(reportUploadNotificationChannel)

        val exportNotificationChannel = NotificationChannel(
            EXPORT_NOTIFICATION_CHANNEL_ID,
            getString(R.string.export_notification_channel_name),
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