package xyz.malkki.neostumbler.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.malkki.neostumbler.domain.CellTower.Companion.fillMissingData

class CellTowerTest {
    @Test
    fun `Test filling missing data from other cell towers`() {
        val cellTowers =
            listOf(
                CellTower(
                    radioType = CellTower.RadioType.LTE,
                    mobileCountryCode = "111",
                    mobileNetworkCode = "55",
                    cellId = 12345,
                    locationAreaCode = 100,
                    asu = null,
                    primaryScramblingCode = null,
                    serving = null,
                    signalStrength = -60,
                    timingAdvance = null,
                    arfcn = null,
                    timestamp = 50000,
                ),
                CellTower(
                    radioType = CellTower.RadioType.LTE,
                    mobileCountryCode = "111",
                    mobileNetworkCode = null,
                    cellId = 12345,
                    locationAreaCode = 100,
                    asu = null,
                    primaryScramblingCode = null,
                    serving = null,
                    signalStrength = -60,
                    timingAdvance = null,
                    arfcn = null,
                    timestamp = 50000,
                ),
            )

        val missingDataFilled = cellTowers.fillMissingData(null)

        assertEquals(2, missingDataFilled.size)
        assertEquals("55", missingDataFilled[0].mobileNetworkCode)
        assertEquals("55", missingDataFilled[1].mobileNetworkCode)
    }

    @Test
    fun `Test filling missing data from numeric operator code`() {
        val cellTowers =
            listOf(
                CellTower(
                    radioType = CellTower.RadioType.LTE,
                    mobileCountryCode = "111",
                    mobileNetworkCode = null,
                    cellId = 12345,
                    locationAreaCode = 100,
                    asu = null,
                    primaryScramblingCode = null,
                    serving = null,
                    signalStrength = -60,
                    timingAdvance = null,
                    arfcn = null,
                    timestamp = 50000,
                )
            )

        val missingDataFilled = cellTowers.fillMissingData("11147")

        assertEquals("47", missingDataFilled[0].mobileNetworkCode)
    }
}
