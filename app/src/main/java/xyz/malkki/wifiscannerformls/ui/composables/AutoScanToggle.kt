package xyz.malkki.wifiscannerformls.ui.composables

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.malkki.wifiscannerformls.R
import xyz.malkki.wifiscannerformls.StumblerApplication
import xyz.malkki.wifiscannerformls.extensions.checkMissingPermissions
import xyz.malkki.wifiscannerformls.extensions.getActivity
import xyz.malkki.wifiscannerformls.scanner.autoscan.ActivityTransitionReceiver

//TODO: move this somewhere else
private val PERMISSION_RATIONALES = mutableMapOf<String, String>().apply {
    put(Manifest.permission.ACCESS_FINE_LOCATION, "Scanning Wi-Fi networks needs access to exact location")
    put(Manifest.permission.ACTIVITY_RECOGNITION, "Automatically starting scanning needs access to current activity")
    put(Manifest.permission.ACCESS_BACKGROUND_LOCATION, "Automatically starting scanning needs access to background location")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        put(Manifest.permission.POST_NOTIFICATIONS, "Showing status notification needs notification permission")
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        put(Manifest.permission.BLUETOOTH_SCAN, "Scanning Bluetooth devices needs access to Bluetooth permission")
    } else {
        put(Manifest.permission.BLUETOOTH, "Scanning Bluetooth devices needs access to Bluetooth permission")
        put(Manifest.permission.BLUETOOTH_ADMIN, "Scanning Bluetooth devices needs access to Bluetooth permission")
    }
}.toMap()

private fun DataStore<Preferences>.autoScanEnabled(): Flow<Boolean?> = data.map { it[booleanPreferencesKey("autoscan_enabled")] }.distinctUntilChanged()

@Composable
fun AutoScanToggle() {
    val context = LocalContext.current

    val googleApiAvailability = GoogleApiAvailability.getInstance()

    val googleApiAvailabilityCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
    val isGoogleApiAvailable = googleApiAvailabilityCode == ConnectionResult.SUCCESS
    val isGoogleApiAvailabilityUserResolvable = googleApiAvailability.isUserResolvableError(googleApiAvailabilityCode)

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val enabled = settingsStore.autoScanEnabled().collectAsState(initial = false)

    val missingPermissionsBasic = remember {
        val neededPermissions = mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACTIVITY_RECOGNITION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }.toTypedArray()

        mutableStateOf(
            context.checkMissingPermissions(*neededPermissions)
        )
    }
    //Background location permission has to be requested separately
    val missingPermissionsAdditional = remember {
        mutableStateOf(context.checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }

    val showBasicPermissionsDialog = remember { mutableStateOf(false) }
    val showAdditionalPermissionsDialog = remember { mutableStateOf(false) }

    suspend fun enableAutoScan() {
        ActivityTransitionReceiver.enable(context)
        settingsStore.edit { it[booleanPreferencesKey("autoscan_enabled")] = true }

        Timber.i("Enabled activity transition listener")
    }

    suspend fun disableAutoScan() {
        ActivityTransitionReceiver.disable(context)
        settingsStore.edit { it[booleanPreferencesKey("autoscan_enabled")] = false }

        Timber.i("Disabled activity transition listener")
    }

    if (showBasicPermissionsDialog.value) {
        PermissionsDialog(
            missingPermissions = missingPermissionsBasic.value,
            permissionRationales = PERMISSION_RATIONALES,
            onPermissionsGranted = { permissions ->
                showBasicPermissionsDialog.value = false

                missingPermissionsBasic.value = missingPermissionsBasic.value.filter {
                    it !in permissions || permissions[it] == false
                }

                if (Manifest.permission.ACCESS_FINE_LOCATION !in missingPermissionsBasic.value
                    && Manifest.permission.ACTIVITY_RECOGNITION !in missingPermissionsBasic.value) {

                    if (missingPermissionsAdditional.value.isEmpty()) {
                        coroutineScope.launch {
                            enableAutoScan()
                        }
                    } else {
                        showAdditionalPermissionsDialog.value = true
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.permissions_not_granted), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAdditionalPermissionsDialog.value) {
        PermissionsDialog(
            missingPermissions = missingPermissionsAdditional.value,
            permissionRationales = PERMISSION_RATIONALES,
            onPermissionsGranted = { permissions ->
                showAdditionalPermissionsDialog.value = false

                if (permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true) {
                    coroutineScope.launch {
                        enableAutoScan()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.permissions_not_granted), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    ToggleWithAction(
        title = stringResource(R.string.autostumble_when_moving),
        enabled = (isGoogleApiAvailable || isGoogleApiAvailabilityUserResolvable),
        checked = enabled.value == true ,
        action = { checked ->
            //If Google APIs are not available, try to make them available before doing anything
            if (!isGoogleApiAvailable) {
                try {
                    withContext(Dispatchers.Main) {
                        googleApiAvailability.makeGooglePlayServicesAvailable(context.getActivity()!!).await()
                    }
                } catch (ex: Exception) {
                    Timber.w("Failed to make Google Play Services available, cannot enable autoscan", ex)
                    return@ToggleWithAction;
                }
            }

            if (checked) {
                if (missingPermissionsBasic.value.isEmpty() && missingPermissionsAdditional.value.isEmpty()) {
                    enableAutoScan()
                } else {
                    if (missingPermissionsBasic.value.isNotEmpty()) {
                        showBasicPermissionsDialog.value = true
                    } else {
                        showAdditionalPermissionsDialog.value = true
                    }
                }
            } else {
                disableAutoScan()
            }
        }
    )
}