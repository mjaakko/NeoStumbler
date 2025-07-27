package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.observation.EmitterObservation

data class CellTower(
    val radioType: RadioType,
    val mobileCountryCode: String?,
    val mobileNetworkCode: String?,
    val cellId: Long?,
    val locationAreaCode: Int?,
    val asu: Int?,
    val primaryScramblingCode: Int?,
    val serving: Int?,
    val timingAdvance: Int?,
    val arfcn: Int?,
    override val signalStrength: Int? = null,
) : Emitter<String> {
    override val uniqueKey: String
        get() =
            listOf(
                    mobileCountryCode,
                    mobileNetworkCode,
                    locationAreaCode,
                    cellId,
                    primaryScramblingCode,
                )
                .joinToString("/")

    companion object {
        /**
         * Fills missing data from other cell towers in the list
         *
         * @param operatorNumeric Combination of MCC / MNC (same as
         *   [android.telephony.ServiceState.getOperatorNumeric])
         */
        fun List<EmitterObservation<CellTower, String>>.fillMissingData(
            operatorNumeric: String?
        ): List<EmitterObservation<CellTower, String>> {
            if (operatorNumeric == null && size == 1) {
                return this
            } else {
                val mobileCountryCodes = mapNotNull { it.emitter.mobileCountryCode }.toSet()
                val mobileNetworkCodes = mapNotNull { it.emitter.mobileNetworkCode }.toSet()

                if (mobileCountryCodes.size != 1) {
                    // MCC not unique, we can't be sure about which MNC to use
                    return this
                }

                val mcc = mobileCountryCodes.first()

                val mnc =
                    if (mobileNetworkCodes.size == 1) {
                        mobileNetworkCodes.first()
                    } else if (operatorNumeric?.startsWith(mcc) == true) {
                        operatorNumeric.replaceFirst(mcc, "")
                    } else {
                        null
                    }

                if (mnc == null) {
                    return this
                }

                return map { emitterObservation ->
                    emitterObservation.copy(
                        emitter =
                            emitterObservation.emitter.copy(
                                mobileCountryCode = mcc,
                                mobileNetworkCode = mnc,
                            )
                    )
                }
            }
        }
    }

    enum class RadioType {
        GSM,
        WCDMA,
        LTE,
        NR,
    }

    /**
     * Checks if the cell info has enough useful data. Used for filtering neighbouring cells which
     * don't specify their cell ID etc.
     */
    fun hasEnoughData(): Boolean {
        if (mobileCountryCode == null || mobileNetworkCode == null) {
            return false
        }

        return when (radioType) {
            RadioType.GSM -> cellId != null || locationAreaCode != null
            RadioType.WCDMA,
            RadioType.LTE,
            RadioType.NR ->
                cellId != null || locationAreaCode != null || primaryScramblingCode != null
        }
    }
}
