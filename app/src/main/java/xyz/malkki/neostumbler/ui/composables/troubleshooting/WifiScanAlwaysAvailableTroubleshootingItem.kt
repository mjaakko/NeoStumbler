package xyz.malkki.neostumbler.ui.composables.troubleshooting

import android.content.Intent
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import xyz.malkki.neostumbler.R

@Suppress("DEPRECATION")
@Composable
fun WifiScanAlwaysAvailableTroubleshootingItem() {
    val context = LocalContext.current

    val wifiManager = context.getSystemService<WifiManager>()!!

    val wifiScanAlwaysAvailable = MutableStateFlow(wifiManager.isScanAlwaysAvailable)
    val wifiScanAlwaysAvailableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        wifiScanAlwaysAvailable.tryEmit(wifiManager.isScanAlwaysAvailable)
    }

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_wifi_scan_always_available),
        stateFlow = wifiScanAlwaysAvailable,
        fixAction = {
            wifiScanAlwaysAvailableLauncher.launch(Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE))
        }
    )
}