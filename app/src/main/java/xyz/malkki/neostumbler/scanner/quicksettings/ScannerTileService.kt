package xyz.malkki.neostumbler.scanner.quicksettings

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.PermissionHelper

class ScannerTileService : TileService() {
    companion object {
        private const val MAIN_ACTIVITY_REQUEST_CODE = 5436

        const val ADD_QS_TILE_ACTION_NAME = "add_scanner_qs_tile"

        fun updateTile(context: Context) {
            requestListeningState(context, ComponentName(context, ScannerTileService::class.java))
        }
    }

    private lateinit var coroutineScope: CoroutineScope

    private val oneTimeActionHelper: OneTimeActionHelper by inject()

    private var updaterJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        coroutineScope = CoroutineScope(Dispatchers.Default)
    }

    override fun onDestroy() {
        coroutineScope.cancel()

        super.onDestroy()
    }

    override fun onTileAdded() {
        coroutineScope.launch {
            // Showing "add QS tile" prompt is unnecessary if the user has already added the QS tile
            oneTimeActionHelper.markActionShown(ADD_QS_TILE_ACTION_NAME)
        }
    }

    override fun onStartListening() {
        updaterJob =
            coroutineScope.launch {
                ScannerService.serviceRunning
                    .combine(ScannerService.reportsCreated) { a, b -> a to b }
                    .collect { (scanningActive, reportsCreated) ->
                        Timber.d(
                            "Updating QS tile, scanning: $scanningActive, reports: $reportsCreated"
                        )

                        qsTile
                            .apply {
                                // Label has to be updated here to support per-app locales even
                                // though it's
                                // specified in the manifest
                                label =
                                    ContextCompat.getString(
                                        this@ScannerTileService,
                                        R.string.wireless_scanning,
                                    )

                                subtitle =
                                    if (scanningActive) {
                                        applicationContext.getQuantityString(
                                            R.plurals
                                                .notification_wireless_scanning_content_reports_created,
                                            reportsCreated,
                                            reportsCreated,
                                        )
                                    } else {
                                        null
                                    }

                                state =
                                    if (scanningActive) {
                                        Tile.STATE_ACTIVE
                                    } else {
                                        Tile.STATE_INACTIVE
                                    }
                            }
                            .updateTile()
                    }
            }
    }

    override fun onStopListening() {
        updaterJob?.cancel()
    }

    override fun onClick() {
        if (qsTile.state == Tile.STATE_INACTIVE) {
            if (
                PermissionHelper.hasScanPermissions(this) &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                        checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            .isEmpty())
            ) {
                // If we already have required permissions, start scanning
                startForegroundService(ScannerService.startIntent(this))
            } else {
                // Otherwise open main activity to request permissions before starting scanning
                startMainActivity(
                    requestBackgroundPermission = backgroundLocationPermissionNeeded()
                )
            }
        } else {
            startService(ScannerService.stopIntent(this))
        }
    }

    private fun backgroundLocationPermissionNeeded(): Boolean {
        // As of Android 14, background location permission is needed to start foreground services
        // using
        // location from quick settings tiles
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION).isNotEmpty()
    }

    private fun startMainActivity(requestBackgroundPermission: Boolean = false) {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MainActivity.EXTRA_START_SCANNING, true)
                putExtra(
                    MainActivity.EXTRA_REQUEST_BACKGROUND_PERMISSION,
                    requestBackgroundPermission,
                )
            }
        val intentWrapper =
            PendingIntentActivityWrapper(
                this,
                MAIN_ACTIVITY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )

        TileServiceCompat.startActivityAndCollapse(this, intentWrapper)
    }
}
