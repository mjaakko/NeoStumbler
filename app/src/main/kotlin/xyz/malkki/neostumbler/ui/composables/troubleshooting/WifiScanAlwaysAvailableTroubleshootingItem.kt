package xyz.malkki.neostumbler.ui.composables.troubleshooting

import android.content.Intent
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.R

@Suppress("DEPRECATION")
@Composable
fun WifiScanAlwaysAvailableTroubleshootingItem() {
    val context = LocalContext.current

    val wifiManager: WifiManager? = remember(context) { context.getSystemService() }

    var wifiScanAlwaysAvailable by remember {
        mutableStateOf(wifiManager?.isScanAlwaysAvailable == true)
    }
    val wifiScanAlwaysAvailableLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            wifiScanAlwaysAvailable = wifiManager?.isScanAlwaysAvailable == true
        }

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_wifi_scan_always_available),
        isOk = wifiScanAlwaysAvailable,
        fixAction = {
            wifiScanAlwaysAvailableLauncher.launch(
                Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE)
            )
        },
    )
}
