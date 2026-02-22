package xyz.malkki.neostumbler.scanner.quicksettings

import android.content.Context
import xyz.malkki.neostumbler.activescan.adapter.ScannerQSTileAdapter

class ScannerQSTileUpdater(context: Context) : ScannerQSTileAdapter {
    private val appContext: Context = context.applicationContext

    override fun updateQuickSettingsTile() {
        ScannerTileService.updateTile(appContext)
    }
}
