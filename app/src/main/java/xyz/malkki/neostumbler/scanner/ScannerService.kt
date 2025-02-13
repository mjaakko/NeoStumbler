package xyz.malkki.neostumbler.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.os.PowerManager.WakeLock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFERRED
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.getOrDefault
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.isWifiScanThrottled
import xyz.malkki.neostumbler.location.LocationSourceProvider
import xyz.malkki.neostumbler.scanner.movement.ConstantMovementDetector
import xyz.malkki.neostumbler.scanner.movement.LocationBasedMovementDetector
import xyz.malkki.neostumbler.scanner.movement.MovementDetector
import xyz.malkki.neostumbler.scanner.movement.MovementDetectorType
import xyz.malkki.neostumbler.scanner.movement.SignificantMotionMovementDetector
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerTileService
import xyz.malkki.neostumbler.scanner.source.AirPressureSource
import xyz.malkki.neostumbler.scanner.source.BeaconLibraryBluetoothBeaconSource
import xyz.malkki.neostumbler.scanner.source.BluetoothBeaconSource
import xyz.malkki.neostumbler.scanner.source.CellInfoSource
import xyz.malkki.neostumbler.scanner.source.MultiSubscriptionCellInfoSource
import xyz.malkki.neostumbler.scanner.source.PressureSensorAirPressureSource
import xyz.malkki.neostumbler.scanner.source.WifiManagerWifiAccessPointSource
import xyz.malkki.neostumbler.scanner.speed.SmoothenedGpsSpeedSource
import xyz.malkki.neostumbler.utils.GpsStats
import xyz.malkki.neostumbler.utils.getGpsStatsFlow

class ScannerService : Service() {
    companion object {
        private const val MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE = 4321
        private const val STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE = 15415

        private const val NOTIFICATION_ID = 6666

        private const val EXTRA_AUTOSTART = "autostart"

        /**
         * Try to get new locations every 3 seconds by default
         *
         * If the interval is too high, data quality will be worse because of the distance traveled
         * between the scan and the location fix. Also there can be some gaps on the map
         *
         * If the interval is too low, there will be many reports with just a few observations which
         * will decrease DB performance in the long term and the reports UI will be cluttered. Also
         * there seems to be a noticeable effect on battery life
         *
         * 3 seconds seems to give a reasonably good balance between these
         */
        const val DEFAULT_LOCATION_INTERVAL = 3

        // By default, try to scan Wi-Fis every 50 meters
        const val DEFAULT_WIFI_SCAN_DISTANCE: Int = 50

        // By default, try to scan cells every 120 meters
        const val DEFAULT_CELL_SCAN_DISTANCE: Int = 120

        fun startIntent(context: Context, autostart: Boolean = false): Intent {
            return Intent(context, ScannerService::class.java).apply {
                putExtra("start", true)
                putExtra(EXTRA_AUTOSTART, autostart)
            }
        }

        fun stopIntent(context: Context, autostart: Boolean = false): Intent {
            return Intent(context, ScannerService::class.java).apply {
                putExtra("start", false)
                putExtra(EXTRA_AUTOSTART, autostart)
            }
        }

        enum class NotificationStyle {
            MINIMAL,
            BASIC,
            DETAILED,
        }

        private val gpsActive = MutableStateFlow(false)

        private val _reportsCreated = MutableStateFlow(0)
        val reportsCreated: StateFlow<Int>
            get() = _reportsCreated.asStateFlow()

        private val _serviceRunning = MutableStateFlow(false)
        val serviceRunning: StateFlow<Boolean>
            get() = _serviceRunning.asStateFlow()
    }

    private val startedAt = System.currentTimeMillis()

    private val settingsStore: DataStore<Preferences> by inject<DataStore<Preferences>>(PREFERENCES)

    private val scanReportCreator: ScanReportCreator by inject()

    private val locationSourceProvider: LocationSourceProvider by inject()

    private lateinit var wakeLock: WakeLock

    private lateinit var notificationManager: NotificationManager

    private lateinit var sensorManager: SensorManager

    private lateinit var coroutineScope: CoroutineScope

    // PendingIntents for the notification
    private lateinit var mainActivityIntent: PendingIntent
    private lateinit var stopScanningIntent: PendingIntent

    private var autostarted = true

    private var scanning = false

    private var notificationStyle = NotificationStyle.BASIC

    private val binder = ScannerServiceBinder()

