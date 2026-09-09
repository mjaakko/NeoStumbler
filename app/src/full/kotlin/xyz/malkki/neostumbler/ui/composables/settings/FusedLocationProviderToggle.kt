package xyz.malkki.neostumbler.ui.composables.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.location.GpsStatusSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.scanner.passive.PassiveScanManager
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction

private fun Settings.fusedProviderEnabled(): Flow<Boolean> =
    getSnapshotFlow()
        .map { prefs ->
            prefs.getBoolean(PreferenceKeys.PREFER_FUSED_LOCATION) != false
        }
        .distinctUntilChanged()

@Composable
fun FusedLocationProviderToggle(
    settings: Settings = koinInject(),
    passiveScanManager: PassiveScanManager = koinInject(),
    gpsStatusSource: GpsStatusSource = koinInject(),
) {
    val context = LocalContext.current

    val preferFusedLocationProvider by
        settings.fusedProviderEnabled().collectAsStateWithLifecycle(initialValue = true)

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

            val passiveScanEnabled =
                settings.getBooleanFlow(PreferenceKeys.PASSIVE_SCAN_ENABLED, false).first()

            if (passiveScanEnabled) {
                val hasPermission =
                    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                // We need to re-enable passive scanning here to use the correct location provider
                // TODO: this feels like a wrong place to handle this
                if (hasPermission) {
                    passiveScanManager.enablePassiveScanning()
                }
            }
        },
    )
}
