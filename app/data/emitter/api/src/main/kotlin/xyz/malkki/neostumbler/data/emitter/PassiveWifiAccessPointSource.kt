package xyz.malkki.neostumbler.data.emitter

import xyz.malkki.neostumbler.core.WifiAccessPoint

/** Passive source for Wi-Fi access points */
fun interface PassiveWifiAccessPointSource {
    /** Finds currently known Wi-Fi access points without initiating a scan */
    fun getWifiAccessPoints(): List<WifiAccessPoint>
}
