package xyz.malkki.neostumbler.ui.map

/** User-configurable override for map tile theme (light/dark/system). */
enum class MapThemeOverride {
    /** Follow the device system theme (default). */
    SYSTEM,

    /** Always use the light map style. */
    LIGHT,

    /** Always use the dark map style when available, otherwise fall back to the light style. */
    DARK,
}
