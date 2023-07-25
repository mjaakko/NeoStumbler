package xyz.malkki.wifiscannerformls.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

fun <T> Flow<T>.buffer(windowMillis: Long): Flow<List<T>> = channelFlow {
    val items: MutableList<T> = mutableListOf<T>()
    var finished = false

    launch {
        collect {
            items.add(it)
        }

        finished = true
    }

    while (true) {
        delay(windowMillis)

        if (items.isNotEmpty()) {
            send(items.toList())
            items.clear()
        }

        if (finished) {
            break
        }
    }
}