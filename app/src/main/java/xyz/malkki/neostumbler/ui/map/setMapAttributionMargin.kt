package xyz.malkki.neostumbler.ui.map

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.maplibre.android.maps.MapLibreMap

fun MapLibreMap.setAttributionMargin(density: Density) {
    val attributionMargin = density.run { 8.dp.roundToPx() }

    uiSettings.isLogoEnabled = false
    uiSettings.setAttributionMargins(attributionMargin, 0, 0, attributionMargin)
    uiSettings.isAttributionEnabled = true
}
