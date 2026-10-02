package xyz.malkki.neostumbler.ui.map

enum class MapTileSource(
    val title: String?,
    val sourceUrl: String?,
    val sourceUrlDark: String? = null,
) {
    OPENSTREETMAP(title = "OpenStreetMap", sourceUrl = "asset://osm_raster_style.json"),
    OPENFREEMAP(
        title = "OpenFreeMap",
        sourceUrl = "https://tiles.openfreemap.org/styles/liberty",
        sourceUrlDark = "https://tiles.openfreemap.org/styles/dark",
    ),
    VERSATILES(
        title = "VersaTiles",
        sourceUrl = "https://tiles.versatiles.org/assets/styles/colorful/style.json",
        sourceUrlDark = "https://tiles.versatiles.org/assets/styles/eclipse/style.json",
    ),
    CUSTOM(title = null, sourceUrl = null);

    companion object {
        val DEFAULT = OPENSTREETMAP
    }
}
