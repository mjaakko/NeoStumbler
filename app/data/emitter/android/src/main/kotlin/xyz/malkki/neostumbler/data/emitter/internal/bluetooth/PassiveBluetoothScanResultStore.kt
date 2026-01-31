package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import android.content.Context
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber

private const val STORAGE_DIR = "ble_scan_results"

internal class PassiveBluetoothScanResultStore(private val directory: Path) {
    constructor(
        context: Context
    ) : this(context.cacheDir.toPath().resolve(STORAGE_DIR).apply { createDirectories() })

    suspend fun save(scanResults: List<PassiveBluetoothBeaconScanResult>) =
        withContext(Dispatchers.IO) {
            directory.resolve("${System.currentTimeMillis()}.json").outputStream().buffered().use {
                outputStream ->
                Json.encodeToStream(scanResults, outputStream)
            }
        }

    /**
     * Retrieves stored beacon scan results from the store. The returned results are removed from
     * the store
     */
    suspend fun get(): List<PassiveBluetoothBeaconScanResult> =
        withContext(Dispatchers.IO) {
            val results =
                directory.listDirectoryEntries().flatMap { file ->
                    val results: List<PassiveBluetoothBeaconScanResult> =
                        file.inputStream().buffered().use { inputStream ->
                            try {
                                Json.decodeFromStream(inputStream)
                            } catch (ex: IllegalArgumentException) {
                                Timber.w(ex, "Failed to parse stored Bluetooth scan results")

                                emptyList()
                            }
                        }

                    file.deleteIfExists()

                    results
                }

            results
        }
}
