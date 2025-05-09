package xyz.malkki.neostumbler.utils

import android.os.FileObserver
import java.nio.file.Path
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** @param mask Event type mask from [FileObserver] */
fun watchDirectory(directory: Path, mask: Int = FileObserver.ALL_EVENTS): Flow<Unit> =
    callbackFlow {
        val fileObserver =
            object : FileObserver(directory.toFile(), mask) {
                override fun onEvent(event: Int, path: String?) {
                    trySendBlocking(Unit)
                }
            }

        fileObserver.startWatching()

        awaitClose { fileObserver.stopWatching() }
    }
