package xyz.malkki.neostumbler.scanner.passive

import org.koin.dsl.module
import xyz.malkki.neostumbler.PASSIVE_SCAN_STATE
import xyz.malkki.neostumbler.PREFERENCES

val passiveScanningModule = module {
    single { PassiveScanManager(get(), get(PREFERENCES)) }

    single { PassiveScanStateManager(get(PASSIVE_SCAN_STATE)) }

    single { PassiveScanReportCreator(get(), get(), get(), getAll()) }
}
