package xyz.malkki.neostumbler.data.emitter.internal.util

import android.app.UiAutomation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceInteractivityListenerTest {

    @Test
    fun testDeviceInteractivityListener() = runTest {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        val screenOnChannel = Channel<Boolean>()

        backgroundScope.launch {
            context.getDeviceInteractiveFlow().collect { screenOnChannel.send(it) }
        }

        val initialState = screenOnChannel.receive()

        instrumentation.uiAutomation.toggleScreen()

        Assert.assertEquals(!initialState, screenOnChannel.receive())

        instrumentation.uiAutomation.toggleScreen()

        Assert.assertEquals(initialState, screenOnChannel.receive())
    }

    private fun UiAutomation.toggleScreen() = executeShellCommand("input keyevent 26")
}
