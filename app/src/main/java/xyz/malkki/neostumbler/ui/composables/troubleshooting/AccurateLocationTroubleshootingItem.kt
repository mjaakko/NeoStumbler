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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import xyz.malkki.neostumbler.R

@Composable
fun AccurateLocationTroubleshootingItem() {
    val context = LocalContext.current

    val locationManager = context.getSystemService<LocationManager>()!!

    val accurateLocationAvailableState = callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    trySendBlocking(
                        locationManager.isLocationEnabled &&
                            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    )
                }
            }

        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(LocationManager.MODE_CHANGED_ACTION)
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            },
        )

        send(
            locationManager.isLocationEnabled &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        )

        awaitClose { context.unregisterReceiver(receiver) }
    }
    val accurateLocationAvailableLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_accurate_location_available),
        stateFlow = accurateLocationAvailableState,
        fixAction = {
            accurateLocationAvailableLauncher.launch(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
        },
    )
}
