package xyz.malkki.neostumbler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.altbeacon.beacon.AltBeaconParser
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber
import xyz.malkki.neostumbler.beacons.IBeaconParser
import xyz.malkki.neostumbler.beacons.StubDistanceCalculator
import xyz.malkki.neostumbler.db.DbPruneWorker
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.export.CsvExporter
import xyz.malkki.neostumbler.http.getCallFactory
import xyz.malkki.neostumbler.location.LocationSourceProvider
import xyz.malkki.neostumbler.scanner.ScanReportCreator
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel
import xyz.malkki.neostumbler.ui.viewmodel.StatisticsViewModel
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import java.time.Duration
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.properties.Delegates

val PREFERENCES = named("preferences")

class StumblerApplication : Application() {
    private val settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private val oneTimeActionsStore: DataStore<Preferences> by preferencesDataStore(name = "one_time_actions")

    var bluetoothScanAvailable by Delegates.notNull<Boolean>()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@StumblerApplication)

            modules(module {
                single {
                    ReportDatabaseManager(get())
                }
            })

            modules(module {
                factory {
                    CsvExporter(get(), get())
                }

                single {
                    ScanReportCreator(get())
                }
            })

            modules(module {
                single {
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.async(start = CoroutineStart.LAZY) {
                        getCallFactory(this@StumblerApplication)
                    }
                }
            })

            modules(module {
                single {
                    LocationSourceProvider(get(PREFERENCES))
                }
            })

            modules(module {
                single(PREFERENCES) {
                    settingsStore
                }

                single {
                    OneTimeActionHelper(oneTimeActionsStore)
                }
            })

            modules(module {
                viewModel {
                    MapViewModel(get(), get(PREFERENCES), get(), get(), get())
                }

                viewModel {
                    StatisticsViewModel(get())
                }

                viewModel {
                    ReportsViewModel(get())
                }
            })
        }

        deleteOsmDroidFiles()

        setupBeaconLibrary()

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
    
    private fun setupBeaconLibrary() {
        //Disable manifest checking, which seems to cause crashes on certain devices
        BeaconManager.setManifestCheckingDisabled(true)

        //Use stub distance calculator to avoid making unnecessary requests for fetching distance calibrations used by the Beacon Library
        Beacon.setDistanceCalculator(StubDistanceCalculator)

        try {
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

            bluetoothScanAvailable = true
        } catch (ex: Exception) {
            /**
             * Configuring beacon manager can throw an exception when the service has been disabled,
             * e.g. when using a custom ROM to block trackers
             */
            Timber.w(ex, "Failed to configure BeaconManager")

            bluetoothScanAvailable = false
        }
    }

    /**
     * Deletes files used by Osmdroid library (no longer used by NeoStumbler)
     */
    private fun deleteOsmDroidFiles() {
        val dataDirPath = dataDir.toPath()

        val osmdroidDir = dataDirPath.resolve("files").resolve("osmdroid")
        if (osmdroidDir.exists()) {
            Timber.d("Deleting OsmDroid files")
            osmdroidDir.deleteRecursively()
        }

        val osmdroidPrefs = dataDirPath.resolve("shared_prefs").resolve("osmdroid.xml")
        if (osmdroidPrefs.exists()) {
            Timber.d("Deleting OsmDroid preferences")
            osmdroidPrefs.deleteIfExists()
        }
    }

    companion object {
        const val STUMBLING_NOTIFICATION_CHANNEL_ID = "wifi_scan"

        const val EXPORT_NOTIFICATION_CHANNEL_ID = "data_exports"

        const val REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID = "report_upload"
    }
}