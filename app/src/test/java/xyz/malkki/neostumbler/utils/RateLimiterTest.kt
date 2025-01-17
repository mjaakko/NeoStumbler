package xyz.malkki.neostumbler.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class RateLimiterTest {
    @Test
    fun `Test rate limiter with 2 coroutines`() = runBlocking {
        val period = 2.seconds

        val rateLimiter = RateLimiter(4, period, { System.nanoTime() / 1_000_000 })

        val job1 = launch(Dispatchers.Default) {
            repeat(4) {
                rateLimiter.doRateLimited {
                    yield()
                }
            }
        }
        val job2 = launch(Dispatchers.Default) {
            repeat(4) {
                rateLimiter.doRateLimited {
                    yield()
                }
            }
        }

        val elapsed = measureTime {
            listOf(job1, job2).joinAll()
        }

        assertTrue(elapsed >= period)
        assertTrue(elapsed < period * 2)
    }

    @Test
    fun `Test rate limiter with 3 coroutines`() = runBlocking {
        val period = 2.seconds

        val rateLimiter = RateLimiter(4, period, { System.nanoTime() / 1_000_000 })

        val job1 = launch(Dispatchers.Default) {
            repeat(4) {
                rateLimiter.doRateLimited {
                    yield()
                }
            }
        }
        val job2 = launch(Dispatchers.Default) {
            repeat(4) {
                rateLimiter.doRateLimited {
                    yield()
                }
            }
        }
        val job3 = launch(Dispatchers.Default) {
            repeat(4) {
                rateLimiter.doRateLimited {
                    yield()
                }
            }
        }

        val elapsed = measureTime {
            listOf(job1, job2, job3).joinAll()
        }

        assertTrue(elapsed >= period * 2)
        assertTrue(elapsed < period * 3)
    }
}