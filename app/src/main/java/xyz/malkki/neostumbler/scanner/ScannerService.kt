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
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.os.PowerManager.WakeLock
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFERRED
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.get
import xyz.malkki.neostumbler.extensions.isWifiScanThrottled
import xyz.malkki.neostumbler.location.LocationSourceProvider
import xyz.malkki.neostumbler.scanner.movement.ConstantMovementDetector
import xyz.malkki.neostumbler.scanner.movement.LocationBasedMovementDetector
import xyz.malkki.neostumbler.scanner.movement.MovementDetectorType
import xyz.malkki.neostumbler.scanner.movement.SignificantMotionMovementDetector
import xyz.malkki.neostumbler.scanner.source.BeaconLibraryBluetoothBeaconSource
import xyz.malkki.neostumbler.scanner.source.BluetoothBeaconSource
import xyz.malkki.neostumbler.scanner.source.MultiSubscriptionCellInfoSource
import xyz.malkki.neostumbler.scanner.source.TelephonyManagerCellInfoSource
import xyz.malkki.neostumbler.scanner.source.WifiManagerWifiAccessPointSource
import xyz.malkki.neostumbler.utils.GpsStats
import xyz.malkki.neostumbler.utils.getGpsStatsFlow
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class ScannerService : Service() {
    companion object {
        private const val MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE = 4321
        private const val STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE = 15415

        private const val NOTIFICATION_ID = 6666

        private const val EXTRA_AUTOSTART = "autostart"

        //Try to get new locations every 5 seconds
        private val LOCATION_INTERVAL = 5.seconds

        private val CELL_SCAN_INTERVAL = 10.seconds

        private val WIFI_SCAN_INTERVAL_THROTTLED = 30.seconds

        private val WIFI_SCAN_INTERVAL_UNTHROTTLED = 10.seconds

        private var _serviceRunning = false
        val serviceRunning: Boolean
            get() = _serviceRunning

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

        enum class NotificationStyle(val detailLevel: Int) {
            MINIMAL(1), BASIC(2), DETAILED(3)
        }
    }

    private lateinit var wakeLock: WakeLock
    private lateinit var wifiLock: WifiLock

    private lateinit var notificationManager: NotificationManager

    private lateinit var settingsStore: DataStore<Preferences>

    private lateinit var scanReportCreator: ScanReportCreator

    private lateinit var coroutineScope: CoroutineScope

    private val startedAt = System.currentTimeMillis()

    private var autostarted = true

    private var scanning = false

    private var notificationStyle = NotificationStyle.BASIC

    private val binder = ScannerServiceBinder()

    @SuppressLint("WakelockTimeout") //We don't know how long the service runs for -> no timeout
    override fun onCreate() {
        super.onCreate()

        _serviceRunning = true

        wakeLock = getSystemService<PowerManager>()!!.newWakeLock(PARTIAL_WAKE_LOCK, this::class.java.canonicalName).apply {
            acquire()
        }
        wifiLock = getSystemService<WifiManager>()!!.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, this::class.java.canonicalName).apply {
            acquire()
        }

        notificationManager = getSystemService()!!

        settingsStore = (application as StumblerApplication).settingsStore

        scanReportCreator = ScanReportCreator(this)

        coroutineScope = CoroutineScope(Dispatchers.Default)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() = coroutineScope.launch {
        if (scanning) {
            return@launch
        }

        scanning = true

        notificationStyle = settingsStore.data
            .map { prefs ->
                prefs.get<NotificationStyle>(PreferenceKeys.SCANNER_NOTIFICATION_STYLE) ?: NotificationStyle.BASIC
            }
            .first()

        val gpsActiveChannel = Channel<Boolean>()

        val gpsStatsFlow = gpsActiveChannel
            .consumeAsFlow()
            .distinctUntilChanged()
            .flatMapLatest { gpsActive ->
                if (gpsActive) {
                    getGpsStatsFlow(this@ScannerService)
                } else {
                    flowOf(null)
                }
            }

        val locationSource = LocationSourceProvider(this@ScannerService).getLocationSource()

        val locationFlow = locationSource
            .getLocations(LOCATION_INTERVAL)
            .onStart {
                gpsActiveChannel.send(true)
            }
            .onCompletion {
                gpsActiveChannel.send(false)
            }
            .shareIn(
                scope = this,
                started = SharingStarted.WhileSubscribed()
            )

        val cellInfoSource = if (hasReadPhoneStatePermission()) {
            MultiSubscriptionCellInfoSource(this@ScannerService)
        } else {
            TelephonyManagerCellInfoSource(this@ScannerService.getSystemService<TelephonyManager>()!!)
        }

        val wifiAccessPointSource = WifiManagerWifiAccessPointSource(this@ScannerService)

        val ignoreScanThrottling = settingsStore.data
            .map { it[booleanPreferencesKey(PreferenceKeys.IGNORE_SCAN_THROTTLING)] }
            .first() == true

        val wifiScanInterval = if (ignoreScanThrottling && isWifiScanThrottled() == false) {
            WIFI_SCAN_INTERVAL_UNTHROTTLED
        } else {
            WIFI_SCAN_INTERVAL_THROTTLED
        }

        val bluetoothBeaconSource = if (hasBluetoothScanPermission()) {
            BeaconLibraryBluetoothBeaconSource(this@ScannerService)
        } else {
            BluetoothBeaconSource { emptyFlow() }
        }

        val movementDetectorType = settingsStore.data
            .map { prefs ->
                prefs[stringPreferencesKey(PreferenceKeys.MOVEMENT_DETECTOR)]?.let {
                    try {
                        MovementDetectorType.valueOf(it)
                    } catch (ex: Exception) {
                        null
                    }
                } ?: MovementDetectorType.NONE
            }
            .first()

        val movementDetector = when (movementDetectorType) {
            MovementDetectorType.NONE -> ConstantMovementDetector
            MovementDetectorType.LOCATION -> LocationBasedMovementDetector {
                locationFlow.map { it.location }
            }
            MovementDetectorType.SIGNIFICANT_MOTION -> SignificantMotionMovementDetector(this@ScannerService.getSystemService<SensorManager>()!!)
        }

        Timber.i("Using ${movementDetector::class.simpleName} for detecting movement")

        val reportsCreatedChannel = Channel<Int>()

        launch {
            var reportsCreated = 0

            WirelessScanner(
                locationSource = { locationFlow },
                cellInfoSource = { cellInfoSource.getCellInfoFlow(CELL_SCAN_INTERVAL) },
                bluetoothBeaconSource = { bluetoothBeaconSource.getBluetoothBeaconFlow() },
                wifiAccessPointSource = { wifiAccessPointSource.getWifiAccessPointFlow(wifiScanInterval) },
                movementDetector = movementDetector
            )
                .createReports()
                .collect { reportData ->
                    scanReportCreator.createReport(
                        locationSource = reportData.location.source.name.lowercase(Locale.ROOT),
                        location = reportData.location.location,
                        cellTowers = reportData.cellTowers,
                        wifiScanResults = reportData.wifiAccessPoints,
                        beacons = reportData.bluetoothBeacons
                    )

                    reportsCreatedChannel.send(reportsCreated++)
                }
        }

        launch {
            reportsCreatedChannel.consumeAsFlow()
                .combine(gpsStatsFlow) { reportsCount, gpsStats ->
                    reportsCount to gpsStats
                }
                .collect { (reportsCount, gpsStats) ->
                    if (notificationManager.areNotificationsEnabled()) {
                        notificationManager.notify(NOTIFICATION_ID, createNotification(reportsCreated = reportsCount, gpsStats = gpsStats))
                    }
                }
        }

        startForeground(NOTIFICATION_ID, createNotification(reportsCreated = 0), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    private fun stopScanning() {
        scanning = false

        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            throw IllegalArgumentException("Intent should not be null")
        }

        if (intent.getBooleanExtra("start", false)) {
            if (autostarted) {
                autostarted = intent.getBooleanExtra(EXTRA_AUTOSTART, false)
            }

            startScanning()
        } else {
            //If autostop request is made, stop service only if it was started automatically
            val autostart = intent.getBooleanExtra(EXTRA_AUTOSTART, false)

            if (!autostart || autostarted == autostart) {
                stopScanning()
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        coroutineScope.cancel()

        _serviceRunning = false

        wifiLock.release()
        wakeLock.release()

        notificationManager.cancel(NOTIFICATION_ID)

        super.onDestroy()
    }

    private fun createNotification(reportsCreated: Int, gpsStats: GpsStats? = null): Notification {
        val intent = PendingIntent.getActivity(
            this@ScannerService,
            MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE,
            Intent(this@ScannerService, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopScanningPendingIntent = PendingIntent.getService(
            this@ScannerService,
            STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE,
            stopIntent(this@ScannerService, false),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reportsCreatedText = getString(R.string.notification_wireless_scanning_content_reports_created, reportsCreated)
        val satellitesInUseText = gpsStats?.let {
            getString(R.string.notification_wireless_scanning_content_satellite_stats, it.satellitesUsedInFix, it.satellitesTotal)
        }

        return NotificationCompat.Builder(this@ScannerService, StumblerApplication.STUMBLING_NOTIFICATION_CHANNEL_ID)
            .apply {
                setSmallIcon(R.drawable.radar_24)

                setContentTitle(getString(R.string.notification_wireless_scanning_title))

                if (notificationStyle == NotificationStyle.BASIC || (notificationStyle >= NotificationStyle.BASIC && satellitesInUseText == null)) {
                    setContentText(reportsCreatedText)
                } else if (notificationStyle.detailLevel >= NotificationStyle.BASIC.detailLevel) {
                    setContentText("$reportsCreatedText | $satellitesInUseText")

                    setStyle(NotificationCompat.BigTextStyle().bigText("""
                        $reportsCreatedText
                       
                        $satellitesInUseText
                    """.trimIndent()))
                }

                setPriority(NotificationCompat.PRIORITY_LOW)

                setOngoing(true)
                setAllowSystemGeneratedContextualActions(false)
                setOnlyAlertOnce(true)
                setLocalOnly(true)

                setCategory(Notification.CATEGORY_SERVICE)

                setForegroundServiceBehavior(if (autostarted) { FOREGROUND_SERVICE_DEFERRED } else { FOREGROUND_SERVICE_IMMEDIATE })

                setUsesChronometer(true)
                setShowWhen(true)
                setWhen(startedAt)

                setContentIntent(intent)
                addAction(NotificationCompat.Action(R.drawable.stop_24, getString(R.string.stop), stopScanningPendingIntent))
            }
            .build()
    }

    private fun hasBluetoothScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        checkMissingPermissions(Manifest.permission.BLUETOOTH_SCAN).isEmpty()
    } else {
        checkMissingPermissions(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN).isEmpty()
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return checkMissingPermissions(Manifest.permission.READ_PHONE_STATE).isEmpty()
    }

    public inner class ScannerServiceBinder : Binder() {
        fun getService(): ScannerService = this@ScannerService
    }
}