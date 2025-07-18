package xyz.malkki.neostumbler.data.emitter

import xyz.malkki.neostumbler.core.CellTower

fun interface PassiveCellTowerSource {
    /** Finds currently known cell towers without initiating a scan */
    fun getCellTowers(): List<CellTower>
}
