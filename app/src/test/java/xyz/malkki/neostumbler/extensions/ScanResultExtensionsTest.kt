package xyz.malkki.neostumbler.extensions

import android.net.wifi.ScanResult

import org.junit.Assert.assertEquals
import org.junit.Test

import xyz.malkki.neostumbler.scanner.filterForMLS

class ScanResultExtensionsTest {
    @Test
    fun `Test filtering scan results`() {
        val valid = listOf(
            ScanResult().apply {
                SSID = "test"
            },
            ScanResult().apply {
                SSID = "hello_123"
            }
        )
        val invalid = listOf(
            ScanResult().apply {
                SSID = "my_network_nomap"
            },
            ScanResult().apply {
                SSID = ""
            }
        )

        val scanResults = valid + invalid

        val filtered = scanResults.filterForMLS()

        assertEquals(valid, filtered)
    }
}