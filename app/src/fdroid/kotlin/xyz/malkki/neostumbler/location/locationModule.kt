package xyz.malkki.neostumbler.location

import org.koin.core.module.Module
import org.koin.dsl.module

val locationModule: Module = module { factory<LocationSource> { PlatformLocationSource(get()) } }
