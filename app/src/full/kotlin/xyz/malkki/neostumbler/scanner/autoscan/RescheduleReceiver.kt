package xyz.malkki.neostumbler.scanner.autoscan

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.utils.PermissionHelper

/**
 * Broadcast received used for rescheduling actions (e.g. activity transition requests) when the app
 * is updated or the device is restarted
 */
class RescheduleReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private val ALLOWED_ACTIONS =
            setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    private val settings: Settings by inject()

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ALLOWED_ACTIONS) {
            val appContext = context.applicationContext as StumblerApplication

            val autoScanEnabled = runBlocking {
                settings.getBooleanFlow(PreferenceKeys.AUTOSCAN_ENABLED, false).first()
            }
            val autoScanPermissionsGranted = PermissionHelper.hasAutoScanPermissions(appContext)

            Timber.d(
                "Received event: ${intent.action}, auto scan enabled: $autoScanEnabled, permissions granted: $autoScanPermissionsGranted"
            )

            if (autoScanEnabled && autoScanPermissionsGranted) {
                Timber.i("Re-enabling activity transition receiver")

                ActivityTransitionReceiver.enableWithTask(appContext).addOnCompleteListener { task
                    ->
                    Timber.i("Activity transition receiver enabled: ${task.isSuccessful}")
                }
            }
        } else {
            Timber.w("Received intent with unexpected action: %s", intent.action)
        }
    }
}
