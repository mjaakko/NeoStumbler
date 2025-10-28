package xyz.malkki.neostumbler.beaconlibrary

import org.altbeacon.beacon.BeaconParser

object RuuviTagV5BeaconParser : BeaconParser() {
    init {
        setBeaconLayout("m:0-2=990405,i:20-25")
    }
}
