package xyz.malkki.neostumbler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.altbeacon.beacon.AltBeaconParser
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Settings
import org.altbeacon.beacon.distance.DistanceCalculator
import org.altbeacon.beacon.distance.DistanceCalculatorFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber
import xyz.malkki.neostumbler.beaconlibrary.IBeaconParser
import xyz.malkki.neostumbler.beaconlibrary.StubDistanceCalculator
import xyz.malkki.neostumbler.crashlog.CrashLogManager
import xyz.malkki.neostumbler.crashlog.FileCrashLogManager
import xyz.malkki.neostumbler.data.battery.AndroidBatteryLevelMonitor
import xyz.malkki.neostumbler.data.battery.BatteryLevelMonitor
import xyz.malkki.neostumbler.data.geocoder.AndroidGeocoder
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.data.reports.RawReportImportExport
import xyz.malkki.neostumbler.data.reports.ReportExportProvider
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportRemover
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.reports.ReportStatisticsProvider
import xyz.malkki.neostumbler.data.reports.ReportStorageMetadataProvider
import xyz.malkki.neostumbler.data.settings.DataStoreSettings
import xyz.malkki.neostumbler.db.DbPruneWorker
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.db.RoomRawReportImportExport
import xyz.malkki.neostumbler.db.RoomReportExportProvider
import xyz.malkki.neostumbler.db.RoomReportProvider
import xyz.malkki.neostumbler.db.RoomReportRemover
import xyz.malkki.neostumbler.db.RoomReportSaver
import xyz.malkki.neostumbler.db.RoomReportStatisticsProvider
import xyz.malkki.neostumbler.db.RoomReportStorageMetadataProvider
import xyz.malkki.neostumbler.export.CsvExporter
import xyz.malkki.neostumbler.extensions.getTextCompat
import xyz.malkki.neostumbler.http.getCallFactory
import xyz.malkki.neostumbler.location.locationModule
import xyz.malkki.neostumbler.scanner.passive.passiveScanningModule
import xyz.malkki.neostumbler.scanner.postprocess.postProcessorsModule
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel
import xyz.malkki.neostumbler.ui.viewmodel.StatisticsViewModel
import xyz.malkki.neostumbler.utils.FileLoggingUncaughtExceptionHandler
import xyz.malkki.neostumbler.utils.OneTimeActionHelper

val PREFERENCES = named("preferences")

val PASSIVE_SCAN_STATE = named("passive_scan_state")

class StumblerApplication : Application() {
    private val settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private val oneTimeActionsStore: DataStore<Preferences> by
        preferencesDataStore(name = "one_time_actions")

    private val passiveScanStateStore: DataStore<Preferences> by
        preferencesDataStore(name = "passive_scan_state")

    var bluetoothScanAvailable by Delegates.notNull<Boolean>()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val crashLogDirectory = filesDir.toPath().resolve("crash_log").createDirectories()

        Thread.setDefaultUncaughtExceptionHandler(
            FileLoggingUncaughtExceptionHandler(
                directory = crashLogDirectory,
                nextHandler = Thread.getDefaultUncaughtExceptionHandler(),
            )
        )

