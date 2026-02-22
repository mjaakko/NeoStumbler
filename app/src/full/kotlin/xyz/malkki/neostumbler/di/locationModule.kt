package xyz.malkki.neostumbler.di

import android.content.Context
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.location.FusedLocationSource
import xyz.malkki.neostumbler.data.location.LocationSourceProvider
import xyz.malkki.neostumbler.data.location.PlatformLocationSource
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.isGoogleApisAvailable

private suspend fun Settings.preferFusedLocation(): Boolean =
    getBooleanFlow(PreferenceKeys.PREFER_FUSED_LOCATION, true).first()

val locationModule: Module = module {
    single<LocationSourceProvider> {
        val context: Context = get()
        val settings: Settings = get()

        val fusedLocationSource = FusedLocationSource(context)
        val platformLocationSource = PlatformLocationSource(context)

        LocationSourceProvider {
            if (settings.preferFusedLocation() && context.isGoogleApisAvailable()) {
                Timber.i("Using fused location source")
                fusedLocationSource
            } else {
                Timber.i("Using platform location source")
                platformLocationSource
            }
        }
    }
}
