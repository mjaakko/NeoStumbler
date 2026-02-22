package xyz.malkki.neostumbler.di

import android.content.Context
import org.koin.dsl.module
import xyz.malkki.neostumbler.data.airpressure.AirPressureSource
import xyz.malkki.neostumbler.data.airpressure.PressureSensorAirPressureSource
import xyz.malkki.neostumbler.data.emitter.ActiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.ActiveCellInfoSource
import xyz.malkki.neostumbler.data.emitter.ActiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.emitter.BLEScannerBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.MultiSubscriptionActiveCellInfoSource
import xyz.malkki.neostumbler.data.emitter.WifiManagerActiveWifiAccessPointSource

val scanDataSources = module {
    single<ActiveCellInfoSource> { MultiSubscriptionActiveCellInfoSource(get()) }

    single<ActiveWifiAccessPointSource> { WifiManagerActiveWifiAccessPointSource(get()) }

    single<ActiveBluetoothBeaconSource> { BLEScannerBluetoothBeaconSource(get()) }

    single<AirPressureSource> { PressureSensorAirPressureSource(get<Context>()) }
}
