package xyz.malkki.neostumbler.data.emitter.internal

import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rate limiter, which allows to do an action N times within the specified time period. Supports
 * bursting within the time period
 *
 * @param permits Number of times the action can be performed
 * @param timePeriod Time period
 * @param timeSource Source for timing. Must return milliseconds
 */
internal class RateLimiter(
    private val permits: Int,
    private val timePeriod: Duration,
    private val timeSource: () -> Long,
) {
    private val mutex = Mutex()

    private var permitsRemaining = permits
    private var startedTimestamp: Long? = null

    suspend fun <T> doRateLimited(block: suspend () -> T): T {
        mutex.withLock {
            if (startedTimestamp == null) {
                startedTimestamp = timeSource.invoke()
            }

            if (permitsRemaining-- == 0) {
                delay(
                    ((startedTimestamp!! + timePeriod.inWholeMilliseconds) - timeSource.invoke())
                        .coerceAtLeast(0L)
                )
                startedTimestamp = timeSource.invoke()
                permitsRemaining = permits - 1
            }
        }

        return block.invoke()
    }
}
