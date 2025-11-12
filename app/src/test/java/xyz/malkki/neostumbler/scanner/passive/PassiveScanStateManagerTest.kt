package xyz.malkki.neostumbler.scanner.passive

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xyz.malkki.neostumbler.geography.LatLng

class PassiveScanStateManagerTest {
    @Rule @JvmField val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var passiveScanStateManager: PassiveScanStateManager

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create { tmpFolder.newFile("test.preferences_pb") }

        passiveScanStateManager =
            PassiveScanStateManager(dataStore, timeSource = { Long.MAX_VALUE })
    }

    @Test
    fun `Save and get passive report location`() = runTest {
        passiveScanStateManager.updateLastReportLocation(
            dataType = PassiveScanStateManager.DataType.CELL,
            latLng = LatLng(latitude = 10.12345, longitude = 20.98765),
        )
        passiveScanStateManager.updateLastReportLocation(
            dataType = PassiveScanStateManager.DataType.WIFI,
            latLng = LatLng(latitude = 11.11111, longitude = 22.22222),
        )

        val cellLocation =
            passiveScanStateManager.getLastReportLocation(
                dataType = PassiveScanStateManager.DataType.CELL
            )
        assertNotNull(cellLocation)
        assertEquals(10.12345, cellLocation!!.latitude, 0.00001)
        assertEquals(20.98765, cellLocation.longitude, 0.00001)

        val wifiLocation =
            passiveScanStateManager.getLastReportLocation(
                dataType = PassiveScanStateManager.DataType.WIFI
            )
        assertNotNull(wifiLocation)
        assertEquals(11.11111, wifiLocation!!.latitude, 0.00001)
        assertEquals(22.22222, wifiLocation.longitude, 0.00001)
    }

    @Test
    fun `Set and get max timestamp`() = runTest {
        passiveScanStateManager.updateMaxTimestamp(
            dataType = PassiveScanStateManager.DataType.CELL,
            10000,
        )
        passiveScanStateManager.updateMaxTimestamp(
            dataType = PassiveScanStateManager.DataType.WIFI,
            20000,
        )

        assertEquals(
            10000L,
            passiveScanStateManager.getMaxTimestamp(
                dataType = PassiveScanStateManager.DataType.CELL
            ),
        )
        assertEquals(
            20000L,
            passiveScanStateManager.getMaxTimestamp(
                dataType = PassiveScanStateManager.DataType.WIFI
            ),
        )
    }
}
