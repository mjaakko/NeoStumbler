package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import kotlin.time.Duration

fun interface WifiAccessPointSource {
    fun getWifiAccessPointFlow(scanInterval: Flow<Duration>): Flow<List<WifiAccessPoint>>
}