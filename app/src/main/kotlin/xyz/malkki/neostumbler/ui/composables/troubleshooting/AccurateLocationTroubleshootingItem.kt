package xyz.malkki.neostumbler.ui.composables.troubleshooting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.R

@Composable
fun AccurateLocationTroubleshootingItem() {
    val accurateLocationAvailableLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    val accurateLocationAvailable by isAccurateLocationAvailable()

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_accurate_location_available),
        isOk = accurateLocationAvailable,
        fixAction = {
            accurateLocationAvailableLauncher.launch(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
        },
    )
}

@Composable
private fun isAccurateLocationAvailable(): State<Boolean> {
    val context = LocalContext.current

    val locationManager: LocationManager? = remember(context) { context.getSystemService() }

    return produceState(false) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    value =
                        (locationManager?.isLocationEnabled == true &&
                            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                }
            }

        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(LocationManager.MODE_CHANGED_ACTION)
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            },
        )

        value =
            (locationManager?.isLocationEnabled == true &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))

        awaitDispose { context.unregisterReceiver(receiver) }
    }
}
