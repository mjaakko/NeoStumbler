package xyz.malkki.wifiscannerformls.beacons

import org.altbeacon.beacon.BeaconParser

/**
 * Parser for iBeacons
 */
object IBeaconParser : BeaconParser() {
    init {
        setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
    }
}