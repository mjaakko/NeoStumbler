package xyz.malkki.neostumbler.network

import org.koin.dsl.module

val networkModule = module {
    single<HttpCallFactoryProvider> { OkHttpCallFactoryProvider(get()) }

    single<NetworkErrorHandler> { SimpleNetworkErrorHandler() }
}
