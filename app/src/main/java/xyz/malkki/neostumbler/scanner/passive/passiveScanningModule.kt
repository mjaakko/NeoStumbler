package xyz.malkki.neostumbler.scanner.passive

import org.koin.dsl.module
import xyz.malkki.neostumbler.PASSIVE_SCAN_STATE
import xyz.malkki.neostumbler.data.emitter.BLEScannerPassiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.MultiSubscriptionPassiveCellInfoSource
import xyz.malkki.neostumbler.data.emitter.PassiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.PassiveCellTowerSource
import xyz.malkki.neostumbler.data.emitter.PassiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.emitter.WifiManagerPassiveWifiAccessPointSource

val passiveScanningModule = module {
    single<PassiveCellTowerSource> { MultiSubscriptionPassiveCellInfoSource(get()) }

    single<PassiveWifiAccessPointSource> { WifiManagerPassiveWifiAccessPointSource(get()) }

    single<PassiveBluetoothBeaconSource> { BLEScannerPassiveBluetoothBeaconSource(get()) }

    single { PassiveScanManager(get(), get(), get(), get()) }

    single { PassiveScanStateManager(get(PASSIVE_SCAN_STATE)) }

    single {
        PassiveScanReportCreator(get(), get(), get(), get(), get(), postProcessors = getAll())
    }
}
