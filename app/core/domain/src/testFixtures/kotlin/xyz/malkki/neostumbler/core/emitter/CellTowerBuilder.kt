package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.values.SignalStrength

fun someCellTower(
    radioType: CellTower.RadioType = CellTower.RadioType.LTE,
    mobileCountryCode: String? = "001",
    mobileNetworkCode: String? = "01",
    cellId: Long? = 12345L,
    locationAreaCode: Int? = 100,
    asu: Int? = null,
    primaryScramblingCode: Int? = null,
    serving: Int? = null,
    timingAdvance: Int? = null,
    arfcn: Int? = null,
    signalStrength: SignalStrength? = SignalStrength(-60),
) =
    CellTower(
        radioType = radioType,
        mobileCountryCode = mobileCountryCode,
        mobileNetworkCode = mobileNetworkCode,
        cellId = cellId,
        locationAreaCode = locationAreaCode,
        asu = asu,
        primaryScramblingCode = primaryScramblingCode,
        serving = serving,
        timingAdvance = timingAdvance,
        arfcn = arfcn,
        signalStrength = signalStrength,
    )
