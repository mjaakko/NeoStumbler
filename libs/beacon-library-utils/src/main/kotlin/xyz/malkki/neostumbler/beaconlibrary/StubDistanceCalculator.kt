package xyz.malkki.neostumbler.beaconlibrary

import org.altbeacon.beacon.distance.DistanceCalculator

/** Stub implementation of DistanceCalculator which always returns 0 */
object StubDistanceCalculator : DistanceCalculator {
    override fun calculateDistance(p0: Int, p1: Double): Double = 0.0
}
