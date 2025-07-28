package xyz.malkki.neostumbler.data.emitter

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation

/** Passive source for Wi-Fi access points */
fun interface PassiveWifiAccessPointSource {
    /** Finds currently known Wi-Fi access points without initiating a scan */
    fun getWifiAccessPoints(): List<EmitterObservation<WifiAccessPoint, MacAddress>>
}
