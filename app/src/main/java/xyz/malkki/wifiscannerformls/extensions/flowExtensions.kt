package xyz.malkki.wifiscannerformls.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration

fun <A, B> Flow<Pair<A?, B?>>.filterNotNullPairs(): Flow<Pair<A, B>> = filter { it.first != null && it.second != null }.map { it.first!! to it.second!! }

fun <A, B, C> Flow<A>.combineAny(other: Flow<B>, combiner: suspend (A?, B?) -> C): Flow<C> = channelFlow {
    var latestA: A? = null
    var latestB: B? = null

    launch {
        collect {
            latestA = it

            send(combiner(latestA, latestB))
        }
    }
    launch {
        other.collect {
            latestB = it

            send(combiner(latestA, latestB))
        }
    }
}

fun <A, B, C, D> Flow<A>.combineAny(other1: Flow<B>, other2: Flow<C>, combiner: suspend (A?, B?, C?) -> D): Flow<D> = channelFlow {
    var latestA: A? = null
    var latestB: B? = null
    var latestC: C? = null

    launch {
        collect {
            latestA = it

            send(combiner(latestA, latestB, latestC))
        }
    }
    launch {
        other1.collect {
            latestB = it

            send(combiner(latestA, latestB, latestC))
        }
    }
    launch {
        other2.collect {
            latestC = it

            send(combiner(latestA, latestB, latestC))
        }
    }
}


fun <T> Flow<T>.buffer(window: Duration): Flow<List<T>> = channelFlow {
    val items: MutableList<T> = mutableListOf<T>()
    var finished = false

    launch {
        collect {
            items.add(it)
        }

        finished = true
    }

    while (true) {
        delay(window)

        if (items.isNotEmpty()) {
            send(items.toList())
            items.clear()
        }

        if (finished) {
            break
        }
    }
}