package xyz.malkki.neostumbler.scanner.quicksettings

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.PermissionHelper

class ScannerTileService : TileService() {
    companion object {
        private const val MAIN_ACTIVITY_REQUEST_CODE = 5436;

        const val ADD_QS_TILE_ACTION_NAME = "add_scanner_qs_tile"
    }

    private lateinit var oneTimeActionHelper: OneTimeActionHelper

    private var scannerService: ScannerService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(service: ComponentName, binder: IBinder) {
            scannerService = (binder as ScannerService.ScannerServiceBinder).getService()

            updateQsTile()
        }

        override fun onServiceDisconnected(service: ComponentName) {
            scannerService = null

            updateQsTile()
        }
    }

    private val scanningActive: Boolean
        get() = scannerService != null

    private fun updateQsTile() {
        Timber.d("Setting QS tile to %s", if (scanningActive) { "active" } else { "inactive" })
        
        qsTile
            .apply {
                state = if (scanningActive) {
                    Tile.STATE_ACTIVE
                } else {
                    Tile.STATE_INACTIVE
                }
            }
            .updateTile()
    }

    override fun onCreate() {
        oneTimeActionHelper = OneTimeActionHelper(application as StumblerApplication)
    }

    override fun onTileAdded() {
        runBlocking {
            //Showing "add QS tile" prompt is unnecessary if the user has already added the QS tile
            oneTimeActionHelper.markActionShown(ADD_QS_TILE_ACTION_NAME)
        }
    }

    override fun onStartListening() {
        bindService(Intent(this, ScannerService::class.java), serviceConnection, 0)

        updateQsTile()
    }

    override fun onStopListening() {
        unbindService(serviceConnection)

        scannerService = null
        updateQsTile()
    }

    override fun onClick() {
        if (!scanningActive) {
            if (PermissionHelper.hasScanPermissions(this)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION).isEmpty())) {
                //If we already have required permissions, start scanning
                startForegroundService(ScannerService.startIntent(this))
            } else {
                //Otherwise open main activity to request permissions before starting scanning
                startMainActivity(requestBackgroundPermission = backgroundLocationPermissionNeeded())
            }
        } else {
            startService(ScannerService.stopIntent(this))
        }
    }

    private fun backgroundLocationPermissionNeeded(): Boolean {
        //As of Android 14, background location permission is needed to start foreground services using location from quick settings tiles
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION).isNotEmpty()
    }

    private fun startMainActivity(requestBackgroundPermission: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(MainActivity.EXTRA_START_SCANNING, true)
            putExtra(MainActivity.EXTRA_REQUEST_BACKGROUND_PERMISSION, requestBackgroundPermission)
        }
        val intentWrapper = PendingIntentActivityWrapper(this, MAIN_ACTIVITY_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)

        TileServiceCompat.startActivityAndCollapse(this, intentWrapper)
    }
}