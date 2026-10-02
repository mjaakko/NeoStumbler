package xyz.malkki.neostumbler.ui.composables.settings.scanning

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.EnumSet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.activescan.ActiveScanPreferenceKeys
import xyz.malkki.neostumbler.data.emitter.ActiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.setEnum
import xyz.malkki.neostumbler.ui.composables.settings.MultiChoiceSettings
import xyz.malkki.neostumbler.ui.composables.shared.PermissionsDialog

private val DISABLED_MODES_WHEN_NOT_SUPPORTED: Set<ActiveWifiAccessPointSource.RangingMode> =
    EnumSet.complementOf(EnumSet.of(ActiveWifiAccessPointSource.RangingMode.NEVER))

private fun Context.hasWifiRttPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) ==
            PackageManager.PERMISSION_GRANTED
}

@Composable
fun WifiRttRangingSettings(settings: Settings = koinInject()) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val packageManager: PackageManager = context.packageManager

    val disabledOptions =
        remember(packageManager) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
                emptySet()
            } else {
                DISABLED_MODES_WHEN_NOT_SUPPORTED
            }
        }

    val wifiRttRangingMode by
        settings
            .getEnumFlow(
                ActiveScanPreferenceKeys.WIFI_RTT_RANGING_MODE,
                ActiveWifiAccessPointSource.RangingMode.NEVER,
            )
            .collectAsStateWithLifecycle(initialValue = null)

    var pendingMode by remember { mutableStateOf<ActiveWifiAccessPointSource.RangingMode?>(null) }

    if (pendingMode != null) {
        @SuppressLint("InlinedApi")
        PermissionsDialog(
            missingPermissions = listOf(Manifest.permission.NEARBY_WIFI_DEVICES),
            permissionRationales =
                mapOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES to
                        stringResource(R.string.permission_rationale_wifi_rtt)
                ),
            onPermissionsGranted = { permissions ->
                if (permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true) {
                    pendingMode?.let { newWifiRttRangingMode ->
                        coroutineScope.launch {
                            settings.edit {
                                setEnum(
                                    ActiveScanPreferenceKeys.WIFI_RTT_RANGING_MODE,
                                    newWifiRttRangingMode,
                                )
                            }
                        }
                    }

                    pendingMode = null
                }
            },
        )
    }

    wifiRttRangingMode?.let {
        MultiChoiceSettings(
            title = stringResource(R.string.wifi_rtt_ranging),
            options = ActiveWifiAccessPointSource.RangingMode.entries,
            selectedOption = it,
            disabledOptions = disabledOptions,
            titleProvider = { option ->
                when (option) {
                    ActiveWifiAccessPointSource.RangingMode.NEVER ->
                        stringResource(R.string.wifi_rtt_ranging_mode_never_title)
                    ActiveWifiAccessPointSource.RangingMode.ALWAYS ->
                        stringResource(R.string.wifi_rtt_ranging_mode_always_title)
                    ActiveWifiAccessPointSource.RangingMode.TWOSIDED ->
                        stringResource(R.string.wifi_rtt_ranging_mode_twosided_title)
                }
            },
            descriptionProvider = { option ->
                when (option) {
                    ActiveWifiAccessPointSource.RangingMode.ALWAYS ->
                        stringResource(R.string.wifi_rtt_ranging_mode_always_description)
                    ActiveWifiAccessPointSource.RangingMode.TWOSIDED ->
                        stringResource(R.string.wifi_rtt_ranging_mode_twosided_description)
                    else -> null
                }
            },
            onValueSelected = { newWifiRttRangingMode ->
                if (
                    newWifiRttRangingMode == ActiveWifiAccessPointSource.RangingMode.NEVER ||
                        context.hasWifiRttPermission()
                ) {
                    settings.edit {
                        setEnum(
                            ActiveScanPreferenceKeys.WIFI_RTT_RANGING_MODE,
                            newWifiRttRangingMode,
                        )
                    }
                } else {
                    pendingMode = newWifiRttRangingMode
                }
            },
        )
    }
}
