package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import android.content.Context
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber
import xyz.malkki.neostumbler.core.MacAddress

private const val STORAGE_DIR = "ble_scan_results"

// Maximum storage size in bytes
private const val MAX_SIZE: Long = 10 * 1024 * 1024

internal class PassiveBluetoothScanResultStore(
    private val directory: Path,
    private val maxSize: Long = MAX_SIZE,
) {
    constructor(
        context: Context
    ) : this(context.cacheDir.toPath().resolve(STORAGE_DIR).apply { createDirectories() })

    suspend fun save(scanResults: List<PassiveBluetoothBeaconScanResult>) =
        withContext(Dispatchers.IO) {
            scanResults.forEach { scanResult ->
                directory
                    .resolve("${MacAddress(scanResult.address).raw}.json")
                    .outputStream()
                    .buffered()
                    .use { outputStream -> Json.encodeToStream(scanResult, outputStream) }
            }

            trimToSize()
        }

    /**
     * Retrieves stored beacon scan results from the store. The returned results are removed from
     * the store
     */
    suspend fun get(): List<PassiveBluetoothBeaconScanResult> =
        withContext(Dispatchers.IO) {
            buildList {
                directory.forEachDirectoryEntry { file ->
                    file.inputStream().buffered().use { inputStream ->
                        try {
                            add(Json.decodeFromStream(inputStream))
                        } catch (ex: IllegalArgumentException) {
                            Timber.w(ex, "Failed to parse stored Bluetooth scan results")
                        }
                    }

                    file.deleteIfExists()
                }
            }
        }

    private fun trimToSize() {
        val files = directory.listDirectoryEntries().sortedBy { it.getLastModifiedTime() }

        var totalSize = files.sumOf { it.fileSize() }

        for (file in files) {
            if (totalSize < maxSize) {
                break
            }

            totalSize -= file.fileSize()

            file.deleteExisting()
        }
    }
}
