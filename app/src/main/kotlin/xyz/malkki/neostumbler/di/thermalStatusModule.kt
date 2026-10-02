package xyz.malkki.neostumbler.di

import android.content.Context
import org.koin.dsl.module
import xyz.malkki.neostumbler.data.thermal.PowerManagerThermalStatusProvider
import xyz.malkki.neostumbler.data.thermal.ThermalStatusProvider

val thermalStatusModule = module {
    single<ThermalStatusProvider> { PowerManagerThermalStatusProvider(get<Context>()) }
}
