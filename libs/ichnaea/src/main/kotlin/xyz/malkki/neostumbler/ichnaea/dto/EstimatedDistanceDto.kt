package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class EstimatedDistanceDto(val distance: Double, val accuracy: Double?, val type: Type?) {
    enum class Type {
        ONE_SIDED,
        TWO_SIDED,
    }
}