    @SuppressLint("WakelockTimeout") // We don't know how long the service runs for -> no timeout
    override fun onCreate() {
        super.onCreate()

        mainActivityIntent =
            PendingIntentCompat.getActivity(
                this,
                MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )!!

        stopScanningIntent =
            PendingIntentCompat.getService(
                this,
                STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE,
                stopIntent(this@ScannerService, false),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )!!

        startForeground(
            NOTIFICATION_ID,
            createNotification(reportsCreated = 0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )

        _serviceRunning.value = true

        ScannerTileService.updateTile(this)

        wakeLock =
            getSystemService<PowerManager>()!!
                .newWakeLock(PARTIAL_WAKE_LOCK, this::class.java.canonicalName)
                .apply { acquire() }

        notificationManager = getSystemService()!!

        sensorManager = getSystemService()!!

        coroutineScope = CoroutineScope(Dispatchers.Default)

        coroutineScope.launch {
            val gpsStatusFlow =
                gpsActive.flatMapLatest { isGpsActive ->
                    if (isGpsActive) {
                        @SuppressLint("MissingPermission") getGpsStatsFlow(this@ScannerService)
                    } else {
                        flowOf(null)
                    }
                }

            gpsStatusFlow
                .combine(reportsCreated) { a, b -> a to b }
                .collect { (gpsStats, reportsCount) ->
                    if (notificationManager.areNotificationsEnabled()) {
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            createNotification(reportsCreated = reportsCount, gpsStats = gpsStats),
                        )
                    }
                }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() =
        coroutineScope.launch {
            if (scanning) {
                return@launch
            }

            scanning = true

            notificationStyle =
                settingsStore.getOrDefault(
                    stringPreferencesKey(PreferenceKeys.SCANNER_NOTIFICATION_STYLE),
                    NotificationStyle.BASIC,
                )

            val wifiScanDistance =
                settingsStore.getOrDefault(
                    intPreferencesKey(PreferenceKeys.WIFI_SCAN_DISTANCE),
                    DEFAULT_WIFI_SCAN_DISTANCE,
                )

            val cellScanDistance =
                settingsStore.getOrDefault(
                    intPreferencesKey(PreferenceKeys.CELL_SCAN_DISTANCE),
                    DEFAULT_CELL_SCAN_DISTANCE,
                )

            val locationInterval =
                settingsStore.getOrDefault(
                    intPreferencesKey(PreferenceKeys.LOCATION_INTERVAL),
                    DEFAULT_LOCATION_INTERVAL,
                )

            Timber.d(
                "Scan distances: ${wifiScanDistance}m - Wi-Fis, ${cellScanDistance}m - cell towers"
            )

            val locationSource = locationSourceProvider.getLocationSource(this@ScannerService)

            val locationFlow =
                locationSource
                    .getLocations(locationInterval.seconds)
                    .onStart { gpsActive.emit(true) }
                    .onCompletion { gpsActive.emit(false) }
                    .shareIn(scope = this, started = SharingStarted.WhileSubscribed())

            val speedFlow = SmoothenedGpsSpeedSource(locationFlow).getSpeedFlow()

            val cellInfoSource = getCellInfoSource()

            val ignoreScanThrottlingPreference =
                settingsStore.getOrDefault(
                    booleanPreferencesKey(PreferenceKeys.IGNORE_SCAN_THROTTLING),
                    false,
                )

            val wifiScanThrottled = !ignoreScanThrottlingPreference || isWifiScanThrottled() == true

            val wifiAccessPointSource =
                WifiManagerWifiAccessPointSource(
                    this@ScannerService,
                    wifiScanThrottled = wifiScanThrottled,
                )

            val bluetoothBeaconSource = getBluetoothBeaconSource()

            val movementDetectorType =
                settingsStore.getOrDefault(
                    stringPreferencesKey(PreferenceKeys.MOVEMENT_DETECTOR),
                    MovementDetectorType.LOCATION,
                )

            val movementDetector = getMovementDetector(movementDetectorType, locationFlow)

            val airPressureSource = getAirPressureSource()

            WirelessScanner(
                    locationSource = { locationFlow },
                    cellInfoSource = {
                        val scanFrequencyFlow =
                            speedFlow.map { speed -> (cellScanDistance.toDouble() / speed).seconds }

                        cellInfoSource.getCellInfoFlow(scanFrequencyFlow)
                    },
                    bluetoothBeaconSource = { bluetoothBeaconSource.getBluetoothBeaconFlow() },
                    wifiAccessPointSource = {
                        val scanFrequencyFlow =
                            speedFlow.map { speed -> (wifiScanDistance.toDouble() / speed).seconds }

                        wifiAccessPointSource.getWifiAccessPointFlow(scanFrequencyFlow)
                    },
                    airPressureSource = {
                        airPressureSource.getAirPressureFlow(locationInterval.seconds / 2)
                    },
                    movementDetector = movementDetector,
                )
                .createReports()
                .collect { reportData ->
                    scanReportCreator.createReport(
                        position = reportData.position,
                        cellTowers = reportData.cellTowers,
                        wifiScanResults = reportData.wifiAccessPoints,
                        beacons = reportData.bluetoothBeacons,
                    )

                    _reportsCreated.update { it + 1 }

                    ScannerTileService.updateTile(this@ScannerService)
                }
        }

    private fun getMovementDetector(
        movementDetectorType: MovementDetectorType,
        locationFlow: Flow<Position>,
    ): MovementDetector {
        return when (movementDetectorType) {
            MovementDetectorType.NONE -> ConstantMovementDetector
            MovementDetectorType.LOCATION -> LocationBasedMovementDetector { locationFlow }
            MovementDetectorType.SIGNIFICANT_MOTION ->
                SignificantMotionMovementDetector(
                    sensorManager = sensorManager,
                    locationSource = { locationFlow },
                )
        }
    }

    private fun getAirPressureSource(): AirPressureSource {
        return if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            PressureSensorAirPressureSource(sensorManager)
        } else {
            Timber.w(
                "Device does not have an air pressure sensor, not collecting air pressure data"
            )

            AirPressureSource { emptyFlow() }
        }
    }

    private fun getCellInfoSource(): CellInfoSource {
        return if (hasReadPhoneStatePermission()) {
            MultiSubscriptionCellInfoSource(this@ScannerService)
        } else {
            CellInfoSource { emptyFlow<List<CellTower>>() }
        }
    }

    private fun getBluetoothBeaconSource(): BluetoothBeaconSource {
        return if (
            hasBluetoothScanPermission() &&
                (application as StumblerApplication).bluetoothScanAvailable
        ) {
            BeaconLibraryBluetoothBeaconSource(this@ScannerService)
        } else {
            BluetoothBeaconSource { emptyFlow() }
        }
    }

    private fun stopScanning() {
        scanning = false

        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        require(intent != null) { "Intent should not be null" }

        if (intent.getBooleanExtra("start", false)) {
            if (autostarted) {
                autostarted = intent.getBooleanExtra(EXTRA_AUTOSTART, false)
            }

            startScanning()
        } else {
            // If autostop request is made, stop service only if it was started automatically
            val autostart = intent.getBooleanExtra(EXTRA_AUTOSTART, false)

            if (!autostart || autostarted == autostart) {
                stopScanning()
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        coroutineScope.cancel()

        _serviceRunning.value = false
        _reportsCreated.value = 0

        ScannerTileService.updateTile(this)

        wakeLock.release()

        notificationManager.cancel(NOTIFICATION_ID)

        super.onDestroy()
    }

    private fun createNotification(reportsCreated: Int, gpsStats: GpsStats? = null): Notification {
        val reportsCreatedText =
            applicationContext.getQuantityString(
                R.plurals.notification_wireless_scanning_content_reports_created,
                reportsCreated,
                reportsCreated,
            )
        val satellitesInUseText =
            gpsStats?.let {
                ContextCompat.getString(
                        applicationContext,
                        R.string.notification_wireless_scanning_content_satellite_stats,
                    )
                    .format(it.satellitesUsedInFix, it.satellitesTotal)
            }

        return NotificationCompat.Builder(
                this@ScannerService,
                StumblerApplication.STUMBLING_NOTIFICATION_CHANNEL_ID,
            )
            .apply {
                setSmallIcon(R.drawable.radar_24)

                setContentTitle(
                    ContextCompat.getString(
                        applicationContext,
                        R.string.notification_wireless_scanning_title,
                    )
                )

                if (
                    notificationStyle == NotificationStyle.BASIC ||
                        (notificationStyle >= NotificationStyle.BASIC &&
                            satellitesInUseText == null)
                ) {
                    setContentText(reportsCreatedText)
                } else if (notificationStyle >= NotificationStyle.BASIC) {
                    setContentText("$reportsCreatedText | $satellitesInUseText")

                    setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                """
                        $reportsCreatedText
                       
                        $satellitesInUseText
                    """
                                    .trimIndent()
                            )
                    )
                }

                setPriority(NotificationCompat.PRIORITY_LOW)

                setOngoing(true)
                setAllowSystemGeneratedContextualActions(false)
                setOnlyAlertOnce(true)
                setLocalOnly(true)

                setCategory(Notification.CATEGORY_SERVICE)

                setForegroundServiceBehavior(
                    if (autostarted) {
                        FOREGROUND_SERVICE_DEFERRED
                    } else {
                        FOREGROUND_SERVICE_IMMEDIATE
                    }
                )

                setUsesChronometer(true)
                setShowWhen(true)
                setWhen(startedAt)

                setContentIntent(mainActivityIntent)
                addAction(
                    NotificationCompat.Action(
                        R.drawable.stop_24,
                        ContextCompat.getString(applicationContext, R.string.stop),
                        stopScanningIntent,
                    )
                )
            }
            .build()
    }

    private fun hasBluetoothScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkMissingPermissions(Manifest.permission.BLUETOOTH_SCAN).isEmpty()
        } else {
            checkMissingPermissions(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                )
                .isEmpty()
        }

    private fun hasReadPhoneStatePermission(): Boolean {
        return checkMissingPermissions(Manifest.permission.READ_PHONE_STATE).isEmpty()
    }

    inner class ScannerServiceBinder : Binder() {
        fun getService(): ScannerService = this@ScannerService
    }
}
