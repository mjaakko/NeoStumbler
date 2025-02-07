package xyz.malkki.neostumbler.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuggestedServiceTest {
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testReadingSuggestedServicesFromJson() {
        val services = SuggestedService.getSuggestedServices(testContext)

        assertTrue(services.isNotEmpty())
    }
}
