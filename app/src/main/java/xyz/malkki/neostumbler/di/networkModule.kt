package xyz.malkki.neostumbler.di

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.koin.dsl.module
import xyz.malkki.neostumbler.http.getCallFactory

val networkModule = module {
    single {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.async(start = CoroutineStart.LAZY) { getCallFactory(get()) }
    }
}
