package xyz.malkki.neostumbler.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MacAddressTest {
    @Test
    fun `Test creating valid MAC address`() {
        val macAddresses =
            listOf("00:00:00:00:00:00", "00:00:00:ab:cd:ef", "ff:ff:ff:ff:ff:ff").flatMap {
                listOf(it, it.uppercase())
            }

        macAddresses.forEach { macAddress ->
            assertEquals(macAddress.lowercase(), MacAddress(macAddress).value)
        }
    }

    @Test
    fun `Test creating invalid MAC address throws an exception`() {
        assertThrows(IllegalArgumentException::class.java) { MacAddress("invalid") }
    }
}
