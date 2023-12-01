package xyz.malkki.neostumbler.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OneTimeActionHelperTest {
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testOneTimeActionIsMarkedAsShown() = runTest {
        val actionName = "test"

        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { testContext.preferencesDataStoreFile("one_time_actions") }
        )

        val helper = OneTimeActionHelper(testDataStore)

        assertFalse(helper.hasActionBeenShown(actionName))

        helper.markActionShown(actionName)

        assertTrue(helper.hasActionBeenShown(actionName))
    }
}