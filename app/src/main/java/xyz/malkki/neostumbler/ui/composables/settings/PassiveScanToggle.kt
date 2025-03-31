package xyz.malkki.neostumbler.ui.composables.settings

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlin.collections.contains
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
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
        }
        .toTypedArray()

private fun DataStore<Preferences>.passiveScanEnabled(): Flow<Boolean?> =
    data
        .map { it[booleanPreferencesKey(PreferenceKeys.PASSIVE_SCAN_ENABLED)] }
        .distinctUntilChanged()

@Composable
fun PassiveScanToggle(
    settingsStore: DataStore<Preferences> = koinInject(PREFERENCES),
    passiveScanManager: PassiveScanManager = koinInject(),
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val showConfirmationDialog = rememberSaveable { mutableStateOf(false) }

    val enabled = settingsStore.passiveScanEnabled().collectAsState(initial = false)

    val missingPermissionsBasic = remember {
        mutableStateOf(context.checkMissingPermissions(*passiveScanPermissions))
    }
    // Background location permission has to be requested separately
    val missingPermissionsAdditional = remember {
        mutableStateOf(
            context.checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        )
    }

    val showBasicPermissionsDialog = rememberSaveable { mutableStateOf(false) }
    val showAdditionalPermissionsDialog = rememberSaveable { mutableStateOf(false) }

    fun enablePassiveScan() {
        if (
            missingPermissionsBasic.value.isEmpty() && missingPermissionsAdditional.value.isEmpty()
        ) {
            @SuppressLint("MissingPermission") passiveScanManager.enablePassiveScanning()

            coroutineScope.launch {
                settingsStore.edit {
                    it[booleanPreferencesKey(PreferenceKeys.PASSIVE_SCAN_ENABLED)] = true
                }
            }
        } else if (missingPermissionsBasic.value.isNotEmpty()) {
            showBasicPermissionsDialog.value = true
        } else if (missingPermissionsAdditional.value.isNotEmpty()) {
            showAdditionalPermissionsDialog.value = true
        }
    }

    if (showBasicPermissionsDialog.value) {
        PassiveScanBasicPermissionsDialog(
            missingPermissions = missingPermissionsBasic.value,
            onPermissionsGranted = { permissions ->
                showBasicPermissionsDialog.value = false

                missingPermissionsBasic.value =
                    missingPermissionsBasic.value.filter {
                        it !in permissions || permissions[it] == false
                    }

                if (missingPermissionsBasic.value.isEmpty()) {
                    enablePassiveScan()
                } else {
                    context.showToast(
                        ContextCompat.getString(context, R.string.permissions_not_granted)
                    )
                }
            },
        )
    }

    if (showAdditionalPermissionsDialog.value) {
        PassiveScanAdditionalPermissionsDialog(
            missingPermissions = missingPermissionsAdditional.value,
            onPermissionsGranted = { permissions ->
                showAdditionalPermissionsDialog.value = false

                missingPermissionsAdditional.value =
                    missingPermissionsAdditional.value.filter {
                        it !in permissions || permissions[it] == false
                    }

                if (missingPermissionsAdditional.value.isEmpty()) {
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
        checked = enabled.value == true,
        action = { enabled ->
            if (enabled) {
                showConfirmationDialog.value = true
            } else {
                passiveScanManager.disablePassiveScanning()

                settingsStore.edit {
                    it[booleanPreferencesKey(PreferenceKeys.PASSIVE_SCAN_ENABLED)] = false
                }
            }
        },
    )
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
