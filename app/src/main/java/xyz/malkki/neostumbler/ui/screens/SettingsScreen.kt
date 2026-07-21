package xyz.malkki.neostumbler.ui.screens

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.AboutNeoStumbler
import xyz.malkki.neostumbler.ui.composables.ReportReuploadButton
import xyz.malkki.neostumbler.ui.composables.settings.AutoUploadToggle
import xyz.malkki.neostumbler.ui.composables.settings.CoverageLayerSettings
import xyz.malkki.neostumbler.ui.composables.settings.CrashLogSettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.DbPruneSettings
import xyz.malkki.neostumbler.ui.composables.settings.GeocoderSettings
import xyz.malkki.neostumbler.ui.composables.settings.LanguageSwitcher
import xyz.malkki.neostumbler.ui.composables.settings.ManageStorageSettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.ScannerNotificationStyleSettings
import xyz.malkki.neostumbler.ui.composables.settings.SettingsGroup
import xyz.malkki.neostumbler.ui.composables.settings.SettingsToggle
import xyz.malkki.neostumbler.ui.composables.settings.geosubmit.GeosubmitEndpointSettings
import xyz.malkki.neostumbler.ui.composables.settings.privacy.RestrictedAreaSettings
import xyz.malkki.neostumbler.ui.composables.settings.privacy.WifiFilterSettings
import xyz.malkki.neostumbler.ui.composables.settings.scanning.ScanningSettings
import xyz.malkki.neostumbler.ui.composables.troubleshooting.TroubleshootingSettingsItem
import xyz.malkki.neostumbler.ui.modifiers.handleDisplayCutouts

@Composable
private fun ReportSettings() {
    SettingsGroup(title = stringResource(id = R.string.settings_group_reports)) {
        GeosubmitEndpointSettings()
        CoverageLayerSettings()
        AutoUploadToggle()
        DbPruneSettings()
    }
}

@Composable
private fun PrivacySettings(openRestrictedAreas: () -> Unit) {
    SettingsGroup(title = stringResource(id = R.string.settings_group_privacy)) {
        WifiFilterSettings()

        SettingsToggle(
            title = stringResource(id = R.string.reduced_metadata_title),
            description = stringResource(id = R.string.reduced_metadata_description),
            preferenceKey = PreferenceKeys.REDUCED_METADATA,
            default = false,
        )

        RestrictedAreaSettings(openRestrictedAreas = openRestrictedAreas)
    }
}

@Composable
private fun OtherSettings() {
    SettingsGroup(title = stringResource(id = R.string.settings_group_other)) {
        ScannerNotificationStyleSettings()
        LanguageSwitcher()
        GeocoderSettings()

        // Dynamic color is available on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsToggle(
                title = stringResource(id = R.string.use_dynamic_color_ui),
                preferenceKey = PreferenceKeys.DYNAMIC_COLOR_THEME,
                default = false,
            )
        }

        TroubleshootingSettingsItem()
        ManageStorageSettingsItem()

        CrashLogSettingsItem()
    }
}

private enum class SettingsGroupType(@StringRes val title: Int) {
    REPORTS(R.string.settings_group_reports),
    SCANNING(R.string.settings_group_scanning),
    PRIVACY(R.string.settings_group_privacy),
    OTHER(R.string.settings_group_other),
}

@Composable
fun SettingsScreen(openRestrictedAreas: () -> Unit) {
    BoxWithConstraints {
        if (maxWidth >= 600.dp) {
            var selectedSettingsGroup by rememberSaveable {
                mutableStateOf(SettingsGroupType.REPORTS)
            }

            Row(
                modifier =
                    Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        .windowInsetsPadding(WindowInsets.safeContent),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.width(250.dp).fillMaxHeight()) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .selectableGroup()
                    ) {
                        SettingsGroupType.entries.forEach {
                            Box(
                                modifier =
                                    Modifier.height(48.dp)
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedSettingsGroup == it,
                                            onClick = { selectedSettingsGroup = it },
                                        )
                                        .background(
                                            color =
                                                if (selectedSettingsGroup == it) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                        )
                                        .padding(8.dp)
                            ) {
                                Text(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    text = stringResource(it.title),
                                )
                            }
                        }
                    }

                    ReportReuploadButton()

                    Spacer(modifier = Modifier.height(8.dp))

                    AboutNeoStumbler()
                }

                VerticalDivider(modifier = Modifier.fillMaxHeight())

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (selectedSettingsGroup) {
                        SettingsGroupType.REPORTS -> ReportSettings()
                        SettingsGroupType.SCANNING -> ScanningSettings()
                        SettingsGroupType.PRIVACY ->
                            PrivacySettings(openRestrictedAreas = openRestrictedAreas)
                        SettingsGroupType.OTHER -> OtherSettings()
                    }
                }
            }
        } else {
            Column(
                modifier =
                    Modifier.padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .handleDisplayCutouts()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportSettings()

                    ScanningSettings()

                    PrivacySettings(openRestrictedAreas = openRestrictedAreas)

                    OtherSettings()
                }

                Spacer(modifier = Modifier.height(8.dp))

                ReportReuploadButton()

                Spacer(modifier = Modifier.height(8.dp))

                AboutNeoStumbler()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
