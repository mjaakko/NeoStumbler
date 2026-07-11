package xyz.malkki.neostumbler.data.restrictedarea

import android.content.Context
import androidx.datastore.core.DataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.LatLng

class DataStoreRestrictedAreaManager
internal constructor(private val dataStore: DataStore<RestrictedAreaSettings>) :
    RestrictedAreaManager {
    constructor(context: Context) : this(context.restrictedAreasDataStore)

    override val restrictedAreas: Flow<List<RestrictedArea>> =
        dataStore.data.map { restrictedAreaSettings ->
            restrictedAreaSettings.restrictedAreas.map { (id, geometry) ->
                RestrictedArea(
                    id = id,
                    circle =
                        Circle(
                            center = LatLng(geometry.latitude, geometry.longitude),
                            radius = geometry.radius,
                        ),
                )
            }
        }

    override suspend fun deleteRestrictedArea(vararg ids: UUID) {
        dataStore.updateData { restrictedAreaSettings ->
            restrictedAreaSettings.copy(
                restrictedAreas = restrictedAreaSettings.restrictedAreas.minus(ids.toSet())
            )
        }
    }

    override suspend fun addRestrictedArea(restrictedArea: RestrictedArea) {
        dataStore.updateData { restrictedAreaSettings ->
            restrictedAreaSettings.copy(
                restrictedAreas =
                    restrictedAreaSettings.restrictedAreas.plus(
                        restrictedArea.id to
                            RestrictedAreaSettings.RestrictedAreaGeometry(
                                latitude = restrictedArea.circle.center.latitude,
                                longitude = restrictedArea.circle.center.longitude,
                                radius = restrictedArea.circle.radius,
                            )
                    )
            )
        }
    }
}
