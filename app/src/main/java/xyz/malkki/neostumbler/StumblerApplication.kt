package xyz.malkki.neostumbler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.StrictMode
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber
import xyz.malkki.neostumbler.crashlog.CrashLogManager
import xyz.malkki.neostumbler.crashlog.FileCrashLogManager
import xyz.malkki.neostumbler.data.battery.AndroidBatteryLevelMonitor
import xyz.malkki.neostumbler.data.battery.BatteryLevelMonitor
import xyz.malkki.neostumbler.data.geocoder.AndroidGeocoder
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.data.location.AndroidGpsStatusSource
import xyz.malkki.neostumbler.data.location.GpsStatusSource
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
        preferencesDataStore(
            name = "passive_scan_state",
            corruptionHandler =
                ReplaceFileCorruptionHandler { ex ->
                    /**
                     * The DataStore seems to get corrupted in some cases (see
                     * https://github.com/mjaakko/NeoStumbler/issues/863). If this happens, let's
                     * just recreate it with no content, because the state is only used to avoid
                     * creating useless reports
                     */
                    Timber.w(ex, "Passive scan state has been corrupted. Recreating an empty state")

                    emptyPreferences()
                },
        )

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            // Allow disk reads in strict mode, because it's not feasible to fix them all
            // (even AndroidX libraries do it..)
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().penaltyLog().build()
            )
        }

        val crashLogDirectory = filesDir.toPath().resolve("crash_log").createDirectories()

        setupCrashMonitoring(crashLogDirectory)

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

            modules(
                module {
                    single<BatteryLevelMonitor> { AndroidBatteryLevelMonitor(get()) }

                    single<GpsStatusSource> { AndroidGpsStatusSource(get()) }
                }
            )

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

        // Schedule worker for removing old reports
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
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

    private fun setupCrashMonitoring(crashLogDir: Path) {
        Thread.setDefaultUncaughtExceptionHandler(
            FileLoggingUncaughtExceptionHandler(
                directory = crashLogDir,
                nextHandler = Thread.getDefaultUncaughtExceptionHandler(),
            )
        )
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
