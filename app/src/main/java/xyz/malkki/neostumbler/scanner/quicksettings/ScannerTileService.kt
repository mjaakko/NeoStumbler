package xyz.malkki.neostumbler.scanner.quicksettings

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import timber.log.Timber
import xyz.malkki.neostumbler.scanner.ScannerService

class ScannerTileService : TileService() {
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
            //TODO: we should check necessary permissions here (and possibly set tile to unavailable state / open an activity to request permissions)
            startForegroundService(ScannerService.startIntent(this))
        } else {
            startService(ScannerService.stopIntent(this))
        }
    }
}