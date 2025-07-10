package xyz.malkki.neostumbler.ui.map

enum class MapTileSource(val title: String?, val sourceUrl: String?) {
    OPENSTREETMAP(title = "OpenStreetMap", sourceUrl = "asset://osm_raster_style.json"),
    OPENFREEMAP(title = "OpenFreeMap", sourceUrl = "https://tiles.openfreemap.org/styles/liberty"),
    VERSATILES(
        title = "VersaTiles",
        sourceUrl = "https://tiles.versatiles.org/assets/styles/colorful/style.json",
    ),
    CUSTOM(title = null, sourceUrl = null);

    companion object {
        val DEFAULT = OPENSTREETMAP
    }
}
