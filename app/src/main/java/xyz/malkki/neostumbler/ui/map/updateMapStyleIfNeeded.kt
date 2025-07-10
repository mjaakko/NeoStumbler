package xyz.malkki.neostumbler.ui.map

import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

fun MapLibreMap.updateMapStyleIfNeeded(styleUrl: String) {
    if (style != null && style!!.uri != styleUrl) {
        setStyle(Style.Builder().fromUri(styleUrl))
    }
}
