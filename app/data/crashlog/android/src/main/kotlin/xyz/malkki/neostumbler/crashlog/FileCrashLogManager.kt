package xyz.malkki.neostumbler.crashlog

import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.crashlog.internal.watchDirectory

/** @property crashLogDirectory Directory which contains the log files */
class FileCrashLogManager(private val crashLogDirectory: Path) : CrashLogManager {
    override suspend fun clearEntries() =
        withContext(Dispatchers.IO) {
            crashLogDirectory.listDirectoryEntries().forEach { it.deleteIfExists() }
        }

    override fun getEntries(): Flow<List<String>> =
        watchDirectory(crashLogDirectory)
            .onStart { emit(Unit) }
            .onEach {
                // File watcher seems to emit events before the change is visible in the filesystem
                delay(100.milliseconds)
            }
            .map {
                withContext(Dispatchers.IO) {
                    return@withContext crashLogDirectory
                        .listDirectoryEntries()
                        .sortedByDescending { it.name }
                        .map { it.relativeTo(crashLogDirectory).toString() }
                }
            }

    override suspend fun deleteEntry(entry: String): Unit =
        withContext(Dispatchers.IO) { crashLogDirectory.resolve(entry).deleteIfExists() }

    override suspend fun getLogsForEntry(entry: String): String? =
        withContext(Dispatchers.IO) {
            val path = crashLogDirectory.resolve(entry)

            return@withContext buildString {
                path.bufferedReader().use { reader ->
                    while (true) {
                        ensureActive()

                        val line = reader.readLine() ?: break

                        if (!isEmpty()) {
                            append("\n")
                        }

                        append(line)
                    }
                }
            }
        }
}
