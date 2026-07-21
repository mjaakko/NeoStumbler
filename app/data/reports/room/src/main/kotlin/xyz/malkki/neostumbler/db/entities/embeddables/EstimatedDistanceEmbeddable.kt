package xyz.malkki.neostumbler.db.entities.embeddables

import xyz.malkki.neostumbler.core.ranging.EstimatedDistance
import xyz.malkki.neostumbler.core.values.Distance

internal data class EstimatedDistanceEmbeddable(
    val meters: Double,
    val accuracy: Double?,
    val rangingType: EstimatedDistance.RangingType?,
)

internal fun EstimatedDistanceEmbeddable.toDomain(): EstimatedDistance {
    return EstimatedDistance(
        distance = Distance(meters),
        accuracy = accuracy?.let { Distance(it) },
        rangingType = rangingType,
    )
}
