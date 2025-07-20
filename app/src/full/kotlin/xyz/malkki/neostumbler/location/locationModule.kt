package xyz.malkki.neostumbler.location

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.location.FusedLocationSource
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.data.location.PlatformLocationSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.isGoogleApisAvailable

private fun Settings.preferFusedLocation(): Boolean = runBlocking {
    getBooleanFlow(PreferenceKeys.PREFER_FUSED_LOCATION, true).first()
}

val locationModule: Module = module {
    factory<LocationSource> {
        val context: Context = get()
        val settings: Settings = get()

        if (settings.preferFusedLocation() && context.isGoogleApisAvailable()) {
            Timber.i("Using fused location source")
            FusedLocationSource(context)
        } else {
            Timber.i("Using platform location source")
            PlatformLocationSource(context)
        }
    }
}
