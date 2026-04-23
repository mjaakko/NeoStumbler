package xyz.malkki.neostumbler.scanner.postprocess

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getStringSetFlow
import xyz.malkki.neostumbler.report.postprocessor.HiddenWifiFilterer
import xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessorProvider
import xyz.malkki.neostumbler.report.postprocessor.SsidBasedWifiFilterer

val postProcessorsModule = module {
    factory {
        val settings: Settings = get()

        val wifiFilterList = runBlocking {
            settings.getStringSetFlow(PreferenceKeys.WIFI_FILTER_LIST, emptySet()).first()
        }

        SsidBasedWifiFilterer(wifiFilterList)
    } bind xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor::class

    factory { HiddenWifiFilterer() } bind
        xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor::class

    single<ReportPostProcessorProvider> { SettingsAwareReportPostProcessorProvider(get()) }
}
