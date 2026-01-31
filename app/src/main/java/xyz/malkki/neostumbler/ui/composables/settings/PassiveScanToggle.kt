package xyz.malkki.neostumbler.ui.composables.settings

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.scanner.passive.PassiveScanManager
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction
import xyz.malkki.neostumbler.ui.composables.shared.ConfirmationDialog
import xyz.malkki.neostumbler.ui.composables.shared.PermissionsDialog

private val passiveScanPermissions =
    buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.READ_PHONE_STATE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        .toTypedArray()

@Composable
fun PassiveScanToggle(
    settings: Settings = koinInject(),
    passiveScanManager: PassiveScanManager = koinInject(),
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showConfirmationDialog = rememberSaveable { mutableStateOf(false) }

    val enabled =
        settings
            .getBooleanFlow(PreferenceKeys.PASSIVE_SCAN_ENABLED, false)
            .collectAsState(initial = false)

    var missingPermissionsBasic by remember {
        mutableStateOf(context.checkMissingPermissions(*passiveScanPermissions))
    }
    // Background location permission has to be requested separately
    var missingPermissionsAdditional by remember {
        mutableStateOf(
            context.checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        )
    }

    var showBasicPermissionsDialog by rememberSaveable { mutableStateOf(false) }
    var showAdditionalPermissionsDialog by rememberSaveable { mutableStateOf(false) }

    fun enablePassiveScan() {
        if (
            missingPermissionsBasic.isNotMissingNecessaryPermissions() &&
                missingPermissionsAdditional.isEmpty()
        ) {
            coroutineScope.launch {
                @SuppressLint("MissingPermission") passiveScanManager.enablePassiveScanning()

                settings.edit { setBoolean(PreferenceKeys.PASSIVE_SCAN_ENABLED, true) }
            }
        } else if (missingPermissionsBasic.isNotEmpty()) {
            showBasicPermissionsDialog = true
        } else {
            showAdditionalPermissionsDialog = true
        }
    }

    if (showBasicPermissionsDialog) {
        PassiveScanBasicPermissionsDialog(
            missingPermissions = missingPermissionsBasic,
            onPermissionsGranted = { permissions ->
                showBasicPermissionsDialog = false

                missingPermissionsBasic =
                    missingPermissionsBasic.filter {
                        it !in permissions || permissions[it] == false
                    }

                if (missingPermissionsBasic.isNotMissingNecessaryPermissions()) {
                    enablePassiveScan()
                } else {
                    context.showToast(
                        ContextCompat.getString(context, R.string.permissions_not_granted)
                    )
                }
            },
        )
    }

    if (showAdditionalPermissionsDialog) {
        PassiveScanAdditionalPermissionsDialog(
            missingPermissions = missingPermissionsAdditional,
            onPermissionsGranted = { permissions ->
                showAdditionalPermissionsDialog = false

                missingPermissionsAdditional =
                    missingPermissionsAdditional.filter {
                        it !in permissions || permissions[it] == false
                    }

                if (missingPermissionsAdditional.isEmpty()) {
                    enablePassiveScan()
                } else {
                    context.showToast(
                        ContextCompat.getString(context, R.string.permissions_not_granted)
                    )
                }
            },
        )
    }

    if (showConfirmationDialog.value) {
        PassiveScanConfirmationDialog(
            onPositiveAction = {
                enablePassiveScan()

                showConfirmationDialog.value = false
            },
            onNegativeAction = { showConfirmationDialog.value = false },
        )
    }

    ToggleWithAction(
        title = stringResource(id = R.string.passive_scanning_title),
        enabled = true,
        checked = enabled.value,
        action = { enabled ->
            if (enabled) {
                showConfirmationDialog.value = true
            } else {
                passiveScanManager.disablePassiveScanning()

                settings.edit { setBoolean(PreferenceKeys.PASSIVE_SCAN_ENABLED, false) }
            }
        },
    )
}

private fun List<String>.isNotMissingNecessaryPermissions(): Boolean {
    return Manifest.permission.ACCESS_FINE_LOCATION !in this &&
        Manifest.permission.READ_PHONE_STATE !in this
}

@Composable
private fun PassiveScanAdditionalPermissionsDialog(
    missingPermissions: List<String>,
    onPermissionsGranted: (Map<String, Boolean>) -> Unit,
) {
    PermissionsDialog(
        missingPermissions = missingPermissions,
        permissionRationales =
            mapOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION to
                    stringResource(
                        id = R.string.permission_rationale_background_location_passive_scan
                    )
            ),
        onPermissionsGranted = onPermissionsGranted,
    )
}

@Composable
private fun PassiveScanBasicPermissionsDialog(
    missingPermissions: List<String>,
    onPermissionsGranted: (Map<String, Boolean>) -> Unit,
) {
    PermissionsDialog(
        missingPermissions = missingPermissions,
        permissionRationales =
            buildMap {
                put(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    stringResource(id = R.string.permission_rationale_fine_location),
                )
                put(
                    Manifest.permission.READ_PHONE_STATE,
                    stringResource(id = R.string.permission_rationale_read_phone_state),
                )

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

@Composable
private fun PassiveScanConfirmationDialog(
    onPositiveAction: () -> Unit,
    onNegativeAction: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.passive_scanning_title),
        description = stringResource(id = R.string.passive_scanning_description),
        positiveButtonText = stringResource(id = R.string.yes),
        negativeButtonText = stringResource(id = R.string.no),
        onPositiveAction = onPositiveAction,
        onNegativeAction = onNegativeAction,
    )
}
