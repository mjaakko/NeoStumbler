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
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.os.PowerManager.WakeLock
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFERRED
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.altbeacon.beacon.Beacon
import timber.log.Timber
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.common.LocationWithSource
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.extensions.buffer
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.combineAny
import xyz.malkki.neostumbler.extensions.filterNotNullPairs
import xyz.malkki.neostumbler.extensions.timestampMillis
import xyz.malkki.neostumbler.location.LocationSourceProvider
import xyz.malkki.neostumbler.scanner.source.MultiSubscriptionCellInfoSource
import xyz.malkki.neostumbler.scanner.source.TelephonyManagerCellInfoSource
import xyz.malkki.neostumbler.utils.getBeaconFlow
import xyz.malkki.neostumbler.utils.getWifiScanFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class ScannerService : Service() {
    companion object {
        private const val MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE = 4321
        private const val STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE = 15415

        private const val NOTIFICATION_ID = 6666

        private const val EXTRA_AUTOSTART = "autostart"

        //Scan devices in slightly less than 20 second windows
        private val SCAN_BUFFER_PERIOD = 19.5.seconds

        //Try to get new locations every 10 seconds and then choose the one with least time difference
        private val LOCATION_INTERVAL = 10.seconds

        private val LOCATION_MAX_AGE = 20.seconds

        //Filter locations where accuracy is higher than 200 meters
        private const val LOCATION_MAX_ACCURACY = 200

        //Maximum age for beacons
        private val BEACON_MAX_AGE = 20.seconds

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

    private var reportsCreated = 0

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

        val locationSource = LocationSourceProvider(this@ScannerService).getLocationSource()

        val locationFlow = locationSource.getLocations(LOCATION_INTERVAL)
            .filter {
                (SystemClock.elapsedRealtimeNanos() - it.location.elapsedRealtimeNanos).nanoseconds < LOCATION_MAX_AGE
            }
            .filter {
                //Filter too inaccurate locations to avoid sending low quality data to MLS
                it.location.hasAccuracy() && it.location.accuracy <= LOCATION_MAX_ACCURACY
            }
            .runningFold<LocationWithSource, Pair<LocationWithSource?, LocationWithSource?>>(null to null) { pair, newLocation ->
                pair.second to newLocation
            }
            .filterNotNullPairs()

        val cellInfoSource = if (hasReadPhoneStatePermission()) {
            MultiSubscriptionCellInfoSource(this@ScannerService)
        } else {
            TelephonyManagerCellInfoSource(this@ScannerService.getSystemService<TelephonyManager>()!!)
        }

        val cellInfoFlow = cellInfoSource.getCellInfoFlow(SCAN_BUFFER_PERIOD)
            .map {
                if (it.isNotEmpty()) {
                    Timestamped(it.maxOf { cellInfo -> cellInfo.timestamp }, it)
                } else {
                    null
                }
            }

        val wifiScanFlow = getWifiScanFlow(this@ScannerService)
            .buffer(SCAN_BUFFER_PERIOD)
            .map { scanResults ->
                //Android seems to return multiple batches of scan results in short succession
                // -> batch them in 30s intervals and group by BSSID to find the latest
                scanResults.flatten()
                    .groupBy { scanResult -> scanResult.BSSID }
                    .mapValues { scanResult ->
                        scanResult.value.maxBy { it.timestamp }
                    }
                    .values.toList()
            }
            //Filter Wi-Fi access points that are not accepted by Mozilla
            .map { it.filterForMLS() }
            .map {
                //Only create reports if at least two access points were detected
                if (it.size >= 2) {
                    it
                } else {
                    //Map to empty list to create report with no Wi-Fi data (instead of filtering and using old data)
                    emptyList()
                }
            }
            .map {
                if (it.isNotEmpty()) {
                    Timestamped(it.map { scanResult -> scanResult.timestampMillis }.average().roundToLong(), it)
                } else {
                    null
                }
            }

        val beaconsFlow = if (hasBluetoothScanPermission()) {
            getBeaconFlow(this@ScannerService)
                .buffer(SCAN_BUFFER_PERIOD)
                .map { beacons ->
                    val now = System.currentTimeMillis()

                    beacons.flatten()
                        //Beacon library seems to sometimes return very old results -> filter them
                        .filter { (it.lastCycleDetectionTimestamp - now).milliseconds < BEACON_MAX_AGE }
                        .groupBy { it.bluetoothAddress }
                        .mapValues { beacon ->
                            beacon.value.maxBy {
                                it.lastCycleDetectionTimestamp
                            }
                        }
                        .values.toList()
                }
                .map { beacons ->
                    if (beacons.isNotEmpty()) {
                        val avgTimestamp = beacons.map { it.lastCycleDetectionTimestamp }.average().roundToLong()
                        val avgTimestampElapsedRealtime = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - avgTimestamp)

                        Timestamped(avgTimestampElapsedRealtime, beacons)
                    } else {
                        null
                    }
                }
        } else {
            Timber.d("No Bluetooth scan permissions, not scanning for beacons")

            emptyFlow()
        }

        val reportDataFlow = cellInfoFlow
            .combineAny(wifiScanFlow, beaconsFlow) { cellInfos, scanResults, beacons ->
                Triple(cellInfos, scanResults, beacons)
            }
            .filter { it.first != null || it.second != null || it.third != null }

        launch {
            channelFlow {
                val mutex = Mutex()

                var reportData: Triple<Timestamped<List<CellTower>>?, Timestamped<List<ScanResult>>?, Timestamped<List<Beacon>>?>? = null

                launch {
                    reportDataFlow.collect {
                        mutex.withLock {
                            reportData = it
                        }
                    }
                }

                locationFlow.collect {
                    val latestReportData = mutex.withLock {
                        val copy = reportData
                        reportData = null
                        copy
                    }

                    if (latestReportData != null) {
                        val timestamp = latestReportData.toList()
                            .filterNotNull()
                            .map { timestamped -> timestamped.timestampMillis }
                            .average()

                        if (!timestamp.isNaN()) {
                            val location = it.selectBetterLocation(timestamp.roundToLong() * 1_000_000)

                            send(location to latestReportData)
                        }
                    }
                }
            }.collect {
                val (location, reportData) = it

                scanReportCreator.createReport(location.source.name.lowercase(Locale.ROOT), location.location,
                    cellTowers = reportData.first?.value ?: emptyList(),
                    wifiScanResults = reportData.second?.value ?: emptyList(),
                    beacons = reportData.third?.value ?: emptyList()
                )

                reportsCreated++

                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(NOTIFICATION_ID, createNotification())
                }
            }
        }

        launch {
            val wifiManager: WifiManager = this@ScannerService.applicationContext.getSystemService()!!

            while (true) {
                if (wifiManager.startScan()) {
                    //Every 30s -> four times in 2 minutes
                    delay(30 * 1000)
                } else {
                    //Wi-Fi scan was not started, maybe we hit scan throttling -> wait longer
                    delay(60 * 1000)
                }
            }
        }

        startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
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

        super.onDestroy()
    }

    private fun createNotification(): Notification {
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

        return NotificationCompat.Builder(this@ScannerService, StumblerApplication.STUMBLING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.radar_24)
            .setContentTitle(getString(R.string.notification_wireless_scanning_active))
            .setContentText(getString(R.string.notification_reports_created, reportsCreated))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAllowSystemGeneratedContextualActions(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(if (autostarted) { FOREGROUND_SERVICE_DEFERRED } else { FOREGROUND_SERVICE_IMMEDIATE })
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setWhen(startedAt)
            .setContentIntent(intent)
            .addAction(NotificationCompat.Action(R.drawable.stop_24, getString(R.string.stop), stopScanningPendingIntent))
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

    private fun Pair<LocationWithSource, LocationWithSource>.selectBetterLocation(elapsedRealtimeNanos: Long): LocationWithSource {
        return toList().minBy {
            abs(elapsedRealtimeNanos - it.location.elapsedRealtimeNanos)
        }
    }

    private data class Timestamped<V>(val timestampMillis: Long, val value: V)

    public inner class ScannerServiceBinder : Binder() {
        fun getService(): ScannerService = this@ScannerService
    }
}