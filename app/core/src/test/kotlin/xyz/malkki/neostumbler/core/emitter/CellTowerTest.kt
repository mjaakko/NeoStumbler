package xyz.malkki.neostumbler.core.emitter

import org.junit.Assert
import org.junit.Test
import xyz.malkki.neostumbler.core.emitter.CellTower.Companion.fillMissingData
import xyz.malkki.neostumbler.core.observation.EmitterObservation

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
                ),
            )

        val observations = cellTowers.map { EmitterObservation(emitter = it, timestamp = 0) }

        val missingDataFilled = observations.fillMissingData(null)

        Assert.assertEquals(2, missingDataFilled.size)
        Assert.assertEquals("55", missingDataFilled[0].emitter.mobileNetworkCode)
        Assert.assertEquals("55", missingDataFilled[1].emitter.mobileNetworkCode)
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
                )
            )

        val observations = cellTowers.map { EmitterObservation(emitter = it, timestamp = 0) }

        val missingDataFilled = observations.fillMissingData("11147")

        Assert.assertEquals("47", missingDataFilled[0].emitter.mobileNetworkCode)
    }
}
