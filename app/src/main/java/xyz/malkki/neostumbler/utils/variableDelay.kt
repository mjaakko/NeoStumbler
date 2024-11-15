package xyz.malkki.neostumbler.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlin.time.Duration

/**
 * Delays with the minimum duration received from the flow
 *
 * @param startTime Time from where the delay starts
 * @param timeSource Time source (milliseconds)
 * @param durationFlow Flow of durations
 */
suspend fun delayWithMinDuration(startTime: Long, timeSource: () -> Long, durationFlow: Flow<Duration>) {
    durationFlow
        .scan(null as Duration?) { a, b ->
            listOfNotNull(a, b).minOrNull()
        }
        .filterNotNull()
        .mapLatest { duration ->
            val delayMs = ((startTime + duration.inWholeMilliseconds) - timeSource.invoke())

            if (delayMs > 0) {
                delay(delayMs)
            }
        }
        .first()
}