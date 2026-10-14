package xyz.malkki.neostumbler.ui.map

import kotlin.io.path.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.SettingsEditor
import xyz.malkki.neostumbler.data.settings.SettingsSnapshot
import xyz.malkki.neostumbler.data.settings.setEnum

class MapSettingsManagerTest {

    private class StubSettings : Settings {
        private val settingsMap = mutableMapOf<String, Any?>()
        private val _snapshotFlow = MutableStateFlow(StubSettingsSnapshot(settingsMap))

        override fun getSnapshotFlow(): kotlinx.coroutines.flow.Flow<SettingsSnapshot> =
            _snapshotFlow

        override suspend fun edit(editSettings: SettingsEditor.() -> Unit) {
            val editor = StubSettingsEditor(settingsMap)
            editor.editSettings()
            _snapshotFlow.value = StubSettingsSnapshot(settingsMap)
        }

        private class StubSettingsSnapshot(private val data: Map<String, Any?>) : SettingsSnapshot {
            override fun getString(key: String): String? = data[key] as? String

            override fun getStringSet(key: String): Set<String>? = data[key] as? Set<String>

            override fun getBoolean(key: String): Boolean? = data[key] as? Boolean

            override fun getInt(key: String): Int? = data[key] as? Int
        }

        private class StubSettingsEditor(private val data: MutableMap<String, Any?>) :
            SettingsEditor {
            override fun setString(key: String, value: String) {
                data[key] = value
            }

            override fun setStringSet(key: String, value: Set<String>) {
                data[key] = value
            }

            override fun setBoolean(key: String, value: Boolean) {
                data[key] = value
            }

            override fun setInt(key: String, value: Int) {
                data[key] = value
            }

            override fun removeString(key: String) {
                data.remove(key)
            }

            override fun removeStringSet(key: String) {
                data.remove(key)
            }

            override fun removeBoolean(key: String) {
                data.remove(key)
            }

            override fun removeInt(key: String) {
                data.remove(key)
            }
        }
    }

    private lateinit var settings: StubSettings
    private lateinit var manager: MapSettingsManager

    @Before
    fun setup() {
        settings = StubSettings()
        manager = MapSettingsManager(settings)
    }

    @Test
    fun `selectedMapStyle returns default when no settings are set`() = runTest {
        assertEquals(MapTileSource.DEFAULT, manager.selectedMapStyle.first())
    }

    @Test
    fun `selectedMapStyle returns predefined source from settings`() = runTest {
        settings.edit {
            setEnum(PreferenceKeys.MAP_TILE_SOURCE, PredefinedMapTileSource.OPENFREEMAP)
        }

        assertEquals(PredefinedMapTileSource.OPENFREEMAP, manager.selectedMapStyle.first())
    }

    @Test
    fun `selectedMapStyle returns custom URL source from settings`() = runTest {
        settings.edit {
            setEnum(PreferenceKeys.MAP_TILE_SOURCE, CustomMapTileSource.CUSTOM_URL)
            setString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL, "https://example.com/style.json")
        }

        val source = manager.selectedMapStyle.first()
        assertEquals(CustomMapTileSource.CUSTOM_URL, source)
    }

    @Test
    fun `selectedMapStyleUrl returns predefined style url`() = runTest {
        settings.edit {
            setEnum(PreferenceKeys.MAP_TILE_SOURCE, PredefinedMapTileSource.OPENFREEMAP)
        }

        val url = manager.selectedMapStyleUrl.first()
        assertEquals("https://tiles.openfreemap.org/styles/liberty", url.styleUrl)
        assertEquals("https://tiles.openfreemap.org/styles/dark", url.styleUrlDark)
    }

    @Test
    fun `selectedMapStyleUrl returns custom url`() = runTest {
        val customUrl = "https://custom.com/style.json"

        settings.edit {
            setEnum(PreferenceKeys.MAP_TILE_SOURCE, CustomMapTileSource.CUSTOM_URL)
            setString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL, customUrl)
        }

        val url = manager.selectedMapStyleUrl.first()
        assertEquals(customUrl, url.styleUrl)
    }

    @Test
    fun `selectedMapStyleUrl returns custom file url`() = runTest {
        val customFile = "/path/to/style.json"

        settings.edit {
            setEnum(PreferenceKeys.MAP_TILE_SOURCE, CustomMapTileSource.CUSTOM_FILE)
            setString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE, customFile)
        }

        val url = manager.selectedMapStyleUrl.first()
        assertEquals("file:/$customFile", url.styleUrl)
    }

    @Test
    fun `setMapTileSource updates settings`() = runTest {
        manager.setMapTileSource(PredefinedMapTileSource.VERSATILES)

        assertEquals(PredefinedMapTileSource.VERSATILES, manager.selectedMapStyle.first())
    }

    @Test
    fun `setCustomUrlMapTileSource updates settings`() = runTest {
        val customUrl = "https://custom.com/style.json"

        manager.setCustomUrlMapTileSource(customUrl)

        assertEquals(
            customUrl,
            settings.getSnapshotFlow().first().getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL),
        )
    }

    @Test
    fun `setCustomFileMapTileSource updates settings`() = runTest {
        val customFile = "/path/to/style.json"

        settings.edit { setEnum(PreferenceKeys.MAP_TILE_SOURCE, CustomMapTileSource.CUSTOM_FILE) }
        manager.setCustomFileMapTileSource(Path(customFile))

        assertEquals(
            customFile,
            settings
                .getSnapshotFlow()
                .first()
                .getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE),
        )
        val url = manager.selectedMapStyleUrl.first()
        assertEquals("file:/$customFile", url.styleUrl)
    }

    @Test
    fun `setCustomFileMapTileSource removes file when null`() = runTest {
        val customFile = "/path/to/style.json"

        settings.edit {
            setEnum(PreferenceKeys.MAP_TILE_SOURCE, CustomMapTileSource.CUSTOM_FILE)
            setString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE, customFile)
        }
        manager.setCustomFileMapTileSource(null)

        assertEquals(
            null,
            settings
                .getSnapshotFlow()
                .first()
                .getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_FILE),
        )
    }
}
