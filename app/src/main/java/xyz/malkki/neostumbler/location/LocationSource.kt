package xyz.malkki.neostumbler.location

import android.Manifest
import androidx.annotation.RequiresPermission
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.Position

fun interface LocationSource {
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getLocations(interval: Duration, usePassiveProvider: Boolean): Flow<Position>
}
