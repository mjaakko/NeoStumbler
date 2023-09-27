package xyz.malkki.wifiscannerformls.scanner.autoscan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.malkki.wifiscannerformls.WifiScannerApplication

/**
 * Broadcast received used for rescheduling actions (e.g. activity transition requests) when the app is updated or the device is restarted
 */
class RescheduleReceiver : BroadcastReceiver() {
    companion object {
        private val ALLOWED_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ALLOWED_ACTIONS) {
            val appContext = context.applicationContext as WifiScannerApplication

            runBlocking {
                val autoWifiScanEnabled = appContext.settingsStore.data
                    .map {
                        //TODO: use constant for preference key
                        it[booleanPreferencesKey("autoscan_enabled")]
                    }
                    .firstOrNull()

                Timber.d("Received event: ${intent.action}, auto scan enabled: $autoWifiScanEnabled")

                if (autoWifiScanEnabled == true) {
                    Timber.i("Re-enabling activity transition receiver")

                    ActivityTransitionReceiver.enable(appContext)
                }
            }
        } else {
            Timber.w("Received intent with unexpected action: %s", intent.action)
        }
    }
}