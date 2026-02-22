package xyz.malkki.neostumbler.coroutineservice

import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class CoroutineServiceTest {
    @Test
    fun `Service is stopped when the coroutine throws an exception`() = runTest {
        val service =
            spy(
                object : CoroutineService(StandardTestDispatcher(testScheduler)) {
                    override fun onBind(p0: Intent?): IBinder? = null

                    override fun onCreate() {
                        super.onCreate()

                        serviceScope.launch {
                            @Suppress("TooGenericExceptionThrown") throw RuntimeException("oops")
                        }
                    }
                }
            )

        service.onCreate()

        testScheduler.advanceUntilIdle()

        verify(service, times(1)).stopSelf()
    }
}
