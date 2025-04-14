package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.checkMissingPermissions

/** Restores passive scanning after reboot or app upgrade */
class PassiveScanRestoreReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private val ALLOWED_ACTIONS =
            setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    private val settingsStore: DataStore<Preferences> by inject(PREFERENCES)

    private val passiveScanManager: PassiveScanManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ALLOWED_ACTIONS) {
            val passiveScanEnabled = runBlocking {
                settingsStore.data
                    .map { it[booleanPreferencesKey(PreferenceKeys.PASSIVE_SCAN_ENABLED)] == true }
                    .first()
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
                    @SuppressLint("MissingPermission") passiveScanManager.enablePassiveScanning()
                }

                // TODO: disable passive scanning if permissions have been revoked?
            }
        }
    }
}
