package xyz.malkki.neostumbler.scanner.postprocess

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getStringSetFlow

val postProcessorsModule = module {
    factory {
        val settings: Settings = get()

        val wifiFilterList = runBlocking {
            settings.getStringSetFlow(PreferenceKeys.WIFI_FILTER_LIST, emptySet()).first()
        }

        SsidBasedWifiFilterer(wifiFilterList)
    } bind ReportPostProcessor::class

    factory { HiddenWifiFilterer() } bind ReportPostProcessor::class
}
