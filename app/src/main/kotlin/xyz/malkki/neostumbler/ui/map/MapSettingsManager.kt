package xyz.malkki.neostumbler.ui.map

import androidx.annotation.StringRes
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnum
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.setEnum

class MapSettingsManager(private val settings: Settings) {
    val selectedMapStyle: Flow<MapTileSource> =
        settings.getSnapshotFlow().map { snapshot ->
            snapshot.getEnum<PredefinedMapTileSource>(PreferenceKeys.MAP_TILE_SOURCE)
                ?: snapshot.getEnum<CustomMapTileSource>(PreferenceKeys.MAP_TILE_SOURCE)
                ?: MapTileSource.DEFAULT
        }

    val selectedMapStyleUrl =
        settings.getSnapshotFlow().map { snapshot ->
            val mapTileSource: MapTileSource =
                snapshot.getEnum<PredefinedMapTileSource>(PreferenceKeys.MAP_TILE_SOURCE)
                    ?: snapshot.getEnum<CustomMapTileSource>(PreferenceKeys.MAP_TILE_SOURCE)
                    ?: MapTileSource.DEFAULT

            when (mapTileSource) {
                CustomMapTileSource.CUSTOM_URL -> {
                    MapStyleUrl(
                        styleUrl =
                            snapshot.getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL) ?: ""
                    )
                }

                CustomMapTileSource.CUSTOM_FILE -> {
                    MapStyleUrl(
                        styleUrl =
                            snapshot.getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE)?.let {
                                "file:/$it"
                            } ?: ""
                    )
                }

                is PredefinedMapTileSource -> {
                    MapStyleUrl(
                        styleUrl = mapTileSource.styleUrl,
                        styleUrlDark = mapTileSource.styleUrlDark,
                    )
                }
            }
        }

    val mapStyleFile =
        settings.getSnapshotFlow().map { snapshot ->
            snapshot.getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE)?.let { Path(it) }
        }

    val mapThemeOverride =
        settings.getEnumFlow(PreferenceKeys.MAP_THEME_OVERRIDE, OverrideMapTheme.SYSTEM)

    suspend fun setMapTileSource(mapTileSource: MapTileSource) {
        settings.edit {
            when (mapTileSource) {
                is PredefinedMapTileSource -> {
                    setEnum(PreferenceKeys.MAP_TILE_SOURCE, mapTileSource)
                }
                is CustomMapTileSource -> {
                    setEnum(PreferenceKeys.MAP_TILE_SOURCE, mapTileSource)
                }
            }
        }
    }

    suspend fun setCustomUrlMapTileSource(styleUrl: String) {
        settings.edit { setString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL, styleUrl) }
    }

    suspend fun setCustomFileMapTileSource(file: Path?) {
        settings.edit {
            if (file != null) {
                setString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE, file.absolutePathString())
            } else {
                removeString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE)
            }
        }
    }

    suspend fun setMapThemeOverride(mapTheme: OverrideMapTheme) {
        settings.edit { setEnum(PreferenceKeys.MAP_THEME_OVERRIDE, mapTheme) }
    }
}

data class MapStyleUrl(
    val styleUrl: String,
    val styleUrlDark: String? = null,
)

sealed interface MapTileSource {
    companion object {
        val options: List<MapTileSource> =
            PredefinedMapTileSource.entries + CustomMapTileSource.entries

        val DEFAULT = PredefinedMapTileSource.OPENSTREETMAP
    }
}

enum class PredefinedMapTileSource(
    val title: String,
    val styleUrl: String,
    val styleUrlDark: String? = null,
) : MapTileSource {
    OPENSTREETMAP(title = "OpenStreetMap", styleUrl = "asset://osm_raster_style.json"),
    OPENFREEMAP(
        title = "OpenFreeMap",
        styleUrl = "https://tiles.openfreemap.org/styles/liberty",
        styleUrlDark = "https://tiles.openfreemap.org/styles/dark",
    ),
    VERSATILES(
        title = "VersaTiles",
        styleUrl = "https://tiles.versatiles.org/assets/styles/colorful/style.json",
        styleUrlDark = "https://tiles.versatiles.org/assets/styles/eclipse/style.json",
    ),
}

enum class CustomMapTileSource(@StringRes val titleResourceId: Int) : MapTileSource {
    CUSTOM_URL(titleResourceId = R.string.map_tile_source_custom_url_title),
    CUSTOM_FILE(titleResourceId = R.string.map_tile_source_custom_file_title),
}

enum class OverrideMapTheme {
    /** Show map in system theme */
    SYSTEM,
    /** Show map in light theme */
    LIGHT,
    /** Show map in dark theme */
    DARK,
}
