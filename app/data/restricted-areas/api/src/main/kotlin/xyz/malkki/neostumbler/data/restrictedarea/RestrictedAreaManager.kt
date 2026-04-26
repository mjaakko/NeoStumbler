package xyz.malkki.neostumbler.data.restrictedarea

import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface RestrictedAreaManager {
    val restrictedAreas: Flow<List<RestrictedArea>>

    suspend fun deleteRestrictedArea(vararg ids: UUID)

    suspend fun addRestrictedArea(restrictedArea: RestrictedArea)
}
