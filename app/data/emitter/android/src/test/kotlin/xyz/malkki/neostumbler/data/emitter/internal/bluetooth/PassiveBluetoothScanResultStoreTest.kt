package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PassiveBluetoothScanResultStoreTest {
    @Rule @JvmField val tempFolder = TemporaryFolder()

    private lateinit var directory: Path
    private lateinit var store: PassiveBluetoothScanResultStore

    @Before
    fun setup() {
        directory = tempFolder.newFolder().toPath()

        store = PassiveBluetoothScanResultStore(directory = directory, maxSize = 10 * 1024)
    }

    @Test
    fun testSaveAndGet() = runTest {
        val results =
            listOf(
                PassiveBluetoothBeaconScanResult(
                    timestamp = 1000L,
                    address = "AA:BB:CC:DD:EE:FF",
                    signalStrength = -50,
                    beaconType = 0x0215,
                    identifiers =
                        listOf(
                            UUID.randomUUID().let { uuid ->
                                PassiveBluetoothBeaconScanResult.Identifier.UuidIdentifier(
                                    uuid.mostSignificantBits,
                                    uuid.leastSignificantBits,
                                )
                            },
                            PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(1),
                            PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(2),
                        ),
                )
            )

        store.save(results)

        assertEquals(results, store.get())

        assertTrue(directory.listDirectoryEntries().isEmpty())
    }

    @Test
    fun `Only one scan result is saved per MAC address`() = runTest {
        val results =
            listOf(
                PassiveBluetoothBeaconScanResult(
                    timestamp = 1000L,
                    address = "AA:BB:CC:DD:EE:FF",
                    signalStrength = -50,
                    beaconType = 0x0215,
                    identifiers =
                        listOf(
                            UUID.randomUUID().let { uuid ->
                                PassiveBluetoothBeaconScanResult.Identifier.UuidIdentifier(
                                    uuid.mostSignificantBits,
                                    uuid.leastSignificantBits,
                                )
                            },
                            PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(1),
                            PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(2),
                        ),
                ),
                PassiveBluetoothBeaconScanResult(
                    timestamp = 3000L,
                    address = "AA:BB:CC:DD:EE:FF",
                    signalStrength = -50,
                    beaconType = 0x0215,
                    identifiers =
                        listOf(
                            UUID.randomUUID().let { uuid ->
                                PassiveBluetoothBeaconScanResult.Identifier.UuidIdentifier(
                                    uuid.mostSignificantBits,
                                    uuid.leastSignificantBits,
                                )
                            },
                            PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(1),
                            PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(2),
                        ),
                ),
            )

        store.save(results)

        assertEquals(1, store.get().size)
    }

    @Test
    fun `Store removes oldest entries when its size grows`() = runTest {
        repeat(1000) {
            val results =
                listOf(
                    PassiveBluetoothBeaconScanResult(
                        timestamp = 1000L,
                        address = "AA:AA:AA:AA:" + it.toString().windowed(2).joinToString(":"),
                        signalStrength = -50,
                        beaconType = 0x0215,
                        identifiers =
                            listOf(
                                UUID.randomUUID().let { uuid ->
                                    PassiveBluetoothBeaconScanResult.Identifier.UuidIdentifier(
                                        uuid.mostSignificantBits,
                                        uuid.leastSignificantBits,
                                    )
                                },
                                PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(1),
                                PassiveBluetoothBeaconScanResult.Identifier.IntIdentifier(2),
                            ),
                    )
                )

            store.save(results)
        }

        assertTrue(directory.listDirectoryEntries().sumOf { it.fileSize() } <= 10 * 1024)
    }

    @Test
    fun testGetWhenDirectoryContainsBrokenData() = runTest {
        directory.resolve("test.json").writeText("not json")

        assertEquals(0, store.get().size)

        assertTrue(directory.listDirectoryEntries().isEmpty())
    }
}
