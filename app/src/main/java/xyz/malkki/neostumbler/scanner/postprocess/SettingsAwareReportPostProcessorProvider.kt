package xyz.malkki.neostumbler.scanner.postprocess

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.report.postprocessor.AutoDetectingMovingWifiBluetoothFilterer
import xyz.malkki.neostumbler.report.postprocessor.HiddenWifiFilterer
import xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor
import xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessorProvider
import xyz.malkki.neostumbler.report.postprocessor.SsidBasedWifiFilterer

class SettingsAwareReportPostProcessorProvider(private val settings: Settings) :
    ReportPostProcessorProvider {
    override suspend fun getReportPostProcessors(): Collection<ReportPostProcessor> {
        val (wifiFilterList, filterMovingDevices) =
            settings
                .getSnapshotFlow()
                .map { settingsSnapshot ->
                    settingsSnapshot.getStringSet(PreferenceKeys.WIFI_FILTER_LIST) to
                        settingsSnapshot.getBoolean(PreferenceKeys.FILTER_MOVING_DEVICES)
                }
                .first()

        return buildList {
            add(HiddenWifiFilterer())
            add(SsidBasedWifiFilterer(wifiFilterList ?: emptySet()))

            if (filterMovingDevices != false) {
                add(AutoDetectingMovingWifiBluetoothFilterer(deterministic = false))
            }
        }
    }
}
