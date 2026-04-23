package xyz.malkki.neostumbler.di

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.malkki.neostumbler.data.location.LocationSourceProvider
import xyz.malkki.neostumbler.data.location.PlatformLocationSource

val locationModule: Module = module {
    single<LocationSourceProvider> {
        val locationSource = PlatformLocationSource(get())

        LocationSourceProvider { locationSource }
    }
}
