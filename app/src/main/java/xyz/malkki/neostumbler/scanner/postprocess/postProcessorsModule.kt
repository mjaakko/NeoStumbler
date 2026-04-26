package xyz.malkki.neostumbler.scanner.postprocess

import kotlinx.coroutines.flow.first
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.data.settings.getStringSetFlow
import xyz.malkki.neostumbler.report.postprocessor.AutoDetectingMovingWifiBluetoothFilterer
import xyz.malkki.neostumbler.report.postprocessor.HiddenWifiFilterer
import xyz.malkki.neostumbler.report.postprocessor.SsidBasedWifiFilterer

val postProcessorsModule = module {
    factory {
        val settings: Settings = get()

        SsidBasedWifiFilterer({
            settings.getStringSetFlow(PreferenceKeys.WIFI_FILTER_LIST, emptySet()).first()
        })
    } bind xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor::class

    factory {
        val settings: Settings = get()

        AutoDetectingMovingWifiBluetoothFilterer(
            enabled = {
                settings.getBooleanFlow(PreferenceKeys.FILTER_MOVING_DEVICES, true).first()
            }
        )
    }

    factory { HiddenWifiFilterer() } bind
        xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor::class
}
