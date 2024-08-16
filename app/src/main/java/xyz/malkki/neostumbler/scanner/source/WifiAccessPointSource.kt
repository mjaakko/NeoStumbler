package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import kotlin.time.Duration

interface WifiAccessPointSource {
    fun getWifiAccessPointFlow(interval: Duration): Flow<List<WifiAccessPoint>>
}