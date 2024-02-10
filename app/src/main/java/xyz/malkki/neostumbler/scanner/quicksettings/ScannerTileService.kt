package xyz.malkki.neostumbler.scanner.quicksettings

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.StumblerApplication
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
            if (PermissionHelper.hasScanPermissions(this)) {
                //If we already have required permissions, start scanning
                startForegroundService(ScannerService.startIntent(this))
            } else {
                //Otherwise open main activity where user can start scanning manually
                startMainActivity()
            }
        } else {
            startService(ScannerService.stopIntent(this))
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(this, MAIN_ACTIVITY_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}