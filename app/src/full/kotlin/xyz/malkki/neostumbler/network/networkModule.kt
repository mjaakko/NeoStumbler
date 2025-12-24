package xyz.malkki.neostumbler.network

import org.koin.dsl.module

val networkModule = module {
    single<HttpCallFactoryProvider> { CronetWithOkHttpFallbackCallFactoryProvider(get()) }

    single<NetworkErrorHandler> { CronetAwareNetworkErrorHandler() }
}
