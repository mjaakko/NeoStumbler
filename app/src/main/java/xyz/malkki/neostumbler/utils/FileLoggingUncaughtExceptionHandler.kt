package xyz.malkki.neostumbler.utils

import java.io.PrintWriter
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.io.path.outputStream

/**
 * @property directory Directory where to create the log files
 * @property nextHandler Next handler in the chain
 */
class FileLoggingUncaughtExceptionHandler(
    private val directory: Path,
    private val nextHandler: Thread.UncaughtExceptionHandler? = null,
) : Thread.UncaughtExceptionHandler {
    private fun getFileName(): String {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        return "crash_$now.txt"
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val file = directory.resolve(getFileName())

            PrintWriter(file.outputStream()).use { e.printStackTrace(it) }
        } finally {
            nextHandler?.uncaughtException(t, e)
        }
    }
}
