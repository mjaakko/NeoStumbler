package xyz.malkki.neostumbler.utils

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkHelperTest {

    @Test
    fun `local addresses return true`() = runTest {
        val localAddresses =
            listOf(
                "10.0.0.1",
                "10.255.255.255",
                "172.16.0.1",
                "172.31.255.255",
                "192.168.1.1",
                "192.168.255.255",
                "100.64.0.1",
                "100.127.255.255",
                "169.254.169.254",
                "169.254.255.255",
                "255.255.255.255",
                "224.0.0.1",
                "239.255.255.255",
            )

        localAddresses.forEach { ip -> assertTrue("Expected $ip to be local", ip.isLocalNetwork()) }
    }

    @Test
    fun `non-local addresses return false`() = runTest {
        val nonLocalAddresses =
            listOf(
                "9.255.255.255",
                "11.0.0.0",
                "172.15.255.255",
                "172.32.0.0",
                "192.167.255.255",
                "192.169.0.0",
                "100.63.255.255",
                "100.128.0.0",
                "169.253.255.255",
                "169.255.0.0",
                "223.255.255.255",
                "240.0.0.0",
                "8.8.8.8",
                "1.1.1.1",
                // Loopback and zero
                "127.0.0.1",
                "0.0.0.0",
            )

        nonLocalAddresses.forEach { ip ->
            assertFalse("Expected $ip to not be local", ip.isLocalNetwork())
        }
    }
}
