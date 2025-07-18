package xyz.malkki.neostumbler.data.emitter

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.WifiAccessPoint

/** API for actively scanning Wi-Fi access points */
fun interface ActiveWifiAccessPointSource {
    fun getWifiAccessPointFlow(scanInterval: Flow<Duration>): Flow<List<WifiAccessPoint>>
}
