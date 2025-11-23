package xyz.malkki.neostumbler.ui.composables.settings

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.location.GpsStatusSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.scanner.passive.PassiveScanManager
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction

private fun Settings.fusedProviderAndPassiveScanEnabled(): Flow<Pair<Boolean, Boolean>> =
    getSnapshotFlow()
        .map { prefs ->
            val fused = prefs.getBoolean(PreferenceKeys.PREFER_FUSED_LOCATION) != false
            val passive = prefs.getBoolean(PreferenceKeys.PASSIVE_SCAN_ENABLED) == true

            fused to passive
        }
        .distinctUntilChanged()

@Composable
fun FusedLocationProviderToggle(
    settings: Settings = koinInject(),
    passiveScanManager: PassiveScanManager = koinInject(),
    gpsStatusSource: GpsStatusSource = koinInject(),
) {
    val state =
        settings.fusedProviderAndPassiveScanEnabled().collectAsState(initial = true to false)
    val (preferFusedLocationProvider, passiveScanEnabled) = state.value

    val gpsAvailable by
        gpsStatusSource.isGpsAvailable().collectAsStateWithLifecycle(initialValue = false)

    ToggleWithAction(
        title = stringResource(id = R.string.prefer_fused_location_title),
        description = stringResource(id = R.string.prefer_fused_location_description),
        warningWhenDisabled = stringResource(id = R.string.fused_location_no_gps_warning),
        enabled = gpsAvailable,
        checked = preferFusedLocationProvider,
        action = { checked ->
            settings.edit { setBoolean(PreferenceKeys.PREFER_FUSED_LOCATION, checked) }

            @SuppressLint(
                "MissingPermission"
            ) // If passive scanning is enabled, we should have the required permissions
            if (passiveScanEnabled) {
                // We need to re-enable passive scanning here to use the correct location provider
                // TODO: this feels like a wrong place to handle this
                passiveScanManager.enablePassiveScanning()
            }
        },
    )
}
