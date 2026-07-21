package xyz.malkki.neostumbler.core.ranging

import xyz.malkki.neostumbler.core.values.Distance

/**
 * Estimated distance to the emitter. The distance can be estimated with technologies such as Wi-Fi
 * RTT or BLE channel sounding.
 */
data class EstimatedDistance(
    val distance: Distance,
    /** Accuracy of the estimated distance. `null` if unknown */
    val accuracy: Distance?,
    /** Type of ranging that was used to estimate the distance. `null if unknown` */
    val rangingType: RangingType?,
) {
    enum class RangingType {
        /**
         * One-sided ranging. One-sided ranging does not subtract the processing delay in the other
         * end. In practice, the estimated distance can be incorrect by multiple orders of
         * magnitude.
         */
        ONE_SIDED,
        /**
         * Two-sided ranging. The processing delay is compensated for and the estimated distance
         * should be more or less accurate.
         */
        TWO_SIDED,
    }
}
