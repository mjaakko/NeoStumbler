package xyz.malkki.neostumbler.coroutinebroadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CoroutineBroadcastReceiverTest {
    @Test
    fun `Broadcast is handled successfully`() = runTest {
        val broadcastReceiver =
            spy(
                object :
                    CoroutineBroadcastReceiver(dispatcher = StandardTestDispatcher(testScheduler)) {
                    override suspend fun handleIntent(context: Context, intent: Intent) {}
                }
            )

        val pendingResult = mock<BroadcastReceiver.PendingResult>()

        doReturn(pendingResult).whenever((broadcastReceiver as BroadcastReceiver)).goAsync()

        broadcastReceiver.onReceive(mock<Context>(), Intent("test"))

        testScheduler.advanceUntilIdle()

        verify(pendingResult, times(1)).finish()
    }

    @Test
    fun `Broadcast does not block indefinitely`() = runTest {
        val broadcastReceiver =
            spy(
                object :
                    CoroutineBroadcastReceiver(dispatcher = StandardTestDispatcher(testScheduler)) {
                    override suspend fun handleIntent(context: Context, intent: Intent) {
                        // Task that never completes
                        delay(Long.MAX_VALUE)
                    }
                }
            )

        val pendingResult = mock<BroadcastReceiver.PendingResult>()

        doReturn(pendingResult).whenever((broadcastReceiver as BroadcastReceiver)).goAsync()

        val now = testTimeSource.markNow()

        broadcastReceiver.onReceive(mock<Context>(), Intent("test"))

        testScheduler.advanceUntilIdle()

        assertTrue(now.elapsedNow() >= 10.seconds)

        verify(pendingResult, times(1)).finish()
    }
}
