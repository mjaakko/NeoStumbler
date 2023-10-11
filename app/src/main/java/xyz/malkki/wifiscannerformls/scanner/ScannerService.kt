package xyz.malkki.wifiscannerformls.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.telephony.CellInfo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFERRED
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import xyz.malkki.wifiscannerformls.MainActivity
import xyz.malkki.wifiscannerformls.R
import xyz.malkki.wifiscannerformls.StumblerApplication
import xyz.malkki.wifiscannerformls.common.LocationWithSource
import xyz.malkki.wifiscannerformls.extensions.buffer
import xyz.malkki.wifiscannerformls.extensions.checkMissingPermissions
import xyz.malkki.wifiscannerformls.extensions.combineAny
import xyz.malkki.wifiscannerformls.extensions.filterNotNullPairs
import xyz.malkki.wifiscannerformls.extensions.timestampMillis
import xyz.malkki.wifiscannerformls.extensions.timestampMillisCompat
import xyz.malkki.wifiscannerformls.utils.getBeaconFlow
import xyz.malkki.wifiscannerformls.utils.getCellInfoFlow
import xyz.malkki.wifiscannerformls.utils.getLocationFlow
import xyz.malkki.wifiscannerformls.utils.getWifiScanFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class ScannerService : Service() {
    companion object {
        private const val MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE = 4321

        private const val NOTIFICATION_ID = 6666

        private const val EXTRA_AUTOSTART = "autostart"

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

        val locationFlow = getLocationFlow(this@ScannerService, 10 * 1000)
            .runningFold<LocationWithSource, Pair<LocationWithSource?, LocationWithSource?>>(null to null) { pair, newLocation ->
                pair.second to newLocation
            }
            .filterNotNullPairs()

        val cellInfoFlow = getCellInfoFlow(this@ScannerService, scanInterval = 20.seconds)
            .map {
                if (it.isNotEmpty()) {
                    Timestamped(it.maxOf { cellInfo -> cellInfo.timestampMillisCompat }, it)
                } else {
                    null
                }
            }

        val wifiScanFlow = getWifiScanFlow(this@ScannerService)
            .buffer(20.seconds)
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
                    Timestamped(it.maxOf { scanResult -> scanResult.timestampMillis }, it)
                } else {
                    null
                }
            }

        val beaconsFlow = if (hasBluetoothScanPermission()) {
            getBeaconFlow(this@ScannerService)
                .buffer(20.seconds)
                .map { beacons ->
                    val now = System.currentTimeMillis()

                    beacons.flatten()
                        //Beacon library seems to sometimes return very old results -> filter them
                        .filter { (it.lastCycleDetectionTimestamp - now) < 20 * 1000 }
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
                        val maxTimestamp = beacons.maxOf { it.lastCycleDetectionTimestamp }
                        val maxTimestampElapsedRealtime = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - maxTimestamp)

                        Timestamped(maxTimestampElapsedRealtime, beacons)
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

                var reportData: Triple<Timestamped<List<CellInfo>>?, Timestamped<List<ScanResult>>?, Timestamped<List<Beacon>>?>? = null

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
                            .maxOfOrNull { timestamped ->
                                timestamped.timestampMillis
                            }

                        if (timestamp != null) {
                            val location = it.selectBetterLocation(timestamp * 1_000_000_000)

                            send(location to latestReportData)
                        }
                    }
                }
            }.collect {
                val (location, reportData) = it

                scanReportCreator.createReport(location.source.name.lowercase(Locale.ROOT), location.location,
                    cellInfo = reportData.first?.value ?: emptyList(),
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

        startForeground(NOTIFICATION_ID, createNotification())
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

        return NotificationCompat.Builder(this@ScannerService, StumblerApplication.STUMBLING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.radar_24)
            .setContentTitle(getString(R.string.notification_wireless_scanning_active))
            .setContentText(getString(R.string.notification_reports_created, reportsCreated))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAllowSystemGeneratedContextualActions(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(if (autostarted) { FOREGROUND_SERVICE_DEFERRED } else { FOREGROUND_SERVICE_IMMEDIATE })
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setWhen(startedAt)
            .setContentIntent(intent)
            .build()
    }

    private fun hasBluetoothScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        checkMissingPermissions(Manifest.permission.BLUETOOTH_SCAN).isEmpty()
    } else {
        checkMissingPermissions(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN).isEmpty()
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