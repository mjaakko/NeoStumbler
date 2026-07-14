package xyz.malkki.neostumbler.data.emitter

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation

/** API for actively scanning Wi-Fi access points */
fun interface ActiveWifiAccessPointSource {
    /**
     * @param scanThrottled Whether the scanning is throttled either due to user preferences or
     *   system settings
     */
    fun getWifiAccessPointFlow(
        scanThrottled: Boolean,
        scanInterval: Flow<Duration>,
        rangingMode: RangingMode,
    ): Flow<List<EmitterObservation<WifiAccessPoint, MacAddress>>>

    enum class RangingMode {
        /** Never do RTT ranging to estimate distance to the access points */
        NEVER,
        /** Always try to use RTT ranging even if the access point does not support it */
        ALWAYS,
        /** Only do RTT ranging for access points supporting two-sided ranging */
        TWOSIDED,
    }
}
