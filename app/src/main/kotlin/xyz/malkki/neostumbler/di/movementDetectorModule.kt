package xyz.malkki.neostumbler.di

import org.koin.dsl.module
import xyz.malkki.neostumbler.data.movement.MovementDetectorProvider
import xyz.malkki.neostumbler.scanner.movement.SettingsAwareMovementDetectorProvider

val movementDetectorModule = module {
    single<MovementDetectorProvider> { SettingsAwareMovementDetectorProvider(get(), get(), get()) }
}
