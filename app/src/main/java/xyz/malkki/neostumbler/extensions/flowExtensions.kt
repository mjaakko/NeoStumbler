package xyz.malkki.neostumbler.extensions

import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

/**
 * Emits null if the flow does not emit another value within the specified duration. Note that this
 * will cause the flow to emit null as the last value (i.e. it probably makes sense to use this
 * operator only for flows that never complete)
 */
fun <T> Flow<T>.maxAge(duration: Duration): Flow<T?> = transformLatest { value ->
    emit(value)
    delay(duration)
    emit(null)
}
