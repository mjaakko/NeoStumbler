package xyz.malkki.neostumbler.activescan

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.os.PowerManager.WakeLock
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import java.time.Instant
import java.util.EnumSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import xyz.malkki.neostumbler.activescan.adapter.NotificationParams
import xyz.malkki.neostumbler.activescan.adapter.NotificationStyle
import xyz.malkki.neostumbler.activescan.adapter.ScanNotificationAdapter
import xyz.malkki.neostumbler.activescan.adapter.ScannerQSTileAdapter
import xyz.malkki.neostumbler.coroutineservice.CoroutineService
import xyz.malkki.neostumbler.data.location.GpsStatus
import xyz.malkki.neostumbler.data.location.GpsStatusSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow

private const val WAKE_LOCK_TAG = "xyz.malkki.neostumbler:ActiveScanService"

private const val NOTIFICATION_ID = 55555

private const val EXTRA_START = "start"
private const val EXTRA_AUTOSTART = "autostart"

class ActiveScanService : CoroutineService() {
    companion object {
        private val _gpsStatus = MutableStateFlow<GpsStatus?>(null)
        internal val gpsStatus = _gpsStatus.asStateFlow()

        private val _reportsCreated = MutableStateFlow(0)
        internal val reportsCreated = _reportsCreated.asStateFlow()

        private val _scanState = MutableStateFlow<ScanState>(ScanState.Stopped)
        internal val scanState = _scanState.asStateFlow()

        /** @param autostart Whether the service is started without user interaction */
        fun startIntent(context: Context, autostart: Boolean = false): Intent {
            return Intent(context, ActiveScanService::class.java).apply {
                putExtra(EXTRA_START, true)
                putExtra(EXTRA_AUTOSTART, autostart)
            }
        }

        fun stopIntent(context: Context, autostart: Boolean = false): Intent {
            return Intent(context, ActiveScanService::class.java).apply {
                putExtra(EXTRA_START, false)
                putExtra(EXTRA_AUTOSTART, autostart)
            }
        }
    }

    private val settingsAwareActiveReportCreator: SettingsAwareActiveReportCreator by inject()

    private val gpsStatusSource: GpsStatusSource by inject()

    private val settings: Settings by inject()

    private val scanNotificationAdapter: ScanNotificationAdapter by inject()

    private val scannerQSTileAdapter: ScannerQSTileAdapter by inject()

    private lateinit var notificationManager: NotificationManager

    private lateinit var wakeLock: WakeLock

    private lateinit var startedAt: Instant

    private lateinit var notificationStyle: StateFlow<NotificationStyle>

    /** Whether the service was started without explicit user input */
    private var serviceAutostarted = true

    private val gpsActive = MutableStateFlow(false)

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()

        startedAt = Instant.now()

        notificationManager = getSystemService<NotificationManager>()!!

        wakeLock =
            getSystemService<PowerManager>()!!.newWakeLock(PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire()
            }

        notificationStyle =
            settings
                .getEnumFlow<NotificationStyle>(
                    ActiveScanPreferenceKeys.SCANNER_NOTIFICATION_STYLE,
                    default = NotificationStyle.BASIC,
                )
                .stateIn(
                    serviceScope,
                    started = SharingStarted.Eagerly,
                    initialValue = NotificationStyle.BASIC,
                )

        serviceScope.launch {
            gpsActive
                .flatMapLatest {
                    if (it) {
                        gpsStatusSource.getGpsStatusFlow()
                    } else {
                        flowOf(null)
                    }
                }
                .collect(_gpsStatus)
        }

        serviceScope.launch {
            combine(reportsCreated, gpsStatus, scanState) { reportsCreated, gpsStatus, scanState ->
                    NotificationParams(
                        notificationStyle = notificationStyle.value,
                        startedAt = startedAt,
                        state = scanState,
                        autostarted = serviceAutostarted,
                        reportsCreated = reportsCreated,
                        gpsStatus = gpsStatus,
                        stopIntent = stopIntent(this@ActiveScanService),
                    )
                }
                .collect { notificationParams ->
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        scanNotificationAdapter.createNotification(
                            context = this@ActiveScanService,
                            params = notificationParams,
                        ),
                    )
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        require(intent != null) { "Intent should not be null" }

        val autostart = intent.getBooleanExtra(EXTRA_AUTOSTART, false)
        if (!intent.getBooleanExtra(EXTRA_START, true) && (!serviceAutostarted || !autostart)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (serviceAutostarted) {
            serviceAutostarted = autostart
        }

        if (_scanState.value !is ScanState.Stopped) {
            return START_REDELIVER_INTENT
        }

        _scanState.value = ScanState.Active

        scannerQSTileAdapter.updateQuickSettingsTile()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            scanNotificationAdapter.createNotification(
                context = this,
                params =
                    NotificationParams(
                        notificationStyle = notificationStyle.value,
                        startedAt = startedAt,
                        state = ScanState.Active,
                        autostarted = serviceAutostarted,
                        reportsCreated = reportsCreated.value,
                        gpsStatus = gpsStatus.value,
                        stopIntent = stopIntent(this@ActiveScanService),
                    ),
            ),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )

        serviceScope.launch {
            settingsAwareActiveReportCreator.scanAndCreateReports(
                onReportCreated = {
                    _reportsCreated.update { it + 1 }

                    scannerQSTileAdapter.updateQuickSettingsTile()
                },
                onGpsActive = { gpsActive -> this@ActiveScanService.gpsActive.value = gpsActive },
                onScanStateChange = { state ->
                    if (state is ActiveScanner.ScanState.Active) {
                        _scanState.value = ScanState.Active
                    } else if (state is ActiveScanner.ScanState.Paused) {
                        val reasons = EnumSet.noneOf(ScanState.Paused.PauseReason::class.java)
                        if (state.notMoving) {
                            reasons.add(ScanState.Paused.PauseReason.NOT_MOVING)
                        }
                        if (state.lowBattery) {
                            reasons.add(ScanState.Paused.PauseReason.LOW_BATTERY)
                        }
                        if (state.overheating) {
                            reasons.add(ScanState.Paused.PauseReason.OVERHEAT)
                        }

                        _scanState.value = ScanState.Paused(reasons)
                    }
                },
            )
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        wakeLock.release()

        super.onDestroy()

        _gpsStatus.value = null
        _reportsCreated.value = 0
        _scanState.value = ScanState.Stopped

        notificationManager.cancel(NOTIFICATION_ID)
        scannerQSTileAdapter.updateQuickSettingsTile()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
