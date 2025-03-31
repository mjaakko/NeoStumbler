package xyz.malkki.neostumbler.scanner.postprocess

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.getOrDefault

val postProcessorsModule = module {
    factory {
        val settingsStore: DataStore<Preferences> = get(PREFERENCES)

        val wifiFilterList = runBlocking {
            settingsStore.getOrDefault(
                stringSetPreferencesKey(PreferenceKeys.WIFI_FILTER_LIST),
                emptySet(),
            )
        }

        SsidBasedWifiFilterer(wifiFilterList)
    } bind ReportPostProcessor::class

    factory { HiddenWifiFilterer() } bind ReportPostProcessor::class
}
