package xyz.malkki.neostumbler.data.emitter

import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation

fun interface PassiveCellTowerSource {
    /** Finds currently known cell towers without initiating a scan */
    fun getCellTowers(): List<EmitterObservation<CellTower, String>>
}
