package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.activescan.ActiveScanPreferenceKeys
import xyz.malkki.neostumbler.activescan.adapter.NotificationStyle
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.setEnum

private val TITLES =
    mapOf(
        NotificationStyle.MINIMAL to R.string.notification_style_minimal_title,
        NotificationStyle.BASIC to R.string.notification_style_basic_title,
        NotificationStyle.DETAILED to R.string.notification_style_detailed_title,
    )

private val DESCRIPTIONS =
    mapOf(
        NotificationStyle.MINIMAL to R.string.notification_style_minimal_description,
        NotificationStyle.BASIC to R.string.notification_style_basic_description,
        NotificationStyle.DETAILED to R.string.notification_style_detailed_description,
    )

@Composable
fun ScannerNotificationStyleSettings(settings: Settings = koinInject()) {
    val notificationStyle by
        settings
            .getEnumFlow(
                ActiveScanPreferenceKeys.SCANNER_NOTIFICATION_STYLE,
                NotificationStyle.BASIC,
            )
            .collectAsStateWithLifecycle(initialValue = null)

    notificationStyle?.let {
        MultiChoiceSettings(
            title = stringResource(id = R.string.notification_style),
            options = NotificationStyle.entries,
            selectedOption = it,
            titleProvider = { option ->
                TITLES[option]?.let { resourceId -> stringResource(resourceId) }.orEmpty()
            },
            descriptionProvider = { option ->
                DESCRIPTIONS[option]?.let { resourceId -> stringResource(resourceId) }
            },
            onValueSelected = { newNotificationStyle ->
                settings.edit {
                    setEnum(
                        ActiveScanPreferenceKeys.SCANNER_NOTIFICATION_STYLE,
                        newNotificationStyle,
                    )
                }
            },
        )
    }
}
