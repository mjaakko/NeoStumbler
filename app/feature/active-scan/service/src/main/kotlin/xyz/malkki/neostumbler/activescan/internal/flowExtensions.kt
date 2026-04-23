package xyz.malkki.neostumbler.activescan.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

internal fun <A, B, C> Flow<A>.combineWithLatestFrom(
    other: Flow<B>,
    combiner: (A, B?) -> C,
): Flow<C> = channelFlow {
    var otherValue: B? = null

    launch { other.collect { otherValue = it } }

    collect { send(combiner.invoke(it, otherValue)) }
}

internal fun <T> Flow<T>.pairwise(): Flow<Pair<T, T>> =
    scan(Pair<T?, T?>(null, null)) { pair, value -> pair.second to value }
        .filter { it.first != null && it.second != null }
        .map { it.first!! to it.second!! }