        startKoin {
            androidContext(this@StumblerApplication)

            modules(module { single<CrashLogManager> { FileCrashLogManager(crashLogDirectory) } })

            modules(
                module {
                    single { ReportDatabaseManager(get()) }

                    single<ReportStorageMetadataProvider> {
                        RoomReportStorageMetadataProvider(get())
                    }

                    single<RawReportImportExport> { RoomRawReportImportExport(get(), get()) }

                    single<ReportStatisticsProvider> { RoomReportStatisticsProvider(get()) }

                    single<ReportProvider> { RoomReportProvider(get()) }

                    single<ReportSaver> { RoomReportSaver(get()) }

                    single<ReportRemover> { RoomReportRemover(get()) }

                    single<ReportExportProvider> { RoomReportExportProvider(get()) }
                }
            )

            modules(module { single<BatteryLevelMonitor> { AndroidBatteryLevelMonitor(get()) } })

            modules(module { single<Geocoder> { AndroidGeocoder(get()) } })

            modules(module { factory { CsvExporter(get(), get()) } })

            modules(
                module {
                    single {
                        @OptIn(DelicateCoroutinesApi::class)
                        GlobalScope.async(start = CoroutineStart.LAZY) {
                            getCallFactory(this@StumblerApplication)
                        }
                    }
                }
            )

            modules(locationModule)

            modules(postProcessorsModule)

            modules(passiveScanningModule)

            modules(
                module {
                    single(PASSIVE_SCAN_STATE) { passiveScanStateStore }

                    single(PREFERENCES) { settingsStore }

                    single<xyz.malkki.neostumbler.data.settings.Settings> {
                        DataStoreSettings(get(PREFERENCES))
                    }

                    single { OneTimeActionHelper(oneTimeActionsStore) }
                }
            )

            modules(
                module {
                    viewModel { MapViewModel(get(), get(), get(), get(), get()) }

                    viewModel { StatisticsViewModel(get()) }

                    viewModel { ReportsViewModel(get(), get()) }
                }
            )
        }

        deleteOsmDroidFiles()

        setupBeaconLibrary()

        val workManager = WorkManager.getInstance(this)

        // Schedule worker for removing old reports
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
                        requiresBatteryNotLow = true,
                    )
                )
                .build(),
        )

        setupNotificationChannels()
    }

    private fun setupNotificationChannels() {
        val notificationManager = getSystemService<NotificationManager>()!!

        val scannerNotificationChannel =
            NotificationChannel(
                    STUMBLING_NOTIFICATION_CHANNEL_ID,
                    getTextCompat(R.string.scanner_status_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply {
                    setShowBadge(false)
                    setBypassDnd(false)
                }
        notificationManager.createNotificationChannel(scannerNotificationChannel)

        val reportUploadNotificationChannel =
            NotificationChannel(
                    REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID,
                    getTextCompat(R.string.report_upload_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply {
                    setShowBadge(false)
                    setBypassDnd(false)
                }
        notificationManager.createNotificationChannel(reportUploadNotificationChannel)

        val exportNotificationChannel =
            NotificationChannel(
                    EXPORT_NOTIFICATION_CHANNEL_ID,
                    getTextCompat(R.string.export_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .apply {
                    setShowBadge(false)
                    setBypassDnd(false)
                }
        notificationManager.createNotificationChannel(exportNotificationChannel)
    }

    @Suppress("MagicNumber", "TooGenericExceptionCaught")
    private fun setupBeaconLibrary() {
        // Disable manifest checking, which seems to cause crashes on certain devices
        BeaconManager.setManifestCheckingDisabled(true)

        try {
            val beaconManager = BeaconManager.getInstanceForApplication(this)

            beaconManager.adjustSettings(
                Settings(
                    distanceCalculatorFactory =
                        object : DistanceCalculatorFactory {
                            override fun getInstance(context: Context): DistanceCalculator {
                                // Use stub distance calculator to avoid making unnecessary requests
                                // for fetching distance calibrations used by the Beacon Library
                                return StubDistanceCalculator
                            }
                        }
                )
            )

            // Try forcing foreground mode (this doesn't seem to work)
            beaconManager.setEnableScheduledScanJobs(false)
            @Suppress("DEPRECATION")
            beaconManager.backgroundMode = false

            beaconManager.backgroundBetweenScanPeriod = 5 * 1000
            beaconManager.backgroundScanPeriod = 1100

            beaconManager.foregroundBetweenScanPeriod = 5 * 1000
            beaconManager.foregroundScanPeriod = 1100
            // Max age for beacons: 10 seconds
            beaconManager.setMaxTrackingAge(10 * 1000)

            // Add parsers for common beacons types
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

    /** Deletes files used by Osmdroid library (no longer used by NeoStumbler) */
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
