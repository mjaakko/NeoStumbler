package xyz.malkki.neostumbler.data.restrictedarea

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.LatLng

class DataStoreRestrictedAreaManagerTest {
    @get:Rule val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<RestrictedAreaSettings>
    private lateinit var manager: DataStoreRestrictedAreaManager

    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher() + Job())

        val testFile = tmpFolder.newFile("test_restricted_areas.json")

        dataStore =
            DataStoreFactory.create(
                serializer = RestrictedAreaSettingsSerializer,
                produceFile = { testFile },
                corruptionHandler =
                    ReplaceFileCorruptionHandler { RestrictedAreaSettings(emptyMap()) },
                scope = testScope,
            )

        manager = DataStoreRestrictedAreaManager(dataStore)
    }

    @Test
    fun `addRestrictedArea writes new area to the DataStore`() = testScope.runTest {
        val id = UUID.randomUUID()
        val newArea =
            RestrictedArea(id = id, circle = Circle(center = LatLng(50.0, 60.0), radius = 500.0))

        manager.addRestrictedArea(newArea)

        val updatedSettings = dataStore.data.first()
        assertTrue(updatedSettings.restrictedAreas.containsKey(id))

        val savedArea = updatedSettings.restrictedAreas[id]
        assertNotNull(savedArea)
        assertEquals(50.0, savedArea!!.latitude, 0.0)
        assertEquals(60.0, savedArea.longitude, 0.0)
        assertEquals(500.0, savedArea.radius, 0.0)
    }

    @Test
    fun `deleteRestrictedArea removes specified IDs from the DataStore`() = testScope.runTest {
        val idToKeep = UUID.randomUUID()
        val idToDelete1 = UUID.randomUUID()
        val idToDelete2 = UUID.randomUUID()

        dataStore.updateData {
            RestrictedAreaSettings(
                restrictedAreas =
                    mapOf(
                        idToKeep to RestrictedAreaSettings.RestrictedAreaGeometry(0.0, 0.0, 10.0),
                        idToDelete1 to
                            RestrictedAreaSettings.RestrictedAreaGeometry(1.0, 1.0, 10.0),
                        idToDelete2 to
                            RestrictedAreaSettings.RestrictedAreaGeometry(2.0, 2.0, 10.0),
                    )
            )
        }

        manager.deleteRestrictedArea(idToDelete1, idToDelete2)

        val updatedSettings = dataStore.data.first()
        assertEquals(1, updatedSettings.restrictedAreas.size)
        assertTrue(idToKeep in updatedSettings.restrictedAreas)
        assertFalse(idToDelete1 in updatedSettings.restrictedAreas)
        assertFalse(idToDelete2 in updatedSettings.restrictedAreas)
    }
}
