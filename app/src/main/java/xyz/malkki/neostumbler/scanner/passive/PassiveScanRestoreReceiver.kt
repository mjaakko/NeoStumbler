package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.checkMissingPermissions

/** Restores passive scanning after reboot or app upgrade */
class PassiveScanRestoreReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private val ALLOWED_ACTIONS =
            setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    private val settings: Settings by inject()

    private val passiveScanManager: PassiveScanManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ALLOWED_ACTIONS) {
            val passiveScanEnabled = runBlocking {
                settings.getBooleanFlow(PreferenceKeys.PASSIVE_SCAN_ENABLED, false).first()
            }

            if (passiveScanEnabled) {
                if (
                    context
                        .checkMissingPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        )
                        .isEmpty()
                ) {
                    @SuppressLint("MissingPermission")
                    runBlocking { passiveScanManager.enablePassiveScanning() }
                }

                // TODO: disable passive scanning if permissions have been revoked?
            }
        }
    }
}
