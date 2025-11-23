package xyz.malkki.neostumbler.ui.composables.reports

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.getActivity
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerTileService
import xyz.malkki.neostumbler.ui.composables.BatteryOptimizationsDialog
import xyz.malkki.neostumbler.ui.composables.shared.AddQSTileDialog
import xyz.malkki.neostumbler.ui.composables.shared.PermissionsDialog
import xyz.malkki.neostumbler.utils.OneTimeActionHelper

private val requiredPermissions =
    buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.READ_PHONE_STATE)
        }
        .toTypedArray()

@Composable
fun ForegroundScanningButton() {
    val context = LocalContext.current
    val intent = context.getActivity()?.intent

    val showBatteryOptimizationsDialog = rememberSaveable { mutableStateOf(false) }

    val showPermissionDialog = rememberSaveable { mutableStateOf(false) }

    val showBackgroundLocationPermissionDialog = rememberSaveable { mutableStateOf(false) }

    val missingPermissions = rememberSaveable {
        context.checkMissingPermissions(*requiredPermissions)
    }

    fun requestBackgroundLocationPermissionOrDisableBatteryOptimizations() {
        val backgroundLocationPermissionNeeded =
            intent?.getBooleanExtra(MainActivity.EXTRA_REQUEST_BACKGROUND_PERMISSION, false) ==
                true &&
                context
                    .checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    .isNotEmpty()

        if (backgroundLocationPermissionNeeded) {
            showBackgroundLocationPermissionDialog.value = true
        } else {
            showBatteryOptimizationsDialog.value = true
        }
    }

    fun stopScanning() {
        context.startService(ScannerService.stopIntent(context))
    }

    fun startScanning() {
        if (Manifest.permission.ACCESS_FINE_LOCATION !in missingPermissions) {
            requestBackgroundLocationPermissionOrDisableBatteryOptimizations()
        } else {
            showPermissionDialog.value = true
        }
    }

    if (showPermissionDialog.value) {
        ScanningPermissionsDialog(
            missingPermissions = missingPermissions,
            onPermissionsGranted = { permissions ->
                showPermissionDialog.value = false

                if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                    requestBackgroundLocationPermissionOrDisableBatteryOptimizations()
                } else {
                    context.showToast(
                        ContextCompat.getString(context, R.string.permissions_not_granted)
                    )
                }
            },
        )
    }

    if (showBackgroundLocationPermissionDialog.value) {
        PermissionsDialog(
            missingPermissions = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            permissionRationales =
                mapOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION to
                        stringResource(
                            id = R.string.permission_rationale_background_location_quick_settings
                        )
                ),
            onPermissionsGranted = {
                showBackgroundLocationPermissionDialog.value = false

                showBatteryOptimizationsDialog.value = true
            },
        )
    }

    if (showBatteryOptimizationsDialog.value) {
        BatteryOptimizationsDialog(
            onBatteryOptimizationsDisabled = {
                showBatteryOptimizationsDialog.value = false

                context.startForegroundService(ScannerService.startIntent(context))
            }
        )
    }

    LaunchedEffect(intent) {
        val shouldStartScanning =
            intent?.getBooleanExtra(MainActivity.EXTRA_START_SCANNING, false) == true

        if (shouldStartScanning) {
            startScanning()

            // Modify the intent to avoid restarting scanning after a configuration change
            context.getActivity()?.intent?.putExtra(MainActivity.EXTRA_START_SCANNING, false)
        }
    }

    StartStopScanningButton(onStart = { startScanning() }, onStop = { stopScanning() })
}

@Composable
private fun StartStopScanningButton(
    oneTimeActionHelper: OneTimeActionHelper = koinInject<OneTimeActionHelper>(),
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showQuickSettingsDialog = rememberSaveable { mutableStateOf(false) }

    val isScanning by
        ScannerService.serviceRunning.collectAsStateWithLifecycle(initialValue = false)

    if (showQuickSettingsDialog.value) {
        @SuppressLint("NewApi")
        AddQSTileDialog(
            componentName = ComponentName(context, ScannerTileService::class.java),
            dialogText = stringResource(id = R.string.add_quick_settings_tile),
            onDialogDismissed = {
                coroutineScope.launch {
                    oneTimeActionHelper.markActionShown(ScannerTileService.ADD_QS_TILE_ACTION_NAME)
                }

                showQuickSettingsDialog.value = false
            },
        )
    }

    FilledIconButton(
        modifier = Modifier.size(48.dp),
        onClick = {
            if (isScanning) {
                onStop()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Prompt user to add the quick settings tile for scanning
                    coroutineScope.launch {
                        val qsTileDialogShown =
                            oneTimeActionHelper.hasActionBeenShown(
                                ScannerTileService.ADD_QS_TILE_ACTION_NAME
                            )

                        if (!qsTileDialogShown) {
                            showQuickSettingsDialog.value = true
                        }
                    }
                }
            } else {
                onStart()
            }
        },
    ) {
        val stringResId =
            if (isScanning) {
                R.string.stop_scanning
            } else {
                R.string.start_scanning
            }
        val icon =
            if (isScanning) {
                R.drawable.stop_24px
            } else {
                R.drawable.play_arrow_24px
            }

        Icon(
            painter = painterResource(id = icon),
            contentDescription = stringResource(stringResId),
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun ScanningPermissionsDialog(
    missingPermissions: List<String>,
    onPermissionsGranted: (Map<String, Boolean>) -> Unit,
) {
    PermissionsDialog(
        missingPermissions = missingPermissions,
        permissionRationales =
            mutableMapOf<String, String>().apply {
                put(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    stringResource(id = R.string.permission_rationale_fine_location),
                )
                put(
                    Manifest.permission.READ_PHONE_STATE,
                    stringResource(id = R.string.permission_rationale_read_phone_state),
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    put(
                        Manifest.permission.POST_NOTIFICATIONS,
                        stringResource(id = R.string.permission_rationale_post_notifications),
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    put(
                        Manifest.permission.BLUETOOTH_SCAN,
                        stringResource(id = R.string.permission_rationale_bluetooth),
                    )
                } else {
                    put(
                        Manifest.permission.BLUETOOTH,
                        stringResource(id = R.string.permission_rationale_bluetooth),
                    )
                    put(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        stringResource(id = R.string.permission_rationale_bluetooth),
                    )
                }
            },
        onPermissionsGranted = onPermissionsGranted,
    )
}
