package xyz.malkki.neostumbler.data.emitter.internal

import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert
import org.junit.Test

class RateLimiterTest {
    @Test
    fun `Test rate limiter with 2 coroutines`() = runTest {
        val period = 2.seconds

        val rateLimiter = RateLimiter(4, period, { testScheduler.currentTime })

        val job1 =
            launch(start = CoroutineStart.LAZY) {
                repeat(4) { rateLimiter.doRateLimited { yield() } }
            }
        val job2 =
            launch(start = CoroutineStart.LAZY) {
                repeat(4) { rateLimiter.doRateLimited { yield() } }
            }

        val elapsed = testScheduler.timeSource.measureTime { listOf(job1, job2).joinAll() }

        Assert.assertTrue("Jobs finished sooner than expected", elapsed >= period)
        Assert.assertTrue("Jobs finished later than expected", elapsed < period * 2)
    }

    @Test
    fun `Test rate limiter with 3 coroutines`() = runTest {
        val period = 2.seconds

        val rateLimiter = RateLimiter(4, period, { testScheduler.currentTime })

        val job1 =
            launch(start = CoroutineStart.LAZY) {
                repeat(4) { rateLimiter.doRateLimited { yield() } }
            }
        val job2 =
            launch(start = CoroutineStart.LAZY) {
                repeat(4) { rateLimiter.doRateLimited { yield() } }
            }
        val job3 =
            launch(start = CoroutineStart.LAZY) {
                repeat(4) { rateLimiter.doRateLimited { yield() } }
            }

        val elapsed = testScheduler.timeSource.measureTime { listOf(job1, job2, job3).joinAll() }

        Assert.assertTrue("Jobs finished sooner than expected", elapsed >= period * 2)
        Assert.assertTrue("Jobs finished later than expected", elapsed < period * 3)
    }
}
