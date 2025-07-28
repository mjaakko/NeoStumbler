package xyz.malkki.neostumbler.location

import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.data.location.PlatformLocationSource

val locationModule: Module = module { factory<LocationSource> { PlatformLocationSource(get()) } }
